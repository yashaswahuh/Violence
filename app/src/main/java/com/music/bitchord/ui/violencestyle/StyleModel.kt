package com.music.bitchord.ui.violencestyle

import androidx.compose.runtime.staticCompositionLocalOf

/*
 * Violence Style (experimental): the state every styled component reads.
 *
 * Everything in this package is new code. With the master toggle OFF nothing here
 * runs and every function returns its input unchanged (see StyleOffParityTest).
 */

/** Which live background is drawn (S2). Arcs was dropped by the owner; it is not an option. */
enum class StyleBackground { AURORA, CORNER_GLOW, HAZE, OFF }

enum class StyleMotion { OFF, SUBTLE, NORMAL }

enum class StyleIntensity {
    LOW, MEDIUM, HIGH;

    /** Peak alpha of the strongest glow blob (STYLE_SPEC.md section 2; raised about 25 percent after the S2 device review). */
    val peakAlpha: Float
        get() = when (this) {
            LOW -> 0.38f
            MEDIUM -> 0.62f
            HIGH -> 0.85f
        }
}

enum class StyleAccentSource { FROM_SONG, FROM_DEVICE, CUSTOM }

enum class StyleHeadingFont { BODONI_MODA, CORMORANT_GARAMOND }

/** FULL: everything. LITE: static background, simpler glass (STYLE_SPEC.md section 10). */
enum class StyleCapability { FULL, LITE }

/** Why the style runs in LITE mode; shown as a note in Settings. */
enum class LiteReason { OLD_ANDROID, LOW_RAM, BATTERY_SAVER, THERMAL, REDUCE_BLUR, USER_REDUCED_TRANSPARENCY }

/**
 * The single immutable style state. [DISABLED] is the default of [LocalViolenceStyle], so any
 * composable outside the provider behaves exactly as before.
 */
data class StyleState(
    val enabled: Boolean,
    val background: StyleBackground,
    val motion: StyleMotion,
    val intensity: StyleIntensity,
    val accentSource: StyleAccentSource,
    /** ARGB of the custom colour (as chosen, before clamping). */
    val customColor: Int,
    val headingFont: StyleHeadingFont,
    /** Solid fills instead of translucent glass. */
    val reduceTransparency: Boolean,
    /** Show the glow above artwork on album, artist and playlist pages (applied in S5). */
    val detailGlow: Boolean,
    val capability: StyleCapability,
    val liteReason: LiteReason?,
    /** System "remove animations" (animator scale 0) or the in-app Reduce animation. */
    val reduceMotion: Boolean,
    /** System high-contrast text, where the device exposes it. */
    val highContrast: Boolean,
) {
    /** The motion that actually runs: none in LITE, none under reduce motion. */
    val effectiveMotion: StyleMotion
        get() = if (!enabled || capability == StyleCapability.LITE || reduceMotion) StyleMotion.OFF else motion

    companion object {
        val DISABLED = StyleState(
            enabled = false,
            background = StyleDefaults.BACKGROUND,
            motion = StyleDefaults.MOTION,
            intensity = StyleDefaults.INTENSITY,
            accentSource = StyleDefaults.ACCENT_SOURCE,
            customColor = StyleDefaults.CUSTOM_COLOR,
            headingFont = StyleDefaults.HEADING_FONT,
            reduceTransparency = false,
            detailGlow = StyleDefaults.DETAIL_GLOW,
            capability = StyleCapability.FULL,
            liteReason = null,
            reduceMotion = false,
            highContrast = false,
        )
    }
}

/** Defaults from STYLE_DECISIONS.md (owner, 2026-11-03). */
object StyleDefaults {
    val BACKGROUND = StyleBackground.AURORA
    val MOTION = StyleMotion.NORMAL
    val INTENSITY = StyleIntensity.MEDIUM
    val ACCENT_SOURCE = StyleAccentSource.FROM_SONG
    val HEADING_FONT = StyleHeadingFont.BODONI_MODA
    const val CUSTOM_COLOR: Int = 0xFF3F7BFF.toInt()
    const val DETAIL_GLOW = true
}

/** Read by shared components. Default DISABLED: outside the provider nothing changes. */
val LocalViolenceStyle = staticCompositionLocalOf { StyleState.DISABLED }
