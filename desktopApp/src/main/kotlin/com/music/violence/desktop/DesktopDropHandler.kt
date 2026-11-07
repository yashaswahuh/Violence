package com.music.violence.desktop

import com.music.violence.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Locale
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/**
 * Handles drag-and-drop import of local audio files and directories into Violence Desktop.
 *
 * Automatically inspects dropped files and folders (up to 4 levels deep), filters for
 * supported audio containers (.flac, .mp3, .m4a, .wav, .opus, .ogg, .aac), builds
 * valid [Song] entries, and dispatches them to the player queue.
 */
internal object DesktopDropHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "flac", "mp3", "m4a", "wav", "opus", "ogg", "aac", "webm",
    )

    private val _isDragging = MutableStateFlow(false)

    /** Whether files are currently hovering over the window. */
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    @Volatile
    var onSongsDropped: ((List<Song>) -> Unit)? = null

    fun attach(component: Component) {
        val target = DropTarget(component, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
            override fun dragEnter(dtde: DropTargetDragEvent) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    _isDragging.value = true
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                } else {
                    dtde.rejectDrag()
                }
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                } else {
                    dtde.rejectDrag()
                }
            }

            override fun dragExit(dte: DropTargetEvent) {
                _isDragging.value = false
            }

            override fun drop(dtde: DropTargetDropEvent) {
                _isDragging.value = false
                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY)
                        val transferable = dtde.transferable
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                        if (!files.isNullOrEmpty()) {
                            processFiles(files)
                            dtde.dropComplete(true)
                            return
                        }
                    }
                    dtde.dropComplete(false)
                } catch (e: Exception) {
                    DesktopTrackLog.log("DesktopDropHandler: drop error: ${e.message}")
                    dtde.dropComplete(false)
                }
            }
        })
        component.dropTarget = target
    }

    private fun processFiles(files: List<File>) {
        scope.launch {
            val audioFiles = mutableListOf<Path>()
            for (file in files) {
                val path = file.toPath()
                if (Files.isDirectory(path)) {
                    runCatching {
                        Files.walk(path, 4).use { stream ->
                            stream.filter(Files::isRegularFile)
                                .filter { it.extension.lowercase(Locale.ROOT) in SUPPORTED_AUDIO_EXTENSIONS }
                                .forEach { audioFiles.add(it) }
                        }
                    }
                } else if (Files.isRegularFile(path)) {
                    if (path.extension.lowercase(Locale.ROOT) in SUPPORTED_AUDIO_EXTENSIONS) {
                        audioFiles.add(path)
                    }
                }
            }

            if (audioFiles.isEmpty()) return@launch

            val songs = audioFiles.distinctBy { it.toAbsolutePath().normalize() }
                .map { it.toDroppedSong() }
                .sortedBy { it.title.lowercase(Locale.ROOT) }

            DesktopTrackLog.log("DesktopDropHandler: imported ${songs.size} dropped track(s)")
            onSongsDropped?.invoke(songs)
        }
    }

    private fun Path.toDroppedSong(): Song {
        val absolutePath = toAbsolutePath().normalize()
        val identity = absolutePath.toString()
        val parsed = parseFileName(nameWithoutExtension)
        val codec = extension.lowercase(Locale.ROOT).let { value ->
            when (value) {
                "flac" -> "FLAC"
                "wav" -> "WAV"
                "aac", "m4a" -> "AAC"
                "ogg" -> "OGG"
                "webm" -> "OPUS"
                else -> value.uppercase(Locale.ROOT)
            }
        }
        return Song(
            videoId = "local:$identity",
            title = parsed.title,
            artist = parsed.artist,
            thumbnailUrl = null,
            localUri = absolutePath.toUri().toString(),
            localPath = absolutePath.toString(),
            localDateAddedSeconds = Instant.now().epochSecond,
            sourceQuality = codec,
        )
    }

    private fun parseFileName(name: String): ParsedName {
        val parts = name.split(" - ", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            ParsedName(title = parts[1].trim(), artist = parts[0].trim())
        } else {
            ParsedName(title = name, artist = "On This Computer")
        }
    }

    private data class ParsedName(val title: String, val artist: String)
}
