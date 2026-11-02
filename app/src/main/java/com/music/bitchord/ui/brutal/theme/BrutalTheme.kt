package com.music.bitchord.ui.brutal.theme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.glass.GlassBudget
import com.music.bitchord.ui.brutal.glass.GlassInputs
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.brutal.glass.LocalGlassBudget
import com.music.bitchord.ui.brutal.glass.LocalGlassLevel
import com.music.bitchord.ui.brutal.glass.decideGlassLevel
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled

/**
 * The brutal theme: palette, type, motion and glass level, with colour and type reaching the
 * existing pages through [MaterialTheme].
 *
 * It is called from the same place with the same content whatever [enabled] is, so that switching
 * the variant changes *values* and never the shape of the composition (the media controller and
 * every page stay where they are). With [enabled] false it re-provides the current theme's own
 * values, unchanged, and registers no receivers or observers.
 *
 * [dark] is accepted for the day a light variant exists; the first version is dark only (the
 * mockups have no light theme), so a light theme choice is ignored while the new UI is on.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun BrutalTheme(enabled: Boolean, dark: Boolean, content: @Composable () -> Unit) {
    val accent by AppSettings.brutalAccent.collectAsStateWithLifecycle()
    val glow by AppSettings.brutalGlow.collectAsStateWithLifecycle()
    val palette = remember(accent, glow) { BrutalPalette.of(accent, glow) }
    val reducedMotion = if (enabled) rememberReducedMotion().value else false
    val level = if (enabled) rememberGlassLevel(reducedMotion) else GlassLevel.SOLID
    val budget = remember { GlassBudget() }
    val profile = ScriptProfile.forLanguage(LocalConfiguration.current.locales.get(0)?.language)
    val typeSet = remember(profile) { BrutalTypeSet(profile) }
    val current = MaterialTheme.colorScheme
    val scheme = if (enabled) brutalColorScheme(palette) else current
    // The existing live-glass machinery (page backdrop recording, glass top controls) runs only at
    // FULL; below that the old pieces fall back to their non-glass materials.
    val glassOn = if (enabled) level == GlassLevel.FULL else LocalLiquidGlassEnabled.current
    CompositionLocalProvider(
        LocalBrutalPalette provides palette,
        LocalBrutalMotion provides BrutalMotion(reducedMotion),
        LocalBrutalType provides typeSet,
        LocalGlassLevel provides level,
        LocalGlassBudget provides budget,
        LocalLiquidGlassEnabled provides glassOn,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = if (enabled) typeSet.material else MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}

/**
 * The palette as a Material colour scheme, so existing pages wear it (spec 3.3): the page is the
 * black token, `primary` is the accent fill with its own ink-or-white on top, `secondary` is the
 * acid cyan, and the grouped surfaces are the measured glass body.
 */
fun brutalColorScheme(p: BrutalPalette): ColorScheme = darkColorScheme(
    primary = p.accentFill,
    onPrimary = p.onAccent,
    secondary = p.acid,
    onSecondary = p.ink,
    background = p.page,
    onBackground = p.white,
    surface = Color(0xFF111113),
    onSurface = p.white,
    surfaceVariant = p.glassBody,
    onSurfaceVariant = p.textMuted,
    outline = p.glassBodyBright,
    outlineVariant = p.hairline,
)

/** The app's "Reduce animation", or the system's animator scale at zero ("Remove animations"). */
@Composable
fun rememberReducedMotion(): State<Boolean> {
    val context = LocalContext.current
    val appSetting by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val system = remember { mutableStateOf(systemAnimationsOff(context)) }
    DisposableEffect(context) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                system.value = systemAnimationsOff(context)
            }
        }
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return remember(appSetting) {
        object : State<Boolean> {
            override val value: Boolean get() = appSetting || system.value
        }
    }
}

private fun systemAnimationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Reads the device and the settings and applies [decideGlassLevel]; updates when either changes. */
@Composable
fun rememberGlassLevel(reducedMotion: Boolean): GlassLevel {
    val context = LocalContext.current
    val reduceTransparency by AppSettings.reduceTransparency.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val power = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val batterySaver = remember { mutableStateOf(power.isPowerSaveMode) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                batterySaver.value = power.isPowerSaveMode
            }
        }
        // A protected system broadcast; the flag keeps Android 14's receiver rule satisfied anyway.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    val lowRam = remember {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
    }
    return decideGlassLevel(
        GlassInputs(
            sdkInt = Build.VERSION.SDK_INT,
            lowRamDevice = lowRam,
            batterySaver = batterySaver.value,
            reduceTransparency = reduceTransparency,
            reduceMotion = reducedMotion,
            reduceDynamicBlur = reduceDynamicBlur,
        ),
    )
}
