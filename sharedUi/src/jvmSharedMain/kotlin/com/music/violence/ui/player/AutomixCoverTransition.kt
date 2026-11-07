package com.music.violence.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

/**
 * The cover art of an Automix blend: the outgoing cover dissolves slowly into
 * the incoming one, following the blend's own progress rather than a timer, so
 * the new cover is fully there when the mix finishes.
 *
 * The dissolve is always the base. From one Automix to the next it is dressed
 * differently — see [Style] — so it does not look the same every time.
 *
 * Laid over the sleeve's own picture and opaque to it for the length of the
 * blend. At progress 0 it shows the outgoing cover at rest and at progress 1 the
 * incoming one at rest, so the hand-over to the sleeve's picture (which switches
 * song partway through the blend) is invisible. Off unless the experimental
 * switch is on, and a plain dissolve under "Reduce animation".
 */
@Composable
internal fun AutomixCoverTransition(modifier: Modifier = Modifier) {
    val enabled by PlayerSettings.automixCoverAnimation.collectAsStateWithLifecycle()
    if (!enabled) return
    val covers by PlayerSettings.coverBlend.collectAsStateWithLifecycle()
    val still by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val blend = covers ?: return
    // One look per blend, chosen when it starts and held to its end.
    val style = remember(blend) { if (still) Style.DISSOLVE else nextStyle() }
    val reported = PlayerSettings.smartMixProgress.collectAsStateWithLifecycle()
    // The blend's progress arrives in steps that do not line up with the screen's frames, which
    // is what made the motion stutter. This follows it once per frame instead: it measures how
    // fast the blend is moving, carries on at that speed between reports, and glides towards the
    // result without ever going backwards. Read in the draw layers below, not here, so a frame
    // does not recompose anything.
    val shown = remember(blend) { mutableFloatStateOf(0f) }
    LaunchedEffect(blend) {
        var lastReported = reported.value
        var lastAt = 0L
        var rate = 0f
        var current = 0f
        while (true) {
            withFrameNanos { now ->
                val value = reported.value
                if (lastAt == 0L) lastAt = now
                if (value != lastReported) {
                    val dt = (now - lastAt) / NANOS
                    if (dt > 0.001f) {
                        rate = (rate * 0.6f + ((value - lastReported) / dt) * 0.4f).coerceIn(0f, MAX_RATE)
                    }
                    lastReported = value
                    lastAt = now
                }
                val ahead = (((now - lastAt) / NANOS) * rate).coerceAtMost(MAX_AHEAD)
                val target = (lastReported + ahead).coerceIn(0f, 1f)
                current = maxOf(current, current + (target - current) * FOLLOW)
                shown.floatValue = current
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        CoverCard(blend.outgoingArtwork, style, outgoing = true) { shown.floatValue }
        CoverCard(blend.incomingArtwork, style, outgoing = false) { shown.floatValue }
    }
}

private const val NANOS = 1_000_000_000f

/** The fastest a blend is believed to move, in progress per second (a blend lasts seconds). */
private const val MAX_RATE = 1f

/** The furthest the picture runs on past the last report, as a share of the blend. */
private const val MAX_AHEAD = 0.03f

/** How much of the gap to the target is closed each frame. */
private const val FOLLOW = 0.3f

/** How the dissolve is dressed. */
private enum class Style {
    /** Nothing but the slow dissolve. */
    DISSOLVE,

    /** The dissolve while the picture slowly swells, the new cover settling back to size. */
    ZOOM,

    /** The dissolve while both covers drift a little sideways, one way out and one way in. */
    DRIFT,

    /** The dissolve with a barely-there turn as the new cover settles. */
    TWIST,
}

/** Plain dissolve twice in five, so it stays the signature look. */
private val ROTATION = listOf(Style.DISSOLVE, Style.ZOOM, Style.DRIFT, Style.DISSOLVE, Style.TWIST)
private var blendsSeen = 0

private fun nextStyle(): Style = ROTATION[blendsSeen++ % ROTATION.size]

@Composable
private fun CoverCard(artwork: String?, style: Style, outgoing: Boolean, progress: () -> Float) {
    AsyncImage(
        model = artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Eased, with a short rest at each end so nothing twitches at
                // the very edge of the blend.
                val t = smoother(((progress() - REST) / (1f - 2f * REST)).coerceIn(0f, 1f))
                // The outgoing cover stays whole underneath and the incoming one
                // dissolves in over it. Both fading at once would let the sleeve's
                // own picture show through the middle of the blend.
                alpha = if (outgoing) 1f else t
                dress(style, outgoing, t)
            },
    )
}

private fun GraphicsLayerScope.dress(style: Style, outgoing: Boolean, t: Float) {
    // 0 at rest; the outgoing cover goes from 0 to 1, the incoming one from -1 to 0.
    val away = if (outgoing) t else t - 1f
    when (style) {
        Style.DISSOLVE -> {
            // Not still while it dissolves: the old cover eases forward and away, the new one
            // settles back from just past full size.
            val s = 1f + PUSH * (if (outgoing) t else 1f - t)
            scaleX = s
            scaleY = s
        }
        Style.ZOOM -> {
            val s = 1f + ZOOM * (if (outgoing) t else 1f - t)
            scaleX = s
            scaleY = s
        }
        Style.DRIFT -> {
            translationX = -away * size.width * DRIFT
            // Grows only as far as it drifts, so it is exactly full size at rest.
            val s = 1f + DRIFT_SWELL * kotlin.math.abs(away)
            scaleX = s
            scaleY = s
        }
        Style.TWIST -> {
            val s = 1f + TWIST_SWELL * (if (outgoing) t else 1f - t)
            scaleX = s
            scaleY = s
            rotationZ = -away * TWIST
        }
    }
}

private fun smoother(x: Float) = x * x * x * (x * (x * 6f - 15f) + 10f)

/** The share of the blend, at each end, the covers hold still. */
private const val REST = 0.04f

/** How much the picture swells, as a share of its size. */
private const val ZOOM = 0.18f

/** How far the plain dissolve pushes in, as a share of the picture's size. */
private const val PUSH = 0.06f

/** How far a cover drifts sideways, as a share of the sleeve's width. */
private const val DRIFT = 0.13f

/** Enough size that a drifting cover never shows a gap at the sleeve's edge. */
private const val DRIFT_SWELL = 0.28f

private const val TWIST_SWELL = 0.13f

/** The turn, in degrees, of a cover furthest from rest. */
private const val TWIST = 5f
