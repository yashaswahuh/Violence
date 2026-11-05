package com.music.violence.desktop

import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPerformanceSettingsTest {

    @Before
    fun setUp() {
        // Reset toggles to defaults
        DesktopAppearanceSettings.setSaveSyncedLyricsWithDownloads(true)
        DesktopAppearanceSettings.setPauseBackgroundVisualsWhenHidden(false)
        DesktopAppearanceSettings.setConstrainImageCacheMemory(false)
        DesktopWindowVisibility.setOccludedOrMinimized(false)
    }

    @After
    fun tearDown() {
        DesktopAppearanceSettings.setSaveSyncedLyricsWithDownloads(true)
        DesktopAppearanceSettings.setPauseBackgroundVisualsWhenHidden(false)
        DesktopAppearanceSettings.setConstrainImageCacheMemory(false)
        DesktopWindowVisibility.setOccludedOrMinimized(false)
    }

    @Test
    fun testSaveSyncedLyricsToggle() {
        assertTrue(DesktopAppearanceSettings.saveSyncedLyricsWithDownloads.value)
        DesktopAppearanceSettings.setSaveSyncedLyricsWithDownloads(false)
        assertFalse(DesktopAppearanceSettings.saveSyncedLyricsWithDownloads.value)
        DesktopAppearanceSettings.setSaveSyncedLyricsWithDownloads(true)
        assertTrue(DesktopAppearanceSettings.saveSyncedLyricsWithDownloads.value)
    }

    @Test
    fun testPauseBackgroundVisualsWhenHiddenToggle() {
        assertFalse(DesktopAppearanceSettings.pauseBackgroundVisualsWhenHidden.value)
        DesktopAppearanceSettings.setPauseBackgroundVisualsWhenHidden(true)
        assertTrue(DesktopAppearanceSettings.pauseBackgroundVisualsWhenHidden.value)
        DesktopAppearanceSettings.setPauseBackgroundVisualsWhenHidden(false)
        assertFalse(DesktopAppearanceSettings.pauseBackgroundVisualsWhenHidden.value)
    }

    @Test
    fun testConstrainImageCacheMemoryToggleAndBudget() {
        assertFalse(DesktopAppearanceSettings.constrainImageCacheMemory.value)
        assertEquals(DesktopArtworkCache.defaultBudgetBytes, DesktopArtworkCache.budgetBytes)

        // Enable memory constraint
        DesktopAppearanceSettings.setConstrainImageCacheMemory(true)
        assertTrue(DesktopAppearanceSettings.constrainImageCacheMemory.value)
        assertEquals(16L * 1024 * 1024, DesktopArtworkCache.budgetBytes)

        // Disable memory constraint
        DesktopAppearanceSettings.setConstrainImageCacheMemory(false)
        assertEquals(DesktopArtworkCache.defaultBudgetBytes, DesktopArtworkCache.budgetBytes)
    }

    @Test
    fun testWindowOcclusionState() {
        assertFalse(DesktopWindowVisibility.isOccludedOrMinimized.value)
        DesktopWindowVisibility.setOccludedOrMinimized(true)
        assertTrue(DesktopWindowVisibility.isOccludedOrMinimized.value)
        DesktopWindowVisibility.setOccludedOrMinimized(false)
        assertFalse(DesktopWindowVisibility.isOccludedOrMinimized.value)
    }
}

