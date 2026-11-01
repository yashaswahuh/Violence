package com.music.bitchord.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NdefUriTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun buildsOneShortWellKnownUriRecord() {
        val uri = "violence://party/AB12CD"
        val message = NdefUri.message(uri)!!
        assertEquals(5 + uri.length, message.size)
        // flags MB|ME|SR|TNF=1, type length 1, payload length 1 + uri, type 'U', no-abbreviation prefix
        assertArrayEquals(bytes(0xD1, 0x01, 1 + uri.length, 0x55, 0x00), message.copyOf(5))
        assertArrayEquals(uri.encodeToByteArray(), message.copyOfRange(5, message.size))
    }

    @Test
    fun refusesEmptyAndOversizedUris() {
        assertNull(NdefUri.message(""))
        assertNotNull(NdefUri.message("a".repeat(NdefUri.MAX_URI_BYTES)))
        assertNull(NdefUri.message("a".repeat(NdefUri.MAX_URI_BYTES + 1)))
        // the limit is in bytes, not characters
        assertNull(NdefUri.message("é".repeat(NdefUri.MAX_URI_BYTES / 2 + 1)))
        assertNotNull(NdefUri.message("é".repeat(NdefUri.MAX_URI_BYTES / 2)))
    }

    @Test
    fun roundTrips() {
        for (uri in listOf(
            "violence://party/AB12CD",
            "violence://song/yt/dQw4w9WgXcQ?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley",
            "violence://profile?v=1&id=123e4567-e89b-42d3-a456-426614174000&n=Alex&a=guitar&c=0a84ff",
            "x".repeat(NdefUri.MAX_URI_BYTES),
        )) {
            assertEquals(uri, NdefUri.parse(NdefUri.message(uri)!!))
        }
    }

    @Test
    fun readsAnHttpsPrefixedRecord() {
        val payload = "yashaswahuh.is-a.dev/Violence/party/AB12CD".encodeToByteArray()
        val message = bytes(0xD1, 0x01, 1 + payload.size, 0x55, 0x04) + payload
        assertEquals("https://yashaswahuh.is-a.dev/Violence/party/AB12CD", NdefUri.parse(message))
    }

    @Test
    fun readsALongRecordFormat() {
        val payload = bytes(0x00) + "violence://party/AB12CD".encodeToByteArray()
        // MB|ME, SR clear: 4-byte payload length
        val message = bytes(0xC1, 0x01, 0, 0, 0, payload.size, 0x55) + payload
        assertEquals("violence://party/AB12CD", NdefUri.parse(message))
    }

    @Test
    fun refusesWhatIsNotExactlyOneUriRecord() {
        val good = NdefUri.message("violence://party/AB12CD")!!
        fun with(index: Int, value: Int) = good.copyOf().also { it[index] = value.toByte() }

        assertNull("first of several", NdefUri.parse(with(0, 0x91))) // ME cleared
        assertNull("not the first record", NdefUri.parse(with(0, 0x51))) // MB cleared
        assertNull("chunked", NdefUri.parse(with(0, 0xF1))) // CF set
        assertNull("not well-known (MIME)", NdefUri.parse(with(0, 0xD2)))
        assertNull("not well-known (absolute URI)", NdefUri.parse(with(0, 0xD3)))
        assertNull("type length 2", NdefUri.parse(with(1, 2)))
        assertNull("type T, not U", NdefUri.parse(with(3, 0x54)))
        assertNull("payload length too small", NdefUri.parse(with(2, 3)))
        assertNull("payload length too big", NdefUri.parse(with(2, good.size)))
        assertNull("empty payload", NdefUri.parse(bytes(0xD1, 0x01, 0x00, 0x55)))
        assertNull("trailing byte", NdefUri.parse(good + bytes(0x00)))
        assertNull("truncated", NdefUri.parse(good.copyOf(good.size - 1)))
        assertNull("too short", NdefUri.parse(bytes(0xD1, 0x01, 0x01)))
        assertNull("empty", NdefUri.parse(ByteArray(0)))
        assertNull("unsupported prefix code 0x01", NdefUri.parse(with(4, 0x01)))
        assertNull("unsupported prefix code 0x23", NdefUri.parse(with(4, 0x23)))
    }

    @Test
    fun refusesHugeAndInvalidPayloads() {
        // long record claiming 4 GB
        assertNull(NdefUri.parse(bytes(0xC1, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0x55, 0x00, 0x41)))
        // claiming more than the reader will take
        assertNull(NdefUri.parse(bytes(0xC1, 0x01, 0x00, 0x00, 0x10, 0x00, 0x55, 0x00, 0x41)))
        // invalid UTF-8 in the URI
        assertNull(NdefUri.parse(bytes(0xD1, 0x01, 0x03, 0x55, 0x00, 0xFF, 0xFE)))
        // a record with an ID field is read past, not rejected
        val withId = bytes(0xD9, 0x01, 0x03, 0x01, 0x55, 0x49, 0x00, 0x41, 0x42)
        assertEquals("AB", NdefUri.parse(withId))
    }
}
