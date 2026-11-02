package com.music.bitchord.ui.v2.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.x contrast arithmetic on packed ARGB ints (alpha is ignored: callers pass the colour as it
 * will look once composited). Pure Kotlin so it is unit-tested on the JVM.
 */
object ContrastMath {

    /** 4.5:1 for body text, 3:1 for large text, icons and UI components. */
    const val TEXT = 4.5
    const val LARGE = 3.0

    private fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Int): Double {
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Alpha-composites [top] (with [alpha], 0..1) over the opaque [bottom]. */
    fun over(top: Int, alpha: Double, bottom: Int): Int {
        fun mix(shift: Int): Int {
            val t = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return (t * alpha + b * (1 - alpha)).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    /** Whichever of white and black reads better on [background]. */
    fun bestText(background: Int): Int =
        if (ratio(WHITE, background) >= ratio(BLACK, background)) WHITE else BLACK

    /**
     * What to do so [text] reads on a backdrop whose worst-case colour is [worstBackground]:
     * - [Guard.Fine] if it already meets [min];
     * - [Guard.Scrim] with the smallest alpha (capped at [MAX_SCRIM]) of [scrimColor] that gets it
     *   there; the scrim should be drawn under the text;
     * - [Guard.Solid] if even the capped scrim cannot, meaning the element should sit on a solid
     *   surface instead.
     */
    fun guard(text: Int, worstBackground: Int, scrimColor: Int, min: Double = TEXT): Guard {
        if (ratio(text, worstBackground) >= min) return Guard.Fine
        var alpha = 0.05
        while (alpha <= MAX_SCRIM + 1e-9) {
            if (ratio(text, over(scrimColor, alpha, worstBackground)) >= min) return Guard.Scrim(alpha)
            alpha += 0.05
        }
        return Guard.Solid
    }

    const val MAX_SCRIM = 0.72

    sealed interface Guard {
        data object Fine : Guard
        data class Scrim(val alpha: Double) : Guard
        data object Solid : Guard
    }

    /**
     * Moves [foreground] toward white or black (whichever is further from [background]) in small
     * steps until it reaches [min] against [background]. Returns it unchanged if it already does;
     * the end of the walk (pure white or black) if nothing in between does.
     */
    fun ensureContrast(foreground: Int, background: Int, min: Double = TEXT): Int {
        if (ratio(foreground, background) >= min) return foreground
        val target = if (ratio(WHITE, background) >= ratio(BLACK, background)) WHITE else BLACK
        var step = 1
        while (step <= 20) {
            val candidate = over(target, step / 20.0, foreground)
            if (ratio(candidate, background) >= min) return candidate
            step++
        }
        return target
    }
}
