package com.music.bitchord.ui.brutal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.safemode.BrutalSafeMode
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.BrutalTheme

/**
 * The one place the app root decides which look to wear. With the variant [UiVariant.CURRENT] this is
 * a pass-through (plus the one-time "went back to the current UI" notice); with
 * [UiVariant.BRUTAL] it also runs the safe-mode marker and the theme carries the new values.
 *
 * It sits *inside* the existing theme and glass providers, so the brutal theme overrides only what
 * it has to, and *around* the whole app, so the media controller, view model, navigator and every
 * dialog stay in the same composition when the variant changes: nothing is recreated, and playback,
 * the queue, Jam and downloads are untouched by a switch. [content] is called from exactly one
 * place ([BrutalTheme]), in both variants; only provided values differ.
 */
@Composable
fun BrutalRoot(dark: Boolean, content: @Composable () -> Unit) {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    val enabled = variant == UiVariant.BRUTAL
    if (enabled) {
        // Marker set as the new UI begins to compose; cleared when its first frame is on screen.
        remember { BrutalSafeMode.begin() }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            withFrameNanos { }
            BrutalSafeMode.shown()
        }
    }
    BrutalTheme(enabled = enabled, dark = dark, content = content)
    var notice by remember { mutableStateOf(BrutalSafeMode.consumeRevertNotice()) }
    if (notice) {
        AlertDialog(
            onDismissRequest = { notice = false },
            title = { Text(stringResource(R.string.brutal_safe_mode_title)) },
            text = { Text(stringResource(R.string.brutal_safe_mode_body)) },
            confirmButton = {
                TextButton(onClick = { notice = false }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }
}

/** Whether the brutal look is on. */
@Composable
fun rememberBrutalUi(): Boolean {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    return variant == UiVariant.BRUTAL
}

/**
 * The dark flag the app runs with: the user's theme choice, except that the brutal look is dark only
 * in this first version (spec open question 3), so it forces dark while it is on. With the current
 * UI this returns [chosenDark] unchanged.
 */
@Composable
fun brutalEffectiveDark(chosenDark: Boolean): Boolean = chosenDark || rememberBrutalUi()

/**
 * The colour the window and the glass backdrop are painted with before any page: the page colour of
 * the active look. [classic] is returned unchanged with the current UI.
 */
@Composable
fun brutalWindowBackground(classic: Color): Color = if (rememberBrutalUi()) BrutalPalette.Page else classic
