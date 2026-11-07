package com.music.violence.playback.crossfeed

import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossfeedTest {

    @Test
    fun testPresetsFromId() {
        assertEquals(CrossfeedPreset.BYPASS, CrossfeedPreset.fromId("bypass"))
        assertEquals(CrossfeedPreset.SUBTLE, CrossfeedPreset.fromId("subtle"))
        assertEquals(CrossfeedPreset.NATURAL, CrossfeedPreset.fromId("natural"))
        assertEquals(CrossfeedPreset.JAN_MEIER, CrossfeedPreset.fromId("jan_meier"))
        assertEquals(CrossfeedPreset.NATURAL, CrossfeedPreset.fromId("unknown"))
    }

    @Test
    fun testBypassIsBitExact() {
        val engine = CrossfeedEngine()
        engine.configure(channels = 2, sampleRate = 48000)
        engine.setParameters(enabled = false, preset = CrossfeedPreset.NATURAL)

        assertFalse(engine.isShaping())

        val input = floatArrayOf(0.5f, -0.25f, 0.75f, -0.8f)
        val copy = input.clone()

        engine.process(copy, copy.size)
        for (i in input.indices) {
            assertEquals(input[i], copy[i], "Bypass must leave samples bit-exact")
        }

        engine.setParameters(enabled = true, preset = CrossfeedPreset.BYPASS)
        assertFalse(engine.isShaping())
        engine.process(copy, copy.size)
        for (i in input.indices) {
            assertEquals(input[i], copy[i], "Bypass preset must leave samples bit-exact")
        }
    }

    @Test
    fun testCrossfeedCrossBleedAndDelay() {
        val engine = CrossfeedEngine()
        val sampleRate = 48000
        engine.configure(channels = 2, sampleRate = sampleRate)
        engine.setParameters(enabled = true, preset = CrossfeedPreset.NATURAL)

        assertTrue(engine.isShaping())

        // Feed hard-left signal (Left = 1.0, Right = 0.0) for 100 frames
        val frames = 100
        val buffer = FloatArray(frames * 2)
        for (i in 0 until frames) {
            buffer[i * 2] = 1.0f // Left
            buffer[i * 2 + 1] = 0.0f // Right
        }

        engine.process(buffer, buffer.size)

        // For Natural preset at 48kHz, delay is 265 us -> ~13 samples.
        // The first few samples of right channel should be 0.0f before delayed crossfeed arrives.
        assertEquals(0.0f, buffer[1], 1e-4f, "Right channel should be silent before delay arrives")

        // After 20 frames, right channel should receive acoustic crossfeed bleed
        val rightSampleAfterDelay = buffer[20 * 2 + 1]
        assertTrue(rightSampleAfterDelay > 0.05f, "Right channel should contain crossfeed bleed: $rightSampleAfterDelay")

        // Left channel direct signal should remain dominant
        val leftSample = buffer[20 * 2]
        assertTrue(leftSample > rightSampleAfterDelay, "Direct channel must be louder than crossfeed bleed")
    }

    @Test
    fun testSampleRateScaling44k48k96k() {
        val sampleRates = intArrayOf(44100, 48000, 96000)
        for (sr in sampleRates) {
            val engine = CrossfeedEngine()
            engine.configure(channels = 2, sampleRate = sr)
            engine.setParameters(enabled = true, preset = CrossfeedPreset.SUBTLE)

            val buffer = FloatArray(256) { if (it % 2 == 0) 0.5f else -0.5f }
            engine.process(buffer, buffer.size)

            assertTrue(buffer.all { !it.isNaN() && !it.isInfinite() }, "Output must be stable finite numbers at $sr Hz")
        }
    }

    @Test
    fun testResetClearsFilterState() {
        val engine = CrossfeedEngine()
        engine.configure(channels = 2, sampleRate = 44100)
        engine.setParameters(enabled = true, preset = CrossfeedPreset.NATURAL)

        val buffer = FloatArray(100) { 0.8f }
        engine.process(buffer, buffer.size)

        engine.reset()

        val zeroBuffer = FloatArray(20) { 0.0f }
        engine.process(zeroBuffer, zeroBuffer.size)

        // Processing zeros on a reset engine should output strictly zeros
        assertTrue(zeroBuffer.all { abs(it) < 1e-6f }, "Output should be zero after reset when feeding silence")
    }
}

