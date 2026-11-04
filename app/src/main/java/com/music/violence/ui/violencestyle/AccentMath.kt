package com.music.violence.ui.violencestyle

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure colour maths for the accent engine. Colours are packed ARGB ints (the same layout as
 * android.graphics.Color), so everything here runs in plain JVM unit tests.
 */
object AccentMath {

    /** The app's dark background (`colorScheme.background` is pure black while the style is on). */
    const val DARK_BACKGROUND: Int = 0xFF000000.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val BLACK: Int = 0xFF000000.toInt()

    // STYLE_SPEC.md section 3: HSL clamp for every source.
    const val MIN_SATURATION = 0.55f
    const val MAX_SATURATION = 0.90f
    const val MIN_LIGHTNESS = 0.45f
    const val MAX_LIGHTNESS = 0.65f

    /** Glow lightness is the accent's x0.8 (section 3). */
    const val GLOW_LIGHTNESS_FACTOR = 0.8f

    /** Below this HSL saturation a colour is treated as neutral (gray / white / black). */
    const val NEUTRAL_SATURATION = 0.08f

    /** HSL saturation explodes near white and black, so a small channel spread also counts as neutral. */
    const val NEUTRAL_CHROMA = 0.08f

    /** Minimum contrast for small text (WCAG AA). */
    const val SMALL_TEXT_CONTRAST = 4.5

    fun alpha(argb: Int) = (argb ushr 24) and 0xFF
    fun red(argb: Int) = (argb shr 16) and 0xFF
    fun green(argb: Int) = (argb shr 8) and 0xFF
    fun blue(argb: Int) = argb and 0xFF
    fun argb(a: Int, r: Int, g: Int, b: Int) =
        ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    fun withAlpha(argb: Int, alpha: Float): Int =
        argb(((alpha.coerceIn(0f, 1f)) * 255f).roundToInt(), red(argb), green(argb), blue(argb))

    private fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    /** WCAG relative luminance, ignoring alpha. */
    fun relativeLuminance(argb: Int): Double =
        0.2126 * channel(red(argb)) + 0.7152 * channel(green(argb)) + 0.0722 * channel(blue(argb))

    /** WCAG contrast ratio, 1.0 to 21.0. */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Spread between the strongest and weakest channel, 0..1 (colour you can see). */
    fun chroma(argb: Int): Float =
        (max(red(argb), max(green(argb), blue(argb))) - min(red(argb), min(green(argb), blue(argb)))) / 255f

    /** Hue 0..360, saturation 0..1, lightness 0..1. */
    class Hsl(val h: Float, val s: Float, val l: Float)

    fun toHsl(argb: Int): Hsl {
        val r = red(argb) / 255f
        val g = green(argb) / 255f
        val b = blue(argb) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return Hsl(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> 60f * (((g - b) / d) % 6f)
            g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        return Hsl(if (h < 0f) h + 360f else h, s.coerceIn(0f, 1f), l)
    }

    fun fromHsl(h: Float, s: Float, l: Float, alpha: Int = 255): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val ss = s.coerceIn(0f, 1f)
        val ll = l.coerceIn(0f, 1f)
        val c = (1f - abs(2f * ll - 1f)) * ss
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = ll - c / 2f
        val (r1, g1, b1) = when {
            hh < 60f -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(
            alpha,
            ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
            ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
            ((b1 + m) * 255f).roundToInt().coerceIn(0, 255),
        )
    }

    /** True when the colour has no usable hue (gray, white, black). */
    fun isNeutral(argb: Int): Boolean = chroma(argb) < NEUTRAL_CHROMA || toHsl(argb).s < NEUTRAL_SATURATION

    /**
     * Clamps a chromatic colour into the accent range (saturation 0.55..0.90, lightness 0.45..0.65).
     * A neutral colour (the White preset) keeps its low saturation and is lifted to a light gray.
     */
    fun clampAccent(argb: Int): Int {
        val hsl = toHsl(argb)
        return if (isNeutral(argb)) {
            fromHsl(hsl.h, min(hsl.s, NEUTRAL_SATURATION), hsl.l.coerceIn(0.80f, 0.95f))
        } else {
            fromHsl(hsl.h, hsl.s.coerceIn(MIN_SATURATION, MAX_SATURATION), hsl.l.coerceIn(MIN_LIGHTNESS, MAX_LIGHTNESS))
        }
    }

    /**
     * Raises the lightness of [argb] until it reaches [minContrast] against [background],
     * keeping the hue. Used for small accent text. Returns white if it never gets there.
     */
    fun liftToContrast(argb: Int, background: Int = DARK_BACKGROUND, minContrast: Double = SMALL_TEXT_CONTRAST): Int {
        if (contrastRatio(argb, background) >= minContrast) return argb or (0xFF shl 24)
        val hsl = toHsl(argb)
        var l = hsl.l
        while (l < 1f) {
            l = min(1f, l + 0.02f)
            val candidate = fromHsl(hsl.h, hsl.s, l)
            if (contrastRatio(candidate, background) >= minContrast) return candidate
        }
        return WHITE
    }

    /** Black or white text on [argb], whichever contrasts more. */
    fun onColor(argb: Int): Int =
        if (contrastRatio(WHITE, argb) >= contrastRatio(BLACK, argb)) WHITE else BLACK

    /** Perceptual-ish closeness used to avoid re-fading for tiny changes: hue within 6 degrees and lightness within 4 percent. */
    fun isNegligibleChange(a: Int, b: Int): Boolean {
        val x = toHsl(a)
        val y = toHsl(b)
        var dh = abs(x.h - y.h)
        if (dh > 180f) dh = 360f - dh
        return dh < 6f && abs(x.l - y.l) < 0.04f && abs(x.s - y.s) < 0.08f
    }

    /**
     * A colour is dull (no glow worth showing) when it has almost no chroma, or its saturation is
     * very low. Gray artwork must never produce a gray glow (STYLE_SPEC.md section 3).
     */
    fun isDull(argb: Int): Boolean = chroma(argb) < 0.08f || toHsl(argb).s < 0.20f

    /**
     * The colour picker's guard. Returns a usable accent derived from [argb], or null when the
     * colour cannot be made usable: black and dark grays have no glow, and a mid gray would read as
     * dirt. White and light grays are allowed and become the neutral accent.
     */
    fun makeUsable(argb: Int): Int? {
        val hsl = toHsl(argb)
        if (isNeutral(argb)) {
            return if (hsl.l >= 0.70f) clampAccent(argb) else null
        }
        // Almost-black or almost-white chromatic colours carry no hue worth keeping.
        if (hsl.l < 0.08f || hsl.l > 0.97f) return null
        return clampAccent(argb)
    }
}
