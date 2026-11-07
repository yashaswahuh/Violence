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
    private const val KEY_POS_X = "pip_mini_player_x"
    private const val KEY_POS_Y = "pip_mini_player_y"
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

    fun getInitialPosition(width: Int = 340, height: Int = 96): java.awt.Point {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return java.awt.Point(100, 100)
        }
        return runCatching {
            val savedX = persistence.int(KEY_POS_X, Int.MIN_VALUE)
            val savedY = persistence.int(KEY_POS_Y, Int.MIN_VALUE)

            val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            val devices = ge.screenDevices

            if (savedX != Int.MIN_VALUE && savedY != Int.MIN_VALUE && devices.isNotEmpty()) {
                val windowRect = java.awt.Rectangle(savedX, savedY, width, height)
                val intersectsAny = devices.any { it.defaultConfiguration.bounds.intersects(windowRect) }
                if (intersectsAny) {
                    return@runCatching clampToScreen(savedX, savedY, width, height)
                }
            }

            // Default: bottom-right corner of primary screen above taskbar
            val gc = ge.defaultScreenDevice.defaultConfiguration
            val bounds = gc.bounds
            val insets = runCatching { java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc) }.getOrNull()
                ?: java.awt.Insets(0, 0, 0, 0)

            val usableX = bounds.x + insets.left
            val usableY = bounds.y + insets.top
            val usableWidth = bounds.width - insets.left - insets.right
            val usableHeight = bounds.height - insets.top - insets.bottom

            val margin = 24
            val defaultX = (usableX + usableWidth - width - margin).coerceAtLeast(usableX)
            val defaultY = (usableY + usableHeight - height - margin).coerceAtLeast(usableY)

            java.awt.Point(defaultX, defaultY)
        }.getOrDefault(java.awt.Point(100, 100))
    }

    fun clampToScreen(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        pointer: java.awt.Point? = null,
    ): java.awt.Point {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return java.awt.Point(x, y)
        }
        return runCatching {
            val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            val devices = ge.screenDevices
            if (devices.isEmpty()) return@runCatching java.awt.Point(x, y)

            val pointerDevice = if (pointer != null) {
                devices.firstOrNull { it.defaultConfiguration.bounds.contains(pointer) }
            } else null

            val windowRect = java.awt.Rectangle(x, y, width.coerceAtLeast(1), height.coerceAtLeast(1))
            val intersectDevice = devices.firstOrNull {
                it.defaultConfiguration.bounds.intersects(windowRect)
            }

            val targetDevice = pointerDevice ?: intersectDevice ?: ge.defaultScreenDevice
            val gc = targetDevice.defaultConfiguration
            val bounds = gc.bounds
            val insets = runCatching { java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc) }.getOrNull()
                ?: java.awt.Insets(0, 0, 0, 0)

            val usableX = bounds.x + insets.left
            val usableY = bounds.y + insets.top
            val usableWidth = (bounds.width - insets.left - insets.right).coerceAtLeast(width)
            val usableHeight = (bounds.height - insets.top - insets.bottom).coerceAtLeast(height)

            val minX = usableX
            val maxX = usableX + usableWidth - width
            val minY = usableY
            val maxY = usableY + usableHeight - height

            val clampedX = if (maxX >= minX) x.coerceIn(minX, maxX) else minX
            val clampedY = if (maxY >= minY) y.coerceIn(minY, maxY) else minY

            java.awt.Point(clampedX, clampedY)
        }.getOrDefault(java.awt.Point(x, y))
    }

    fun savePosition(x: Int, y: Int) {
        persistence.saveInt(KEY_POS_X, x)
        persistence.saveInt(KEY_POS_Y, y)
    }
}
