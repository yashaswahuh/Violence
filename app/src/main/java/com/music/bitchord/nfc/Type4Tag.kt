package com.music.bitchord.nfc

/**
 * The NFC Forum Type 4 Tag "NDEF Tag Application", both halves, as plain byte-in/byte-out code.
 *
 * [Type4TagEmulator] is what the sharing phone answers with (through a `HostApduService`);
 * [Type4TagReader] is what the receiving phone asks with (over `IsoDep`). Neither touches Android,
 * so the whole conversation, down to every status word, is covered by JVM unit tests, including a
 * test that runs the real reader against the real emulator.
 *
 * Everything here is short-APDU only and read-only. See `fork-docs/SHARE_DESIGN.md` section 3 for
 * the tables; the AID, file ids and status words are from the NFC Forum Type 4 Tag specification
 * as commonly implemented (UNVERIFIED against the spec text itself, which is not freely available).
 */
object Type4Tag {

    /** "NDEF Tag Application" AID: D2 76 00 00 85 01 01. */
    val AID: ByteArray = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01)

    const val CC_FILE_ID = 0xE103
    const val NDEF_FILE_ID = 0xE104

    /** Most bytes a reader may ask for in one READ BINARY (advertised as MLe in the capability container). */
    const val MLE = 0x00FF

    /** NLEN (2) + the longest record ([NdefUri.MAX_URI_BYTES] + 5). */
    const val NDEF_FILE_MAX = 2 + 5 + NdefUri.MAX_URI_BYTES

    /** What a reader refuses to read: anything longer than this many message bytes. */
    const val READER_MAX_MESSAGE = 512

    // Status words (SW1 SW2)
    val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
    val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
    val SW_INCORRECT_P1P2 = byteArrayOf(0x6A, 0x86.toByte())
    val SW_WRONG_OFFSET = byteArrayOf(0x6B, 0x00)
    val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
    val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69, 0x85.toByte())
    val SW_SECURITY_NOT_SATISFIED = byteArrayOf(0x69, 0x82.toByte())
    val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
    val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E, 0x00)
    val SW_UNKNOWN = byteArrayOf(0x6F, 0x00)

    /** The 15-byte capability container (file E103). */
    fun capabilityContainer(): ByteArray = byteArrayOf(
        0x00, 0x0F, // CCLEN
        0x20, // mapping version 2.0
        0x00, 0xFF.toByte(), // MLe
        0x00, 0xFF.toByte(), // MLc
        0x04, 0x06, // NDEF File Control TLV
        0xE1.toByte(), 0x04, // NDEF file id
        (NDEF_FILE_MAX shr 8).toByte(), (NDEF_FILE_MAX and 0xFF).toByte(), // max NDEF file size
        0x00, // read access: granted
        0xFF.toByte(), // write access: denied
    )

    /** The NDEF file (E104) for [message]: its length as two bytes (NLEN), then the message. */
    fun ndefFile(message: ByteArray): ByteArray {
        val out = ByteArray(2 + message.size)
        out[0] = (message.size shr 8).toByte()
        out[1] = (message.size and 0xFF).toByte()
        message.copyInto(out, 2)
        return out
    }

    fun status(response: ByteArray): Int =
        if (response.size < 2) -1 else ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)

    fun isOk(response: ByteArray): Boolean = status(response) == 0x9000
}

/**
 * The card side. One instance per HCE service; [reset] when the link drops.
 *
 * [message] is asked for each time an NDEF file is selected, so a conversation reads one
 * consistent snapshot even if the shared item changes in between. When it returns null (no share
 * is active) nothing is revealed: selecting the application answers "not found".
 */
