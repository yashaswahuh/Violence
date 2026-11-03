package com.music.bitchord.ui.violencestyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AccentMathTest {

    private val pureBlue = 0xFF0000FF.toInt()
    private val pureRed = 0xFFFF0000.toInt()
    private val nearWhite = 0xFFF8F8FF.toInt()
    private val nearBlack = 0xFF050508.toInt()
    private val gray = 0xFF808080.toInt()

    @Test fun contrastOfBlackOnWhiteIs21() {
        assertEquals(21.0, AccentMath.contrastRatio(AccentMath.WHITE, AccentMath.BLACK), 0.01)
    }

    @Test fun contrastOfIdenticalColoursIs1() {
        assertEquals(1.0, AccentMath.contrastRatio(pureRed, pureRed), 0.0001)
    }

    @Test fun contrastIsSymmetric() {
        assertEquals(
            AccentMath.contrastRatio(pureBlue, AccentMath.WHITE),
            AccentMath.contrastRatio(AccentMath.WHITE, pureBlue),
            0.0001,
        )
    }

    @Test fun luminanceOfPrimariesMatchesWcag() {
        assertEquals(0.2126, AccentMath.relativeLuminance(pureRed), 0.001)
        assertEquals(0.0722, AccentMath.relativeLuminance(pureBlue), 0.001)
    }

    @Test fun hslRoundTrips() {
        for (c in listOf(0xFF3F7BFF.toInt(), 0xFFE5384F.toInt(), 0xFF2FBF71.toInt(), 0xFFF2C230.toInt())) {
            val hsl = AccentMath.toHsl(c)
            val back = AccentMath.fromHsl(hsl.h, hsl.s, hsl.l)
            assertTrue(abs(AccentMath.red(c) - AccentMath.red(back)) <= 1)
            assertTrue(abs(AccentMath.green(c) - AccentMath.green(back)) <= 1)
            assertTrue(abs(AccentMath.blue(c) - AccentMath.blue(back)) <= 1)
        }
    }

    @Test fun clampKeepsChromaticColoursInRange() {
        for (c in listOf(pureBlue, pureRed, 0xFF0A0AFF.toInt(), 0xFF332211.toInt(), 0xFFFFEECC.toInt())) {
            if (AccentMath.isNeutral(c)) continue
            val hsl = AccentMath.toHsl(AccentMath.clampAccent(c))
            assertTrue("s=${hsl.s}", hsl.s in 0.54f..0.91f)
            assertTrue("l=${hsl.l}", hsl.l in 0.44f..0.66f)
        }
    }

    @Test fun clampKeepsTheHue() {
        val before = AccentMath.toHsl(pureBlue).h
        val after = AccentMath.toHsl(AccentMath.clampAccent(pureBlue)).h
        assertEquals(before, after, 2f)
    }

    @Test fun grayAndNearWhiteAreNeutral() {
        assertTrue(AccentMath.isNeutral(gray))
        assertTrue(AccentMath.isNeutral(nearWhite))
        assertTrue(AccentMath.isNeutral(nearBlack))
        assertFalse(AccentMath.isNeutral(pureBlue))
    }

    @Test fun liftReachesSmallTextContrastForEveryPresetAndExtreme() {
        val all = AccentPreset.entries.map { it.argb } + listOf(pureBlue, pureRed, nearWhite, nearBlack, gray)
        for (c in all) {
            val lifted = AccentMath.liftToContrast(AccentMath.clampAccent(c))
            assertTrue(
                "contrast of ${Integer.toHexString(lifted)} = ${AccentMath.contrastRatio(lifted, AccentMath.DARK_BACKGROUND)}",
                AccentMath.contrastRatio(lifted, AccentMath.DARK_BACKGROUND) >= AccentMath.SMALL_TEXT_CONTRAST,
            )
        }
    }

    @Test fun liftDoesNotTouchColoursThatAlreadyPass() {
        val white = AccentMath.WHITE
        assertEquals(white, AccentMath.liftToContrast(white))
    }

    @Test fun onColorPicksTheBetterContrast() {
        assertEquals(AccentMath.BLACK, AccentMath.onColor(0xFFF2C230.toInt())) // yellow
        assertEquals(AccentMath.WHITE, AccentMath.onColor(0xFF1B2A6B.toInt())) // navy
    }

    @Test fun dullColoursAreDetected() {
        assertTrue(AccentMath.isDull(gray))
        assertTrue(AccentMath.isDull(nearBlack))
        assertTrue(AccentMath.isDull(nearWhite))
        assertTrue(AccentMath.isDull(0xFF8A7F80.toInt())) // barely tinted
        assertFalse(AccentMath.isDull(pureRed))
        assertFalse(AccentMath.isDull(0xFF3F7BFF.toInt()))
    }

    @Test fun pickerRefusesColoursThatCannotBeMadeUsable() {
        assertNull(AccentMath.makeUsable(AccentMath.BLACK))
        assertNull(AccentMath.makeUsable(nearBlack))
        assertNull(AccentMath.makeUsable(gray))
        assertNull(AccentMath.makeUsable(0xFF404040.toInt()))
    }

    @Test fun pickerAcceptsPresetsWhiteAndVividColours() {
        for (p in AccentPreset.entries) assertNotNull(p.name, AccentMath.makeUsable(p.argb))
        assertNotNull(AccentMath.makeUsable(nearWhite))
        assertNotNull(AccentMath.makeUsable(pureBlue))
        assertNotNull(AccentMath.makeUsable(pureRed))
    }

    @Test fun negligibleChangeIgnoresTinyShifts() {
        val a = 0xFF3F7BFF.toInt()
        val b = AccentMath.fromHsl(AccentMath.toHsl(a).h + 2f, AccentMath.toHsl(a).s, AccentMath.toHsl(a).l + 0.01f)
        assertTrue(AccentMath.isNegligibleChange(a, b))
        assertFalse(AccentMath.isNegligibleChange(a, 0xFFE5384F.toInt()))
    }

    @Test fun withAlphaKeepsRgb() {
        val c = AccentMath.withAlpha(0xFF3F7BFF.toInt(), 0.5f)
        assertEquals(128, AccentMath.alpha(c))
        assertEquals(0x3F, AccentMath.red(c))
    }
}
