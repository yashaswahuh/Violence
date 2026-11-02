package com.music.bitchord.ui.brutal.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.components.ContinuousCornerShape

/**
 * Colour tokens of the brutal look (spec sections 1.1 and 3). The accent parts come from the user's
 * setting through [BrutalAccentRules.derive]; the rest are measured off the mockups.
 *
 * Dark only in this first version (spec open question 3): the mockups have no light theme.
 */
@Immutable
data class BrutalPalette(
    val page: Color,
    val ink: Color,
    val white: Color,
    /** Secondary acid colour (the cyan emphasis in the mockup). */
    val acid: Color,
    /** Muted text on the page and on glass: at least 4.5:1 on [glassBodyBright]. */
    val textMuted: Color,
    /** The glass body over the page: white at alpha 0.09 (measured `#1D1D1D`). */
    val glassBody: Color,
    /** The brightest glass measured (transport bar): the worst case for text on glass. */
    val glassBodyBright: Color,
    val hairline: Color,
    /** The derived accent colours (spec 3.2). */
    val accent: AccentColors,
    /** Whether the Neon look is on (the "Glow" setting; spec 3.5). */
    val glow: Boolean,
) {
    val accentUi: Color get() = Color(accent.ui)
    val accentText: Color get() = Color(accent.text)
    val onAccent: Color get() = Color(accent.onAccent)
    val accentFill: Color get() = Color(accent.fill)

    companion object {
        val Page = Color(BrutalAccentRules.PAGE)
        val Ink = Color(BrutalAccentRules.INK)

        fun of(accent: BrutalAccent, glow: Boolean): BrutalPalette = BrutalPalette(
            page = Page,
            ink = Ink,
            white = Color.White,
            acid = Color(0xFF1FDEDB),
            textMuted = Color(0xFFA8A8B0),
            glassBody = Color(BrutalAccentRules.GLASS_BODY),
            glassBodyBright = Color(BrutalAccentRules.GLASS_WORST),
            hairline = Color.White.copy(alpha = 0.10f),
            accent = BrutalAccentRules.derive(accent.argb),
            glow = glow,
        )

        val Default: BrutalPalette = of(BrutalAccent.Default, glow = true)
    }
}

val LocalBrutalPalette = staticCompositionLocalOf { BrutalPalette.Default }

/** The 4 dp grid and the sizes the spec fixes (section 1.2). Nothing interactive is under [MinTouch]. */
object BrutalDimens {
    val MinTouch: Dp = 48.dp
    val Gap1: Dp = 4.dp
    val Gap2: Dp = 8.dp
    val Gap3: Dp = 12.dp
    val Gap4: Dp = 16.dp
    val Gap5: Dp = 24.dp
    val Gap6: Dp = 32.dp
    val ScreenEdge: Dp = 16.dp

    /** Masonry gap and tile frame (the glass border around artwork). */
    val MasonryGap: Dp = 12.dp
    val TileFrame: Dp = 5.dp

    /** The vertical label lane beside a tile column (stamp and title), plus its gap. */
    val LabelLane: Dp = 28.dp
    val LabelLaneGap: Dp = 6.dp

    /** The rail lane: a 48 dp rail plus an 8 dp edge inset (spec 6.2). */
    val RailItemWidth: Dp = 48.dp
    val RailLane: Dp = 56.dp
    val RailGapTabs: Dp = 4.dp
    val RailGapButtons: Dp = 40.dp
    val RailGapSearchToTabs: Dp = 28.dp
    val RailTabEndPadding: Dp = 16.dp
    val SearchButtonHeight: Dp = 72.dp

    /** Bars: the mini player (PLAYING) and the dock (PAUSE + transport). */
    val MiniPlayerHeight: Dp = 80.dp
    val DockHeight: Dp = 96.dp
    val BarArtwork: Dp = 56.dp
    val PauseCircle: Dp = 68.dp

    val PlayDisc: Dp = 28.dp
    val ChipMinHeight: Dp = 20.dp
    val ListArtwork: Dp = 68.dp

    /** Sticker rules and rim strokes. */
    val StickerRule: Dp = 2.dp
    val RimStroke: Dp = 2.dp

