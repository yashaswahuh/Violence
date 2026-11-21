package com.music.violence.ui.violencestyle

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Phone-app chrome helpers for stage S3 (the top bar's glass pill). Each returns what the original code
 * used when the style is off, so a call site swaps one expression and nothing else.
 */

/** Pure colour maths, tested without Compose. */
object ViolenceChromeColors {
    /** Glass tint for floating controls: near-black blue with 12 percent of the accent, 45 percent opaque. */
    const val TINT_BASE: Int = 0xFF101018.toInt()
    const val TINT_ACCENT_MIX = 0.12f
    const val TINT_ALPHA = 0.45f

    /** The light version's glass tint: a near-white with the same 12 percent of the accent, a little more opaque. */
    const val TINT_BASE_LIGHT: Int = 0xFFF6F6FA.toInt()
    const val TINT_ALPHA_LIGHT = 0.58f

    fun tint(accent: Int, alpha: Float = TINT_ALPHA, light: Boolean = false): Int {
        val base = if (light) TINT_BASE_LIGHT else TINT_BASE
        val mixed = AccentMath.argb(
            255,
            mix(AccentMath.red(base), AccentMath.red(accent)),
            mix(AccentMath.green(base), AccentMath.green(accent)),
            mix(AccentMath.blue(base), AccentMath.blue(accent)),
        )
        return AccentMath.withAlpha(mixed, alpha)
    }

    private fun mix(a: Int, b: Int): Int = (a + (b - a) * TINT_ACCENT_MIX).toInt()
}

/** The accent-tinted glass surface tint while the style is on; null (the default tint) otherwise. */
@Composable
fun violenceGlassTint(): Color? {
    if (!LocalViolenceStyle.current.enabled) return null
    val accent = LocalViolenceAccent.current.accent
    val solid = LocalViolenceStyle.current.reduceTransparency
    val light = LocalViolenceLight.current
    return remember(accent, solid, light) {
        val alpha = when {
            solid -> ViolenceGlassColors.SOLID_TINT_ALPHA
            light -> ViolenceChromeColors.TINT_ALPHA_LIGHT
            else -> ViolenceChromeColors.TINT_ALPHA
        }
        Color(ViolenceChromeColors.tint(accent, alpha, light))
    }
}

/**
 * The hairline around a glass control. Off: exactly `border(width, color, shape)` as before. On: a
 * 0.75 dp rim, white 22 percent at the top fading to the accent at 14 percent at the bottom (neutral
 * accent: white throughout).
 */
@Composable
fun Modifier.violenceGlassEdge(shape: Shape, width: Dp, color: Color): Modifier {
    if (!LocalViolenceStyle.current.enabled) return border(width, color, shape)
    val tokens = LocalViolenceAccent.current
    val light = LocalViolenceLight.current
    val brush = remember(tokens.accent, tokens.neutral, light) {
        if (light) {
            // On the light page a dark hairline reads; a white one would vanish.
            val bottom = if (tokens.neutral) Color.Black.copy(alpha = 0.08f) else Color(tokens.accent).copy(alpha = 0.22f)
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.10f), bottom))
        } else {
            val bottom = if (tokens.neutral) Color.White.copy(alpha = 0.14f) else Color(tokens.accent).copy(alpha = 0.14f)
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), bottom))
        }
    }
    return border(0.75.dp, brush, shape)
}
