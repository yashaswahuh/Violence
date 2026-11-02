package com.music.bitchord.ui.v2.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.music.bitchord.ui.components.GLASS_EDGE_COLOR
import com.music.bitchord.ui.components.GLASS_EDGE_WIDTH
import com.music.bitchord.ui.components.backdrop.highlight.Highlight
import com.music.bitchord.ui.components.backdrop.highlight.HighlightElement
import com.music.bitchord.ui.components.backdrop.internal.ShapeProvider
import com.music.bitchord.ui.components.liquidGlass
import com.music.bitchord.ui.v2.theme.LocalV2Colors

val LocalGlassLevel = staticCompositionLocalOf { GlassLevel.SOLID }
val LocalGlassBudget = staticCompositionLocalOf { GlassBudget() }

/**
 * A floating glass surface (the dock, an orb, a chip cluster) at the level the device and settings
 * allow, built on the app's existing Liquid Glass so upstream fixes to it carry over:
 *
 * - [GlassLevel.FULL]: `liquidGlass(shape)`, the live backdrop with blur, edge lens, rim and shadow,
 *   if the [GlassBudget] has a live layer to spare; otherwise it drops to SIMPLIFIED for this
 *   surface only.
 * - [GlassLevel.SIMPLIFIED]: tint, the directional rim ([HighlightElement]) and a hairline; nothing
 *   is captured or blurred.
 * - [GlassLevel.SOLID]: the theme's surface colour and a hairline.
 */
@Composable
fun Modifier.v2Glass(shape: CornerBasedShape): Modifier {
    val level = LocalGlassLevel.current
    val budget = LocalGlassBudget.current
    val colors = LocalV2Colors.current
    val granted = remember(level, budget) { level == GlassLevel.FULL && budget.tryAcquire() }
    DisposableEffect(level, budget) {
        onDispose { if (granted) budget.release() }
    }
    val effective = if (level == GlassLevel.FULL && !granted) GlassLevel.SIMPLIFIED else level
    return when (effective) {
        GlassLevel.FULL -> liquidGlass(shape, surfaceTint = colors.glassTint.copy(alpha = colors.glassTintAlpha))
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
        GlassLevel.SIMPLIFIED -> {
            val provider = remember(shape) { ShapeProvider { shape } }
            clip(shape)
                .background(colors.glassTint.copy(alpha = (colors.glassTintAlpha + 0.2f).coerceAtMost(0.9f)), shape)
                .then(HighlightElement(shapeProvider = provider, highlight = { Highlight.Default }))
                .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
        }
        GlassLevel.SOLID -> clip(shape)
            .background(colors.surface, shape)
            .border(GLASS_EDGE_WIDTH, colors.outline, shape)
    }
}

/** A solid surface with a hairline: what every page-level and text-heavy element uses (G0). */
@Composable
fun Modifier.v2Solid(shape: CornerBasedShape, color: Color = LocalV2Colors.current.surface): Modifier =
    clip(shape).background(color, shape).border(GLASS_EDGE_WIDTH, LocalV2Colors.current.outline, shape)
