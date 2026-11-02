package com.music.bitchord.ui.brutal.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * WCAG 2.x contrast arithmetic on packed ARGB ints (alpha is ignored: callers pass the colour as it
 * will look once composited). Pure Kotlin, so it is unit-tested on the JVM.
 */
object ContrastMath {

    /** 4.5:1 for body text; 3:1 for large text, icons and UI components (WCAG 1.4.3 and 1.4.11). */
    const val TEXT = 4.5
    const val LARGE = 3.0

    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()

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

    /** Alpha-composites [top] (with [alpha], 0..1) over the opaque [bottom], in sRGB space. */
    fun over(top: Int, alpha: Double, bottom: Int): Int {
        fun mix(shift: Int): Int {
            val t = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return (t * alpha + b * (1 - alpha)).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /** Whichever of white and black reads better on [background]. */
    fun bestText(background: Int): Int =
        if (ratio(WHITE, background) >= ratio(BLACK, background)) WHITE else BLACK

    /**
     * What to do so [text] reads on a backdrop whose worst-case colour is [worstBackground]:
     * - [Guard.Fine] if it already meets [min];
     * - [Guard.Scrim] with the smallest alpha (capped at [MAX_SCRIM]) of [scrimColor] that gets it
     *   there; the scrim is drawn under the text;
     * - [Guard.Solid] if even the capped scrim cannot, meaning the element sits on a solid surface.
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

    /**
     * The black scrim that sits in front of the backdrop and behind the text on every text-bearing
     * glass surface (spec 7.4). It is computed once, not per frame: over any backdrop, including
     * pure white, white text on it is at least [TEXT]:1 (asserted in `BrutalContrastTest`).
     */
    const val TEXT_SCRIM_ALPHA = 0.55

    sealed interface Guard {
        data object Fine : Guard
        data class Scrim(val alpha: Double) : Guard
        data object Solid : Guard
    }

    /**
     * Moves [foreground] toward white or black (whichever reads better on [background]) in 5 percent
     * steps until it reaches [min] against [background]. Unchanged if it already does; the end of
     * the walk (pure white or black) if nothing in between does.
     */
    fun ensureContrast(foreground: Int, background: Int, min: Double = TEXT): Int {
        if (ratio(foreground, background) >= min) return foreground
        val target = bestText(background)
        return walk(foreground, target, STEP_FINE) { ratio(it, background) >= min }.color
    }

    /** One walk of a colour toward [target]: the colour reached and the steps taken. */
    data class Walk(val color: Int, val steps: Int)

    /**
     * Mixes [start] toward [target] by [stepFraction] per step (compounding, in floating point so
     * the result does not drift with rounding) until [done] holds or [MAX_STEPS] steps have been
     * taken. Zero steps if [done] already holds.
     */
    fun walk(start: Int, target: Int, stepFraction: Double, done: (Int) -> Boolean): Walk {
        var r = ((start shr 16) and 0xFF).toDouble()
        var g = ((start shr 8) and 0xFF).toDouble()
        var b = (start and 0xFF).toDouble()
        val tr = ((target shr 16) and 0xFF).toDouble()
        val tg = ((target shr 8) and 0xFF).toDouble()
        val tb = (target and 0xFF).toDouble()
        fun pack() = (0xFF shl 24) or (r.roundToInt().coerceIn(0, 255) shl 16) or
            (g.roundToInt().coerceIn(0, 255) shl 8) or b.roundToInt().coerceIn(0, 255)
        var steps = 0
        while (!done(pack()) && steps < MAX_STEPS) {
            r += (tr - r) * stepFraction
            g += (tg - g) * stepFraction
            b += (tb - b) * stepFraction
            steps++
        }
        return Walk(pack(), steps)
    }

    const val STEP_FINE = 0.05
    const val MAX_STEPS = 60
}
