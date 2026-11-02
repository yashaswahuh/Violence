package com.music.bitchord.ui.v2.theme

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.v2.glass.GlassBudget
import com.music.bitchord.ui.v2.glass.GlassInputs
import com.music.bitchord.ui.v2.glass.GlassLevel
import com.music.bitchord.ui.v2.glass.LocalGlassBudget
import com.music.bitchord.ui.v2.glass.LocalGlassLevel
import com.music.bitchord.ui.v2.glass.decideGlassLevel

/**
 * The new UI's theme: Violence palette and type through [MaterialTheme] (so the existing pages
 * wear them too), the v2 tokens, the motion rules and the decided [GlassLevel].
 *
 * It is always called from the same place with the same content, whatever [enabled] is, so that
 * switching the variant changes values and never the shape of the composition (the media
 * controller and every page stay where they are). With [enabled] false it re-provides the current
 * theme's own values, unchanged.
 */
@Composable
fun V2Theme(enabled: Boolean, dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) V2Colors.Dark else V2Colors.Light
    val reducedMotion = if (enabled) rememberReducedMotion().value else false
    val level = if (enabled) rememberGlassLevel(reducedMotion) else GlassLevel.SOLID
    val budget = remember { GlassBudget() }
    val current = MaterialTheme.colorScheme
    val scheme = when {
        !enabled -> current
        dark -> darkColorScheme(
            primary = colors.voltageText, onPrimary = colors.background,
            background = colors.background, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink,
            surfaceVariant = colors.surface, onSurfaceVariant = colors.ink2,
            outline = colors.outline, error = colors.ember,
        )
        else -> lightColorScheme(
            primary = colors.voltageText, onPrimary = colors.surface,
            background = colors.background, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink,
            surfaceVariant = colors.glassTint, onSurfaceVariant = colors.ink2,
            outline = colors.outline, error = colors.ember,
        )
    }
    val glassOn = if (enabled) level == GlassLevel.FULL else LocalLiquidGlassEnabled.current
    CompositionLocalProvider(
        LocalV2Colors provides colors,
        LocalV2Motion provides V2Motion(reducedMotion),
        LocalGlassLevel provides level,
        LocalGlassBudget provides budget,
        // The existing live-glass machinery (page backdrop recording, glass top controls) runs only
        // at FULL; below that the new UI draws its own cheaper look and the old pieces fall back
        // to their non-glass materials.
        LocalLiquidGlassEnabled provides glassOn,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = if (enabled) V2Type.material else MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}

/** The app's "Reduce animation" or the system's animator scale at zero ("Remove animations"). */
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
    return remember(appSetting) { derivedReduced(appSetting, system) }
}

private fun derivedReduced(app: Boolean, system: State<Boolean>): State<Boolean> =
    object : State<Boolean> {
        override val value: Boolean get() = app || system.value
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
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
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
