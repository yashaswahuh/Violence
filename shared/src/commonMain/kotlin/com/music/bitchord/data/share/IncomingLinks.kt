package com.music.bitchord.data.share

import com.music.bitchord.data.listentogether.JamInvite
import com.music.bitchord.data.profile.ProfileCard
import com.music.bitchord.data.profile.ProfileCardCodec

/** Why an incoming link was refused. A category only: the text of a bad link is never echoed. */
enum class InvalidReason { EMPTY, TOO_LONG, NOT_A_VIOLENCE_LINK, UNSUPPORTED_VERSION, MALFORMED }

/** What an incoming link turned out to be. Always a candidate: nothing here acts on its own. */
sealed interface IncomingLink {
    data class Song(val ref: SongRef) : IncomingLink
    data class Playlist(val ref: PlaylistRef) : IncomingLink
    data class Profile(val card: ProfileCard) : IncomingLink
    data class Party(val invite: PartyInvite) : IncomingLink
    data class Invalid(val reason: InvalidReason) : IncomingLink

    /** The same item whatever form it arrived in, for spotting a repeated read. Null for [Invalid]. */
    val key: String?
        get() = when (this) {
            is Song -> ref.key
            is Playlist -> ref.key
            is Profile -> "profile:${card.id}"
            is Party -> "party:${invite.code}"
            is Invalid -> null
        }
}

/**
 * The one place an incoming link is read, for NFC, a QR scan, a pasted link, a deep link and an
 * App Link alike. Everything is validated (scheme, host and path, version, lengths, character
 * sets, ids); plain `http` is never accepted; nothing is trusted.
 */
object IncomingLinks {

    private const val MAX_INPUT_CHARS = 512

    /**
     * Reads exactly one link. [extraPartyBases] are additional https bases that may carry a Jam
     * code (the build's `JAM_INVITE_BASE_URL`); songs and profiles are only taken from the
     * owner's site and the `violence://` scheme.
     */
    fun parse(input: String?, extraPartyBases: List<String> = emptyList()): IncomingLink {
        val text = input?.trim().orEmpty()
        if (text.isEmpty()) return IncomingLink.Invalid(InvalidReason.EMPTY)
        if (text.length > MAX_INPUT_CHARS) return IncomingLink.Invalid(InvalidReason.TOO_LONG)

        val lower = text.lowercase()
        return when {
            lower.startsWith(SongLinks.CUSTOM_PREFIX) || lower.startsWith(SongLinks.WEB_PREFIX.lowercase()) ->
                when (val r = SongLinks.parse(text)) {
                    is SongLinks.Result.Ok -> IncomingLink.Song(r.ref)
                    is SongLinks.Result.Rejected -> IncomingLink.Invalid(
                        if (r.error == SongLinks.Error.WRONG_VERSION) InvalidReason.UNSUPPORTED_VERSION else InvalidReason.MALFORMED,
                    )
                }

            lower.startsWith(PlaylistLinks.CUSTOM_PREFIX) || lower.startsWith(PlaylistLinks.WEB_PREFIX.lowercase()) ->
                when (val r = PlaylistLinks.parse(text)) {
                    is PlaylistLinks.Result.Ok -> IncomingLink.Playlist(r.ref)
                    is PlaylistLinks.Result.Rejected -> IncomingLink.Invalid(
                        if (r.wrongVersion) InvalidReason.UNSUPPORTED_VERSION else InvalidReason.MALFORMED,
                    )
                }

            isProfile(lower) -> when (val r = ProfileCardCodec.parse(text)) {
                is ProfileCardCodec.Result.Ok -> IncomingLink.Profile(r.card)
                is ProfileCardCodec.Result.Rejected -> IncomingLink.Invalid(
                    if (r.error == ProfileCardCodec.Error.WRONG_VERSION) InvalidReason.UNSUPPORTED_VERSION else InvalidReason.MALFORMED,
                )
            }

            else -> {
                val party = PartyLinks.parse(text, extraPartyBases)
                if (party != null) {
                    IncomingLink.Party(party)
                } else if (looksLikePartyLink(lower, extraPartyBases)) {
                    IncomingLink.Invalid(InvalidReason.MALFORMED)
                } else {
                    IncomingLink.Invalid(InvalidReason.NOT_A_VIOLENCE_LINK)
                }
            }
        }
    }

    /**
     * Reads a pasted message that may hold a link among other words. Takes the first
     * whitespace-separated token that is a valid Violence link; if there is none, the most
     * informative reason (so "wrong version" is not lost behind "not a link").
     */
    fun extract(text: String?, extraPartyBases: List<String> = emptyList()): IncomingLink {
        val input = text?.trim().orEmpty()
        if (input.isEmpty()) return IncomingLink.Invalid(InvalidReason.EMPTY)
        if (input.length > MAX_INPUT_CHARS) return IncomingLink.Invalid(InvalidReason.TOO_LONG)
        var best: IncomingLink = IncomingLink.Invalid(InvalidReason.NOT_A_VIOLENCE_LINK)
        for (token in input.split(Regex("\\s+"))) {
            if ("://" !in token) continue
            val candidate = parse(token.trim('<', '>', '(', ')', '"', '\'', ',', '.'), extraPartyBases)
            if (candidate !is IncomingLink.Invalid) return candidate
            if (candidate.reason != InvalidReason.NOT_A_VIOLENCE_LINK) best = candidate
        }
        return best
    }

    private fun isProfile(lower: String): Boolean {
        val base = lower.substringBefore('?').substringBefore('#').trimEnd('/')
        return base == ProfileCardCodec.CUSTOM_PREFIX || base == ProfileCardCodec.WEB_BASE.lowercase()
    }

    /** A link in a Violence shape whose code was bad: "malformed", not "someone else's link". */
    private fun looksLikePartyLink(lower: String, extraBases: List<String>): Boolean {
        if (lower.startsWith("violence://party") || lower.startsWith("bitchord://party")) return true
        val bases = listOf(JamInvite.ORIGIN) + extraBases.map { it.trim().trimEnd('/') }.filter { it.startsWith("https://", true) }
        return bases.any { b -> lower.startsWith("${b.lowercase()}/party/") || lower.startsWith("${b.lowercase()}/invite/") }
    }
}
