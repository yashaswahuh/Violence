package com.music.violence.playback

import com.music.violence.data.model.Song
import com.music.violence.share.UniversalLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which search result a tapped link plays: the song it names, never just the first hit. */
class UniversalLinksPickTest {

    private fun song(id: String, title: String, artist: String, duration: String? = null, album: String? = null) =
        Song(videoId = id, title = title, artist = artist, thumbnailUrl = null, durationText = duration, albumName = album)

    @Test fun theOriginalIsPlayedEvenWhenARemixIsFirst() {
        val results = listOf(
            song("remix", "Harder Better Faster Stronger (Remix)", "Daft Punk", "4:10"),
            song("live", "Harder Better Faster Stronger (Live)", "Daft Punk", "5:30"),
            song("orig", "Harder Better Faster Stronger", "Daft Punk", "3:45", "Discovery"),
        )
        val picked = UniversalLinks.pickSong(results, "Harder Better Faster Stronger", "Daft Punk", "Discovery", 224_693L)
        assertEquals("orig", picked?.videoId)
    }

    @Test fun nothingIsPlayedWhenOnlyOtherVersionsAreFound() {
        val results = listOf(
            song("remix", "Harder Better Faster Stronger (Remix)", "Someone Else", "4:10"),
            song("cover", "Harder Better Faster Stronger (Cover)", "A Cover Band", "3:40"),
        )
        assertNull(UniversalLinks.pickSong(results, "Harder Better Faster Stronger", "Daft Punk", "Discovery", 224_693L))
    }

    @Test fun aDifferentSongByTheSameArtistIsNotPlayed() {
        val results = listOf(song("other", "One More Time", "Daft Punk", "5:20"))
        assertNull(UniversalLinks.pickSong(results, "Harder Better Faster Stronger", "Daft Punk", "Discovery", 224_693L))
    }

    @Test fun noResultsMeansNothingToPlay() {
        assertNull(UniversalLinks.pickSong(emptyList(), "Anything", "Anyone", null, null))
    }
}
