package com.music.bitchord.ui.brutal.safemode

import com.music.bitchord.ui.brutal.UiVariant

/** The few values safe mode keeps, behind an interface so the logic is a pure JVM unit test. */
interface SafeModeStore {
    var startMarker: Boolean
    var failedStarts: Int
    var revertNotice: Boolean
}

/**
 * Crash-safe fallback for the brutal look.
 *
 * A *marker* is set when the new UI begins to compose ([begin]) and cleared once its first screen
 * has been displayed ([shown]). A marker still set at the next launch means the last start never
 * got that far (a crash, or a kill during start). Two such starts in a row make the next launch go
 * back to [UiVariant.CURRENT] and raise a one-time notice.
 *
 * A start that reaches its first screen resets the count, so only *consecutive* failures count.
 */
class BrutalSafeModeLogic(private val store: SafeModeStore) {

    /**
     * Call once at process start, before anything composes. Returns the variant to run: [wanted],
     * or [UiVariant.CURRENT] if the new UI has failed [MAX_FAILED_STARTS] starts in a row (in which
     * case the caller must also persist CURRENT as the setting).
     */
    fun startupCheck(wanted: UiVariant): UiVariant {
        if (wanted != UiVariant.BRUTAL) {
            // Not running the new UI: nothing can fail, and old state must not linger.
            store.startMarker = false
            store.failedStarts = 0
            return wanted
        }
        if (store.startMarker) {
            store.failedStarts += 1
            store.startMarker = false
        }
        if (store.failedStarts >= MAX_FAILED_STARTS) {
            store.failedStarts = 0
            store.revertNotice = true
            return UiVariant.CURRENT
        }
        return wanted
    }

    /** The new UI is about to compose for the first time this process. */
    fun begin() {
        store.startMarker = true
    }

    /** The new UI's first screen is on screen. */
    fun shown() {
        store.startMarker = false
        store.failedStarts = 0
    }

    /** True once after a revert; reading it clears it. */
    fun consumeRevertNotice(): Boolean {
        val pending = store.revertNotice
        store.revertNotice = false
        return pending
    }

    companion object {
        const val MAX_FAILED_STARTS = 2
    }
}
