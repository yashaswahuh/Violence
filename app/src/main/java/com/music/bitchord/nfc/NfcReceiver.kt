package com.music.bitchord.nfc

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle

/** Whether this phone can read by tapping. */
enum class NfcReceiveState { READY, NO_NFC, NFC_OFF }

/**
 * The receiving half of tap-to-share: reader mode.
 *
 * While the Receive screen is resumed the phone polls for tags itself (`enableReaderMode`), reads
 * the Type 4 tag the other phone is emulating (or a plain NFC tag holding a link), and hands the
 * link text to [onLink]. This does not depend on the system's own tag dispatch, which is the less
 * reliable path across phones. It is switched off again when the screen is paused.
 *
 * Whatever is read is untrusted text: it is not interpreted here. [onLink] gets it as is.
 * Nothing in this class logs.
 */
object NfcReceiver {

    fun state(context: Context): NfcReceiveState {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) return NfcReceiveState.NO_NFC
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcReceiveState.NO_NFC
        return if (adapter.isEnabled) NfcReceiveState.READY else NfcReceiveState.NFC_OFF
    }

    /**
     * Starts reader mode. [onLink] is called on a background thread with the link text of every
     * tag that holds one; [onUnreadable] when a tag was found but could not be read as a link.
     */
    fun enable(activity: Activity, onLink: (String) -> Unit, onUnreadable: () -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) }
        runCatching {
            adapter.enableReaderMode(
                activity,
                { tag -> readTag(tag, onLink, onUnreadable) },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                extras,
            )
        }
    }

    fun disable(activity: Activity) {
        runCatching { NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity) }
    }

    private fun readTag(tag: Tag, onLink: (String) -> Unit, onUnreadable: () -> Unit) {
        val fromType4 = runCatching { readIsoDep(tag) }.getOrNull()
        val link = fromType4 ?: runCatching { readNdef(tag) }.getOrNull()
        if (link != null) onLink(link) else onUnreadable()
    }

    /** The phone-to-phone path: a Type 4 tag over ISO-DEP. */
    private fun readIsoDep(tag: Tag): String? {
        val iso = IsoDep.get(tag) ?: return null
        try {
            iso.connect()
            iso.timeout = 3000
            val result = Type4TagReader.read { iso.transceive(it) }
            return (result as? Type4TagReader.Result.Ok)?.let { NdefUri.parse(it.message) }
        } finally {
            runCatching { iso.close() }
        }
    }

    /** A plain NFC tag that already holds an NDEF message. */
    private fun readNdef(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        try {
            ndef.connect()
            val message = ndef.ndefMessage ?: return null
            if (message.byteArrayLength > Type4Tag.READER_MAX_MESSAGE) return null
            return NdefUri.parse(message.toByteArray())
        } finally {
            runCatching { ndef.close() }
        }
    }
}
