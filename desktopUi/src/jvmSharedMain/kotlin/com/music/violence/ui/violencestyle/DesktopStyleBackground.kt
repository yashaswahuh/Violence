package com.music.violence.ui.violencestyle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Desktop Violence Style live background (DW-4).
 *
 * Renders Aurora, Corner Glow, or Haze radial gradient blobs using Compose Canvas / Skia hardware
 * acceleration. Respects the 30 fps clock throttle cap, stepping down or pausing when motion is OFF
 * to consume zero idle CPU. Falls back to a subtle flat gradient when reduceTransparency is enabled.
 */
@Composable
fun DesktopViolenceBackground(
    style: StyleState,
    tokens: AccentTokens,
    modifier: Modifier = Modifier,
) {
    if (!style.enabled || style.background == StyleBackground.OFF) return

    val motion = style.effectiveMotion
    val running = motion != StyleMotion.OFF
    val time = rememberBackgroundClock(running)

    val glow = tokens.glowColor
    val guardKey = glow and 0xF8F8F8
    val scales = remember(style.background, style.intensity, motion, guardKey) {
        GlowGuard.scales(style.background, glow, style.intensity, motion)
    }

    val brushes = remember { DesktopBackgroundBrushes() }
    val glowColor = remember(glow) { Color(glow) }
    val baseColor = remember(glow) { Color(BackgroundGeometry.baseColor(glow)) }
    val unit = remember(glow) { brushes.unitGlow(glowColor) }
    val blobs = remember { FloatArray(BackgroundGeometry.MAX_BLOBS * BackgroundGeometry.STRIDE) }

    val config = remember(style.background, motion, style.intensity, style.reduceTransparency, glowColor, baseColor, scales, unit) {
        DesktopBackgroundConfig(
            style = style.background,
            motion = motion,
            intensity = style.intensity,
            flat = style.reduceTransparency || style.capability == StyleCapability.LITE,
            glow = glowColor,
            base = baseColor,
            scales = scales,
            unit = unit,
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val seconds = if (running) time.value / 1.0e9 else 0.0
                if (size.width < 1f || size.height < 1f) return@drawBehind
                drawDesktopGlow(config, seconds, blobs, brushes)
            },
    )
}

internal class DesktopBackgroundConfig(
    val style: StyleBackground,
    val motion: StyleMotion,
    val intensity: StyleIntensity,
    val flat: Boolean,
    val glow: Color,
    val base: Color,
    val scales: FloatArray,
    val unit: Brush,
)

internal class DesktopBackgroundBrushes {
    private var vignetteHeight = -1f
    private var vignetteStart = -1f
    private var vignetteAlpha = -1f
    private var vignetteBase = Color.Unspecified
    private var vignette: Brush? = null

    private var flatHeight = -1f
    private var flatGlow = Color.Unspecified
    private var flat: Brush? = null

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

    fun flat(height: Float, glow: Color): Brush {
        val cached = flat
        if (cached != null && flatHeight == height && flatGlow == glow) return cached
        val brush = Brush.verticalGradient(
            0f to glow.copy(alpha = 0.16f),
            1f to glow.copy(alpha = 0f),
            startY = 0f,
            endY = height * 0.6f,
        )
        flat = brush
        flatHeight = height; flatGlow = glow
        return brush
    }
}

internal fun DrawScope.drawDesktopGlow(
    config: DesktopBackgroundConfig,
    seconds: Double,
    blobs: FloatArray,
    brushes: DesktopBackgroundBrushes,
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
                val tick = computeDesktopClockTick(last, now, interval)
                if (tick != null) {
                    val (newLast, delta) = tick
                    if (last != 0L) {
                        val d = now - last
                        sum += d
                        count++
                        if (count >= StyleFrameThrottle.SAMPLE_COUNT) {
                            level = StyleFrameThrottle.nextLevel(level, sum / count)
                            sum = 0L
                            count = 0
                        }
                    }
                    last = newLast
                    time.longValue += delta
                }
            }
        }
    }
    return time
}

internal fun computeDesktopClockTick(
    last: Long,
    now: Long,
    interval: Long,
    toleranceNanos: Long = TICK_TOLERANCE_NANOS,
): Pair<Long, Long>? {
    if (last == 0L) {
        return Pair(now, 0L)
    }
    val d = now - last
    if (d >= interval - toleranceNanos) {
        return Pair(now, minOf(d, interval * 2L))
    }
    return null
}

private const val TICK_TOLERANCE_NANOS = 4_000_000L

