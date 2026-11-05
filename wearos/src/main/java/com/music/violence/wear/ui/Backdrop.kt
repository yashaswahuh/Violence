package com.music.violence.wear.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * The player's scene: the cover, blurred and a little oversaturated, filling
 * the screen — visible, but never competing with the controls.
 *
 * [blur] is already blurred (see PhoneLink), so this is one bitmap draw and a
 * cached scrim; nothing here runs a GPU blur. The phone's mesh sits underneath,
 * which is what shows for the moment before a new cover arrives. A darker band
 * at the top and bottom, and a vignette round the edge, keep the title and
 * the controls legible on any cover, bright ones included.
 */
@Composable
fun ArtBackdrop(blur: ImageBitmap?, colors: List<Color>, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        MeshBackground(colors)
        Crossfade(targetState = blur, animationSpec = tween(700), label = "backdrop") { art ->
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.94f,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.35f) }),
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val bands = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.50f),
                        0.32f to Color.Black.copy(alpha = 0.16f),
                        0.62f to Color.Black.copy(alpha = 0.12f),
                        1f to Color.Black.copy(alpha = 0.66f),
                    )
                    val vignette = Brush.radialGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.62f,
                    )
                    onDrawBehind {
                        drawRect(bands)
                        drawRect(vignette)
                    }
                },
        )
    }
}

/**
 * The playhead as a wave along the bezel — Material 3 Expressive's wavy
 * progress, on a round screen. The wave ripples while the music plays and
 * settles flat when it pauses, on a spring. A gap at twelve o'clock keeps it
 * clear of the curved clock. While the play button is a scrubber it shows the
 * target, in white, and stays flat.
 *
 * Drawn from state read only in the draw pass: a tick or a ripple frame
 * redraws one path and recomposes nothing. The ripple runs at about twenty
 * frames a second, only while playing, on screen and not in ambient mode.
 */
@Composable
fun WavyBezel(
    progress: State<Float>,
    scrubMs: Long?,
    durationMs: Long,
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val scrubFraction = if (scrubMs != null && durationMs > 0) scrubMs.toFloat() / durationMs else null
    val amplitude = animateFloatAsState(
        if (playing && scrubFraction == null) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "wave",
    )
    val phase = remember { mutableFloatStateOf(0f) }
    val ambient = LocalAmbient.current.active
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(playing, ambient) {
        if (!playing || ambient) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                phase.floatValue = (phase.floatValue + RIPPLE_STEP) % TWO_PI
                delay(RIPPLE_FRAME_MS)
            }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val wave = WAVE_AMPLITUDE.toPx() * amplitude.value
        val inset = 4.dp.toPx() + WAVE_AMPLITUDE.toPx() + stroke / 2f
        val radius = minOf(size.width, size.height) / 2f - inset
        val centre = Offset(size.width / 2f, size.height / 2f)
        val start = -90f + EDGE_GAP / 2f
        val span = 360f - EDGE_GAP

        // The track: a quiet, flat arc.
        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = start,
            sweepAngle = span,
            useCenter = false,
            topLeft = Offset(centre.x - radius, centre.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(stroke * 0.6f, cap = StrokeCap.Round),
        )

        val fraction = (scrubFraction ?: progress.value).coerceIn(0f, 1f)
        val sweep = span * fraction
        if (sweep < 0.5f) return@Canvas
        val path = Path()
        var deg = 0f
        var first = true
        while (true) {
            val d = minOf(deg, sweep)
            val theta = Math.toRadians((start + d).toDouble())
            // The wave grows in over the first few degrees, so it does not
            // start with a kink.
            val taper = (d / 10f).coerceAtMost(1f)
            val r = radius + wave * taper * sin(d * WAVES_PER_DEGREE + phase.floatValue)
            val x = centre.x + r * cos(theta).toFloat()
            val y = centre.y + r * sin(theta).toFloat()
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            if (d >= sweep) break
            deg += ARC_STEP
        }
        drawPath(
            path = path,
            color = if (scrubFraction != null) Color.White else color,
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Degrees left open at the top of the bezel ring, under the clock. */
private const val EDGE_GAP = 44f

/** Peak of the ripple, either side of the ring. */
private val WAVE_AMPLITUDE = 2.6.dp

/** 36 ripples round the full circle, in radians per degree. */
private val WAVES_PER_DEGREE = (36 * 2 * Math.PI / 360).toFloat()

private val TWO_PI = (2 * Math.PI).toFloat()
private const val RIPPLE_STEP = 0.32f
private const val RIPPLE_FRAME_MS = 50L
private const val ARC_STEP = 1.5f
