package com.music.bitchord.ui.violencestyle

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppFont
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.theme.BitChordTheme
import com.music.bitchord.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/** The current song's artwork, reported by the app root so the accent can follow it. */
object ViolenceStyleSong {
    val artworkUrl = MutableStateFlow<String?>(null)

    /** True while the full player covers the page (the background clock stops). */
    val playerOpen = MutableStateFlow(false)

    /** Plain value writes; no-ops when unchanged. Safe to call with the style off. */
    fun report(url: String?, playerOpen: Boolean = false) {
        artworkUrl.value = url
        this.playerOpen.value = playerOpen
    }
}

/** The derived accent colours. Default is the Blue preset, never read while the style is off. */
val LocalViolenceAccent = compositionLocalOf { AccentTokens.DEFAULT }

/** Reads the system signals that decide LITE mode and reduced motion. */
internal object StyleSignals {
    fun isLowRam(context: Context): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

    fun isBatterySaver(context: Context): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true

    /** Thermal status SEVERE (3) or worse, API 29+. */
    fun isThermalSevere(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val status = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.currentThermalStatus ?: return false
        return status >= PowerManager.THERMAL_STATUS_SEVERE
    }

    fun animatorScale(context: Context): Float = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    } catch (_: Exception) {
        1f
    }

    /**
     * System high-contrast text. UNCLEAR: this key is not in the public SDK (the constant is hidden),
     * so it may be absent on some devices; a missing value reads as off.
     */
    fun isHighContrast(context: Context): Boolean = try {
        Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
    } catch (_: Exception) {
        false
    }

    /** The system accent on Android 12 and later; null below that. */
    fun deviceAccent(context: Context): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_500)
        } else {
            null
        }
}

/**
 * Drop-in replacement for the `BitChordTheme(...)` call in MainActivity. With the style OFF it is
 * exactly `BitChordTheme(darkTheme, font) { content() }`: no provider, no typography change, no
 * extra state. With it ON it applies the Inter typography and provides the style state, the accent
 * tokens and the heading styles to everything below.
 */
@Composable
fun ViolenceStyledTheme(
    darkTheme: Boolean,
    font: AppFont,
    content: @Composable () -> Unit,
) {
    val enabled by ViolenceStyleSettings.enabled.collectAsStateWithLifecycle()
    BitChordTheme(darkTheme = darkTheme, font = font) {
        // The app's whole UI sits in this slot. Turning the style on or off moves it between the plain
        // path and the host; without a movable slot Compose would treat that as a new subtree and drop every
        // remember (open pages, scroll positions, dialogs), so the owner would lose their place on a toggle.
        val current by rememberUpdatedState(content)
        val slot = remember { movableContentOf { current() } }
        if (enabled) {
            ViolenceStyleHost { slot() }
        } else {
            slot()
        }
    }
}

