package com.music.violence.ui.violencestyle

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BackgroundGeometryTest {
    private val w = 390f
    private val h = 844f
    private val buf = FloatArray(BackgroundGeometry.MAX_BLOBS * BackgroundGeometry.STRIDE)
    private val styles = listOf(StyleBackground.AURORA, StyleBackground.CORNER_GLOW, StyleBackground.HAZE)

    @Test fun blobCountsMatchTheSpec() {
        assertEquals(3, BackgroundGeometry.blobCount(StyleBackground.AURORA))
        assertEquals(2, BackgroundGeometry.blobCount(StyleBackground.CORNER_GLOW))
        assertEquals(2, BackgroundGeometry.blobCount(StyleBackground.HAZE))
        assertEquals(0, BackgroundGeometry.blobCount(StyleBackground.OFF))
        assertTrue(BackgroundGeometry.blobCount(StyleBackground.AURORA) <= BackgroundGeometry.MAX_BLOBS)
    }

    @Test fun offDrawsNothing() {
        assertEquals(0, BackgroundGeometry.fill(StyleBackground.OFF, w, h, 0.0, StyleMotion.NORMAL, StyleIntensity.HIGH, null, buf))
    }

    @Test fun motionOffIsFullyStatic() {
        for (s in styles) {
            val a = FloatArray(buf.size); val b = FloatArray(buf.size)
            BackgroundGeometry.fill(s, w, h, 0.0, StyleMotion.OFF, StyleIntensity.MEDIUM, null, a)
            BackgroundGeometry.fill(s, w, h, 12345.6, StyleMotion.OFF, StyleIntensity.MEDIUM, null, b)
            assertTrue(s.name, a.contentEquals(b))
        }
    }

    @Test fun driftNeverExceedsTheAmplitudeAndNormalMovesMoreThanSubtle() {
        for (s in styles) {
            val rest = FloatArray(buf.size)
            BackgroundGeometry.fill(s, w, h, 0.0, StyleMotion.OFF, StyleIntensity.MEDIUM, null, rest)
            for (motion in listOf(StyleMotion.SUBTLE, StyleMotion.NORMAL)) {
                val amp = BackgroundGeometry.amplitude(motion) * w
                var maxMove = 0f
                var t = 0.0
                while (t < 400.0) {
                    val n = BackgroundGeometry.fill(s, w, h, t, motion, StyleIntensity.MEDIUM, null, buf)
                    for (i in 0 until n) {
                        val o = i * BackgroundGeometry.STRIDE
                        maxMove = maxOf(maxMove, abs(buf[o] - rest[o]), abs(buf[o + 1] - rest[o + 1]))
                    }
                    t += 0.5
                }
                // Each axis moves by at most 2 x the amplitude from the rest pose (the rest pose has its own phase offset).
                assertTrue("${s.name} $motion $maxMove", maxMove <= 2f * amp + 0.01f)
            }
        }
        assertTrue(BackgroundGeometry.amplitude(StyleMotion.NORMAL) > BackgroundGeometry.amplitude(StyleMotion.SUBTLE))
    }

    @Test fun blobsStayNearTheScreenForEveryTimeSample() {
        for (s in styles) for (m in StyleMotion.entries) for (i in StyleIntensity.entries) {
            var t = 0.0
            while (t < 600.0) {
                val n = BackgroundGeometry.fill(s, w, h, t, m, i, null, buf)
                for (k in 0 until n) {
                    val o = k * BackgroundGeometry.STRIDE
                    assertTrue(buf[o] in -0.3f * w..1.3f * w)
                    assertTrue(buf[o + 1] in -0.3f * h..1.3f * h)
                    assertTrue(buf[o + 2] > 0f)
                }
                t += 7.3
            }
        }
    }

    @Test fun alphaIsPeakTimesIntensityTimesScale() {
        BackgroundGeometry.fill(StyleBackground.AURORA, w, h, 0.0, StyleMotion.OFF, StyleIntensity.HIGH, null, buf)
        assertEquals(0.85f, buf[3], 1e-5f)
        BackgroundGeometry.fill(StyleBackground.AURORA, w, h, 0.0, StyleMotion.OFF, StyleIntensity.LOW, floatArrayOf(0.5f, 1f, 1f), buf)
        assertEquals(0.38f * 0.5f, buf[3], 1e-5f)
    }

    @Test fun intensityScalesTheRadius() {
        val low = FloatArray(buf.size); val high = FloatArray(buf.size)
        BackgroundGeometry.fill(StyleBackground.CORNER_GLOW, w, h, 0.0, StyleMotion.OFF, StyleIntensity.LOW, null, low)
        BackgroundGeometry.fill(StyleBackground.CORNER_GLOW, w, h, 0.0, StyleMotion.OFF, StyleIntensity.HIGH, null, high)
        assertEquals(0.8f, low[2] / (0.95f * w), 1e-4f)
        assertEquals(1.15f, high[2] / (0.95f * w), 1e-4f)
    }

    @Test fun highGlowDriftsFurtherAndTheBrightnessBreathNeverExceedsTheGuardedAlpha() {
        assertEquals(1f, BackgroundGeometry.driftBoost(StyleIntensity.MEDIUM), 0f)
        assertTrue(BackgroundGeometry.driftBoost(StyleIntensity.HIGH) > 1.4f)
        var min = 10f
        var max = 0f
        var t = 0.0
        while (t < 300.0) {
            BackgroundGeometry.fill(StyleBackground.AURORA, 390f, 844f, t, StyleMotion.NORMAL, StyleIntensity.HIGH, null, buf)
            min = minOf(min, buf[3]); max = maxOf(max, buf[3])
            t += 0.37
        }
        assertTrue("max $max", max <= StyleIntensity.HIGH.peakAlpha + 1e-5f)
        assertTrue("min $min", min < StyleIntensity.HIGH.peakAlpha * 0.85f) // visibly dips
    }

    @Test fun subtleStretchesThePeriods() {
        assertEquals(1f, BackgroundGeometry.periodScale(StyleMotion.SUBTLE), 0f)
        assertEquals(0.6f, BackgroundGeometry.periodScale(StyleMotion.NORMAL), 0f)
        // Normal must be visibly livelier than Subtle: a larger drift in a shorter loop.
        assertTrue(BackgroundGeometry.amplitude(StyleMotion.NORMAL) >= 2f * BackgroundGeometry.amplitude(StyleMotion.SUBTLE))
    }

    @Test fun falloffIsMonotonicAndHitsTheStops() {
        assertEquals(1f, BackgroundGeometry.falloff(0f), 1e-6f)
        assertEquals(0.55f, BackgroundGeometry.falloff(0.35f), 1e-6f)
        assertEquals(0.18f, BackgroundGeometry.falloff(0.7f), 1e-6f)
        assertEquals(0f, BackgroundGeometry.falloff(1f), 1e-6f)
        var prev = 2f
        var d = 0f
        while (d <= 1.2f) {
            val f = BackgroundGeometry.falloff(d)
            assertTrue(f <= prev + 1e-6f); prev = f; d += 0.01f
        }
    }

    @Test fun farFromEveryBlobTheColourIsTheBase() {
        val glow = 0xFF3F7BFF.toInt()
        val base = BackgroundGeometry.baseColor(glow)
        val n = BackgroundGeometry.fill(StyleBackground.CORNER_GLOW, w, h, 0.0, StyleMotion.OFF, StyleIntensity.HIGH, null, buf)
        assertEquals(base, BackgroundGeometry.compositeAt(w, h, w, h, buf, n, glow, base, null))
    }

    @Test fun baseColorIsBlackTintedFourPercent() {
        val c = BackgroundGeometry.baseColor(0xFF3F7BFF.toInt())
        assertTrue(AccentMath.red(c) <= 3 && AccentMath.green(c) <= 5 && AccentMath.blue(c) <= 11)
    }

    @Test fun vignettesMatchTheSpec() {
        assertEquals(0.60f, BackgroundGeometry.vignette(StyleBackground.AURORA)!!.startFraction, 0f)
        assertEquals(0.70f, BackgroundGeometry.vignette(StyleBackground.HAZE)!!.startFraction, 0f)
        assertEquals(null, BackgroundGeometry.vignette(StyleBackground.CORNER_GLOW))
    }
}

