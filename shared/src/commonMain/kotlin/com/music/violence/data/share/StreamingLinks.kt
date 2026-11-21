package com.music.violence.data.share

/** The music services whose links Violence can open as a song. */
enum class StreamingService(val label: String) {
    APPLE_MUSIC("Apple Music"),
    SPOTIFY("Spotify"),
    DEEZER("Deezer"),
    SOUNDCLOUD("SoundCloud"),
}

/**
 * What a link from another music service points at. Only the shape is read here: no network, no
 * title. Turning a [Track] into a song Violence can play is [com.music.violence.data.share.StreamingTracks]
 * in the platform code, which asks the service's public page or API what the song is called.
 */
sealed interface StreamingLink {
    val service: StreamingService

    /** The link as it was given, kept for a short link that has to be followed. */
    val url: String

    /** One song. [id] is the service's own id; for SoundCloud, the page's path (`/artist/song`). */
    data class Track(
        override val service: StreamingService,
        val id: String,
        override val url: String,
        /** The country page a link was shared from (Apple Music's `us`, `in`), where the id is known. */
        val storefront: String? = null,
    ) : StreamingLink

    data class Album(
        override val service: StreamingService,
        val id: String,
        override val url: String,
        val storefront: String? = null,
    ) : StreamingLink

    data class Playlist(
        override val service: StreamingService,
        val id: String,
        override val url: String,
    ) : StreamingLink

    data class Artist(
        override val service: StreamingService,
        val id: String,
        override val url: String,
    ) : StreamingLink

    /** A short link (`spotify.link/…`) that only a request can open, to be followed to the real one. */
    data class Short(
        override val service: StreamingService,
        override val url: String,
    ) : StreamingLink
}

/**
 * Reads links from Apple Music, Spotify, Deezer and SoundCloud, in any of the forms they are shared in:
 * the web address (with or without a country or `intl-` prefix, tracking parameters and all), the
 * `spotify:track:…` form, and the short links that redirect.
 *
 * Strict about what it accepts: a link that is not clearly a song, an album, a playlist or an artist of
 * one of these services is null, so something else that merely has a similar address is never claimed.
 */
object StreamingLinks {

    private val SPOTIFY_ID = Regex("^[A-Za-z0-9]{22}$")
    private val DIGITS = Regex("^[0-9]+$")
    private val URL_IN_TEXT = Regex("""https?://[^\s<>"']+""")
    private val SPOTIFY_URI = Regex("""spotify:(track|album|playlist|artist):([A-Za-z0-9]{22})""")

    private val APPLE_HOSTS = setOf("music.apple.com", "embed.music.apple.com", "geo.music.apple.com", "itunes.apple.com")
    private val SPOTIFY_HOSTS = setOf("open.spotify.com", "play.spotify.com")
    private val SPOTIFY_SHORT = setOf("spotify.link", "spotify.app.link")
    private val DEEZER_HOSTS = setOf("deezer.com", "www.deezer.com")
    private val DEEZER_SHORT = setOf("link.deezer.com", "deezer.page.link", "dzr.page.link")
    private val SOUNDCLOUD_HOSTS = setOf("soundcloud.com", "m.soundcloud.com", "www.soundcloud.com")
    private val SOUNDCLOUD_SHORT = setOf("on.soundcloud.com")

    /** Top-level SoundCloud pages that are not an artist's. */
    private val SOUNDCLOUD_RESERVED = setOf(
        "discover", "search", "stream", "you", "upload", "charts", "pages", "terms-of-use", "tags", "mobile",
        "messages", "notifications", "settings", "popular", "feed", "people", "jobs", "login", "signup", "imprint",
        "creators", "pro", "go", "download", "stations",
    )

    /** What follows an artist on SoundCloud that is a listing, not a song. */
    private val SOUNDCLOUD_LISTINGS = setOf(
        "tracks", "albums", "sets", "likes", "followers", "following", "reposts", "popular-tracks", "comments", "spotlight",
    )

    /** The first link of one of these services in [text], which may be a whole shared message. */
    fun extract(text: String?): StreamingLink? {
        if (text.isNullOrBlank()) return null
        SPOTIFY_URI.find(text)?.let { parse(it.value)?.let { link -> return link } }
        for (match in URL_IN_TEXT.findAll(text)) {
            parse(match.value.trimEnd('.', ',', ')', ']', '!', '?'))?.let { return it }
        }
        return null
    }

    /** The link [raw] is, or null when it is not one of these services' (or not a link to anything in them). */
    fun parse(raw: String?): StreamingLink? {
        val text = raw?.trim() ?: return null
        if (text.isEmpty() || text.length > MAX_LENGTH) return null
        SPOTIFY_URI.matchEntire(text)?.let { m ->
            return spotifyLink(m.groupValues[1], m.groupValues[2], text)
        }
        val url = SplitUrl.of(text) ?: return null
        val host = url.host
        return when (host) {
            in APPLE_HOSTS -> apple(url, text)
            in SPOTIFY_HOSTS -> spotify(url, text)
            in SPOTIFY_SHORT -> StreamingLink.Short(StreamingService.SPOTIFY, text)
            in DEEZER_HOSTS -> deezer(url, text)
            in DEEZER_SHORT -> StreamingLink.Short(StreamingService.DEEZER, text)
            in SOUNDCLOUD_HOSTS -> soundcloud(url, text)
            in SOUNDCLOUD_SHORT -> StreamingLink.Short(StreamingService.SOUNDCLOUD, text)
            else -> null
        }
    }

