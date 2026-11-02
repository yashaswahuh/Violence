package com.music.bitchord.ui.brutal

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The accent presets of spec section 3.1. Yellow is measured off the mockups (`#FAD60A`); the other
 * hex values are chosen. [id] is what is stored, so it never changes once shipped.
 */
enum class AccentPreset(val id: String, val argb: Int) {
    YELLOW("yellow", 0xFFFAD60A.toInt()),
    RED("red", 0xFFFF3B3B.toInt()),
    BLUE("blue", 0xFF2F5BFF.toInt()),
    GREEN("green", 0xFFB8FF1F.toInt()),
    ORANGE("orange", 0xFFFF7A00.toInt()),
    PINK("pink", 0xFFFF3EA5.toInt()),
    CYAN("cyan", 0xFF1FDEDB.toInt()),
    WHITE("white", 0xFFFFFFFF.toInt()),
    ;

    companion object {
        fun fromId(id: String?): AccentPreset? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The accent the user chose: one of the [AccentPreset]s or a free colour from the picker.
 *
 * Stored as one string: the preset id (`"yellow"`) or `"custom:AARRGGBB"`. Parsing never throws:
 * anything unreadable is the default, yellow. A custom colour is always stored opaque (an accent
 * with transparency would change every contrast figure), whatever alpha was handed in.
 *
 * This is the raw choice. What the app actually draws with is derived from it by
 * [BrutalAccentRules.derive], so a choice that is unreadable is repaired at use and the choice
 * itself survives a later change of the background.
 */
sealed interface BrutalAccent {
    val argb: Int

    fun serialize(): String

    data class Preset(val preset: AccentPreset) : BrutalAccent {
        override val argb: Int get() = preset.argb
        override fun serialize(): String = preset.id
    }

    /**
     * A free colour. Equality is on the opaque colour, so `Custom(0x00336699)` (what a picker might
     * hand in) equals what was read back from storage.
     */
    class Custom(rawArgb: Int) : BrutalAccent {
        override val argb: Int = rawArgb or OPAQUE
        override fun serialize(): String = CUSTOM_PREFIX + "%08X".format(argb)
        override fun equals(other: Any?): Boolean = other is Custom && other.argb == argb
        override fun hashCode(): Int = argb
        override fun toString(): String = "Custom(" + "%08X".format(argb) + ")"
    }

    companion object {
        val Default: BrutalAccent = Preset(AccentPreset.YELLOW)

        private const val CUSTOM_PREFIX = "custom:"
        private const val OPAQUE = 0xFF000000.toInt()

        fun parse(raw: String?): BrutalAccent {
            if (raw == null) return Default
            AccentPreset.fromId(raw)?.let { return Preset(it) }
            if (raw.startsWith(CUSTOM_PREFIX)) {
                val hex = raw.removePrefix(CUSTOM_PREFIX)
                if (hex.length == 6 || hex.length == 8) {
                    val value = hex.toLongOrNull(16)
                    // Six digits are RGB, eight are ARGB; either way the colour is stored opaque.
                    if (value != null) return Custom(value.toInt())
                }
            }
            return Default
        }

        /** The preset equal to [argb], so a picker result that lands on a preset is stored as it. */
        fun fromArgb(argb: Int): BrutalAccent {
            val opaque = argb or OPAQUE
            val hit = AccentPreset.entries.firstOrNull { it.argb == opaque }
            return if (hit != null) Preset(hit) else Custom(opaque)
        }
    }
}

/** Hue 0..360, saturation 0..1, value 0..1: what the custom picker's three sliders edit. */
data class Hsv(val hue: Float, val saturation: Float, val value: Float) {
    init {
        require(hue in 0f..360f && saturation in 0f..1f && value in 0f..1f) { "HSV out of range: $this" }
    }

    fun toArgb(): Int {
        val c = value * saturation
        val h = (hue % 360f) / 60f
        val x = c * (1f - abs(h % 2f - 1f))
        val m = value - c
        val (r, g, b) = when {
            h < 1f -> Triple(c, x, 0f)
            h < 2f -> Triple(x, c, 0f)
            h < 3f -> Triple(0f, c, x)
            h < 4f -> Triple(0f, x, c)
            h < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    companion object {
        fun fromArgb(argb: Int): Hsv {
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val d = max - min
            val hue = when {
                d == 0f -> 0f
                max == r -> 60f * (((g - b) / d) % 6f)
                max == g -> 60f * (((b - r) / d) + 2f)
                else -> 60f * (((r - g) / d) + 4f)
            }.let { if (it < 0f) it + 360f else it }
            val sat = if (max == 0f) 0f else d / max
            return Hsv(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), max.coerceIn(0f, 1f))
        }
    }
}
