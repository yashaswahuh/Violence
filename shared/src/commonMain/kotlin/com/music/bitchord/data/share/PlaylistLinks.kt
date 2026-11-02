package com.music.bitchord.data.share

import com.music.bitchord.data.listentogether.JamInvite
import com.music.bitchord.data.profile.ProfileCard
import com.music.bitchord.data.profile.ProfileCardCodec
import com.music.bitchord.data.profile.ProfileRules

/**
 * A playlist named by a link. Two shapes:
 *  - [Kind.YOUTUBE]: a YouTube Music playlist by its id ([id]); the receiver opens that page.
 *  - [Kind.SONGS]: the first few songs of a playlist that lives only on the sender's phone
 *    ([videoIds]); the receiver can save them as a playlist of their own. A tap carries only what
 *    fits on an NFC tag, so a long playlist arrives shortened.
 */
data class PlaylistRef(
    val kind: Kind,
    val id: String?,
    val title: String,
    val videoIds: List<String> = emptyList(),
    val from: ProfileCard? = null,
) {
    enum class Kind(val code: String) { YOUTUBE("yt"), SONGS("songs") }

    /** The same playlist whatever the title or sender, to spot a repeated link. */
    val key: String get() = if (kind == Kind.YOUTUBE) "playlist:yt:$id" else "playlist:songs:${videoIds.joinToString(",")}"
}

/**
 * Playlist links: `violence://playlist/yt/<playlistId>?v=1&t=<title>` and
 * `violence://playlist/songs?v=1&t=<title>&s=<id>,<id>,...`, and the same under the web base.
 * As with [SongLinks], every incoming link is untrusted: [parse] never throws, never repairs, and
 * returns a valid [PlaylistRef] or [Result.Rejected]. [toUri] shortens to fit.
 */
object PlaylistLinks {

    const val VERSION = "1"
    const val CUSTOM_PREFIX = "violence://playlist/"
    const val WEB_PREFIX = "${JamInvite.ORIGIN}/playlist/"
    const val TARGET_BYTES = 200
    const val MAX_INPUT_CHARS = 512
    const val TITLE_MAX = 60
    const val MAX_SONGS = 30

    sealed interface Result {
        data class Ok(val ref: PlaylistRef) : Result
        data class Rejected(val wrongVersion: Boolean = false) : Result
    }

    fun isPlaylistId(id: String): Boolean =
        id.length in 10..64 && id.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' }

    // ----------------------------------------------------------------- building --

    /** A link to the YouTube Music playlist [rawId] (a leading `VL` is dropped), or null if it is not an id. */
    fun youtube(rawId: String, title: String?, from: ProfileCard? = null): PlaylistRef? {
        val id = rawId.removePrefix("VL")
        if (!isPlaylistId(id)) return null
        return PlaylistRef(PlaylistRef.Kind.YOUTUBE, id, hint(title), from = from)
    }

    /** The first [MAX_SONGS] playable ids of a playlist kept on this phone, or null if none has an id. */
    fun songs(title: String?, videoIds: List<String>, from: ProfileCard? = null): PlaylistRef? {
        val ids = videoIds.filter(SongLinks::isYouTubeId).distinct().take(MAX_SONGS)
        if (ids.isEmpty()) return null
        return PlaylistRef(PlaylistRef.Kind.SONGS, null, hint(title), ids, from)
    }

    private fun hint(title: String?): String =
        title?.let { ShareText.sanitize(it, TITLE_MAX) }.orEmpty().ifBlank { "Playlist" }

    fun fit(ref: PlaylistRef, maxBytes: Int = TARGET_BYTES): PlaylistRef {
        var title = ref.title
        var ids = ref.videoIds
        var from = ref.from
        while (build(CUSTOM_PREFIX, ref.copy(title = title, videoIds = ids, from = from)).encodeToByteArray().size > maxBytes) {
            when {
                ids.size > 1 -> ids = ids.dropLast(1)
                title.length > 1 -> title = ShareText.dropLast(title)
                from != null -> from = null
                else -> break
            }
        }
        return ref.copy(title = title, videoIds = ids, from = from)
    }

