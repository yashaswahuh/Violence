package com.music.violence.ui.violencestyle

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.violence.ui.components.PAGE_GUTTER
import kotlin.math.max
import kotlin.math.min

/*
 * Violence Style (experimental), the shared half of stage S3: the styled section header, the card rim
 * and scrim, and the pure helpers behind them. :sharedUi cannot see the phone app, so the app provides
 * everything through [LocalViolenceShared]. The default is null: with the style off, or in a host that
 * never provides it (the desktop app), every shared component renders exactly what it always did.
 */

/** Colours, heading text styles and strengths the shared components need, provided by the phone app. */
@Immutable
class ViolenceSharedStyle(
    val accent: Color,
    val accentSoft: Color,
    val textAccent: Color,
    /** The neutral (White) accent: no coloured rim light. */
    val neutral: Boolean,
    /** Peak glow alpha of the chosen intensity (0.38, 0.62, 0.85); scales the rim bloom. */
    val strength: Float,
    /** "TOP PICKS": display serif, caps. */
    val displayCaps: TextStyle,
    /** "for You": the italic of the same family. */
    val displayItalic: TextStyle,
    /** "RECENTS". */
    val sectionCaps: TextStyle,
    /** Card title in caps. */
    val cardTitleCaps: TextStyle,
    /** Card label and subtitle (the body face). */
    val label: TextStyle,
    /** True when the live background may move (motion not Off, FULL mode, no reduce motion). */
    val motion: Boolean = false,
)

/** Null while the style is off. */
val LocalViolenceShared = compositionLocalOf<ViolenceSharedStyle?> { null }

/** Pure helpers, unit-tested from the phone app's test source set. */
object ViolenceStyleText {

    /** The most the display serif scales with the system font size (STYLE_SPEC.md section 5). */
    const val HEADING_FONT_SCALE_CAP = 1.3f

    fun cappedFontScale(fontScale: Float): Float = min(fontScale, HEADING_FONT_SCALE_CAP)

    /**
     * Splits a heading into its upright caps part and an optional italic second part, as the mockups
     * set "TOP PICKS" over "for You": at the word "for" (only when something precedes and follows it).
     * Everything else is a single caps line.
     */
    class Heading(val caps: String, val italic: String?)

    fun split(title: String): Heading {
        val text = title.trim()
        val at = text.indexOf(" for ", ignoreCase = true)
        if (at > 0 && at + 5 < text.length) {
            return Heading(text.substring(0, at).uppercase(), text.substring(at + 1))
        }
        return Heading(text.uppercase(), null)
    }

    /** Hairline length that is left for the decoration, or 0 when it would be shorter than [minPx]. */
    fun hairlineWidth(available: Int, titleWidth: Int, actionWidth: Int, gapPx: Int, minPx: Int): Int {
        val room = available - titleWidth - actionWidth - gapPx * (if (actionWidth > 0) 2 else 1)
        return if (room >= minPx) room else 0
    }
}

/** Corner radius of a hero card under the style (STYLE_SPEC.md: about 26 dp). */
val ViolenceHeroCornerDp = 26.dp

/**
 * The mockups' section header: a display-serif title, a thin hairline running to the right, and for the
 * hero heading a glowing dot at its end; the original "Show all" kept, with a 48 dp target. The
 * hairline is decoration: it shortens, then disappears, before the title is ever squeezed, and it is
 * not announced by screen readers. The title is a TalkBack heading. The display serif scales at most
 * 1.3x with the system font size.
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
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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

/** The hairline, and for the hero heading the glowing dot with two halos. Plain strokes and circles, no blur. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHairline(style: ViolenceSharedStyle, hero: Boolean) {
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

/**
 * The card's translucent rim and, on the first card, the accent rim light with its bloom and the accent
 * edge along the bottom (STYLE_SPEC.md section 3). Strokes only (no blur), drawn from a cache that is
 * rebuilt only when the size or the style changes. Returns the receiver unchanged when [style] is null.
 */
fun Modifier.violenceCardRim(
    style: ViolenceSharedStyle?,
    glowFraction: () -> Float,
    shape: Shape,
): Modifier {
    if (style == null) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val thin = Stroke(1.dp.toPx())
        val bloomStroke = Stroke(6.dp.toPx())
        val lightStroke = Stroke(1.2.dp.toPx())
        val bloomAlpha = 0.22f * (style.strength / 0.62f).coerceIn(0.5f, 1.4f)
        val edgeBrush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to style.accent.copy(alpha = 0.35f),
            startY = size.height * 0.88f,
            endY = size.height,
        )
        onDrawWithContent {
            drawContent()
            strokeOutline(outline, Color.White.copy(alpha = 0.16f), style = thin)
            if (!style.neutral) {
                val fraction = glowFraction().coerceIn(0f, 1f)
                if (fraction > 0.001f) {
                    val edge = if (fraction >= 0.999f) {
                        edgeBrush
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to style.accent.copy(alpha = 0.35f * fraction),
                            startY = size.height * 0.88f,
                            endY = size.height,
                        )
                    }
                    drawRect(edge, topLeft = Offset(0f, size.height * 0.88f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.12f))
                    strokeOutline(outline, style.accent.copy(alpha = bloomAlpha * fraction), style = bloomStroke)
                    strokeOutline(outline, style.accent.copy(alpha = 0.85f * fraction), style = lightStroke)
                }
            }
        }
    }
}

