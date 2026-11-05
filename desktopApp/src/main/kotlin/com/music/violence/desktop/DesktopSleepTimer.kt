package com.music.violence.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow

/** Desktop equivalent of Android's elapsed-realtime sleep timer. */
object DesktopSleepTimer {
    private val persistence = DesktopPersistence()
    private val deadlineNs = AtomicLong(0L)

    const val FADE_DURATION_MS = 30_000L
    const val OUTRO_FADE_DURATION_MS = 3_000L

    val minutes = MutableStateFlow<Int?>(null)
    val afterTrack = MutableStateFlow(false)
    val fadeToSilence = MutableStateFlow(persistence.boolean(KEY_FADE, true))
    val finishCurrentTrack = MutableStateFlow(persistence.boolean(KEY_FINISH_TRACK, false))

    val presets = listOf(15, 30, 45, 60)

    fun start(minutes: Int) {
        require(minutes > 0) { "Sleep timer duration must be positive" }
        afterTrack.value = false
        this.minutes.value = minutes
        deadlineNs.set(System.nanoTime() + minutes * 60L * 1_000_000_000L)
    }

    fun startAfterTrack() {
        minutes.value = null
        deadlineNs.set(0L)
        afterTrack.value = true
    }

    fun cancel() {
        minutes.value = null
        deadlineNs.set(0L)
        afterTrack.value = false
    }

    fun setFadeToSilence(enabled: Boolean) {
        fadeToSilence.value = enabled
        persistence.saveBoolean(KEY_FADE, enabled)
    }

    fun setFinishCurrentTrack(enabled: Boolean) {
        finishCurrentTrack.value = enabled
        persistence.saveBoolean(KEY_FINISH_TRACK, enabled)
    }

    fun remainingMs(): Long? = deadlineNs.get().takeIf { it != 0L }
        ?.let { (it - System.nanoTime()).coerceAtLeast(0L) / 1_000_000L }

    fun isExpired(): Boolean = deadlineNs.get() != 0L && remainingMs() == 0L

    fun calculateFadeGain(progress: Float): Float {
        val clamped = progress.coerceIn(0f, 1f)
        return (1.0f - clamped).pow(2.5f)
    }

    private const val KEY_FADE = "sleep_timer_fade"
    private const val KEY_FINISH_TRACK = "sleep_timer_finish_track"
}
