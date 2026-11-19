package com.music.violence.desktop

import com.music.violence.data.model.artworkAt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance decoded artwork cache with two-tier caching:
 * 1. Fast in-memory LRU cache held to an active heap byte budget.
 * 2. Persistent local disk cache in the application state directory, preventing redundant network requests.
 * 3. Concurrent in-flight request deduplication to prevent duplicate HTTP streams when scrolling.
 */
internal object DesktopArtworkCache {

    private val diskCacheDir: Path by lazy {
        val state = System.getenv("XDG_STATE_HOME")
            ?: System.getenv("LOCALAPPDATA")
            ?: (System.getProperty("user.home") + "/.local/state")
        Path.of(state, "violence", "artwork_cache").also {
            runCatching { Files.createDirectories(it) }
        }
    }

    private val inFlight = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    internal val defaultBudgetBytes: Long =
        (Runtime.getRuntime().maxMemory() / 6).coerceIn(32L * 1024 * 1024, 128L * 1024 * 1024)

    internal val budgetBytes: Long
        get() = if (DesktopAppearanceSettings.constrainImageCacheMemory.value) {
            16L * 1024 * 1024
        } else {
            defaultBudgetBytes
        }

    private val images = DesktopByteBudgetLru<ImageBitmap>(budgetBytes) {
        it.width.toLong() * it.height.toLong() * 4L
    }

    private fun digest(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Clears both in-memory LRU and persistent disk cache. */
    fun clear() {
        images.clear()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                Files.list(diskCacheDir).use { stream ->
                    stream.forEach { runCatching { Files.deleteIfExists(it) } }
                }
            }
        }
    }

    suspend fun load(url: String?): ImageBitmap? {
        if (url.isNullOrBlank()) return null
        images.get(url)?.let { return it }

        return withContext(Dispatchers.IO) {
            // Check disk cache first before downloading
            val cacheFile = diskCacheDir.resolve(digest(url) + ".img")
            if (Files.exists(cacheFile)) {
                val diskBitmap = runCatching {
                    val bytes = Files.readAllBytes(cacheFile)
                    bytes.decodeToImageBitmap()
                }.getOrNull()
                if (diskBitmap != null) {
                    images.put(url, diskBitmap)
                    return@withContext diskBitmap
                }
            }

            // In-flight deduplication
            val deferred = CompletableDeferred<ImageBitmap?>()
            val prior = inFlight.putIfAbsent(url, deferred)
            if (prior != null) {
                return@withContext prior.await()
            }

            try {
                val downloadedBitmap = runCatching {
                    val conn = URI(url).toURL().openConnection().apply {
                        connectTimeout = 8_000
                        readTimeout = 10_000
                    }
                    val bytes = conn.getInputStream().use { it.readBytes() }
                    runCatching { Files.write(cacheFile, bytes) }
                    bytes.decodeToImageBitmap()
                }.getOrNull()

                if (downloadedBitmap != null) {
                    images.put(url, downloadedBitmap)
                }
                deferred.complete(downloadedBitmap)
                downloadedBitmap
            } catch (e: Throwable) {
                deferred.complete(null)
                null
            } finally {
                inFlight.remove(url)
            }
        }
    }
}

/** Artwork, fetched at the size the surface drawing it actually needs. */
@Composable
fun DesktopArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    px: Int? = null,
) {
    val constrainMemory = DesktopAppearanceSettings.constrainImageCacheMemory.value
    val effectivePx = if (constrainMemory) (px?.coerceAtMost(256) ?: 128) else px
    val sized = if (effectivePx != null) url.artworkAt(effectivePx) else url
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = sized) {
        value = withContext(Dispatchers.IO) {
            DesktopArtworkCache.load(sized)
        }
    }
    if (bitmap == null) {
        Box(
            modifier
                .background(Color.White.copy(alpha = 0.06f))
        )
    } else {
        Image(
            painter = BitmapPainter(bitmap!!),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
