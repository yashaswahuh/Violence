package com.music.bitchord.ui.components

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.backdrop.backdrops.SnapshotBackdrop
import com.music.bitchord.ui.components.backdrop.drawBackdrop
import com.music.bitchord.ui.components.backdrop.effects.blur
import com.music.bitchord.ui.components.backdrop.effects.colorControls
import com.music.bitchord.ui.components.backdrop.effects.lens
import com.music.bitchord.ui.components.backdrop.highlight.Highlight
import com.music.bitchord.ui.components.backdrop.shadow.Shadow

/**
 * The page, held as one small still for the surfaces that live in windows of their own (dialogs,
 * bottom sheets): they cannot sample the page live the way the bars do, because the page is drawn
 * in another window, but a panel sitting still over a page nobody is touching has no use for a live
 * one. One picture is taken when the first such panel opens, shared by every panel that opens
 * while it is up, and let go shortly after the last one has gone.
 */
class AppStill(val backdrop: SnapshotBackdrop) {
    private var users = 0
    private val handler = Handler(Looper.getMainLooper())
    private val letGo = Runnable { if (users == 0) backdrop.release() }

    fun acquire() {
        handler.removeCallbacks(letGo)
        users++
        if (users == 1 || !backdrop.hasStill) backdrop.capture()
    }

    fun release() {
        users = (users - 1).coerceAtLeast(0)
        // Not at once: a panel is still playing its exit when it leaves composition.
        if (users == 0) handler.postDelayed(letGo, LET_GO_DELAY_MS)
    }

    private companion object {
        const val LET_GO_DELAY_MS = 500L
    }
}

/** Null where there is no page still to give (the TV build, previews): panels fall back to plain. */
val LocalAppStill = staticCompositionLocalOf<AppStill?> { null }

/** Same pipeline as the bars, tuned for panels: a lot of surface, so a wide bevel and more body. */
private const val PANEL_SCALE = 0.33f
private const val PANEL_BLUR_DP = 18f
private const val PANEL_LENS_HEIGHT_DP = 26f
private const val PANEL_LENS_AMOUNT_DP = 40f

/** The scrim behind a dialog or sheet darkens the page; the glass sits above it, so it is dimmed too. */
private const val SCRIM_ALPHA = 0.30f

/** Until the still lands, a panel is nearly solid, so it never opens as a see-through frame. */
private const val PANEL_UNREADY_ALPHA = 0.94f

private fun panelTint(dark: Boolean): Color = if (dark) Color(0xFF121214) else Color(0xFFFAFAFC)
private fun panelTintAlpha(dark: Boolean): Float = if (dark) 0.42f else 0.52f

/**
 * The glass for a dialog or a sheet: the page's still blurred, lensed at the edges (the same
 * refraction the bars have), with the light-catching rim and a shadow. Null — meaning draw the
 * ordinary surface — while Liquid Glass is off, unsupported, under "reduce dynamic blur", or with
 * no page still to sample.
 */
@Composable
private fun rememberPanelGlass(shape: CornerBasedShape): Modifier? {
    if (!LocalLiquidGlassEnabled.current || !isGlassSupported()) return null
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    if (reduceDynamicBlur) return null
    val still = LocalAppStill.current ?: return null
    DisposableEffect(still) {
        still.acquire()
        onDispose { still.release() }
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    val ready = still.backdrop.hasStill
    val tintAlpha by animateFloatAsState(
        targetValue = if (ready) panelTintAlpha(dark) else PANEL_UNREADY_ALPHA,
        animationSpec = tween(220),
        label = "panelGlassTint",
    )
    val density = LocalDensity.current
    val blurPx = with(density) { PANEL_BLUR_DP.dp.toPx() } * PANEL_SCALE
    val lensHeightPx = with(density) { PANEL_LENS_HEIGHT_DP.dp.toPx() } * PANEL_SCALE
    val lensAmountPx = with(density) { PANEL_LENS_AMOUNT_DP.dp.toPx() } * PANEL_SCALE
    // The tint's alpha is read where it is drawn (a state read inside the draw lambda), so the
    // fade from the near-solid opening frame to the glass re-draws without rebuilding the chain.
    val tint = panelTint(dark)
    val alphaState = rememberAlphaHolder()
    alphaState.value = tintAlpha
    return remember(still, shape, blurPx, lensHeightPx, lensAmountPx, tint) {
        Modifier.drawBackdrop(
            backdrop = still.backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = 1.7f)
                blur(blurPx)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = true,
                        chromaticAberration = true,
                    )
                }
            },
            highlight = { Highlight.Default },
            shadow = { Shadow.Default },
            onDrawSurface = {
                drawRect(Color.Black.copy(alpha = SCRIM_ALPHA), size = size)
                drawRect(tint.copy(alpha = alphaState.value), size = size)
            },
            backdropScale = PANEL_SCALE,
        )
    }
}

