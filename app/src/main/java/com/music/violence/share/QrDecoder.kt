package com.music.violence.share

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/**
 * Reading QR codes with ZXing `core` (Apache-2.0), from a camera frame or a picture.
 *
 * Only QR codes are looked for. The result is just the text in the code: it is untrusted and is
 * handed to [IncomingLinkHub], never interpreted here. Pure ZXing, so it runs in unit tests.
 */
object QrDecoder {

    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
    )

    /** Decodes the brightness plane of a camera frame ([rowStride] bytes per row), or null. */
    fun decodeLuminance(data: ByteArray, rowStride: Int, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || rowStride < width || data.size < rowStride * (height - 1) + width) return null
        val source = PlanarYUVLuminanceSource(data, rowStride, height, 0, 0, width, height, false)
        return decode(source)
    }

    /** Decodes a picture given as ARGB pixels, or null. */
    fun decodePixels(pixels: IntArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null
        return decode(RGBLuminanceSource(width, height, pixels))
    }

    private fun decode(source: LuminanceSource): String? {
        for (binarizer in listOf<(LuminanceSource) -> com.google.zxing.Binarizer>(::HybridBinarizer, ::GlobalHistogramBinarizer)) {
            val reader = MultiFormatReader()
            reader.setHints(hints)
            val result = runCatching { reader.decodeWithState(BinaryBitmap(binarizer(source))) }.getOrNull()
            if (result != null) return result.text
        }
        return null
    }
}
