package com.music.bitchord.ui.brutal.glass

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min

/**
 * The geometry the glass layers are drawn from. Everything here is plain arithmetic on sizes in
 * pixels, so it is unit-tested on the JVM and is computed once per size, never per frame.
 */

/** The four corner radii of a shape at a size, in pixels, in screen (not start/end) order. */
data class CornerRadiiPx(val topLeft: Float, val topRight: Float, val bottomRight: Float, val bottomLeft: Float) {
    val largest: Float get() = max(max(topLeft, topRight), max(bottomRight, bottomLeft))
}

/**
 * The radii of [shape] at [size]. Start and end are resolved to left and right by [layoutDirection],
 * and each radius is capped at half the shorter side (a corner cannot be larger than that).
 */
fun cornerRadiiPx(shape: CornerBasedShape, size: Size, density: Density, layoutDirection: LayoutDirection): CornerRadiiPx {
    val cap = min(size.width, size.height) / 2f
    val ltr = layoutDirection == LayoutDirection.Ltr
    val ts = shape.topStart.toPx(size, density).coerceIn(0f, cap)
    val te = shape.topEnd.toPx(size, density).coerceIn(0f, cap)
    val bs = shape.bottomStart.toPx(size, density).coerceIn(0f, cap)
    val be = shape.bottomEnd.toPx(size, density).coerceIn(0f, cap)
    return if (ltr) CornerRadiiPx(ts, te, be, bs) else CornerRadiiPx(te, ts, bs, be)
}

/**
 * How far along the edges a specular arc reaches from its corner. About two and a half corner
 * radii, never shorter than [minReachPx] (small corners still get a readable arc) and never longer
 * than half the long side (an arc must not run the whole bar).
 */
fun arcReachPx(cornerRadiusPx: Float, size: Size, minReachPx: Float): Float {
    val cap = max(size.width, size.height) / 2f
    return (cornerRadiusPx * ARC_REACH_RADII).coerceAtLeast(minReachPx).coerceAtMost(cap)
}

const val ARC_REACH_RADII = 3.2f

/** One band of the under-glow: a stroke [widthPx] wide (half of it outside the shape) at [alpha]. */
data class HaloStep(val widthPx: Float, val alpha: Float)

/**
 * The under-glow as [count] nested strokes standing in for a blur (no `RenderEffect`, no layer).
 * Band `i` reaches `reach * i / count` outside the shape. Alphas are chosen so the alpha *summed*
 * at distance `d` from the edge falls like `total * (1 - d / reach)^2`: strong at the edge, a long
 * soft tail, zero at [reachPx]. The sum at the edge equals [totalAlpha].
 */
fun haloSteps(totalAlpha: Float, reachPx: Float, count: Int = HALO_STEPS): List<HaloStep> {
    require(count >= 1) { "count must be at least 1" }
    if (totalAlpha <= 0f || reachPx <= 0f) return emptyList()
    // cumulative[i] is the summed alpha inside band i (i = 0 is the band at the edge).
    fun cumulative(i: Int): Float {
        if (i >= count) return 0f
        val d = 1f - i.toFloat() / count
        return totalAlpha * d * d
    }
    return (0 until count).map { i ->
        HaloStep(
            widthPx = 2f * reachPx * (i + 1).toFloat() / count,
            alpha = (cumulative(i) - cumulative(i + 1)).coerceAtLeast(0f),
        )
    }
}

const val HALO_STEPS = 9

/**
 * Where the accent light sits inside a surface (spec 2.1 layer 4). All points are in pixels within
 * (or just outside) the surface; the surface clips them.
 */
data class BleedSpec(
    val linearStart: Offset,
    val linearEnd: Offset,
    val hotCenter: Offset,
    val hotRadius: Float,
    val endCenter: Offset,
    val endRadius: Float,
)

/**
 * The bleed for a surface of [size]: a linear wash climbing from the bottom edge near the start
 * corner (where the artwork sits in a bar) to about 55 percent of the way up, a radial hot spot in
 * the start corner, and a smaller one in the end corner. Proportions are read off the mockup's
 * bars and tiles; they are APPROXIMATE.
 */
fun bleedSpec(size: Size): BleedSpec {
    val w = size.width
    val h = size.height
    val side = min(w, h)
    return BleedSpec(
        linearStart = Offset(w * 0.12f, h),
        linearEnd = Offset(w * 0.55f, h * 0.12f),
        hotCenter = Offset(w * 0.12f, h * 1.02f),
        hotRadius = max(side * 0.95f, w * 0.30f),
        endCenter = Offset(w * 0.92f, h * 1.02f),
        endRadius = side * 0.75f,
    )
}

/**
 * The diagonal gloss: a gradient across the end 40 percent of the surface from its top-end corner
 * toward the bottom-start, so the sheen never reaches the title area (spec 2.1 layer 3).
 */
fun glossLine(size: Size): Pair<Offset, Offset> =
    Offset(size.width, 0f) to Offset(size.width * 0.55f, size.height * 0.95f)

/**
 * How much of the bleed a surface of this short side gets. A bar or panel (80 dp and up) gets all of
 * it; a round button or a rail tab (48 dp and under) gets about half, because the same wash on a small
 * surface floods it; in between it is linear. [minSideDp] is the surface's shorter side in dp.
 */
fun bleedScaleFor(minSideDp: Float): Float = (0.5f + 0.5f * ((minSideDp - 48f) / (80f - 48f))).coerceIn(0.5f, 1f)