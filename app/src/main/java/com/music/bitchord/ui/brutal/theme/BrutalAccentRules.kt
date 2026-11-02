package com.music.bitchord.ui.brutal.theme

import androidx.compose.runtime.Immutable

/**
 * The colours the app actually draws an accent with, derived from what the user chose
 * (spec sections 3.2 to 3.5). Everything here is pure arithmetic on ARGB ints.
 */
@Immutable
data class AccentColors(
    /** The colour the user chose, untouched. */
    val chosen: Int,
    /** Fills, strokes, rims, glow and icons: at least 3:1 on the page. */
    val ui: Int,
    /** Accent-coloured text on glass: at least 4.5:1 on the brightest glass. */
    val text: Int,
    /** Text and icons on a surface filled with [fill]: ink or white, at least 4.5:1. */
    val onAccent: Int,
    /** The fill that carries [onAccent]: [ui], moved only if neither ink nor white reached 4.5:1. */
    val fill: Int,
    /** Relative luminance of [ui], which decides [glowGain]. */
    val luminance: Double,
    /** Multiplier on the Neon glow alphas: dark accents glow less, so they get more. */
    val glowGain: Float,
    /** True if the chosen colour had to be changed to meet the contrast rules. */
    val adjusted: Boolean,
)

object BrutalAccentRules {
    /** The page: black with a hint of blue. Measured `#070708` and `#0C0C0F`; one token between them. */
    const val PAGE = 0xFF08080A.toInt()

    /** The brightest glass body measured (the transport bar). The worst case for accent text on glass. */
    const val GLASS_WORST = 0xFF2C2C2C.toInt()

    /** The usual glass body: white at alpha 0.09 over the page, measured `#1D1D1D`. */
    const val GLASS_BODY = 0xFF1D1D1D.toInt()

    /** Black on accent. Measured `#070501` on the sticker; the token is neutral. */
    const val INK = 0xFF0A0A0A.toInt()

    private const val UI_MIN = ContrastMath.LARGE
    private const val TEXT_MIN = ContrastMath.TEXT
    private const val FILL_STEP = 0.04

    fun derive(chosen: Int): AccentColors {
        val opaque = chosen or 0xFF000000.toInt()
        val white = ContrastMath.WHITE

        // 1. Fills and strokes: 3:1 on the page, lightened in 5 percent steps if not.
        val ui = ContrastMath.walk(opaque, white, ContrastMath.STEP_FINE) {
            ContrastMath.ratio(it, PAGE) >= UI_MIN
        }.color

        // 2. Accent as text on glass: 4.5:1 on the brightest glass, lightened again if not.
        val text = ContrastMath.walk(ui, white, ContrastMath.STEP_FINE) {
            ContrastMath.ratio(it, GLASS_WORST) >= TEXT_MIN
        }.color

        // 3. Text on an accent fill: ink or white, whichever is higher; if neither reaches 4.5:1,
        //    move the fill (toward white when ink wins, toward black when white wins).
        val onInk = ContrastMath.ratio(INK, ui)
        val onWhite = ContrastMath.ratio(white, ui)
        val inkWins = onInk >= onWhite
        val onAccent = if (inkWins) INK else white
        val fill = if (maxOf(onInk, onWhite) >= TEXT_MIN) {
            ui
        } else {
            val toward = if (inkWins) white else ContrastMath.BLACK
            ContrastMath.walk(ui, toward, FILL_STEP) { ContrastMath.ratio(onAccent, it) >= TEXT_MIN }.color
        }

        val luminance = ContrastMath.luminance(ui)
        return AccentColors(
            chosen = opaque,
            ui = ui,
            text = text,
            onAccent = onAccent,
            fill = fill,
            luminance = luminance,
            glowGain = glowGain(luminance),
            adjusted = ui != opaque,
        )
    }

    /** Spec 3.5: 1.0 for light accents, 1.15 for mid, 1.35 for dark ones. */
    fun glowGain(luminance: Double): Float = when {
        luminance >= 0.5 -> 1.0f
        luminance >= 0.2 -> 1.15f
        else -> 1.35f
    }

    /**
     * Spec 3.4: how strong the accent bleed may be under [text] on a [base] glass body. Starts at
     * [requested] and steps down by 0.05 until [text] on the blended colour is at least 4.5:1.
     * Returns 0 if even a faint bleed breaks it.
     */
    fun bleedAlphaCap(
        accent: Int,
        text: Int,
        requested: Double = 0.55,
        base: Int = GLASS_BODY,
    ): Double {
        var alpha = requested
        while (alpha > 1e-9) {
            if (ContrastMath.ratio(text, ContrastMath.over(accent, alpha, base)) >= TEXT_MIN) return alpha
            alpha -= 0.05
        }
        return 0.0
    }
}