fun Modifier.violenceCardRim(style: ViolenceSharedStyle?, first: Boolean, shape: Shape): Modifier =
    violenceCardRim(style, { if (first) 1f else 0f }, shape)

/** The scrim's stops (height fraction, black alpha), shared by the brush and the contrast tests. */
object ViolenceScrim {
    const val START = 0.35f
    const val MID = 0.62f
    const val MID_ALPHA = 0.60f
    const val END_ALPHA = 0.88f

    /** Black alpha at [fraction] of the card height, piecewise linear through the stops. */
    fun alphaAt(fraction: Float): Float = when {
        fraction <= START -> 0f
        fraction <= MID -> MID_ALPHA * (fraction - START) / (MID - START)
        else -> MID_ALPHA + (END_ALPHA - MID_ALPHA) * ((fraction - MID) / (1f - MID)).coerceAtMost(1f)
    }
}

/** The bottom scrim every hero card gets, dark enough for white text over the brightest artwork. */
val ViolenceCardScrim: Brush = Brush.verticalGradient(
    ViolenceScrim.START to Color.Transparent,
    ViolenceScrim.MID to Color.Black.copy(alpha = ViolenceScrim.MID_ALPHA),
    1f to Color.Black.copy(alpha = ViolenceScrim.END_ALPHA),
)

/** Strokes a shape's outline (rounded rectangle, rectangle or any path) in [color]. */
private fun DrawScope.strokeOutline(outline: Outline, color: Color, style: Stroke) {
    when (outline) {
        is Outline.Rectangle -> drawRect(color, topLeft = outline.rect.topLeft, size = outline.rect.size, style = style)
        is Outline.Rounded -> {
            val r = outline.roundRect
            drawRoundRect(
                color = color,
                topLeft = Offset(r.left, r.top),
                size = androidx.compose.ui.geometry.Size(r.width, r.height),
                cornerRadius = CornerRadius(r.topLeftCornerRadius.x, r.topLeftCornerRadius.y),
                style = style,
            )
        }
        is Outline.Generic -> drawPath(outline.path, color, style = style)
    }
}

/**
 * The selected segment of a chip row under the style: [base] mixed toward the [accent] (35 percent), the
 * mix lowered until white labels keep 4.5:1 against it. Pure, so the phone app's tests cover it.
 */
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

/** Peak alpha of the glow behind the full player: 0.16 to 0.36, scaled by the chosen intensity. */
fun violencePlayerGlowAlpha(strength: Float): Float = (0.26f * strength / 0.62f).coerceIn(0.16f, 0.36f)

/**
 * The accent glow behind the full player (stage S5 follow-up): two soft radial blobs over the artwork
 * backdrop, kept to the top and right edges so the controls and text are not under it. [drift] is read in
 * the draw phase (0..1, one slow loop); a constant gives a still glow. One cached unit brush, moved and
 * scaled by the canvas, nothing allocated per frame. Returns the receiver unchanged when [style] is null.
 */
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
                        drawRect(unit, topLeft = Offset(-1f, -1f), size = androidx.compose.ui.geometry.Size(2f, 2f), alpha = alpha)
                    }
                }
            }
            blob(size.width * 0.12f + kotlin.math.sin(t) * move, size.height * 0.04f + kotlin.math.cos(t) * move * 0.6f, size.width * 0.95f, peak)
            blob(size.width * 1.0f + kotlin.math.cos(t * 0.8f) * move * 0.6f, size.height * 0.32f + kotlin.math.sin(t * 0.8f) * move, size.width * 0.6f, peak * 0.55f)
        }
    }
}

/** [violencePlayerGlowFor] with one slow 28 s loop (only while the style allows motion). */
@Composable
fun Modifier.violencePlayerGlow(): Modifier {
    val style = LocalViolenceShared.current ?: return this
    if (!style.motion) return violencePlayerGlowFor(style) { 0f }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "playerGlow")
    val phase: androidx.compose.runtime.State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable<Float>(
            animation = androidx.compose.animation.core.tween<Float>(28_000, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "playerGlowPhase",
    )
    return violencePlayerGlowFor(style) { phase.value }
}

/**
 * The four mesh colours of a Replay card under the style: the app accent, a deeper shade, a softer shade and
 * a lighter one, so a card reads as lit by the accent whatever its cover. Pure, unit-tested.
 */
fun violenceReplayColors(style: ViolenceSharedStyle): List<Color> = listOf(
    style.accent,
    androidx.compose.ui.graphics.lerp(style.accent, Color.Black, 0.40f),
    style.accentSoft,
    androidx.compose.ui.graphics.lerp(style.accent, Color.White, 0.18f),
)
