package com.music.bitchord.data.share

import com.music.bitchord.data.profile.ProfileRules

/**
 * Cleaning the song titles and artists that go into a link.
 *
 * What a track is called comes from YouTube and can hold anything, so before it goes into a
 * shareable link every character the receiver would refuse (control, invisible, format) is
 * dropped and the text is cut to a sensible size. The receiving side is strict and rejects
 * rather than repairs; this is the sending side being tidy.
 */
object ShareText {

    /** More than the receiver accepts is never sent; the link builder shortens it further to fit. */
    const val TITLE_BUILD_MAX = SongLinks.TITLE_MAX
    const val ARTIST_BUILD_MAX = SongLinks.ARTIST_MAX

    /** [raw] without characters a link may not carry, spaces collapsed, at most [max] code points. */
    fun sanitize(raw: String, max: Int): String {
        val points = ProfileRules.codePoints(raw) ?: return ""
        val kept = StringBuilder()
        for (cp in points) {
            val piece = fromCodePoint(cp)
            val ok = ProfileRules.checkText(piece, 1) is ProfileRules.NameResult.Valid
            // A space (or a tab, newline, no-break space) becomes a plain space; anything else the
            // receiver would refuse is dropped.
            if (ok) kept.append(piece) else if (piece.isBlank()) kept.append(' ')
        }
        val collapsed = ProfileRules.checkText(kept.toString().trim().replace(Regex(" +"), " "), Int.MAX_VALUE)
        val clean = (collapsed as? ProfileRules.NameResult.Valid)?.name ?: return ""
        return truncate(clean, max)
    }

    /** At most [max] code points of [text], cut on a character boundary, with trailing space removed. */
    fun truncate(text: String, max: Int): String {
        val points = ProfileRules.codePoints(text) ?: return ""
        if (points.size <= max) return text
        val out = StringBuilder()
        for (i in 0 until max) out.append(fromCodePoint(points[i]))
        return out.toString().trimEnd()
    }

    /** [text] without its last code point. */
    fun dropLast(text: String): String {
        val points = ProfileRules.codePoints(text) ?: return ""
        if (points.isEmpty()) return ""
        val out = StringBuilder()
        for (i in 0 until points.size - 1) out.append(fromCodePoint(points[i]))
        return out.toString().trimEnd()
    }

    private fun fromCodePoint(cp: Int): String =
        if (cp < 0x10000) {
            cp.toChar().toString()
        } else {
            val v = cp - 0x10000
            "" + (0xD800 + (v shr 10)).toChar() + (0xDC00 + (v and 0x3FF)).toChar()
        }
}
