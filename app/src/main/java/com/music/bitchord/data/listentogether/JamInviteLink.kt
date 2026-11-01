package com.music.bitchord.data.listentogether

import android.content.Intent
import com.music.bitchord.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class ParsedJamInvite(
    val code: String,
    val serverUrl: String? = null,
)

/**
 * Violence party invites: building them, reading them back, and relaying one that arrived from
 * outside ([com.music.bitchord.MainActivity], [com.music.bitchord.TvActivity]) to Compose.
 *
 * Shapes, all carrying the same six-character code:
 *  - `violence://party/<CODE>[?server=<jam server>]`: opens the app. What a QR code holds when no
 *    web base is configured, and what the web landing page's "Open in Violence" button uses.
 *  - `<JAM_INVITE_BASE_URL>/party/<CODE>[?server=...]`: the web landing page. Only built when the
 *    owner configured a base; **empty means "share only the code"** (there is no built-in address).
 *  - `<anything>/invite/<CODE>`: the older web shape, still read (the Jam server itself serves one).
 */
object JamInviteLink {

    private const val EXTRA_CONSUMED = "violence.jamInviteConsumed"
    private const val CUSTOM_SCHEME = "violence"
    private const val CUSTOM_HOST = "party"

    /** The letters and digits the Jam server mints codes from (no I, L or O). */
    private const val CODE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTUVWXYZ"

    private val _pending = MutableStateFlow<ParsedJamInvite?>(null)
    val pending: StateFlow<ParsedJamInvite?> = _pending.asStateFlow()

    /** The configured web base for shared links (`JAM_INVITE_BASE_URL`), or "" when none. */
    val inviteBaseUrl: String get() = normalizeBase(BuildConfig.JAM_INVITE_BASE_URL)

    /** Reads an invite from a cold launch or a new intent on the existing task. */
    fun consume(intent: Intent?): Boolean {
        if (
            intent == null ||
            intent.action != Intent.ACTION_VIEW ||
            intent.getBooleanExtra(EXTRA_CONSUMED, false)
        ) return false

        val invite = parseInvite(intent.dataString) ?: return false
        intent.putExtra(EXTRA_CONSUMED, true)
        _pending.value = invite
        return true
    }

    fun handled() {
        _pending.value = null
    }

    /** Returns the normalized party code only for a recognised invite URL. */
    fun parse(value: String?): String? = parseInvite(value)?.code

    /**
     * Parses one invite URL:
     *  1. violence://party/<CODE>?server=<SERVER> (or the legacy bitchord:// scheme)
     *  2. https://<host>/<any base path>/party/<CODE>?server=<SERVER> or .../invite/<CODE>
     */
    fun parseInvite(value: String?): ParsedJamInvite? {
        val uri = runCatching { URI(value?.trim() ?: return null) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val query = uri.rawQuery
        val server = extractQueryParam(query, "server")?.let { sanitizeServerUrl(it) }

        if ((scheme == CUSTOM_SCHEME || scheme == "bitchord") && host == CUSTOM_HOST) {
            val pathPart = uri.path.orEmpty().trim('/').takeIf { it.isNotBlank() }
            val candidate = pathPart ?: extractQueryParam(query, "code") ?: return null
            val code = cleanCode(candidate) ?: return null
            return ParsedJamInvite(code = code, serverUrl = server)
        }

        // https only: a plain http link is not an invite (a join is an action taken on someone's behalf).
        if (scheme == "https") {
            val match = INVITE_PATH.matchEntire(uri.path.orEmpty()) ?: return null
            return ParsedJamInvite(code = match.groupValues[1].uppercase(), serverUrl = server)
        }
        return null
    }

    /**
     * The party code inside whatever somebody pasted or typed, or null.
     *
     * Accepts a bare code (in any case, with spaces or hyphens), a full invite link, or a whole
     * share message that contains either. Free text is only trusted when it holds exactly one
     * six-character token made of code letters, so ordinary words in a message are not mistaken
     * for a code.
     */
    fun extractCode(text: String?): String? {
        val input = text?.trim().orEmpty()
        if (input.isEmpty()) return null
        input.split(WHITESPACE).forEach { token ->
            if (token.contains("://")) {
                parseInvite(token.trim('<', '>', '(', ')', '"', '\'', ',', '.'))?.let { return it.code }
            }
        }
        val squashed = input.filter { it.isLetterOrDigit() }.uppercase()
        if (squashed.length == ListenTogether.CODE_LENGTH && !input.contains("://")) return squashed
        val candidates = input.split(NON_ALPHANUMERIC)
            .map { it.uppercase() }
            .filter { it.length == ListenTogether.CODE_LENGTH && it.all { c -> c in CODE_ALPHABET } }
            .distinct()
        return candidates.singleOrNull()
    }

    /**
     * The web link to share, or null when no base is configured (share only the code).
     * [customServer] is the Jam server holding the party when it is not the built-in one.
     */
    fun webLink(code: String, customServer: String? = null, base: String = inviteBaseUrl): String? {
        val origin = normalizeBase(base)
        if (origin.isEmpty()) return null
        val link = "$origin/party/${code.uppercase()}"
        val server = customServer?.trim()?.trimEnd('/')
        return if (server.isNullOrBlank()) link else "$link?server=${encode(server)}"
    }

    /** `violence://party/<CODE>`, which opens the app. Carries the Jam server when it is a custom one. */
    fun schemeUrl(code: String, customServer: String? = null): String {
        val normalizedCode = code.uppercase()
        val server = customServer?.trim()?.trimEnd('/')
        return if (!server.isNullOrBlank()) {
            "violence://party/$normalizedCode?server=${encode(server)}"
        } else {
            "violence://party/$normalizedCode"
        }
    }

    /** What a QR code should hold: the web link when there is one, else the app link. */
    fun qrContent(code: String, customServer: String? = null, base: String = inviteBaseUrl): String =
        webLink(code, customServer, base) ?: schemeUrl(code, customServer)

    private fun cleanCode(raw: String): String? {
        val cleaned = raw.filter { it.isLetterOrDigit() }.uppercase()
        return if (cleaned.length == ListenTogether.CODE_LENGTH) cleaned else null
    }

    private fun normalizeBase(raw: String?): String {
        val trimmed = raw?.trim()?.trimEnd('/').orEmpty()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun encode(value: String): String = runCatching { URLEncoder.encode(value, "UTF-8") }.getOrDefault(value)

    private fun extractQueryParam(query: String?, paramName: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split('&').asSequence()
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.isNotEmpty() && it[0].equals(paramName, ignoreCase = true) }
            ?.getOrNull(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }

    private fun sanitizeServerUrl(raw: String?): String? {
        val trimmed = raw?.trim()?.trimEnd('/') ?: return null
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (uri.host.isNullOrBlank()) return null
        return withScheme
    }

    /** `.../party/<CODE>` or `.../invite/<CODE>` under any base path (the site lives under /Violence/). */
    private val INVITE_PATH = Regex("""(?:/[^/]+)*/(?:party|invite)/([A-Za-z0-9]{${ListenTogether.CODE_LENGTH}})/?""")
    private val WHITESPACE = Regex("""\s+""")
    private val NON_ALPHANUMERIC = Regex("""[^A-Za-z0-9]+""")
}
