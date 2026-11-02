package com.music.bitchord.ui.v2.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * One accent from a song's artwork colour, made safe: the artwork can be any colour at all, and
 * text and controls must read on the result whatever it is.
 *
 * Saturation is capped (a neon cover must not make a neon UI) and lightness is held inside a band
 * per theme; then, if the accent used as *text or an icon* on [background] would fall under 4.5:1,
 * it is walked toward the readable end ([ContrastMath.ensureContrast]). The accent used as a
 * *fill* is kept separate: [onFill] is the black or white that reads best on it, so a button is
 * legible whatever the accent turned out to be. Colours without enough colour in them (greys,
 * near-black, near-white) fall back to [fallback], the Violence accent.
 */
object ArtworkAccent {

    data class Result(
        /** The accent for fills (progress, selected pill, the orb ring). */
        val fill: Int,
        /** What reads best on [fill]. */
        val onFill: Int,
        /** The accent for text and icons on the page background; at least 4.5:1 against it. */
        val text: Int,
        /** True when [fallback] was used because the artwork had no usable colour. */
        val fromFallback: Boolean,
    )

    private const val MIN_SATURATION = 0.18f
    private const val MAX_SATURATION = 0.78f

    fun from(seed: Int?, dark: Boolean, background: Int, fallback: Int): Result {
        val hsl = seed?.let(::toHsl)
        val usable = hsl != null && hsl[1] >= MIN_SATURATION && hsl[2] in 0.08f..0.94f
        val fill = if (usable) {
            val s = min(hsl!![1], MAX_SATURATION)
            val l = if (dark) hsl[2].coerceIn(0.42f, 0.62f) else hsl[2].coerceIn(0.30f, 0.50f)
            fromHsl(hsl[0], s, l)
        } else {
            fallback
        }
        return Result(
            fill = fill,
            onFill = ContrastMath.bestText(fill),
            text = ContrastMath.ensureContrast(fill, background),
            fromFallback = !usable,
        )
    }

    /** [h] in degrees, [s] and [l] in 0..1. */
    fun toHsl(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        return floatArrayOf(if (h < 0) h + 360f else h, s, l)
    }

    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
