package com.music.violence.ui.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.music.violence.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lifecycle and screen state hook for suspending heavy background visuals.
 *
 * When the "Pause Background Visuals When Hidden / Screen Off" experimental setting is enabled,
 * halts Skia/Compose animation clocks, Canvas video decoders, and background shaders whenever
 * the device screen turns off or the main activity receives `ON_STOP`.
 *
 * Playback audio continues seamlessly in [com.music.violence.playback.PlaybackService]
 * without interruption while eliminating all background GPU/CPU draw cycles.
 */
object BackgroundVisualsController {

    private val _isSuspended = MutableStateFlow(false)
    val isSuspended: StateFlow<Boolean> = _isSuspended.asStateFlow()

    @Volatile
    private var isScreenOn = true

    @Volatile
    private var isActivityStarted = true

    private var isRegistered = false

    fun init(context: Context) {
        if (isRegistered) return
        isRegistered = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        evaluate()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        evaluate()
                    }
                }
            }
        }

        context.registerReceiver(receiver, filter)
        evaluate()
    }

    fun onActivityStarted() {
        isActivityStarted = true
        evaluate()
    }

    fun onActivityStopped() {
        isActivityStarted = false
        evaluate()
    }

    fun updateScreenState(screenOn: Boolean) {
        isScreenOn = screenOn
        evaluate()
    }

    fun evaluate() {
        val enabled = AppSettings.pauseBackgroundVisualsWhenHidden.value
        val shouldSuspend = enabled && (!isScreenOn || !isActivityStarted)
        if (_isSuspended.value != shouldSuspend) {
            _isSuspended.value = shouldSuspend
        }
    }
}

