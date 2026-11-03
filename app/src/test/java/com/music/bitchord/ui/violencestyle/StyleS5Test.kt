package com.music.bitchord.ui.violencestyle

import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolenceS5Test {
    private val sources = AccentPreset.entries.map { it.argb } +
        listOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt())
    private val on = StyleState.DISABLED.copy(enabled = true)
    private val tokens = AccentTokens.from(AccentPreset.BLUE.argb)

    @Test fun panelTintStaysDarkEnoughForWhiteTextForEveryAccent() {
        for (s in sources) {
            val t = ViolenceGlassColors.panelTint(AccentTokens.from(s).accent)
            assertTrue(AccentMath.contrastRatio(AccentMath.WHITE, t) >= 12.0)
        }
    }

    @Test fun panelTintMovesTowardTheAccent() {
        val blue = ViolenceGlassColors.panelTint(AccentPreset.BLUE.argb)
        val red = ViolenceGlassColors.panelTint(AccentPreset.RED.argb)
        assertTrue(AccentMath.blue(blue) > AccentMath.blue(red))
    }

    @Test fun detailGlowAlphaFollowsIntensityAndStaysBelowHalf() {
        assertEquals(0.30f, ViolenceGlassColors.detailGlowAlpha(StyleIntensity.MEDIUM), 1e-4f)
        assertTrue(ViolenceGlassColors.detailGlowAlpha(StyleIntensity.LOW) < ViolenceGlassColors.detailGlowAlpha(StyleIntensity.HIGH))
        assertTrue(ViolenceGlassColors.detailGlowAlpha(StyleIntensity.HIGH) <= 0.42f + 1e-4f)
    }

    @Test fun glowAboveAnAlbumPageKeepsWhiteTitlesAtThreeToOneOnADarkPageColour() {
        // Worst case: the strongest alpha (High) of every glow colour over a near-black page colour, and over a mid-dark one.
        for (page in listOf(0xFF000000.toInt(), 0xFF1A1A22.toInt())) for (s in sources) {
            val glow = AccentTokens.from(s).glowColor
            val a = ViolenceGlassColors.detailGlowAlpha(StyleIntensity.HIGH)
            fun mix(p: Int, g: Int) = (p + (g - p) * a).toInt()
            val c = AccentMath.argb(255, mix(AccentMath.red(page), AccentMath.red(glow)), mix(AccentMath.green(page), AccentMath.green(glow)), mix(AccentMath.blue(page), AccentMath.blue(glow)))
            assertTrue("${Integer.toHexString(s)} on ${Integer.toHexString(page)}: ${AccentMath.contrastRatio(AccentMath.WHITE, c)}", AccentMath.contrastRatio(AccentMath.WHITE, c) >= 3.0)
        }
    }

    @Test fun detailGlowIsOffWhenTheStyleOrItsSwitchIsOff() {
        val m: Modifier = Modifier
        assertSame(m, m.violenceDetailGlowFor(StyleState.DISABLED, tokens))
        assertSame(m, m.violenceDetailGlowFor(on.copy(detailGlow = false), tokens))
        assertFalse(m === m.violenceDetailGlowFor(on, tokens))
    }
}
