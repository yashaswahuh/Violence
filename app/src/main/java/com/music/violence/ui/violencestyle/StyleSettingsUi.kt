package com.music.violence.ui.violencestyle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.R
import com.music.violence.ui.components.IosSwitch
import com.music.violence.ui.components.PillSegmentedControl
import com.music.violence.ui.components.lightweightLiquidGlass
import com.music.violence.ui.screens.SettingsRow

private val RowInset = 16.dp

/**
 * The "Violence Style (experimental)" block in Settings: the master toggle and, while it is on, every
 * option plus a preview. One composable so the Settings screen needs a single call site.
 */
@Composable
fun ViolenceStyleSettingsBlock() {
    val enabled by ViolenceStyleSettings.enabled.collectAsStateWithLifecycle()
    val safeMode by ViolenceStyleSettings.safeModeTriggered.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth()) {
        SettingsRow(
            title = stringResource(R.string.vs_title),
            subtitle = stringResource(R.string.vs_description),
            trailing = { IosSwitch(checked = enabled, onCheckedChange = ViolenceStyleSettings::setEnabled) },
            onClick = { ViolenceStyleSettings.setEnabled(!enabled) },
        )
        if (safeMode && !enabled) {
            Text(
                text = stringResource(R.string.vs_safe_mode_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = RowInset).padding(bottom = 12.dp),
            )
        }
        if (enabled) ViolenceStyleOptions()
    }
}

