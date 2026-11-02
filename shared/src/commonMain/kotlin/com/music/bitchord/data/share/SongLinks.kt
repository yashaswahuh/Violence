package com.music.bitchord.data.share

import com.music.bitchord.data.listentogether.JamInvite
import com.music.bitchord.data.profile.ProfileCard
import com.music.bitchord.data.profile.ProfileCardCodec
import com.music.bitchord.data.profile.ProfileRules

/** Where a shared song's id comes from. */
enum class SongSource(val code: String) {
    /** A YouTube video id: the real playable id. */
    YOUTUBE("yt"),

    /** No id to play, only words: the receiver searches "title artist". */
    SEARCH("q"),

    /** A Spotify track id. Accepted on input; there is no way to play it, so it needs a title to search by. */
    SPOTIFY("sp"),
    ;

    companion object {
        fun of(code: String): SongSource? = entries.firstOrNull { it.code == code }
    }
}

/** A song named by a link. [title] and [artist] are display hints only. */
data class SongRef(
    val source: SongSource,
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    /** Who sent it, if they chose to say (see [SharedFrom]). A label, never part of the song's identity. */
    val from: ProfileCard? = null,
) {
    /** The same song, whatever hints came with it: used to spot a repeated link. */
    val key: String get() = "song:${source.code}:${if (source == SongSource.SEARCH) searchWords() else id}"

    /** The words to search for when there is no playable id. */
    fun searchWords(): String = listOfNotNull(title, artist).joinToString(" ").trim()
}

/**
 * Song links: `violence://song/<source>/<id>?v=1&t=<title>&a=<artist>` and the same path under the
 * web base (`https://yashaswahuh.is-a.dev/Violence/song/...`).
 *
 * Like the profile card, every incoming link is untrusted. [parse] never throws, never truncates
 * and never "repairs"; it returns a valid [SongRef] or a coded reason (the text is never echoed).
 * [toUri] does the opposite job and **does** shorten the hints, so a link always fits an NFC tag.
 * The title in a link is only a label: a `yt` song is always looked up by its id.
 */
object SongLinks {

    const val VERSION = "1"
    const val CUSTOM_PREFIX = "violence://song/"
    const val WEB_PREFIX = "${JamInvite.ORIGIN}/song/"

    /** The `violence://` form is built to fit in this many UTF-8 bytes. */
    const val TARGET_BYTES = 200
    const val MAX_INPUT_CHARS = 512
    const val TITLE_MAX = 120
    const val ARTIST_MAX = 80

    /** The id of a search-only link. */
    const val SEARCH_ID = "search"

    enum class Error {
        NOT_A_SONG_LINK,
        TOO_LONG,
        WRONG_VERSION,
        MISSING_FIELD,
        DUPLICATE_FIELD,
        UNKNOWN_FIELD,
        BAD_SOURCE,
        BAD_ID,
        BAD_ENCODING,
        BAD_TITLE,
        BAD_ARTIST,
        BAD_FROM,
    }

    sealed interface Result {
        data class Ok(val ref: SongRef) : Result
        data class Rejected(val error: Error) : Result
    }

    fun isYouTubeId(id: String): Boolean =
        id.length == 11 && id.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' }

