package com.music.violence.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDropHandlerTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("violence_drop_test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
        DesktopDropHandler.onSongsDropped = null
    }

    @Test
    fun testDropHandlerState() {
        assertFalse(DesktopDropHandler.isDragging.value)
    }

    @Test
    fun testDropAudioFileExtensions() {
        val flac = File(tempDir, "Artist - Track.flac").apply { writeText("dummy audio") }
        val mp3 = File(tempDir, "Another - Song.mp3").apply { writeText("dummy audio") }
        val txt = File(tempDir, "readme.txt").apply { writeText("not audio") }

        assertTrue(flac.extension.lowercase() in setOf("flac", "mp3", "m4a", "wav", "opus", "ogg", "aac"))
        assertTrue(mp3.extension.lowercase() in setOf("flac", "mp3", "m4a", "wav", "opus", "ogg", "aac"))
        assertFalse(txt.extension.lowercase() in setOf("flac", "mp3", "m4a", "wav", "opus", "ogg", "aac"))
    }
}

