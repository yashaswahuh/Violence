package com.music.violence.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    // Read in the draw layers below, not here: it changes on every tick of the blend.
    val progress = PlayerSettings.smartMixProgress.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        CoverCard(blend.outgoingArtwork, style, outgoing = true) { progress.value }
        CoverCard(blend.incomingArtwork, style, outgoing = false) { progress.value }
    }
}

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
        Style.DISSOLVE -> Unit
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
private const val ZOOM = 0.10f

/** How far a cover drifts sideways, as a share of the sleeve's width. */
private const val DRIFT = 0.07f

/** Enough size that a drifting cover never shows a gap at the sleeve's edge. */
private const val DRIFT_SWELL = 0.16f

private const val TWIST_SWELL = 0.07f

/** The turn, in degrees, of a cover furthest from rest. */
private const val TWIST = 3f
