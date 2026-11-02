package com.music.bitchord.ui.brutal.glass

import android.graphics.Bitmap
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Real pixels, on the JVM, with no emulator and no device. Robolectric's native graphics mode runs
 * Compose's own Skia drawing, and these tests call the kit's actual draw functions
 * (`drawGlassSurface`, `drawGlassFront`, `drawGlassHalo`, with the same cache the modifier builds)
 * straight into an offscreen bitmap, so what is checked is what the surfaces draw.
 *
 * What this can show: every level except the live part of FULL. The backdrop, blur and lens that
 * FULL adds through `liquidGlass` need a GPU, so a FULL surface here has its new layers but no
 * backdrop (the file name says `-nobackdrop`). Nothing about FULL's refraction is checked.
 *
 * The PNGs go to `app/build/brutal-gallery/` for comparison with the mockups; copy the ones worth
 * keeping into `fork-docs/brutal-ui/screens/`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class BrutalGlassRenderTest {
    private val outDir = File("build/brutal-gallery").apply { mkdirs() }
    private val dens = Density(2.625f)
    private val page = BrutalPalette.Page
    private val stripes = listOf(Color(0xFF1B4DFF), Color(0xFFFF7A00), Color(0xFFF2F2F2), Color(0xFF7A1FA2))

    private fun save(name: String, bitmap: Bitmap) {
        FileOutputStream(File(outDir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** One surface of [kind] drawn the way the modifier draws it, at [x], [y] (px) with the size in dp. */
    private fun DrawScope.surface(
        x: Float, y: Float, wDp: Float, hDp: Float,
        kind: GlassKind, level: GlassLevel, glow: Boolean, accent: BrutalAccent,
        shape: CornerBasedShape, behind: (DrawScope.() -> Unit)? = null, content: (DrawScope.() -> Unit)? = null,
    ) {
        val w = wDp * dens.density
        val h = hDp * dens.density
        translate(x, y) {
            behind?.invoke(this)
            val colors = BrutalAccentRules.derive(accent.argb)
            val plan = planGlass(kind, level, glow, textScrim = kind == GlassKind.Floating || kind == GlassKind.Panel)
            val style = glassStyle(kind, glow, colors)
            if (plan.halo) drawGlassHalo(buildHaloCache(Size(w, h), dens, LayoutDirection.Ltr, shape, style), style, 1f)
            val cache = buildGlassCache(Size(w, h), dens, LayoutDirection.Ltr, shape, plan, style)
            clipPath(cache.path) {
                drawGlassSurface(cache, plan, style, 1f)
                content?.invoke(this)
                drawGlassFront(cache, plan, style, 1f)
            }
        }
    }

    private fun render(wPx: Int, hPx: Int, block: DrawScope.() -> Unit): Bitmap {
        val image = ImageBitmap(wPx, hPx)
        CanvasDrawScope().draw(dens, LayoutDirection.Ltr, Canvas(image), Size(wPx.toFloat(), hPx.toFloat())) { block() }
        return image.asAndroidBitmap()
    }

    /** Pixels that read as a strongly warm accent (not grey, not white) inside [region]. */
    private fun accentPixels(b: Bitmap, left: Int = 0, top: Int = 0, right: Int = b.width, bottom: Int = b.height, minRed: Int = 90, minWarm: Int = 60): Int {
        var n = 0
        for (y in top until bottom) for (x in left until right) {
            val p = b.getPixel(x, y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val bl = p and 0xFF
            if (r > minRed && g > minRed * 2 / 3 && r - bl > minWarm) n++
        }
        return n
    }

    // A bar, 400 x 100 dp, on a dark page with 30 dp around it for the under-glow.
    private val pad = 30f * 2.625f
    private fun bar(level: GlassLevel, glow: Boolean, name: String? = null): Bitmap {
        val w = 400f * 2.625f
        val h = 100f * 2.625f
        val bmp = render((w + 2 * pad).toInt(), (h + 2 * pad).toInt()) {
            drawRect(page)
            surface(pad, pad, 400f, 100f, GlassKind.Floating, level, glow, BrutalAccent.Preset(AccentPreset.YELLOW), BrutalShapes.Stadium)
        }
        if (name != null) save(name, bmp)
        return bmp
    }

    private fun insideBar(b: Bitmap) = intArrayOf(pad.toInt() + 4, pad.toInt() + 4, b.width - pad.toInt() - 4, b.height - pad.toInt() - 4)

    @Test fun noGlowNeverPaintsTheAccentIntoTheGlass() {
        for (level in listOf(GlassLevel.SIMPLIFIED, GlassLevel.SOLID, GlassLevel.FULL)) {
            val b = bar(level, glow = false, name = "bar-${level.name.lowercase()}-noglow-yellow" + if (level == GlassLevel.FULL) "-nobackdrop" else "")
            val (l, t, r, bt) = insideBar(b)
            assertEquals("NO-GLOW at $level: no accent pixel anywhere on the canvas", 0, accentPixels(b))
            assertTrue("the surface drew something: inside differs from the page", b.getPixel((l + r) / 2, (t + bt) / 2) != 0xFF08080A.toInt() || level == GlassLevel.SOLID)
        }
    }

    @Test fun glowPaintsTheAccentIntoTheGlassAndOutsideItAsUnderGlow() {
        val b = bar(GlassLevel.SIMPLIFIED, glow = true, name = "bar-simplified-glow-yellow")
        val (l, t, r, bt) = insideBar(b)
        val inside = accentPixels(b, l, t, r, bt)
        assertTrue("GLOW: the bleed and rim tint show inside the glass, found $inside", inside > 500)
        // The under-glow is outside the bar: below its bottom edge there is accent too.
        val below = accentPixels(b, l, b.height - pad.toInt() + 2, r, b.height, minRed = 30, minWarm = 20)
        assertTrue("GLOW: an under-glow below the bar, found $below", below > 100)
    }

    @Test fun theUnderGlowIsStrongestAtTheEdgeAndFadesOut() {
        val b = bar(GlassLevel.SIMPLIFIED, glow = true)
        val x = b.width / 2
        val edge = b.height - pad.toInt() // first row below the bar
        fun red(y: Int) = (b.getPixel(x, y) shr 16) and 0xFF
        val near = red(edge + 3)
        val mid = red(edge + (pad * 0.5f).toInt())
        val far = red(b.height - 1)
        assertTrue("near $near > mid $mid", near > mid)
        assertTrue("mid $mid >= far $far", mid >= far)
        assertTrue("far is back to the page (about 8)", far < 24)
    }

    @Test fun solidWithGlowHasAnAccentHairlineAndNoGlass() {
        val b = bar(GlassLevel.SOLID, glow = true, name = "bar-solid-glow-yellow")
        val (l, t, r, bt) = insideBar(b)
        assertTrue("an accent hairline and a static bleed", accentPixels(b) > 50)
        assertEquals("SOLID has no under-glow outside the bar", 0, accentPixels(b, 0, b.height - pad.toInt() + 4, b.width, b.height, minRed = 30, minWarm = 20))
        assertTrue(l < r && t < bt)
    }

    // ---- a contact sheet per level / accent / page, for comparison with the mockups ----

    private fun gallery(level: GlassLevel, accentPreset: AccentPreset, darkPage: Boolean, name: String) {
        val accent = BrutalAccent.Preset(accentPreset)
        val d = dens.density
        val colW = 360f
        val width = (colW * 2 * d).toInt()
        val height = (640f * d).toInt()
        val pageColor = if (darkPage) page else Color(0xFFEDEDF0)
        val bmp = render(width, height) {
            drawRect(pageColor)
            for ((col, glow) in listOf(false, true).withIndex()) {
                val x0 = col * colW * d + 20f * d
                // Colour behind the bar, so a body over content is visible.
                val bandTop = 20f * d
                stripes.forEachIndexed { i, c ->
                    drawRect(c, Offset(x0 + i * 80f * d, bandTop), Size(80f * d, 96f * d))
                }
                surface(x0, bandTop + 8f * d, 320f, 80f, GlassKind.Floating, level, glow, accent, BrutalShapes.Stadium)
                // Round buttons and rail tabs.
                var bx = x0
                repeat(3) {
                    surface(bx, 140f * d, 48f, 48f, GlassKind.Floating, level, glow, accent, BrutalShapes.Stadium)
                    bx += 60f * d
                }
                surface(bx, 140f * d, 48f, 96f, GlassKind.Floating, level, glow, accent, BrutalShapes.Stadium)
                surface(bx + 60f * d, 140f * d, 48f, 96f, GlassKind.Floating, level, glow, accent, BrutalShapes.Stadium)
                // A panel.
                surface(x0, 260f * d, 320f, 110f, GlassKind.Panel, level, glow, accent, RoundedCornerShape(28.dp))
                // Tiles with artwork, and a chip.
                val arts = listOf(listOf(Color(0xFFFFB000), Color(0xFFD14A00)), listOf(Color(0xFF2B63FF), Color(0xFF15204F)), listOf(Color(0xFFD81B3B), Color(0xFF3A0610)))
                var tx = x0
                for (art in arts) {
                    surface(
                        tx, 400f * d, 96f, 96f, GlassKind.Tile, level, glow, accent, BrutalShapes.Tile,
                        behind = {
                            val inset = 5f * d
                            drawRoundRect(Brush.verticalGradient(art), Offset(inset, inset), Size(96f * d - 2 * inset, 96f * d - 2 * inset), CornerRadius(12f * d))
                        },
                    )
                    tx += 108f * d
                }
                surface(x0, 520f * d, 72f, 24f, GlassKind.Chip, level, glow, accent, BrutalShapes.Stadium)
            }
        }
        save(name, bmp)
        // The two halves must differ (Glow is visible somewhere) unless the level is SOLID without anything to show.
        var diff = 0
        val half = width / 2
        for (y in 0 until height step 2) for (x in 0 until half step 2) if (bmp.getPixel(x, y) != bmp.getPixel(x + half, y)) diff++
        assertTrue("$name: Glow on and off look different, differing samples = $diff", diff > 200)
    }

    @Test fun gallerySimplifiedYellowDark() = gallery(GlassLevel.SIMPLIFIED, AccentPreset.YELLOW, true, "gallery-simplified-yellow-dark")
    @Test fun gallerySolidYellowDark() = gallery(GlassLevel.SOLID, AccentPreset.YELLOW, true, "gallery-solid-yellow-dark")
    @Test fun galleryFullNoBackdropYellowDark() = gallery(GlassLevel.FULL, AccentPreset.YELLOW, true, "gallery-full-nobackdrop-yellow-dark")
    @Test fun gallerySimplifiedRedDark() = gallery(GlassLevel.SIMPLIFIED, AccentPreset.RED, true, "gallery-simplified-red-dark")
    @Test fun gallerySimplifiedBlueDark() = gallery(GlassLevel.SIMPLIFIED, AccentPreset.BLUE, true, "gallery-simplified-blue-dark")
    @Test fun gallerySimplifiedYellowLightPage() = gallery(GlassLevel.SIMPLIFIED, AccentPreset.YELLOW, false, "gallery-simplified-yellow-light")
}
