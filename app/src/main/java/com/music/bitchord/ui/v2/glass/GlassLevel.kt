package com.music.bitchord.ui.v2.glass

/**
 * How much glass the new UI draws, decided once from the device and the user's settings
 * ([decideGlassLevel]) and handed to every surface through `LocalGlassLevel`.
 *
 * - [FULL]: live backdrop glass with the edge lens (the app's existing `liquidGlass`).
 * - [SIMPLIFIED]: tint, rim and hairline with no backdrop capture and no lens (the app's
 *   `lightweightLiquidGlass`). Cheap, and still reads as glass.
 * - [SOLID]: opaque surface with a hairline. Nothing is sampled or blurred.
 */
enum class GlassLevel { FULL, SIMPLIFIED, SOLID }

/** Everything the decision depends on, so the rules are a pure function and can be tested. */
data class GlassInputs(
    val sdkInt: Int,
    val lowRamDevice: Boolean,
    val batterySaver: Boolean,
    /** The app's own "Reduce transparency" switch. */
    val reduceTransparency: Boolean,
    /** The app's own "Reduce animation" switch, or the system animator scale being 0. */
    val reduceMotion: Boolean,
    /** The existing "Reduce dynamic blur" switch: it has always meant "nothing is blurred". */
    val reduceDynamicBlur: Boolean,
)

const val GLASS_MIN_SDK = 31 // RenderEffect blur (Android 12)
const val LENS_MIN_SDK = 33 // the lens shader (Android 13)

/**
 * The rules, in order:
 * 1. [GlassLevel.SOLID] when glass cannot be drawn (below Android 12), or the user asked for no
 *    transparency ("Reduce transparency") or no blur ("Reduce dynamic blur").
 * 2. [GlassLevel.SIMPLIFIED] when it would be costly or unwanted: a low-RAM device, battery saver,
 *    reduced motion (the lens and a live backdrop are the moving parts), or no lens shader
 *    (Android 12).
 * 3. Otherwise [GlassLevel.FULL].
 */
fun decideGlassLevel(i: GlassInputs): GlassLevel = when {
    i.sdkInt < GLASS_MIN_SDK || i.reduceTransparency || i.reduceDynamicBlur -> GlassLevel.SOLID
    i.lowRamDevice || i.batterySaver || i.reduceMotion || i.sdkInt < LENS_MIN_SDK -> GlassLevel.SIMPLIFIED
    else -> GlassLevel.FULL
}

/**
 * A hard limit on live blurred layers: a surface asks [tryAcquire] before it draws live glass and
 * draws the simplified look if the answer is no; [release] when it leaves the screen. The limit is
 * 3 (dock plus orb as one surface, one floating cluster, one panel), per `UI_DESIGN.md`.
 */
class GlassBudget(val limit: Int = MAX_LIVE_BLURRED_LAYERS) {
    private var live = 0

    val inUse: Int get() = live

    fun tryAcquire(): Boolean {
        if (live >= limit) return false
        live++
        return true
    }

    fun release() {
        if (live > 0) live--
    }

    companion object {
        const val MAX_LIVE_BLURRED_LAYERS = 3
    }
}
