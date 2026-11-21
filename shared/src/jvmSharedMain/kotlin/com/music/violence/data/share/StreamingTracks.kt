package com.music.violence.data.share

import com.music.violence.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/** What another service calls a song: enough to find the same one on YouTube Music. */
data class StreamingTrack(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
)

/** What another service calls an album. */
data class StreamingAlbum(val title: String, val artist: String)

/**
 * Asks a music service, by its public page or API and with no account, what a linked song or album is
 * called, so Violence can find it. Only the link's own id goes out, only after the person opened the
 * link, and what comes back is a title and a name: nothing is stored.
 *
 *  - Apple Music: the iTunes lookup API, by the song's id and its country page.
 *  - Spotify: the embed page of the track, which carries its name, artists and length.
 *  - Deezer: the public API, by id.
 *  - SoundCloud: oEmbed, by the page address.
 *
 * The parsing is kept apart from the fetching, so it is tested against real responses.
 */
object StreamingTracks {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** The song a link names, or null when the service did not say (a private or removed song, no network). */
    suspend fun track(link: StreamingLink.Track): StreamingTrack? = withContext(Dispatchers.IO) {
        runCatching {
            when (link.service) {
                StreamingService.APPLE_MUSIC -> appleTrack(link)
                StreamingService.SPOTIFY -> parseSpotifyEmbed(get("https://open.spotify.com/embed/track/${link.id}"))
                StreamingService.DEEZER -> parseDeezerTrack(get("https://api.deezer.com/track/${link.id}"))
                StreamingService.SOUNDCLOUD -> parseSoundCloudOEmbed(
                    get("https://soundcloud.com/oembed".toHttpUrl().newBuilder()
                        .addQueryParameter("format", "json")
                        .addQueryParameter("url", "https://soundcloud.com${link.id}")
                        .build().toString()),
                )
            }
        }.getOrNull()
    }

    /** The album a link names, or null. */
    suspend fun album(link: StreamingLink.Album): StreamingAlbum? = withContext(Dispatchers.IO) {
        runCatching {
            when (link.service) {
                StreamingService.APPLE_MUSIC -> parseItunesAlbum(appleLookup(link.id, link.storefront))
                StreamingService.DEEZER -> parseDeezerAlbum(get("https://api.deezer.com/album/${link.id}"))
                StreamingService.SPOTIFY -> parseSpotifyEmbedAlbum(get("https://open.spotify.com/embed/album/${link.id}"))
                StreamingService.SOUNDCLOUD -> null
            }
        }.getOrNull()
    }

