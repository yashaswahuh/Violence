package com.music.bitchord.ui.screens

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.music.bitchord.R

/*
 * The two animations of Tap to share. Both are free LottieFiles animations (Lottie Simple
 * License), bundled in res/raw with their fixed colours already set to the app's:
 *   lottie_phone_tap.json     "Phone tap" by Mark Clarke: two phones touching, looped
 *   lottie_success_check.json "Black successful check" by BASURI AUTOMOTIVE: a check that draws itself
 * See fork-docs/SHARE_DESIGN.md for the credits and how they were recoloured.
 */

/** The phone layers of the tap animation, drawn in white in the original. */
private val PHONE_LAYERS = listOf("phone-speaker", "phone-screen", "phone-mask", "phone-outline", "phone-shine")

/**
 * Two phones touching, looping. The line art takes the theme's text colour (white in the original,
 * which would vanish on a light theme); the ripple keeps the app's accent red set in the file.
 */
@Composable
fun PhoneTapAnimation(modifier: Modifier = Modifier, playing: Boolean = true) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_phone_tap))
    val ink = MaterialTheme.colorScheme.onBackground.toArgb()
    val dynamic = rememberLottieDynamicProperties(
        *PHONE_LAYERS.flatMap { layer ->
            listOf(
                rememberLottieDynamicProperty(LottieProperty.COLOR, ink, "**", layer, "**"),
                rememberLottieDynamicProperty(LottieProperty.STROKE_COLOR, ink, "**", layer, "**"),
            )
        }.toTypedArray(),
    )
    LottieAnimation(
        composition = composition,
        modifier = modifier.aspectRatio(4f / 3f),
        iterations = LottieConstants.IterateForever,
        isPlaying = playing,
        dynamicProperties = dynamic,
    )
}

/** A green check that draws itself once, each time [play] turns true. */
@Composable
fun SuccessCheckAnimation(play: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_success_check))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = play,
        restartOnPlay = true,
        iterations = 1,
    )
    LottieAnimation(composition = composition, progress = { if (play) progress else 0f }, modifier = modifier)
}
