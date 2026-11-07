package com.music.violence.data.storage

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageHealthTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("violence_storage_test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", StorageBreakdown.formatBytes(0L))
        assertEquals("500 B", StorageBreakdown.formatBytes(500L))
        assertEquals("1.0 KB", StorageBreakdown.formatBytes(1024L))
        assertEquals("2.5 MB", StorageBreakdown.formatBytes((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.2 GB", StorageBreakdown.formatBytes((1.2 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testCalculateBreakdown() {
        val musicDir = File(tempDir, "Music")
        musicDir.mkdirs()
        val imgDir = File(tempDir, "Images")
        imgDir.mkdirs()

        // Create sample music files
        val song1 = File(musicDir, "Artist - Song1.flac")
        song1.writeBytes(ByteArray(10000))
        val song2 = File(musicDir, "Artist - Song2.m4a")
        song2.writeBytes(ByteArray(20000))

        // Create a lyrics file
        val lyrics = File(musicDir, "song1.lyrics.json")
        lyrics.writeBytes(ByteArray(500))

        // Create an image cache file
        val img = File(imgDir, "cover.jpg")
        img.writeBytes(ByteArray(5000))

        val breakdown = StorageHealthHub.calculateBreakdown(
            downloadsDir = musicDir,
            imageDir = imgDir,
            streamCacheBytes = 50000L,
        )

        assertEquals(2, breakdown.downloadedTracksCount)
        assertEquals(30000L, breakdown.downloadedTracksBytes)
        assertEquals(1, breakdown.lyricsCacheCount)
        assertEquals(500L, breakdown.lyricsCacheBytes)
        assertEquals(5000L, breakdown.imageCacheBytes)
        assertEquals(50000L, breakdown.streamCacheBytes)
        assertEquals(30000L + 500L + 5000L + 50000L, breakdown.totalBytes)
    }

    @Test
    fun testVerifyAndRepairLyrics() = runBlocking {
        val musicDir = File(tempDir, "Music")
        musicDir.mkdirs()

        val song1 = File(musicDir, "The Beatles - Yesterday.opus")
        song1.writeBytes(ByteArray(5000))

        val (total, repaired) = StorageHealthHub.verifyAndRepairLyrics(musicDir) { videoId, artist, title ->
            assertEquals("The Beatles", artist)
            assertEquals("Yesterday", title)
            true
        }

        assertEquals(1, total)
        assertEquals(1, repaired)
    }
}

