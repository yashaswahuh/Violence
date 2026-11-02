package com.music.bitchord.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistSortTest {
    private fun song(id: String, title: String, artist: String, duration: String?) =
        Song(videoId = id, title = title, artist = artist, thumbnailUrl = null, durationText = duration)

    private val songs = listOf(
        song("a", "banana", "Zed", "3:47"),
        song("b", "Apple", "Zed", "1:02:03"),
        song("c", "cherry", "Amy", null),
    )

    @Test fun titleIgnoresCase() =
        assertEquals(listOf("b", "a", "c"), sortedPlaylist(songs, PlaylistSort.TITLE).map { it.videoId })

    @Test fun artistKeepsTiesStable() =
        assertEquals(listOf("c", "a", "b"), sortedPlaylist(songs, PlaylistSort.ARTIST).map { it.videoId })

    @Test fun durationPutsUnknownLast() =
        assertEquals(listOf("a", "b", "c"), sortedPlaylist(songs, PlaylistSort.DURATION).map { it.videoId })

    @Test fun descendingReverses() =
        assertEquals(listOf("c", "a", "b"), sortedPlaylist(songs, PlaylistSort.TITLE, descending = true).map { it.videoId })
}
