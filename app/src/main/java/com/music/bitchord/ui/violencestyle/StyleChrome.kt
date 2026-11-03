package com.music.bitchord.ui.violencestyle

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

    fun tint(accent: Int): Int {
        val mixed = AccentMath.argb(
            255,
            mix(AccentMath.red(TINT_BASE), AccentMath.red(accent)),
            mix(AccentMath.green(TINT_BASE), AccentMath.green(accent)),
            mix(AccentMath.blue(TINT_BASE), AccentMath.blue(accent)),
        )
        return AccentMath.withAlpha(mixed, TINT_ALPHA)
    }

    private fun mix(a: Int, b: Int): Int = (a + (b - a) * TINT_ACCENT_MIX).toInt()
}

/** The accent-tinted glass surface tint while the style is on; null (the default tint) otherwise. */
@Composable
fun violenceGlassTint(): Color? {
    if (!LocalViolenceStyle.current.enabled) return null
    val accent = LocalViolenceAccent.current.accent
    return remember(accent) { Color(ViolenceChromeColors.tint(accent)) }
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
    val brush = remember(tokens.accent, tokens.neutral) {
        val bottom = if (tokens.neutral) Color.White.copy(alpha = 0.14f) else Color(tokens.accent).copy(alpha = 0.14f)
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), bottom))
    }
    return border(0.75.dp, brush, shape)
}
