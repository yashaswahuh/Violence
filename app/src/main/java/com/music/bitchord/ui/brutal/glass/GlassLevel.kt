package com.music.bitchord.ui.brutal.glass

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How much glass the brutal look draws, decided once from the device and the user's settings
 * ([decideGlassLevel]) and handed to every surface through [LocalGlassLevel] (spec 7.2).
 *
 * - [FULL]: live backdrop glass with the edge lens (the app's existing `liquidGlass` pipeline).
 * - [SIMPLIFIED]: tint, rim and hairline with no backdrop capture and no lens (the app's
 *   `lightweightLiquidGlass` approach). Cheap, and still reads as glass.
 * - [SOLID]: an opaque surface with a hairline. Nothing is sampled or blurred.
 */
enum class GlassLevel { FULL, SIMPLIFIED, SOLID }

/** Everything the decision depends on, so the rules are a pure function and can be tested. */
data class GlassInputs(
    val sdkInt: Int,
    /** `ActivityManager.isLowRamDevice`. */
    val lowRamDevice: Boolean,
    /** `PowerManager.isPowerSaveMode`. */
    val batterySaver: Boolean,
    /** The app's own "Reduce transparency" switch (Android has no system one). */
    val reduceTransparency: Boolean,
    /** The app's own "Reduce animation" switch, or the system animator scale being 0. */
    val reduceMotion: Boolean,
    /** The existing "Reduce dynamic blur" switch: it has always meant "nothing is blurred". */
    val reduceDynamicBlur: Boolean,
)

/** `RenderEffect` blur needs Android 12. */
const val GLASS_MIN_SDK = 31

/** The lens (refraction) shader needs Android 13. */
const val LENS_MIN_SDK = 33

/**
 * The rules, in order:
 * 1. [GlassLevel.SOLID] when glass cannot be drawn (below Android 12), or the user asked for no
 *    transparency ("Reduce transparency") or no blur ("Reduce dynamic blur").
 * 2. [GlassLevel.SIMPLIFIED] when it would be costly or unwanted: a low-RAM device, battery saver,
 *    reduced motion (a live backdrop and the lens are the moving parts), or no lens shader
 *    (Android 12, API 31 and 32).
 * 3. Otherwise [GlassLevel.FULL].
 */
fun decideGlassLevel(i: GlassInputs): GlassLevel = when {
    i.sdkInt < GLASS_MIN_SDK || i.reduceTransparency || i.reduceDynamicBlur -> GlassLevel.SOLID
    i.lowRamDevice || i.batterySaver || i.reduceMotion || i.sdkInt < LENS_MIN_SDK -> GlassLevel.SIMPLIFIED
    else -> GlassLevel.FULL
}

/**
 * A hard limit on live blurred layers (spec 7.1): a surface asks [tryAcquire] before it draws live
 * glass and draws the simplified look when the answer is no; [release] when it leaves the screen.
 * The normal design needs two live surfaces (the player bar or dock, and one sheet).
 */
class GlassBudget(val limit: Int = MAX_LIVE_LAYERS) {
    private var live = 0

    /** Live layers handed out right now. */
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
        const val MAX_LIVE_LAYERS = 3
    }
}

/** Solid until the brutal theme says otherwise, so a stray surface can never assume glass. */
val LocalGlassLevel = staticCompositionLocalOf { GlassLevel.SOLID }

val LocalGlassBudget = staticCompositionLocalOf { GlassBudget() }
