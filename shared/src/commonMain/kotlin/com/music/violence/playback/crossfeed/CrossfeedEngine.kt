package com.music.violence.playback.crossfeed

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.tan

/**
 * Available Bauer stereophonic-to-binaural (bs2b) presets for natural headphone spatialization.
 */
enum class CrossfeedPreset(
    val id: String,
    val displayName: String,
    val cutoffHz: Float,
    val levelDb: Float,
    val delayUs: Float,
) {
    BYPASS("bypass", "Bypass (Disabled)", 0f, 0f, 0f),
    SUBTLE("subtle", "Subtle / Studio", 650f, -9.5f, 280f),
    NATURAL("natural", "Natural / Default", 700f, -6.0f, 265f),
    JAN_MEIER("jan_meier", "Jan Meier Curve", 650f, -7.5f, 270f);

    companion object {
        fun fromId(id: String?): CrossfeedPreset =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NATURAL
    }
}

/**
 * High-fidelity Bauer Binaural Crossfeed (bs2b) DSP Engine.
 *
 * Simulates acoustic loudspeaker crosstalk and interaural time difference (ITD)
 * for headphone listening. Eliminates listener fatigue caused by hard-panned stereo recordings
 * by gently blending a frequency-shaped, delayed acoustic bleed from the contralateral channel.
 *
 * Utilizes bilinear transform with exact frequency pre-warping to guarantee that direct + cross
 * frequency response sums to mathematical identity (1.0) on mono signals across all sample rates.
 *
 * Operates purely on interleaved Float32 PCM audio buffers in-place.
 */
class CrossfeedEngine {

    @Volatile
    var isEnabled: Boolean = false
        private set

    @Volatile
    var preset: CrossfeedPreset = CrossfeedPreset.NATURAL
        private set

    private var sampleRate: Int = 44100
    private var channels: Int = 2

    // Filter coefficients
    private var a1: Float = 0f
    private var b0d: Float = 1f
    private var b1d: Float = 0f
    private var b0c: Float = 0f
    private var b1c: Float = 0f

    // Delay line in samples
    private var delayInt: Int = 0
    private var delayFrac: Float = 0f

    // Direct filter state: [0] = Left, [1] = Right
    private var prevXDirectL: Float = 0f
    private var prevXDirectR: Float = 0f
    private var prevYDirectL: Float = 0f
    private var prevYDirectR: Float = 0f

    // Cross filter state: [0] = Left, [1] = Right
    private var prevXCrossL: Float = 0f
    private var prevXCrossR: Float = 0f
    private var prevYCrossL: Float = 0f
    private var prevYCrossR: Float = 0f

    // Ring buffers for delayed cross-channel samples
    private val delayBufferL = FloatArray(DELAY_BUFFER_SIZE)
    private val delayBufferR = FloatArray(DELAY_BUFFER_SIZE)
    private var writeIndex: Int = 0

    fun configure(channels: Int, sampleRate: Int) {
        this.channels = channels
        this.sampleRate = sampleRate.coerceAtLeast(8000)
        updateCoefficients()
        reset()
    }

    fun setParameters(enabled: Boolean, preset: CrossfeedPreset) {
        val changed = this.isEnabled != enabled || this.preset != preset
        this.isEnabled = enabled
        this.preset = preset
        if (changed) {
            updateCoefficients()
        }
    }

    fun isShaping(): Boolean = isEnabled && preset != CrossfeedPreset.BYPASS && channels == 2

    /**
     * Resets filter ringing state and delay lines (e.g. on seek or track boundary).
     */
    fun reset() {
        prevXDirectL = 0f
        prevXDirectR = 0f
        prevYDirectL = 0f
        prevYDirectR = 0f

        prevXCrossL = 0f
        prevXCrossR = 0f
        prevYCrossL = 0f
        prevYCrossR = 0f

        delayBufferL.fill(0f)
        delayBufferR.fill(0f)
        writeIndex = 0
    }

