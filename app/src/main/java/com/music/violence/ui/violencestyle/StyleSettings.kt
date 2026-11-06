package com.music.violence.ui.violencestyle

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.music.violence.R
import kotlinx.coroutines.flow.MutableStateFlow

/** Pure parsing of stored values: an unknown or missing value falls back to the default. */
object StylePrefsParse {
    inline fun <reified T : Enum<T>> enumOr(raw: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: default
}

/**
 * Violence Style settings. Stored in the app's existing settings file (`bitchord_settings`, the
 * same one [com.music.violence.data.settings.AppSettings] uses) under keys that all start with
 * `violence_style.`. Nothing here is read unless [init] has run; the master toggle defaults OFF.
 */
object ViolenceStyleSettings {
    private const val PREFS_NAME = "bitchord_settings"
    private const val P = "violence_style."
    internal const val KEY_ENABLED = P + "enabled"
    private const val KEY_BACKGROUND = P + "background"
    private const val KEY_MOTION = P + "motion"
    private const val KEY_INTENSITY = P + "intensity"
    private const val KEY_ACCENT_SOURCE = P + "accent_source"
    private const val KEY_CUSTOM_COLOR = P + "custom_color"
    private const val KEY_HEADING_FONT = P + "heading_font"
    private const val KEY_REDUCE_TRANSPARENCY = P + "reduce_transparency"
    private const val KEY_DETAIL_GLOW = P + "detail_glow"
    private const val KEY_ACCOUNT_LIQUID_GLASS = P + "account_liquid_glass"
    private const val KEY_FULL_COVER = P + "full_cover_pages"
    private const val KEY_AUTOMIX_COVER = P + "automix_cover_animation"
    internal const val KEY_PENDING = P + "startup_pending"
    internal const val KEY_NOTICE = P + "safe_mode_notice"

    private var prefs: SharedPreferences? = null

    val enabled = MutableStateFlow(false)
    val background = MutableStateFlow(StyleDefaults.BACKGROUND)
    val motion = MutableStateFlow(StyleDefaults.MOTION)
    val intensity = MutableStateFlow(StyleDefaults.INTENSITY)
    val accentSource = MutableStateFlow(StyleDefaults.ACCENT_SOURCE)
    val customColor = MutableStateFlow(StyleDefaults.CUSTOM_COLOR)
    val headingFont = MutableStateFlow(StyleDefaults.HEADING_FONT)

    /** Solid fills even when Reduce dynamic blur is off (it always follows that setting too). */
    val reduceTransparency = MutableStateFlow(false)
    val detailGlow = MutableStateFlow(StyleDefaults.DETAIL_GLOW)

    /** The account menu drawn as Liquid Glass (the bars' lens and blur). Off by default. */
    val accountLiquidGlass = MutableStateFlow(false)

    /** Album and playlist pages open with the cover filling the top of the page, like the player. Off by default. */
    val fullCoverPages = MutableStateFlow(false)

    /** The player's cover art glides from one song to the next through an Automix blend. Off by default. */
    val automixCoverAnimation = MutableStateFlow(false)

    /** True after safe mode switched the style off; cleared when the user turns it on again. */
    val safeModeTriggered = MutableStateFlow(false)

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        enabled.value = p.getBoolean(KEY_ENABLED, false)
        background.value = StylePrefsParse.enumOr(p.getString(KEY_BACKGROUND, null), StyleDefaults.BACKGROUND)
        motion.value = StylePrefsParse.enumOr(p.getString(KEY_MOTION, null), StyleDefaults.MOTION)
        intensity.value = StylePrefsParse.enumOr(p.getString(KEY_INTENSITY, null), StyleDefaults.INTENSITY)
        accentSource.value = StylePrefsParse.enumOr(p.getString(KEY_ACCENT_SOURCE, null), StyleDefaults.ACCENT_SOURCE)
        customColor.value = p.getInt(KEY_CUSTOM_COLOR, StyleDefaults.CUSTOM_COLOR)
        headingFont.value = StylePrefsParse.enumOr(p.getString(KEY_HEADING_FONT, null), StyleDefaults.HEADING_FONT)
        reduceTransparency.value = p.getBoolean(KEY_REDUCE_TRANSPARENCY, false)
        detailGlow.value = p.getBoolean(KEY_DETAIL_GLOW, StyleDefaults.DETAIL_GLOW)
        accountLiquidGlass.value = p.getBoolean(KEY_ACCOUNT_LIQUID_GLASS, false)
        fullCoverPages.value = p.getBoolean(KEY_FULL_COVER, false)
        automixCoverAnimation.value = p.getBoolean(KEY_AUTOMIX_COVER, false)
        safeModeTriggered.value = p.getBoolean(KEY_NOTICE, false)
    }