class Type4TagEmulator(
    private val message: () -> ByteArray?,
    /** Called when a reader has read the NDEF file through to its last byte (the link was delivered). */
    private val onDelivered: () -> Unit = {},
) {

    private enum class File { NONE, CC, NDEF }

    private var appSelected = false
    private var selected = File.NONE
    private var ndefSnapshot: ByteArray? = null

    fun reset() {
        appSelected = false
        selected = File.NONE
        ndefSnapshot = null
    }

    /** Answers one command APDU. Never throws. */
    fun process(apdu: ByteArray): ByteArray = try {
        handle(apdu)
    } catch (_: RuntimeException) {
        Type4Tag.SW_UNKNOWN
    }

    private fun handle(apdu: ByteArray): ByteArray {
        if (apdu.size < 4) return Type4Tag.SW_WRONG_LENGTH
        val cla = apdu[0].toInt() and 0xFF
        if (cla != 0x00) return Type4Tag.SW_CLA_NOT_SUPPORTED
        return when (apdu[1].toInt() and 0xFF) {
            0xA4 -> select(apdu)
            0xB0 -> readBinary(apdu)
            0xD6, 0xD7 -> Type4Tag.SW_SECURITY_NOT_SATISFIED
            else -> Type4Tag.SW_INS_NOT_SUPPORTED
        }
    }

    private fun select(apdu: ByteArray): ByteArray {
        if (apdu.size < 5) return Type4Tag.SW_WRONG_LENGTH
        val p1 = apdu[2].toInt() and 0xFF
        val p2 = apdu[3].toInt() and 0xFF
        val lc = apdu[4].toInt() and 0xFF
        // Header(4) + Lc(1) + data, with an optional trailing Le.
        if (apdu.size != 5 + lc && apdu.size != 5 + lc + 1) return Type4Tag.SW_WRONG_LENGTH
        val data = apdu.copyOfRange(5, 5 + lc)

        return when (p1) {
            0x04 -> {
                if (data.contentEquals(Type4Tag.AID) && message() != null) {
                    appSelected = true
                    selected = File.NONE
                    ndefSnapshot = null
                    Type4Tag.SW_OK
                } else {
                    appSelected = false
                    selected = File.NONE
                    Type4Tag.SW_FILE_NOT_FOUND
                }
            }
            0x00 -> {
                if (p2 != 0x00 && p2 != 0x0C) return Type4Tag.SW_INCORRECT_P1P2
                if (lc != 2) return Type4Tag.SW_WRONG_LENGTH
                if (!appSelected) return Type4Tag.SW_CONDITIONS_NOT_SATISFIED
                when (((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)) {
                    Type4Tag.CC_FILE_ID -> {
                        selected = File.CC
                        Type4Tag.SW_OK
                    }
                    Type4Tag.NDEF_FILE_ID -> {
                        val current = message() ?: return Type4Tag.SW_FILE_NOT_FOUND
                        ndefSnapshot = Type4Tag.ndefFile(current)
                        selected = File.NDEF
                        Type4Tag.SW_OK
                    }
                    else -> Type4Tag.SW_FILE_NOT_FOUND
                }
            }
            else -> Type4Tag.SW_INCORRECT_P1P2
        }
    }

    private fun readBinary(apdu: ByteArray): ByteArray {
        if (apdu.size != 5) return Type4Tag.SW_WRONG_LENGTH
        val p1 = apdu[2].toInt() and 0xFF
        // P1 bit 8 set means "short file id", which this tag does not offer.
        if (p1 and 0x80 != 0) return Type4Tag.SW_WRONG_OFFSET
        val offset = (p1 shl 8) or (apdu[3].toInt() and 0xFF)
        val requested = (apdu[4].toInt() and 0xFF).let { if (it == 0) 256 else it }
        val content = when (selected) {
            File.CC -> Type4Tag.capabilityContainer()
            File.NDEF -> ndefSnapshot ?: return Type4Tag.SW_FILE_NOT_FOUND
            File.NONE -> return Type4Tag.SW_CONDITIONS_NOT_SATISFIED
        }
        if (offset > content.size) return Type4Tag.SW_WRONG_OFFSET
        val count = minOf(requested, Type4Tag.MLE, content.size - offset)
        // The message starts after the 2-byte length, so a read that ends exactly at the end of the
        // file, having returned message bytes, means the whole link has now been read.
        if (selected == File.NDEF && offset >= 2 && count > 0 && offset + count == content.size) onDelivered()
        val out = ByteArray(count + 2)
        content.copyInto(out, 0, offset, offset + count)
        out[count] = 0x90.toByte()
        out[count + 1] = 0x00
        return out
    }
}

/**
 * The reader side: performs the standard Type 4 NDEF read over any APDU transport.
 *
 * Hostile or broken tags are expected, so every length is checked before it is trusted and the
 * amount read is bounded by [Type4Tag.READER_MAX_MESSAGE]. Transport errors (`IOException` from
 * `IsoDep.transceive`) propagate to the caller.
 */
object Type4TagReader {

    enum class Failure { NOT_AN_NDEF_TAG, BAD_CAPABILITY_CONTAINER, NOT_READABLE, TOO_BIG, TRUNCATED }

    sealed interface Result {
        data class Ok(val message: ByteArray) : Result
        data class Failed(val reason: Failure) : Result
    }

    fun read(transceive: (ByteArray) -> ByteArray): Result {
        // 1. Select the NDEF Tag Application.
        val selectApp = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, Type4Tag.AID.size.toByte()) + Type4Tag.AID + byteArrayOf(0x00)
        if (!Type4Tag.isOk(transceive(selectApp))) return Result.Failed(Failure.NOT_AN_NDEF_TAG)

        // 2. Capability container.
        if (!Type4Tag.isOk(transceive(selectFile(Type4Tag.CC_FILE_ID)))) return Result.Failed(Failure.BAD_CAPABILITY_CONTAINER)
        val ccResponse = transceive(readBinary(0, 15))
        // Exactly the 15 bytes asked for, then the status word: a tag that sends more is not a tag.
        if (!Type4Tag.isOk(ccResponse) || ccResponse.size != 17) return Result.Failed(Failure.BAD_CAPABILITY_CONTAINER)
        val cc = ccResponse.copyOf(ccResponse.size - 2)
        val ccLength = u16(cc, 0)
        if (ccLength < 15 || ccLength > cc.size) return Result.Failed(Failure.BAD_CAPABILITY_CONTAINER)
        if ((cc[2].toInt() and 0xFF) shr 4 != 2) return Result.Failed(Failure.BAD_CAPABILITY_CONTAINER)
        val mle = u16(cc, 3)
        val chunk = mle.coerceIn(1, 255)

        // Find the NDEF File Control TLV (type 04, length 06).
        var index = 7
        var fileId = -1
        var maxSize = 0
        var readAccess = -1
        while (index + 2 <= ccLength) {
            val type = cc[index].toInt() and 0xFF
            val length = cc[index + 1].toInt() and 0xFF
            if (type == 0x04 && length == 6 && index + 2 + 6 <= ccLength) {
                fileId = u16(cc, index + 2)
                maxSize = u16(cc, index + 4)
                readAccess = cc[index + 6].toInt() and 0xFF
                break
            }
            index += 2 + length
        }
        if (fileId < 0) return Result.Failed(Failure.BAD_CAPABILITY_CONTAINER)
        if (readAccess != 0x00) return Result.Failed(Failure.NOT_READABLE)

        // 3. NDEF file: its length first.
        if (!Type4Tag.isOk(transceive(selectFile(fileId)))) return Result.Failed(Failure.NOT_READABLE)
        val nlenResponse = transceive(readBinary(0, 2))
        if (!Type4Tag.isOk(nlenResponse) || nlenResponse.size != 4) return Result.Failed(Failure.TRUNCATED)
        val nlen = u16(nlenResponse, 0)
        if (nlen == 0) return Result.Failed(Failure.NOT_AN_NDEF_TAG)
        if (nlen > Type4Tag.READER_MAX_MESSAGE || (maxSize in 1..65535 && nlen + 2 > maxSize)) return Result.Failed(Failure.TOO_BIG)

        // 4. The message, in chunks no bigger than the tag allows.
        val message = ByteArray(nlen)
        var got = 0
        while (got < nlen) {
            val want = minOf(chunk, nlen - got)
            val response = transceive(readBinary(2 + got, want))
            if (!Type4Tag.isOk(response)) return Result.Failed(Failure.TRUNCATED)
            val data = response.size - 2
            if (data < 1 || data > want) return Result.Failed(Failure.TRUNCATED)
            response.copyInto(message, got, 0, data)
            got += data
        }
        return Result.Ok(message)
    }

    private fun selectFile(id: Int) = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, (id shr 8).toByte(), (id and 0xFF).toByte())

    private fun readBinary(offset: Int, length: Int) =
        byteArrayOf(0x00, 0xB0.toByte(), (offset shr 8).toByte(), (offset and 0xFF).toByte(), length.toByte())

    private fun u16(bytes: ByteArray, at: Int) = ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)
}
