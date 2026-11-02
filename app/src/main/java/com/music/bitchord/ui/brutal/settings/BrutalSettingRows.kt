package com.music.bitchord.ui.brutal.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.BrutalRailSide
import com.music.bitchord.ui.brutal.UiVariant
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.icons.SfIcons
import com.music.bitchord.ui.screens.ROW_INSET
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsRow

/** One Settings row: the words somebody might search for it with, and what it draws. */
class BrutalSettingRow(val keywords: Array<String>, val content: @Composable () -> Unit)

/**
 * The rows the brutal look adds at the top of Settings, Advanced ("Advanced & experimental
 * options"), in order: the New UI switch, and, only while it is on, Glow, Accent colour, Rail side
 * and Reduce transparency. Settings draws each one through its own `row(...)` call, so they search
 * and divide like every other row; all the code behind them lives here.
 */
@Composable
fun brutalSettingRows(): List<BrutalSettingRow> {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    val on = variant == UiVariant.BRUTAL
    val newUi = stringResource(R.string.brutal_new_ui_title)
    val glow = stringResource(R.string.brutal_glow_title)
    val accent = stringResource(R.string.brutal_accent_title)
    val rail = stringResource(R.string.brutal_rail_side_title)
    val transparency = stringResource(R.string.brutal_reduce_transparency_title)
    return buildList {
        add(BrutalSettingRow(arrayOf(newUi, "new ui", "experimental", "brutal", "brutalism", "custom ui")) { NewUiSettingRow() })
        if (on) {
            add(BrutalSettingRow(arrayOf(glow, "neon", "glass", "new ui")) { GlowSettingRow() })
            add(BrutalSettingRow(arrayOf(accent, "colour", "color", "yellow", "theme", "new ui")) { AccentSettingRow() })
            add(BrutalSettingRow(arrayOf(rail, "left", "right", "left-handed", "navigation", "new ui")) { RailSideSettingRow() })
            add(BrutalSettingRow(arrayOf(transparency, "glass", "solid", "blur", "new ui")) { ReduceTransparencySettingRow() })
        }
    }
}

/** The "New UI (experimental)" switch. Applies at once; playback, queue, Jam and downloads are not touched. */
@Composable
fun NewUiSettingRow() {
    val variant by AppSettings.uiVariant.collectAsStateWithLifecycle()
    val on = variant == UiVariant.BRUTAL
    val set = { value: Boolean -> AppSettings.setUiVariant(if (value) UiVariant.BRUTAL else UiVariant.CURRENT) }
    SettingsRow(
        icon = SfIcons.AutoAwesome,
        title = stringResource(R.string.brutal_new_ui_title),
        subtitle = stringResource(R.string.brutal_new_ui_subtitle),
        trailing = { IosSwitch(checked = on, onCheckedChange = set) },
        onClick = { set(!on) },
    )
}

/** "Glow": the Neon look on glass surfaces (accent bleed, rim tint, under-glow). */
@Composable
fun GlowSettingRow() {
    val on by AppSettings.brutalGlow.collectAsStateWithLifecycle()
    SettingsRow(
        icon = SfIcons.Brightness,
        title = stringResource(R.string.brutal_glow_title),
        subtitle = stringResource(R.string.brutal_glow_subtitle),
        trailing = { IosSwitch(checked = on, onCheckedChange = AppSettings::setBrutalGlow) },
        onClick = { AppSettings.setBrutalGlow(!on) },
    )
}

/** "Accent colour": shows the current one and opens [BrutalAccentSheet]. */
@Composable
fun AccentSettingRow() {
    val accent by AppSettings.brutalAccent.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    SettingsRow(
        icon = SfIcons.Gradient,
        title = stringResource(R.string.brutal_accent_title),
        subtitle = stringResource(R.string.brutal_accent_subtitle),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = accentName(accent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Spacer(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(accent.argb)),
                )
            }
        },
        onClick = { open = true },
    )
    if (open) BrutalAccentSheet(onDismiss = { open = false })
}

/** "Rail side": left or right edge for the navigation rail (left-handed use). */
@Composable
fun RailSideSettingRow() {
    val side by AppSettings.brutalRailSide.collectAsStateWithLifecycle()
    Column {
        SettingsRow(
            icon = SfIcons.Reorder,
            title = stringResource(R.string.brutal_rail_side_title),
            subtitle = stringResource(R.string.brutal_rail_side_subtitle),
        )
        SegmentedControl(
            options = listOf(stringResource(R.string.brutal_rail_left), stringResource(R.string.brutal_rail_right)),
            selectedIndex = BrutalRailSide.entries.indexOf(side),
            onSelect = { AppSettings.setBrutalRailSide(BrutalRailSide.entries[it]) },
            modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
        )
    }
}

/** "Reduce transparency": every new-UI surface is solid. The app's own switch (Android has no system one). */
@Composable
fun ReduceTransparencySettingRow() {
    val on by AppSettings.reduceTransparency.collectAsStateWithLifecycle()
    SettingsRow(
        icon = SfIcons.BlurOff,
        title = stringResource(R.string.brutal_reduce_transparency_title),
        subtitle = stringResource(R.string.brutal_reduce_transparency_subtitle),
        trailing = { IosSwitch(checked = on, onCheckedChange = AppSettings::setReduceTransparency) },
        onClick = { AppSettings.setReduceTransparency(!on) },
    )
}

/** The display name of an accent: the preset's name, or "Custom". */
@Composable
fun accentName(accent: BrutalAccent): String = when (accent) {
    is BrutalAccent.Custom -> stringResource(R.string.brutal_accent_custom)
    is BrutalAccent.Preset -> stringResource(accent.preset.nameRes())
}

fun AccentPreset.nameRes(): Int = when (this) {
    AccentPreset.YELLOW -> R.string.brutal_accent_yellow
    AccentPreset.RED -> R.string.brutal_accent_red
    AccentPreset.BLUE -> R.string.brutal_accent_blue
    AccentPreset.GREEN -> R.string.brutal_accent_green
    AccentPreset.ORANGE -> R.string.brutal_accent_orange
    AccentPreset.PINK -> R.string.brutal_accent_pink
    AccentPreset.CYAN -> R.string.brutal_accent_cyan
    AccentPreset.WHITE -> R.string.brutal_accent_white
}
