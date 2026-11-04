package com.music.violence.data.share

import com.music.violence.data.listentogether.JamInvite
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec

/** A Jam invite read out of a link: the party code and, when the party is on a custom server, its address. */
data class PartyInvite(val code: String, val serverUrl: String? = null, val from: ProfileCard? = null)

/**
 * Reading Jam invite links that arrive from outside (an NFC tag, a QR code, a pasted link).
 *
 * The forms are the ones the app already builds and accepts:
 * `violence://party/<CODE>[?server=<https address>]` (and the older `bitchord://party/...`), and
 * `https://yashaswahuh.is-a.dev/Violence/party/<CODE>` or `.../invite/<CODE>`, plus the same under
 * any extra web base the build was configured with (`JAM_INVITE_BASE_URL`). Plain `http` is never
 * an invite: joining is something done on the user's behalf.
 *
 * The code must be the real format: six characters of [ALPHABET], after the same forgiving
 * normalisation the Jam server applies (case, and I/L to 1, O to 0).
 */
object PartyLinks {

    const val CODE_LENGTH = 6

    /** What the Jam server mints codes from (no I, L or O). */
    const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTUVWXYZ"

    private const val MAX_INPUT_CHARS = 512
    private const val MAX_SERVER_CHARS = 200
    private val SERVER = Regex("""https://[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?(:[0-9]{1,5})?""")

    /** The normalised code for [raw] (six letters/digits), or null if it is not a real code. */
    fun normalizeCode(raw: String): String? {
        if (raw.length != CODE_LENGTH) return null
        val out = StringBuilder()
        for (c in raw.uppercase()) {
            val mapped = when (c) {
                'I', 'L' -> '1'
                'O' -> '0'
                else -> c
            }
            if (mapped !in ALPHABET) return null
            out.append(mapped)
        }
        return out.toString()
    }

    /** The invite in [input], or null if it is not a Jam invite link at all (or the code is not real). */
    fun parse(input: String?, extraWebBases: List<String> = emptyList()): PartyInvite? {
        val text = input?.trim().orEmpty()
        if (text.isEmpty() || text.length > MAX_INPUT_CHARS) return null
        val noFragment = text.substringBefore('#')
        val path = noFragment.substringBefore('?')
        val query = if (noFragment.contains('?')) noFragment.substringAfter('?') else null

        val code = when {
            path.startsWith("violence://party", ignoreCase = true) || path.startsWith("bitchord://party", ignoreCase = true) -> {
                val rest = path.substringAfter("://party").trim('/')
                (if (rest.isNotEmpty()) rest else queryParam(query, "code")) ?: return null
            }
            else -> webCode(path, extraWebBases) ?: return null
        }
        val normalized = normalizeCode(code) ?: return null
        val server = queryParam(query, "server")?.let(::cleanServer)
        // "Sent by": a label and a convenience only; a damaged one is simply ignored.
        val fromFields = SharedFrom.KEYS.mapNotNull { key -> rawParam(query, key)?.let { key to it } }.toMap()
        val from = (SharedFrom.read(fromFields) as? SharedFrom.Read.Ok)?.card
        return PartyInvite(normalized, server, from)
    }

    private fun webCode(path: String, extraWebBases: List<String>): String? {
        val bases = listOf(JamInvite.ORIGIN) + extraWebBases.map { it.trim().trimEnd('/') }.filter { it.startsWith("https://", ignoreCase = true) }
        for (base in bases) {
            for (kind in listOf("party", "invite")) {
                val prefix = "$base/$kind/"
                if (path.startsWith(prefix, ignoreCase = true)) {
                    val code = path.substring(prefix.length).trimEnd('/')
                    if ('/' !in code) return code
                }
            }
        }
        return null
    }

    /** The still percent-encoded value of [name]. */
    private fun rawParam(query: String?, name: String): String? {
        if (query.isNullOrEmpty()) return null
        for (pair in query.split('&')) {
            if (pair.substringBefore('=') == name && pair.contains('=')) return pair.substringAfter('=')
        }
        return null
    }

    private fun queryParam(query: String?, name: String): String? {
        if (query.isNullOrEmpty()) return null
        for (pair in query.split('&')) {
            if (pair.substringBefore('=').equals(name, ignoreCase = true) && pair.contains('=')) {
                return ProfileCardCodec.percentDecode(pair.substringAfter('='))
            }
        }
        return null
    }

    /** A custom Jam server address, https only; anything else is ignored (the invite still works). */
    private fun cleanServer(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty() || trimmed.length > MAX_SERVER_CHARS) return null
        return trimmed.takeIf { SERVER.matches(it) }
    }
}
