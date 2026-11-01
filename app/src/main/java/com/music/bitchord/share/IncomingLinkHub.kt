package com.music.bitchord.share

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import com.music.bitchord.data.listentogether.JamInviteLink
import com.music.bitchord.data.listentogether.ParsedJamInvite
import com.music.bitchord.data.share.IncomingLink
import com.music.bitchord.data.share.IncomingLinkGate
import com.music.bitchord.data.share.IncomingLinks
import com.music.bitchord.data.share.InvalidReason
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a link came from. */
enum class LinkSource { INTENT, NFC, QR, PASTE }

/** Something the user should be told about an incoming link that was refused. Never carries the link. */
enum class LinkNotice { NOT_A_VIOLENCE_LINK, UNSUPPORTED_VERSION, MALFORMED, TOO_LONG, EMPTY, UNREADABLE }

/**
 * The one door every incoming link goes through: an NFC tap (system dispatch or the Receive
 * screen), a QR scan, a pasted link, a deep link or an App Link.
 *
 * Everything is untrusted and **nothing acts on its own**: a song or a profile becomes
 * [pending] and the UI asks the user; a Jam invite is handed to the existing join flow, which
 * previews the party and asks before joining. Repeats and floods are dropped by
 * [IncomingLinkGate]. Anything invalid becomes a [LinkNotice] (a category, never the text).
 *
 * Nothing here logs and nothing here uses the network.
 */
object IncomingLinkHub {

    private const val EXTRA_CONSUMED = "violence.incomingLinkConsumed"

    private val gate = IncomingLinkGate()

    /** The clock, replaceable by a test. */
    @Volatile
    internal var clock: () -> Long = System::currentTimeMillis

    private val _pending = MutableStateFlow<IncomingLink?>(null)

    /** A song or a profile waiting for the user's decision. */
    val pending: StateFlow<IncomingLink?> = _pending.asStateFlow()

    private val _notices = MutableSharedFlow<LinkNotice>(extraBufferCapacity = 4)
    val notices: SharedFlow<LinkNotice> = _notices.asSharedFlow()

    /** Called by the Receive screen when a tag was found but held no link. */
    fun unreadable() {
        _notices.tryEmit(LinkNotice.UNREADABLE)
    }

    /**
     * Offers [text] as an incoming link. [extract] looks for a link inside a pasted message.
     * Returns true if it was accepted for the user to confirm.
     */
    fun offer(text: String?, source: LinkSource, extract: Boolean = false): Boolean {
        val bases = listOfNotNull(JamInviteLink.inviteBaseUrl.takeIf { it.isNotBlank() })
        val link = if (extract) IncomingLinks.extract(text, bases) else IncomingLinks.parse(text, bases)
        return accept(link, source)
    }

    internal fun accept(link: IncomingLink, source: LinkSource): Boolean {
        if (link is IncomingLink.Invalid) {
            // A link tapped in another app that is not ours (a YouTube link, say) is not an error.
            if (!(source == LinkSource.INTENT && link.reason == InvalidReason.NOT_A_VIOLENCE_LINK)) {
                _notices.tryEmit(link.reason.toNotice())
            }
            return false
        }
        val key = link.key ?: return false
        val admitted = synchronized(gate) { gate.admit(key, clock()) }
        if (!admitted) return false
        when (link) {
            is IncomingLink.Party -> JamInviteLink.offer(ParsedJamInvite(link.invite.code, link.invite.serverUrl))
            is IncomingLink.Song, is IncomingLink.Profile -> _pending.value = link
            is IncomingLink.Invalid -> return false
        }
        return true
    }

    /** Reads a link from a cold launch or a new intent (a tapped link, an App Link, an NFC tag). */
    fun consume(intent: Intent?): Boolean {
        if (intent == null) return false
        val action = intent.action
        if (action != Intent.ACTION_VIEW && action != NfcAdapter.ACTION_NDEF_DISCOVERED) return false
        if (intent.getBooleanExtra(EXTRA_CONSUMED, false)) return false
        val text = intent.dataString ?: ndefLink(intent)
        intent.putExtra(EXTRA_CONSUMED, true)
        val source = if (action == NfcAdapter.ACTION_NDEF_DISCOVERED) LinkSource.NFC else LinkSource.INTENT
        return offer(text, source)
    }

    /** The user decided: forget the pending item (it stays remembered so a re-read is ignored). */
    fun handled() {
        _pending.value = null
    }

    /** The user said no: forget it, and let them offer the same link again straight away. */
    fun dismiss() {
        _pending.value?.key?.let { key -> synchronized(gate) { gate.forget(key) } }
        _pending.value = null
    }

    @Suppress("DEPRECATION")
    private fun ndefLink(intent: Intent): String? = runCatching {
        val raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return null
        val message = raw.firstOrNull() as? NdefMessage ?: return null
        if (message.records.size != 1) return null
        message.records[0].toUri()?.toString()
    }.getOrNull()

    private fun InvalidReason.toNotice() = when (this) {
        InvalidReason.EMPTY -> LinkNotice.EMPTY
        InvalidReason.TOO_LONG -> LinkNotice.TOO_LONG
        InvalidReason.NOT_A_VIOLENCE_LINK -> LinkNotice.NOT_A_VIOLENCE_LINK
        InvalidReason.UNSUPPORTED_VERSION -> LinkNotice.UNSUPPORTED_VERSION
        InvalidReason.MALFORMED -> LinkNotice.MALFORMED
    }
}
