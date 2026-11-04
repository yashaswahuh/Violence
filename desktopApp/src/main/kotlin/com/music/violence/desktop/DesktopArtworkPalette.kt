package com.music.violence.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.music.violence.ui.graphics.ColorUtils
import com.music.violence.ui.graphics.paletteSwatches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

data class DesktopArtworkPalette(
    val primary: Color,
    val secondary: Color,
) {
    companion object {
        val Default = DesktopArtworkPalette(Color(0xFF3F7BFF), Color(0xFF4E6DFF))
    }
}

@Composable
fun rememberDesktopArtworkPalette(url: String?): DesktopArtworkPalette {
    var palette by remember { mutableStateOf(DesktopArtworkPalette.Default) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            val bitmap = DesktopArtworkCache.load(url)
            bitmap?.let { DesktopArtworkPalette.from(it) }
        }
        if (loaded != null) {
            palette = loaded
        }
    }
    return palette
}

private fun DesktopArtworkPalette.Companion.from(bitmap: ImageBitmap?): DesktopArtworkPalette {
    if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return DesktopArtworkPalette.Default
    return runCatching {
        val all = paletteSwatches(bitmap, maxColors = 24, clearFilters = true)
        if (all.isEmpty()) return DesktopArtworkPalette.Default
        val accentCandidates = paletteSwatches(bitmap, maxColors = 24, clearFilters = false).ifEmpty { all }
        val vibrant = accentCandidates.maxByOrNull { swatch ->
            val hsl = FloatArray(3).also { ColorUtils.colorToHSL(swatch.rgb, it) }
            hsl[1] * sqrt(swatch.population.toFloat())
        } ?: all.first()

        val vibrantColor = Color(vibrant.rgb)
        DesktopArtworkPalette(
            primary = vibrantColor.copy(alpha = 1f),
            secondary = Color(
                red = (vibrantColor.red * 0.55f + 0.22f).coerceAtMost(1f),
                green = (vibrantColor.green * 0.45f + 0.18f).coerceAtMost(1f),
                blue = (vibrantColor.blue * 0.70f + 0.24f).coerceAtMost(1f),
            ),
        )
    }.getOrDefault(DesktopArtworkPalette.Default)
}
