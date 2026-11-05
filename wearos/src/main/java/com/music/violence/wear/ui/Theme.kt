package com.music.violence.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.MotionScheme
import androidx.wear.compose.material3.Typography
import com.music.violence.wear.R

/**
 * SF Pro Display, as on the phone — subset to Latin for the watch (the phone's
 * files are 2.3 MB a weight; these are ~50 KB). Anything outside the subset, a
 * Hindi or Japanese title, falls back to the system face glyph by glyph.
 */
val SFPro = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.W400),
    Font(R.font.sf_pro_display_medium, FontWeight.W500),
    Font(R.font.sf_pro_display_semibold, FontWeight.W600),
    Font(R.font.sf_pro_display_bold, FontWeight.W700),
)

/** Violence "Voltage": the accent fill (white on it is 5.7:1). */
val Voltage = Color(0xFF7C3AED)

/** Voltage as text, icons and progress on black (9.7:1). */
val VoltageText = Color(0xFFC4A8FF)

/** Love, and the heart only: "Ember" on the phone. */
val Ember = Color(0xFFFF6B8A)

/** Secondary text on the mesh and on black: the phone's Ink-2 (8:1 on black). */
val Secondary = Color(0xFFA7A5B8)

/** Grouped cells on black: a violet-tinted surface, a step above Void. */
val Cell = Color(0xFF15131F)

/** A cell while a finger is on it. */
val CellPressed = Color(0xFF221E30)

/** The hairline round a cell, so it reads on black without a shadow. */
val CellEdge = Color.White.copy(alpha = 0.07f)

/** A glass fill for controls that sit on the blurred cover. */
val Glass = Color.White.copy(alpha = 0.16f)

/**
 * The song's own accent: the first colour the phone's player took from the
 * cover, pushed to a light, saturated tone so it reads on the dark blurred
 * backdrop. Voltage's light tone when the phone sent none.
 */
fun accentFor(colors: List<Color>): Color {
    val base = colors.firstOrNull() ?: return VoltageText
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    // A grey cover has no hue worth keeping.
    if (hsl[1] < 0.12f) return VoltageText
    hsl[1] = hsl[1].coerceIn(0.55f, 1f)
    hsl[2] = 0.70f
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Dark or white, whichever reads on [accent]. */
fun onAccent(accent: Color): Color =
    if (ColorUtils.calculateLuminance(accent.toArgb()) > 0.42) Color(0xFF14101F) else Color.White

object WearType {
    /** The track title on the player: big and heavy, the expressive way. */
    val display = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W700, fontSize = 19.sp, letterSpacing = (-0.4).sp)
    val title = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W600, fontSize = 15.sp, letterSpacing = (-0.2).sp)
    val subtitle = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W400, fontSize = 13.sp, letterSpacing = (-0.1).sp)
    val header = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W700, fontSize = 17.sp, letterSpacing = (-0.3).sp)
    val row = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W600, fontSize = 14.sp, letterSpacing = (-0.15).sp)
    val rowSub = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W400, fontSize = 12.sp)
    val button = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W600, fontSize = 14.sp)
    val caption = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W500, fontSize = 11.sp, letterSpacing = 0.2.sp)
}

@Composable
fun ViolenceWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme(
            primary = Voltage,
            onPrimary = Color.White,
            primaryContainer = VoltageText,
            onPrimaryContainer = Color(0xFF14101F),
            secondaryContainer = Color(0xFF2A2340),
            onSecondaryContainer = Color(0xFFE9DDFF),
            background = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Secondary,
            surfaceContainer = Cell,
            surfaceContainerHigh = CellPressed,
        ),
        typography = Typography(defaultFontFamily = SFPro),
        // Springs for every movement: presses, shape changes, list items.
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
