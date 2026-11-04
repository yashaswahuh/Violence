package com.music.violence.ui.violencestyle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.violence.ui.components.PAGE_GUTTER
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Pure helpers for text splitting and hairline widths. */
object ViolenceStyleText {
    const val HEADING_FONT_SCALE_CAP = 1.3f

    fun cappedFontScale(fontScale: Float): Float = min(fontScale, HEADING_FONT_SCALE_CAP)

    class Heading(val caps: String, val italic: String?)

    fun split(title: String): Heading {
        val text = title.trim()
        val at = text.indexOf(" for ", ignoreCase = true)
        if (at > 0 && at + 5 < text.length) {
            return Heading(text.substring(0, at).uppercase(), text.substring(at + 1))
        }
        return Heading(text.uppercase(), null)
    }

    fun hairlineWidth(available: Int, titleWidth: Int, actionWidth: Int, gapPx: Int, minPx: Int): Int {
        val room = available - titleWidth - actionWidth - gapPx * (if (actionWidth > 0) 2 else 1)
        return if (room >= minPx) room else 0
    }
}

val ViolenceHeroCornerDp = 26.dp

/**
 * Section header under Violence Style: display serif title, thin hairline running to the right,
 * and for hero headings a glowing accent dot at its end.
 */
@Composable
fun ViolenceSectionHeader(
    title: String,
    subtitle: String,
    onShowAll: (() -> Unit)?,
    showAllLabel: String,
    hero: Boolean,
    style: ViolenceSharedStyle,
) {
    val density = LocalDensity.current
    val capped = Density(density.density, ViolenceStyleText.cappedFontScale(density.fontScale))
    val heading = ViolenceStyleText.split(title)
    Column(
        Modifier
            .padding(start = PAGE_GUTTER + 8.dp, end = PAGE_GUTTER + 8.dp, top = 10.dp, bottom = 10.dp)
            .fillMaxWidth(),
    ) {
        val gap = 12.dp
        val minHairline = 24.dp
        val lastLine = if (hero && heading.italic != null) 40.dp else 28.dp
        Layout(
            content = {
                Column(Modifier.semantics { heading() }) {
                    CompositionLocalProvider(LocalDensity provides capped) {
                        Text(
                            text = heading.caps,
                            style = if (hero) style.displayCaps else style.sectionCaps,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (heading.italic != null) {
                            Text(
                                text = heading.italic,
                                style = if (hero) style.displayItalic else style.sectionCaps.copy(
                                    fontStyle = FontStyle.Italic,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Canvas(Modifier.heightIn(min = 12.dp)) { drawHairline(style, hero) }
                if (onShowAll != null) {
                    Box(
                        Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics { role = Role.Button }
                            .clickable(onClick = onShowAll)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = showAllLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = style.textAccent,
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { measurables, constraints ->
            val gapPx = gap.roundToPx()
            val minPx = minHairline.roundToPx()
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val action = measurables.getOrNull(2)?.measure(loose)
            val actionW = action?.width ?: 0
            val titleMax = max(0, constraints.maxWidth - actionW - (gapPx + minPx) - (if (actionW > 0) gapPx else 0))
            val titleP = measurables[0].measure(Constraints(maxWidth = titleMax))
            val lineW = ViolenceStyleText.hairlineWidth(constraints.maxWidth, titleP.width, actionW, gapPx, minPx)
            val h = max(titleP.height, action?.height ?: 0)
            val lineP = measurables[1].measure(Constraints.fixed(lineW, 12.dp.roundToPx()))
            layout(constraints.maxWidth, h) {
                titleP.placeRelative(0, (h - titleP.height) / 2)
                if (lineW > 0) {
                    val center = (h - titleP.height) / 2 + titleP.height - lastLine.roundToPx() / 2
                    lineP.placeRelative(titleP.width + gapPx, center - lineP.height / 2)
                }
                action?.placeRelative(constraints.maxWidth - actionW, (h - action.height) / 2)
            }
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = style.label.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun DrawScope.drawHairline(style: ViolenceSharedStyle, hero: Boolean) {
    val y = size.height / 2f
    val endX = if (hero) size.width - 6.dp.toPx() else size.width
    drawLine(
        color = Color.White.copy(alpha = if (hero) 0.32f else 0.24f),
        start = Offset(0f, y),
        end = Offset(endX, y),
        strokeWidth = 1.dp.toPx(),
    )
    if (hero) {
        val c = Offset(size.width - 3.dp.toPx(), y)
        drawCircle(style.accent.copy(alpha = 0.10f), radius = 12.dp.toPx(), center = c)
        drawCircle(style.accent.copy(alpha = 0.28f), radius = 7.dp.toPx(), center = c)
        drawCircle(style.accent, radius = 3.dp.toPx(), center = c)
    }
}

/** Card translucent rim and accent bloom. Returns receiver unchanged when style is null. */
fun Modifier.violenceCardRim(style: ViolenceSharedStyle?, first: Boolean, shape: Shape): Modifier {
    if (style == null) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val thin = Stroke(1.dp.toPx())
        val rimLit = first && !style.neutral
        val bloomStroke = Stroke(6.dp.toPx())
        val lightStroke = Stroke(1.2.dp.toPx())
        val bloomAlpha = 0.22f * (style.strength / 0.62f).coerceIn(0.5f, 1.4f)
        val edge = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to style.accent.copy(alpha = 0.35f),
            startY = size.height * 0.88f,
            endY = size.height,
        )
        onDrawWithContent {
            drawContent()
            strokeOutline(outline, Color.White.copy(alpha = 0.16f), style = thin)
            if (rimLit) {
                drawRect(edge, topLeft = Offset(0f, size.height * 0.88f), size = Size(size.width, size.height * 0.12f))
                strokeOutline(outline, style.accent.copy(alpha = bloomAlpha), style = bloomStroke)
                strokeOutline(outline, style.accent.copy(alpha = 0.85f), style = lightStroke)
            }
        }
    }
}

object ViolenceScrim {
    const val START = 0.35f
    const val MID = 0.62f
    const val MID_ALPHA = 0.60f
    const val END_ALPHA = 0.88f

    fun alphaAt(fraction: Float): Float = when {
        fraction <= START -> 0f
        fraction <= MID -> MID_ALPHA * (fraction - START) / (MID - START)
        else -> MID_ALPHA + (END_ALPHA - MID_ALPHA) * ((fraction - MID) / (1f - MID)).coerceAtMost(1f)
    }
}

val ViolenceCardScrim: Brush = Brush.verticalGradient(
    ViolenceScrim.START to Color.Transparent,
    ViolenceScrim.MID to Color.Black.copy(alpha = ViolenceScrim.MID_ALPHA),
    1f to Color.Black.copy(alpha = ViolenceScrim.END_ALPHA),
)

private fun DrawScope.strokeOutline(outline: Outline, color: Color, style: Stroke) {
    when (outline) {
        is Outline.Rectangle -> drawRect(color, topLeft = outline.rect.topLeft, size = outline.rect.size, style = style)
        is Outline.Rounded -> {
            val r = outline.roundRect
            drawRoundRect(
                color = color,
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
                cornerRadius = CornerRadius(r.topLeftCornerRadius.x, r.topLeftCornerRadius.y),
                style = style,
            )
        }
        is Outline.Generic -> drawPath(outline.path, color, style = style)
    }
}

fun violencePillColor(base: Color, accent: Color): Color {
    var mix = 0.35f
    while (true) {
        val c = androidx.compose.ui.graphics.lerp(base, accent, mix)
        val contrast = (1.0f + 0.05f) / (relativeLum(c) + 0.05f)
        if (contrast >= 4.5f || mix <= 0.05f) return c
        mix -= 0.05f
    }
}

private fun relativeLum(c: Color): Float {
    fun ch(v: Float) = if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * ch(c.red) + 0.7152f * ch(c.green) + 0.0722f * ch(c.blue)
}

fun violencePlayerGlowAlpha(strength: Float): Float = (0.26f * strength / 0.62f).coerceIn(0.16f, 0.36f)

fun Modifier.violencePlayerGlowFor(style: ViolenceSharedStyle?, drift: () -> Float): Modifier {
    if (style == null) return this
    val peak = violencePlayerGlowAlpha(style.strength)
    return drawWithCache {
        val unit = Brush.radialGradient(
            0f to style.accent.copy(alpha = 1f),
            0.35f to style.accent.copy(alpha = 0.55f),
            0.7f to style.accent.copy(alpha = 0.18f),
            1f to style.accent.copy(alpha = 0f),
            center = Offset.Zero,
            radius = 1f,
        )
        onDrawWithContent {
            drawContent()
            val t = drift() * 2f * Math.PI.toFloat()
            val move = if (style.motion) size.width * 0.12f else 0f
            fun blob(cx: Float, cy: Float, radius: Float, alpha: Float) {
                translate(cx, cy) {
                    scale(radius, radius, pivot = Offset.Zero) {
                        drawRect(unit, topLeft = Offset(-1f, -1f), size = Size(2f, 2f), alpha = alpha)
                    }
                }
            }
            blob(size.width * 0.12f + sin(t) * move, size.height * 0.04f + cos(t) * move * 0.6f, size.width * 0.95f, peak)
            blob(size.width * 1.0f + cos(t * 0.8f) * move * 0.6f, size.height * 0.32f + sin(t * 0.8f) * move, size.width * 0.6f, peak * 0.55f)
        }
    }
}

@Composable
fun Modifier.violencePlayerGlow(): Modifier {
    val style = LocalViolenceShared.current ?: return this
    if (!style.motion) return violencePlayerGlowFor(style) { 0f }
    val transition = rememberInfiniteTransition(label = "playerGlow")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(28_000, easing = LinearEasing),
        ),
        label = "playerGlowPhase",
    )
    return violencePlayerGlowFor(style) { phase.value }
}

fun violenceReplayColors(style: ViolenceSharedStyle): List<Color> = listOf(
    style.accent,
    androidx.compose.ui.graphics.lerp(style.accent, Color.Black, 0.40f),
    style.accentSoft,
    androidx.compose.ui.graphics.lerp(style.accent, Color.White, 0.18f),
)

object ViolenceGlassColors {
    const val CAPSULE_MAX_ALPHA = 0.32f
    const val CAPSULE_TEXT_CONTRAST = 4.5
    const val THUMB_CONTRAST = 3.0

    private fun over(base: Int, top: Int, alpha: Float): Int = AccentMath.argb(
        255,
        (AccentMath.red(base) + (AccentMath.red(top) - AccentMath.red(base)) * alpha).toInt(),
        (AccentMath.green(base) + (AccentMath.green(top) - AccentMath.green(base)) * alpha).toInt(),
        (AccentMath.blue(base) + (AccentMath.blue(top) - AccentMath.blue(base)) * alpha).toInt(),
    )

    fun capsuleAlpha(accent: Int): Float {
        var alpha = CAPSULE_MAX_ALPHA
        while (alpha > 0.06f) {
            val composite = over(ViolenceChromeColors.TINT_BASE, accent, alpha)
            if (AccentMath.contrastRatio(AccentMath.WHITE, composite) >= CAPSULE_TEXT_CONTRAST) return alpha
            alpha -= 0.04f
        }
        return 0.06f
    }

    fun capsuleFill(accent: Int): Int = AccentMath.withAlpha(accent, capsuleAlpha(accent))

    fun glowLine(accent: Int): Int {
        val hsl = AccentMath.toHsl(accent)
        return AccentMath.fromHsl(hsl.h, hsl.s, (hsl.l + 0.15f).coerceAtMost(0.85f))
    }

    fun glowStrength(intensity: StyleIntensity): Float = (intensity.peakAlpha / 0.62f).coerceIn(0.6f, 1.4f)

    const val PANEL_BASE: Int = 0xFF121214.toInt()
    const val PANEL_ACCENT_MIX = 0.08f

    fun panelTint(accent: Int): Int = AccentMath.argb(
        255,
        (AccentMath.red(PANEL_BASE) + (AccentMath.red(accent) - AccentMath.red(PANEL_BASE)) * PANEL_ACCENT_MIX).toInt(),
        (AccentMath.green(PANEL_BASE) + (AccentMath.green(accent) - AccentMath.green(PANEL_BASE)) * PANEL_ACCENT_MIX).toInt(),
        (AccentMath.blue(PANEL_BASE) + (AccentMath.blue(accent) - AccentMath.blue(PANEL_BASE)) * PANEL_ACCENT_MIX).toInt(),
    )

    fun detailGlowAlpha(intensity: StyleIntensity): Float = 0.30f * glowStrength(intensity)

    fun groupFillAlpha(reduceTransparency: Boolean): Float = if (reduceTransparency) 0.94f else 0.62f

    const val SOLID_TINT_ALPHA = 0.92f

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

object ViolenceChromeColors {
    const val TINT_BASE: Int = 0xFF101018.toInt()
    const val TINT_ACCENT_MIX = 0.12f
    const val TINT_ALPHA = 0.45f

    fun tint(accent: Int, alpha: Float = TINT_ALPHA): Int {
        val mixed = AccentMath.argb(
            255,
            mix(AccentMath.red(TINT_BASE), AccentMath.red(accent)),
            mix(AccentMath.green(TINT_BASE), AccentMath.green(accent)),
            mix(AccentMath.blue(TINT_BASE), AccentMath.blue(accent)),
        )
        return AccentMath.withAlpha(mixed, alpha)
    }

    private fun mix(a: Int, b: Int): Int = (a + (b - a) * TINT_ACCENT_MIX).toInt()
}

@Composable
fun violenceIndicatorColor(): Color? {
    val style = LocalViolenceStyle.current
    if (!style.enabled) return null
    val tokens = LocalViolenceAccent.current
    if (tokens.neutral) return null
    return remember(tokens.accent) { Color(ViolenceGlassColors.capsuleFill(tokens.accent)) }
}

@Composable
fun Modifier.violenceGlassRim(shape: Shape): Modifier {
    if (!LocalViolenceStyle.current.enabled) return this
    return violenceGlassEdge(shape, 0.75.dp, Color.Transparent)
}

fun Modifier.violenceCapsuleGlowFor(style: StyleState, tokens: AccentTokens): Modifier {
    if (!style.enabled || tokens.neutral) return this
    val strength = ViolenceGlassColors.glowStrength(style.intensity)
    val line = Color(ViolenceGlassColors.glowLine(tokens.accent))
    val accent = Color(tokens.accent)
    return drawWithCache {
        val inset = size.width * 0.14f
        val washTop = size.height * 0.55f
        val capsule = Path().apply {
            addRoundRect(
                RoundRect(
                    0f, 0f, size.width, size.height,
                    CornerRadius(size.height / 2f, size.height / 2f),
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

@Composable
fun Modifier.violenceGlassEdge(shape: Shape, width: Dp, color: Color): Modifier {
    if (!LocalViolenceStyle.current.enabled) return border(width, color, shape)
    val tokens = LocalViolenceAccent.current
    val brush = remember(tokens.accent, tokens.neutral) {
        val bottom = if (tokens.neutral) Color.White.copy(alpha = 0.14f) else Color(tokens.accent).copy(alpha = 0.14f)
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), bottom))
    }
    return border(0.75.dp, brush, shape)
}

