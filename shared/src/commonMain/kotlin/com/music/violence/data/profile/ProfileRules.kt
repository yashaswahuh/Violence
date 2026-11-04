package com.music.violence.data.profile

/**
 * What the in-app profile is allowed to contain, in one place.
 *
 * A name typed into the edit screen, a name read out of a friend's card and a name read out
 * of storage all go through the same checks, so a rule can never be stricter on one path than
 * on another. Nothing here touches Android, the clock or the network.
 */
object ProfileRules {

    /** Names are counted in Unicode code points (an emoji is one, a family emoji is several). */
    const val NAME_MAX_CODE_POINTS = 24

    /** The Jam server rejects display names longer than this many UTF-8 bytes. */
    const val JAM_NAME_MAX_BYTES = 80

    const val DEFAULT_AVATAR_ID = "headphones"
    const val DEFAULT_COLOR = "fa2d48"

    /** Colour swatches offered by the editors. Any six hex digits is also accepted. */
    val COLOR_PRESETS: List<String> = listOf(
        "fa2d48", "ff9500", "ffcc00", "34c759", "30b0c7",
        "0a84ff", "5e5ce6", "bf5af2", "ff375f", "8e8e93",
    )

    enum class NameProblem { EMPTY, TOO_LONG, CONTROL_CHARACTER, INVISIBLE_CHARACTER }

    sealed interface NameResult {
        data class Valid(val name: String) : NameResult
        data class Invalid(val problem: NameProblem) : NameResult
    }

    /** Characters that render as nothing but are not caught by their Unicode category. */
    private val INVISIBLE_LOOKALIKES = setOf(
        0x034F, 0x115F, 0x1160, 0x17B4, 0x17B5, 0x2800, 0x3164, 0xFFA0,
    )

    /** More stacked combining marks than this in a row is "zalgo" text, not a name. */
    private const val MAX_COMBINING_RUN = 4

    /**
     * Normalises [raw] (trim, every run of spaces becomes one space) and checks it.
     *
     * Rejects control characters, invisible/format characters (the zero-width joiner stays: emoji
     * sequences need it), line and paragraph separators, lone surrogates, private-use and
     * non-characters, tag characters, and runs of stacked combining marks. Too long is rejected,
     * never truncated, so a card cannot silently become someone else's name.
     */
    fun checkName(raw: String): NameResult = checkText(raw, NAME_MAX_CODE_POINTS)

    /**
     * [checkName] with a different length limit: the same character rules for any short piece
     * of text that came from somebody else (a song title or artist hint in a shared link).
     */
    fun checkText(raw: String, maxCodePoints: Int): NameResult {
        val points = codePoints(raw) ?: return NameResult.Invalid(NameProblem.INVISIBLE_CHARACTER)
        val out = StringBuilder()
        val kept = ArrayList<Int>()
        var combiningRun = 0
        var pendingSpace = false
        for (cp in points) {
            val kind = classify(cp)
            when (kind) {
                CharKind.CONTROL -> return NameResult.Invalid(NameProblem.CONTROL_CHARACTER)
                CharKind.INVISIBLE -> return NameResult.Invalid(NameProblem.INVISIBLE_CHARACTER)
                CharKind.SPACE -> {
                    pendingSpace = kept.isNotEmpty()
                    combiningRun = 0
                    continue
                }
                CharKind.MARK -> {
                    combiningRun++
                    if (combiningRun > MAX_COMBINING_RUN) {
                        return NameResult.Invalid(NameProblem.INVISIBLE_CHARACTER)
                    }
                }
                CharKind.OTHER -> combiningRun = 0
            }
            if (pendingSpace) {
                out.append(' ')
                kept.add(' '.code)
                pendingSpace = false
            }
            appendCodePoint(out, cp)
            kept.add(cp)
        }
        if (kept.isEmpty()) return NameResult.Invalid(NameProblem.EMPTY)
        if (kept.size > maxCodePoints) return NameResult.Invalid(NameProblem.TOO_LONG)
        return NameResult.Valid(out.toString())
    }

    /** [checkName] as a nullable. */
    fun normalizeName(raw: String): String? = (checkName(raw) as? NameResult.Valid)?.name

