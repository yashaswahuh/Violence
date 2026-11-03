@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.music.bitchord.ui.violencestyle

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.music.bitchord.R

/*
 * Fonts, all bundled as local resources (no network fonts), all SIL Open Font License 1.1:
 *   Inter              res/font/vs_inter.ttf                        (variable: opsz, wght)
 *   Bodoni Moda        res/font/vs_bodoni_moda(.|_italic).ttf       (variable: opsz, wght)
 *   Cormorant Garamond res/font/vs_cormorant_garamond(.|_italic).ttf (variable: wght)
 * Licence texts: fork-docs/style/licenses/ and NOTICE.
 */

/** Inter, the body face while the style is on. Variable font, so each weight is its own instance. */
val ViolenceInter: FontFamily = FontFamily(
    listOf(400, 500, 600).map { w ->
        Font(
            resId = R.font.vs_inter,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.opticalSizing(16.sp)),
        )
    },
)

private val HeadingOpticalSize: TextUnit = 96.sp

private fun headingFamily(upright: Int, italic: Int, opsz: Boolean): FontFamily = FontFamily(
    listOf(300, 400, 500).flatMap { w ->
        val weightSetting = FontVariation.weight(w)
        val settings = if (opsz) {
            FontVariation.Settings(weightSetting, FontVariation.opticalSizing(HeadingOpticalSize))
        } else {
            FontVariation.Settings(weightSetting)
        }
        listOf(
            Font(resId = upright, weight = FontWeight(w), style = FontStyle.Normal, variationSettings = settings),
            Font(resId = italic, weight = FontWeight(w), style = FontStyle.Italic, variationSettings = settings),
        )
    },
)

/** Bodoni Moda: high contrast Didone. Its lightest cut is Regular; lighter weights snap to 400. */
val ViolenceBodoniModa: FontFamily by lazy {
    headingFamily(R.font.vs_bodoni_moda, R.font.vs_bodoni_moda_italic, opsz = true)
}

/** Cormorant Garamond: the thinner, softer option (it has a true Light). */
val ViolenceCormorantGaramond: FontFamily by lazy {
    headingFamily(R.font.vs_cormorant_garamond, R.font.vs_cormorant_garamond_italic, opsz = false)
}

fun StyleHeadingFont.family(): FontFamily = when (this) {
    StyleHeadingFont.BODONI_MODA -> ViolenceBodoniModa
    StyleHeadingFont.CORMORANT_GARAMOND -> ViolenceCormorantGaramond
}

/**
 * The heading tokens S3 uses (STYLE_SPEC.md section 5). Sizes are sp and scale with the system font
 * size; [headingFontScaleCap] is the most they should scale (1.3x) so headings never break a layout
 * at 200 percent.
 */
class ViolenceHeadingStyles(
    /** "TOP PICKS": display serif, caps (the caller upper-cases the text). */
    val displayCaps: TextStyle,
    /** "for You": the italic of the same family. */
    val displayItalic: TextStyle,
    /** "RECENTS". */
    val sectionCaps: TextStyle,
    /** "GUZAARISH": card title, caps. */
    val cardTitleCaps: TextStyle,
    /** Card label and subtitle, sentence case (Inter). */
    val label: TextStyle,
    /** Card label in letter-spaced caps (the ref1 variant; unused by default). */
    val labelCaps: TextStyle,
) {
    companion object {
        const val headingFontScaleCap = 1.3f

        fun build(font: FontFamily): ViolenceHeadingStyles {
            val display = TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.W400,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.01.em,
            )
            return ViolenceHeadingStyles(
                displayCaps = display,
                displayItalic = display.copy(fontStyle = FontStyle.Italic),
                sectionCaps = display.copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = 0.02.em),
                cardTitleCaps = display.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.02.em),
                label = TextStyle(
                    fontFamily = ViolenceInter,
                    fontWeight = FontWeight.W400,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
                labelCaps = TextStyle(
                    fontFamily = ViolenceInter,
                    fontWeight = FontWeight.W500,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.08.em,
                ),
            )
        }
    }
}

/** Heading tokens read by styled components. The default uses the platform serif: it is never drawn while the style is off. */
val LocalViolenceHeadings = staticCompositionLocalOf { ViolenceHeadingStyles.build(FontFamily.Serif) }

/** Material text weights for the body face: Medium for titles and labels, Regular for body (spec section 5). */
private fun TextStyle.inter(weight: FontWeight): TextStyle = copy(fontFamily = ViolenceInter, fontWeight = weight)

/**
 * The Material typography under the style: Inter everywhere, sizes and tracking as they are today.
 * Returns [base] itself (the very same object) when [enabled] is false.
 */
fun violenceTypography(base: Typography, enabled: Boolean): Typography {
    if (!enabled) return base
    return base.copy(
        displayLarge = base.displayLarge.inter(FontWeight.W600),
        displayMedium = base.displayMedium.inter(FontWeight.W600),
        displaySmall = base.displaySmall.inter(FontWeight.W600),
        headlineLarge = base.headlineLarge.inter(FontWeight.W600),
        headlineMedium = base.headlineMedium.inter(FontWeight.W600),
        headlineSmall = base.headlineSmall.inter(FontWeight.W600),
        titleLarge = base.titleLarge.inter(FontWeight.W600),
        titleMedium = base.titleMedium.inter(FontWeight.W500),
        titleSmall = base.titleSmall.inter(FontWeight.W500),
        bodyLarge = base.bodyLarge.inter(FontWeight.W400),
        bodyMedium = base.bodyMedium.inter(FontWeight.W400),
        bodySmall = base.bodySmall.inter(FontWeight.W400),
        labelLarge = base.labelLarge.inter(FontWeight.W500),
        labelMedium = base.labelMedium.inter(FontWeight.W500),
        labelSmall = base.labelSmall.inter(FontWeight.W500),
    )
}

/** Wraps the Material typography with [violenceTypography]; with the style off it adds nothing. */
@Composable
internal fun ViolenceTypographyScope(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val m = androidx.compose.material3.MaterialTheme
    androidx.compose.material3.MaterialTheme(
        colorScheme = m.colorScheme,
        shapes = m.shapes,
        typography = violenceTypography(m.typography, enabled = true),
        content = content,
    )
}