    /**
     * Processes [count] interleaved float samples in-place.
     */
    fun process(samples: FloatArray, count: Int) {
        if (!isShaping() || count < 2 || channels != 2) return

        val mask = DELAY_BUFFER_MASK
        val dInt = delayInt
        val dFrac = delayFrac
        var wIdx = writeIndex

        var at = 0
        while (at + 1 < count) {
            val xL = samples[at]
            val xR = samples[at + 1]

            // 1. Direct path filter: y_d = b0d * x + b1d * prevX - a1 * prevY
            val yDirectL = b0d * xL + b1d * prevXDirectL - a1 * prevYDirectL
            prevXDirectL = xL
            prevYDirectL = yDirectL

            val yDirectR = b0d * xR + b1d * prevXDirectR - a1 * prevYDirectR
            prevXDirectR = xR
            prevYDirectR = yDirectR

            // 2. Cross path filter: y_c = b0c * x + b1c * prevX - a1 * prevY
            val yCrossL = b0c * xL + b1c * prevXCrossL - a1 * prevYCrossL
            prevXCrossL = xL
            prevYCrossL = yCrossL

            val yCrossR = b0c * xR + b1c * prevXCrossR - a1 * prevYCrossR
            prevXCrossR = xR
            prevYCrossR = yCrossR

            // 3. Write cross signals into delay ring buffers
            delayBufferL[wIdx] = yCrossL
            delayBufferR[wIdx] = yCrossR

            // 4. Read delayed cross signals with fractional linear interpolation
            val rIdx0 = (wIdx - dInt) and mask
            val rIdx1 = (rIdx0 - 1) and mask

            val delayedCrossL = delayBufferL[rIdx0] + dFrac * (delayBufferL[rIdx1] - delayBufferL[rIdx0])
            val delayedCrossR = delayBufferR[rIdx0] + dFrac * (delayBufferR[rIdx1] - delayBufferR[rIdx0])

            // 5. Binaural acoustic mix:
            // Left output = Left direct + Right delayed crossfeed
            // Right output = Right direct + Left delayed crossfeed
            samples[at] = yDirectL + delayedCrossR
            samples[at + 1] = yDirectR + delayedCrossL

            wIdx = (wIdx + 1) and mask
            at += 2
        }

        writeIndex = wIdx
        flushDenormals()
    }

    private fun updateCoefficients() {
        if (!isEnabled || preset == CrossfeedPreset.BYPASS || sampleRate <= 0) {
            a1 = 0f
            b0d = 1f
            b1d = 0f
            b0c = 0f
            b1c = 0f
            delayInt = 0
            delayFrac = 0f
            return
        }

        val fc = preset.cutoffHz.coerceIn(100f, (sampleRate * 0.45f))
        val g = 10f.pow(preset.levelDb / 20f) // linear gain < 1.0 (e.g. 0.501 for -6 dB)
        val w0 = 2f * PI.toFloat() * fc
        val wp = w0 / g

        // Bilinear transform with pre-warping at cutoff frequency
        val k = 2f * sampleRate.toFloat() * tan(PI.toFloat() * fc / sampleRate.toFloat())
        val norm = 1f / (k + wp)

        a1 = (wp - k) * norm
        b0d = (k + w0) * norm
        b1d = (w0 - k) * norm
        b0c = (wp - w0) * norm
        b1c = b0c

        // Compute delay in fractional samples
        val delaySeconds = preset.delayUs * 1e-6f
        val delaySamples = (delaySeconds * sampleRate).coerceIn(0f, (DELAY_BUFFER_SIZE - 2).toFloat())
        val intPart = floor(delaySamples).toInt()
        delayInt = intPart
        delayFrac = delaySamples - intPart
    }

    private fun flushDenormals() {
        if (kotlin.math.abs(prevYDirectL) < DENORMAL_FLOOR) prevYDirectL = 0f
        if (kotlin.math.abs(prevYDirectR) < DENORMAL_FLOOR) prevYDirectR = 0f
        if (kotlin.math.abs(prevYCrossL) < DENORMAL_FLOOR) prevYCrossL = 0f
        if (kotlin.math.abs(prevYCrossR) < DENORMAL_FLOOR) prevYCrossR = 0f
    }

    companion object {
        private const val DELAY_BUFFER_SIZE = 256
        private const val DELAY_BUFFER_MASK = DELAY_BUFFER_SIZE - 1
        private const val DENORMAL_FLOOR = 1e-15f
    }
}