@Composable
private fun ViolenceStyleOptions() {
    val background by ViolenceStyleSettings.background.collectAsStateWithLifecycle()
    val motion by ViolenceStyleSettings.motion.collectAsStateWithLifecycle()
    val intensity by ViolenceStyleSettings.intensity.collectAsStateWithLifecycle()
    val source by ViolenceStyleSettings.accentSource.collectAsStateWithLifecycle()
    val headingFont by ViolenceStyleSettings.headingFont.collectAsStateWithLifecycle()
    val forcedTransparency by ViolenceStyleSettings.reduceTransparency.collectAsStateWithLifecycle()
    val detailGlow by ViolenceStyleSettings.detailGlow.collectAsStateWithLifecycle()
    val accountLiquidGlass by ViolenceStyleSettings.accountLiquidGlass.collectAsStateWithLifecycle()
    val fullCoverPages by ViolenceStyleSettings.fullCoverPages.collectAsStateWithLifecycle()
    val automixCover by ViolenceStyleSettings.automixCoverAnimation.collectAsStateWithLifecycle()
    val lyricsQuote by ViolenceStyleSettings.lyricsQuoteCard.collectAsStateWithLifecycle()
    val style = LocalViolenceStyle.current

    OptionDivider()
    ChoiceRow(
        title = stringResource(R.string.vs_background),
        options = StyleBackground.entries.map { stringResource(it.labelRes()) },
        selected = background.ordinal,
        onSelect = { ViolenceStyleSettings.setBackground(StyleBackground.entries[it]) },
    )
    ChoiceRow(
        title = stringResource(R.string.vs_motion),
        options = StyleMotion.entries.map { stringResource(it.labelRes()) },
        selected = motion.ordinal,
        onSelect = { ViolenceStyleSettings.setMotion(StyleMotion.entries[it]) },
    )
    ChoiceRow(
        title = stringResource(R.string.vs_intensity),
        options = StyleIntensity.entries.map { stringResource(it.labelRes()) },
        selected = intensity.ordinal,
        onSelect = { ViolenceStyleSettings.setIntensity(StyleIntensity.entries[it]) },
    )
    ChoiceRow(
        title = stringResource(R.string.vs_accent_source),
        options = StyleAccentSource.entries.map { stringResource(it.labelRes()) },
        selected = source.ordinal,
        onSelect = { ViolenceStyleSettings.setAccentSource(StyleAccentSource.entries[it]) },
    )
    if (source == StyleAccentSource.CUSTOM) CustomColorPicker()
    ChoiceRow(
        title = stringResource(R.string.vs_heading_font),
        options = StyleHeadingFont.entries.map { stringResource(it.labelRes()) },
        selected = headingFont.ordinal,
        onSelect = { ViolenceStyleSettings.setHeadingFont(StyleHeadingFont.entries[it]) },
    )
    OptionDivider()
    SettingsRow(
        title = stringResource(R.string.vs_reduce_transparency),
        subtitle = stringResource(R.string.vs_reduce_transparency_subtitle),
        trailing = { IosSwitch(checked = forcedTransparency, onCheckedChange = ViolenceStyleSettings::setReduceTransparency) },
        onClick = { ViolenceStyleSettings.setReduceTransparency(!forcedTransparency) },
    )
    SettingsRow(
        title = stringResource(R.string.vs_detail_glow),
        subtitle = stringResource(R.string.vs_detail_glow_subtitle),
        trailing = { IosSwitch(checked = detailGlow, onCheckedChange = ViolenceStyleSettings::setDetailGlow) },
        onClick = { ViolenceStyleSettings.setDetailGlow(!detailGlow) },
    )
    SettingsRow(
        title = stringResource(R.string.vs_account_liquid_glass),
        subtitle = stringResource(R.string.vs_account_liquid_glass_subtitle),
        trailing = { IosSwitch(checked = accountLiquidGlass, onCheckedChange = ViolenceStyleSettings::setAccountLiquidGlass) },
        onClick = { ViolenceStyleSettings.setAccountLiquidGlass(!accountLiquidGlass) },
    )
    SettingsRow(
        title = stringResource(R.string.vs_full_cover),
        subtitle = stringResource(R.string.vs_full_cover_subtitle),
        trailing = { IosSwitch(checked = fullCoverPages, onCheckedChange = ViolenceStyleSettings::setFullCoverPages) },
        onClick = { ViolenceStyleSettings.setFullCoverPages(!fullCoverPages) },
    )
    SettingsRow(
        title = stringResource(R.string.vs_automix_cover),
        subtitle = stringResource(R.string.vs_automix_cover_subtitle),
        trailing = { IosSwitch(checked = automixCover, onCheckedChange = ViolenceStyleSettings::setAutomixCoverAnimation) },
        onClick = { ViolenceStyleSettings.setAutomixCoverAnimation(!automixCover) },
    )
    SettingsRow(
        title = stringResource(R.string.vs_lyrics_quote),
        subtitle = stringResource(R.string.vs_lyrics_quote_subtitle),
        trailing = { IosSwitch(checked = lyricsQuote, onCheckedChange = ViolenceStyleSettings::setLyricsQuoteCard) },
        onClick = { ViolenceStyleSettings.setLyricsQuoteCard(!lyricsQuote) },
    )
    Text(
        text = if (style.capability == StyleCapability.FULL) {
            stringResource(R.string.vs_mode_full)
        } else {
            stringResource(R.string.vs_mode_lite, stringResource(style.liteReason.labelRes()))
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = RowInset, vertical = 8.dp),
    )
    ViolenceStylePreviewCard()
}

@Composable
private fun OptionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = RowInset),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    )
}

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = RowInset, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        PillSegmentedControl(options = options, selectedIndex = selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth())
    }
}

