package com.music.violence.data.listentogether

/**
 * The public invite URL for a party, and the only shape that is accepted back.
 *
 * Shared because both applications have to agree on it exactly: a link built on one and opened on
 * the other must resolve to the same code, and a link that is not this shape must be refused on
 * both rather than half-parsed into a join attempt.
 */
object JamInvite {

    /** The owner's web player (GitHub Pages is case sensitive: the path is /Violence/). */
    const val ORIGIN = "https://yashaswahuh.is-a.dev/Violence"

    /** How many characters a party code carries. */
    const val CODE_LENGTH = 6

    /** The normalized party code, for the public landing-page shapes only (`/party/` and the older `/invite/`). */
    fun parse(value: String?): String? {
        val text = value?.trim() ?: return null
        val prefix = listOf("$ORIGIN/party/", "$ORIGIN/invite/").firstOrNull { text.startsWith(it, ignoreCase = true) }
            ?: return null
        val code = text.substring(prefix.length).substringBefore('?').substringBefore('#')
        if (code.length != CODE_LENGTH || !code.all(Char::isLetterOrDigit)) return null
        return code.uppercase()
    }

    fun url(code: String): String = "$ORIGIN/party/${code.uppercase()}"
}
