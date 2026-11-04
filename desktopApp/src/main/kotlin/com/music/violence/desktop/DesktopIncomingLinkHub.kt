package com.music.violence.desktop

import com.music.violence.data.listentogether.JamInvite
import com.music.violence.data.share.IncomingLink
import com.music.violence.data.share.IncomingLinkGate
import com.music.violence.data.share.IncomingLinks
import com.music.violence.data.share.InvalidReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DesktopLinkSource { ARGUMENT, CLIPBOARD, PASTE }

/**
 * Inbound link hub on Desktop.
 * Validates and gates incoming `violence://` and landing page URLs.
 */
internal object DesktopIncomingLinkHub {

    private val gate = IncomingLinkGate()

    private val _pending = MutableStateFlow<IncomingLink?>(null)
    val pending: StateFlow<IncomingLink?> = _pending.asStateFlow()

    private val _lastNotice = MutableStateFlow<String?>(null)
    val lastNotice: StateFlow<String?> = _lastNotice.asStateFlow()

    fun offer(text: String?, source: DesktopLinkSource = DesktopLinkSource.PASTE, extract: Boolean = true): Boolean {
        val input = text?.trim().orEmpty()
        if (input.isBlank()) return false

        val link = if (extract) IncomingLinks.extract(input) else IncomingLinks.parse(input)
        return accept(link, source)
    }

    private fun accept(link: IncomingLink, source: DesktopLinkSource): Boolean {
        if (link is IncomingLink.Invalid) {
            val message = when (link.reason) {
                InvalidReason.EMPTY -> "The link was empty."
                InvalidReason.TOO_LONG -> "The link was too long."
                InvalidReason.NOT_A_VIOLENCE_LINK -> "That is not a Violence link."
                InvalidReason.UNSUPPORTED_VERSION -> "This link was made by a newer version of Violence."
                InvalidReason.MALFORMED -> "That link could not be read."
            }
            _lastNotice.value = message
            return false
        }

        val key = link.key
        if (key != null && !gate.admit(key, System.currentTimeMillis())) {
            return false
        }

        _pending.value = link
        _lastNotice.value = null
        return true
    }

    fun dismiss() {
        _pending.value = null
    }

    fun handled() {
        _pending.value = null
    }

    fun clearNotice() {
        _lastNotice.value = null
    }
}
