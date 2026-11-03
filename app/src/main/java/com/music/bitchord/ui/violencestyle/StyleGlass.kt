package com.music.bitchord.ui.violencestyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/*
 * Stage S4: accent-tinted glass for the bars and controls. Everything here extends the existing Liquid
 * Glass (it supplies a tint and a rim to `liquidGlass`, and a glow line to the selection capsule); none of
 * it replaces the glass renderer, so upstream improvements to it carry over. Each function returns the
 * original value (null, the same Modifier) when the style is off.
 */

/** Pure colour rules, tested without Compose. */
object ViolenceGlassColors {
    /** The most opaque the selected capsule's accent fill is (over the dark glass). */
    const val CAPSULE_MAX_ALPHA = 0.32f

    /** Icons and labels on the capsule must reach this contrast (STYLE_SPEC.md; text 4.5:1). */
    const val CAPSULE_TEXT_CONTRAST = 4.5

    /** A switch track must reach this contrast against the white thumb (non-text, 3:1). */
    const val THUMB_CONTRAST = 3.0

    private fun over(base: Int, top: Int, alpha: Float): Int = AccentMath.argb(
        255,
        (AccentMath.red(base) + (AccentMath.red(top) - AccentMath.red(base)) * alpha).toInt(),
        (AccentMath.green(base) + (AccentMath.green(top) - AccentMath.green(base)) * alpha).toInt(),
        (AccentMath.blue(base) + (AccentMath.blue(top) - AccentMath.blue(base)) * alpha).toInt(),
    )

    /**
     * Alpha of the accent fill behind the selected tab: at most [CAPSULE_MAX_ALPHA], lowered until white
     * icons and labels keep [CAPSULE_TEXT_CONTRAST] over it on the dark glass. (When no alpha passes the
     * capsule falls back to a faint 6 percent tint: a solid surface rather than a failing contrast.)
     */
    fun capsuleAlpha(accent: Int): Float {
        var alpha = CAPSULE_MAX_ALPHA
        while (alpha > 0.06f) {
            val composite = over(ViolenceChromeColors.TINT_BASE, accent, alpha)
            if (AccentMath.contrastRatio(AccentMath.WHITE, composite) >= CAPSULE_TEXT_CONTRAST) return alpha
            alpha -= 0.04f
        }
        return 0.06f
    }

    /** The selected tab's fill: the accent at [capsuleAlpha]. */
    fun capsuleFill(accent: Int): Int = AccentMath.withAlpha(accent, capsuleAlpha(accent))

    /** The bright bottom line: the accent, lifted 15 points of lightness. */
    fun glowLine(accent: Int): Int {
        val hsl = AccentMath.toHsl(accent)
        return AccentMath.fromHsl(hsl.h, hsl.s, (hsl.l + 0.15f).coerceAtMost(0.85f))
    }

    /** Strength of the glow line and bloom relative to the Medium intensity. */
    fun glowStrength(intensity: StyleIntensity): Float = (intensity.peakAlpha / 0.62f).coerceIn(0.6f, 1.4f)

    /** Dark panel tint (dialogs, sheets, menus): `#121214` with 8 percent of the accent. */
    const val PANEL_BASE: Int = 0xFF121214.toInt()
    const val PANEL_ACCENT_MIX = 0.08f

    fun panelTint(accent: Int): Int = AccentMath.argb(
        255,
        (AccentMath.red(PANEL_BASE) + (AccentMath.red(accent) - AccentMath.red(PANEL_BASE)) * PANEL_ACCENT_MIX).toInt(),
        (AccentMath.green(PANEL_BASE) + (AccentMath.green(accent) - AccentMath.green(PANEL_BASE)) * PANEL_ACCENT_MIX).toInt(),
        (AccentMath.blue(PANEL_BASE) + (AccentMath.blue(accent) - AccentMath.blue(PANEL_BASE)) * PANEL_ACCENT_MIX).toInt(),
    )

    /** Peak alpha of the glow above an album or playlist page: 0.30 at Medium, 0.19 to 0.40 across the intensities. */
    fun detailGlowAlpha(intensity: StyleIntensity): Float = 0.30f * glowStrength(intensity)

    /** Opaque alpha used instead of the translucent tint when transparency is reduced. */
    const val SOLID_TINT_ALPHA = 0.92f

    /** A switch's "on" track: the accent, darkened until the white thumb keeps 3:1 against it. */
    fun switchTrack(accent: Int): Int {
        if (AccentMath.contrastRatio(AccentMath.WHITE, accent) >= THUMB_CONTRAST) return accent
        val hsl = AccentMath.toHsl(accent)
        var l = hsl.l
        while (l > 0.2f) {
            l -= 0.02f
            val c = AccentMath.fromHsl(hsl.h, hsl.s, l)
            if (AccentMath.contrastRatio(AccentMath.WHITE, c) >= THUMB_CONTRAST) return c
        }
        return AccentMath.fromHsl(hsl.h, hsl.s, 0.2f)
    }
}

/** The accent fill of the selected tab capsule, or null (the original indicator colour) when off or neutral. */
@Composable
fun violenceIndicatorColor(): Color? {
    if (!LocalViolenceStyle.current.enabled) return null
    val tokens = LocalViolenceAccent.current
    if (tokens.neutral) return null
    return remember(tokens.accent) { Color(ViolenceGlassColors.capsuleFill(tokens.accent)) }
}

