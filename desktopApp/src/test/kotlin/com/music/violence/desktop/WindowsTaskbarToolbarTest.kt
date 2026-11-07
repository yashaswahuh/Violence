package com.music.violence.desktop

import com.music.violence.desktop.platform.WindowsTaskbarToolbar
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class WindowsTaskbarToolbarTest {

    @AfterTest
    fun tearDown() {
        WindowsTaskbarToolbar.stop()
    }

    @Test
    fun updateStateDoesNotThrowEvenWhenStopped() {
        // Safe when not started
        WindowsTaskbarToolbar.update(isPlaying = true, hasTrack = true)
        WindowsTaskbarToolbar.update(isPlaying = false, hasTrack = false)
        WindowsTaskbarToolbar.stop()
        assertTrue(true)
    }

    @Test
    fun startAndStopCycleGracefully() {
        val actions = mutableListOf<String>()
        val controller = object : WindowsTaskbarToolbar.Controller {
            override fun onPlay() { actions += "play" }
            override fun onPause() { actions += "pause" }
            override fun onNext() { actions += "next" }
            override fun onPrevious() { actions += "previous" }
        }

        if (!DesktopPlatform.isWindows || java.awt.GraphicsEnvironment.isHeadless()) {
            assertTrue(true)
            return
        }
        val dummyFrame = java.awt.Frame()
        try {
            WindowsTaskbarToolbar.start(dummyFrame, controller)
            WindowsTaskbarToolbar.update(isPlaying = true, hasTrack = true)
            WindowsTaskbarToolbar.update(isPlaying = false, hasTrack = true)
        } finally {
            WindowsTaskbarToolbar.stop()
            dummyFrame.dispose()
        }
        assertTrue(true)
    }
}
