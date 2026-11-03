package com.music.bitchord.ui.brutal.glass

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.theme.LocalBrutalMotion
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.components.liquidGlass
import com.music.bitchord.ui.components.rememberPanelGlass

/**
 * The glass of the brutal look (spec section 2), built **on top of** the app's own glass so its
 * performance fixes carry over:
 *
 * - [GlassKind.Floating] at FULL is the app's [liquidGlass] (live backdrop, blur, lens, highlight)
 *   with the new layers drawn over it.
 * - [GlassKind.Panel] at FULL is the app's own panel glass (`rememberPanelGlass`: a still of the
 *   page, taken once and shared) with the new layers over it.
 * - [GlassKind.Tile] and [GlassKind.Chip] never sample anything: gradients and strokes only.
 * - SIMPLIFIED is the same layers over a dense dark body, with no capture and no lens.
 * - SOLID is an opaque fill with a hairline (the accent hairline and a static bleed with Glow).
 *
 * The new layers are `drawRect` and `drawPath` calls with brushes and strokes built **once per
 * size** (`drawWithCache`). There is no extra `RenderEffect`, no extra graphics layer and nothing
 * allocated in the draw pass. Glow's strength is read from a `State` in the draw pass, so it fades
 * on a change of the setting without recomposing anything.
 *
 * [shape] must be a [CornerBasedShape]: the lens shader throws for any other. A continuous-corner
 * shape (the app's `ContinuousCornerShape`, which the brutal tokens use for tiles and bars) is one.
 *
 * With Glow off ([glow] false) the accent never enters the glass.
 *
 * [textScrim] puts the black scrim of spec 7.4 behind the content, so white text on it is at least
 * 4.5:1 over any backdrop. Set it false for icon-only surfaces that sit over the plain page.
 */
@Composable
fun Modifier.brutalGlass(
    shape: CornerBasedShape,
    kind: GlassKind,
    glow: Boolean = LocalBrutalPalette.current.glow,
    textScrim: Boolean = kind == GlassKind.Floating || kind == GlassKind.Panel,
    level: GlassLevel = LocalGlassLevel.current,
): Modifier {
    val palette = LocalBrutalPalette.current
    val inList = LocalGlassInScrollingList.current
    val wantsLive = level == GlassLevel.FULL && kind == GlassKind.Floating && !inList
    val liveGranted = rememberGlassSlot(LocalGlassBudget.current, wantsLive)
    // The app's panel glass acquires the page still while this surface is on screen. It answers
    // null while glass is off or there is no still, in which case the panel is drawn SIMPLIFIED.
    val panelGlass = if (kind == GlassKind.Panel && level == GlassLevel.FULL && !inList) rememberPanelGlass(shape) else null
    val effective = effectiveLevel(
        requested = level,
        kind = kind,
        liveSlotGranted = liveGranted,
        stillAvailable = panelGlass != null,
        inScrollingList = inList,
    )
    val plan = remember(kind, effective, glow, textScrim) { planGlass(kind, effective, glow, textScrim) }
    LocalGlassPlanObserver.current?.let { observer -> SideEffect { observer(kind, plan) } }
    val style = remember(kind, glow, palette.accent) { glassStyle(kind, glow, palette.accent) }
    val strength = animateFloatAsState(
        targetValue = if (glow) 1f else 0f,
        animationSpec = LocalBrutalMotion.current.fade(),
        label = "brutalGlassGlow",
    )
    val layers = remember(plan, style, shape, strength) { Modifier.glassLayers(plan, style, shape, strength) }
    val halo = remember(plan.halo, style, shape, strength) {
        if (plan.halo) Modifier.glassHalo(style, shape, strength) else Modifier
    }
    val base = when (plan.backdrop) {
        // The app's own live glass; its own tint is replaced by this kit's body (transparent here).
        BackdropSource.LIVE -> Modifier.liquidGlass(shape = shape, surfaceTint = Color.Transparent)
        BackdropSource.STILL -> panelGlass ?: Modifier.clip(shape)
        BackdropSource.NONE -> Modifier.clip(shape)
    }
    return this.then(halo).then(base).then(layers)
}

/**
 * True inside [BrutalGlassListScope]: a surface in a scrolling list. A Floating surface there is
 * drawn SIMPLIFIED whatever it was asked for, so a live backdrop can never end up inside a lazy
 * list item (it would be recorded per item and cost a capture each).
 */
