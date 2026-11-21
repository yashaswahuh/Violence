package com.music.violence.ui.violencestyle

import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Unslop (experimental): a second pass over Violence Style that takes out what makes an interface look
 * machine-made, and keeps the style's own design language: the song's accent, the serif headings, the
 * heading rule, the glass bars and the cover art as the one bright thing on a screen.
 *
 * The values and rules here come from the "unslop" skill by Damir Sokolovsky (@heydamir),
 * https://github.com/heydamir/unslop, licensed CC BY 4.0, adapted for this app. Keep this credit.
 *
 * Pure and platform-free, so the rules are tested and shared with the desktop app.
 */
object UnslopTokens {

    // ---- the neutral ramp (the skill's graphite ramp: every surface is a grey, the song supplies the colour)

    const val DARK_DEEPEST: Int = 0xFF0F0F10.toInt()
    const val DARK_PAGE: Int = 0xFF141415.toInt()
    const val DARK_CARD: Int = 0xFF19191B.toInt()
    const val DARK_RAISED: Int = 0xFF232325.toInt()
    const val DARK_TEXT: Int = 0xFFEDEDED.toInt()
    const val DARK_TEXT_SECONDARY: Int = 0xFFA1A1A6.toInt()
    const val DARK_TEXT_MUTED: Int = 0xFF85858B.toInt()

    const val LIGHT_PAGE: Int = 0xFFFAFAFA.toInt()
    const val LIGHT_CARD: Int = 0xFFFFFFFF.toInt()
    const val LIGHT_RAISED: Int = 0xFFF4F4F5.toInt()
    const val LIGHT_TEXT: Int = 0xFF171717.toInt()
    const val LIGHT_TEXT_SECONDARY: Int = 0xFF525252.toInt()
    const val LIGHT_TEXT_MUTED: Int = 0xFF737373.toInt()

    /** Hairlines are the text colour at 6 to 10 percent. */
    const val HAIRLINE_ALPHA = 0.08f

    /** Semantic colours, toned down to sit inside the ramp. */
    const val POSITIVE_DARK: Int = 0xFF4CC38A.toInt()
    const val NEGATIVE_DARK: Int = 0xFFE5484D.toInt()

    fun page(light: Boolean): Int = if (light) LIGHT_PAGE else DARK_PAGE
    fun card(light: Boolean): Int = if (light) LIGHT_CARD else DARK_CARD
    fun raised(light: Boolean): Int = if (light) LIGHT_RAISED else DARK_RAISED
    fun text(light: Boolean): Int = if (light) LIGHT_TEXT else DARK_TEXT
    fun textSecondary(light: Boolean): Int = if (light) LIGHT_TEXT_SECONDARY else DARK_TEXT_SECONDARY
    fun textMuted(light: Boolean): Int = if (light) LIGHT_TEXT_MUTED else DARK_TEXT_MUTED

    // ---- spacing: everything sits on a 4 dp grid

    const val GRID = 4

    /** The nearest step of the 4 dp grid, never below one step. */
    fun onGrid(dp: Int): Int = max(GRID, (dp / GRID.toFloat()).roundToInt() * GRID)

    // ---- radius: by size, and nested (an inner radius is the outer one less the padding between them)

    const val RADIUS_CONTROL = 10
    const val RADIUS_CARD = 16
    const val RADIUS_PANEL = 20
    const val RADIUS_MIN = 4

    fun nestedRadius(outer: Int, padding: Int): Int = max(RADIUS_MIN, outer - padding)

    // ---- motion: one curve, a few durations

    /** cubic-bezier(0.22, 1, 0.36, 1). */
    val EASING: FloatArray = floatArrayOf(0.22f, 1f, 0.36f, 1f)
    const val DURATION_FEEDBACK_MS = 150
    const val DURATION_POPOVER_MS = 200
    const val DURATION_ENTRANCE_MS = 400

    // ---- type: two weights, 600 for rare emphasis only

    const val WEIGHT_REGULAR = 400
    const val WEIGHT_MEDIUM = 500
}

/**
 * The words on screen, the way the skill's copy pass wants them: no exclamation marks, no trailing period
 * on a short label, no hype. Applied only to text that passes through the styled components, never to
 * what a person wrote (a playlist name, a lyric, a song title).
 */
object UnslopCopy {

    /** A label ends without a period; a paragraph of several sentences keeps its own. */
    fun clean(text: String): String {
        var out = text.trim()
        if (out.isEmpty()) return out
        // "Saved!" becomes "Saved"; "Done!!" the same. Both the ASCII and the full-width mark.
        out = out.trimEnd('!', '！')
        // One sentence only: a single trailing period goes. Several sentences keep theirs.
        if (out.endsWith('.') && !out.endsWith("..") && out.count { it == '.' } == 1) out = out.dropLast(1)
        return out.trim()
    }

    /** True when [text] is entirely capitals (a shouted label), so it is worth setting in sentence case. */
    fun isShouted(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        return letters.length >= 3 && letters.all { it.isUpperCase() }
    }

    /**
     * Sentence case for a shouted label ("PAID THIS MONTH" becomes "Paid this month"). Anything that is
     * already mixed case is left alone, because it may hold a name.
     */
    fun sentenceCase(text: String): String {
        if (!isShouted(text)) return text
        val lower = text.lowercase()
        val first = lower.indexOfFirst { it.isLetter() }
        return if (first < 0) lower else lower.substring(0, first) + lower[first].uppercaseChar() + lower.substring(first + 1)
    }
}