    fun setEnabled(value: Boolean) {
        enabled.value = value
        val e = prefs?.edit()?.putBoolean(KEY_ENABLED, value) ?: return
        if (value) {
            // Turning it on again after a safe-mode shutdown: start counting from zero.
            safeModeTriggered.value = false
            e.putInt(KEY_PENDING, 0).putBoolean(KEY_NOTICE, false)
        }
        e.apply()
    }

    fun setBackground(value: StyleBackground) { background.value = value; put(KEY_BACKGROUND, value.name) }
    fun setMotion(value: StyleMotion) { motion.value = value; put(KEY_MOTION, value.name) }
    fun setIntensity(value: StyleIntensity) { intensity.value = value; put(KEY_INTENSITY, value.name) }
    fun setAccentSource(value: StyleAccentSource) { accentSource.value = value; put(KEY_ACCENT_SOURCE, value.name) }
    fun setHeadingFont(value: StyleHeadingFont) { headingFont.value = value; put(KEY_HEADING_FONT, value.name) }

    fun setCustomColor(value: Int) {
        customColor.value = value
        prefs?.edit()?.putInt(KEY_CUSTOM_COLOR, value)?.apply()
    }

    fun setReduceTransparency(value: Boolean) {
        reduceTransparency.value = value
        prefs?.edit()?.putBoolean(KEY_REDUCE_TRANSPARENCY, value)?.apply()
    }

    fun setDetailGlow(value: Boolean) {
        detailGlow.value = value
        prefs?.edit()?.putBoolean(KEY_DETAIL_GLOW, value)?.apply()
    }

    fun setAccountLiquidGlass(value: Boolean) {
        accountLiquidGlass.value = value
        prefs?.edit()?.putBoolean(KEY_ACCOUNT_LIQUID_GLASS, value)?.apply()
    }

    fun setFullCoverPages(value: Boolean) {
        fullCoverPages.value = value
        prefs?.edit()?.putBoolean(KEY_FULL_COVER, value)?.apply()
    }

    fun setAutomixCoverAnimation(value: Boolean) {
        automixCoverAnimation.value = value
        prefs?.edit()?.putBoolean(KEY_AUTOMIX_COVER, value)?.apply()
    }

    private fun put(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    /**
     * The theme override for dark-only handling: while the style is on the app resolves to its dark
     * theme (owner decision: a light version comes later). Null while off, so the caller's own
     * theme choice is used unchanged.
     */
    fun forceDark(styleEnabled: Boolean): Boolean? = if (styleEnabled) true else null
}

/** Startup hooks for safe mode. Called from MainActivity (two lines). */
object ViolenceStyleBoot {

    /**
     * Loads the settings and applies safe mode. Must run before any style code is composed.
     * Uses commit(), not apply(): a write that is still queued when the process dies is lost, and
     * the whole point of the counter is to survive a crash.
     */
    fun start(context: Context) {
        ViolenceStyleSettings.init(context)
        val prefs = context.applicationContext.getSharedPreferences("bitchord_settings", Context.MODE_PRIVATE)
        val decision = StyleSafeModeLogic.onStart(
            pending = prefs.getInt(ViolenceStyleSettings.KEY_PENDING, 0),
            styleEnabled = ViolenceStyleSettings.enabled.value,
        )
        val editor = prefs.edit().putInt(ViolenceStyleSettings.KEY_PENDING, decision.pending)
        if (decision.disableStyle) {
            editor.putBoolean(ViolenceStyleSettings.KEY_ENABLED, false)
                .putBoolean(ViolenceStyleSettings.KEY_NOTICE, true)
        }
        editor.commit()
        if (decision.disableStyle) {
            ViolenceStyleSettings.enabled.value = false
            ViolenceStyleSettings.safeModeTriggered.value = true
        }
        if (decision.showNotice) {
            Toast.makeText(context, R.string.vs_safe_mode_notice, Toast.LENGTH_LONG).show()
        }
    }

    /** The first screen has been drawn: this start succeeded. */
    fun firstFrameDisplayed(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences("bitchord_settings", Context.MODE_PRIVATE)
        if (prefs.getInt(ViolenceStyleSettings.KEY_PENDING, 0) != StyleSafeModeLogic.onFirstFrame()) {
            prefs.edit().putInt(ViolenceStyleSettings.KEY_PENDING, StyleSafeModeLogic.onFirstFrame()).apply()
        }
    }
}
