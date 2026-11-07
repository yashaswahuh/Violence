package com.music.violence.data.lyrics

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineLyricsStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testBaseDir: File
    private lateinit var testSearchDir: File

    @Before
    fun setUp() {
        testBaseDir = tempFolder.newFolder("base_lyrics")
        testSearchDir = tempFolder.newFolder("search_lyrics")
        OfflineLyricsStorage.baseDirectory = testBaseDir
        OfflineLyricsStorage.registerDirectory(testSearchDir)
    }

    @After
    fun tearDown() {
        OfflineLyricsStorage.baseDirectory = null
        OfflineLyricsStorage.unregisterDirectory(testSearchDir)
    }

    private val sampleLines = listOf(
        LyricLine(
            timeMs = 1200L,
            text = "Hello world",
            words = listOf(
                LyricWord(
                    startMs = 1200L,
                    endMs = 1600L,
                    text = "Hello",
                    syllables = listOf(
                        LyricSyllable(startMs = 1200L, endMs = 1400L, startChar = 0, endChar = 2),
                        LyricSyllable(startMs = 1400L, endMs = 1600L, startChar = 2, endChar = 5),
                    ),
                ),
                LyricWord(
                    startMs = 1700L,
                    endMs = 2100L,
                    text = "world",
                ),
            ),
            sungUntilMs = 2200L,
            background = LyricLine(
                timeMs = 1500L,
                text = "(world)",
                words = emptyList(),
            ),
            alignment = LyricAlignment.Start,
        ),
        LyricLine(
            timeMs = 3000L,
            text = "Second line duet",
            alignment = LyricAlignment.End,
        ),
    )

    @Test
    fun testJsonRoundTripPreservesAllFields() {
        val json = OfflineLyricsStorage.toJson(sampleLines, source = "TestProvider")
        assertTrue(json.contains("Hello world"))
        assertTrue(json.contains("TestProvider"))

        val restored = OfflineLyricsStorage.fromJson(json)
        assertNotNull(restored)
        assertEquals(sampleLines.size, restored.size)

        val first = restored[0]
        assertEquals(1200L, first.timeMs)
        assertEquals("Hello world", first.text)
        assertEquals(2200L, first.sungUntilMs)
        assertEquals(LyricAlignment.Start, first.alignment)
        assertEquals(2, first.words.size)

        assertEquals("Hello", first.words[0].text)
        assertEquals(2, first.words[0].syllables.size)
        assertEquals(0, first.words[0].syllables[0].startChar)
        assertEquals(2, first.words[0].syllables[0].endChar)

        assertNotNull(first.background)
        assertEquals(1500L, first.background?.timeMs)
        assertEquals("(world)", first.background?.text)

        val second = restored[1]
        assertEquals(3000L, second.timeMs)
        assertEquals("Second line duet", second.text)
        assertEquals(LyricAlignment.End, second.alignment)
    }

    @Test
    fun testSaveAndReadFromBaseDirectory() {
        val videoId = "test_vid_123"
        val saved = OfflineLyricsStorage.save(videoId, sampleLines)
        assertTrue(saved)

        val readBack = OfflineLyricsStorage.read(videoId)
        assertNotNull(readBack)
        assertEquals(sampleLines.size, readBack.size)
        assertEquals("Hello world", readBack[0].text)

        // Verify file exists on disk
        val file = File(testBaseDir, "$videoId.lyrics.json")
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        // Remove and verify
        val removed = OfflineLyricsStorage.remove(videoId)
        assertTrue(removed)
        assertNull(OfflineLyricsStorage.read(videoId))
    }

    @Test
    fun testSaveAndReadFromSearchDirectory() {
        val videoId = "search_vid_456"
        val destination = File(testSearchDir, "$videoId.lyrics.json")
        val saved = OfflineLyricsStorage.save(videoId, sampleLines, destinationFile = destination)
        assertTrue(saved)

        // Clear base directory to ensure reading comes from search directory
        OfflineLyricsStorage.baseDirectory = null

        val readBack = OfflineLyricsStorage.read(videoId)
        assertNotNull(readBack)
        assertEquals("Hello world", readBack[0].text)
    }

    @Test
    fun testMissingAndCorruptFileHandling() {
        assertNull(OfflineLyricsStorage.read("non_existent_id"))

        val corruptFile = File(testBaseDir, "corrupt_vid.lyrics.json")
        corruptFile.writeText("{ invalid json ")
        assertNull(OfflineLyricsStorage.read("corrupt_vid"))
    }

    @Test
    fun testLyricsRepositoryPrioritizesOfflineCache() = runBlocking {
        val videoId = "repo_offline_hit"
        OfflineLyricsStorage.save(videoId, sampleLines)

        // Request lyrics from repository
        val result = LyricsRepository.lyrics(
            videoId = videoId,
            title = "Unknown",
            artist = "Unknown",
            durationMs = 180_000L,
            sources = setOf(LyricsSource.LRCLIB, LyricsSource.LOCAL_CACHE),
        )

        assertNotNull(result)
        assertEquals(LyricsSource.LOCAL_CACHE, result.source)
        assertEquals(sampleLines.size, result.lines.size)
        assertEquals("Hello world", result.lines[0].text)
    }
}

