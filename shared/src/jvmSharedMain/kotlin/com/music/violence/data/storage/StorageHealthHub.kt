package com.music.violence.data.storage

import com.music.violence.data.lyrics.OfflineLyricsStorage
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * Breakdown of storage usage across permanent downloads, lyrics, images, and audio stream cache.
 */
data class StorageBreakdown(
    val downloadedTracksBytes: Long = 0L,
    val downloadedTracksCount: Int = 0,
    val lyricsCacheBytes: Long = 0L,
    val lyricsCacheCount: Int = 0,
    val imageCacheBytes: Long = 0L,
    val streamCacheBytes: Long = 0L,
) {
    val totalBytes: Long get() = downloadedTracksBytes + lyricsCacheBytes + imageCacheBytes + streamCacheBytes

    fun formattedTotal(): String = formatBytes(totalBytes)
    fun formattedDownloaded(): String = formatBytes(downloadedTracksBytes)
    fun formattedLyrics(): String = formatBytes(lyricsCacheBytes)
    fun formattedImages(): String = formatBytes(imageCacheBytes)
    fun formattedStream(): String = formatBytes(streamCacheBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / 1024.0.pow(digitGroups.toDouble())
            return if (digitGroups == 0) "$bytes B" else "%.1f %s".format(Locale.US, value, units[digitGroups])
        }
    }
}

/**
 * Diagnostic and management hub for local storage health across Android and Desktop.
 */
object StorageHealthHub {

    private val AUDIO_EXTENSIONS = setOf("m4a", "flac", "opus", "ogg", "mp3", "wav")

    /**
     * Calculates storage breakdown across downloads, lyrics, artwork images, and stream cache.
     */
    fun calculateBreakdown(
        downloadsDir: File?,
        imageDir: File? = null,
        streamCacheBytes: Long = 0L,
    ): StorageBreakdown {
        var downloadedBytes = 0L
        var downloadedCount = 0

        val lyricsFiles = LinkedHashSet<File>()

        if (downloadsDir != null && downloadsDir.exists() && downloadsDir.isDirectory) {
            downloadsDir.walkTopDown().maxDepth(3).forEach { file ->
                if (file.isFile) {
                    val ext = file.extension.lowercase(Locale.ROOT)
                    if (file.name.endsWith(".lyrics.json")) {
                        lyricsFiles.add(file)
                    } else if (ext in AUDIO_EXTENSIONS && !file.name.endsWith(".part")) {
                        downloadedBytes += file.length()
                        downloadedCount++
                    }
                }
            }
        }

        // Also inspect OfflineLyricsStorage.baseDirectory
        OfflineLyricsStorage.baseDirectory?.let { baseDir ->
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.walkTopDown().maxDepth(2).forEach { file ->
                    if (file.isFile && file.name.endsWith(".lyrics.json")) {
                        lyricsFiles.add(file)
                    }
                }
            }
        }

        val lyricsBytes = lyricsFiles.sumOf { it.length() }
        val lyricsCount = lyricsFiles.size

        var imageBytes = 0L
        if (imageDir != null && imageDir.exists() && imageDir.isDirectory) {
            imageDir.walkTopDown().maxDepth(3).forEach { file ->
                if (file.isFile) {
                    imageBytes += file.length()
                }
            }
        }

        return StorageBreakdown(
            downloadedTracksBytes = downloadedBytes,
            downloadedTracksCount = downloadedCount,
            lyricsCacheBytes = lyricsBytes,
            lyricsCacheCount = lyricsCount,
            imageCacheBytes = imageBytes,
            streamCacheBytes = streamCacheBytes,
        )
    }

    /**
     * Scans [downloadsDir] to identify all downloaded songs and check for missing `.lyrics.json` sidecars.
     * Invokes [fetchLyrics] for tracks lacking offline lyrics and saves them.
     * Returns Pair(totalTracksScanned, repairedCount).
     */
    suspend fun verifyAndRepairLyrics(
        downloadsDir: File?,
        fetchLyrics: suspend (videoId: String, artist: String, title: String) -> Boolean,
    ): Pair<Int, Int> {
        if (downloadsDir == null || !downloadsDir.exists() || !downloadsDir.isDirectory) {
            return 0 to 0
        }

        val audioFiles = downloadsDir.walkTopDown().maxDepth(2).filter { file ->
            file.isFile && file.extension.lowercase(Locale.ROOT) in AUDIO_EXTENSIONS && !file.name.endsWith(".part")
        }.toList()

        var repaired = 0

        for (file in audioFiles) {
            val nameWithoutExt = file.nameWithoutExtension
            // Check if .lyrics.json exists alongside or in storage
            val sidecar = File(file.parentFile ?: downloadsDir, "$nameWithoutExt.lyrics.json")
            val cachedInStorage = OfflineLyricsStorage.read(nameWithoutExt) != null

            if (!sidecar.exists() && !cachedInStorage) {
                // Parse artist and title: "Artist - Title"
                val parts = nameWithoutExt.split(" - ", limit = 2)
                val artist = if (parts.size == 2) parts[0].trim() else ""
                val title = if (parts.size == 2) parts[1].trim() else nameWithoutExt.trim()

                val success = runCatching {
                    fetchLyrics(nameWithoutExt, artist, title)
                }.getOrDefault(false)

                if (success) {
                    repaired++
                }
            }
        }

        return audioFiles.size to repaired
    }
}

