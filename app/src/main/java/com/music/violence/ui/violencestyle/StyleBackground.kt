package com.music.violence.ui.violencestyle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.toIntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.ui.rememberIsForeground

/*
 * The live background (STYLE_SPEC.md section 2). One layer behind the whole app, drawn only while the
 * style is on and the background is not Off.
 *
 * Primitives only: radial gradients (one unit-radius brush, moved and scaled by the canvas transform, so
 * the draw path allocates nothing), one linear gradient for the vignette, flat rects. No blur, no
 * RenderEffect, no bitmap. The clock is read in the draw phase only, so a tick redraws this node and
 * nothing else (it owns a graphicsLayer).
 */

/**
 * The background, recorded into its own [GraphicsLayer] so two places can show the same picture: the root
 * (glass off) and the glass recording layer in MainActivity (glass on, so the glass blurs the glow).
 */
class ViolenceBackgroundHolder(val graphicsLayer: GraphicsLayer) {
    /** True while the layer holds a recorded picture. A state, so the glass recording redraws when it flips. */
    var ready by mutableStateOf(false)
        internal set

    /** Draws the recorded background, if any. Called from the glass recording's floor. */
    fun drawInto(scope: ContentDrawScope) {
        if (ready) with(scope) { drawLayer(graphicsLayer) }
    }
}

/** Provided by the style host while the style is on; null otherwise (so nothing is ever drawn when off). */
val LocalViolenceBackground = staticCompositionLocalOf<ViolenceBackgroundHolder?> { null }

/** True when the style is on and a background is chosen: screens must then let it show through. */
fun StyleState.showsBackground(): Boolean = enabled && background != StyleBackground.OFF

/**
 * The one helper for "opaque page background becomes see-through": the original `background(color)`
 * unless the style shows a background, in which case the modifier is returned unchanged. With the style
 * off the result is exactly `background(color)`, as before.
 */
fun Modifier.violenceBackground(style: StyleState, color: Color): Modifier =
    if (style.showsBackground()) this else this.background(color)

/** Put once at the top of the root box, behind the app. Composes nothing unless the style shows a background. */
@Composable
fun ViolenceBackground() {
    val holder = LocalViolenceBackground.current ?: return
    val style = LocalViolenceStyle.current
    if (!style.showsBackground()) return
    val tokens = LocalViolenceAccent.current
    val foreground = rememberIsForeground()
    val playerOpen by ViolenceStyleSong.playerOpen.collectAsStateWithLifecycle()
    val running = StyleCapabilityRules.backgroundClockMayRun(style, foreground, playerOpen)
    val time = rememberBackgroundClock(running)

    val glow = tokens.glowColor
    // Quantised: the guard runs when the colour has moved visibly, not on every frame of a crossfade.
    val guardKey = glow and 0xF8F8F8
    val scales = remember(style.background, style.intensity, style.effectiveMotion, guardKey) {
        GlowGuard.scales(style.background, glow, style.intensity, style.effectiveMotion)
    }
    val brushes = remember { BackgroundBrushes() }
    val glowColor = Color(glow)
    val unit = remember(glow) { brushes.unitGlow(glowColor) }
    val config = BackgroundConfig(
        style = style.background,
        motion = style.effectiveMotion,
        intensity = style.intensity,
        flat = style.reduceTransparency,
        glow = glowColor,
        base = Color(BackgroundGeometry.baseColor(glow)),
        scales = scales,
        unit = unit,
    )
    val blobs = remember { FloatArray(BackgroundGeometry.MAX_BLOBS * BackgroundGeometry.STRIDE) }

    DisposableEffect(holder) { onDispose { holder.ready = false } }

    Box(
        Modifier
            .fillMaxSize()
            // Its own layer: a tick redraws this node only, not the page.
            .graphicsLayer { }
            .drawBehind {
                // The only state read: the clock. Everything else is fixed for this composition.
                val seconds = time.value / 1.0e9
                if (size.width < 1f || size.height < 1f) return@drawBehind
                holder.graphicsLayer.record(size.toIntSize()) {
                    drawViolenceGlow(config, seconds, blobs, brushes)
                }
                drawLayer(holder.graphicsLayer)
                if (!holder.ready) holder.ready = true
            },
    )
}

/** What one frame needs, built once per composition. */
internal class BackgroundConfig(
    val style: StyleBackground,
    val motion: StyleMotion,
    val intensity: StyleIntensity,
    val flat: Boolean,
    val glow: Color,
    val base: Color,
    val scales: FloatArray,
    val unit: Brush,
)

