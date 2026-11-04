package com.music.violence.desktop

import com.music.violence.ui.violencestyle.StyleAccentSource
import com.music.violence.ui.violencestyle.StyleBackground
import com.music.violence.ui.violencestyle.StyleCapability
import com.music.violence.ui.violencestyle.StyleDefaults
import com.music.violence.ui.violencestyle.StyleHeadingFont
import com.music.violence.ui.violencestyle.StyleIntensity
import com.music.violence.ui.violencestyle.StyleMotion
import com.music.violence.ui.violencestyle.StyleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The toggles read far from the Settings sheet that sets them.
 *
 * Mirrors Android's `AppSettings` and `ViolenceStyleSettings`:
 * Standard desktop appearance flags plus the Violence Style foundation settings (default OFF).
 */
internal object DesktopAppearanceSettings {

    private fun flag(key: String, default: Boolean) =
        MutableStateFlow(DesktopPersistence().boolean(key, default))

    private fun <T : Enum<T>> enumFlag(key: String, default: T, values: Array<T>): MutableStateFlow<T> {
        val raw = DesktopPersistence().string(key, "")
        val initial = values.firstOrNull { it.name == raw } ?: default
        return MutableStateFlow(initial)
    }

    private fun intFlag(key: String, default: Int) =
        MutableStateFlow(DesktopPersistence().int(key, default))

    private val _reduceAnimation = flag(KEY_REDUCE_ANIMATION, false)
    private val _reduceDynamicBlur = flag(KEY_REDUCE_DYNAMIC_BLUR, false)
    private val _hideVolumeBar = flag(KEY_HIDE_VOLUME_BAR, false)
    private val _ambientBackdrop = flag(KEY_AMBIENT_BACKDROP, false)

    // Violence Style settings
    private val _violenceStyleEnabled = flag(KEY_VIOLENCE_STYLE_ENABLED, false)
    private val _styleBackground = enumFlag(KEY_STYLE_BACKGROUND, StyleDefaults.BACKGROUND, StyleBackground.entries.toTypedArray())
    private val _styleMotion = enumFlag(KEY_STYLE_MOTION, StyleDefaults.MOTION, StyleMotion.entries.toTypedArray())
    private val _styleIntensity = enumFlag(KEY_STYLE_INTENSITY, StyleDefaults.INTENSITY, StyleIntensity.entries.toTypedArray())
    private val _styleAccentSource = enumFlag(KEY_STYLE_ACCENT_SOURCE, StyleDefaults.ACCENT_SOURCE, StyleAccentSource.entries.toTypedArray())
    private val _styleCustomColor = intFlag(KEY_STYLE_CUSTOM_COLOR, StyleDefaults.CUSTOM_COLOR)
    private val _styleHeadingFont = enumFlag(KEY_STYLE_HEADING_FONT, StyleDefaults.HEADING_FONT, StyleHeadingFont.entries.toTypedArray())
    private val _styleReduceTransparency = flag(KEY_STYLE_REDUCE_TRANSPARENCY, false)

    /** Freezes the main player's gradient instead of drifting. */
    val reduceAnimation: StateFlow<Boolean> = _reduceAnimation

    /** Swaps frosted glass for solid fills across the app. */
    val reduceDynamicBlur: StateFlow<Boolean> = _reduceDynamicBlur

    /** Removes the volume slider from the main player. */
    val hideVolumeBar: StateFlow<Boolean> = _hideVolumeBar

    /** Diffuses current artwork colors under the window glass chrome like Apple Music. */
    val ambientBackdrop: StateFlow<Boolean> = _ambientBackdrop

    /** Master toggle for Violence Style (luminous dark skin). Default OFF. */
    val violenceStyleEnabled: StateFlow<Boolean> = _violenceStyleEnabled
    val styleBackground: StateFlow<StyleBackground> = _styleBackground
    val styleMotion: StateFlow<StyleMotion> = _styleMotion
    val styleIntensity: StateFlow<StyleIntensity> = _styleIntensity
    val styleAccentSource: StateFlow<StyleAccentSource> = _styleAccentSource
    val styleCustomColor: StateFlow<Int> = _styleCustomColor
    val styleHeadingFont: StateFlow<StyleHeadingFont> = _styleHeadingFont
    val styleReduceTransparency: StateFlow<Boolean> = _styleReduceTransparency

