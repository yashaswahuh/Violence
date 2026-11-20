package com.music.violence.wear.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The accent engine.
 *
 * The phone sends the four colours its own player took from the cover. This
 * picks the one with real colour in it, and builds the watch's whole palette
 * from that one hue as a set of tones — the way Material 3's dynamic colour
 * does — instead of nudging a single colour to a fixed brightness, which is
 * what made covers come out dull or wrong.
 *
 * The maths is OKLCH (a perceptual colour space: equal steps in lightness look
 * equal), with chroma pulled in until the colour fits sRGB. Lightness is
 * fixed per role, so the dark glyph on a pastel button always has the same
 * contrast (about 10:1) whatever the cover is.
 */
@Immutable
class Palette(
    /** The play button and the transport pills. */
    val accent: Color,
    /** Glyphs on [accent]. */
    val onAccent: Color,
    /** The always-on screen's lines, arc and V. */
    val aod: Color,
    /** The always-on screen's hairline track. */
    val aodDim: Color,
    /** Primary text and icons, lightly tinted. */
    val text: Color,
    /** Secondary text. */
    val textDim: Color,
    /** Dark glass, for the pills under the play button. */
    val glass: Color,
)

/** Voltage violet, for a grey or missing cover. */
private const val FALLBACK_HUE = 300.0
private const val FALLBACK_CHROMA = 0.13

fun paletteFor(colors: List<Color>): Palette {
    var best: Oklch? = null
    var bestScore = -1.0
    for (c in colors) {
        val k = c.toOklch()
        // Too grey, too dark or too light to carry a hue.
        if (k.c < 0.04 || k.l < 0.12 || k.l > 0.97) continue
        val score = k.c + 0.15 * (1.0 - kotlin.math.abs(k.l - 0.55))
        if (score > bestScore) {
            bestScore = score
            best = k
        }
    }
    val hue = best?.h ?: FALLBACK_HUE
    val chroma = best?.let { min(max(it.c, 0.09), 0.15) } ?: FALLBACK_CHROMA
    return Palette(
        accent = tone(0.87, chroma, hue),
        onAccent = tone(0.26, chroma * 0.45, hue),
        aod = tone(0.82, chroma, hue),
        aodDim = tone(0.40, chroma * 0.7, hue),
        text = tone(0.96, 0.015, hue),
        textDim = tone(0.70, 0.02, hue),
        glass = tone(0.30, 0.035, hue).copy(alpha = 0.82f),
    )
}

private class Oklch(val l: Double, val c: Double, val h: Double)

private fun lin(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

private fun gam(v: Double) = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055

private fun Color.toOklch(): Oklch {
    val r = lin(red.toDouble())
    val g = lin(green.toDouble())
    val b = lin(blue.toDouble())
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val ll = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val h = Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360.0 else it }
    return Oklch(ll, hypot(a, bb), h)
}

/** An OKLCH colour as sRGB, with chroma reduced until it is in gamut. */
private fun tone(l: Double, c: Double, h: Double): Color {
    var chroma = c
    repeat(40) {
        val a = chroma * cos(Math.toRadians(h))
        val b = chroma * sin(Math.toRadians(h))
        val l3 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m3 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s3 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        val r = 4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3
        val g = -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3
        val bl = -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3
        if (r in -0.0005..1.0005 && g in -0.0005..1.0005 && bl in -0.0005..1.0005) {
            return Color(
                gam(r.coerceIn(0.0, 1.0)).toFloat(),
                gam(g.coerceIn(0.0, 1.0)).toFloat(),
                gam(bl.coerceIn(0.0, 1.0)).toFloat(),
            )
        }
        chroma *= 0.93
    }
    return Color.Gray
}
