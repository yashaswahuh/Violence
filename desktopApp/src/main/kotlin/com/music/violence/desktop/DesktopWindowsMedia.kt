package com.music.violence.desktop

import com.sun.jna.WString
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.WinReg
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Windows' System Media Transport Controls — the volume flyout's media card,
 * the lock screen, and the media keys.
 *
 * The Windows counterpart of [DesktopMprisController], and the same contract:
 * the player hands it state, it hands back button presses. What it talks to is
 * WinRT, which the JVM cannot reach, so the work happens in `violence_smtc.dll`
 * beside the analyser.
 *
 * Absent, everything here is a no-op. The library is only built by MSVC, so a
 * Linux cross-build of the Windows archive has no SMTC in it — the installers,
 * which CI builds on Windows, do.
 */
internal object DesktopWindowsMedia {

    private val started = AtomicBoolean(false)

    private val shutdownHookAdded = AtomicBoolean(false)

    @Volatile
    private var controller: Controller? = null

    /** What a media key does, filled in by the player. */
    internal class Controller(
        val onPlay: () -> Unit,
        val onPause: () -> Unit,
        val onNext: () -> Unit,
        val onPrevious: () -> Unit,
        val onStop: () -> Unit,
        /** Moves playback to a position the shell's scrubber was dragged to, in milliseconds. */
        val onSeek: (Long) -> Unit,
    )

    private val available: Boolean by lazy {
        if (!DesktopPlatform.isWindows) return@lazy false
        runCatching { DesktopAnalysisRuntime.loadNative(LIBRARY) }
            .onFailure { DesktopTrackLog.log("Windows media controls unavailable: ${it.message}") }
            .isSuccess
    }

    /**
     * Initializes the explicit AppUserModelID ("Violence") for this process and registers
     * fallback registry keys in HKCU so Windows SMTC, the volume flyout, and Action Center
     * identify the application as "Violence" with its icon rather than "Unknown app".
     */
    fun initAppUserModelId() {
        if (!DesktopPlatform.isWindows) return
        runCatching {
            Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(WString("Violence"))
        }.onFailure {
            DesktopTrackLog.log("Failed to set explicit AppUserModelID: ${it.message}")
        }
        runCatching {
            val userClassesKey = "Software\\Classes\\AppUserModelId\\Violence"
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, userClassesKey)
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, userClassesKey, "DisplayName", "Violence")
            val exeCandidate = System.getProperty("compose.application.resources.dir")?.let {
                File(it).parentFile?.resolve("Violence.exe")?.takeIf { f -> f.exists() }?.absolutePath
            } ?: System.getProperty("java.home")?.let {
                File(it).resolve("bin").resolve("Violence.exe").takeIf { f -> f.exists() }?.absolutePath
            }
            if (!exeCandidate.isNullOrBlank()) {
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, userClassesKey, "IconUri", "$exeCandidate,0")
            }
        }.onFailure {
            DesktopTrackLog.log("Failed to register AppUserModelID registry keys: ${it.message}")
        }
    }

    fun start(controller: Controller) {
        // Held whether or not the library is there, so what a button does is
        // decided in one place and can be checked without Windows.
        this.controller = controller
        initAppUserModelId()
        if (!available || !started.compareAndSet(false, true)) return
        if (!nativeStart()) {
            started.set(false)
            DesktopTrackLog.log("Windows media controls could not be created")
            return
        }
        // Quitting from the tray is exitProcess, which never reaches the window's onDispose. The
        // library's pump thread was then still running when the DLL unloaded, and destroying a
        // joinable std::thread aborts the process — every quit ended in a native crash.
        if (shutdownHookAdded.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(Thread(::stop, "violence-smtc-stop"))
        }
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        controller = null
        runCatching { nativeStop() }
    }

    /** Publishes the current track. Called on every change the shell would show. */
    fun publish(state: DesktopPlaybackState) {
        if (!started.get()) return
        val song = state.song
        runCatching {
            nativeUpdate(
                song?.title.orEmpty(),
                song?.artist.orEmpty(),
                song?.albumName.orEmpty(),
                song?.thumbnailUrl.orEmpty(),
                state.isPlaying,
                state.positionMs,
                state.durationMs,
            )
        }
    }

    /** Called from the native side when a media key or the shell's card is used. */
    @JvmStatic
    fun onButton(button: Int) {
        val target = controller ?: return
        when (button) {
            BUTTON_PLAY -> target.onPlay()
            BUTTON_PAUSE -> target.onPause()
            BUTTON_NEXT -> target.onNext()
            BUTTON_PREVIOUS -> target.onPrevious()
            BUTTON_STOP -> target.onStop()
        }
    }

    /** Called from the native side when the shell's scrubber is dragged to a new position. */
    @JvmStatic
    fun onSeek(positionMs: Long) {
        controller?.onSeek(positionMs)
    }

    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeUpdate(
        title: String,
        artist: String,
        album: String,
        artUrl: String,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
    )

    private const val LIBRARY = "violence_smtc"

    private const val BUTTON_PLAY = 0
    private const val BUTTON_PAUSE = 1
    private const val BUTTON_NEXT = 2
    private const val BUTTON_PREVIOUS = 3
    private const val BUTTON_STOP = 4
}
