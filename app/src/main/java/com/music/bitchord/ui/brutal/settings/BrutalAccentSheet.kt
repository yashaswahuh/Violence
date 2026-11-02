package com.music.bitchord.ui.brutal.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.Hsv
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.components.GlassBottomSheet
import com.music.bitchord.ui.icons.SfIcons

/**
 * The accent colour sheet: the eight preset swatches, and a custom colour built from three sliders
 * (hue, saturation, brightness; no library). A preset applies the moment it is tapped; a custom
 * colour applies on "Use this colour", so dragging a slider does not re-theme the whole app on every
 * frame. The preview shows what the app will actually use, after the readability rules of the spec
 * (section 3.2), so a colour that had to be lightened is visibly so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrutalAccentSheet(onDismiss: () -> Unit) {
    val accent by AppSettings.brutalAccent.collectAsStateWithLifecycle()
    GlassBottomSheet(onDismissRequest = onDismiss) {
        BrutalAccentPicker(current = accent, onPick = AppSettings::setBrutalAccent)
    }
}

/** The sheet's content, separate from the sheet so it can be drawn and tested on its own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrutalAccentPicker(current: BrutalAccent, onPick: (BrutalAccent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.brutal_accent_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(R.string.brutal_accent_presets))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccentPreset.entries.forEach { preset ->
                val label = stringResource(preset.nameRes())
                AccentSwatch(
                    argb = preset.argb,
                    label = label,
                    selected = current == BrutalAccent.Preset(preset),
                    onClick = { onPick(BrutalAccent.Preset(preset)) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.brutal_accent_custom))
        var hsv by remember(current) { mutableStateOf(Hsv.fromArgb(current.argb)) }
        val chosen = hsv.toArgb()
        LabeledSlider(
            label = stringResource(R.string.brutal_accent_hue),
            value = hsv.hue,
            range = 0f..360f,
            onChange = { hsv = hsv.copy(hue = it) },
        )
        LabeledSlider(
            label = stringResource(R.string.brutal_accent_saturation),
            value = hsv.saturation,
            range = 0f..1f,
            onChange = { hsv = hsv.copy(saturation = it) },
        )
        LabeledSlider(
            label = stringResource(R.string.brutal_accent_brightness),
            value = hsv.value,
            range = 0f..1f,
            onChange = { hsv = hsv.copy(value = it) },
        )
        Spacer(Modifier.height(8.dp))
        AccentPreview(chosen)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPick(BrutalAccent.fromArgb(chosen)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(stringResource(R.string.brutal_accent_use_custom))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** A 48 dp round swatch: the target is the swatch itself, selected is a white ring and a tick. */
@Composable
private fun AccentSwatch(argb: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val onColor = Color(BrutalAccentRules.derive(argb).onAccent)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(argb))
            .then(if (selected) Modifier.border(BorderStroke(3.dp, Color.White), CircleShape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(SfIcons.Check, contentDescription = null, tint = onColor, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = label },
        )
    }
}

/** What the app will actually draw with: a fill with its text, accent text on glass, and the stroke colour. */
@Composable
private fun AccentPreview(chosenArgb: Int) {
    val derived = remember(chosenArgb) { BrutalAccentRules.derive(chosenArgb) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PreviewTile(background = Color(derived.fill), text = "Aa", textColor = Color(derived.onAccent))
            PreviewTile(
                background = Color(BrutalAccentRules.GLASS_WORST),
                text = "Aa",
                textColor = Color(derived.text),
            )
            Box(
                Modifier
                    .size(48.dp)
                    .border(BorderStroke(3.dp, Color(derived.ui)), RoundedCornerShape(14.dp)),
            )
        }
        if (derived.adjusted) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.brutal_accent_adjusted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewTile(background: Color, text: String, textColor: Color) {
    Box(
        Modifier
            .size(width = 72.dp, height = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = textColor)
    }
}
