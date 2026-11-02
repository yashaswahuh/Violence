package com.music.bitchord.data.model

/** How the reorder page can arrange a playlist before it is saved. */
enum class PlaylistSort { TITLE, ARTIST, ALBUM, DURATION }

/**
 * [songs] ordered by [sort], case-insensitively and stably, so tracks that tie
 * (two by one artist, say) keep the order they had. [descending] flips the key
 * order only, not the order of ties.
 */
fun sortedPlaylist(songs: List<Song>, sort: PlaylistSort, descending: Boolean = false): List<Song> {
    val comparator: Comparator<Song> = when (sort) {
        PlaylistSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        PlaylistSort.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
        PlaylistSort.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.albumName.orEmpty() }
        PlaylistSort.DURATION -> compareBy { durationSeconds(it.durationText) }
    }
    return songs.sortedWith(if (descending) comparator.reversed() else comparator)
}

/** "3:47" or "1:02:03" in seconds; an unreadable or missing length sorts last. */
internal fun durationSeconds(text: String?): Int {
    val parts = text?.trim()?.split(':')?.map { it.toIntOrNull() ?: return Int.MAX_VALUE }
        ?: return Int.MAX_VALUE
    if (parts.isEmpty() || parts.size > 3) return Int.MAX_VALUE
    return parts.fold(0) { total, part -> total * 60 + part }
}
