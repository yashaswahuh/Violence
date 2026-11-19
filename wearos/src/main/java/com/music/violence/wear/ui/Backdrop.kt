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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The player's scene: the cover, only lightly blurred and a little
 * oversaturated, filling the screen — you can still make out the picture, but
 * it never competes with the controls.
 *
 * [blur] is already softened (see PhoneLink), so this is one bitmap draw and a
 * cached scrim; nothing here runs a GPU blur. The phone's mesh sits underneath,
 * which is what shows for the moment before a new cover arrives. A dark band
 * at the top and bottom, where the text and the pills sit, and a vignette round
 * the edge keep everything legible on any cover, bright ones included.
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
                    alpha = 0.92f,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.25f) }),
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
                        0f to Color.Black.copy(alpha = 0.62f),
                        0.38f to Color.Black.copy(alpha = 0.38f),
                        1f to Color.Black.copy(alpha = 0.72f),
                    )
                    val vignette = Brush.radialGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
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
 * The playhead as a wave round the play button — Material 3 Expressive's
 * wavy progress, as on the Pixel Watch. The wave ripples while the music plays
 * and settles flat when it pauses, on a spring. While the button is a
 * scrubber it shows the target and stays flat.
 *
 * Drawn from state read only in the draw pass: a tick or a ripple frame
 * redraws one path and recomposes nothing. The ripple runs at about twenty
 * frames a second, only while playing, on screen and not in ambient mode.
 */
@Composable
fun WavyRing(
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
        val stroke = size.minDimension * (4f / 128f)
        val wave = size.minDimension * (3f / 128f)
        val radius = size.minDimension / 2f - wave - stroke / 2f - size.minDimension * (1f / 128f)
        val fraction = (scrubFraction ?: progress.value).coerceIn(0f, 1f)
        val sweep = 360f * fraction
        if (sweep < 0.5f) return@Canvas
        val path = wavyArcPath(
            cx = size.width / 2f,
            cy = size.height / 2f,
            radius = radius,
            amplitude = wave * amplitude.value,
            sweepDeg = sweep,
            startDeg = -90f,
            waves = RING_WAVES,
            phase = phase.floatValue,
        )
        drawPath(
            path = path,
            color = if (scrubFraction != null) Color.White else color,
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * An arc that ripples: [amplitude] either side of [radius], [waves] radians of
 * ripple per degree of arc, growing in over the first few degrees so it does not start
 * with a kink. A polyline at 1.5 degrees a step, which at these sizes reads as
 * a curve.
 */
internal fun wavyArcPath(
    cx: Float,
    cy: Float,
    radius: Float,
    amplitude: Float,
    sweepDeg: Float,
    startDeg: Float,
    waves: Float,
    phase: Float,
): Path {
    val path = Path()
    var deg = 0f
    var first = true
    while (true) {
        val d = min(deg, sweepDeg)
        val theta = Math.toRadians((startDeg + d).toDouble())
        val taper = (d / 10f).coerceAtMost(1f)
        // [waves] is in radians per degree.
        val r = radius + amplitude * taper * sin(d * waves + phase)
        val x = cx + r * cos(theta).toFloat()
        val y = cy + r * sin(theta).toFloat()
        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }
        if (d >= sweepDeg) break
        deg += ARC_STEP
    }
    return path
}

/**
 * A scalloped circle: [lobes] bumps, each [amplitude] (a fraction of the
 * radius) tall. The play button while the music plays; with an amplitude of
 * zero it is a plain circle, which is what it relaxes into on pause.
 */
internal fun cookiePath(cx: Float, cy: Float, radius: Float, lobes: Int, amplitude: Float): Path {
    val path = Path()
    // Sized so the tips of the lobes, not the valleys, touch [radius].
    val base = radius / (1f + amplitude)
    var deg = 0
    while (deg <= 360) {
        val th = Math.toRadians(deg.toDouble())
        val r = base * (1f + amplitude * cos(lobes * th).toFloat())
        val x = cx + r * cos(th - Math.PI / 2).toFloat()
        val y = cy + r * sin(th - Math.PI / 2).toFloat()
        if (deg == 0) path.moveTo(x, y) else path.lineTo(x, y)
        deg += 2
    }
    path.close()
    return path
}

/** [cookiePath] as a clip / background shape that fills its bounds. */
class CookieShape(private val lobes: Int, private val amplitude: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(cookiePath(size.width / 2f, size.height / 2f, size.minDimension / 2f, lobes, amplitude))
}

/** Ripples round the play button's ring: 22 round a full circle, in radians per degree. */
private val RING_WAVES = (22 * 2 * Math.PI / 360).toFloat()

private val TWO_PI = (2 * Math.PI).toFloat()
private const val RIPPLE_STEP = 0.32f
private const val RIPPLE_FRAME_MS = 50L
private const val ARC_STEP = 1.5f
