package com.music.violence.desktop

import com.music.violence.data.lyrics.LyricLine
import com.music.violence.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State manager and coordinator for the Desktop Floating PiP Mini-Player.
 *
 * Drives window visibility transitions between full-window and floating mini-player mode,
 * synchronizes live track metadata, active word-synced lyrics line, playback state, and
 * dispatches user actions (play, pause, next, previous, seek) across both views.
 */
internal object DesktopMiniPlayerManager {

    private const val KEY_MINI_MODE = "pip_mini_player_mode"
    private val persistence by lazy { DesktopPersistence() }

    private val _isMiniMode = MutableStateFlow(false)

    /** Whether the desktop mini-player window is currently active and floating. */
    val isMiniMode: StateFlow<Boolean> = _isMiniMode.asStateFlow()

    val currentSong = MutableStateFlow<Song?>(null)
    val isPlaying = MutableStateFlow(false)
    val positionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    val activeLyricText = MutableStateFlow<String?>(null)

    @Volatile
    private var cachedLyricLines: List<LyricLine> = emptyList()

    var onPlayPause: () -> Unit = {}
    var onNext: () -> Unit = {}
    var onPrevious: () -> Unit = {}
    var onSeek: (Long) -> Unit = {}

    fun toggle() = setMiniMode(!_isMiniMode.value)

    fun enterMiniMode() = setMiniMode(true)

    fun exitMiniMode() = setMiniMode(false)

    fun setMiniMode(enabled: Boolean) {
        if (_isMiniMode.value == enabled) return
        _isMiniMode.value = enabled
        persistence.saveBoolean(KEY_MINI_MODE, enabled)
        if (!enabled) {
            DesktopWindowVisibility.show()
        }
    }

    fun updatePlayback(song: Song?, playing: Boolean, pos: Long, dur: Long) {
        currentSong.value = song
        isPlaying.value = playing
        positionMs.value = pos
        durationMs.value = dur
        updateActiveLyric(pos)
    }

    fun setLyrics(lines: List<LyricLine>, currentPosMs: Long) {
        cachedLyricLines = lines
        updateActiveLyric(currentPosMs)
    }

    private fun updateActiveLyric(posMs: Long) {
        val lines = cachedLyricLines
        if (lines.isEmpty()) {
            activeLyricText.value = null
            return
        }
        val line = lines.lastOrNull { it.timeMs <= posMs && !it.isGap }
        activeLyricText.value = line?.text?.takeIf { it.isNotBlank() }
    }
}
