package com.music.violence.ui.violencestyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The light version's colour rules: readable accent text, a light background and lighter surfaces. */
class StyleLightTest {

    @Test fun everyPresetReadsOnTheLightPage() {
        for (preset in AccentPreset.entries) {
            val text = AccentTokens.from(preset.argb).textAccentOnLight
            val contrast = AccentMath.contrastRatio(text, AccentMath.LIGHT_BACKGROUND)
            assertTrue("${preset.name}: $contrast", contrast >= AccentMath.SMALL_TEXT_CONTRAST - 0.01)
        }
    }

    @Test fun darkTextAccentIsUnchanged() {
        val tokens = AccentTokens.from(AccentPreset.BLUE.argb)
        assertEquals(AccentMath.liftToContrast(tokens.accent), tokens.textAccent)
    }

    @Test fun dropToContrastKeepsAnAlreadyReadableColour() {
        val ink = 0xFF202020.toInt()
        assertEquals(ink, AccentMath.dropToContrast(ink))
    }

    @Test fun lightBackgroundBaseIsLightForAnyGlow() {
        for (preset in AccentPreset.entries) {
            val glow = AccentTokens.from(preset.argb).glowColor
            val base = BackgroundGeometry.baseColorLight(glow)
            assertTrue("${preset.name} base too dark", AccentMath.relativeLuminance(base) > 0.80)
        }
    }

    @Test fun lightSurfacesAreLighterThanDarkOnes() {
        val accent = AccentPreset.PINK.argb
        assertTrue(
            AccentMath.relativeLuminance(ViolenceGlassColors.panelTint(accent, light = true)) >
                AccentMath.relativeLuminance(ViolenceGlassColors.panelTint(accent, light = false)),
        )
        assertTrue(
            AccentMath.relativeLuminance(ViolenceChromeColors.tint(accent, light = true)) >
                AccentMath.relativeLuminance(ViolenceChromeColors.tint(accent, light = false)),
        )
    }

    @Test fun darkPanelTintIsStillTheOldOne() {
        // The default must be the dark version, so nothing changes for anyone who never turns light on.
        val accent = AccentPreset.BLUE.argb
        assertEquals(ViolenceGlassColors.panelTint(accent, light = false), ViolenceGlassColors.panelTint(accent))
    }
}
