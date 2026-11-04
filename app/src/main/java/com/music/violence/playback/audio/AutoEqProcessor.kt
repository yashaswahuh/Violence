package com.music.violence.playback.audio

import com.music.violence.playback.autoeq.AutoEqEngine
import com.music.violence.playback.autoeq.AutoEqProfile

/**
 * Float32 AudioProcessor stage integrating [AutoEqEngine] into Violence's [DspChain].
 *
 * Implements high-precision, in-place parametric equalization for headphone calibration
 * with zero-cost bypass when disabled or flat.
 */
class AutoEqProcessor(
    val engine: AutoEqEngine = AutoEqEngine(),
) : FloatAudioProcessor {

    override fun configure(sampleRate: Int, channelCount: Int) {
        engine.configure(channelCount, sampleRate)
    }

    override fun process(block: AudioBlock) {
        if (block.frameCount == 0 || !engine.isShaping()) return
        engine.process(block.samples, block.sampleCount)
    }

    override fun flush() {
        engine.reset()
    }

    override fun reset() {
        engine.reset()
    }

    fun setProfile(enabled: Boolean, profile: AutoEqProfile?) {
        engine.setProfile(enabled, profile)
    }

    fun isShaping(): Boolean = engine.isShaping()
}