val LocalGlassInScrollingList = compositionLocalOf { false }

/** Wrap the content of a lazy list (or any scrolling container) with this. Components built on [GlassKind.Tile] do not need it. */
@Composable
fun BrutalGlassListScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassInScrollingList provides true, content = content)
}

/**
 * Tests (and the perf harness) can watch every decision: called after composition with the kind and
 * the plan a surface ended up with. Null, and free, in the app.
 */
val LocalGlassPlanObserver = staticCompositionLocalOf<((GlassKind, GlassPlan) -> Unit)?> { null }

// ---------------------------------------------------------------------------------------------
// The live-surface budget
// ---------------------------------------------------------------------------------------------

/**
 * A live slot in the [GlassBudget], taken when the surface first composes and given back when it
 * leaves the composition, including when its composition is thrown away unused (`onAbandoned`).
 */
private class BudgetSlot(private val budget: GlassBudget, wants: Boolean) : RememberObserver {
    val granted: Boolean = wants && budget.tryAcquire()

    override fun onRemembered() = Unit
    override fun onForgotten() { if (granted) budget.release() }
    override fun onAbandoned() { if (granted) budget.release() }
}

/** Whether this surface got a live slot. `false` when it does not want one. */
@Composable
private fun rememberGlassSlot(budget: GlassBudget, wants: Boolean): Boolean =
    remember(budget, wants) { BudgetSlot(budget, wants) }.granted

// ---------------------------------------------------------------------------------------------
// The layers
// ---------------------------------------------------------------------------------------------

/**
 * Everything a surface draws that depends on its size, built once per size: the shape as a path, the
 * brushes and the stroke widths. Immutable after construction.
 */
internal class GlassDrawCache(
    val path: Path,
    val innerPath: Path?,
    val frameInsetPx: Float,
    val tint: Brush?,
    val gloss: Brush?,
    val glint: Brush?,
    val bleedLinear: Brush?,
    val bleedHot: Brush?,
    val bleedEnd: Brush?,
    val bevel: Brush?,
    val bevelStroke: Stroke,
    val rim: Brush?,
    val rimStroke: Stroke,
    val rimTint: Brush?,
    val rimTintStroke: Stroke,
    val arcTopLeft: Brush?,
    val arcTopLeftStroke: Stroke,
    val arcBottomRight: Brush?,
    val arcBottomRightStroke: Stroke,
    val frameRing: Brush?,
    val frameStroke: Stroke,
    val edgeStroke: Stroke,
    val hairlineStroke: Stroke,
    val frameInnerStroke: Stroke,
)

