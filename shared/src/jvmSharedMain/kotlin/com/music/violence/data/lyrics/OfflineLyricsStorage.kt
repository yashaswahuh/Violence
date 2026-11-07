package com.music.violence.data.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Storage and serialization engine for offline cached synced lyrics (.lyrics.json).
 *
 * Saves per-track lyrics alongside downloaded media files and in a dedicated app-private
 * cache directory. Preserves full timing fidelity: line timestamps, word timings, syllable
 * slices, line duration (sungUntilMs), duet alignment, and background/answering vocals.
 *
 * When querying lyrics, [LyricsRepository] inspects this storage before reaching out over
 * the network, giving instant zero-latency lyrics playback even completely offline.
 */
object OfflineLyricsStorage {

    @Volatile
    var baseDirectory: File? = null

    private val searchDirectories = CopyOnWriteArraySet<File>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    /** Register an additional search directory (e.g. downloaded music folder). */
    fun registerDirectory(dir: File?) {
        if (dir != null) {
            searchDirectories.add(dir)
        }
    }

    /** Unregister a search directory. */
    fun unregisterDirectory(dir: File?) {
        if (dir != null) {
            searchDirectories.remove(dir)
        }
    }

    /** Serializes a list of [LyricLine] to a compact JSON string. */
    fun toJson(lines: List<LyricLine>, source: String? = null): String {
        val payload = OfflineLyricsPayload(
            version = 1,
            source = source,
            lines = lines.map { it.toPayload() },
        )
        return json.encodeToString(OfflineLyricsPayload.serializer(), payload)
    }

    /** Deserializes a JSON string into a list of [LyricLine], or returns null if malformed. */
    fun fromJson(rawJson: String): List<LyricLine>? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val payload = json.decodeFromString(OfflineLyricsPayload.serializer(), rawJson)
            payload.lines.map { it.toLyricLine() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Saves [lines] for [videoId] as `<videoId>.lyrics.json`.
     *
     * If [destinationFile] is specified, writes to that exact file.
     * Also writes to [baseDirectory]/`<videoId>.lyrics.json` if [baseDirectory] is set.
     */
    fun save(
        videoId: String,
        lines: List<LyricLine>,
        destinationFile: File? = null,
        source: String? = null,
    ): Boolean {
        if (videoId.isBlank() || lines.isEmpty()) return false
        val serialized = toJson(lines, source)

        var savedAny = false
        if (destinationFile != null) {
            if (writeFileSafely(destinationFile, serialized)) {
                savedAny = true
            }
        }

        val baseDir = baseDirectory
        if (baseDir != null) {
            if (!baseDir.exists()) baseDir.mkdirs()
            val primary = File(baseDir, "$videoId.lyrics.json")
            if (writeFileSafely(primary, serialized)) {
                savedAny = true
            }
        }

        return savedAny
    }

    /**
     * Reads offline cached lyrics for [videoId].
     * Checks [baseDirectory] first, then all registered [searchDirectories].
     */
    fun read(videoId: String): List<LyricLine>? {
        if (videoId.isBlank()) return null

        val fileName = "$videoId.lyrics.json"

        // 1. Check baseDirectory
        baseDirectory?.let { base ->
            val file = File(base, fileName)
            if (file.exists() && file.isFile && file.length() > 0) {
                readFromFile(file)?.let { return it }
            }
        }

        // 2. Check registered directories
        for (dir in searchDirectories) {
            if (!dir.exists() || !dir.isDirectory) continue
            val file = File(dir, fileName)
            if (file.exists() && file.isFile && file.length() > 0) {
                readFromFile(file)?.let { return it }
            }
        }

        return null
    }

    /** Reads and decodes lyrics directly from [file]. */
    fun readFromFile(file: File?): List<LyricLine>? {
        if (file == null || !file.exists() || !file.isFile || file.length() == 0L) return null
        return runCatching {
            file.readText(StandardCharsets.UTF_8).let(::fromJson)
        }.getOrNull()
    }

    /** Removes cached lyrics for [videoId] from [baseDirectory] and [searchDirectories]. */
    fun remove(videoId: String): Boolean {
        if (videoId.isBlank()) return false
        val fileName = "$videoId.lyrics.json"
        var deleted = false

        baseDirectory?.let { base ->
            val file = File(base, fileName)
            if (file.exists() && file.delete()) deleted = true
        }

        for (dir in searchDirectories) {
            val file = File(dir, fileName)
            if (file.exists() && file.delete()) deleted = true
        }

        return deleted
    }

    private fun writeFileSafely(target: File, content: String): Boolean {
        return runCatching {
            val parent = target.parentFile
            if (parent != null && !parent.exists()) parent.mkdirs()
            val temp = File(parent ?: target, ".${target.name}.part")
            FileOutputStream(temp).use { out ->
                out.write(content.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            }
            if (target.exists()) target.delete()
            temp.renameTo(target)
        }.getOrDefault(false)
    }
}

@Serializable
internal data class OfflineLyricsPayload(
    val version: Int = 1,
    val source: String? = null,
    val lines: List<OfflineLyricLinePayload> = emptyList(),
)

@Serializable
internal data class OfflineLyricLinePayload(
    val timeMs: Long,
    val text: String,
    val words: List<OfflineLyricWordPayload> = emptyList(),
    val sungUntilMs: Long? = null,
    val background: OfflineLyricLinePayload? = null,
    val alignment: String = "Start",
)

@Serializable
internal data class OfflineLyricWordPayload(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<OfflineLyricSyllablePayload> = emptyList(),
)

@Serializable
internal data class OfflineLyricSyllablePayload(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
)

private fun LyricLine.toPayload(): OfflineLyricLinePayload = OfflineLyricLinePayload(
    timeMs = timeMs,
    text = text,
    words = words.map { word ->
        OfflineLyricWordPayload(
            startMs = word.startMs,
            endMs = word.endMs,
            text = word.text,
            syllables = word.syllables.map { syllable ->
                OfflineLyricSyllablePayload(
                    startMs = syllable.startMs,
                    endMs = syllable.endMs,
                    startChar = syllable.startChar,
                    endChar = syllable.endChar,
                )
            },
        )
    },
    sungUntilMs = sungUntilMs,
    background = background?.toPayload(),
    alignment = alignment.name,
)

private fun OfflineLyricLinePayload.toLyricLine(): LyricLine = LyricLine(
    timeMs = timeMs,
    text = text,
    words = words.map { word ->
        LyricWord(
            startMs = word.startMs,
            endMs = word.endMs,
            text = word.text,
            syllables = word.syllables.map { syllable ->
                LyricSyllable(
                    startMs = syllable.startMs,
                    endMs = syllable.endMs,
                    startChar = syllable.startChar,
                    endChar = syllable.endChar,
                )
            },
        )
    },
    sungUntilMs = sungUntilMs,
    background = background?.toLyricLine(),
    alignment = if (alignment.equals("End", ignoreCase = true)) LyricAlignment.End else LyricAlignment.Start,
)