    fun toUri(ref: PlaylistRef, maxBytes: Int = TARGET_BYTES): String = build(CUSTOM_PREFIX, fit(ref, maxBytes))

    fun toWebUrl(ref: PlaylistRef, maxBytes: Int = TARGET_BYTES): String = build(WEB_PREFIX, fit(ref, maxBytes))

    private fun build(prefix: String, ref: PlaylistRef): String {
        val sb = StringBuilder(prefix)
        if (ref.kind == PlaylistRef.Kind.YOUTUBE) sb.append("yt/").append(ref.id) else sb.append("songs")
        sb.append("?v=").append(VERSION).append("&t=").append(ProfileCardCodec.percentEncode(ref.title))
        if (ref.kind == PlaylistRef.Kind.SONGS) sb.append("&s=").append(ref.videoIds.joinToString(","))
        ref.from?.let { sb.append(SharedFrom.query(it)) }
        return sb.toString()
    }

    // ------------------------------------------------------------------ parsing --

    fun parse(input: String?): Result {
        val text = input?.trim().orEmpty()
        if (text.length > MAX_INPUT_CHARS) return Result.Rejected()
        val withoutFragment = text.substringBefore('#')
        val prefixLength = when {
            withoutFragment.startsWith(CUSTOM_PREFIX, ignoreCase = true) -> CUSTOM_PREFIX.length
            withoutFragment.startsWith(WEB_PREFIX, ignoreCase = true) -> WEB_PREFIX.length
            else -> return Result.Rejected()
        }
        val rest = withoutFragment.substring(prefixLength)
        val path = rest.substringBefore('?')
        val query = if (rest.contains('?')) rest.substringAfter('?') else return Result.Rejected()
        val segments = path.split('/')
        val kind: PlaylistRef.Kind
        var id: String? = null
        when {
            segments.size == 2 && segments[0] == "yt" && isPlaylistId(segments[1]) -> {
                kind = PlaylistRef.Kind.YOUTUBE
                id = segments[1]
            }
            segments.size == 1 && segments[0] == "songs" -> kind = PlaylistRef.Kind.SONGS
            else -> return Result.Rejected()
        }

        val fields = HashMap<String, String>()
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val key = pair.substringBefore('=')
            val value = if (pair.contains('=')) pair.substringAfter('=') else ""
            if (key != "v" && key != "t" && key != "s" && key !in SharedFrom.KEYS) return Result.Rejected()
            if (fields.put(key, value) != null) return Result.Rejected()
        }
        val version = fields["v"] ?: return Result.Rejected()
        if (version != VERSION) return Result.Rejected(wrongVersion = true)

        val title = fields["t"]?.let { raw ->
            val decoded = ProfileCardCodec.percentDecode(raw) ?: return Result.Rejected()
            (ProfileRules.checkText(decoded, TITLE_MAX) as? ProfileRules.NameResult.Valid)?.name ?: return Result.Rejected()
        } ?: return Result.Rejected()

        var ids = emptyList<String>()
        if (kind == PlaylistRef.Kind.SONGS) {
            ids = fields["s"]?.split(',') ?: return Result.Rejected()
            if (ids.isEmpty() || ids.size > MAX_SONGS || !ids.all(SongLinks::isYouTubeId)) return Result.Rejected()
        } else if (fields.containsKey("s")) {
            return Result.Rejected()
        }
        val from = when (val read = SharedFrom.read(fields)) {
            SharedFrom.Read.None -> null
            SharedFrom.Read.Bad -> return Result.Rejected()
            is SharedFrom.Read.Ok -> read.card
        }
        return Result.Ok(PlaylistRef(kind, id, title, ids, from))
    }
}
