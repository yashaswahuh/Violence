package com.music.bitchord.ui.v2

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.v2.safemode.UiSafeMode
import com.music.bitchord.ui.v2.theme.V2Theme

/**
 * The one place the app root decides which look to wear. With the variant CURRENT this is a
 * pass-through (plus the one-time "went back to classic" notice, shown in the current theme);
 * with NEW it wraps [content] in [V2Theme] and runs the safe-mode marker.
 *
 * It sits *inside* the existing theme and glass providers, so the new theme only overrides what it
 * has to, and *around* the whole app, so the media controller, view model, navigator and every
 * dialog stay in the same composition when the variant changes: nothing is recreated, and playback,
 * the queue, Jam and downloads are untouched by a switch.
 */
@Composable
fun V2Root(dark: Boolean, content: @Composable () -> Unit) {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    val enabled = variant == UiVariant.NEW
    if (enabled) {
        // Marker set as the new UI begins to compose; cleared when its first frame is on screen.
        remember { UiSafeMode.begin() }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            withFrameNanos { }
            UiSafeMode.shown()
        }
    }
    V2Theme(enabled = enabled, dark = dark, content = content)
    var notice by remember { mutableStateOf(UiSafeMode.consumeRevertNotice()) }
    if (notice) {
        AlertDialog(
            onDismissRequest = { notice = false },
            title = { Text(stringResource(R.string.v2_safe_mode_title)) },
            text = { Text(stringResource(R.string.v2_safe_mode_body)) },
            confirmButton = {
                TextButton(onClick = { notice = false }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }
}

/** Whether the New UI is on; also puts the placeholder Home back whenever the variant changes. */
@Composable
fun rememberNewUi(): Boolean {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    val on = variant == UiVariant.NEW
    LaunchedEffect(on) { com.music.bitchord.ui.v2.chrome.V2HomeState.classic = false }
    return on
}
