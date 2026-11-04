package com.music.violence.ui.violencestyle

/**
 * FULL versus LITE and the reduce-motion rules (STYLE_SPEC.md section 10), as pure functions.
 * The Android readers that feed them are in [StyleSignals].
 */
object StyleCapabilityRules {

    /** The lowest API with Liquid Glass (RenderEffect). */
    const val MIN_GLASS_SDK = 31

    /**
     * The first reason the style must run in LITE, or null for FULL. Order is the order the
     * reasons are shown to the user.
     */
    fun liteReason(
        sdkInt: Int,
        lowRamDevice: Boolean,
        batterySaver: Boolean,
        thermalSevere: Boolean,
        reduceDynamicBlur: Boolean,
        userReducedTransparency: Boolean,
    ): LiteReason? = when {
        sdkInt < MIN_GLASS_SDK -> LiteReason.OLD_ANDROID
        lowRamDevice -> LiteReason.LOW_RAM
        batterySaver -> LiteReason.BATTERY_SAVER
        thermalSevere -> LiteReason.THERMAL
        reduceDynamicBlur -> LiteReason.REDUCE_BLUR
        userReducedTransparency -> LiteReason.USER_REDUCED_TRANSPARENCY
        else -> null
    }

    fun capability(reason: LiteReason?): StyleCapability =
        if (reason == null) StyleCapability.FULL else StyleCapability.LITE

    /** The system animator duration scale of 0 means "remove animations". */
    fun systemReducesMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

    /** Reduce transparency: forced by the user, or following the in-app Reduce dynamic blur. */
    fun reduceTransparency(forced: Boolean, reduceDynamicBlur: Boolean): Boolean = forced || reduceDynamicBlur

    /**
     * The continuous background clock may run only when the style is on, motion is not Off, the mode
     * is FULL, nothing asks for less motion, the app is in the foreground and the full player is not
     * covering the page. (Used by S2; defined and tested here.)
     */
    fun backgroundClockMayRun(
        state: StyleState,
        appInForeground: Boolean,
        playerCoversPage: Boolean,
    ): Boolean = state.enabled &&
        state.background != StyleBackground.OFF &&
        state.effectiveMotion != StyleMotion.OFF &&
        appInForeground &&
        !playerCoversPage
}