    private fun apple(url: SplitUrl, text: String): StreamingLink? {
        val segments = url.segments
        val kindAt = segments.indexOfFirst { it == "song" || it == "album" || it == "playlist" || it == "artist" }
        if (kindAt < 0) return null
        val storefront = segments.firstOrNull()?.takeIf { kindAt > 0 && it.length == 2 && it.all(Char::isLetter) }?.lowercase()
        val after = segments.drop(kindAt + 1)
        // Older addresses write the id as `id123`.
        val ids = after.map { it.removePrefix("id") }.filter { DIGITS.matches(it) }
        return when (segments[kindAt]) {
            "song" -> ids.lastOrNull()?.let { StreamingLink.Track(StreamingService.APPLE_MUSIC, it, text, storefront) }
            "album" -> {
                // A song shared from an album page keeps the album in the path and names the song in `i`.
                val song = url.query["i"]?.takeIf { DIGITS.matches(it) }
                when {
                    song != null -> StreamingLink.Track(StreamingService.APPLE_MUSIC, song, text, storefront)
                    ids.isNotEmpty() -> StreamingLink.Album(StreamingService.APPLE_MUSIC, ids.last(), text, storefront)
                    else -> null
                }
            }
            "playlist" -> after.lastOrNull { it.startsWith("pl.") }
                ?.let { StreamingLink.Playlist(StreamingService.APPLE_MUSIC, it, text) }
            "artist" -> ids.lastOrNull()?.let { StreamingLink.Artist(StreamingService.APPLE_MUSIC, it, text) }
            else -> null
        }
    }

    private fun spotify(url: SplitUrl, text: String): StreamingLink? {
        val segments = url.segments.filterNot { it == "embed" || (it.startsWith("intl-") && it.length <= 9) }
        val kindAt = segments.indexOfFirst { it == "track" || it == "album" || it == "playlist" || it == "artist" }
        if (kindAt < 0) return null
        val id = segments.getOrNull(kindAt + 1) ?: return null
        return spotifyLink(segments[kindAt], id, text)
    }

    private fun spotifyLink(kind: String, id: String, text: String): StreamingLink? {
        if (!SPOTIFY_ID.matches(id)) return null
        return when (kind) {
            "track" -> StreamingLink.Track(StreamingService.SPOTIFY, id, text)
            "album" -> StreamingLink.Album(StreamingService.SPOTIFY, id, text)
            "playlist" -> StreamingLink.Playlist(StreamingService.SPOTIFY, id, text)
            "artist" -> StreamingLink.Artist(StreamingService.SPOTIFY, id, text)
            else -> null
        }
    }

    private fun deezer(url: SplitUrl, text: String): StreamingLink? {
        // A language prefix (`/en/`, `/fr/`) may come first.
        val segments = url.segments.filterNot { it.length == 2 && it.all(Char::isLetter) }
        val kind = segments.firstOrNull() ?: return null
        val id = segments.getOrNull(1)?.takeIf { DIGITS.matches(it) } ?: return null
        return when (kind) {
            "track" -> StreamingLink.Track(StreamingService.DEEZER, id, text)
            "album" -> StreamingLink.Album(StreamingService.DEEZER, id, text)
            "playlist" -> StreamingLink.Playlist(StreamingService.DEEZER, id, text)
            "artist" -> StreamingLink.Artist(StreamingService.DEEZER, id, text)
            else -> null
        }
    }

    private fun soundcloud(url: SplitUrl, text: String): StreamingLink? {
        val segments = url.segments
        val artist = segments.firstOrNull() ?: return null
        if (artist in SOUNDCLOUD_RESERVED) return null
        return when {
            segments.size == 2 && segments[1] !in SOUNDCLOUD_LISTINGS ->
                StreamingLink.Track(StreamingService.SOUNDCLOUD, "/${segments[0]}/${segments[1]}", text)
            segments.size == 3 && segments[1] == "sets" ->
                StreamingLink.Playlist(StreamingService.SOUNDCLOUD, "/${segments[0]}/sets/${segments[2]}", text)
            segments.size == 1 -> StreamingLink.Artist(StreamingService.SOUNDCLOUD, "/$artist", text)
            else -> null
        }
    }

    private const val MAX_LENGTH = 2048
}

/** A web address taken apart, without any platform's URL class: host in lower case, path segments, query. */
internal class SplitUrl private constructor(
    val host: String,
    val segments: List<String>,
    val query: Map<String, String>,
) {
    companion object {
        fun of(text: String): SplitUrl? {
            val lower = text.lowercase()
            val rest = when {
                lower.startsWith("https://") -> text.substring(8)
                lower.startsWith("http://") -> text.substring(7)
                else -> return null
            }
            val cut = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
            val authority = if (cut < 0) rest else rest.substring(0, cut)
            // No credentials and no port in a link worth opening.
            if ('@' in authority || ':' in authority || authority.isEmpty()) return null
            val tail = if (cut < 0) "" else rest.substring(cut)
            val noFragment = tail.substringBefore('#')
            val path = noFragment.substringBefore('?')
            val query = noFragment.substringAfter('?', "")
            val segments = path.split('/').filter { it.isNotEmpty() }.map(::decode)
            val map = LinkedHashMap<String, String>()
            for (pair in query.split('&')) {
                if (pair.isEmpty()) continue
                val key = decode(pair.substringBefore('='))
                if (key !in map) map[key] = decode(pair.substringAfter('=', ""))
            }
            return SplitUrl(authority.lowercase(), segments, map)
        }

        private fun decode(value: String): String {
            if ('%' !in value && '+' !in value) return value
            val out = ArrayList<Byte>(value.length)
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '%' && i + 2 < value.length) {
                    val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
                    if (hex != null) {
                        out.add(hex.toByte())
                        i += 3
                        continue
                    }
                }
                if (c == '+') out.add(' '.code.toByte()) else c.toString().encodeToByteArray().forEach { out.add(it) }
                i++
            }
            return out.toByteArray().decodeToString()
        }
    }
}
