package com.music.violence.playback.autoeq

import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutoEqTest {

    @Test
    fun testAutoEqParser() {
        val sampleText = """
            Preamp: -6.3 dB
            Filter 1: ON LSC Fc 105 Hz Gain 6.3 dB Q 0.70
            Filter 2: ON PK Fc 169 Hz Gain -2.1 dB Q 0.77
            Filter 3: ON PK Fc 5646 Hz Gain -5.5 dB Q 4.03
            Filter 4: OFF PK Fc 8000 Hz Gain 2.0 dB Q 2.00
            Filter 5: ON HSC Fc 10000 Hz Gain -7.1 dB Q 0.70
        """.trimIndent()

        val profile = AutoEqParser.parse(sampleText, "Test Headphone", "oratory1990")
        assertEquals("Test Headphone", profile.model)
        assertEquals("oratory1990", profile.source)
        assertEquals(-6.3f, profile.preampDb)
        // Filter 4 was OFF, so only 4 active filters
        assertEquals(4, profile.filters.size)

        assertEquals(AutoEqFilterType.LOW_SHELF, profile.filters[0].type)
        assertEquals(105f, profile.filters[0].frequencyHz)
        assertEquals(6.3f, profile.filters[0].gainDb)
        assertEquals(0.70f, profile.filters[0].q)

        assertEquals(AutoEqFilterType.PEAKING, profile.filters[1].type)
        assertEquals(169f, profile.filters[1].frequencyHz)
        assertEquals(-2.1f, profile.filters[1].gainDb)

        assertEquals(AutoEqFilterType.HIGH_SHELF, profile.filters[3].type)
        assertEquals(10000f, profile.filters[3].frequencyHz)
        assertEquals(-7.1f, profile.filters[3].gainDb)
    }

    @Test
    fun testAutoEqDatabaseLoadingAndSearch() {
        AutoEqDatabase.ensureLoaded()
        assertTrue(AutoEqDatabase.isLoaded, "Database should load successfully")
        assertTrue(AutoEqDatabase.totalCount >= 8000, "Should contain over 8,000 entries (actual: ${AutoEqDatabase.totalCount})")

        // Search for Sennheiser HD 600
        val hd600Results = AutoEqDatabase.search("HD 600")
        assertTrue(hd600Results.isNotEmpty(), "Should find HD 600 results")
        assertTrue(hd600Results.any { it.model.contains("HD 600", ignoreCase = true) })

        val hd600Profile = AutoEqDatabase.getProfile(hd600Results.first().id)
        assertNotNull(hd600Profile)
        assertTrue(hd600Profile.filters.isNotEmpty())

        // Search for Sony WH-1000XM4 / XM5
        val sonyResults = AutoEqDatabase.search("WH-1000XM4")
        assertTrue(sonyResults.isNotEmpty(), "Should find Sony WH-1000XM4")

        // Search for AirPods Pro
        val airPodsResults = AutoEqDatabase.search("AirPods Pro")
        assertTrue(airPodsResults.isNotEmpty(), "Should find AirPods Pro")

        // Search for Tangzu Waner (without apostrophe)
        val wanerResults = AutoEqDatabase.search("Tangzu Waner")
        assertTrue(wanerResults.isNotEmpty(), "Should find Tangzu Wan'er without apostrophe")
        assertTrue(wanerResults.any { it.model.contains("Wan'er", ignoreCase = true) })

        // Search for hd600 (without space)
        val hd600Compact = AutoEqDatabase.search("hd600")
        assertTrue(hd600Compact.isNotEmpty(), "Should find HD 600 from compact query")

        // Search for xm4 (short compact query)
        val xm4Results = AutoEqDatabase.search("xm4")
        assertTrue(xm4Results.isNotEmpty(), "Should find 1000XM4 from xm4 query")

        // Test popular summaries
        val popular = AutoEqDatabase.popularSummaries()
        assertTrue(popular.isNotEmpty())
        assertTrue(popular.size >= 10, "Should return curated popular headphones")
    }

    @Test
    fun testAutoEqEngineBypassIsIdentity() {
        val engine = AutoEqEngine()
        engine.configure(channels = 2, sampleRate = 48000)
        engine.setProfile(enabled = false, profile = null)

        val input = FloatArray(512) { i -> sin(2.0 * Math.PI * 440.0 * i / 48000.0).toFloat() }
        val copy = input.copyOf()

        engine.process(input, input.size)
        // With engine disabled, output should be byte-identical to input
        for (i in input.indices) {
            assertEquals(copy[i], input[i], 0.000001f, "Bypass must be sample-identical at index $i")
        }
        assertFalse(engine.isShaping())
    }

    @Test
    fun testAutoEqEngineFilterResponse() {
        val engine = AutoEqEngine()
        val sampleRate = 48000
        engine.configure(channels = 1, sampleRate = sampleRate)

        // 1 kHz boost of +6 dB with 0 dB preamp
        val profile = AutoEqProfile(
            model = "1kHz Boost Test",
            source = "test",
            category = "test",
            preampDb = 0f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAKING, 1000f, 6.0f, 1.41f),
            ),
        )

        engine.setProfile(enabled = true, profile = profile)
        assertTrue(engine.isShaping())

        // Generate 1 kHz sine wave
        val frames = 4800
        val buffer = FloatArray(frames) { i -> sin(2.0 * Math.PI * 1000.0 * i / sampleRate).toFloat() }

        engine.process(buffer, buffer.size)

        // Check steady state amplitude (last 1000 samples)
        var peak = 0f
        for (i in (frames - 1000) until frames) {
            val a = abs(buffer[i])
            if (a > peak) peak = a
        }

        // Input amplitude was 1.0. A +6 dB boost corresponds to factor ~10^(6/20) = 1.995 (~2.0)
        assertTrue(peak > 1.8f && peak < 2.2f, "Peak amplitude at 1kHz should be boosted to ~2.0, actual: $peak")
    }
}