    /** Where a short link goes: the full link it redirects to, or null if it does not lead to one of these services. */
    suspend fun follow(link: StreamingLink.Short): StreamingLink? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(link.url).header("User-Agent", USER_AGENT).build()
            Http.client.newCall(request).execute().use { response ->
                StreamingLinks.parse(response.request.url.toString())?.takeIf { it !is StreamingLink.Short }
            }
        }.getOrNull()
    }

    private fun appleTrack(link: StreamingLink.Track): StreamingTrack? =
        parseItunesTrack(appleLookup(link.id, link.storefront))

    /** The lookup in the link's own country page, then in the US one: a song may be missing from the first. */
    private fun appleLookup(id: String, storefront: String?): String {
        val countries = listOfNotNull(storefront, "us").distinct()
        var last = ""
        for (country in countries) {
            last = get("https://itunes.apple.com/lookup?id=$id&country=$country")
            if (resultCount(last) > 0) return last
        }
        return last
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).header("Accept-Language", "en-US,en;q=0.9").build()
        return Http.client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) error("HTTP ${response.code}")
            body
        }
    }

    // ---- parsing: pure, and tested on real responses -------------------------------------------

    internal fun resultCount(lookup: String): Int =
        runCatching { json.parseToJsonElement(lookup).jsonObject["resultCount"]?.jsonPrimitive?.content?.toIntOrNull() }
            .getOrNull() ?: 0

    internal fun parseItunesTrack(body: String): StreamingTrack? {
        val item = firstResult(body) ?: return null
        if (item["wrapperType"]?.text() != "track" && item["kind"]?.text() != "song") return null
        val title = item["trackName"]?.text() ?: return null
        val artist = item["artistName"]?.text() ?: return null
        return StreamingTrack(
            title = title,
            artist = artist,
            album = item["collectionName"]?.text(),
            durationMs = item["trackTimeMillis"]?.longValue(),
        )
    }

    internal fun parseItunesAlbum(body: String): StreamingAlbum? {
        val item = firstResult(body) ?: return null
        val title = item["collectionName"]?.text() ?: return null
        val artist = item["artistName"]?.text() ?: return null
        return StreamingAlbum(title, artist)
    }

    private fun firstResult(body: String): JsonObject? =
        runCatching { json.parseToJsonElement(body).jsonObject["results"]?.jsonArray?.firstOrNull()?.jsonObject }.getOrNull()

    internal fun parseDeezerTrack(body: String): StreamingTrack? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["error"] != null) return null
        val title = root["title"]?.text() ?: return null
        val artist = root["artist"]?.jsonObject?.get("name")?.text() ?: return null
        return StreamingTrack(
            title = title,
            artist = artist,
            album = root["album"]?.jsonObject?.get("title")?.text(),
            durationMs = root["duration"]?.longValue()?.takeIf { it > 0 }?.times(1000),
        )
    }

    internal fun parseDeezerAlbum(body: String): StreamingAlbum? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["error"] != null) return null
        val title = root["title"]?.text() ?: return null
        val artist = root["artist"]?.jsonObject?.get("name")?.text() ?: return null
        return StreamingAlbum(title, artist)
    }

    /** SoundCloud writes the title as `Song by Artist`; the artist is also given on its own. */
    internal fun parseSoundCloudOEmbed(body: String): StreamingTrack? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val full = root["title"]?.text() ?: return null
        val author = root["author_name"]?.text()
        val title = if (author != null && full.endsWith(" by $author")) full.removeSuffix(" by $author") else full
        return StreamingTrack(title = title.trim(), artist = author ?: "")
    }

    /** The embed page of a track holds a `__NEXT_DATA__` block with the track as `entity`. */
    internal fun parseSpotifyEmbed(html: String): StreamingTrack? {
        val entity = spotifyEntity(html) ?: return null
        val title = (entity["name"] ?: entity["title"])?.text() ?: return null
        val artist = entity["artists"]?.let { artists ->
            (artists as? JsonArray)?.mapNotNull { it.jsonObject["name"]?.text() }?.joinToString(", ")
        }?.takeIf { it.isNotBlank() } ?: entity["subtitle"]?.text() ?: return null
        return StreamingTrack(
            title = title,
            artist = artist,
            album = null,
            durationMs = (entity["duration"] ?: entity["durationMs"])?.longValue()?.takeIf { it > 0 },
        )
    }

    internal fun parseSpotifyEmbedAlbum(html: String): StreamingAlbum? {
        val entity = spotifyEntity(html) ?: return null
        val title = (entity["name"] ?: entity["title"])?.text() ?: return null
        val artist = entity["artists"]?.let { (it as? JsonArray)?.mapNotNull { a -> a.jsonObject["name"]?.text() }?.joinToString(", ") }
            ?.takeIf { it.isNotBlank() } ?: entity["subtitle"]?.text() ?: return null
        return StreamingAlbum(title, artist)
    }

    private fun spotifyEntity(html: String): JsonObject? {
        val block = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: return null
        val props = runCatching { json.parseToJsonElement(block).jsonObject["props"]?.jsonObject?.get("pageProps")?.jsonObject }
            .getOrNull() ?: return null
        return props["state"]?.jsonObject?.get("data")?.jsonObject?.get("entity")?.jsonObject
            ?: props["entity"]?.jsonObject
    }

    /** Apple's page title is `Song by Artist on Apple Music`: the way to read a song when the lookup has none. */
    internal fun parseApplePageTitle(html: String): StreamingTrack? {
        val title = Regex("""<meta[^>]*property="og:title"[^>]*content="([^"]+)"""").find(html)?.groupValues?.get(1)
            ?.let(::unescape) ?: return null
        val match = Regex("""^(.*) by (.*) on Apple Music$""").find(title) ?: return null
        return StreamingTrack(match.groupValues[1].trim(), match.groupValues[2].trim())
    }

    private fun unescape(text: String): String = text
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")

    private fun JsonElement.text(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonElement.longValue(): Long? = (this as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toDoubleOrNull()?.toLong() }
}
