package com.music.violence.data.spotify

import com.music.violence.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/**
 * Reads a public Apple Music playlist from its web page, with no account or key.
 *
 * The page carries its tracks twice: a schema.org block with titles only, and the
 * page's own serialized data, which also has each song's artist. The second is
 * read. The result is the same shape [SpotifyImporter] hands over, so the songs
 * are matched on YouTube Music by the same code ([SpotifyImporter.resolveToSongs]).
 *
 * Only what the page lists is imported: a very long playlist may be cut short by
 * Apple, and a private or library-only playlist has no public page at all.
 */
object AppleMusicImporter {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Apple's playlist ids look like `pl.f4d106fed2bd41149aaacabb233eb5eb` or `pl.u-…`. */
    private val PLAYLIST_ID = Regex("""pl\.[A-Za-z0-9\-]+""")

    private fun isAppleHost(host: String) =
        host == "music.apple.com" || host == "embed.music.apple.com" || host == "classical.music.apple.com"

    /**
     * The page address of the playlist [input] names, or null when it is not an Apple Music
     * playlist link. Any storefront ("us", "in", "gb"…) is kept, since a playlist is public in
     * the storefront it was shared from.
     */
    fun extractPlaylistUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        if (!isAppleHost(uri.host?.lowercase().orEmpty())) return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val at = segments.indexOf("playlist")
        if (at == -1) return null
        val id = segments.drop(at + 1).firstOrNull { PLAYLIST_ID.matches(it) } ?: return null
        val storefront = segments.firstOrNull()?.takeIf { it.length == 2 && it != "pl" } ?: "us"
        val slug = segments.getOrNull(at + 1)?.takeIf { it != id } ?: "playlist"
        return "https://music.apple.com/$storefront/playlist/$slug/$id"
    }

    /** The playlist's name and its songs, as the page lists them. */
    suspend fun fetchPlaylistTracks(url: String): Pair<String, List<SpotifyImportTrack>> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            val html = Http.client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    error("Failed to load the Apple Music playlist (HTTP ${response.code})")
                }
                body
            }
            parsePlaylistPage(html)
        }

    internal fun parsePlaylistPage(html: String): Pair<String, List<SpotifyImportTrack>> {
        val dataRegex = Regex(
            """<script[^>]*id="serialized-server-data"[^>]*>(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val data = dataRegex.find(html)?.groupValues?.get(1)
            ?: error("Could not read the Apple Music playlist. Make sure it is public.")
        val tracks = mutableListOf<SpotifyImportTrack>()
        collectSongs(json.parseToJsonElement(data), tracks)
        if (tracks.isEmpty()) error("No songs found in that Apple Music playlist.")

        val ldRegex = Regex(
            """<script[^>]*id=["']?schema:music-playlist["']?[^>]*>(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val name = ldRegex.find(html)?.groupValues?.get(1)
            ?.let { runCatching { json.parseToJsonElement(it).jsonObject["name"]?.jsonPrimitive?.content }.getOrNull() }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Imported Apple Music Playlist"
        return name to tracks
    }

    /** Every song object under [element], in page order. A song has a title, an artist and kind "song". */
    private fun collectSongs(element: JsonElement, out: MutableList<SpotifyImportTrack>) {
        when (element) {
            is JsonObject -> {
                val title = (element["title"] as? JsonPrimitive)?.content
                val artist = (element["artistName"] as? JsonPrimitive)?.content
                val kind = (element["contentDescriptor"] as? JsonObject)?.get("kind")
                    ?.let { (it as? JsonPrimitive)?.content }
                if (title != null && artist != null && (kind == null || kind == "song")) {
                    out.add(SpotifyImportTrack(title = title.trim(), artist = artist.trim()))
                } else {
                    element.values.forEach { collectSongs(it, out) }
                }
            }
            is JsonArray -> element.forEach { collectSongs(it, out) }
            else -> Unit
        }
    }
}