/** Brushes that are rebuilt only when their inputs change, never per frame. */
internal class BackgroundBrushes {
    private var vignetteHeight = -1f
    private var vignetteStart = -1f
    private var vignetteAlpha = -1f
    private var vignetteBase = Color.Unspecified
    private var vignette: Brush? = null

    private var flatHeight = -1f
    private var flatGlow = Color.Unspecified
    private var flat: Brush? = null

    /** A radial gradient of unit radius at the origin, with the shared falloff. Scaled by the canvas. */
    fun unitGlow(glow: Color): Brush {
        val stops = BackgroundGeometry.FALLOFF_STOPS
        val alphas = BackgroundGeometry.FALLOFF_ALPHAS
        return Brush.radialGradient(
            colorStops = Array(stops.size) { stops[it] to glow.copy(alpha = alphas[it]) },
            center = Offset.Zero,
            radius = 1f,
        )
    }

    fun vignette(height: Float, startFraction: Float, alpha: Float, base: Color): Brush {
        val cached = vignette
        if (cached != null && vignetteHeight == height && vignetteStart == startFraction &&
            vignetteAlpha == alpha && vignetteBase == base
        ) return cached
        val brush = Brush.verticalGradient(
            0f to base.copy(alpha = 0f),
            1f to base.copy(alpha = alpha),
            startY = startFraction * height,
            endY = height,
        )
        vignette = brush
        vignetteHeight = height; vignetteStart = startFraction; vignetteAlpha = alpha; vignetteBase = base
        return brush
    }

    /** Reduce transparency: a very dark, subtle gradient of the accent, top to 60 percent of the height. */
    fun flat(height: Float, glow: Color): Brush {
        val cached = flat
        if (cached != null && flatHeight == height && flatGlow == glow) return cached
        val brush = Brush.verticalGradient(
            0f to glow.copy(alpha = FLAT_ALPHA),
            1f to glow.copy(alpha = 0f),
            startY = 0f,
            endY = height * 0.6f,
        )
        flat = brush
        flatHeight = height; flatGlow = glow
        return brush
    }

    private companion object {
        const val FLAT_ALPHA = 0.16f
    }
}

/** One frame of the background. No allocation: brushes are cached, positions go into [blobs]. */
internal fun DrawScope.drawViolenceGlow(
    config: BackgroundConfig,
    seconds: Double,
    blobs: FloatArray,
    brushes: BackgroundBrushes,
) {
    drawRect(config.base)
    if (config.flat) {
        drawRect(brushes.flat(size.height, config.glow), size = Size(size.width, size.height * 0.6f))
        return
    }
    val count = BackgroundGeometry.fill(
        config.style, size.width, size.height, seconds, config.motion, config.intensity, config.scales, blobs,
    )
    for (i in 0 until count) {
        val o = i * BackgroundGeometry.STRIDE
        val radius = blobs[o + 2]
        translate(blobs[o], blobs[o + 1]) {
            scale(radius, radius, pivot = Offset.Zero) {
                drawRect(config.unit, topLeft = Offset(-1f, -1f), size = Size(2f, 2f), alpha = blobs[o + 3])
            }
        }
    }
    val vignette = BackgroundGeometry.vignette(config.style)
    if (vignette != null) {
        val top = vignette.startFraction * size.height
        drawRect(
            brushes.vignette(size.height, vignette.startFraction, vignette.alpha, config.base),
            topLeft = Offset(0f, top),
            size = Size(size.width, size.height - top),
        )
    }
}

/**
 * The background clock, in nanoseconds of frame time. Ticks at 30 fps while [running]; steps down to 20,
 * 10 fps and then stops if the real tick interval overruns the intended one (the device cannot hold it).
 * Stopped, the picture is simply not invalidated: an idle screen costs nothing.
 */
@Composable
private fun rememberBackgroundClock(running: Boolean): State<Long> {
    val time = remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var level = 0
        var last = 0L
        var sum = 0L
        var count = 0
        while (level < StyleFrameThrottle.STATIC_LEVEL) {
            val interval = StyleFrameThrottle.intervalNanos(level)
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                    time.longValue = now
                } else {
                    val d = now - last
                    if (d >= interval - TICK_TOLERANCE_NANOS) {
                        sum += d
                        count++
                        last = now
                        time.longValue = now
                        if (count >= StyleFrameThrottle.SAMPLE_COUNT) {
                            level = StyleFrameThrottle.nextLevel(level, sum / count)
                            sum = 0L
                            count = 0
                        }
                    }
                }
            }
        }
    }
    return time
}

private const val TICK_TOLERANCE_NANOS = 4_000_000L
