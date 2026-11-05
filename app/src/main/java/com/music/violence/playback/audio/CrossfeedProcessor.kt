package com.music.violence.playback.audio

import com.music.violence.playback.crossfeed.CrossfeedEngine
import com.music.violence.playback.crossfeed.CrossfeedPreset

/**
 * Float32 AudioProcessor stage integrating [CrossfeedEngine] into Violence's [DspChain].
 *
 * Implements high-fidelity Bauer stereophonic-to-binaural (bs2b) crossfeed for headphone listening,
 * with zero-cost self-bypass when disabled or bypassed.
 */
class CrossfeedProcessor(
    val engine: CrossfeedEngine = CrossfeedEngine(),
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

    fun setParameters(enabled: Boolean, preset: CrossfeedPreset) {
        engine.setParameters(enabled, preset)
    }

    fun isShaping(): Boolean = engine.isShaping()
}