/** The rim of a glass surface that had none before (the nav bar and mini player). Nothing when off. */
@Composable
fun Modifier.violenceGlassRim(shape: Shape): Modifier {
    if (!LocalViolenceStyle.current.enabled) return this
    return violenceGlassEdge(shape, 0.75.dp, Color.Transparent)
}

/**
 * The glowing line along the selected capsule's bottom edge, with a soft accent wash above it
 * (STYLE_SPEC.md section 3). Strokes and gradients only, built once per size in a draw cache. Returns
 * the receiver unchanged when the style is off, or for the neutral (White) accent whose capsule stays gray.
 */
fun Modifier.violenceCapsuleGlowFor(style: StyleState, tokens: AccentTokens): Modifier {
    if (!style.enabled || tokens.neutral) return this
    val strength = ViolenceGlassColors.glowStrength(style.intensity)
    val line = Color(ViolenceGlassColors.glowLine(tokens.accent))
    val accent = Color(tokens.accent)
    return drawWithCache {
        val inset = size.width * 0.14f
        val washTop = size.height * 0.55f
        // Everything stays inside the capsule: a plain rectangle showed as a hard-edged box under the tab.
        val capsule = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f, 0f, size.width, size.height,
                    androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                ),
            )
        }
        val wash = Brush.verticalGradient(
            0f to accent.copy(alpha = 0f),
            1f to accent.copy(alpha = 0.28f * strength),
            startY = washTop,
            endY = size.height,
        )
        val lineBrush = Brush.horizontalGradient(
            0f to line.copy(alpha = 0f),
            0.5f to line.copy(alpha = 0.95f),
            1f to line.copy(alpha = 0f),
            startX = inset,
            endX = size.width - inset,
        )
        val soft = Brush.horizontalGradient(
            0f to line.copy(alpha = 0f),
            0.5f to line.copy(alpha = 0.24f * strength),
            1f to line.copy(alpha = 0f),
            startX = inset,
            endX = size.width - inset,
        )
        val thin = 2.dp.toPx()
        val wide = 6.dp.toPx()
        onDrawWithContent {
            drawContent()
            clipPath(capsule) {
                drawRect(wash, topLeft = Offset(0f, washTop), size = Size(size.width, size.height - washTop))
                drawLine(soft, Offset(inset, size.height - wide / 2f), Offset(size.width - inset, size.height - wide / 2f), strokeWidth = wide, cap = StrokeCap.Butt)
                drawLine(lineBrush, Offset(inset, size.height - thin), Offset(size.width - inset, size.height - thin), strokeWidth = thin, cap = StrokeCap.Butt)
            }
        }
    }
}

/** [violenceCapsuleGlowFor] reading the current style and accent. */
@Composable
fun Modifier.violenceCapsuleGlow(): Modifier =
    violenceCapsuleGlowFor(LocalViolenceStyle.current, LocalViolenceAccent.current)

/** The "on" track colour of a switch: the accent while the style is on, null (the original green) otherwise. */
@Composable
fun violenceSwitchTrack(): Color? {
    if (!LocalViolenceStyle.current.enabled) return null
    val accent = LocalViolenceAccent.current.accent
    return remember(accent) { Color(ViolenceGlassColors.switchTrack(accent)) }
}

/**
 * Tab semantics for screen readers while the style is on: role Tab and `selected`, so the selected
 * destination is announced by more than its colour. Off: the original modifier, nothing added.
 */
@Composable
fun Modifier.violenceTabSemantics(selected: Boolean): Modifier {
    if (!LocalViolenceStyle.current.enabled) return this
    return semantics {
        role = Role.Tab
        this.selected = selected
    }
}

/** The dark, accent-tinted tint of a dialog, sheet or menu panel; null (the original tint) when the style is off. */
@Composable
fun violencePanelTint(): Color? {
    if (!LocalViolenceStyle.current.enabled) return null
    val accent = LocalViolenceAccent.current.accent
    return remember(accent) { Color(ViolenceGlassColors.panelTint(accent)) }
}

/** True while the style is on and transparency is reduced: panels use their near-solid alpha. */
@Composable
fun violencePanelSolid(): Boolean {
    val s = LocalViolenceStyle.current
    return s.enabled && s.reduceTransparency
}

/** The colour of text buttons in dialogs and sheets under the style (the lightened accent); null when off. */
@Composable
fun violenceButtonColor(): Color? {
    if (!LocalViolenceStyle.current.enabled) return null
    val accent = LocalViolenceAccent.current
    return remember(accent.textAccent) { Color(accent.textAccent) }
}

/**
 * The glow above an album, playlist or artist page (owner decision: "glow above the artwork", with its own
 * switch). A static accent wash from the top edge over the page's own artwork colour: no clock, one cached
 * brush. Returns the receiver unchanged when the style or the switch is off.
 */
fun Modifier.violenceDetailGlowFor(style: StyleState, tokens: AccentTokens): Modifier {
    if (!style.enabled || !style.detailGlow) return this
    val alpha = ViolenceGlassColors.detailGlowAlpha(style.intensity)
    val glow = Color(tokens.glowColor)
    return drawWithCache {
        val brush = Brush.radialGradient(
            0f to glow.copy(alpha = alpha),
            0.5f to glow.copy(alpha = alpha * 0.45f),
            1f to glow.copy(alpha = 0f),
            center = Offset(size.width / 2f, 0f),
            radius = size.width * 1.1f,
        )
        onDrawBehind { drawRect(brush) }
    }
}

@Composable
fun Modifier.violenceDetailGlow(): Modifier =
    violenceDetailGlowFor(LocalViolenceStyle.current, LocalViolenceAccent.current)