    /** The visible focus ring, in the accent (spec 7.5 and the components' rule). */
    val FocusRing: Dp = 2.dp

    /** The sticker rows, perpendicular heights (spec 1.2). */
    val StickerRow1: Dp = 62.dp
    val StickerRow2: Dp = 62.dp
    val StickerRow3: Dp = 47.dp

    /** Header zone and masthead tile. */
    val HeaderHeight: Dp = 96.dp
    val MastheadWidth: Dp = 300.dp
    val MastheadHeight: Dp = 128.dp
    val MastheadBleed: Dp = 24.dp

    /** Display text is not scaled past this font scale (spec 5.2); everything else scales fully. */
    const val DISPLAY_FONT_SCALE_CAP = 1.3f

    /** At this font scale (or when the rail does not fit) the rail becomes a bottom dock (spec 6.9). */
    const val RAIL_TO_DOCK_FONT_SCALE = 1.5f
}

/** Rotation angles fitted to the mockups (spec 1.3), degrees; negative is counter-clockwise. */
object BrutalAngles {
    const val STICKER = -12f
    const val MASTHEAD = -8.5f
    const val TAG = -4f
    const val VERTICAL_LABEL = -90f

    /** Where a sticker starts its one-shot settle (skipped under reduced motion). */
    const val STICKER_SETTLE_FROM = -16f
}

object BrutalShapes {
    /** Tiles and list artwork are continuous (squircle) corners: 0.19 and 0.20 of the short side. */
    val Tile: CornerBasedShape = ContinuousCornerShape(percent = 19)
    val ListArtwork: CornerBasedShape = ContinuousCornerShape(percent = 20)

    /** Stadium: bars, chips, discs, rail items (radius = half the short side). */
    val Stadium: CornerBasedShape = ContinuousCornerShape(percent = 50)

    /** The masthead glass tile: about a third of its height. */
    val Masthead: CornerBasedShape = ContinuousCornerShape(percent = 33)

    /** The sticker has sharp corners. */
    val Sticker = RectangleShape

    /** Spec 1.2: tile radius = 0.19 x the short side, kept between 12 and 28 dp. */
    fun tileCornerDp(shortSideDp: Float): Float = (0.19f * shortSideDp).coerceIn(12f, 28f)
}

/**
 * Motion tokens. Movement is a spring; fades and small state changes are short tweens. With
 * [reduced] (the app's "Reduce animation", or the system animator scale at 0) there is no
 * overshoot and no travel: only a very short fade, and no tilt animation at all (spec 7.5).
 */
@Immutable
class BrutalMotion(val reduced: Boolean) {
    /** Rail, chips, bars. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        if (reduced) tween(durationMillis = REDUCED_FADE_MS) else spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Sheets and panels. */
    fun <T> panel(): FiniteAnimationSpec<T> =
        if (reduced) tween(durationMillis = REDUCED_FADE_MS) else spring(dampingRatio = 0.9f, stiffness = 260f)

    /** Opacity and colour changes. */
    fun <T> fade(): AnimationSpec<T> = tween(durationMillis = if (reduced) 0 else 150)

    /** The rail fold (spec 6.3): 200 ms, instant when reduced. */
    fun <T> railFold(): FiniteAnimationSpec<T> = tween(durationMillis = if (reduced) 0 else 200)

    /** Whether a sticker may settle from a steeper angle when a page opens. Never when reduced. */
    val allowsTiltSettle: Boolean get() = !reduced

    /** Whether the Neon glow may pulse once when playback starts. Never when reduced; never idle. */
    val allowsGlowPulse: Boolean get() = !reduced

    companion object {
        const val REDUCED_FADE_MS = 90
    }
}

val LocalBrutalMotion = staticCompositionLocalOf { BrutalMotion(reduced = false) }

/** The density with its font scale capped for display text (spec 5.2); a copy, nothing global. */
fun Density.withDisplayFontScale(cap: Float = BrutalDimens.DISPLAY_FONT_SCALE_CAP): Density =
    if (fontScale <= cap) this else Density(density = density, fontScale = cap)