    fun setReduceAnimation(value: Boolean) = write(KEY_REDUCE_ANIMATION, value, _reduceAnimation)
    fun setReduceDynamicBlur(value: Boolean) = write(KEY_REDUCE_DYNAMIC_BLUR, value, _reduceDynamicBlur)
    fun setHideVolumeBar(value: Boolean) = write(KEY_HIDE_VOLUME_BAR, value, _hideVolumeBar)
    fun setAmbientBackdrop(value: Boolean) = write(KEY_AMBIENT_BACKDROP, value, _ambientBackdrop)

    fun setViolenceStyleEnabled(value: Boolean) = write(KEY_VIOLENCE_STYLE_ENABLED, value, _violenceStyleEnabled)
    fun setStyleBackground(value: StyleBackground) = writeEnum(KEY_STYLE_BACKGROUND, value, _styleBackground)
    fun setStyleMotion(value: StyleMotion) = writeEnum(KEY_STYLE_MOTION, value, _styleMotion)
    fun setStyleIntensity(value: StyleIntensity) = writeEnum(KEY_STYLE_INTENSITY, value, _styleIntensity)
    fun setStyleAccentSource(value: StyleAccentSource) = writeEnum(KEY_STYLE_ACCENT_SOURCE, value, _styleAccentSource)
    fun setStyleCustomColor(value: Int) = writeInt(KEY_STYLE_CUSTOM_COLOR, value, _styleCustomColor)
    fun setStyleHeadingFont(value: StyleHeadingFont) = writeEnum(KEY_STYLE_HEADING_FONT, value, _styleHeadingFont)
    fun setStyleReduceTransparency(value: Boolean) = write(KEY_STYLE_REDUCE_TRANSPARENCY, value, _styleReduceTransparency)

    /** Builds the current StyleState snapshot. */
    fun currentStyleState(): StyleState {
        val enabled = _violenceStyleEnabled.value
        return if (!enabled) StyleState.DISABLED else StyleState(
            enabled = true,
            background = _styleBackground.value,
            motion = _styleMotion.value,
            intensity = _styleIntensity.value,
            accentSource = _styleAccentSource.value,
            customColor = _styleCustomColor.value,
            headingFont = _styleHeadingFont.value,
            reduceTransparency = _styleReduceTransparency.value || _reduceDynamicBlur.value,
            detailGlow = StyleDefaults.DETAIL_GLOW,
            capability = StyleCapability.FULL,
            liteReason = null,
            reduceMotion = _reduceAnimation.value,
            highContrast = false,
        )
    }

    private fun write(key: String, value: Boolean, into: MutableStateFlow<Boolean>) {
        DesktopPersistence().saveBoolean(key, value)
        into.value = value
    }

    private fun <T : Enum<T>> writeEnum(key: String, value: T, into: MutableStateFlow<T>) {
        DesktopPersistence().saveString(key, value.name)
        into.value = value
    }

    private fun writeInt(key: String, value: Int, into: MutableStateFlow<Int>) {
        DesktopPersistence().saveInt(key, value)
        into.value = value
    }

    internal const val KEY_REDUCE_ANIMATION = "reduce_animation"
    internal const val KEY_REDUCE_DYNAMIC_BLUR = "reduce_dynamic_blur"
    internal const val KEY_HIDE_VOLUME_BAR = "hide_volume_bar"
    internal const val KEY_AMBIENT_BACKDROP = "ambient_backdrop"

    internal const val KEY_VIOLENCE_STYLE_ENABLED = "violence_style.enabled"
    internal const val KEY_STYLE_BACKGROUND = "violence_style.background"
    internal const val KEY_STYLE_MOTION = "violence_style.motion"
    internal const val KEY_STYLE_INTENSITY = "violence_style.intensity"
    internal const val KEY_STYLE_ACCENT_SOURCE = "violence_style.accent_source"
    internal const val KEY_STYLE_CUSTOM_COLOR = "violence_style.custom_color"
    internal const val KEY_STYLE_HEADING_FONT = "violence_style.heading_font"
    internal const val KEY_STYLE_REDUCE_TRANSPARENCY = "violence_style.reduce_transparency"
}
