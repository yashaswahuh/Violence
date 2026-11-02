package com.music.bitchord.ui.brutal.components

import androidx.compose.ui.graphics.toArgb
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.ContrastMath

/**
 * Every foreground-on-background pair the brutal components draw, so a test can check all of them for
 * every accent preset and for any custom colour (the accent derivation of spec 3.2 must make each
 * one pass, whatever the user picked). Text needs 4.5:1; icons, strokes and the focus ring need 3:1.
 */
data class ContrastPair(val name: String, val foreground: Int, val background: Int, val minRatio: Double) {
    val ratio: Double get() = ContrastMath.ratio(foreground, background)
    val passes: Boolean get() = ratio >= minRatio
}

fun brutalContrastPairs(p: BrutalPalette): List<ContrastPair> {
    val page = p.page.toArgb()
    val body = p.glassBody.toArgb()
    val bright = p.glassBodyBright.toArgb()
    val white = p.white.toArgb()
    val accentUi = p.accentUi.toArgb()
    val accentText = p.accentText.toArgb()
    val fill = p.accentFill.toArgb()
    val onAccent = p.onAccent.toArgb()
    val muted = p.textMuted.toArgb()
    val text = ContrastMath.TEXT
    val ui = ContrastMath.LARGE
    return listOf(
        // On the solid accent plate: sticker, tag, chip, selected tab, button label, play triangle.
        ContrastPair("sticker, tag, chip, tab, button text on the accent plate", onAccent, fill, text),
        ContrastPair("play triangle on the accent disc", onAccent, fill, ui),
        // White text: titles, the wordmark, statements.
        ContrastPair("white on the page", white, page, text),
        ContrastPair("white on the glass body", white, body, text),
        ContrastPair("white on the brightest glass", white, bright, text),
        // Accent as text: artist line, date stamp, unselected tab label, rail icons.
        ContrastPair("accent text on the page", accentText, page, text),
        ContrastPair("accent text on the glass body", accentText, body, text),
        ContrastPair("accent text on the brightest glass", accentText, bright, text),
        ContrastPair("rail icon on the brightest glass", accentText, bright, ui),
        // Muted text: statements and notes.
        ContrastPair("muted text on the page", muted, page, text),
        ContrastPair("muted text on the glass body", muted, body, text),
        // Strokes and the focus ring.
        // The focus ring is drawn in the accent-text colour (the lightened accent).
        ContrastPair("focus ring on the page", accentText, page, ui),
        ContrastPair("focus ring on the brightest glass", accentText, bright, ui),
        // Accent plates and fills against the page (the shape itself is a UI component).
        ContrastPair("accent plate on the page", accentUi, page, ui),
        // White text over the accent bleed (GLOW), at the alpha the kit allows.
        run {
            val alpha = BrutalAccentRules.bleedAlphaCap(
                accent = accentUi,
                text = ContrastMath.WHITE,
                requested = (0.55f * p.accent.glowGain).toDouble().coerceAtMost(0.80),
            )
            ContrastPair("white text over the accent bleed", white, ContrastMath.over(accentUi, alpha, BrutalAccentRules.GLASS_BODY), text)
        },
    )
}
