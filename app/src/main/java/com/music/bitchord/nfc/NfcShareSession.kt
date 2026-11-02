package com.music.bitchord.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether this phone can send by tapping, and if not why not. */
enum class NfcSendState {
    /** Ready to share. */
    READY,

    /** The phone has no NFC chip. */
    NO_NFC,

    /** NFC is turned off in the system settings. */
    NFC_OFF,

    /** The chip cannot emulate a tag (no host card emulation). */
    NO_HCE,
}

/**
 * The sending half of tap-to-share: holds the one link that is being offered and turns the HCE
 * service on and off around it.
 *
 * `start` is called when the Tap to share screen is resumed and `stop` when it is paused or
 * closed, so the phone is a "tag" only while that screen is in the foreground (and, because the
 * service asks for `requireDeviceUnlock`, only while unlocked).
 */
object NfcShareSession {

    @Volatile
    private var message: ByteArray? = null

    private val main = Handler(Looper.getMainLooper())

    private val _delivered = MutableStateFlow(0)

    /**
     * Counts completed reads: it goes up each time the other phone has read the whole link. The
     * Tap to share screen watches it to show that the link was sent.
     */
    val delivered: StateFlow<Int> = _delivered.asStateFlow()

    /** The NDEF message being offered right now, or null when nothing is being shared. */
    fun currentMessage(): ByteArray? = message

    /** Called by the tag service when a reader has read the whole message. */
    internal fun notifyDelivered() {
        _delivered.value = _delivered.value + 1
    }

    fun sendState(context: Context): NfcSendState {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) return NfcSendState.NO_NFC
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcSendState.NO_NFC
        if (!adapter.isEnabled) return NfcSendState.NFC_OFF
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return NfcSendState.NO_HCE
        return NfcSendState.READY
    }

    /** Whether [uri] fits on the air (at most [NdefUri.MAX_URI_BYTES] bytes). */
    fun fits(uri: String): Boolean = NdefUri.message(uri) != null

    /**
     * Starts offering [uri]. Returns false (and offers nothing) if the phone cannot, NFC is off,
     * or the link is too long. Safe to call again with another link: it replaces the first.
     */
    fun start(activity: Activity, uri: String): Boolean {
        if (sendState(activity) != NfcSendState.READY) return false
        val ndef = NdefUri.message(uri) ?: return false
        message = ndef
        setServiceEnabled(activity, true)
        preferThisService(activity)
        // The system takes a moment to notice a component that has just been enabled, and asking
        // for it as the preferred service can fail until it has: ask again shortly after.
        main.removeCallbacksAndMessages(null)
        for (delayMs in longArrayOf(400, 1500)) {
            main.postDelayed({ if (message != null) preferThisService(activity) }, delayMs)
        }
        return true
    }

    /** Stops offering anything. Idempotent; never throws. */
    fun stop(activity: Activity) {
        main.removeCallbacksAndMessages(null)
        message = null
        runCatching {
            val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return@runCatching
            CardEmulation.getInstance(adapter).unsetPreferredService(activity)
        }
        setServiceEnabled(activity, false)
    }

    /** Makes sure the service is off, for example at app start after an unclean exit. */
    fun ensureOff(context: Context) {
        message = null
        setServiceEnabled(context, false)
    }

    private fun preferThisService(activity: Activity) {
        runCatching {
            val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return@runCatching
            CardEmulation.getInstance(adapter).setPreferredService(activity, serviceName(activity))
        }
    }

    private fun serviceName(context: Context) = ComponentName(context, ShareHostApduService::class.java)

    private fun setServiceEnabled(context: Context, enabled: Boolean) {
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                serviceName(context),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
