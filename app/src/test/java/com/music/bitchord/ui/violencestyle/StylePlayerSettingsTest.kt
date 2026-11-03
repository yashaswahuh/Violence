package com.music.bitchord.ui.violencestyle

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StylePlayerSettingsTest {
    private val heading = ViolenceHeadingStyles.build(ViolenceBodoniModa)
    private val shared = ViolenceSharedStyle(
        accent = Color.Blue, accentSoft = Color.Blue, textAccent = Color.Blue, neutral = false, strength = 0.62f,
        displayCaps = heading.displayCaps, displayItalic = heading.displayItalic, sectionCaps = heading.sectionCaps,
        cardTitleCaps = heading.cardTitleCaps, label = heading.label, motion = true,
    )

    @Test fun playerGlowIsTheSameModifierWhenTheStyleIsOff() {
        val m: Modifier = Modifier
        assertSame(m, m.violencePlayerGlowFor(null) { 0f })
        assertFalse(m === m.violencePlayerGlowFor(shared) { 0f })
    }

    @Test fun playerGlowAlphaFollowsIntensityWithinBounds() {
        assertEquals(0.26f, violencePlayerGlowAlpha(0.62f), 1e-4f)
        assertTrue(violencePlayerGlowAlpha(0.38f) < violencePlayerGlowAlpha(0.85f))
        for (s in listOf(0.1f, 0.38f, 0.62f, 0.85f, 2f)) assertTrue(violencePlayerGlowAlpha(s) in 0.16f..0.36f)
    }

    @Test fun playerGlowStaysLegibleForWhiteTitlesOverMidArtwork() {
        // Worst case: the strongest glow alpha of any preset over a mid-gray backdrop pixel; white must keep 3:1 (large title).
        val a = violencePlayerGlowAlpha(0.85f)
        for (p in AccentPreset.entries) {
            val glow = AccentTokens.from(p.argb).glowColor
            val backdrop = 0xFF303034.toInt()
            fun mix(x: Int, y: Int) = (x + (y - x) * a).toInt()
            val c = AccentMath.argb(255, mix(AccentMath.red(backdrop), AccentMath.red(glow)), mix(AccentMath.green(backdrop), AccentMath.green(glow)), mix(AccentMath.blue(backdrop), AccentMath.blue(glow)))
            assertTrue(p.name, AccentMath.contrastRatio(AccentMath.WHITE, c) >= 3.0)
        }
    }

    @Test fun settingsGroupFillIsTranslucentAndNearSolidWhenTransparencyIsReduced() {
        assertTrue(ViolenceGlassColors.groupFillAlpha(false) < ViolenceGlassColors.groupFillAlpha(true))
        for (p in AccentPreset.entries) {
            val tint = ViolenceGlassColors.panelTint(AccentTokens.from(p.argb).accent)
            // The card's secondary text (#8E8E93) over the tint at its translucent alpha, over a mid glow: still 4.5:1.
            val glowPage = 0xFF14141C.toInt()
            val a = ViolenceGlassColors.groupFillAlpha(false)
            fun mix(x: Int, y: Int) = (x + (y - x) * a).toInt()
            val c = AccentMath.argb(255, mix(AccentMath.red(glowPage), AccentMath.red(tint)), mix(AccentMath.green(glowPage), AccentMath.green(tint)), mix(AccentMath.blue(glowPage), AccentMath.blue(tint)))
            assertTrue(p.name, AccentMath.contrastRatio(GlowGuard.SECONDARY_TEXT, c) >= 4.5)
        }
    }
}
