package com.music.violence.ui.violencestyle

/**
 * Safe mode (STYLE_PLAN.md): if the style is on and the app failed to reach its first screen on two
 * starts in a row, the style turns itself off.
 *
 * `pending` counts starts with the style on whose first screen never displayed. It is raised when a
 * start begins and cleared when the first frame has been drawn.
 */
object StyleSafeModeLogic {

    /** Starts in a row that may fail before the style is switched off. */
    const val MAX_FAILED_STARTS = 2

    class Decision(
        /** New value of the pending counter to store. */
        val pending: Int,
        /** Turn the master toggle off now. */
        val disableStyle: Boolean,
        /** Show the one-time message. */
        val showNotice: Boolean,
    )

    /** Called at the start of every launch, before any style code runs. */
    fun onStart(pending: Int, styleEnabled: Boolean): Decision = when {
        !styleEnabled -> Decision(pending = 0, disableStyle = false, showNotice = false)
        pending >= MAX_FAILED_STARTS -> Decision(pending = 0, disableStyle = true, showNotice = true)
        else -> Decision(pending = pending + 1, disableStyle = false, showNotice = false)
    }

    /** Called once the first frame has been drawn. */
    fun onFirstFrame(): Int = 0
}