/** A state holder read at draw time; assigned during composition by its owner. */
private class AlphaHolder {
    private val state = androidx.compose.runtime.mutableFloatStateOf(PANEL_UNREADY_ALPHA)
    var value: Float
        get() = state.floatValue
        set(v) { state.floatValue = v }
}

@Composable
private fun rememberAlphaHolder(): AlphaHolder = remember { AlphaHolder() }

/**
 * A panel in a window of its own: Liquid Glass over the page's still when it can be, else a plain
 * [Surface] in [plainColor] exactly as before. The content is drawn in the theme's on-surface colour
 * either way.
 */
@Composable
fun GlassSurface(
    shape: CornerBasedShape,
    plainColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val glass = rememberPanelGlass(shape)
    if (glass == null) {
        Surface(shape = shape, color = plainColor, modifier = modifier, content = content)
    } else {
        Box(modifier.then(glass)) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
    }
}

/**
 * Whether a dialog or sheet will be drawn as glass right now — the callers that restructure
 * themselves for glass ask this, so the plain path stays the original code.
 */
@Composable
fun panelGlassActive(): Boolean {
    if (!LocalLiquidGlassEnabled.current || !isGlassSupported()) return false
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    return !reduceDynamicBlur && LocalAppStill.current != null
}

/**
 * A small round control as glass over a [SnapshotBackdrop] still of what it sits on (the player's
 * buttons): blur and lens refraction of the still, the rim, no shadow. [fallback] is the control's
 * plain fill, worn while Liquid Glass is off and until the still is in.
 */
@Composable
fun Modifier.snapshotGlassDisc(
    backdrop: SnapshotBackdrop,
    shape: CornerBasedShape,
    fallback: Color,
): Modifier {
    if (!LocalLiquidGlassEnabled.current || !isGlassSupported()) {
        return clip(shape).background(fallback)
    }
    val ready = backdrop.hasStill
    val density = LocalDensity.current
    val blurPx = with(density) { DISC_BLUR_DP.dp.toPx() } * DISC_SCALE
    val lensHeightPx = with(density) { DISC_LENS_HEIGHT_DP.dp.toPx() } * DISC_SCALE
    val lensAmountPx = with(density) { DISC_LENS_AMOUNT_DP.dp.toPx() } * DISC_SCALE
    val tint = if (ready) Color(0xFF121214).copy(alpha = 0.22f) else fallback
    val glass = remember(backdrop, shape, blurPx, lensHeightPx, lensAmountPx, tint) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = 1.6f)
                blur(blurPx)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = true,
                        chromaticAberration = false,
                    )
                }
            },
            highlight = null,
            shadow = null,
            onDrawSurface = { drawRect(tint, size = size) },
            backdropScale = DISC_SCALE,
        )
    }
    return then(glass)
}

private const val DISC_SCALE = 0.5f
private const val DISC_BLUR_DP = 6f
private const val DISC_LENS_HEIGHT_DP = 9f
private const val DISC_LENS_AMOUNT_DP = 12f
