package com.music.violence.desktop

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.music.violence.ui.violencestyle.StyleHeadingFont
import org.jetbrains.compose.resources.Font as composeFont
import violence.desktopapp.generated.resources.Res
import violence.desktopapp.generated.resources.sf_pro_display_bold
import violence.desktopapp.generated.resources.sf_pro_display_heavy
import violence.desktopapp.generated.resources.sf_pro_display_medium
import violence.desktopapp.generated.resources.sf_pro_display_regular
import violence.desktopapp.generated.resources.sf_pro_display_semibold
import violence.desktopapp.generated.resources.vs_anton
import violence.desktopapp.generated.resources.vs_bodoni_moda
import violence.desktopapp.generated.resources.vs_bodoni_moda_italic
import violence.desktopapp.generated.resources.vs_cormorant_garamond
import violence.desktopapp.generated.resources.vs_cormorant_garamond_italic
import violence.desktopapp.generated.resources.vs_inter

/**
 * SIL Open Font License 1.1 fonts configured for Desktop:
 * - Inter (variable body font)
 * - Bodoni Moda (Didone display serif)
 * - Cormorant Garamond (light display serif)
 * - Anton (grotesque condensed)
 */
internal object DesktopFonts {

    @Composable
    fun inter(): FontFamily = FontFamily(
        composeFont(Res.font.vs_inter, FontWeight.W400),
        composeFont(Res.font.vs_inter, FontWeight.W500),
        composeFont(Res.font.vs_inter, FontWeight.W600),
        composeFont(Res.font.vs_inter, FontWeight.W700),
    )

    @Composable
    fun bodoniModa(): FontFamily = FontFamily(
        composeFont(Res.font.vs_bodoni_moda, FontWeight.W400, FontStyle.Normal),
        composeFont(Res.font.vs_bodoni_moda_italic, FontWeight.W400, FontStyle.Italic),
    )

    @Composable
    fun cormorantGaramond(): FontFamily = FontFamily(
        composeFont(Res.font.vs_cormorant_garamond, FontWeight.W400, FontStyle.Normal),
        composeFont(Res.font.vs_cormorant_garamond_italic, FontWeight.W400, FontStyle.Italic),
    )

    @Composable
    fun anton(): FontFamily = FontFamily(
        composeFont(Res.font.vs_anton, FontWeight.W400, FontStyle.Normal),
    )

    @Composable
    fun heading(font: StyleHeadingFont): FontFamily = when (font) {
        StyleHeadingFont.BODONI_MODA -> bodoniModa()
        StyleHeadingFont.CORMORANT_GARAMOND -> cormorantGaramond()
        StyleHeadingFont.ANTON -> anton()
    }
}

/**
 * Standard Desktop typography using SF Pro Display (upstream default look).
 */
@Composable
internal fun desktopStandardTypography(): Typography {
    val sfProDisplay = FontFamily(
        composeFont(Res.font.sf_pro_display_regular, FontWeight.W400),
        composeFont(Res.font.sf_pro_display_medium, FontWeight.W500),
        composeFont(Res.font.sf_pro_display_semibold, FontWeight.W600),
        composeFont(Res.font.sf_pro_display_bold, FontWeight.W700),
        composeFont(Res.font.sf_pro_display_heavy, FontWeight.W800),
    )
    val defaults = Typography()
    return defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W800, fontSize = 34.sp, letterSpacing = (-0.8).sp),
        displayMedium = defaults.displayMedium.copy(fontFamily = sfProDisplay),
        displaySmall = defaults.displaySmall.copy(fontFamily = sfProDisplay),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = (-0.4).sp),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = sfProDisplay),
        titleLarge = defaults.titleLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.3).sp),
        titleMedium = defaults.titleMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
        titleSmall = defaults.titleSmall.copy(fontFamily = sfProDisplay),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 16.sp),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 14.sp),
        bodySmall = defaults.bodySmall.copy(fontFamily = sfProDisplay),
        labelLarge = defaults.labelLarge.copy(fontFamily = sfProDisplay),
        labelMedium = defaults.labelMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 12.sp),
        labelSmall = defaults.labelSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 11.sp),
    )
}

/**
 * Violence Style typography for Desktop: Inter body face with styled weights.
 */
@Composable
internal fun desktopViolenceTypography(base: Typography): Typography {
    val inter = DesktopFonts.inter()
    fun TextStyle.interStyle(weight: FontWeight) = copy(fontFamily = inter, fontWeight = weight)

    return base.copy(
        displayLarge = base.displayLarge.interStyle(FontWeight.W600),
        displayMedium = base.displayMedium.interStyle(FontWeight.W600),
        displaySmall = base.displaySmall.interStyle(FontWeight.W600),
        headlineLarge = base.headlineLarge.interStyle(FontWeight.W600),
        headlineMedium = base.headlineMedium.interStyle(FontWeight.W600),
        headlineSmall = base.headlineSmall.interStyle(FontWeight.W600),
        titleLarge = base.titleLarge.interStyle(FontWeight.W600),
        titleMedium = base.titleMedium.interStyle(FontWeight.W500),
        titleSmall = base.titleSmall.interStyle(FontWeight.W500),
        bodyLarge = base.bodyLarge.interStyle(FontWeight.W400),
        bodyMedium = base.bodyMedium.interStyle(FontWeight.W400),
        bodySmall = base.bodySmall.interStyle(FontWeight.W400),
        labelLarge = base.labelLarge.interStyle(FontWeight.W500),
        labelMedium = base.labelMedium.interStyle(FontWeight.W500),
        labelSmall = base.labelSmall.interStyle(FontWeight.W500),
    )
}

/**
 * Returns desktop typography honoring the OFF-parity guarantee:
 * when [enabled] is false, returns exactly the standard SF Pro typography.
 */
@Composable
internal fun desktopTypography(enabled: Boolean): Typography {
    val standard = desktopStandardTypography()
    return if (!enabled) standard else desktopViolenceTypography(standard)
}