/** Presets plus an HSL picker. A colour that cannot be made usable is refused with a message. */
@Composable
private fun CustomColorPicker() {
    val custom by ViolenceStyleSettings.customColor.collectAsStateWithLifecycle()
    val initial = remember { AccentMath.toHsl(custom) }
    var hue by remember { mutableStateOf(initial.h) }
    var sat by remember { mutableStateOf(initial.s) }
    var light by remember { mutableStateOf(initial.l) }
    var refused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = RowInset, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.vs_custom_color),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(modifier = Modifier.fillMaxWidth()) {
            AccentPreset.entries.forEach { preset ->
                val label = stringResource(preset.labelRes())
                val picked = AccentMath.clampAccent(preset.argb) == AccentMath.clampAccent(custom)
                // 48 dp touch target around the 28 dp swatch; announced as a selectable colour, not by colour alone.
                Box(
                    Modifier
                        .minimumInteractiveComponentSize()
                        .semantics {
                            contentDescription = label
                            role = androidx.compose.ui.semantics.Role.RadioButton
                            selected = picked
                        }
                        .clickable {
                            ViolenceStyleSettings.setCustomColor(preset.argb)
                            val h = AccentMath.toHsl(preset.argb)
                            hue = h.h; sat = h.s; light = h.l
                            refused = false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(preset.argb))
                            .border(
                                width = if (picked) 2.dp else 0.5.dp,
                                color = if (picked) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.vs_pick_color),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        fun apply() {
            val argb = AccentMath.fromHsl(hue, sat, light)
            if (AccentMath.makeUsable(argb) != null) {
                refused = false
                ViolenceStyleSettings.setCustomColor(argb)
            } else {
                refused = true
            }
        }
        val hueLabel = stringResource(R.string.vs_hue)
        val satLabel = stringResource(R.string.vs_saturation)
        val lightLabel = stringResource(R.string.vs_lightness)
        Slider(value = hue, onValueChange = { hue = it; apply() }, valueRange = 0f..360f, modifier = Modifier.semantics { contentDescription = hueLabel })
        Slider(value = sat, onValueChange = { sat = it; apply() }, valueRange = 0f..1f, modifier = Modifier.semantics { contentDescription = satLabel })
        Slider(value = light, onValueChange = { light = it; apply() }, valueRange = 0f..1f, modifier = Modifier.semantics { contentDescription = lightLabel })
        if (refused) {
            Text(
                text = stringResource(R.string.vs_color_refused),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * A small static preview so the owner can judge the fonts and the accent on a real device: the
 * background in a mini frame, a swatch of the accent, a heading with its hairline and dot, and a
 * glass button tinted with the accent. It is the only new visual element of stage S1.
 */
@Composable
fun ViolenceStylePreviewCard() {
    val style = LocalViolenceStyle.current
    val tokens = LocalViolenceAccent.current
    val headings = LocalViolenceHeadings.current
    val frame = RoundedCornerShape(20.dp)
    val glow = Color(tokens.glowColor)
    val dot = Color(tokens.accent)
    val peak = style.intensity.peakAlpha

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = RowInset, vertical = 8.dp)
            .clip(frame)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), frame)
            .drawBehind { drawPreviewBackground(style.background, glow, peak) }
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.vs_preview_title),
            style = headings.label,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "#%06X".format(tokens.accent and 0xFFFFFF),
                style = headings.label,
                color = Color(tokens.textAccent),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.vs_preview_heading_caps),
            style = headings.displayCaps,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.vs_preview_heading_italic),
                style = headings.displayItalic.copy(fontStyle = FontStyle.Italic),
                color = Color.White,
                maxLines = 1,
            )
            Spacer(Modifier.width(12.dp))
            Canvas(Modifier.weight(1f).height(12.dp)) {
                val y = size.height / 2f
                drawLine(Color.White.copy(alpha = 0.32f), Offset(0f, y), Offset(size.width - 6.dp.toPx(), y), strokeWidth = 1.dp.toPx())
                val c = Offset(size.width - 3.dp.toPx(), y)
                drawCircle(dot.copy(alpha = 0.10f), radius = 10.dp.toPx(), center = c)
                drawCircle(dot.copy(alpha = 0.28f), radius = 6.dp.toPx(), center = c)
                drawCircle(dot, radius = 3.dp.toPx(), center = c)
            }
        }
        Spacer(Modifier.height(16.dp))
        val pill = RoundedCornerShape(percent = 50)
        Box(
            Modifier
                .lightweightLiquidGlass(pill, Color(0xFF14141C))
                .background(Color(tokens.accentSoft), pill)
                .padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.vs_preview_button),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