internal fun buildGlassCache(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection,
    shape: CornerBasedShape,
    plan: GlassPlan,
    style: GlassStyle,
): GlassDrawCache = with(density) {
    val w = size.width
    val h = size.height
    val white = Color.White
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, density)) }
    val radii = cornerRadiiPx(shape, size, density, layoutDirection)
    val bleedOn = plan.bleed || plan.staticBleed
    val bleed = if (bleedOn) bleedSpec(size) else null
    val gloss = if (plan.gloss) glossLine(size) else null
    val frameInset = if (plan.frame) style.frameDp.dp.toPx() else 0f
    val minReach = style.arcMinReachDp.dp.toPx()
    // Small surfaces (round buttons, rail tabs) get a lighter bleed so the wash does not flood them.
    val bleedScale = bleedScaleFor(minOf(w, h) / density.density)
    fun scaled(c: Color) = c.copy(alpha = c.alpha * bleedScale)

    GlassDrawCache(
        path = path,
        innerPath = if (plan.frame && w > 2f * frameInset && h > 2f * frameInset) {
            Path().apply { addOutline(shape.createOutline(Size(w - 2f * frameInset, h - 2f * frameInset), layoutDirection, density)) }
        } else {
            null
        },
        frameInsetPx = frameInset,
        tint = if (plan.tint) {
            Brush.verticalGradient(listOf(white.copy(alpha = style.bodyTopAlpha), white.copy(alpha = style.bodyBottomAlpha)), 0f, h)
        } else {
            null
        },
        gloss = gloss?.let { (start, end) ->
            Brush.linearGradient(listOf(white.copy(alpha = style.glossAlpha), Color.Transparent), start, end)
        },
        // The bubble highlight in the start cap: an elliptical sheen near the top-left corner.
        glint = if (plan.glint && radii.topLeft > 0f) {
            Brush.radialGradient(
                listOf(white.copy(alpha = style.glintAlpha), Color.Transparent),
                Offset(radii.topLeft * 1.05f, h * 0.30f),
                radii.topLeft * 0.95f,
            )
        } else {
            null
        },
        bleedLinear = bleed?.let { Brush.linearGradient(listOf(scaled(style.bleed), Color.Transparent), it.linearStart, it.linearEnd) },
        bleedHot = bleed?.let {
            Brush.radialGradient(listOf(scaled(style.bleedHot), Color.Transparent), it.hotCenter, it.hotRadius)
        },
        bleedEnd = bleed?.let {
            Brush.radialGradient(listOf(scaled(style.bleedEnd), Color.Transparent), it.endCenter, it.endRadius)
        },
        bevel = if (plan.bevel && style.bevelDp > 0f) {
            Brush.verticalGradient(
                0f to white.copy(alpha = style.bevelTopAlpha),
                0.5f to white.copy(alpha = 0.06f),
                1f to white.copy(alpha = style.bevelBottomAlpha),
                startY = 0f,
                endY = h,
            )
        } else {
            null
        },
        bevelStroke = round(2f * style.bevelDp.dp.toPx()),
        rim = if (plan.rim) {
            Brush.verticalGradient(listOf(white.copy(alpha = style.rimTopAlpha), white.copy(alpha = style.rimBottomAlpha)), 0f, h)
        } else {
            null
        },
        // The rim sits just inside the dark edge line, so its stroke is widened by the edge's share.
        rimStroke = round(2f * (style.rimDp + style.edgeDp).dp.toPx()),
        rimTint = if (plan.rimTint) {
            Brush.verticalGradient(listOf(style.rimTint.copy(alpha = 0.35f), style.rimTint), 0f, h)
        } else {
            null
        },
        rimTintStroke = round(2f * (style.rimTintDp + style.edgeDp).dp.toPx()),
        // Top-left: bright, thick, centred on the bounding corner so it fades along both edges.
        arcTopLeft = if (plan.arcs) {
            val reach = arcReachPx(radii.topLeft, size, minReach)
            Brush.radialGradient(
                0f to white.copy(alpha = style.arcTopLeftAlpha),
                0.45f to white.copy(alpha = style.arcTopLeftAlpha * 0.4f),
                1f to Color.Transparent,
                center = Offset.Zero,
                radius = reach,
            )
        } else {
            null
        },
        arcTopLeftStroke = round(2f * style.arcTopLeftDp.dp.toPx()),
        // Bottom-right: the same, softer and thinner.
        arcBottomRight = if (plan.arcs) {
            val reach = arcReachPx(radii.bottomRight, size, minReach)
            Brush.radialGradient(
                0f to white.copy(alpha = style.arcBottomRightAlpha),
                0.45f to white.copy(alpha = style.arcBottomRightAlpha * 0.4f),
                1f to Color.Transparent,
                center = Offset(w, h),
                radius = reach,
            )
        } else {
            null
        },
        arcBottomRightStroke = round(2f * style.arcBottomRightDp.dp.toPx()),
        frameRing = if (plan.frame) {
            Brush.verticalGradient(listOf(white.copy(alpha = 0.32f), white.copy(alpha = 0.12f)), 0f, h)
        } else {
            null
        },
        frameStroke = round(2f * frameInset),
        edgeStroke = round(2f * style.edgeDp.dp.toPx()),
        hairlineStroke = round(2f * 1.dp.toPx()),
        frameInnerStroke = round(1.dp.toPx()),
    )
}

/** A round-joined stroke; built in the cache, never in the draw pass. */
private fun round(width: Float): Stroke = Stroke(width = width, join = StrokeJoin.Round)

/**
 * The layers drawn over the backdrop and under the content (surface), and over the content
 * (front), clipped to the shape by whatever precedes this modifier. [strength] is the Glow
 * strength, read here in the draw pass.
 */
private fun Modifier.glassLayers(
    plan: GlassPlan,
    style: GlassStyle,
    shape: CornerBasedShape,
    strength: State<Float>,
): Modifier = drawWithCache {
    val cache = buildGlassCache(size, this, layoutDirection, shape, plan, style)
    onDrawWithContent {
        drawGlassSurface(cache, plan, style, strength.value)
        drawContent()
        drawGlassFront(cache, plan, style, strength.value)
    }
}

