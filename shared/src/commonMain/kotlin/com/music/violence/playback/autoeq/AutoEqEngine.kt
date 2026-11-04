package com.music.violence.playback.autoeq

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * High-precision parametric equalizer DSP engine for AutoEQ headphone calibration.
 *
 * Utilizes Cytomic's trapezoidal State-Variable Filters (SVF) with frequency pre-warping.
 * Supports up to 16 arbitrary cascaded biquad sections (Peaking, Low Shelf, High Shelf),
 * dynamic pre-amp gain attenuation, coefficient smoothing/gliding, and zero-cost self-bypass.
 */
class AutoEqEngine(
    private val maxFilters: Int = MAX_FILTERS,
) {

    private class Tuning(
        val count: Int,
        val kinds: IntArray, // 0: Peaking, 1: Low Shelf, 2: High Shelf
        val frequencies: FloatArray,
        val gainsDb: FloatArray,
        val qs: FloatArray,
        val preampDb: Float,
    ) {
        companion object {
            val BYPASS = Tuning(
                count = 0,
                kinds = IntArray(MAX_FILTERS),
                frequencies = FloatArray(MAX_FILTERS) { 1000f },
                gainsDb = FloatArray(MAX_FILTERS) { 0f },
                qs = FloatArray(MAX_FILTERS) { 0.707f },
                preampDb = 0f,
            )
        }
    }

    @Volatile
    private var target: Tuning = Tuning.BYPASS

    @Volatile
    var isEnabled: Boolean = false
        private set

    private var channels = 0
    private var sampleRate = 0

    private val currentKinds = IntArray(maxFilters)
    private val currentFrequencies = FloatArray(maxFilters) { 1000f }
    private val currentGainDb = FloatArray(maxFilters) { 0f }
    private val currentQ = FloatArray(maxFilters) { 0.707f }
    private var currentPreampDb = 0f
    private var currentCount = 0

    private val coeffA1 = FloatArray(maxFilters)
    private val coeffA2 = FloatArray(maxFilters)
    private val coeffA3 = FloatArray(maxFilters)
    private val mixInput = FloatArray(maxFilters)
    private val mixBand = FloatArray(maxFilters)
    private val mixLow = FloatArray(maxFilters)

    private val activeSlots = IntArray(maxFilters)
    private val running = BooleanArray(maxFilters)

    /** Two integrator states per section, per channel. */
    private var state = FloatArray(0)
    private var channelGain = 1f

    /**
     * Updates the calibration tuning. Safe to call from any thread.
     */
    fun setProfile(enabled: Boolean, profile: AutoEqProfile?) {
        isEnabled = enabled
        if (!enabled || profile == null || profile.filters.isEmpty()) {
            target = Tuning.BYPASS
            return
        }

        val filters = profile.filters.take(maxFilters)
        val count = filters.size
        val kinds = IntArray(maxFilters)
        val frequencies = FloatArray(maxFilters) { 1000f }
        val gains = FloatArray(maxFilters) { 0f }
        val qs = FloatArray(maxFilters) { 0.707f }

        for (i in 0 until count) {
            val f = filters[i]
            kinds[i] = when (f.type) {
                AutoEqFilterType.LOW_SHELF -> KIND_LOW_SHELF
                AutoEqFilterType.HIGH_SHELF -> KIND_HIGH_SHELF
                AutoEqFilterType.PEAKING -> KIND_PEAKING
            }
            frequencies[i] = f.frequencyHz
            gains[i] = f.gainDb
            qs[i] = f.q
        }

        target = Tuning(
            count = count,
            kinds = kinds,
            frequencies = frequencies,
            gainsDb = gains,
            qs = qs,
            preampDb = profile.preampDb,
        )
    }

    /**
     * Configures the channel count and sample rate.
     */
    fun configure(channels: Int, sampleRate: Int) {
        if (channels == this.channels && sampleRate == this.sampleRate) return
        this.channels = channels.coerceAtLeast(1)
        this.sampleRate = sampleRate.coerceAtLeast(8000)
        state = FloatArray(this.channels * maxFilters * 2)
        running.fill(false)
        snapToTarget()
    }

    /**
     * Resets filter ringing states on seek or track boundary.
     */
    fun reset() {
        state.fill(0f)
        running.fill(false)
        snapToTarget()
    }

    /**
     * Returns true if the processor is active and actively shaping the signal.
     */
    fun isShaping(): Boolean {
        val tuning = target
        return isEnabled && (tuning.count > 0 || !isSettled(tuning))
    }

    /**
     * Processes [count] interleaved samples in-place.
     */
    fun process(block: FloatArray, count: Int) {
        if (count == 0 || channels == 0 || sampleRate == 0) return
        val tuning = target

        // If bypass is targeted and filters have fully glided to flat, return immediately.
        if (tuning.count == 0 && isSettled(tuning)) {
            return
        }

        var index = 0
        while (index < count) {
            val remaining = count - index
            val subBlock = min(remaining, GLIDE_FRAMES * channels)
            glideTowards(tuning)
            val active = prepareSections()
            channelGain = 10f.pow(currentPreampDb / 20f)

            var at = index
            val end = index + subBlock
            while (at < end) {
                for (channel in 0 until channels) {
                    if (at >= end) break
                    var sample = block[at]
                    for (s in 0 until active) {
                        sample = section(activeSlots[s], channel, sample)
                    }
                    sample *= channelGain
                    block[at] = sample
                    at++
                }
            }
            flushDenormals(active)
            index = end
        }
    }

    private fun snapToTarget() {
        val tuning = target
        currentCount = tuning.count
        tuning.kinds.copyInto(currentKinds)
        tuning.frequencies.copyInto(currentFrequencies)
        tuning.gainsDb.copyInto(currentGainDb)
        tuning.qs.copyInto(currentQ)
        currentPreampDb = tuning.preampDb
    }

    private fun glideTowards(tuning: Tuning) {
        currentCount = tuning.count
        for (slot in 0 until maxFilters) {
            currentKinds[slot] = tuning.kinds[slot]
            currentFrequencies[slot] = tuning.frequencies[slot]
            currentGainDb[slot] = linearGlide(currentGainDb[slot], tuning.gainsDb[slot])
            currentQ[slot] = geometricGlide(currentQ[slot], tuning.qs[slot])
        }
        currentPreampDb = linearGlide(currentPreampDb, tuning.preampDb)
    }

    private fun linearGlide(current: Float, target: Float): Float =
        current + (target - current) * GLIDE_RATE

    private fun geometricGlide(current: Float, target: Float): Float {
        val from = ln(current.coerceAtLeast(MIN_Q))
        val to = ln(target.coerceAtLeast(MIN_Q))
        return exp(from + (to - from) * GLIDE_RATE)
    }

    private fun isSettled(tuning: Tuning): Boolean {
        if (abs(currentPreampDb - tuning.preampDb) >= SETTLED_DB) return false
        for (slot in 0 until maxFilters) {
            if (abs(currentGainDb[slot] - tuning.gainsDb[slot]) >= SETTLED_DB) return false
        }
        return true
    }

    private fun prepareSections(): Int {
        var active = 0
        for (slot in 0 until currentCount) {
            if (abs(currentGainDb[slot]) >= SETTLED_DB) {
                updateCoefficients(slot)
                activeSlots[active++] = slot
                running[slot] = true
            } else if (running[slot]) {
                clearState(slot)
                running[slot] = false
            }
        }
        return active
    }

    private fun updateCoefficients(slot: Int) {
        val a = 10f.pow(currentGainDb[slot] / 40f)
        val q = currentQ[slot].coerceAtLeast(MIN_Q)
        val fc = currentFrequencies[slot].coerceIn(MIN_HZ, sampleRate * MAX_FREQUENCY_FRACTION)
        val base = tan(PI * fc / sampleRate).toFloat()
        val g: Float
        val k: Float

        when (currentKinds[slot]) {
            KIND_PEAKING -> {
                g = base
                k = 1f / (q * a)
                mixInput[slot] = 1f
                mixBand[slot] = k * (a * a - 1f)
                mixLow[slot] = 0f
            }
            KIND_LOW_SHELF -> {
                g = base / sqrt(a)
                k = 1f / q
                mixInput[slot] = 1f
                mixBand[slot] = k * (a - 1f)
                mixLow[slot] = a * a - 1f
            }
            KIND_HIGH_SHELF -> {
                g = base * sqrt(a)
                k = 1f / q
                mixInput[slot] = a * a
                mixBand[slot] = k * (1f - a) * a
                mixLow[slot] = 1f - a * a
            }
            else -> {
                g = base
                k = 1f / (q * a)
                mixInput[slot] = 1f
                mixBand[slot] = 0f
                mixLow[slot] = 0f
            }
        }

        val d = 1f / (1f + g * (g + k))
        coeffA1[slot] = d
        coeffA2[slot] = g * d
        coeffA3[slot] = g * (g * d)
    }

    private fun section(slot: Int, channel: Int, input: Float): Float {
        val i = (channel * maxFilters + slot) * 2
        val ic1 = state[i]
        val ic2 = state[i + 1]
        val v3 = input - ic2
        val v1 = coeffA1[slot] * ic1 + coeffA2[slot] * v3
        val v2 = ic2 + coeffA2[slot] * ic1 + coeffA3[slot] * v3
        state[i] = 2f * v1 - ic1
        state[i + 1] = 2f * v2 - ic2
        return mixInput[slot] * input + mixBand[slot] * v1 + mixLow[slot] * v2
    }

    private fun clearState(slot: Int) {
        for (channel in 0 until channels) {
            val i = (channel * maxFilters + slot) * 2
            state[i] = 0f
            state[i + 1] = 0f
        }
    }

    private fun flushDenormals(active: Int) {
        for (index in 0 until active) {
            val slot = activeSlots[index]
            for (channel in 0 until channels) {
                val i = (channel * maxFilters + slot) * 2
                if (abs(state[i]) < DENORMAL_FLOOR) state[i] = 0f
                if (abs(state[i + 1]) < DENORMAL_FLOOR) state[i + 1] = 0f
            }
        }
    }

    companion object {
        const val MAX_FILTERS = 16
        const val KIND_PEAKING = 0
        const val KIND_LOW_SHELF = 1
        const val KIND_HIGH_SHELF = 2

        private const val MIN_HZ = 10f
        private const val MIN_Q = 0.1f
        private const val MAX_FREQUENCY_FRACTION = 0.45f
        private const val GLIDE_FRAMES = 64
        private const val GLIDE_RATE = 0.08f
        private const val SETTLED_DB = 0.01f
        private const val DENORMAL_FLOOR = 1e-15f
    }
}