class GlowGuardTest {
    private val styles = listOf(StyleBackground.AURORA, StyleBackground.CORNER_GLOW, StyleBackground.HAZE)
    private val extremes = listOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt(), 0xFFF8F8FF.toInt(), 0xFF050508.toInt(), 0xFF808080.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt())
    private val sources = AccentPreset.entries.map { it.argb } + extremes

    @Test fun glowNeverReducesTextContrastBelowTheLimitsForAnyColourStyleIntensityOrMotion() {
        for (source in sources) {
            val glow = AccentTokens.from(source).glowColor
            for (style in styles) for (intensity in StyleIntensity.entries) for (motion in StyleMotion.entries) {
                val scales = GlowGuard.scales(style, glow, intensity, motion)
                val upper = GlowGuard.worstContrast(style, glow, intensity, scales, AccentMath.WHITE, zoneUpper = true)
                val lowerSecondary = GlowGuard.worstContrast(style, glow, intensity, scales, GlowGuard.SECONDARY_TEXT, zoneUpper = false)
                val lowerWhite = GlowGuard.worstContrast(style, glow, intensity, scales, AccentMath.WHITE, zoneUpper = false)
                val tag = "${Integer.toHexString(source)} $style $intensity $motion"
                assertTrue("$tag upper white $upper", upper >= GlowGuard.LARGE_TEXT_CONTRAST)
                assertTrue("$tag lower secondary $lowerSecondary", lowerSecondary >= GlowGuard.BODY_TEXT_CONTRAST)
                assertTrue("$tag lower white $lowerWhite", lowerWhite >= GlowGuard.BODY_TEXT_CONTRAST)
            }
        }
    }

    @Test fun scalesStayInRangeAndNeverExceedOne() {
        for (source in sources) for (style in styles) for (intensity in StyleIntensity.entries) {
            val scales = GlowGuard.scales(style, AccentTokens.from(source).glowColor, intensity, StyleMotion.NORMAL)
            for (s in scales) assertTrue("$s", s in 0.05f..1f)
        }
    }

    @Test fun higherIntensityIsNeverBrighterThanTheGuardAllows() {
        val glow = AccentTokens.from(AccentPreset.BLUE.argb).glowColor
        val high = GlowGuard.scales(StyleBackground.AURORA, glow, StyleIntensity.HIGH, StyleMotion.NORMAL)
        val low = GlowGuard.scales(StyleBackground.AURORA, glow, StyleIntensity.LOW, StyleMotion.NORMAL)
        // High is the one that has to be held back the most.
        assertTrue(high.sum() <= low.sum() + 1e-4f)
    }

    @Test fun offHasNothingToScale() {
        val s = GlowGuard.scales(StyleBackground.OFF, 0xFF3F7BFF.toInt(), StyleIntensity.HIGH, StyleMotion.NORMAL)
        assertEquals(1, s.size); assertEquals(1f, s[0], 0f)
    }

    @Test fun maxBackgroundLuminanceMatchesTheContrastFormula() {
        // White text (luminance 1) at 4.5:1 allows background luminance 0.1833.
        assertEquals(0.1833, GlowGuard.maxBackgroundLuminance(1.0, 4.5), 0.001)
        // #8E8E93 at 4.5:1 allows about 0.021.
        val l = GlowGuard.maxBackgroundLuminance(AccentMath.relativeLuminance(GlowGuard.SECONDARY_TEXT), 4.5)
        assertEquals(0.021, l, 0.004)
    }

    @Test fun worstCaseAccentsOnAWhiteishArtworkColourAreStillSafe() {
        // A near-white artwork colour becomes a neutral light glow: the guard must hold it back.
        val glow = AccentTokens.from(0xFFF8F8FF.toInt()).glowColor
        val scales = GlowGuard.scales(StyleBackground.HAZE, glow, StyleIntensity.HIGH, StyleMotion.NORMAL)
        assertTrue(GlowGuard.worstContrast(StyleBackground.HAZE, glow, StyleIntensity.HIGH, scales, AccentMath.WHITE, true) >= 3.0)
        assertTrue(GlowGuard.worstContrast(StyleBackground.HAZE, glow, StyleIntensity.HIGH, scales, GlowGuard.SECONDARY_TEXT, false) >= 4.5)
    }
}

