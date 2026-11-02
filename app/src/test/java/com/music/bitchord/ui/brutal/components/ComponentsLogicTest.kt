package com.music.bitchord.ui.brutal.components

import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.ContrastMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

class RotatedGeometryTest {
    @Test fun noRotationKeepsTheSize() {
        val b = rotatedBounds(300f, 100f, 0f)
        assertEquals(300f, b.width, 1e-3f)
        assertEquals(100f, b.height, 1e-3f)
    }

    @Test fun aQuarterTurnSwapsWidthAndHeight() {
        val b = rotatedBounds(300f, 100f, -90f)
        assertEquals(100f, b.width, 1e-2f)
        assertEquals(300f, b.height, 1e-2f)
    }

    @Test fun clockwiseAndCounterClockwiseGiveTheSameBox() {
        val a = rotatedBounds(240f, 120f, -12f)
        val b = rotatedBounds(240f, 120f, 12f)
        assertEquals(a.width, b.width, 1e-4f)
        assertEquals(a.height, b.height, 1e-4f)
    }

    @Test fun theStickerTiltGrowsTheBoxModestly() {
        // 240 x 120 plate at -12 degrees: width 240 cos + 120 sin, height 240 sin + 120 cos.
        val b = rotatedBounds(240f, 120f, -12f)
        assertEquals(240f * 0.97815f + 120f * 0.20791f, b.width, 0.05f)
        assertEquals(240f * 0.20791f + 120f * 0.97815f, b.height, 0.05f)
        assertTrue(b.width > 240f && b.height > 120f)
    }

    @Test fun theBoxIsNeverSmallerThanTheShapeInEitherDirection() {
        val rnd = Random(7)
        repeat(500) {
            val w = 10f + rnd.nextFloat() * 400f
            val h = 10f + rnd.nextFloat() * 400f
            val deg = -90f + rnd.nextFloat() * 180f
            val b = rotatedBounds(w, h, deg)
            // The rotated box's area is at least the shape's area (a rotated rectangle never fits a smaller box).
            assertTrue("$w x $h at $deg", b.width * b.height >= w * h - 0.5f)
        }
    }

    @Test fun widthThatFitsWhenRotatedFitsExactly() {
        val maxWidth = 320f
        val ratio = 0.5f
        val w = widthThatFitsWhenRotated(maxWidth, ratio, -12f)
        val b = rotatedBounds(w, w * ratio, -12f)
        assertEquals(maxWidth, b.width, 0.05f)
    }

    @Test fun anUprightPlateKeepsItsWholeWidth() {
        assertEquals(300f, widthThatFitsWhenRotated(300f, 0.5f, 0f), 1e-3f)
    }
}

class ComponentTextTest {
    @Test fun durationPartsSplitAndClamp() {
        assertEquals(DurationParts(0, 3, 45), durationParts(225_000))
        assertEquals(DurationParts(1, 2, 3), durationParts(3_723_000))
        assertEquals(DurationParts(0, 0, 0), durationParts(-5000))
        assertEquals(DurationParts(0, 0, 0), durationParts(999))
        assertEquals(DurationParts(0, 0, 1), durationParts(1999))
        assertTrue(durationParts(Long.MAX_VALUE).hours > 0)
    }

