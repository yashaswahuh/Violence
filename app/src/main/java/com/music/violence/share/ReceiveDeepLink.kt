package com.music.violence.share

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Open the Receive page": set by the Quick Settings tile ([ReceiveTileService]). Same shape as
 * the player deep link: whoever acts on the request clears it with [handled].
 */
object ReceiveDeepLink {

    const val ACTION = "com.music.violence.action.RECEIVE"

    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    fun consume(intent: Intent?): Boolean {
        if (intent?.action != ACTION) return false
        intent.action = null
        _pending.value = true
        return true
    }

    fun handled() {
        _pending.value = false
    }
}