class StyleBackgroundRulesTest {
    private val on = StyleState.DISABLED.copy(enabled = true)

    @Test fun backgroundShowsOnlyWhenOnAndNotOff() {
        assertFalse(StyleState.DISABLED.showsBackground())
        assertTrue(on.showsBackground())
        assertFalse(on.copy(background = StyleBackground.OFF).showsBackground())
        assertFalse(StyleState.DISABLED.copy(background = StyleBackground.HAZE).showsBackground())
    }

    @Test fun offPathKeepsTheOriginalBackgroundModifierExactly() {
        val c = Color(0xFF000000)
        assertEquals(Modifier.background(c), Modifier.violenceBackground(StyleState.DISABLED, c))
        assertEquals(Modifier.background(c), Modifier.violenceBackground(on.copy(background = StyleBackground.OFF), c))
    }

    @Test fun onPathDropsTheOpaqueFill() {
        val m = Modifier
        assertSame(m, m.violenceBackground(on, Color.Black))
    }

    @Test fun clockStopsWhenTheAppIsHiddenThePlayerCoversTheScreenOrMotionIsReduced() {
        assertTrue(StyleCapabilityRules.backgroundClockMayRun(on, true, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on, false, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on, true, true))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on.copy(reduceMotion = true), true, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on.copy(capability = StyleCapability.LITE), true, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on.copy(motion = StyleMotion.OFF), true, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(StyleState.DISABLED, true, false))
    }

    @Test fun throttleStepsDownAndNeverBackUp() {
        val i0 = StyleFrameThrottle.intervalNanos(0)
        assertEquals(0, StyleFrameThrottle.nextLevel(0, i0))
        assertEquals(0, StyleFrameThrottle.nextLevel(0, (i0 * 1.3).toLong()))
        assertEquals(1, StyleFrameThrottle.nextLevel(0, (i0 * 1.5).toLong()))
        assertEquals(2, StyleFrameThrottle.nextLevel(1, StyleFrameThrottle.intervalNanos(1) * 2))
        assertEquals(StyleFrameThrottle.STATIC_LEVEL, StyleFrameThrottle.nextLevel(2, StyleFrameThrottle.intervalNanos(2) * 2))
        assertEquals(StyleFrameThrottle.STATIC_LEVEL, StyleFrameThrottle.nextLevel(StyleFrameThrottle.STATIC_LEVEL, 1L))
        assertEquals(0L, StyleFrameThrottle.intervalNanos(StyleFrameThrottle.STATIC_LEVEL))
    }
}
