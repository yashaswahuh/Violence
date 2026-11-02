package com.music.bitchord.ui.brutal.safemode

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.settings.AppSettings

/**
 * The Android side of [BrutalSafeModeLogic]: its own small preferences file (not the settings file,
 * so a settings problem cannot hide a failed start), written with `commit()` because the marker
 * must be on disk before a crash that could follow it.
 */
object BrutalSafeMode {
    private const val FILE = "violence_brutal_boot"
    private lateinit var logic: BrutalSafeModeLogic
    private var begun = false

    private class PrefsStore(private val prefs: SharedPreferences) : SafeModeStore {
        override var startMarker: Boolean
            get() = prefs.getBoolean("marker", false)
            set(v) { prefs.edit().putBoolean("marker", v).commit() }
        override var failedStarts: Int
            get() = prefs.getInt("failed", 0)
            set(v) { prefs.edit().putInt("failed", v).commit() }
        override var revertNotice: Boolean
            get() = prefs.getBoolean("notice", false)
            set(v) { prefs.edit().putBoolean("notice", v).commit() }
    }

    /**
     * Call once from `MainActivity.onCreate`, after `AppSettings` is read. If the new UI has failed
     * two starts in a row this persists the current UI as the setting and queues the notice.
     */
    fun startupCheck(context: Context) {
        if (!::logic.isInitialized) {
            logic = BrutalSafeModeLogic(
                PrefsStore(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)),
            )
        }
        val wanted = AppSettings.uiVariant.value
        val run = logic.startupCheck(wanted)
        if (run != wanted) AppSettings.setUiVariant(run)
    }

    /** The new UI is composing for the first time this process (idempotent). */
    fun begin() {
        if (!::logic.isInitialized || begun) return
        begun = true
        logic.begin()
    }

    /** The new UI's first screen has drawn. */
    fun shown() {
        if (!::logic.isInitialized) return
        logic.shown()
    }

    /** True once after a revert. */
    fun consumeRevertNotice(): Boolean = ::logic.isInitialized && logic.consumeRevertNotice()
}