/** Static mini version of the S2 backgrounds, enough to judge colour and intensity. */
private fun DrawScope.drawPreviewBackground(style: StyleBackground, glow: Color, peak: Float) {
    drawRect(Color(0xFF05050A))
    fun blob(cx: Float, cy: Float, r: Float, alpha: Float) {
        val center = Offset(size.width * cx, size.height * cy)
        val radius = size.width * r
        drawRect(
            brush = Brush.radialGradient(
                0f to glow.copy(alpha = alpha),
                0.35f to glow.copy(alpha = alpha * 0.55f),
                0.7f to glow.copy(alpha = alpha * 0.18f),
                1f to Color.Transparent,
                center = center,
                radius = radius,
            ),
        )
    }
    when (style) {
        StyleBackground.AURORA -> {
            blob(0.08f, 0.08f, 0.85f, peak)
            blob(1.0f, 0.46f, 0.75f, peak * 0.8f)
        }
        StyleBackground.CORNER_GLOW -> {
            blob(0.05f, 0.02f, 0.95f, peak)
            blob(0.25f, 0.12f, 1.3f, peak * 0.35f)
        }
        StyleBackground.HAZE -> {
            blob(0.5f, 0.13f, 1.0f, peak * 0.9f)
            blob(0.5f, 0.58f, 1.2f, peak * 0.55f)
        }
        StyleBackground.OFF -> Unit
    }
}

private fun StyleBackground.labelRes() = when (this) {
    StyleBackground.AURORA -> R.string.vs_bg_aurora
    StyleBackground.CORNER_GLOW -> R.string.vs_bg_corner
    StyleBackground.HAZE -> R.string.vs_bg_haze
    StyleBackground.OFF -> R.string.vs_off
}

private fun StyleMotion.labelRes() = when (this) {
    StyleMotion.OFF -> R.string.vs_off
    StyleMotion.SUBTLE -> R.string.vs_motion_subtle
    StyleMotion.NORMAL -> R.string.vs_motion_normal
}

private fun StyleIntensity.labelRes() = when (this) {
    StyleIntensity.LOW -> R.string.vs_low
    StyleIntensity.MEDIUM -> R.string.vs_medium
    StyleIntensity.HIGH -> R.string.vs_high
}

private fun StyleAccentSource.labelRes() = when (this) {
    StyleAccentSource.FROM_SONG -> R.string.vs_src_song
    StyleAccentSource.FROM_DEVICE -> R.string.vs_src_device
    StyleAccentSource.CUSTOM -> R.string.vs_src_custom
}

private fun StyleHeadingFont.labelRes() = when (this) {
    StyleHeadingFont.BODONI_MODA -> R.string.vs_font_bodoni
    StyleHeadingFont.CORMORANT_GARAMOND -> R.string.vs_font_cormorant
    StyleHeadingFont.ANTON -> R.string.vs_font_anton
}

private fun LiteReason?.labelRes() = when (this) {
    LiteReason.OLD_ANDROID -> R.string.vs_lite_old_android
    LiteReason.LOW_RAM -> R.string.vs_lite_low_ram
    LiteReason.BATTERY_SAVER -> R.string.vs_lite_battery
    LiteReason.THERMAL -> R.string.vs_lite_thermal
    LiteReason.REDUCE_BLUR -> R.string.vs_lite_reduce_blur
    LiteReason.USER_REDUCED_TRANSPARENCY -> R.string.vs_lite_reduce_transparency
    null -> R.string.vs_mode_full
}

private fun AccentPreset.labelRes() = when (this) {
    AccentPreset.BLUE -> R.string.vs_preset_blue
    AccentPreset.RED -> R.string.vs_preset_red
    AccentPreset.PINK -> R.string.vs_preset_pink
    AccentPreset.GREEN -> R.string.vs_preset_green
    AccentPreset.ORANGE -> R.string.vs_preset_orange
    AccentPreset.YELLOW -> R.string.vs_preset_yellow
    AccentPreset.CYAN -> R.string.vs_preset_cyan
    AccentPreset.PURPLE -> R.string.vs_preset_purple
    AccentPreset.WHITE -> R.string.vs_preset_white
}
