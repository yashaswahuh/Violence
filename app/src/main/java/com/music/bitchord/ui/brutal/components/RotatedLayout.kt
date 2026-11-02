package com.music.bitchord.ui.brutal.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rotation that takes part in layout.
 *
 * `Modifier.rotate` and `graphicsLayer { rotationZ }` only move pixels: the parent still measures the
 * unrotated box, so a tilted sticker pokes out of it and gets clipped, and a text turned on its side
 * keeps the wrong width and height. The two modifiers here measure the *rotated* shape.
 */

/** The size of the smallest axis-aligned box that holds a [width] x [height] rectangle turned by [degrees]. */
data class RotatedBounds(val width: Float, val height: Float)

fun rotatedBounds(width: Float, height: Float, degrees: Float): RotatedBounds {
    val r = Math.toRadians(degrees.toDouble())
    val c = abs(cos(r)).toFloat()
    val s = abs(sin(r)).toFloat()
    return RotatedBounds(width = width * c + height * s, height = width * s + height * c)
}

/**
 * The widest a box of [heightToWidth] (height over width) may be so that, turned by [degrees], it
 * still fits [maxWidth]: `w * (cos + ratio * sin) <= maxWidth`.
 */
fun widthThatFitsWhenRotated(maxWidth: Float, heightToWidth: Float, degrees: Float): Float {
    val r = Math.toRadians(degrees.toDouble())
    val c = abs(cos(r)).toFloat()
    val s = abs(sin(r)).toFloat()
    return maxWidth / (c + heightToWidth * s)
}

/**
 * Turns the content by [degrees] (negative is counter-clockwise) about its centre and reports the
 * size of the rotated shape to the parent, so nothing is clipped. When the width is bounded, the
 * content is given a narrow enough width that the rotated shape fits: a sticker at a large font size
 * wraps its lines and grows taller instead of running out of the window. The rotation is a static
 * `graphicsLayer`: nothing animates.
 *
 * Compose allows a child to be measured **once**, so the width is found with intrinsic measurements
 * (which may be asked any number of times) and the child is then measured a single time at it.
 */
fun Modifier.rotatedLayout(degrees: Float): Modifier = layout { measurable, constraints ->
    val bounded = constraints.hasBoundedWidth
    val maxWidth = constraints.maxWidth.toFloat()
    val preferred = measurable.maxIntrinsicWidth(Constraints.Infinity)
    val narrowest = measurable.minIntrinsicWidth(Constraints.Infinity)
    var width = preferred
    if (bounded) {
        // First guess: a plate about a third as tall as it is wide. Corrected from the height it really needs.
        width = minOf(preferred, widthThatFitsWhenRotated(maxWidth, 0.35f, degrees).toInt())
        var attempts = 0
        while (attempts < 4) {
            width = width.coerceAtLeast(narrowest).coerceAtLeast(1)
            val height = measurable.minIntrinsicHeight(width)
            val bounds = rotatedBounds(width.toFloat(), height.toFloat(), degrees)
            if (bounds.width <= maxWidth || width <= narrowest) break
            width = widthThatFitsWhenRotated(maxWidth, height.toFloat() / width, degrees).toInt()
            attempts++
        }
    }
    val placeable = measurable.measure(
        Constraints(minWidth = 0, maxWidth = width.coerceAtLeast(0), minHeight = 0, maxHeight = Constraints.Infinity),
    )
    val bounds = rotatedBounds(placeable.width.toFloat(), placeable.height.toFloat(), degrees)
    val w = (bounds.width.toInt() + 1).let { if (bounded) minOf(it, constraints.maxWidth) else it }
    val h = bounds.height.toInt() + 1
    layout(w, h) {
        placeable.placeWithLayer((w - placeable.width) / 2, (h - placeable.height) / 2) {
            rotationZ = degrees
            transformOrigin = TransformOrigin.Center
        }
    }
}
/**
 * Text turned to run bottom to top (a vertical label), with the **layout size swapped**: the parent
 * sees a box as wide as one line is tall and as tall as the text is long. The text runs out along
 * the incoming maximum *height* (it ellipsises there, so a long title never outgrows its tile) and
 * the lane's width follows the font size, so at 200 percent the lane widens instead of clipping.
 *
 * The turn is -90 degrees in both text directions (see spec 5.5: whether that suits right-to-left
 * scripts is UNCLEAR until checked on a device in Hebrew).
 */
fun Modifier.verticalText(): Modifier = layout { measurable, constraints ->
    val length = if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity
    val lane = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
    val placeable = measurable.measure(
        Constraints(minWidth = 0, maxWidth = length, minHeight = 0, maxHeight = lane),
    )
    layout(placeable.height, placeable.width) {
        // Pivot at the placed top-left corner: (x, y) maps to (y, -x), so the rotated box spans
        // [0, height] across and [-width, 0] down; placing it width down brings it to [0, width].
        placeable.placeWithLayer(0, placeable.width) {
            rotationZ = -90f
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}
