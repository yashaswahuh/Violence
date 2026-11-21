package com.music.violence.ui.violencestyle

/*
 * Unslop (experimental): a polish pass over Violence Style, in the manner of the "unslop" skill's polish mode:
 * it fixes only the craft details that read as machine-made and keeps the style as it is. The glow, the
 * glass, the heading rule and its star, and the headings are the app's own and are never touched.
 *
 * What it does: tidies copy (below), sets the weights at 400 and 500, makes figures line up, and puts
 * heading spacing on the 4 dp grid.
 *
 * The rules come from the "unslop" skill by Damir Sokolovsky (@heydamir),
 * https://github.com/heydamir/unslop, licensed CC BY 4.0, adapted for this app. Keep this credit.
 */
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
