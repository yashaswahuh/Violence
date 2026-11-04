package com.music.violence.desktop

import androidx.compose.ui.graphics.Color
import com.music.violence.ui.violencestyle.AccentEngine
import com.music.violence.ui.violencestyle.AccentPreset
import com.music.violence.ui.violencestyle.AccentTokens
import com.music.violence.ui.violencestyle.BackgroundGeometry
import com.music.violence.ui.violencestyle.StyleAccentSource
import com.music.violence.ui.violencestyle.StyleBackground
import com.music.violence.ui.violencestyle.StyleDefaults
import com.music.violence.ui.violencestyle.StyleHeadingFont
import com.music.violence.ui.violencestyle.StyleIntensity
import com.music.violence.ui.violencestyle.StyleMotion
import com.music.violence.ui.violencestyle.StyleState
import com.music.violence.ui.violencestyle.ViolenceChromeColors
import com.music.violence.ui.violencestyle.ViolenceGlassColors
import com.music.violence.ui.violencestyle.ViolenceScrim
import com.music.violence.ui.violencestyle.ViolenceStyleText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopStyleParityTest {

    @Test
    fun `desktop appearance settings default to style off`() {
        // By default, violenceStyleEnabled must be false (100% OFF parity)
        val style = DesktopAppearanceSettings.currentStyleState()
        assertFalse(style.enabled, "Violence style must be disabled by default")
        assertEquals(StyleState.DISABLED, style)
    }

    @Test
    fun `enabling violence style builds active style state with defaults`() {
        DesktopAppearanceSettings.setViolenceStyleEnabled(true)
        try {
            val style = DesktopAppearanceSettings.currentStyleState()
            assertTrue(style.enabled)
            assertEquals(DesktopAppearanceSettings.styleBackground.value, style.background)
            assertEquals(DesktopAppearanceSettings.styleMotion.value, style.motion)
            assertEquals(DesktopAppearanceSettings.styleIntensity.value, style.intensity)
            assertEquals(DesktopAppearanceSettings.styleAccentSource.value, style.accentSource)
            assertEquals(DesktopAppearanceSettings.styleCustomColor.value, style.customColor)
            assertEquals(DesktopAppearanceSettings.styleHeadingFont.value, style.headingFont)
        } finally {
            // Restore default OFF state
            DesktopAppearanceSettings.setViolenceStyleEnabled(false)
        }
    }

    @Test
    fun `customizing style options persists and reflects in currentStyleState`() {
        DesktopAppearanceSettings.setViolenceStyleEnabled(true)
        try {
            DesktopAppearanceSettings.setStyleBackground(StyleBackground.HAZE)
            DesktopAppearanceSettings.setStyleMotion(StyleMotion.SUBTLE)
            DesktopAppearanceSettings.setStyleIntensity(StyleIntensity.HIGH)
            DesktopAppearanceSettings.setStyleHeadingFont(StyleHeadingFont.CORMORANT_GARAMOND)

            val style = DesktopAppearanceSettings.currentStyleState()
            assertEquals(StyleBackground.HAZE, style.background)
            assertEquals(StyleMotion.SUBTLE, style.motion)
            assertEquals(StyleIntensity.HIGH, style.intensity)
            assertEquals(StyleHeadingFont.CORMORANT_GARAMOND, style.headingFont)
        } finally {
            // Reset to defaults
            DesktopAppearanceSettings.setStyleBackground(StyleDefaults.BACKGROUND)
            DesktopAppearanceSettings.setStyleMotion(StyleDefaults.MOTION)
            DesktopAppearanceSettings.setStyleIntensity(StyleDefaults.INTENSITY)
            DesktopAppearanceSettings.setStyleHeadingFont(StyleDefaults.HEADING_FONT)
            DesktopAppearanceSettings.setViolenceStyleEnabled(false)
        }
    }

    @Test
    fun `desktop colorScheme produces luminous dark canvas when enabled`() {
        val disabledScheme = desktopColorScheme(StyleState.DISABLED, null)
        assertEquals(DesktopBackground, disabledScheme.background)

        val tokens = AccentTokens.from(AccentPreset.BLUE.argb)
        val activeState = StyleState.DISABLED.copy(enabled = true)
        val enabledScheme = desktopColorScheme(activeState, tokens)

        assertEquals(Color(0xFF05050A), enabledScheme.background, "Background must be forced to dark canvas")
        assertEquals(Color(tokens.accent), enabledScheme.primary, "Primary must follow resolved accent")
    }

    @Test
    fun `desktop glass colors and chrome tints compute properly`() {
        val accent = AccentPreset.BLUE.argb
        val capsuleAlpha = ViolenceGlassColors.capsuleAlpha(accent)
        assertTrue(capsuleAlpha in 0.06f..0.32f, "Capsule alpha must be clamped")

        val panelTint = ViolenceGlassColors.panelTint(accent)
        assertTrue(panelTint != 0)

        val chromeTint = ViolenceChromeColors.tint(accent)
        assertTrue(chromeTint != 0)
    }

    @Test
    fun `desktop background geometry supports aurora corner glow and haze`() {
        assertEquals(3, BackgroundGeometry.blobCount(StyleBackground.AURORA))
        assertEquals(2, BackgroundGeometry.blobCount(StyleBackground.CORNER_GLOW))
        assertEquals(2, BackgroundGeometry.blobCount(StyleBackground.HAZE))
        assertEquals(0, BackgroundGeometry.blobCount(StyleBackground.OFF))

        assertNotNull(BackgroundGeometry.vignette(StyleBackground.AURORA))
        assertNotNull(BackgroundGeometry.vignette(StyleBackground.HAZE))
        assertNull(BackgroundGeometry.vignette(StyleBackground.CORNER_GLOW))
    }

    @Test
    fun `desktop section header text split helper`() {
        val hero = ViolenceStyleText.split("Top Picks for You")
        assertEquals("TOP PICKS", hero.caps)
        assertEquals("for You", hero.italic)

        val single = ViolenceStyleText.split("Recents")
        assertEquals("RECENTS", single.caps)
        assertNull(single.italic)
    }

    @Test
    fun `desktop card scrim curve is monotonic and bounded`() {
        assertEquals(0f, ViolenceScrim.alphaAt(0f))
        assertEquals(0f, ViolenceScrim.alphaAt(ViolenceScrim.START))
        assertEquals(ViolenceScrim.MID_ALPHA, ViolenceScrim.alphaAt(ViolenceScrim.MID), 0.01f)
        assertEquals(ViolenceScrim.END_ALPHA, ViolenceScrim.alphaAt(1f), 0.01f)
    }
}


