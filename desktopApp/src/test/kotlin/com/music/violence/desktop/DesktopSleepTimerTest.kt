package com.music.violence.desktop

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The sleep timer's two modes and the boundary between them.
 *
 * A timer that quietly does not run is indistinguishable from one that has not
 * fired yet, so its state has to be checkable rather than watched.
 */
class DesktopSleepTimerTest {

    @AfterTest
    fun tearDown() = DesktopSleepTimer.cancel()

    @Test
    fun nothingIsSetUntilSomethingStartsIt() {
        DesktopSleepTimer.cancel()
        assertNull(DesktopSleepTimer.minutes.value)
        assertNull(DesktopSleepTimer.remainingMs())
        assertFalse(DesktopSleepTimer.afterTrack.value)
        assertFalse(DesktopSleepTimer.isExpired())
    }

    @Test
    fun aDurationCountsDownFromWhatWasAsked() {
        DesktopSleepTimer.start(30)
        assertEquals(30, DesktopSleepTimer.minutes.value)
        val remaining = DesktopSleepTimer.remainingMs()!!
        // Within a second of the full half hour, and never over it.
        assertTrue(remaining in 29 * 60_000L..30 * 60_000L, "remaining was $remaining")
        assertFalse(DesktopSleepTimer.isExpired())
    }

    @Test
    fun theTwoModesDisplaceEachOther() {
        // Both at once has no meaning: one stops at a time, the other at the end
        // of the track, and the UI shows a single line either way.
        DesktopSleepTimer.start(15)
        DesktopSleepTimer.startAfterTrack()
        assertTrue(DesktopSleepTimer.afterTrack.value)
        assertNull(DesktopSleepTimer.minutes.value)
        assertNull(DesktopSleepTimer.remainingMs())

        DesktopSleepTimer.start(15)
        assertFalse(DesktopSleepTimer.afterTrack.value)
        assertEquals(15, DesktopSleepTimer.minutes.value)
    }

    @Test
    fun cancellingClearsBoth() {
        DesktopSleepTimer.startAfterTrack()
        DesktopSleepTimer.cancel()
        assertFalse(DesktopSleepTimer.afterTrack.value)
        assertNull(DesktopSleepTimer.minutes.value)
    }

    @Test
    fun aTimerOfNoLengthIsRefusedRatherThanFiringAtOnce() {
        assertFailsWith<IllegalArgumentException> { DesktopSleepTimer.start(0) }
        assertFailsWith<IllegalArgumentException> { DesktopSleepTimer.start(-5) }
    }

    @Test
    fun everyPresetIsAUsableDuration() {
        assertEquals(listOf(15, 30, 45, 60), DesktopSleepTimer.presets)
        DesktopSleepTimer.presets.forEach { DesktopSleepTimer.start(it) }
    }

    @Test
    fun fadeGainCurvesLogarithmicallyToSilence() {
        assertEquals(1.0f, DesktopSleepTimer.calculateFadeGain(0.0f))
        assertEquals(0.0f, DesktopSleepTimer.calculateFadeGain(1.0f))
        val halfFade = DesktopSleepTimer.calculateFadeGain(0.5f)
        assertTrue(halfFade in 0.15f..0.20f, "Halfway gain should be approx (0.5)^2.5 ~ 0.1768, got $halfFade")
        // Clamping check
        assertEquals(1.0f, DesktopSleepTimer.calculateFadeGain(-0.5f))
        assertEquals(0.0f, DesktopSleepTimer.calculateFadeGain(1.5f))
    }

    @Test
    fun optionsCanBeSet() {
        DesktopSleepTimer.setFadeToSilence(false)
        assertFalse(DesktopSleepTimer.fadeToSilence.value)
        DesktopSleepTimer.setFadeToSilence(true)
        assertTrue(DesktopSleepTimer.fadeToSilence.value)

        DesktopSleepTimer.setFinishCurrentTrack(true)
        assertTrue(DesktopSleepTimer.finishCurrentTrack.value)
        DesktopSleepTimer.setFinishCurrentTrack(false)
        assertFalse(DesktopSleepTimer.finishCurrentTrack.value)
    }
}
