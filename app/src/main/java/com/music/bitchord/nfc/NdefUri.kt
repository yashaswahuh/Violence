package com.music.bitchord.nfc

/**
 * The one NDEF record a shared item travels in: a single, short, well-known URI record.
 *
 * Built by hand because nothing more is needed and because the byte layout is what the unit
 * tests pin down (the whole of what goes over the air is these bytes):
 *
 * ```
 * D1        flags: MB, ME, SR (short record), TNF = 1 (well-known)
 * 01        type length
 * LL        payload length (1 byte, = 1 + the URI's UTF-8 length)
 * 55        type "U"
 * 00        URI identifier code 0: no abbreviation, the URI follows in full
 * ...       the URI, UTF-8
 * ```
 *
 * Reading is strict: exactly one record, well-known type `U`, no chunking, nothing left over.
 */
object NdefUri {

    /** The longest URI that is ever put on the air (so the record, NLEN included, is at most 257 bytes). */
    const val MAX_URI_BYTES = 250

    private const val FLAGS_SHORT_URI: Int = 0xD1
    private const val MB = 0x80
    private const val ME = 0x40
    private const val CF = 0x20
    private const val SR = 0x10
    private const val IL = 0x08
    private const val TNF_MASK = 0x07
    private const val TNF_WELL_KNOWN = 0x01
    private const val TYPE_URI: Int = 0x55

    /** Longest payload a reader will accept (a chunk of a long URI would still be a bounded read). */
    private const val MAX_PAYLOAD = 1024

    /** The NDEF message for [uri], or null when it is empty or longer than [MAX_URI_BYTES]. */
    fun message(uri: String): ByteArray? {
        val bytes = uri.encodeToByteArray()
        if (bytes.isEmpty() || bytes.size > MAX_URI_BYTES) return null
        val out = ByteArray(5 + bytes.size)
        out[0] = FLAGS_SHORT_URI.toByte()
        out[1] = 0x01
        out[2] = (1 + bytes.size).toByte()
        out[3] = TYPE_URI.toByte()
        out[4] = 0x00
        bytes.copyInto(out, 5)
        return out
    }

    /** The URI inside [message], or null if it is not exactly one well-formed URI record. */
    fun parse(message: ByteArray): String? {
        if (message.size < 5) return null
        val flags = message[0].toInt() and 0xFF
        // One record only: it has to be both the first and the last, and not a chunk.
        if (flags and MB == 0 || flags and ME == 0 || flags and CF != 0) return null
        if (flags and TNF_MASK != TNF_WELL_KNOWN) return null
        val short = flags and SR != 0
        val hasId = flags and IL != 0

        var i = 1
        val typeLength = message[i++].toInt() and 0xFF
        val payloadLength: Long
        if (short) {
            payloadLength = (message[i++].toLong() and 0xFF)
        } else {
            if (message.size < i + 4) return null
            payloadLength = ((message[i].toLong() and 0xFF) shl 24) or ((message[i + 1].toLong() and 0xFF) shl 16) or
                ((message[i + 2].toLong() and 0xFF) shl 8) or (message[i + 3].toLong() and 0xFF)
            i += 4
        }
        val idLength = if (hasId) {
            if (message.size <= i) return null
            message[i++].toInt() and 0xFF
        } else {
            0
        }
        if (payloadLength < 1 || payloadLength > MAX_PAYLOAD) return null
        if (typeLength != 1) return null
        if (message.size.toLong() != i.toLong() + typeLength + idLength + payloadLength) return null

        if ((message[i].toInt() and 0xFF) != TYPE_URI) return null
        i += typeLength + idLength
        val prefix = when (message[i].toInt() and 0xFF) {
            0x00 -> ""
            0x04 -> "https://"
            else -> return null
        }
        val rest = message.copyOfRange(i + 1, message.size)
        return runCatching { prefix + decodeUtf8Strict(rest) }.getOrNull()
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String = bytes.decodeToString(throwOnInvalidSequence = true)
}
