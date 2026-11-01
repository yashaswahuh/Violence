package com.music.bitchord.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Lets this phone look like an NFC tag holding one link, for as long as the Tap to share screen
 * is showing (see [NfcShareSession]).
 *
 * Declared **disabled** in the manifest and switched on only by [NfcShareSession.start], so the
 * Type 4 Tag AID is not registered with the system at any other time. If the app is killed while
 * the component is still enabled it simply answers "not found": with no active share there is
 * nothing to reveal, and no other data of the app can be reached through here.
 *
 * Nothing in this class logs.
 */
class ShareHostApduService : HostApduService() {

    private val tag = Type4TagEmulator { NfcShareSession.currentMessage() }

    // Runs on the main thread (HCE docs): the work is a few array copies.
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
        tag.process(commandApdu ?: ByteArray(0))

    override fun onDeactivated(reason: Int) {
        tag.reset()
    }
}
