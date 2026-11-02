package com.music.bitchord.ui.brutal.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.music.bitchord.ui.brutal.theme.AccentColors
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.brutal.theme.ContrastMath

/**
 * Every number the glass layers are drawn with, for one kind and one Glow setting. Alphas and widths
 * are from the measurements of `BRUTAL_UI_SPEC.md` (sections 1.1 and 2.1); where the spec says
 * APPROXIMATE, so does this. Widths are dp and become pixels when the cache is built.
 *
 * With Glow off every accent field is transparent: the accent never enters the glass.
 */
@Immutable
data class GlassStyle(
    // Body (layers 2a and 2b)
    val scrimAlpha: Float,
    val bodyTopAlpha: Float,
    val bodyBottomAlpha: Float,
    /** Without a backdrop the body has to carry legibility itself: a dense dark fill. */
    val denseBody: Color,
    val solidColor: Color,
    // Gloss (layer 3)
    val glossAlpha: Float,
    val glintAlpha: Float,
    // Bevel, arcs, rim, edge (layers 6 and 7)
    val bevelDp: Float,
    val bevelTopAlpha: Float,
    val bevelBottomAlpha: Float,
    val arcTopLeftAlpha: Float,
    val arcBottomRightAlpha: Float,
    val arcTopLeftDp: Float,
    val arcBottomRightDp: Float,
    val arcMinReachDp: Float,
    val rimDp: Float,
    val rimTopAlpha: Float,
    val rimBottomAlpha: Float,
    val edgeDp: Float,
    val edgeAlpha: Float,
    // Tile frame
    val frameDp: Float,
    // Accent (GLOW only; transparent otherwise)
    val bleed: Color,
    /** The hot spot in the start corner: much stronger than the wash, away from the text. */
    val bleedHot: Color,
    val bleedEnd: Color,
    val rimTint: Color,
    val rimTintDp: Float,
    val halo: Color,
    val haloAlpha: Float,
    val haloReachDp: Float,
    /** Bands in the under-glow. Tiles sit in scrolling lists, so they get fewer. */
    val haloSteps: Int,
    val haloOffsetDp: Float,
    val hairline: Color,
)

/**
 * The style of [kind] under Glow [glow] for the accent [accent].
 *
 * The accent parts follow spec 3.5: the bleed is the accent at a peak alpha of 0.55 times its
 * `glowGain`, lowered by [BrutalAccentRules.bleedAlphaCap] so white text on it stays 4.5:1; the rim
 * tint is the accent mixed a quarter toward white; the under-glow is 0.30 times the gain.
 */
fun glassStyle(kind: GlassKind, glow: Boolean, accent: AccentColors): GlassStyle {
    val tile = kind == GlassKind.Tile
    val chip = kind == GlassKind.Chip
    val panel = kind == GlassKind.Panel
    val ui = Color(accent.ui)
    val bleedPeak = BrutalAccentRules.bleedAlphaCap(
        accent = accent.ui,
        text = ContrastMath.WHITE,
        requested = (0.55f * accent.glowGain).toDouble().coerceAtMost(0.80),
    ).toFloat()
    val rimMix = Color(ContrastMath.over(ContrastMath.WHITE, 0.25, accent.ui))
    val none = Color.Transparent
    return GlassStyle(
        scrimAlpha = ContrastMath.TEXT_SCRIM_ALPHA.toFloat(),
        bodyTopAlpha = if (chip) 0.16f else 0.13f,
        bodyBottomAlpha = if (chip) 0.10f else 0.06f,
        // Near-black and dense: with the 0.13 to 0.06 white body on top it lands on the measured #1D1D1D.
        denseBody = Color(0xFF050506).copy(alpha = 0.90f),
        solidColor = Color(BrutalAccentRules.GLASS_BODY),
        glossAlpha = if (glow) 0.18f else 0.24f,
        glintAlpha = 0.12f,
        bevelDp = when {
            chip -> 0f
            panel -> 7f
            tile -> 4f
            else -> 6f
        },
        bevelTopAlpha = 0.45f,
        bevelBottomAlpha = 0.20f,
        arcTopLeftAlpha = 1.0f,
        arcBottomRightAlpha = 0.60f,
        arcTopLeftDp = if (chip) 1.5f else if (panel) 4f else 3.5f,
        arcBottomRightDp = if (chip) 1.2f else 2.5f,
        arcMinReachDp = if (chip) 10f else 28f,
        rimDp = 1.5f,
        rimTopAlpha = 0.40f,
        rimBottomAlpha = 0.12f,
        edgeDp = 1f,
        edgeAlpha = 0.30f,
        frameDp = if (tile) 5f else 0f,
        bleed = if (glow) ui.copy(alpha = bleedPeak) else none,
        bleedHot = if (glow) ui.copy(alpha = (bleedPeak * 1.7f).coerceAtMost(0.80f)) else none,
        bleedEnd = if (glow) ui.copy(alpha = bleedPeak * 0.6f) else none,
        rimTint = if (glow) rimMix else none,
        rimTintDp = 1.5f,
        halo = if (glow) ui else none,
        haloAlpha = if (glow) (0.30f * accent.glowGain).coerceAtMost(0.50f) else 0f,
        haloReachDp = 21f,
        haloSteps = if (tile) 4 else HALO_STEPS,
        haloOffsetDp = 6f,
        hairline = if (glow) ui.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.10f),
    )
}
