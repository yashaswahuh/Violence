package com.music.bitchord.ui.brutal.components

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/**
 * The component gallery drawn to PNG files on the JVM (Robolectric native graphics), with no emulator
 * and no device: real layout, real Skia drawing, the bundled fonts. The pictures are what the previews
 * show, for the combinations the brief lists (Glow on and off, three accents, dark page and a light
 * page, font scale 1 and 2). Assertions are on pixels: the accent plate is really drawn in the
 * derived accent, and a focused control really shows its ring.
 *
 * Not covered: the live glass backdrop (FULL), which needs a GPU; the gallery runs at SIMPLIFIED.
 * Files go to `app/build/brutal-components/`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w360dp-h3400dp-xhdpi")
@ConscryptMode(ConscryptMode.Mode.OFF)
class ComponentsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Page(dark: Boolean, scale: Float, content: @Composable () -> Unit) {
        MaterialTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = scale)) {
                Box(Modifier.fillMaxSize().background(if (dark) BrutalPalette.Page else Color(0xFFEDEDF0))) { content() }
            }
        }
    }

    private fun near(p: Int, argb: Int, tol: Int = 14): Boolean =
        listOf(16, 8, 0).all { kotlin.math.abs(((p shr it) and 0xFF) - ((argb shr it) and 0xFF)) <= tol }

    private fun count(b: Bitmap, argb: Int, tol: Int = 14): Int {
        var n = 0
        for (y in 0 until b.height step 2) for (x in 0 until b.width step 2) if (near(b.getPixel(x, y), argb, tol)) n++
        return n
    }

    private fun gallery(name: String, preset: AccentPreset, glow: Boolean, dark: Boolean, scale: Float): Bitmap {
        rule.setContent {
            Page(dark, scale) {
                Box(Modifier.verticalScroll(rememberScrollState())) {
                    BrutalComponentsGallery(BrutalAccent.Preset(preset), glow, GlassLevel.SIMPLIFIED, dark)
                }
            }
        }
        val bmp = rule.renderToBitmap()
        saveRender(name, bmp)
        // The accent plate (sticker, chips, selected tab, buttons) is drawn in the derived fill colour.
        val fill = BrutalAccentRules.derive(preset.argb).fill
        val n = count(bmp, fill)
        assertTrue("$name: the accent plate colour #%06X is on the page ($n samples)".format(fill and 0xFFFFFF), n > 400)
        return bmp
    }

    @Test fun yellowGlowDark1x() { gallery("components-yellow-glow-dark-1x", AccentPreset.YELLOW, true, true, 1f) }
    @Test fun yellowNoGlowDark1x() { gallery("components-yellow-noglow-dark-1x", AccentPreset.YELLOW, false, true, 1f) }
    @Test fun yellowGlowDark2x() { gallery("components-yellow-glow-dark-2x", AccentPreset.YELLOW, true, true, 2f) }
    @Test fun yellowNoGlowDark2x() { gallery("components-yellow-noglow-dark-2x", AccentPreset.YELLOW, false, true, 2f) }
    @Test fun redGlowDark1x() { gallery("components-red-glow-dark-1x", AccentPreset.RED, true, true, 1f) }
    @Test fun blueGlowDark1x() { gallery("components-blue-glow-dark-1x", AccentPreset.BLUE, true, true, 1f) }
    @Test fun blueNoGlowDark2x() { gallery("components-blue-noglow-dark-2x", AccentPreset.BLUE, false, true, 2f) }
    @Test fun yellowGlowLightPage1x() { gallery("components-yellow-glow-light-1x", AccentPreset.YELLOW, true, false, 1f) }
    @Test fun yellowNoGlowLightPage1x() { gallery("components-yellow-noglow-light-1x", AccentPreset.YELLOW, false, false, 1f) }
    @Test fun redNoGlowLightPage2x() { gallery("components-red-noglow-light-2x", AccentPreset.RED, false, false, 2f) }

    @Test fun aFocusedControlShowsItsAccentRing() {
        // Window focus never reaches a control under Robolectric, so the focus event is sent to the
        // control's interaction source directly: exactly what the focusable emits on a real device.
        val source = MutableInteractionSource()
        rule.setContent {
            Page(dark = true, scale = 1f) {
                BrutalPreviewHost(BrutalAccent.Preset(AccentPreset.YELLOW), glow = false, level = GlassLevel.SOLID) {
                    Box(Modifier.padding(40.dp)) {
                        Box(Modifier.size(48.dp).testTag("btn").brutalClickable(onClick = {}, shape = CircleShape, interactionSource = source))
                    }
                }
            }
        }
        val before = rule.renderToBitmap()
        val ring = BrutalAccentRules.derive(AccentPreset.YELLOW.argb).text
        val without = count(before, ring, 24)
        val focus = FocusInteraction.Focus()
        rule.runOnUiThread { source.tryEmit(focus) }
        val after = rule.renderToBitmap()
        saveRender("focus-ring", after)
        val with = count(after, ring, 24)
        assertTrue("no ring before focus: $without", without < 10)
        assertTrue("the focus ring adds a circle of accent pixels: $without -> $with", with > 60)
        // And it goes away again when focus leaves.
        rule.runOnUiThread { source.tryEmit(FocusInteraction.Unfocus(focus)) }
        assertTrue("back to no ring", count(rule.renderToBitmap(), ring, 24) < 10)
    }
}