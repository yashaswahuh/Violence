package com.music.violence.playback

import android.os.SystemClock
import com.music.violence.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Sleep timer. Holds a deadline; [PlaybackService] watches it and pauses
 * playback once it passes.
 *
 * A deadline rather than a countdown: nothing has to tick for the timer to
 * stay accurate, so it survives the player UI being dismissed, and any
 * observer can work out how long is left for itself. elapsedRealtime, not
 * wall clock, so changing the system time can't cut a timer short.
 *
 * Includes audiophile logarithmic fade-to-silence and finish-current-track completion.
 */
object SleepTimer {

    /** Deadline on [SystemClock.elapsedRealtime], or null when no timer is set. */
    val deadline = MutableStateFlow<Long?>(null)

    /** The preset that was chosen, so the picker can tick it. Null when off. */
    val minutes = MutableStateFlow<Int?>(null)

    /**
     * Pause when the current track ends instead of after a fixed wait.
     */
    val afterTrack = MutableStateFlow(false)

    /**
     * Whether to gently fade volume to silence (-80 dB) over the final 30 seconds.
     */
    val fadeToSilence = MutableStateFlow(true)

    /**
     * When timer expires, wait for current track to naturally conclude with 3-second outro fade.
     */
    val finishCurrentTrack = MutableStateFlow(false)

    /** Durations offered in the picker. */
    val PRESETS = listOf(15, 30, 45, 60)

    /** The custom picker moves in steps this size, from one step up to [MAX_MINUTES]. */
    const val STEP_MINUTES = 5

    /** Longest custom duration: twelve hours. */
    const val MAX_MINUTES = 720

    /** Final fade-to-silence duration in milliseconds (30 seconds). */
    const val FADE_DURATION_MS = 30_000L

    /** Gentle track outro fade duration in milliseconds (3 seconds). */
    const val OUTRO_FADE_DURATION_MS = 3_000L

    /**
     * Calculates the psychoacoustic logarithmic fade gain: Gain = (1.0 - progress)^2.5.
     * [progress] is in range 0.0 (fade start / 0 dB) to 1.0 (silence / -80 dB).
     */
    fun calculateFadeGain(progress: Float): Float {
        val x = progress.coerceIn(0f, 1f)
        return (1.0f - x).pow(2.5f)
    }

    /** The custom duration a slider at [fraction] (0..1) of its travel stands for. */
    fun minutesAt(fraction: Float): Int {
        val raw = STEP_MINUTES + fraction.coerceIn(0f, 1f) * (MAX_MINUTES - STEP_MINUTES)
        return (raw / STEP_MINUTES).roundToInt() * STEP_MINUTES
    }

    /** Where [minutes] sits along the custom slider, the inverse of [minutesAt]. */
    fun fractionOf(minutes: Int): Float =
        (minutes - STEP_MINUTES).toFloat() / (MAX_MINUTES - STEP_MINUTES)

    /** Whether any kind of timer is currently armed. */
    val isRunning: Boolean get() = deadline.value != null || afterTrack.value

    fun start(
        minutes: Int,
        fade: Boolean = AppSettings.sleepTimerFade.value,
        finishTrack: Boolean = AppSettings.sleepTimerFinishTrack.value,
    ) {
        afterTrack.value = false
        fadeToSilence.value = fade
        finishCurrentTrack.value = finishTrack
        this.minutes.value = minutes
        deadline.value = SystemClock.elapsedRealtime() + minutes * 60_000L
    }

    /** Pause once the track playing right now finishes. */
    fun startAfterTrack() {
        minutes.value = null
        deadline.value = null
        afterTrack.value = true
    }

    fun cancel() {
        minutes.value = null
        deadline.value = null
        afterTrack.value = false
    }

    /** How long is left, or null when no timer is running. */
    fun remainingMs(): Long? =
        deadline.value?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }
}
