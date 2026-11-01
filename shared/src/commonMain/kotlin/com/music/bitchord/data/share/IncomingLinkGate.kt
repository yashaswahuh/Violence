package com.music.bitchord.data.share

/**
 * Keeps repeated and runaway reads from piling up confirmations.
 *
 * An NFC reader can see the same tag every time its presence check runs, a camera sees a QR
 * code on every frame, and a hostile tag stream could offer links endlessly. So:
 *  - the **same** link seen again within [repeatWindowMs] of the last time it was seen is ignored
 *    (each ignored sighting extends the window, so holding the phones together does not retrigger);
 *  - at most [maxDistinct] **different** links are admitted per [rateWindowMs].
 * Time is passed in, so this is deterministic to test. Not thread-safe by itself: callers lock around it.
 */
class IncomingLinkGate(
    private val repeatWindowMs: Long = 10_000,
    private val maxDistinct: Int = 5,
    private val rateWindowMs: Long = 10_000,
) {
    private val lastSeen = HashMap<String, Long>()
    private val admitted = ArrayList<Long>()

    /** True if the link [key] should be offered to the user now. */
    fun admit(key: String, nowMs: Long): Boolean {
        lastSeen.entries.removeAll { nowMs - it.value > repeatWindowMs }
        admitted.removeAll { nowMs - it > rateWindowMs }
        val previous = lastSeen[key]
        lastSeen[key] = nowMs
        if (previous != null) return false
        if (admitted.size >= maxDistinct) return false
        admitted.add(nowMs)
        return true
    }

    /** Lets the user deliberately offer [key] again right away (they dismissed it and tapped again). */
    fun forget(key: String) {
        lastSeen.remove(key)
    }
}