internal fun DrawScope.drawGlassSurface(c: GlassDrawCache, plan: GlassPlan, style: GlassStyle, glow: Float) {
    if (plan.solidFill) drawRect(style.solidColor)
    if (plan.scrim) drawRect(Color.Black.copy(alpha = style.scrimAlpha))
    // Without a backdrop the body carries legibility itself: a dense dark fill under the gradient.
    if (plan.tint && !plan.samplesBackdrop) drawRect(style.denseBody)
    c.tint?.let { drawRect(it) }
    c.gloss?.let { drawRect(it) }
    c.glint?.let { drawRect(it) }
    if (plan.bleed) {
        c.bleedLinear?.let { drawRect(it, alpha = glow) }
        c.bleedHot?.let { drawRect(it, alpha = glow) }
        c.bleedEnd?.let { drawRect(it, alpha = glow) }
    } else if (plan.staticBleed) {
        // SOLID with Glow: the same light, static and half as strong.
        c.bleedLinear?.let { drawRect(it, alpha = 0.5f) }
        c.bleedHot?.let { drawRect(it, alpha = 0.5f) }
    }
}

internal fun DrawScope.drawGlassFront(c: GlassDrawCache, plan: GlassPlan, style: GlassStyle, glow: Float) {
    if (plan.hairline) {
        drawPath(c.path, color = style.hairline, style = c.hairlineStroke)
        return
    }
    // Frame ring over the artwork's edge, then a dark line on its inner edge.
    c.frameRing?.let { drawPath(c.path, brush = it, style = c.frameStroke) }
    c.innerPath?.let { inner ->
        translate(c.frameInsetPx, c.frameInsetPx) {
            drawPath(inner, color = Color.Black.copy(alpha = 0.35f), style = c.frameInnerStroke)
        }
    }
    c.bevel?.let { drawPath(c.path, brush = it, style = c.bevelStroke) }
    c.rim?.let { drawPath(c.path, brush = it, style = c.rimStroke) }
    if (plan.edge) drawPath(c.path, color = Color.Black.copy(alpha = style.edgeAlpha), style = c.edgeStroke)
    c.rimTint?.let { drawPath(c.path, brush = it, alpha = glow, style = c.rimTintStroke) }
    c.arcTopLeft?.let { drawPath(c.path, brush = it, style = c.arcTopLeftStroke) }
    c.arcBottomRight?.let { drawPath(c.path, brush = it, style = c.arcBottomRightStroke) }
}

/** The under-glow's path, bands and strokes, built once per size. */
internal class GlassHaloCache(val path: Path, val steps: List<HaloStep>, val strokes: List<Stroke>, val offsetPx: Float)

internal fun buildHaloCache(size: Size, density: Density, layoutDirection: LayoutDirection, shape: CornerBasedShape, style: GlassStyle): GlassHaloCache =
    with(density) {
        val steps = haloSteps(style.haloAlpha, style.haloReachDp.dp.toPx(), style.haloSteps)
        GlassHaloCache(
            path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, density)) },
            steps = steps,
            strokes = steps.map { round(it.widthPx) },
            offsetPx = style.haloOffsetDp.dp.toPx(),
        )
    }

/** Strokes outside the shape, nudged down, with the shape itself cut out so it never tints the glass from behind. */
internal fun DrawScope.drawGlassHalo(c: GlassHaloCache, style: GlassStyle, strength: Float) {
    if (strength <= 0f) return
    clipPath(c.path, ClipOp.Difference) {
        translate(top = c.offsetPx) {
            for (i in c.steps.indices) {
                drawPath(c.path, color = style.halo, alpha = c.steps[i].alpha * strength, style = c.strokes[i])
            }
        }
    }
}

/**
 * The under-glow: [haloSteps] strokes outside the shape (see [drawGlassHalo]). Drawn *behind*, outside
 * the surface's own clip, so it needs a parent that does not clip to bounds.
 */
private fun Modifier.glassHalo(style: GlassStyle, shape: CornerBasedShape, strength: State<Float>): Modifier =
    drawWithCache {
        val cache = buildHaloCache(size, this, layoutDirection, shape, style)
        onDrawBehind { drawGlassHalo(cache, style, strength.value) }
    }
