package com.music.violence.style

import com.music.violence.ui.violencestyle.AccentEngine
import com.music.violence.ui.violencestyle.AccentMath
import com.music.violence.ui.violencestyle.AccentOrigin
import com.music.violence.ui.violencestyle.AccentPreset
import com.music.violence.ui.violencestyle.AccentTokens
import com.music.violence.ui.violencestyle.BackgroundGeometry
import com.music.violence.ui.violencestyle.GlowGuard
import com.music.violence.ui.violencestyle.StyleAccentSource
import com.music.violence.ui.violencestyle.StyleBackground
import com.music.violence.ui.violencestyle.StyleCapability
import com.music.violence.ui.violencestyle.StyleDefaults
import com.music.violence.ui.violencestyle.StyleIntensity
import com.music.violence.ui.violencestyle.StyleMotion
import com.music.violence.ui.violencestyle.StyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyleCoreTest {

    @Test
    fun disabledStateHasExactDefaultsAndEffectiveMotionOff() {
        val s = StyleState.DISABLED
        assertFalse(s.enabled)
        assertEquals(StyleBackground.AURORA, s.background)
        assertEquals(StyleIntensity.MEDIUM, s.intensity)
        assertEquals(StyleAccentSource.FROM_SONG, s.accentSource)
        assertEquals(StyleMotion.NORMAL, s.motion)
        assertEquals(StyleDefaults.CUSTOM_COLOR, s.customColor)
        assertEquals(StyleMotion.OFF, s.effectiveMotion)
    }

    @Test
    fun effectiveMotionFollowsRules() {
        val on = StyleState.DISABLED.copy(enabled = true)
        assertEquals(StyleMotion.NORMAL, on.effectiveMotion)
        assertEquals(StyleMotion.OFF, on.copy(capability = StyleCapability.LITE).effectiveMotion)
        assertEquals(StyleMotion.OFF, on.copy(reduceMotion = true).effectiveMotion)
        assertEquals(StyleMotion.OFF, on.copy(motion = StyleMotion.OFF).effectiveMotion)
    }

    @Test
    fun intensityPeaksMatchSpec() {
        assertEquals(0.38f, StyleIntensity.LOW.peakAlpha)
        assertEquals(0.62f, StyleIntensity.MEDIUM.peakAlpha)
        assertEquals(0.85f, StyleIntensity.HIGH.peakAlpha)
    }

    @Test
    fun accentMathBasicChannelsAndArgb() {
        val argb = AccentMath.argb(255, 12, 34, 56)
        assertEquals(255, AccentMath.alpha(argb))
        assertEquals(12, AccentMath.red(argb))
        assertEquals(34, AccentMath.green(argb))
        assertEquals(56, AccentMath.blue(argb))
    }

    @Test
    fun relativeLuminanceAndContrast() {
        val white = AccentMath.WHITE
        val black = AccentMath.BLACK
        assertEquals(1.0, AccentMath.relativeLuminance(white), 0.01)
        assertEquals(0.0, AccentMath.relativeLuminance(black), 0.01)
        assertEquals(21.0, AccentMath.contrastRatio(white, black), 0.1)
        assertEquals(1.0, AccentMath.contrastRatio(white, white), 0.01)
    }

    @Test
    fun neutralDetection() {
        assertTrue(AccentMath.isNeutral(AccentMath.argb(255, 128, 128, 128)))
        assertTrue(AccentMath.isNeutral(AccentPreset.WHITE.argb))
        assertFalse(AccentMath.isNeutral(AccentPreset.RED.argb))
        assertFalse(AccentMath.isNeutral(AccentPreset.BLUE.argb))
    }

    @Test
    fun clampAccentKeepsSaturationAndLightnessInRange() {
        for (c in listOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt(), 0xFF0A0AFF.toInt(), 0xFF332211.toInt(), 0xFFFFEECC.toInt())) {
            if (AccentMath.isNeutral(c)) continue
            val hsl = AccentMath.toHsl(AccentMath.clampAccent(c))
            assertTrue(hsl.s in 0.54f..0.91f, "s=${hsl.s}")
            assertTrue(hsl.l in 0.44f..0.66f, "l=${hsl.l}")
        }
    }

    @Test
    fun liftToContrastGuaranteesSmallTextContrast() {
        AccentPreset.entries.forEach { preset ->
            val lifted = AccentMath.liftToContrast(preset.argb)
            val ratio = AccentMath.contrastRatio(lifted, AccentMath.DARK_BACKGROUND)
            assertTrue(ratio >= AccentMath.SMALL_TEXT_CONTRAST, "${preset.name} contrast $ratio")
        }
    }

    @Test
    fun accentEngineResolvesSourcesCorrectly() {
        val custom = AccentPreset.PURPLE.argb

        // FROM_SONG with vivid song
        val songRes = AccentEngine.resolve(StyleAccentSource.FROM_SONG, AccentPreset.RED.argb, null, custom)
        assertEquals(AccentOrigin.SONG, songRes.origin)
        assertEquals(AccentPreset.RED.argb, songRes.argb)

        // FROM_SONG with dull/gray song -> SONG_NEUTRAL
        val dullSong = AccentMath.argb(255, 60, 60, 60)
        val dullRes = AccentEngine.resolve(StyleAccentSource.FROM_SONG, dullSong, null, custom)
        assertEquals(AccentOrigin.SONG_NEUTRAL, dullRes.origin)
        assertEquals(AccentPreset.WHITE.argb, dullRes.argb)

        // FROM_SONG with null song holds previous
        val heldRes = AccentEngine.resolve(StyleAccentSource.FROM_SONG, null, null, custom, previousSongAccent = AccentPreset.GREEN.argb)
        assertEquals(AccentOrigin.HELD_SONG, heldRes.origin)
        assertEquals(AccentPreset.GREEN.argb, heldRes.argb)

        // FROM_DEVICE
        val devRes = AccentEngine.resolve(StyleAccentSource.FROM_DEVICE, null, AccentPreset.YELLOW.argb, custom)
        assertEquals(AccentOrigin.DEVICE, devRes.origin)
        assertEquals(AccentPreset.YELLOW.argb, devRes.argb)

        // CUSTOM
        val custRes = AccentEngine.resolve(StyleAccentSource.CUSTOM, null, null, custom)
        assertEquals(AccentOrigin.CUSTOM, custRes.origin)
        assertEquals(custom, custRes.argb)
    }

    @Test
    fun backgroundGeometryBlobsAreBounded() {
        listOf(StyleBackground.AURORA, StyleBackground.CORNER_GLOW, StyleBackground.HAZE).forEach { bg ->
            val count = BackgroundGeometry.blobCount(bg)
            assertTrue(count in 1..BackgroundGeometry.MAX_BLOBS)
            val buf = FloatArray(BackgroundGeometry.MAX_BLOBS * BackgroundGeometry.STRIDE)
            val n = BackgroundGeometry.fill(bg, 1000f, 1000f, 0.0, StyleMotion.NORMAL, StyleIntensity.MEDIUM, null, buf)
            assertEquals(count, n)
            for (i in 0 until n) {
                val alpha = buf[i * BackgroundGeometry.STRIDE + 3]
                assertTrue(alpha in 0f..1f, "Alpha out of bounds: $alpha")
            }
        }
    }
}
