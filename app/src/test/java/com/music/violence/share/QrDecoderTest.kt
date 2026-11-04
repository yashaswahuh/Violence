package com.music.violence.share

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.music.violence.data.share.IncomingLink
import com.music.violence.data.share.IncomingLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrDecoderTest {

    private fun matrix(content: String): BitMatrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 4),
    )

    /** What a camera hands over: one brightness byte per pixel, rows padded to [stride]. */
    private fun frame(m: BitMatrix, scale: Int, stride: Int): Triple<ByteArray, Int, Int> {
        val w = m.width * scale
        val h = m.height * scale
        val data = ByteArray(stride * h) { 0xFF.toByte() }
        for (y in 0 until h) for (x in 0 until w) {
            if (m[x / scale, y / scale]) data[y * stride + x] = 0x10
        }
        return Triple(data, w, h)
    }

    private val links = listOf(
        "https://yashaswahuh.is-a.dev/Violence/song/yt/dQw4w9WgXcQ?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley",
        "https://yashaswahuh.is-a.dev/Violence/profile?v=1&id=123e4567-e89b-42d3-a456-426614174000&n=Alex&a=guitar&c=0a84ff",
        "https://yashaswahuh.is-a.dev/Violence/party/AB12CD",
        "violence://party/AB12CD",
    )

    @Test
    fun decodesCameraFramesWithPaddedRows() {
        for (link in links) {
            val (data, w, h) = frame(matrix(link), scale = 4, stride = matrix(link).width * 4 + 9)
            assertEquals(link, QrDecoder.decodeLuminance(data, matrix(link).width * 4 + 9, w, h))
        }
    }

    @Test
    fun decodesPicturePixels() {
        val m = matrix(links[0])
        val scale = 5
        val w = m.width * scale
        val h = m.height * scale
        val pixels = IntArray(w * h) { i -> if (m[(i % w) / scale, (i / w) / scale]) 0xFF101010.toInt() else 0xFFFFFFFF.toInt() }
        assertEquals(links[0], QrDecoder.decodePixels(pixels, w, h))
    }

    @Test
    fun decodesARotatedCode() {
        val m = matrix(links[2])
        val rotated = BitMatrix(m.height, m.width)
        for (x in 0 until m.width) for (y in 0 until m.height) if (m[x, y]) rotated.set(m.height - 1 - y, x)
        val (data, w, h) = frame(rotated, 4, rotated.width * 4)
        assertEquals(links[2], QrDecoder.decodeLuminance(data, rotated.width * 4, w, h))
    }

    @Test
    fun blankAndNoisyFramesDecodeToNothing() {
        assertNull(QrDecoder.decodeLuminance(ByteArray(200 * 200) { 0xFF.toByte() }, 200, 200, 200))
        val random = java.util.Random(7)
        assertNull(QrDecoder.decodeLuminance(ByteArray(200 * 200).also(random::nextBytes), 200, 200, 200))
        assertNull(QrDecoder.decodePixels(IntArray(100 * 100) { 0xFF808080.toInt() }, 100, 100))
    }

    @Test
    fun badDimensionsDoNotThrow() {
        assertNull(QrDecoder.decodeLuminance(ByteArray(10), 10, 20, 20))
        assertNull(QrDecoder.decodeLuminance(ByteArray(100), 5, 10, 10))
        assertNull(QrDecoder.decodeLuminance(ByteArray(100), 10, 0, 10))
        assertNull(QrDecoder.decodePixels(IntArray(4), 10, 10))
    }

    @Test
    fun aScannedViolenceCodeIsAValidIncomingLink() {
        for (link in links) {
            val (data, w, h) = frame(matrix(link), 4, matrix(link).width * 4)
            val text = QrDecoder.decodeLuminance(data, matrix(link).width * 4, w, h)!!
            assertTrue(IncomingLinks.parse(text) !is IncomingLink.Invalid)
        }
    }

    @Test
    fun aQrCodeForSomethingElseIsReadButNotAViolenceLink() {
        val m = matrix("https://example.org/not-ours")
        val (data, w, h) = frame(m, 4, m.width * 4)
        val text = QrDecoder.decodeLuminance(data, m.width * 4, w, h)
        assertEquals("https://example.org/not-ours", text)
        assertTrue(IncomingLinks.parse(text) is IncomingLink.Invalid)
    }
}
