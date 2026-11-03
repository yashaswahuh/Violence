package com.music.bitchord.ui.violencestyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentEngineTest {

    private val extremes = mapOf(
        "pureBlue" to 0xFF0000FF.toInt(),
        "pureRed" to 0xFFFF0000.toInt(),
        "nearWhite" to 0xFFF8F8FF.toInt(),
        "nearBlack" to 0xFF050508.toInt(),
        "gray" to 0xFF808080.toInt(),
    )

    private fun allSources(): Map<String, Int> =
        AccentPreset.entries.associate { it.name to it.argb } + extremes

    @Test fun everyTokenSetIsConsistent() {
        for ((name, source) in allSources()) {
            val t = AccentTokens.from(source)
            // Contrast of small accent text on the dark background.
            assertTrue(
                "$name textAccent ${AccentMath.contrastRatio(t.textAccent, AccentMath.DARK_BACKGROUND)}",
                AccentMath.contrastRatio(t.textAccent, AccentMath.DARK_BACKGROUND) >= AccentMath.SMALL_TEXT_CONTRAST,
            )
            // onAccent is black or white and is the better of the two.
            assertTrue(name, t.onAccent == AccentMath.WHITE || t.onAccent == AccentMath.BLACK)
            val other = if (t.onAccent == AccentMath.WHITE) AccentMath.BLACK else AccentMath.WHITE
            assertTrue(name, AccentMath.contrastRatio(t.onAccent, t.accent) >= AccentMath.contrastRatio(other, t.accent))
            // accentSoft is the accent at 22 percent alpha.
            assertEquals(name, 56, AccentMath.alpha(t.accentSoft))
            assertEquals(name, AccentMath.red(t.accent), AccentMath.red(t.accentSoft))
            // The glow is never lighter than the accent and never over-saturated.
            val accentHsl = AccentMath.toHsl(t.accent)
            val glowHsl = AccentMath.toHsl(t.glowColor)
            assertTrue("$name glow l ${glowHsl.l} vs ${accentHsl.l}", glowHsl.l <= accentHsl.l * 0.8f + 0.02f)
            assertTrue("$name glow s ${glowHsl.s}", glowHsl.s <= 0.86f)
        }
    }

    @Test fun chromaticSourcesAreClampedAndNotNeutral() {
        for (p in AccentPreset.entries) {
            val t = AccentTokens.from(p.argb)
            if (p == AccentPreset.WHITE) {
                assertTrue(t.neutral)
                continue
            }
            assertFalse(p.name, t.neutral)
            val hsl = AccentMath.toHsl(t.accent)
            assertTrue(p.name, hsl.s in 0.54f..0.91f)
            assertTrue(p.name, hsl.l in 0.44f..0.66f)
        }
    }

    @Test fun grayNearWhiteAndNearBlackAreNeutralAndNeverOvershoot() {
        for (name in listOf("gray", "nearWhite", "nearBlack")) {
            val t = AccentTokens.from(extremes.getValue(name))
            assertTrue(name, t.neutral)
            assertTrue(name, AccentMath.toHsl(t.glowColor).s <= AccentMath.NEUTRAL_SATURATION + 0.01f)
        }
    }

    @Test fun whitePresetStaysNeutralAndLight() {
        val t = AccentTokens.from(AccentPreset.WHITE.argb)
        assertTrue(t.neutral)
        assertTrue(AccentMath.toHsl(t.accent).l >= 0.79f)
    }

    @Test fun defaultTokensAreTheBluePreset() {
        assertEquals(AccentTokens.from(AccentPreset.BLUE.argb), AccentTokens.DEFAULT)
    }

    // ---- resolution and fallbacks ----

    private val song = 0xFFE5384F.toInt()
    private val device = 0xFF2FBF71.toInt()
    private val custom = 0xFF8B5CF6.toInt()
    private val gray = 0xFF808080.toInt()

    @Test fun fromSongUsesTheSongColour() {
        val r = AccentEngine.resolve(StyleAccentSource.FROM_SONG, song, device, custom)
        assertEquals(song, r.argb); assertEquals(AccentOrigin.SONG, r.origin)
    }

    @Test fun missingArtworkHoldsThePreviousSongAccent() {
        val r = AccentEngine.resolve(StyleAccentSource.FROM_SONG, null, device, custom, previousSongAccent = song)
        assertEquals(song, r.argb); assertEquals(AccentOrigin.HELD_SONG, r.origin)
    }

    @Test fun grayArtworkHoldsThePreviousOrFallsBackToDeviceThenCustom() {
        assertEquals(AccentOrigin.HELD_SONG, AccentEngine.resolve(StyleAccentSource.FROM_SONG, gray, device, custom, song).origin)
        assertEquals(AccentOrigin.DEVICE, AccentEngine.resolve(StyleAccentSource.FROM_SONG, gray, device, custom).origin)
        val last = AccentEngine.resolve(StyleAccentSource.FROM_SONG, gray, null, custom)
        assertEquals(AccentOrigin.CUSTOM, last.origin); assertEquals(custom, last.argb)
    }

    @Test fun fromDeviceUsesSystemAccentOrTheFixedDefault() {
        assertEquals(device, AccentEngine.resolve(StyleAccentSource.FROM_DEVICE, song, device, custom).argb)
        val old = AccentEngine.resolve(StyleAccentSource.FROM_DEVICE, song, null, custom)
        assertEquals(StyleDefaults.CUSTOM_COLOR, old.argb); assertEquals(AccentOrigin.FIXED_DEFAULT, old.origin)
    }

    @Test fun customUsesTheChosenColourButRefusedColoursFallBackToBlue() {
        assertEquals(custom, AccentEngine.resolve(StyleAccentSource.CUSTOM, song, device, custom).argb)
        val refused = AccentEngine.resolve(StyleAccentSource.CUSTOM, song, device, AccentMath.BLACK)
        assertEquals(StyleDefaults.CUSTOM_COLOR, refused.argb)
    }

    @Test fun songColourIsIgnoredForOtherSources() {
        assertEquals(custom, AccentEngine.resolve(StyleAccentSource.CUSTOM, song, device, custom).argb)
        assertTrue(AccentEngine.resolve(StyleAccentSource.FROM_DEVICE, song, device, custom).argb != song)
    }

    @Test fun shouldReplaceIgnoresTinyChangesOnly() {
        assertTrue(AccentEngine.shouldReplace(null, song))
        assertFalse(AccentEngine.shouldReplace(song, song))
        assertTrue(AccentEngine.shouldReplace(song, device))
        val nudged = AccentMath.fromHsl(AccentMath.toHsl(song).h + 3f, AccentMath.toHsl(song).s, AccentMath.toHsl(song).l)
        assertFalse(AccentEngine.shouldReplace(song, nudged))
    }
}
