package com.music.violence.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
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

object WearType {
    val title = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W600, fontSize = 15.sp, letterSpacing = (-0.2).sp)
    val subtitle = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W400, fontSize = 13.sp, letterSpacing = (-0.1).sp)
    val header = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W700, fontSize = 17.sp, letterSpacing = (-0.3).sp)
    val row = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W500, fontSize = 14.sp, letterSpacing = (-0.15).sp)
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
            background = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Secondary,
            surfaceContainer = Cell,
        ),
        typography = Typography(defaultFontFamily = SFPro),
        content = content,
    )
}
