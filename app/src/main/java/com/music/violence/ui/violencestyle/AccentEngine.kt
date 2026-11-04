package com.music.violence.ui.violencestyle

/** The derived colours every styled component draws with. All packed ARGB ints. */
data class AccentTokens(
    /** The clamped accent: capsules, rims, terminators. */
    val accent: Int,
    /** Translucent fill (22 percent alpha). */
    val accentSoft: Int,
    /** Colour of the background glow: same hue, saturation capped, lightness x0.8. */
    val glowColor: Int,
    /** Black or white, whichever reads better on [accent]. */
    val onAccent: Int,
    /** Lightened accent that reaches 4.5:1 against the dark background, for small text. */
    val textAccent: Int,
    /** True for the White preset and other neutral colours: the capsule stays neutral (ref1 look). */
    val neutral: Boolean,
) {
    companion object {
        /** Derives every token from any source colour, however extreme. */
        fun from(source: Int): AccentTokens {
            val accent = AccentMath.clampAccent(source)
            val neutral = AccentMath.isNeutral(source)
            val hsl = AccentMath.toHsl(accent)
            val glow = AccentMath.fromHsl(
                hsl.h,
                hsl.s.coerceAtMost(if (neutral) AccentMath.NEUTRAL_SATURATION else 0.85f),
                hsl.l * AccentMath.GLOW_LIGHTNESS_FACTOR,
            )
            return AccentTokens(
                accent = accent,
                accentSoft = AccentMath.withAlpha(accent, 0.22f),
                glowColor = glow,
                onAccent = AccentMath.onColor(accent),
                textAccent = AccentMath.liftToContrast(accent),
                neutral = neutral,
            )
        }

        /** The Blue preset's tokens: the value outside a provider. */
        val DEFAULT: AccentTokens = from(StyleDefaults.CUSTOM_COLOR)
    }
}

/** The nine presets of the custom colour (STYLE_SPEC.md section 3). */
enum class AccentPreset(val argb: Int) {
    BLUE(0xFF3F7BFF.toInt()),
    RED(0xFFE5384F.toInt()),
    PINK(0xFFE04A9B.toInt()),
    GREEN(0xFF2FBF71.toInt()),
    ORANGE(0xFFF28C28.toInt()),
    YELLOW(0xFFF2C230.toInt()),
    CYAN(0xFF26C6DA.toInt()),
    PURPLE(0xFF8B5CF6.toInt()),
    WHITE(0xFFE8E8F0.toInt()),
}

/** Where a resolved accent came from, for the preview card and for tests. */
enum class AccentOrigin { SONG, SONG_NEUTRAL, HELD_SONG, DEVICE, FIXED_DEFAULT, CUSTOM }

class ResolvedAccent(val argb: Int, val origin: AccentOrigin)

/**
 * Chooses the accent colour. Pure: the caller supplies the song's colour (already read off the
 * artwork), the device accent (null below Android 12) and the previous song accent of this session.
 *
 * - FROM_SONG: the song colour; if missing or dull, hold the previous song accent (no flicker, no
 *   gray glow); if none yet, the device accent; if none, the custom colour.
 * - FROM_DEVICE: the device accent, or the fixed default Blue on Android 8 to 11.
 * - CUSTOM: the chosen colour (made usable; a colour that cannot be made usable falls back to Blue).
 */
object AccentEngine {

    fun resolve(
        source: StyleAccentSource,
        songColor: Int?,
        deviceAccent: Int?,
        custom: Int,
        previousSongAccent: Int? = null,
    ): ResolvedAccent = when (source) {
        StyleAccentSource.FROM_SONG -> when {
            // Black, white and gray artwork have no colour to follow: the accent becomes the neutral White
            // look (gray capsule, white glow) instead of keeping the previous song's colour.
            songColor != null && AccentMath.isDull(songColor) -> ResolvedAccent(AccentPreset.WHITE.argb, AccentOrigin.SONG_NEUTRAL)
            songColor != null -> ResolvedAccent(songColor, AccentOrigin.SONG)
            previousSongAccent != null -> ResolvedAccent(previousSongAccent, AccentOrigin.HELD_SONG)
            deviceAccent != null -> ResolvedAccent(deviceAccent, AccentOrigin.DEVICE)
            else -> customOrDefault(custom)
        }
        StyleAccentSource.FROM_DEVICE ->
            if (deviceAccent != null) ResolvedAccent(deviceAccent, AccentOrigin.DEVICE)
            else ResolvedAccent(StyleDefaults.CUSTOM_COLOR, AccentOrigin.FIXED_DEFAULT)
        StyleAccentSource.CUSTOM -> customOrDefault(custom)
    }

    private fun customOrDefault(custom: Int): ResolvedAccent {
        val usable = AccentMath.makeUsable(custom)
        return if (usable != null) ResolvedAccent(custom, AccentOrigin.CUSTOM)
        else ResolvedAccent(StyleDefaults.CUSTOM_COLOR, AccentOrigin.FIXED_DEFAULT)
    }

    /**
     * Whether a new song colour should replace the one on screen. Tiny changes (same hue within 6
     * degrees and 4 percent lightness) are ignored so the glow does not re-fade for nothing.
     */
    fun shouldReplace(current: Int?, next: Int): Boolean =
        current == null || !AccentMath.isNegligibleChange(current, next)
}
