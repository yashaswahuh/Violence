package com.music.bitchord.ui.v2.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R

/**
 * Colour tokens of the new UI (Violence palette, `UI_DESIGN.md` section 0). Contrast ratios are
 * for solid colours and are asserted by `V2ColorsTest`.
 */
@Immutable
data class V2Colors(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    /** The glass tint: drawn over the sampled backdrop at [glassTintAlpha]. */
    val glassTint: Color,
    val glassTintAlpha: Float,
    val ink: Color,
    val ink2: Color,
    /** The accent for fills; white text on it is at least 4.5:1. */
    val voltage: Color,
    /** The accent for text and icons on [background]. */
    val voltageText: Color,
    val ember: Color,
    val mint: Color,
    val outline: Color,
    val focusRing: Color,
) {
    companion object {
        val Dark = V2Colors(
            dark = true,
            background = Color(0xFF0B0B12),
            surface = Color(0xFF14121D),
            glassTint = Color(0xFF14121D),
            glassTintAlpha = 0.48f,
            ink = Color(0xFFF4F2FA),
            ink2 = Color(0xFFA7A5B8),
            voltage = Color(0xFF7C3AED),
            voltageText = Color(0xFFC4A8FF),
            ember = Color(0xFFFF6B8A),
            mint = Color(0xFF5EEAD4),
            outline = Color(0xFF2A2638),
            focusRing = Color(0xFFC4A8FF),
        )
        val Light = V2Colors(
            dark = false,
            background = Color(0xFFF6F4FB),
            surface = Color(0xFFFFFFFF),
            glassTint = Color(0xFFEDE9F7),
            glassTintAlpha = 0.62f,
            ink = Color(0xFF15131F),
            ink2 = Color(0xFF5B586B),
            voltage = Color(0xFF7C3AED),
            voltageText = Color(0xFF6D28D9),
            ember = Color(0xFFB4234A),
            mint = Color(0xFF0F766E),
            outline = Color(0xFFDAD5E8),
            focusRing = Color(0xFF6D28D9),
        )
    }
}

val LocalV2Colors = staticCompositionLocalOf { V2Colors.Dark }

/** Spacing, shapes and sizes. The 4 dp grid; nothing interactive is smaller than [MinTouch]. */
object V2Dimens {
    val MinTouch: Dp = 48.dp
    val Gap1: Dp = 4.dp
    val Gap2: Dp = 8.dp
    val Gap3: Dp = 12.dp
    val Gap4: Dp = 16.dp
    val Gap5: Dp = 24.dp
    val Gap6: Dp = 32.dp
    val ScreenEdge: Dp = 16.dp
    val DockCorner: Dp = 36.dp
    val DockMinHeight: Dp = 70.dp
    val OrbSize: Dp = 64.dp
    val FocusRingWidth: Dp = 3.dp
    val HairlineWidth: Dp = 0.5.dp
}

object V2Shapes {
    val Chip = RoundedCornerShape(50)
    val Card = RoundedCornerShape(20.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Dock = RoundedCornerShape(V2Dimens.DockCorner)
}

/** Elevation is shadow only where glass draws its own; flat surfaces use a hairline instead. */
object V2Elevation {
    val None: Dp = 0.dp
    val Floating: Dp = 12.dp
}

/**
 * Motion tokens. Movement is a spring; fades and small state changes are short tweens. With
 * [reduced] (the app's "Reduce animation" or the system animator scale at 0) there is no
 * overshoot and no travel: only a very short fade.
 */
@Immutable
class V2Motion(val reduced: Boolean) {
    /** Dock, orb, chips. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        if (reduced) tween(durationMillis = 90) else spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Sheets and panels. */
    fun <T> panel(): FiniteAnimationSpec<T> =
        if (reduced) tween(durationMillis = 90) else spring(dampingRatio = 0.9f, stiffness = 260f)

    /** Opacity and colour changes. */
    fun <T> fade(): AnimationSpec<T> = tween(durationMillis = if (reduced) 0 else 150)

    companion object {
        /** The spring stiffness constants this app stays inside, for tests and docs. */
        const val STANDARD_STIFFNESS = 380f
        const val NO_BOUNCE = Spring.DampingRatioNoBouncy
    }
}

val LocalV2Motion = staticCompositionLocalOf { V2Motion(reduced = false) }

/**
 * Sora for titles and Inter for body and labels, both bundled variable fonts under the SIL Open
 * Font License 1.1 (see NOTICE). Variable weights need Android 8 (API 26), which is the app's
 * minSdk. Scale: Display 32 / Title 22 / Heading 18 / Body 16 / Label 14 / Caption 12 (sp); nothing
 * smaller than 12 sp, and no text is ever placed in a fixed-height box.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object V2Type {
    private fun sora(weight: Int) = Font(
        R.font.v2_sora,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    private fun inter(weight: Int) = Font(
        R.font.v2_inter,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    val Display: FontFamily = FontFamily(sora(500), sora(600), sora(700))
    val Body: FontFamily = FontFamily(inter(400), inter(500), inter(600), inter(700))

    val display = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp)
    val title = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val heading = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val label = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)

    /** The whole scale as a Material typography, so shared pages pick it up through the theme. */
    val material = Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = title,
        headlineLarge = display,
        headlineMedium = title,
        headlineSmall = heading,
        titleLarge = title,
        titleMedium = heading,
        titleSmall = label,
        bodyLarge = body,
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = caption,
        labelLarge = label,
        labelMedium = caption,
        labelSmall = caption,
    )
}