    @Test fun durationFormatMatchesTheMockupsChip() {
        assertEquals("03:45", formatDuration(225_000))
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:07", formatDuration(7_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
        assertEquals("10:00:00", formatDuration(36_000_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test fun dateStampIsDayMonthYearTwoDigits() {
        assertEquals("28 11 22", formatDateStamp(LocalDate.of(2022, 11, 28)))
        assertEquals("01 02 23", formatDateStamp(LocalDate.of(2023, 2, 1)))
        assertEquals("15 09 22", formatDateStamp(LocalDate.of(2022, 9, 15)))
        assertEquals("31 12 05", formatDateStamp(LocalDate.of(2005, 12, 31)))
        assertEquals("01 01 00", formatDateStamp(LocalDate.of(2000, 1, 1)))
    }

    @Test fun spokenDateIsWordsInTheLanguage() {
        val en = spokenDate(LocalDate.of(2022, 11, 28), Locale.US)
        assertTrue(en, en.contains("November") && en.contains("2022"))
        val fr = spokenDate(LocalDate.of(2022, 11, 28), Locale.FRANCE)
        assertTrue(fr, fr.contains("novembre"))
    }

    @Test fun tileAspectIsClampedToTheMockupsRange() {
        assertEquals(1.0f, clampTileAspect(0.4f), 0f)
        assertEquals(1.0f, clampTileAspect(1.0f), 0f)
        assertEquals(1.27f, clampTileAspect(1.27f), 0f)
        assertEquals(1.8f, clampTileAspect(5f), 0f)
        assertEquals(1.0f, clampTileAspect(Float.NaN), 0f)
    }
}

class MasonryJitterTest {
    @Test fun theSameKeyAlwaysMovesTheSameWay() {
        for (key in listOf(0, 1, 42, -7, Int.MAX_VALUE, Int.MIN_VALUE, "weekend".hashCode())) {
            assertEquals(masonryJitter(key, 6f), masonryJitter(key, 6f))
        }
    }

    @Test fun theOffsetIsBounded() {
        for (key in -5000..5000) {
            val j = masonryJitter(key, 6f)
            assertTrue("y $key ${j.yDp}", kotlin.math.abs(j.yDp) <= 6f + 1e-4f)
            assertTrue("x $key ${j.xDp}", kotlin.math.abs(j.xDp) <= 3f + 1e-4f)
        }
    }

    @Test fun noOverlapMeansNoOffset() {
        for (key in 0..100) assertEquals(Jitter(0f, 0f), masonryJitter(key, 0f).let { Jitter(it.xDp + 0f, it.yDp + 0f) })
    }

    @Test fun neighbouringKeysDoNotAllLeanTheSameWay() {
        val ys = (0..40).map { masonryJitter(it, 6f).yDp }
        assertTrue("some go up", ys.any { it < -1f })
        assertTrue("some go down", ys.any { it > 1f })
        assertTrue("they are not all equal", ys.toSet().size > 10)
    }

    @Test fun differentKeysUsuallyDiffer() {
        val seen = (0..200).map { masonryJitter(it, 6f) }.toSet()
        assertTrue(seen.size > 150)
    }

    @Test fun theOffsetScalesWithTheOverlap() {
        val small = masonryJitter(17, 4f)
        val big = masonryJitter(17, 8f)
        assertEquals(small.yDp * 2f, big.yDp, 1e-4f)
        assertEquals(small.xDp * 2f, big.xDp, 1e-4f)
    }
}

/**
 * Every text and icon pair the components draw, for every preset and for any custom colour. The
 * accent derivation (spec 3.2) is what makes them hold: whatever the user picks, the guard repairs
 * it, so no pair may fall under its limit (4.5:1 for text, 3:1 for icons, strokes and the focus ring).
 */
class ComponentContrastTest {
    private fun describe(p: ContrastPair) = "${p.name}: %.2f < ${p.minRatio}".format(p.ratio)

    @Test fun everyPresetPassesEveryPairWithAndWithoutGlow() {
        for (preset in AccentPreset.entries) for (glow in listOf(false, true)) {
            val palette = BrutalPalette.of(BrutalAccent.Preset(preset), glow)
            for (pair in brutalContrastPairs(palette)) {
                assertTrue("${preset.id} glow=$glow ${describe(pair)}", pair.passes)
            }
        }
    }

    @Test fun anyRandomCustomColourPassesEveryPair() {
        val rnd = Random(20261102)
        repeat(3000) {
            val argb = (0xFF shl 24) or rnd.nextInt(0x1000000)
            val palette = BrutalPalette.of(BrutalAccent.Custom(argb), glow = rnd.nextBoolean())
            for (pair in brutalContrastPairs(palette)) {
                assertTrue("#%06X ${describe(pair)}".format(argb and 0xFFFFFF), pair.passes)
            }
        }
    }

    @Test fun theEdgeColoursPassToo() {
        // Black, white, mid grey, the page colour, pure primaries and a colour that is the on-accent ink itself.
        for (argb in listOf(0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFF08080A, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF0A0A0A, 0xFF2C2C2C, 0xFF1D1D1D)) {
            for (glow in listOf(false, true)) {
                val palette = BrutalPalette.of(BrutalAccent.Custom(argb.toInt()), glow)
                for (pair in brutalContrastPairs(palette)) {
                    assertTrue("#%08X glow=$glow ${describe(pair)}".format(argb), pair.passes)
                }
            }
        }
    }

    @Test fun theListCoversTheTextAndTheIconCases() {
        val pairs = brutalContrastPairs(BrutalPalette.Default)
        assertTrue(pairs.size >= 14)
        assertTrue(pairs.any { it.minRatio == ContrastMath.TEXT })
        assertTrue(pairs.any { it.minRatio == ContrastMath.LARGE })
        assertTrue(pairs.map { it.name }.toSet().size == pairs.size)
    }

    @Test fun aSimpleBadPairWouldBeCaught() {
        // The guard itself works: dark grey text on the page fails 4.5:1.
        val bad = ContrastPair("dark on dark", 0xFF303030.toInt(), 0xFF08080A.toInt(), ContrastMath.TEXT)
        assertFalse(bad.passes)
        assertNotEquals(0.0, bad.ratio, 0.0)
    }
}
