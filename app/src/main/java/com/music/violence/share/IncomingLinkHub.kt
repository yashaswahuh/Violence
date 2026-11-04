package com.music.violence.share

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import com.music.violence.data.listentogether.JamInviteLink
import com.music.violence.data.listentogether.ParsedJamInvite
import com.music.violence.data.profile.ProfileManager
import com.music.violence.data.share.IncomingLink
import com.music.violence.data.share.IncomingLinkGate
import com.music.violence.data.share.IncomingLinks
import com.music.violence.data.share.InvalidReason
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a link came from. */
enum class LinkSource { INTENT, NFC, QR, PASTE }

/** Something the user should be told about an incoming link that was refused. Never carries the link. */
enum class LinkNotice { NOT_A_VIOLENCE_LINK, UNSUPPORTED_VERSION, MALFORMED, TOO_LONG, EMPTY, UNREADABLE, NFC_NOT_SHARING, NFC_INTERRUPTED }

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

    private val _lastRead = MutableStateFlow("")

    /**
     * What the last tap, scan or paste came to, in a line for the Receive page's "last read" note:
     * where it came from and what the reader made of it, never the link itself. It is how a failed
     * tap can be reported precisely.
     */
    val lastRead: StateFlow<String> = _lastRead.asStateFlow()

    private fun note(source: String, what: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(clock()))
        _lastRead.value = "$time \u00B7 $source \u00B7 $what"
    }

    private val _notices = MutableSharedFlow<LinkNotice>(extraBufferCapacity = 4)
    val notices: SharedFlow<LinkNotice> = _notices.asSharedFlow()

    @Volatile
    private var lastTapProblemAt = 0L

    /**
     * Called by the Receive screen when a tap was not read. While two phones touch, the reader can
     * fail on every presence check, so the same kind of message is shown at most once every few seconds.
     */
    fun tapFailed(reason: com.music.violence.nfc.NfcReadFailure) {
        note("NFC", "not read (${reason.name})")
        val now = clock()
        if (now - lastTapProblemAt < 3_000) return
        lastTapProblemAt = now
        _notices.tryEmit(
            when (reason) {
                com.music.violence.nfc.NfcReadFailure.NOT_SHARING -> LinkNotice.NFC_NOT_SHARING
                com.music.violence.nfc.NfcReadFailure.INTERRUPTED -> LinkNotice.NFC_INTERRUPTED
                com.music.violence.nfc.NfcReadFailure.UNREADABLE -> LinkNotice.UNREADABLE
            },
        )
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
        note(source.name, if (link is IncomingLink.Invalid) "refused (${link.reason.name})" else "read ${link::class.simpleName}")
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
        RecentShares.received(link)
        when (link) {
            is IncomingLink.Party -> {
                val invite = link.invite
                val sender = invite.from
                // A convenience, never a trust decision: the sender is only a claim in the link, so it
                // skips the preview only for a physical tap, only from somebody already on the list,
                // and only if the user switched it on.
                val auto = source == LinkSource.NFC && sender != null &&
                    ProfileManager.autoJoinFriends.value &&
                    sender.id != ProfileManager.current()?.id &&
                    ProfileManager.isFriend(sender.id)
                JamInviteLink.offer(
                    ParsedJamInvite(
                        code = invite.code,
                        serverUrl = invite.serverUrl,
                        autoJoin = auto,
                        fromName = sender?.let { ProfileManager.friendName(it.id) ?: it.displayName },
                    ),
                )
            }
            is IncomingLink.Song, is IncomingLink.Playlist, is IncomingLink.Profile -> _pending.value = link
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

    /** Opens a remembered link again (from the Recent page). Asks first, like any incoming link. */
    fun reopen(uri: String) {
        val link = IncomingLinks.parse(uri)
        if (link is IncomingLink.Song || link is IncomingLink.Playlist || link is IncomingLink.Profile) {
            _pending.value = link
        }
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
