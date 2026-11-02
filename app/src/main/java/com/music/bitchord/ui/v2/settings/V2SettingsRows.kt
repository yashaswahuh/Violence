package com.music.bitchord.ui.v2.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.icons.SfIcons
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.v2.UiVariant

/** The "New UI (experimental)" switch. Applies at once; playback and the queue are not touched. */
@Composable
fun NewUiSettingRow() {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    val on = variant == UiVariant.NEW
    val set = { value: Boolean -> AppSettings.setUiVariant(if (value) UiVariant.NEW else UiVariant.CURRENT) }
    SettingsRow(
        icon = SfIcons.AutoAwesome,
        title = stringResource(R.string.v2_new_ui_title),
        subtitle = stringResource(R.string.v2_new_ui_subtitle),
        trailing = { IosSwitch(checked = on, onCheckedChange = set) },
        onClick = { set(!on) },
    )
}

/** "Reduce transparency": every new-UI surface is solid. The app's own switch (Android has no system one). */
@Composable
fun ReduceTransparencySettingRow() {
    val on by AppSettings.reduceTransparency.collectAsStateWithLifecycle()
    SettingsRow(
        icon = SfIcons.BlurOff,
        title = stringResource(R.string.v2_reduce_transparency_title),
        subtitle = stringResource(R.string.v2_reduce_transparency_subtitle),
        trailing = { IosSwitch(checked = on, onCheckedChange = AppSettings::setReduceTransparency) },
        onClick = { AppSettings.setReduceTransparency(!on) },
    )
}
