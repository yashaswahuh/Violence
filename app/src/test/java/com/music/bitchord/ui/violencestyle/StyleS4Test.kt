package com.music.bitchord.ui.violencestyle

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolenceGlassColorsTest {
    private val sources = AccentPreset.entries.map { it.argb } +
        listOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(), 0xFFF8F8FF.toInt())

    private fun over(base: Int, top: Int, a: Float) = AccentMath.argb(
        255,
        (AccentMath.red(base) + (AccentMath.red(top) - AccentMath.red(base)) * a).toInt(),
        (AccentMath.green(base) + (AccentMath.green(top) - AccentMath.green(base)) * a).toInt(),
        (AccentMath.blue(base) + (AccentMath.blue(top) - AccentMath.blue(base)) * a).toInt(),
    )

    @Test fun capsuleKeepsWhiteLabelsAtFourPointFiveForEveryAccent() {
        for (s in sources) {
            val accent = AccentTokens.from(s).accent
            val alpha = ViolenceGlassColors.capsuleAlpha(accent)
            assertTrue("$alpha", alpha in 0.06f..ViolenceGlassColors.CAPSULE_MAX_ALPHA + 1e-6f)
            val composite = over(ViolenceChromeColors.TINT_BASE, accent, alpha)
            assertTrue(
                "${Integer.toHexString(s)} contrast ${AccentMath.contrastRatio(AccentMath.WHITE, composite)}",
                AccentMath.contrastRatio(AccentMath.WHITE, composite) >= ViolenceGlassColors.CAPSULE_TEXT_CONTRAST,
            )
        }
    }

    @Test fun capsuleFillCarriesTheAccentHueAndTheChosenAlpha() {
        val accent = AccentTokens.from(AccentPreset.BLUE.argb).accent
        val fill = ViolenceGlassColors.capsuleFill(accent)
        assertEquals(accent and 0xFFFFFF, fill and 0xFFFFFF)
        assertEquals(Math.round(ViolenceGlassColors.capsuleAlpha(accent) * 255f), AccentMath.alpha(fill))
    }

    @Test fun glowLineIsBrighterThanTheAccentAndKeepsItsHue() {
        for (s in sources) {
            val accent = AccentTokens.from(s).accent
            if (AccentMath.isNeutral(accent)) continue
            val line = ViolenceGlassColors.glowLine(accent)
            assertTrue(AccentMath.toHsl(line).l >= AccentMath.toHsl(accent).l)
            assertEquals(AccentMath.toHsl(accent).h, AccentMath.toHsl(line).h, 3f)
        }
    }

    @Test fun glowStrengthFollowsTheIntensityWithinBounds() {
        assertEquals(1f, ViolenceGlassColors.glowStrength(StyleIntensity.MEDIUM), 1e-4f)
        assertTrue(ViolenceGlassColors.glowStrength(StyleIntensity.LOW) < 1f)
        assertTrue(ViolenceGlassColors.glowStrength(StyleIntensity.HIGH) > 1f)
        assertTrue(ViolenceGlassColors.glowStrength(StyleIntensity.HIGH) <= 1.4f)
        assertTrue(ViolenceGlassColors.glowStrength(StyleIntensity.LOW) >= 0.6f)
    }

    @Test fun switchTrackKeepsThreeToOneAgainstTheWhiteThumb() {
        for (s in sources) {
            val track = ViolenceGlassColors.switchTrack(AccentTokens.from(s).accent)
            assertTrue(
                "${Integer.toHexString(s)} ${AccentMath.contrastRatio(AccentMath.WHITE, track)}",
                AccentMath.contrastRatio(AccentMath.WHITE, track) >= ViolenceGlassColors.THUMB_CONTRAST,
            )
        }
    }

    @Test fun switchTrackIsUntouchedWhenItAlreadyPasses() {
        val dark = AccentMath.fromHsl(220f, 0.6f, 0.30f)
        assertEquals(dark, ViolenceGlassColors.switchTrack(dark))
    }

    @Test fun reducedTransparencyTintIsNearlySolidAndStillDarkEnoughForWhiteIcons() {
        for (s in sources) {
            val t = ViolenceChromeColors.tint(AccentTokens.from(s).accent, ViolenceGlassColors.SOLID_TINT_ALPHA)
            assertEquals(235.0, AccentMath.alpha(t).toDouble(), 1.0)
            val opaque = AccentMath.argb(255, AccentMath.red(t), AccentMath.green(t), AccentMath.blue(t))
            assertTrue(AccentMath.contrastRatio(AccentMath.WHITE, opaque) >= 7.0)
        }
    }

    @Test fun chipSelectedColourKeepsWhiteLabelsReadable() {
        val base = Color(0xFF636366)
        for (s in sources) {
            val accent = Color(AccentTokens.from(s).accent)
            val c = com.music.bitchord.ui.violencestyle.violencePillColor(base, accent)
            val argb = AccentMath.argb(255, (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
            assertTrue("${Integer.toHexString(s)} ${AccentMath.contrastRatio(AccentMath.WHITE, argb)}", AccentMath.contrastRatio(AccentMath.WHITE, argb) >= 4.4)
        }
    }
}

class ViolenceS4OffParityTest {
    private val tokens = AccentTokens.from(AccentPreset.BLUE.argb)
    private val on = StyleState.DISABLED.copy(enabled = true)

    @Test fun capsuleGlowReturnsTheSameModifierWhenOffOrNeutral() {
        val m: Modifier = Modifier
        assertSame(m, m.violenceCapsuleGlowFor(StyleState.DISABLED, tokens))
        assertSame(m, m.violenceCapsuleGlowFor(on, AccentTokens.from(AccentPreset.WHITE.argb)))
    }

    @Test fun capsuleGlowDrawsWhenOnWithAChromaticAccent() {
        val m: Modifier = Modifier
        assertFalse(m === m.violenceCapsuleGlowFor(on, tokens))
    }

    @Test fun noPresetIsTreatedAsNeutralExceptWhite() {
        for (p in AccentPreset.entries) {
            assertEquals(p.name, p == AccentPreset.WHITE, AccentTokens.from(p.argb).neutral)
        }
    }
}
