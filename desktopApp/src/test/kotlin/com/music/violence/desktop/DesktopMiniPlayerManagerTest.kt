package com.music.violence.desktop

import com.music.violence.data.lyrics.LyricLine
import com.music.violence.data.model.Song
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMiniPlayerManagerTest {

    @AfterTest
    fun tearDown() {
        DesktopMiniPlayerManager.exitMiniMode()
        DesktopMiniPlayerManager.updatePlayback(null, false, 0L, 0L)
        DesktopMiniPlayerManager.setLyrics(emptyList(), 0L)
    }

    @Test
    fun testMiniModeToggleAndState() {
        DesktopMiniPlayerManager.exitMiniMode()
        assertFalse(DesktopMiniPlayerManager.isMiniMode.value)

        DesktopMiniPlayerManager.toggle()
        assertTrue(DesktopMiniPlayerManager.isMiniMode.value)

        DesktopMiniPlayerManager.toggle()
        assertFalse(DesktopMiniPlayerManager.isMiniMode.value)

        DesktopMiniPlayerManager.enterMiniMode()
        assertTrue(DesktopMiniPlayerManager.isMiniMode.value)

        DesktopMiniPlayerManager.exitMiniMode()
        assertFalse(DesktopMiniPlayerManager.isMiniMode.value)
    }

    @Test
    fun testPlaybackStateSynchronization() {
        val testSong = Song(
            videoId = "test_vid_1",
            title = "Starboy",
            artist = "The Weeknd",
            thumbnailUrl = "https://example.com/art.jpg",
        )

        DesktopMiniPlayerManager.updatePlayback(testSong, playing = true, pos = 15_000L, dur = 230_000L)

        assertEquals("Starboy", DesktopMiniPlayerManager.currentSong.value?.title)
        assertEquals("The Weeknd", DesktopMiniPlayerManager.currentSong.value?.artist)
        assertTrue(DesktopMiniPlayerManager.isPlaying.value)
        assertEquals(15_000L, DesktopMiniPlayerManager.positionMs.value)
        assertEquals(230_000L, DesktopMiniPlayerManager.durationMs.value)
    }

    @Test
    fun testActiveLyricsResolution() {
        val lines = listOf(
            LyricLine(timeMs = 1_000L, text = "First line of the song"),
            LyricLine(timeMs = 5_000L, text = "Second line comes here"),
            LyricLine(timeMs = 10_000L, text = "Third line is singing"),
        )

        DesktopMiniPlayerManager.setLyrics(lines, currentPosMs = 0L)
        assertNull(DesktopMiniPlayerManager.activeLyricText.value)

        DesktopMiniPlayerManager.updatePlayback(null, true, pos = 2_000L, dur = 30_000L)
        assertEquals("First line of the song", DesktopMiniPlayerManager.activeLyricText.value)

        DesktopMiniPlayerManager.updatePlayback(null, true, pos = 7_500L, dur = 30_000L)
        assertEquals("Second line comes here", DesktopMiniPlayerManager.activeLyricText.value)

        DesktopMiniPlayerManager.updatePlayback(null, true, pos = 15_000L, dur = 30_000L)
        assertEquals("Third line is singing", DesktopMiniPlayerManager.activeLyricText.value)
    }

    @Test
    fun testActionCallbacksTriggerProperly() {
        var playPauseCalled = false
        var nextCalled = false
        var prevCalled = false
        var seekPosition = -1L

        DesktopMiniPlayerManager.onPlayPause = { playPauseCalled = true }
        DesktopMiniPlayerManager.onNext = { nextCalled = true }
        DesktopMiniPlayerManager.onPrevious = { prevCalled = true }
        DesktopMiniPlayerManager.onSeek = { seekPosition = it }

        DesktopMiniPlayerManager.onPlayPause()
        DesktopMiniPlayerManager.onNext()
        DesktopMiniPlayerManager.onPrevious()
        DesktopMiniPlayerManager.onSeek(42_000L)

        assertTrue(playPauseCalled)
        assertTrue(nextCalled)
        assertTrue(prevCalled)
        assertEquals(42_000L, seekPosition)
    }
}

