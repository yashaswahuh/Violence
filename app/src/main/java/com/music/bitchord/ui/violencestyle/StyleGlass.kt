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
            drawRect(wash, topLeft = Offset(inset, washTop), size = Size(size.width - 2 * inset, size.height - washTop))
            drawLine(soft, Offset(inset, size.height - wide / 2f), Offset(size.width - inset, size.height - wide / 2f), strokeWidth = wide, cap = StrokeCap.Round)
            drawLine(lineBrush, Offset(inset, size.height - thin), Offset(size.width - inset, size.height - thin), strokeWidth = thin, cap = StrokeCap.Round)
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
