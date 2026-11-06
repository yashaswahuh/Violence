package com.music.violence.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.violence.data.settings.MixDeck
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The cover art of an Automix blend: as one song mixes into the next, the
 * outgoing cover drifts away and back while the incoming one glides in from the
 * other side and settles, both following the blend's own progress rather than a
 * timer, so the picture lands exactly when the sound does.
 *
 * Laid over the sleeve's own picture and opaque to it for the length of the
 * blend. At progress 0 it shows the outgoing cover at rest and at progress 1 the
 * incoming one at rest, so the hand-over to the sleeve's picture (which switches
 * song partway through the blend) is invisible. Off unless the experimental
 * switch is on, and a plain cross-fade with "Reduce animation".
 */
@Composable
internal fun AutomixCoverTransition(modifier: Modifier = Modifier) {
    val enabled by PlayerSettings.automixCoverAnimation.collectAsStateWithLifecycle()
    if (!enabled) return
    val mixing by PlayerSettings.smartMixInProgress.collectAsStateWithLifecycle()
    val view by PlayerSettings.smartMixView.collectAsStateWithLifecycle()
    val still by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val blend = view?.takeIf { mixing } ?: return
    // Read in the draw layers below, not here: it changes on every tick of the blend.
    val progress = PlayerSettings.smartMixProgress.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        // The incoming cover is drawn last, over the outgoing one, the way a
        // card is dealt onto a pile.
        CoverCard(blend.outgoing, outgoing = true, still = still, progress = { progress.value })
        CoverCard(blend.incoming, outgoing = false, still = still, progress = { progress.value })
    }
}

@Composable
private fun CoverCard(deck: MixDeck, outgoing: Boolean, still: Boolean, progress: () -> Float) {
    AsyncImage(
        model = deck.artworkUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Eased, and given a beat of rest at either end so the cover
                // does not twitch at the very edge of the blend.
                val t = smooth(((progress() - REST) / (1f - 2f * REST)).coerceIn(0f, 1f))
                if (still) {
                    // A plain cross-fade, no motion.
                    alpha = if (outgoing) 1f - t else t
                    return@graphicsLayer
                }
                // 0 at rest, then out to one side (outgoing) or in from the
                // other (incoming), with a little swell where the two cross.
                val away = if (outgoing) t else t - 1f
                val swell = 1f + BREATH * sin(PI.toFloat() * t)
                val scale = (1f - SHRINK * abs(away)) * swell
                translationX = -away * size.width * SLIDE
                rotationZ = -away * TILT
                scaleX = scale
                scaleY = scale
                alpha = if (outgoing) {
                    1f - smooth(((t - 0.30f) / 0.50f).coerceIn(0f, 1f))
                } else {
                    smooth((t / 0.55f).coerceIn(0f, 1f))
                }
            },
    )
}

private fun smooth(x: Float) = x * x * (3f - 2f * x)

/** The share of the blend, at each end, the covers hold still. */
private const val REST = 0.06f

/** How far a cover travels sideways, as a share of the sleeve's width. */
private const val SLIDE = 0.34f

/** How much smaller than at rest a cover is when it is furthest away. */
private const val SHRINK = 0.16f

/** The swell where the covers cross, as a share of the size. */
private const val BREATH = 0.03f

/** The tilt, in degrees, of a cover furthest from rest. */
private const val TILT = 5f
