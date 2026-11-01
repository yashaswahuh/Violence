package com.music.bitchord.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Byte-level tests of the Type 4 Tag conversation. */
class Type4TagTest {

    private fun hex(text: String): ByteArray {
        val clean = text.replace(" ", "")
        return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun ByteArray.hex() = joinToString("") { "%02X".format(it) }

    private val uri = "violence://party/AB12CD"
    private val message = NdefUri.message(uri)!!

    private fun emulator(current: () -> ByteArray? = { message }) = Type4TagEmulator(current)

    private val selectAid = hex("00 A4 04 00 07 D2760000850101 00")
    private val selectCc = hex("00 A4 00 0C 02 E103")
    private val selectNdef = hex("00 A4 00 0C 02 E104")
    private fun read(offset: Int, le: Int) = hex("00 B0") + byteArrayOf((offset shr 8).toByte(), offset.toByte(), le.toByte())

    private fun Type4TagEmulator.ready(): Type4TagEmulator {
        assertEquals("9000", process(selectAid).hex())
        return this
    }

    // ------------------------------------------------------------ selecting --

    @Test
    fun selectsTheApplicationWithAndWithoutTrailingLe() {
        assertEquals("9000", emulator().process(selectAid).hex())
        assertEquals("9000", emulator().process(hex("00 A4 04 00 07 D2760000850101")).hex())
    }

    @Test
    fun theApplicationIsNotFoundWhenNothingIsBeingShared() {
        assertEquals("6A82", emulator { null }.process(selectAid).hex())
    }

    @Test
    fun anotherAidIsNotFound() {
        assertEquals("6A82", emulator().process(hex("00 A4 04 00 07 D2760000850102 00")).hex())
        assertEquals("6A82", emulator().process(hex("00 A4 04 00 05 D276000085 00")).hex())
        assertEquals("6A82", emulator().process(hex("00 A4 04 00 08 D276000085010100")).hex())
        // an AID that was found earlier is dropped when another is selected
        val e = emulator().ready()
        assertEquals("6A82", e.process(hex("00 A4 04 00 07 F0010203040506 00")).hex())
        assertEquals("6985", e.process(selectCc).hex())
    }

    @Test
    fun fileSelectionNeedsTheApplicationFirst() {
        val e = emulator()
        assertEquals("6985", e.process(selectCc).hex())
        assertEquals("6985", e.process(selectNdef).hex())
    }

    @Test
    fun selectsTheTwoFilesAndNothingElse() {
        val e = emulator().ready()
        assertEquals("9000", e.process(selectCc).hex())
        assertEquals("9000", e.process(selectNdef).hex())
        assertEquals("9000", e.process(hex("00 A4 00 00 02 E104")).hex()) // P2 = 00 is accepted too
        assertEquals("6A82", e.process(hex("00 A4 00 0C 02 E105")).hex())
        assertEquals("6A82", e.process(hex("00 A4 00 0C 02 3F00")).hex())
        assertEquals("6700", e.process(hex("00 A4 00 0C 01 E1")).hex())
        assertEquals("6700", e.process(hex("00 A4 00 0C 03 E10400")).hex())
        assertEquals("6A86", e.process(hex("00 A4 00 04 02 E104")).hex())
        assertEquals("6A86", e.process(hex("00 A4 02 0C 02 E104")).hex())
    }

    // ---------------------------------------------------------- the files --

    @Test
    fun capabilityContainerBytes() {
        val e = emulator().ready()
        e.process(selectCc)
        assertEquals("000F2000FF00FF0406E1040101" + "00FF" + "9000", e.process(read(0, 15)).hex())
        assertEquals(15, Type4Tag.capabilityContainer().size)
        // the file size it advertises really covers the biggest message we ever send
        assertEquals(Type4Tag.NDEF_FILE_MAX, 2 + NdefUri.message("a".repeat(NdefUri.MAX_URI_BYTES))!!.size)
    }

    @Test
    fun ndefLengthBytesAndContent() {
        val e = emulator().ready()
        e.process(selectNdef)
        val nlen = e.process(read(0, 2))
        assertEquals(4, nlen.size)
        assertEquals(message.size, ((nlen[0].toInt() and 0xFF) shl 8) or (nlen[1].toInt() and 0xFF))
        assertEquals("9000", nlen.copyOfRange(2, 4).hex())
        val body = e.process(read(2, message.size))
        assertArrayEquals(message, body.copyOf(body.size - 2))
        assertEquals("9000", body.copyOfRange(body.size - 2, body.size).hex())
    }

    @Test
    fun readsAtOffsetsAndInChunks() {
        val e = emulator().ready()
        e.process(selectNdef)
        val whole = Type4Tag.ndefFile(message)
        // byte by byte
        val assembled = ByteArray(whole.size)
        for (i in whole.indices) {
            val r = e.process(read(i, 1))
            assertEquals(3, r.size)
            assembled[i] = r[0]
        }
        assertArrayEquals(whole, assembled)
        // an offset in the middle, with a length running past the end gives the remainder
        val tail = e.process(read(10, 255))
        assertArrayEquals(whole.copyOfRange(10, whole.size), tail.copyOf(tail.size - 2))
    }

    @Test
    fun largestMessageAndChunkBoundaries() {
        val big = NdefUri.message("a".repeat(NdefUri.MAX_URI_BYTES))!!
        val file = Type4Tag.ndefFile(big)
        assertEquals(257, file.size)
        val e = emulator { big }.ready()
        e.process(selectNdef)

        val first = e.process(read(0, 255))
        assertEquals(255 + 2, first.size)
        val second = e.process(read(255, 255))
        assertEquals(2 + 2, second.size) // only 2 bytes left
        assertArrayEquals(file, first.copyOf(255) + second.copyOf(2))

        // Le = 00 means 256: capped to MLe (255)
        assertEquals(255 + 2, e.process(read(0, 0)).size)
        // exactly at the end: nothing but the status word
        assertEquals("9000", e.process(read(257, 10)).hex())
        // beyond the end
        assertEquals("6B00", e.process(read(258, 10)).hex())
        assertEquals("6B00", e.process(read(0x7FFF, 10)).hex())
    }

    @Test
    fun shortFileIdAddressingIsNotOffered() {
        val e = emulator().ready()
        e.process(selectNdef)
        assertEquals("6B00", e.process(hex("00 B0 84 00 02")).hex())
    }

    @Test
    fun readWithoutAFileOrWithABadShapeIsRefused() {
        val e = emulator().ready()
        assertEquals("6985", e.process(read(0, 2)).hex())
        e.process(selectCc)
        assertEquals("6700", e.process(hex("00 B0 00 00")).hex())
        assertEquals("6700", e.process(hex("00 B0 00 00 02 00")).hex())
    }

    // -------------------------------------------------- read-only and junk --

    @Test
    fun theTagIsReadOnly() {
        val e = emulator().ready()
        e.process(selectNdef)
        assertEquals("6982", e.process(hex("00 D6 00 00 02 0000")).hex())
        assertEquals("6982", e.process(hex("00 D7 00 00 02 0000")).hex())
        // and nothing changed
        assertArrayEquals(message, e.process(read(2, message.size)).let { it.copyOf(it.size - 2) })
    }

    @Test
    fun unknownCommandsAreRefusedWithTheRightWords() {
        val e = emulator().ready()
        assertEquals("6D00", e.process(hex("00 CA 00 00 00")).hex()) // GET DATA
        assertEquals("6D00", e.process(hex("00 88 00 00 00")).hex())
        assertEquals("6E00", e.process(hex("80 A4 04 00 07 D2760000850101")).hex())
        assertEquals("6E00", e.process(hex("FF B0 00 00 02")).hex())
        assertEquals("6700", e.process(ByteArray(0)).hex())
        assertEquals("6700", e.process(hex("00 A4 04")).hex())
    }

    @Test
    fun garbageNeverThrows() {
        val random = java.util.Random(42)
        val e = emulator().ready()
        repeat(2000) {
            val apdu = ByteArray(random.nextInt(24)).also(random::nextBytes)
            val r = e.process(apdu)
            assertTrue(r.size >= 2)
        }
    }

    // ------------------------------------------------ lifecycle and updates --

    @Test
    fun resetForgetsTheSelection() {
        val e = emulator().ready()
        e.process(selectNdef)
        e.reset()
        assertEquals("6985", e.process(read(0, 2)).hex())
        assertEquals("6985", e.process(selectNdef).hex())
        assertEquals("9000", e.process(selectAid).hex())
    }

    @Test
    fun stoppingAShareMidConversationRevealsNothing() {
        var current: ByteArray? = message
        val e = emulator { current }.ready()
        current = null
        assertEquals("6A82", e.process(selectNdef).hex())
        assertEquals("6A82", e.process(selectAid).hex())
    }

    @Test
    fun oneConversationReadsOneConsistentSnapshot() {
        var current: ByteArray? = message
        val e = emulator { current }.ready()
        e.process(selectNdef)
        val second = NdefUri.message("violence://party/ZZZZZZ")!!
        current = second // the shared item changes while the reader is mid-read
        val firstRead = e.process(read(2, message.size))
        assertArrayEquals(message, firstRead.copyOf(firstRead.size - 2))
        // a fresh select picks up the new item
        e.process(selectNdef)
        val nextRead = e.process(read(2, second.size))
        assertArrayEquals(second, nextRead.copyOf(nextRead.size - 2))
    }

    // --------------------------------------------------------- the reader --

    @Test
    fun readerAgainstEmulatorRoundTrip() {
        for (link in listOf(
            "violence://party/AB12CD",
            "violence://song/yt/dQw4w9WgXcQ?v=1&t=Hello&a=World",
            "x".repeat(100),
            "y".repeat(NdefUri.MAX_URI_BYTES),
        )) {
            val msg = NdefUri.message(link)!!
            val emu = emulator { msg }
            val result = Type4TagReader.read { emu.process(it) }
            val ok = result as Type4TagReader.Result.Ok
            assertArrayEquals(msg, ok.message)
            assertEquals(link, NdefUri.parse(ok.message))
        }
    }

    @Test
    fun readerFindsNothingWhenNoShareIsActive() {
        val emu = emulator { null }
        val result = Type4TagReader.read { emu.process(it) }
        assertEquals(Type4TagReader.Result.Failed(Type4TagReader.Failure.NOT_AN_NDEF_TAG), result)
    }

    @Test
    fun readerCommandsAreTheDocumentedBytes() {
        val seen = mutableListOf<String>()
        val emu = emulator()
        Type4TagReader.read { cmd -> seen.add(cmd.hex()); emu.process(cmd) }
        assertEquals(
            listOf(
                "00A4040007D276000085010100",
                "00A4000C02E103",
                "00B000000F",
                "00A4000C02E104",
                "00B0000002",
                "00B00002" + "%02X".format(message.size),
            ),
            seen,
        )
    }

    // A tag we do not control: every length must be checked before it is trusted.
    private fun cc(version: Int = 0x20, mle: Int = 0x00FF, fileId: Int = 0xE104, max: Int = 0x0101, read: Int = 0x00, write: Int = 0xFF) =
        byteArrayOf(
            0x00, 0x0F, version.toByte(), (mle shr 8).toByte(), mle.toByte(), 0x00, 0xFF.toByte(), 0x04, 0x06,
            (fileId shr 8).toByte(), fileId.toByte(), (max shr 8).toByte(), max.toByte(), read.toByte(), write.toByte(),
        )

    /** A scripted tag: select answers by id, reads answer from the file selected last. */
    private fun scripted(cc: ByteArray, file: ByteArray, mle: Int = 255, ok: ByteArray = Type4Tag.SW_OK, overshoot: Boolean = false): Pair<(ByteArray) -> ByteArray, IntArray> {
        var selected = 0
        val requested = intArrayOf(0)
        val f = { cmd: ByteArray ->
            when (cmd[1].toInt() and 0xFF) {
                0xA4 -> {
                    if (cmd[2].toInt() == 0x00) selected = ((cmd[5].toInt() and 0xFF) shl 8) or (cmd[6].toInt() and 0xFF)
                    ok
                }
                0xB0 -> {
                    val offset = ((cmd[2].toInt() and 0xFF) shl 8) or (cmd[3].toInt() and 0xFF)
                    val le = (cmd[4].toInt() and 0xFF).coerceAtMost(mle)
                    requested[0] += le
                    val source = if (selected == 0xE103) cc else file
                    val n = if (overshoot) le + 5 else minOf(le, (source.size - offset).coerceAtLeast(0))
                    ByteArray(n) { source.getOrElse(offset + it) { 0 } } + Type4Tag.SW_OK
                }
                else -> Type4Tag.SW_UNKNOWN
            }
        }
        return f to requested
    }

    private fun ndefFile(len: Int, actual: Int = len) = byteArrayOf((len shr 8).toByte(), len.toByte()) + ByteArray(actual) { 0x41 }

    @Test
    fun readerHonoursASmallMle() {
        val msg = NdefUri.message("y".repeat(100))!!
        val (tag, requested) = scripted(cc(mle = 0x0010), Type4Tag.ndefFile(msg), mle = 16)
        val result = Type4TagReader.read(tag)
        assertArrayEquals(msg, (result as Type4TagReader.Result.Ok).message)
        // it asked for the 15 CC bytes, 2 length bytes, and exactly the message, in pieces of at most 16
        assertEquals(15 + 2 + msg.size, requested[0])
    }

    @Test
    fun readerRefusesBadCapabilityContainers() {
        val msg = NdefUri.message("violence://party/AB12CD")!!
        fun run(cc: ByteArray) = Type4TagReader.read(scripted(cc, Type4Tag.ndefFile(msg)).first)
        assertEquals(Type4TagReader.Result.Failed(Type4TagReader.Failure.BAD_CAPABILITY_CONTAINER), run(cc(version = 0x10)))
        assertEquals(Type4TagReader.Result.Failed(Type4TagReader.Failure.BAD_CAPABILITY_CONTAINER), run(cc().also { it[1] = 0x05 }))
        assertEquals(Type4TagReader.Result.Failed(Type4TagReader.Failure.BAD_CAPABILITY_CONTAINER), run(cc().also { it[7] = 0x05; it[8] = 0x01 }))
        assertEquals(Type4TagReader.Result.Failed(Type4TagReader.Failure.NOT_READABLE), run(cc(read = 0x80)))
    }

    @Test
    fun readerRefusesHugeAndEmptyFiles() {
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.TOO_BIG),
            Type4TagReader.read(scripted(cc(max = 0xFFFF), ndefFile(0xFFFF, 10)).first),
        )
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.TOO_BIG),
            Type4TagReader.read(scripted(cc(), ndefFile(600, 600)).first),
        )
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.TOO_BIG),
            Type4TagReader.read(scripted(cc(max = 0x0010), ndefFile(100, 100)).first),
            // NLEN above what the tag itself advertises as its maximum
        )
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.NOT_AN_NDEF_TAG),
            Type4TagReader.read(scripted(cc(), ndefFile(0, 0)).first),
        )
    }

    @Test
    fun readerNeverReadsBeyondTheDeclaredLength() {
        val (tag, requested) = scripted(cc(max = 0x0200), ndefFile(300, 300))
        Type4TagReader.read(tag)
        assertEquals(15 + 2 + 300, requested[0])
    }

    @Test
    fun readerRefusesTruncatedAndOvershootingAnswers() {
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.TRUNCATED),
            Type4TagReader.read(scripted(cc(), ndefFile(200, 50)).first),
        )
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.BAD_CAPABILITY_CONTAINER),
            Type4TagReader.read(scripted(cc(), ndefFile(20), overshoot = true).first),
        )
    }

    @Test
    fun readerGivesUpOnErrorStatusWords() {
        val deny = byteArrayOf(0x6A, 0x82.toByte())
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.NOT_AN_NDEF_TAG),
            Type4TagReader.read(scripted(cc(), ndefFile(5), ok = deny).first),
        )
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.NOT_AN_NDEF_TAG),
            Type4TagReader.read { ByteArray(0) },
        )
        assertEquals(
            Type4TagReader.Result.Failed(Type4TagReader.Failure.NOT_AN_NDEF_TAG),
            Type4TagReader.read { byteArrayOf(0x90.toByte()) },
        )
    }
}
