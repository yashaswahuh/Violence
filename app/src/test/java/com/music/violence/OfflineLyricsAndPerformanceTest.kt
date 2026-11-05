package com.music.violence

import com.music.violence.data.settings.AppSettings
import com.music.violence.ui.performance.BackgroundVisualsController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineLyricsAndPerformanceTest {

    @Before
    fun setUp() {
        AppSettings.saveSyncedLyricsWithDownloads.value = true
        AppSettings.pauseBackgroundVisualsWhenHidden.value = false
        AppSettings.constrainImageCacheMemory.value = false
        BackgroundVisualsController.onActivityStarted()
        BackgroundVisualsController.updateScreenState(true)
    }

    @After
    fun tearDown() {
        AppSettings.saveSyncedLyricsWithDownloads.value = true
        AppSettings.pauseBackgroundVisualsWhenHidden.value = false
        AppSettings.constrainImageCacheMemory.value = false
        BackgroundVisualsController.onActivityStarted()
        BackgroundVisualsController.updateScreenState(true)
    }

    @Test
    fun testAppSettingsToggles() {
        assertTrue(AppSettings.saveSyncedLyricsWithDownloads.value)
        AppSettings.saveSyncedLyricsWithDownloads.value = false
        assertFalse(AppSettings.saveSyncedLyricsWithDownloads.value)

        assertFalse(AppSettings.pauseBackgroundVisualsWhenHidden.value)
        AppSettings.pauseBackgroundVisualsWhenHidden.value = true
        assertTrue(AppSettings.pauseBackgroundVisualsWhenHidden.value)

        assertFalse(AppSettings.constrainImageCacheMemory.value)
        AppSettings.constrainImageCacheMemory.value = true
        assertTrue(AppSettings.constrainImageCacheMemory.value)
    }

    @Test
    fun testBackgroundVisualsSuspensionLifecycle() {
        // Toggle is OFF: screen off or activity stopped should NOT suspend
        AppSettings.pauseBackgroundVisualsWhenHidden.value = false
        BackgroundVisualsController.onActivityStopped()
        assertFalse(BackgroundVisualsController.isSuspended.value)

        BackgroundVisualsController.updateScreenState(false)
        assertFalse(BackgroundVisualsController.isSuspended.value)

        // Toggle is ON: should suspend when screen off or stopped
        AppSettings.pauseBackgroundVisualsWhenHidden.value = true
        BackgroundVisualsController.evaluate()
        assertTrue(BackgroundVisualsController.isSuspended.value)

        // Activity comes back to foreground and screen is on
        BackgroundVisualsController.updateScreenState(true)
        BackgroundVisualsController.onActivityStarted()
        assertFalse(BackgroundVisualsController.isSuspended.value)

        // Screen turns off while active
        BackgroundVisualsController.updateScreenState(false)
        assertTrue(BackgroundVisualsController.isSuspended.value)
    }
}

