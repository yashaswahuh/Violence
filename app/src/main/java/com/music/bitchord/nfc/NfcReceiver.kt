package com.music.bitchord.nfc

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import java.io.IOException

/** Whether this phone can read by tapping. */
enum class NfcReceiveState { READY, NO_NFC, NFC_OFF }

/** Why a tap was not read, so the message can say what to do about it. */
enum class NfcReadFailure {
    /** A tag was found, but it is not a phone that is on its Tap to share screen. */
    NOT_SHARING,

    /** The phones moved apart (or the link dropped) in the middle of the read. */
    INTERRUPTED,

    /** Something answered, but it was not a readable Violence link. */
    UNREADABLE,
}

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
     * tag that holds one; [onFailed] when a tag was found but could not be read as a link.
     */
    fun enable(activity: Activity, onLink: (String) -> Unit, onFailed: (NfcReadFailure) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return
        val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) }
        runCatching {
            adapter.enableReaderMode(
                activity,
                { tag -> readTag(tag, onLink, onFailed) },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                extras,
            )
        }
    }

    fun disable(activity: Activity) {
        runCatching { NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity) }
    }

    private sealed interface Attempt {
        data class Link(val text: String) : Attempt
        data class Failed(val reason: NfcReadFailure) : Attempt
    }

    private fun readTag(tag: Tag, onLink: (String) -> Unit, onFailed: (NfcReadFailure) -> Unit) {
        val phone = readIsoDep(tag)
        if (phone is Attempt.Link) return onLink(phone.text)
        // Not a phone sharing (a plain NFC tag that already holds a link, for example).
        val plain = readNdef(tag)
        if (plain is Attempt.Link) return onLink(plain.text)
        onFailed((phone as? Attempt.Failed)?.reason ?: (plain as? Attempt.Failed)?.reason ?: NfcReadFailure.UNREADABLE)
    }

    /** The phone-to-phone path: a Type 4 tag over ISO-DEP. Null when the tag is not ISO-DEP at all. */
    private fun readIsoDep(tag: Tag): Attempt? {
        val iso = IsoDep.get(tag) ?: return null
        return try {
            iso.connect()
            iso.timeout = 3000
            when (val result = Type4TagReader.read { iso.transceive(it) }) {
                is Type4TagReader.Result.Ok ->
                    NdefUri.parse(result.message)?.let { Attempt.Link(it) } ?: Attempt.Failed(NfcReadFailure.UNREADABLE)
                is Type4TagReader.Result.Failed -> Attempt.Failed(
                    when (result.reason) {
                        Type4TagReader.Failure.NOT_AN_NDEF_TAG -> NfcReadFailure.NOT_SHARING
                        Type4TagReader.Failure.TRUNCATED -> NfcReadFailure.INTERRUPTED
                        else -> NfcReadFailure.UNREADABLE
                    },
                )
            }
        } catch (_: IOException) {
            // TagLostException is an IOException: the phones moved apart mid-read.
            Attempt.Failed(NfcReadFailure.INTERRUPTED)
        } catch (_: RuntimeException) {
            Attempt.Failed(NfcReadFailure.UNREADABLE)
        } finally {
            runCatching { iso.close() }
        }
    }

    /** A plain NFC tag that already holds an NDEF message. Null when the tag has no NDEF at all. */
    private fun readNdef(tag: Tag): Attempt? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.ndefMessage ?: return Attempt.Failed(NfcReadFailure.UNREADABLE)
            if (message.byteArrayLength > Type4Tag.READER_MAX_MESSAGE) return Attempt.Failed(NfcReadFailure.UNREADABLE)
            NdefUri.parse(message.toByteArray())?.let { Attempt.Link(it) } ?: Attempt.Failed(NfcReadFailure.UNREADABLE)
        } catch (_: IOException) {
            Attempt.Failed(NfcReadFailure.INTERRUPTED)
        } catch (_: RuntimeException) {
            Attempt.Failed(NfcReadFailure.UNREADABLE)
        } finally {
            runCatching { ndef.close() }
        }
    }
}