    fun isSpotifyId(id: String): Boolean =
        id.length == 22 && id.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }

    // ------------------------------------------------------------------ building --

    /** A link to the YouTube video [videoId], or null if it is not a valid id. */
    fun youtube(videoId: String, title: String? = null, artist: String? = null): SongRef? =
        if (isYouTubeId(videoId)) SongRef(SongSource.YOUTUBE, videoId, hint(title, ShareText.TITLE_BUILD_MAX), hint(artist, ShareText.ARTIST_BUILD_MAX)) else null

    /** A search-only link, or null when there is no title to search for. */
    fun search(title: String?, artist: String? = null): SongRef? {
        val t = hint(title, ShareText.TITLE_BUILD_MAX) ?: return null
        return SongRef(SongSource.SEARCH, SEARCH_ID, t, hint(artist, ShareText.ARTIST_BUILD_MAX))
    }

    private fun hint(raw: String?, max: Int): String? = raw?.let { ShareText.sanitize(it, max) }?.takeIf { it.isNotEmpty() }

    /** [ref] with the hints shortened until the `violence://` form fits [maxBytes]. */
    fun fit(ref: SongRef, maxBytes: Int = TARGET_BYTES): SongRef {
        var title = ref.title
        var artist = ref.artist
        var from = ref.from
        while (build(CUSTOM_PREFIX, ref.copy(title = title, artist = artist, from = from)).encodeToByteArray().size > maxBytes) {
            // Cut the longer of the two, one character at a time, never mid-character.
            val tLen = title?.let { ProfileCardCodec.percentEncode(it).length } ?: 0
            val aLen = artist?.let { ProfileCardCodec.percentEncode(it).length } ?: 0
            if (tLen == 0 && aLen == 0) {
                // Nothing left to shorten: the "sent by" label is the next thing to give up.
                if (from == null) break
                from = null
                continue
            }
            if (aLen >= tLen) artist = artist?.let { ShareText.dropLast(it) } else title = title?.let { ShareText.dropLast(it) }
        }
        return ref.copy(title = title?.takeIf { it.isNotEmpty() }, artist = artist?.takeIf { it.isNotEmpty() }, from = from)
    }

    /** `violence://song/...`, at most [maxBytes] bytes (hints shortened to fit). */
    fun toUri(ref: SongRef, maxBytes: Int = TARGET_BYTES): String = build(CUSTOM_PREFIX, fit(ref, maxBytes))

    /** The https landing URL, with the same (shortened) hints. */
    fun toWebUrl(ref: SongRef, maxBytes: Int = TARGET_BYTES): String = build(WEB_PREFIX, fit(ref, maxBytes))

    private fun build(prefix: String, ref: SongRef): String {
        val sb = StringBuilder(prefix).append(ref.source.code).append('/').append(ref.id).append("?v=").append(VERSION)
        ref.title?.let { sb.append("&t=").append(ProfileCardCodec.percentEncode(it)) }
        ref.artist?.let { sb.append("&a=").append(ProfileCardCodec.percentEncode(it)) }
        ref.from?.let { sb.append(SharedFrom.query(it)) }
        return sb.toString()
    }

    // ------------------------------------------------------------------- parsing --

    fun parse(input: String?): Result {
        val text = input?.trim().orEmpty()
        if (text.length > MAX_INPUT_CHARS) return Result.Rejected(Error.TOO_LONG)
        val withoutFragment = text.substringBefore('#')
        val prefixLength = when {
            withoutFragment.startsWith(CUSTOM_PREFIX, ignoreCase = true) -> CUSTOM_PREFIX.length
            withoutFragment.startsWith(WEB_PREFIX, ignoreCase = true) -> WEB_PREFIX.length
            else -> return Result.Rejected(Error.NOT_A_SONG_LINK)
        }
        val rest = withoutFragment.substring(prefixLength)
        val path = rest.substringBefore('?')
        val query = if (rest.contains('?')) rest.substringAfter('?') else null
        val segments = path.split('/')
        if (segments.size != 2) return Result.Rejected(Error.NOT_A_SONG_LINK)
        val source = SongSource.of(segments[0]) ?: return Result.Rejected(Error.BAD_SOURCE)
        val id = segments[1]
        when (source) {
            SongSource.YOUTUBE -> if (!isYouTubeId(id)) return Result.Rejected(Error.BAD_ID)
            SongSource.SPOTIFY -> if (!isSpotifyId(id)) return Result.Rejected(Error.BAD_ID)
            SongSource.SEARCH -> if (id != SEARCH_ID) return Result.Rejected(Error.BAD_ID)
        }
        if (query == null) return Result.Rejected(Error.MISSING_FIELD)

        val fields = HashMap<String, String>()
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val key = pair.substringBefore('=')
            val value = if (pair.contains('=')) pair.substringAfter('=') else ""
            if (key != "v" && key != "t" && key != "a" && key !in SharedFrom.KEYS) return Result.Rejected(Error.UNKNOWN_FIELD)
            if (fields.put(key, value) != null) return Result.Rejected(Error.DUPLICATE_FIELD)
        }
        val version = fields["v"] ?: return Result.Rejected(Error.MISSING_FIELD)
        if (version != VERSION) return Result.Rejected(Error.WRONG_VERSION)

        val title = fields["t"]?.let { raw ->
            val decoded = ProfileCardCodec.percentDecode(raw) ?: return Result.Rejected(Error.BAD_ENCODING)
            cleanHint(decoded, TITLE_MAX) ?: return Result.Rejected(Error.BAD_TITLE)
        }
        val artist = fields["a"]?.let { raw ->
            val decoded = ProfileCardCodec.percentDecode(raw) ?: return Result.Rejected(Error.BAD_ENCODING)
            cleanHint(decoded, ARTIST_MAX) ?: return Result.Rejected(Error.BAD_ARTIST)
        }
        if ((source == SongSource.SEARCH || source == SongSource.SPOTIFY) && title == null) {
            return Result.Rejected(Error.MISSING_FIELD)
        }
        val from = when (val read = SharedFrom.read(fields)) {
            SharedFrom.Read.None -> null
            SharedFrom.Read.Bad -> return Result.Rejected(Error.BAD_FROM)
            is SharedFrom.Read.Ok -> read.card
        }
        return Result.Ok(SongRef(source, id, title, artist, from))
    }

    private fun cleanHint(decoded: String, max: Int): String? =
        (ProfileRules.checkText(decoded, max) as? ProfileRules.NameResult.Valid)?.name
}
