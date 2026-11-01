package com.music.bitchord.data.profile

import com.music.bitchord.data.listentogether.JamInvite

/**
 * The profile-card link: a small, versioned, strictly validated string that Phase 9 moves over
 * NFC, QR codes and shared links.
 *
 * ```
 * violence://profile?v=1&id=<uuid>&n=<name, percent-encoded>&a=<avatarId>&c=<hex6>
 * https://yashaswahuh.is-a.dev/Violence/profile?v=1&id=...        (same query)
 * ```
 *
 * Everything that arrives here is untrusted: a card pasted by a friend, scanned from a tag or
 * opened from a chat. [parse] never throws and never truncates or "repairs" a value; it either
 * returns a fully valid [ProfileCard] or says why not, by code only (the input is never echoed
 * into a message or a log).
 */
object ProfileCardCodec {

    const val VERSION = "1"
    const val CUSTOM_PREFIX = "violence://profile"
    const val WEB_BASE = "${JamInvite.ORIGIN}/profile"

    /** Longest input accepted at all. A valid card is far shorter; this bounds the work done. */
    const val MAX_INPUT_CHARS = 512

    /** What fits comfortably on a small NFC tag; Latin-script names stay under it. */
    const val NFC_TARGET_BYTES = 200

    enum class Error {
        NOT_A_CARD,
        TOO_LONG,
        WRONG_VERSION,
        MISSING_FIELD,
        DUPLICATE_FIELD,
        UNKNOWN_FIELD,
        BAD_ID,
        BAD_ENCODING,
        BAD_NAME,
        BAD_AVATAR,
        BAD_COLOR,
    }

    sealed interface Result {
        data class Ok(val card: ProfileCard) : Result
        data class Rejected(val error: Error, val nameProblem: ProfileRules.NameProblem? = null) : Result
    }

    private val KEYS = setOf("v", "id", "n", "a", "c")

    /** The `violence://` form of [card]. */
    fun toUri(card: ProfileCard): String = CUSTOM_PREFIX + query(card)

    /** The https form of [card], for the landing page and App Links. */
    fun toWebUrl(card: ProfileCard): String = WEB_BASE + query(card)

    /** Whether [uri] is small enough for the NFC target. */
    fun fitsNfcTarget(uri: String): Boolean = uri.encodeToByteArray().size <= NFC_TARGET_BYTES

    private fun query(card: ProfileCard) =
        "?v=$VERSION&id=${card.id}&n=${percentEncode(card.displayName)}&a=${card.avatarId}&c=${card.color}"

    fun parse(input: String?): Result {
        val text = input?.trim().orEmpty()
        if (text.length > MAX_INPUT_CHARS) return Result.Rejected(Error.TOO_LONG)
        val queryText = queryOf(text) ?: return Result.Rejected(Error.NOT_A_CARD)

        val fields = HashMap<String, String>()
        for (pair in queryText.split('&')) {
            if (pair.isEmpty()) continue
            val key = pair.substringBefore('=')
            val value = if (pair.contains('=')) pair.substringAfter('=') else ""
            if (key !in KEYS) return Result.Rejected(Error.UNKNOWN_FIELD)
            if (fields.put(key, value) != null) return Result.Rejected(Error.DUPLICATE_FIELD)
        }

        val version = fields["v"] ?: return Result.Rejected(Error.MISSING_FIELD)
        if (version != VERSION) return Result.Rejected(Error.WRONG_VERSION)
        val rawId = fields["id"] ?: return Result.Rejected(Error.MISSING_FIELD)
        val rawName = fields["n"] ?: return Result.Rejected(Error.MISSING_FIELD)
        val rawAvatar = fields["a"] ?: return Result.Rejected(Error.MISSING_FIELD)
        val rawColor = fields["c"] ?: return Result.Rejected(Error.MISSING_FIELD)

        val id = ProfileRules.normalizeId(rawId) ?: return Result.Rejected(Error.BAD_ID)
        val decoded = percentDecode(rawName) ?: return Result.Rejected(Error.BAD_ENCODING)
        val name = when (val checked = ProfileRules.checkName(decoded)) {
            is ProfileRules.NameResult.Valid -> checked.name
            is ProfileRules.NameResult.Invalid -> return Result.Rejected(Error.BAD_NAME, checked.problem)
        }
        if (!AvatarCatalog.isKnown(rawAvatar)) return Result.Rejected(Error.BAD_AVATAR)
        val color = ProfileRules.normalizeColor(rawColor) ?: return Result.Rejected(Error.BAD_COLOR)
        return Result.Ok(ProfileCard(id, name, rawAvatar, color))
    }

    /** The query part if [text] is one of the two card shapes, else null. */
    private fun queryOf(text: String): String? {
        val withoutFragment = text.substringBefore('#')
        val base = withoutFragment.substringBefore('?')
        if (!withoutFragment.contains('?')) return null
        val trimmedBase = base.trimEnd('/')
        val isCard = trimmedBase.equals(CUSTOM_PREFIX, ignoreCase = true) ||
            trimmedBase.equals(WEB_BASE, ignoreCase = true)
        if (!isCard) return null
        return withoutFragment.substringAfter('?')
    }

    // ----------------------------------------------------------- percent encoding --

    private const val HEX = "0123456789ABCDEF"

    internal fun percentEncode(value: String): String {
        val out = StringBuilder()
        for (b in value.encodeToByteArray()) {
            val v = b.toInt() and 0xFF
            val c = v.toChar()
            if (v < 0x80 && (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '.' || c == '_' || c == '~')) {
                out.append(c)
            } else {
                out.append('%').append(HEX[v shr 4]).append(HEX[v and 0xF])
            }
        }
        return out.toString()
    }

    /**
     * Reads `%XX` escapes (and `+` as a space) into UTF-8. A stray `%`, a bad escape, raw
     * whitespace/control characters or bytes that are not valid UTF-8 give null. Raw non-ASCII
     * characters are tolerated, so a link a chat app showed decoded still reads.
     */
    internal fun percentDecode(value: String): String? {
        val bytes = ArrayList<Byte>(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when {
                c == '%' -> {
                    if (i + 2 >= value.length) return null
                    val hi = hexValue(value.getOrNull(i + 1)) ?: return null
                    val lo = hexValue(value.getOrNull(i + 2)) ?: return null
                    bytes.add(((hi shl 4) or lo).toByte())
                    i += 3
                }
                c == '+' -> {
                    bytes.add(' '.code.toByte())
                    i++
                }
                c.code < 0x21 || c.code == 0x7F -> return null
                c.code < 0x80 -> {
                    bytes.add(c.code.toByte())
                    i++
                }
                else -> {
                    // A raw non-ASCII character: take its UTF-16 unit(s) and encode as UTF-8.
                    val end = if (c.isHighSurrogate() && i + 1 < value.length) i + 2 else i + 1
                    value.substring(i, end).encodeToByteArray().forEach(bytes::add)
                    i = end
                }
            }
        }
        return try {
            bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun hexValue(c: Char?): Int? = when (c) {
        null -> null
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> null
    }
}