    /** The canonical lower-case dashed UUID for [raw] (dashed or 32 plain hex digits), or null. */
    fun normalizeId(raw: String): String? {
        val text = raw.trim().lowercase()
        val hex = when (text.length) {
            36 -> {
                if (text[8] != '-' || text[13] != '-' || text[18] != '-' || text[23] != '-') return null
                text.filterIndexed { i, _ -> i != 8 && i != 13 && i != 18 && i != 23 }
            }
            32 -> text
            else -> return null
        }
        if (hex.length != 32 || !hex.all(::isHex)) return null
        if (hex.all { it == '0' }) return null
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20)}"
    }

    /** Six hex digits (no `#`), lower-cased, or null. */
    fun normalizeColor(raw: String): String? {
        val text = raw.trim().lowercase()
        return text.takeIf { it.length == 6 && it.all(::isHex) }
    }

    /**
     * [name] cut so its UTF-8 form is at most [maxBytes] bytes, never in the middle of a
     * character. Used for the one place a longer name than the server accepts can occur.
     */
    fun truncateUtf8(name: String, maxBytes: Int = JAM_NAME_MAX_BYTES): String {
        if (name.encodeToByteArray().size <= maxBytes) return name
        val points = codePoints(name) ?: return ""
        val out = StringBuilder()
        var bytes = 0
        for (cp in points) {
            val size = utf8Size(cp)
            if (bytes + size > maxBytes) break
            bytes += size
            appendCodePoint(out, cp)
        }
        return out.toString().trimEnd()
    }

    // ------------------------------------------------------------------ internals --

    private enum class CharKind { CONTROL, INVISIBLE, SPACE, MARK, OTHER }

    private fun isHex(c: Char) = c in '0'..'9' || c in 'a'..'f'

    private fun utf8Size(cp: Int) = when {
        cp < 0x80 -> 1
        cp < 0x800 -> 2
        cp < 0x10000 -> 3
        else -> 4
    }

    private fun appendCodePoint(out: StringBuilder, cp: Int) {
        if (cp < 0x10000) {
            out.append(cp.toChar())
        } else {
            val v = cp - 0x10000
            out.append((0xD800 + (v shr 10)).toChar())
            out.append((0xDC00 + (v and 0x3FF)).toChar())
        }
    }

    /** The code points of [text], or null if it holds an unpaired surrogate. */
    internal fun codePoints(text: String): List<Int>? {
        val result = ArrayList<Int>(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isHighSurrogate() -> {
                    val next = text.getOrNull(i + 1)
                    if (next == null || !next.isLowSurrogate()) return null
                    result.add(0x10000 + ((c.code - 0xD800) shl 10) + (next.code - 0xDC00))
                    i += 2
                }
                c.isLowSurrogate() -> return null
                else -> {
                    result.add(c.code)
                    i++
                }
            }
        }
        return result
    }

    private fun classify(cp: Int): CharKind {
        if (cp in INVISIBLE_LOOKALIKES) return CharKind.INVISIBLE
        if (cp >= 0x10000) {
            return when {
                // Tag characters (invisible, used to smuggle text), planes 15 and 16 (private use).
                cp in 0xE0000..0xE007F -> CharKind.INVISIBLE
                cp >= 0xF0000 -> CharKind.INVISIBLE
                // The last two code points of every plane are non-characters.
                (cp and 0xFFFF) >= 0xFFFE -> CharKind.INVISIBLE
                // Variation selectors supplement is a combining mark.
                cp in 0xE0100..0xE01EF -> CharKind.MARK
                else -> CharKind.OTHER
            }
        }
        if (cp in 0xFDD0..0xFDEF || cp == 0xFFFE || cp == 0xFFFF) return CharKind.INVISIBLE
        if (cp == 0x200D) return CharKind.OTHER // zero-width joiner, needed by emoji sequences
        return when (cp.toChar().category) {
            CharCategory.CONTROL -> CharKind.CONTROL
            CharCategory.FORMAT,
            CharCategory.LINE_SEPARATOR,
            CharCategory.PARAGRAPH_SEPARATOR,
            CharCategory.PRIVATE_USE,
            CharCategory.SURROGATE,
            -> CharKind.INVISIBLE
            CharCategory.SPACE_SEPARATOR -> CharKind.SPACE
            CharCategory.NON_SPACING_MARK,
            CharCategory.ENCLOSING_MARK,
            -> if (cp == 0xFE0F || cp == 0xFE0E) CharKind.OTHER else CharKind.MARK
            else -> CharKind.OTHER
        }
    }
}