@Composable
private fun ViolenceStyleHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    ViolenceTypographyScope(enabled = true) {
        val background by ViolenceStyleSettings.background.collectAsStateWithLifecycle()
        val motion by ViolenceStyleSettings.motion.collectAsStateWithLifecycle()
        val intensity by ViolenceStyleSettings.intensity.collectAsStateWithLifecycle()
        val source by ViolenceStyleSettings.accentSource.collectAsStateWithLifecycle()
        val custom by ViolenceStyleSettings.customColor.collectAsStateWithLifecycle()
        val headingFont by ViolenceStyleSettings.headingFont.collectAsStateWithLifecycle()
        val forcedTransparency by ViolenceStyleSettings.reduceTransparency.collectAsStateWithLifecycle()
        val detailGlow by ViolenceStyleSettings.detailGlow.collectAsStateWithLifecycle()
        val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
        val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

        val signalVersion = rememberSignalVersion(context)
        val sdk = Build.VERSION.SDK_INT
        val state = remember(
            background, motion, intensity, source, custom, headingFont, forcedTransparency,
            detailGlow, reduceDynamicBlur, reduceAnimation, signalVersion,
        ) {
            val lite = StyleCapabilityRules.liteReason(
                sdkInt = sdk,
                lowRamDevice = StyleSignals.isLowRam(context),
                batterySaver = StyleSignals.isBatterySaver(context),
                thermalSevere = StyleSignals.isThermalSevere(context),
                reduceDynamicBlur = reduceDynamicBlur,
                userReducedTransparency = forcedTransparency,
            )
            StyleState(
                enabled = true,
                background = background,
                motion = motion,
                intensity = intensity,
                accentSource = source,
                customColor = custom,
                headingFont = headingFont,
                // System high-contrast text also asks for solid surfaces (S6 accessibility fix).
                reduceTransparency = StyleCapabilityRules.reduceTransparency(forcedTransparency, reduceDynamicBlur) ||
                    StyleSignals.isHighContrast(context),
                detailGlow = detailGlow,
                capability = StyleCapabilityRules.capability(lite),
                liteReason = lite,
                reduceMotion = reduceAnimation || StyleCapabilityRules.systemReducesMotion(StyleSignals.animatorScale(context)),
                highContrast = StyleSignals.isHighContrast(context),
            )
        }

        val tokens = rememberAccentTokens(state)
        val headings = remember(headingFont) { ViolenceHeadingStyles.build(headingFont.family()) }
        val shared = remember(tokens, headings, state.intensity) {
            ViolenceSharedStyle(
                accent = Color(tokens.accent),
                accentSoft = Color(tokens.accentSoft),
                textAccent = Color(tokens.textAccent),
                neutral = tokens.neutral,
                strength = state.intensity.peakAlpha,
                displayCaps = headings.displayCaps,
                displayItalic = headings.displayItalic,
                sectionCaps = headings.sectionCaps,
                cardTitleCaps = headings.cardTitleCaps,
                label = headings.label,
            )
        }
        val backgroundLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
        val backgroundHolder = remember(backgroundLayer) { ViolenceBackgroundHolder(backgroundLayer) }

        // The start succeeded once this has been composed and one frame has been drawn.
        LaunchedEffect(Unit) {
            androidx.compose.runtime.withFrameNanos { }
            ViolenceStyleBoot.firstFrameDisplayed(context)
        }

        CompositionLocalProvider(
            LocalViolenceStyle provides state,
            LocalViolenceAccent provides tokens,
            LocalViolenceHeadings provides headings,
            LocalViolenceBackground provides backgroundHolder,
            LocalViolenceSeeThrough provides state.showsBackground(),
            LocalViolenceShared provides shared,
            content = content,
        )
    }
}

/** Bumped when battery saver changes or the app returns to the foreground, so the signals are re-read. */
@Composable
private fun rememberSignalVersion(context: Context): Int {
    var version by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                version++
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) version++
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return version
}

/**
 * Resolves the accent and animates it: about 800 ms FastOutSlowIn, a cut under reduce motion. The
 * song colour is debounced (400 ms) so fast skipping does not flicker, and a song with dull or
 * missing artwork keeps the previous accent.
 */
@Composable
private fun rememberAccentTokens(state: StyleState): AccentTokens {
    val context = LocalContext.current
    val songUrl by ViolenceStyleSong.artworkUrl.collectAsStateWithLifecycle()
    var stableUrl by remember { mutableStateOf(songUrl) }
    LaunchedEffect(songUrl) {
        if (songUrl != stableUrl) {
            delay(SONG_DEBOUNCE_MS)
            stableUrl = songUrl
        }
    }

    val fromSong = state.accentSource == StyleAccentSource.FROM_SONG
    val palette = rememberArtworkPalette(imageUrl = if (fromSong) stableUrl else null)
    // With no artwork the palette hands back the theme's own primary: that is "no colour".
    val scheme = MaterialTheme.colorScheme
    // No artwork at all means no change; a song WITH artwork always yields a colour, even gray or white.
    val songColor: Int? = if (!fromSong || stableUrl == null) null else palette.accent.toArgb()
    // The palette hands back the theme's primary (white) while it is still reading the picture, and for
    // white artwork; only after a short wait is that taken as the artwork's own (neutral) colour.
    val stillLoading = palette.accent == scheme.primary

    var held by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(songColor, stillLoading) {
        if (songColor == null) return@LaunchedEffect
        if (stillLoading) delay(NEUTRAL_SETTLE_MS)
        val colour = if (AccentMath.isDull(songColor)) AccentPreset.WHITE.argb else songColor
        if (AccentEngine.shouldReplace(held, colour)) held = colour
    }

    val resolved = AccentEngine.resolve(
        source = state.accentSource,
        songColor = held,
        deviceAccent = StyleSignals.deviceAccent(context),
        custom = state.customColor,
    )
    val animated by animateColorAsState(
        targetValue = Color(resolved.argb),
        animationSpec = if (state.reduceMotion) snap() else tween(ACCENT_FADE_MS, easing = FastOutSlowInEasing),
        label = "violenceStyleAccent",
    )
    return remember(animated) { AccentTokens.from(animated.toArgb()) }
}

private const val SONG_DEBOUNCE_MS = 400L
private const val NEUTRAL_SETTLE_MS = 700L
private const val ACCENT_FADE_MS = 800
