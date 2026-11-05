package com.music.violence.wear.ui

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.Text
import com.music.violence.wear.R
import com.music.violence.wear.data.NowPlaying
import com.music.violence.wear.data.Volume
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the player can ask of the phone. */
class PlayerActions(
    val togglePlay: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val toggleLike: () -> Unit,
    val volume: (Int) -> Unit,
    val seekTo: (Long) -> Unit,
    val openQueue: () -> Unit,
    val openMore: () -> Unit,
)

/**
 * The phone's player, on a round screen, in Material 3 Expressive.
 *
 * The cover is the *scene*: a blurred, saturated copy fills the screen behind
 * everything ([ArtBackdrop]) and the controls take the song's own accent
 * ([accentFor]). Progress is a wave along the bezel ([WavyBezel]) that ripples
 * while the music plays and goes flat when it pauses. The play button is a
 * shape that morphs — a circle when paused, a rounded square when playing — and
 * every press is a spring. Hold it to scrub; the bezel and crown follow.
 *
 * The crown is the phone's volume, as on every watch media app.
 */
@Composable
fun NowPlayingScreen(
    np: NowPlaying,
    artworkBlur: ImageBitmap?,
    volume: Volume?,
    actions: PlayerActions,
) {
    val view = LocalView.current
    var rotary by remember { mutableFloatStateOf(0f) }
    // The playhead, drawn along the bezel. Read only in the draw pass.
    val progress = rememberProgress(np)
    val accent = remember(np.colors) { accentFor(np.colors) }
    // Bumped on every crown step; the readout shows while it keeps changing.
    var volumeNudge by remember { mutableIntStateOf(0) }

    // Hold-to-seek: the target while the button is a scrubber, null otherwise.
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    var scrubHeld by remember { mutableStateOf(false) }
    var scrubActivity by remember { mutableIntStateOf(0) }
    fun scrubBy(deltaMs: Long) {
        val from = scrubMs ?: return
        scrubMs = (from + deltaMs).coerceIn(0L, np.durationMs.coerceAtLeast(0L))
        scrubActivity++
    }
    fun commitScrub() {
        scrubMs?.let(actions.seekTo)
        scrubMs = null
        scrubHeld = false
    }
    // Let go and left alone for a moment: that is the answer.
    LaunchedEffect(scrubActivity, scrubHeld) {
        if (scrubMs == null || scrubHeld) return@LaunchedEffect
        delay(SCRUB_SETTLE_MS)
        commitScrub()
    }
    // A new track makes the old target meaningless.
    LaunchedEffect(np.mediaId) {
        scrubMs = null
        scrubHeld = false
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                val px = event.verticalScrollPixels
                // A notched bezel reports one big event per detent, a smooth
                // crown a stream of small ones: one step per detent either way.
                val steps = if (abs(px) >= ROTARY_STEP_PX) {
                    rotary = 0f
                    if (px > 0) 1 else -1
                } else {
                    rotary += px
                    (rotary / ROTARY_STEP_PX).toInt().also { rotary -= it * ROTARY_STEP_PX }
                }
                if (steps != 0) {
                    // While scrubbing the bezel seeks instead, five seconds a detent.
                    if (scrubMs != null) {
                        scrubBy(steps * SCRUB_STEP_MS)
                    } else {
                        actions.volume(steps)
                        volumeNudge++
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                true
            }
            // The crown goes to whichever screen is on top, so this one takes
            // focus back whenever Up Next or More is swiped away.
            .requestFocusOnHierarchyActive()
            .focusable(),
    ) {
        val w = maxWidth
        val h = maxHeight

        ArtBackdrop(blur = artworkBlur, colors = np.colors)
        WavyBezel(
            progress = progress,
            scrubMs = scrubMs,
            durationMs = np.durationMs,
            playing = np.advancing,
            color = accent,
        )

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Below the curved clock.
            Spacer(Modifier.height(h * 0.125f))
            key(np.mediaId) {
                Text(
                    text = np.title,
                    style = WearType.display.copy(shadow = TextShadow),
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = w * 0.74f)
                        // Twice, then rest: a title that scrolls forever is a
                        // screen that never stops redrawing.
                        .basicMarquee(iterations = 2, initialDelayMillis = 1_200, repeatDelayMillis = 2_000),
                )
            }
            Text(
                text = np.artist,
                style = WearType.subtitle.copy(shadow = TextShadow),
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = w * 0.66f),
            )

            Spacer(Modifier.height(h * 0.025f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportButton(
                    icon = R.drawable.ic_previous,
                    description = stringResource(R.string.previous),
                    size = w * 0.19f,
                    iconSize = w * 0.10f,
                    onClick = actions.previous,
                )
                Spacer(Modifier.width(w * 0.01f))
                PlayButton(
                    np = np,
                    size = h * 0.375f,
                    accent = accent,
                    scrubMs = scrubMs,
                    onToggle = actions.togglePlay,
                    onScrubStart = {
                        scrubHeld = true
                        scrubMs = np.positionAt(System.currentTimeMillis())
                        scrubActivity++
                    },
                    onScrubBy = ::scrubBy,
                    onScrubRelease = {
                        scrubHeld = false
                        scrubActivity++
                    },
                    onCommit = ::commitScrub,
                )
                Spacer(Modifier.width(w * 0.01f))
                TransportButton(
                    icon = R.drawable.ic_next,
                    description = stringResource(R.string.next),
                    size = w * 0.19f,
                    iconSize = w * 0.10f,
                    enabled = np.hasNext,
                    onClick = actions.next,
                )
            }

            Spacer(Modifier.height(h * 0.025f))

            ActionPill(np = np, buttonSize = w * 0.15f, actions = actions)
        }

        VolumeReadout(volume = volume, nudge = volumeNudge, edgeInset = 5.dp)
    }
}

/** A soft shadow so white text holds on any cover. */
private val TextShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 8f)

/** Previous / next: a glass tonal button that morphs its corners as it is pressed. */
@Composable
private fun TransportButton(
    @DrawableRes icon: Int,
    description: String,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        enabled = enabled,
        shapes = IconButtonDefaults.animatedShapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Glass,
            contentColor = Color.White,
        ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Love, Up Next and More in one glass capsule, the way Material 3 Expressive
 * groups related actions. Love is a toggle: it fills with Ember and its corners
 * morph when it is on.
 */
@Composable
private fun ActionPill(np: NowPlaying, buttonSize: Dp, actions: PlayerActions) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Only where the phone's player shows one: signed in, not a local file.
        if (np.canLike) {
            IconToggleButton(
                checked = np.liked,
                onCheckedChange = { actions.toggleLike() },
                modifier = Modifier.size(buttonSize),
                shapes = IconToggleButtonDefaults.animatedShapes(),
                colors = IconToggleButtonDefaults.colors(
                    uncheckedContainerColor = Color.Transparent,
                    uncheckedContentColor = Color.White,
                    checkedContainerColor = Ember,
                    checkedContentColor = Color(0xFF14101F),
                ),
            ) {
                Icon(
                    painter = painterResource(if (np.liked) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                    contentDescription = stringResource(if (np.liked) R.string.loved else R.string.love),
                    modifier = Modifier.size(buttonSize * 0.5f),
                )
            }
        }
        PillButton(R.drawable.ic_queue, stringResource(R.string.up_next), buttonSize, actions.openQueue)
        PillButton(R.drawable.ic_more, stringResource(R.string.more), buttonSize, actions.openMore)
    }
}

@Composable
private fun PillButton(@DrawableRes icon: Int, description: String, size: Dp, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        shapes = IconButtonDefaults.animatedShapes(),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * The play button: solid in the song's accent, with the glyph over it. It
 * morphs between a circle (paused) and a rounded square (playing) on a spring,
 * dips with a bounce when pressed, and while the phone is still finding a
 * stream — play pressed, nothing audible yet — a ring turns round it, so a slow
 * YouTube resolve reads as "working" rather than as a tap that didn't land.
 *
 * Held, it becomes a scrubber, as on Spotify's watch player: drag sideways
 * through the song, or turn the bezel. The centre reads out the target
 * ([scrubMs]); the caller commits it.
 */
@Composable
private fun PlayButton(
    np: NowPlaying,
    size: Dp,
    accent: Color,
    scrubMs: Long?,
    onToggle: () -> Unit,
    onScrubStart: () -> Unit,
    onScrubBy: (Long) -> Unit,
    onScrubRelease: () -> Unit,
    onCommit: () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val ambient = LocalAmbient.current.active
    val scrubbing = scrubMs != null
    val buffering = np.playing && !np.advancing
    val spin = if (buffering && !ambient && !scrubbing) {
        rememberInfiniteTransition(label = "buffering").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1_100, easing = LinearEasing)),
            label = "spin",
        )
    } else {
        null
    }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        when {
            scrubbing -> 1.06f
            pressed -> 0.90f
            else -> 1f
        },
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val face = size * 0.86f
    val corner by animateDpAsState(
        if (np.playing) face * 0.30f else face / 2f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "morph",
    )
    val glyph = onAccent(accent)
    // Read by the gesture blocks, which live across recompositions.
    val latestScrubbing by rememberUpdatedState(scrubbing)
    val latestDuration by rememberUpdatedState(np.durationMs)
    val toggleLabel = stringResource(if (np.playing) R.string.pause else R.string.play)

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val stroke = (if (scrubMs != null) 4.dp else 2.5.dp).toPx()
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = Offset(inset, inset)
                val turning = spin?.value
                val fraction = if (scrubMs != null && np.durationMs > 0) scrubMs.toFloat() / np.durationMs else 0f
                if (turning != null) {
                    drawArc(
                        color = Color.White,
                        startAngle = turning - 90f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                } else if (fraction * 360f > 0.5f) {
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                onClick(toggleLabel) {
                    onToggle()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    // The drag detector below owns the hold; this is set only
                    // so a hold is never also taken as a tap when let go.
                    onLongPress = {},
                    onTap = { if (latestScrubbing) onCommit() else onToggle() },
                )
            }
            .pointerInput(Unit) {
                // A full screen's width of drag covers half the song, but never
                // less than a minute, so a short track stays controllable.
                val screenPx = with(density) { 210.dp.toPx() }
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        if (latestDuration > 0) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onScrubStart()
                        }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        if (latestScrubbing) {
                            val span = maxOf(latestDuration / 2, 60_000L)
                            onScrubBy((drag.x / screenPx * span).toLong())
                        }
                    },
                    onDragEnd = { if (latestScrubbing) onScrubRelease() },
                    onDragCancel = { if (latestScrubbing) onScrubRelease() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(face)
                .clip(RoundedCornerShape(corner))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            if (scrubMs != null) {
                val from = remember(np) { np.positionAt(System.currentTimeMillis()) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(scrubMs),
                        style = WearType.title.copy(fontFeatureSettings = "tnum"),
                        color = glyph,
                    )
                    Text(
                        text = formatDelta(scrubMs - from),
                        style = WearType.caption.copy(fontFeatureSettings = "tnum"),
                        color = glyph.copy(alpha = 0.7f),
                    )
                }
            } else {
                Crossfade(targetState = np.playing, animationSpec = tween(140), label = "glyph") { playing ->
                    Icon(
                        painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null,
                        tint = glyph,
                        modifier = Modifier.size(face * 0.42f),
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun formatDelta(ms: Long): String =
    (if (ms < 0) "−" else "+") + formatTime(abs(ms))

/**
 * The playhead, run on from the phone's last reading.
 *
 * Read only in the ring's draw pass, so a tick redraws one arc and recomposes
 * nothing. Twice a second is a pixel or so of ring on a three-minute song —
 * smooth to the eye at a fraction of a frame-clock's cost — and only while the
 * screen is on.
 */
@Composable
private fun rememberProgress(np: NowPlaying): State<Float> {
    val progress = remember { mutableFloatStateOf(np.progressAt(System.currentTimeMillis())) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // The always-on screen draws its own ring once a minute; this one rests.
    val ambient = LocalAmbient.current.active
    LaunchedEffect(np, ambient) {
        progress.floatValue = np.progressAt(System.currentTimeMillis())
        if (!np.advancing || ambient) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                progress.floatValue = np.progressAt(System.currentTimeMillis())
                delay(PROGRESS_TICK_MS)
            }
        }
    }
    return progress
}

/**
 * The phone's volume as an arc on the right edge, where the crown is — shown
 * while the crown turns and gone a moment after.
 */
@Composable
private fun BoxScope.VolumeReadout(volume: Volume?, nudge: Int, edgeInset: Dp) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(nudge) {
        if (nudge == 0) return@LaunchedEffect
        visible = true
        delay(VOLUME_SHOWN_MS)
        visible = false
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 120 else 400), label = "volumeAlpha")
    val fraction by animateFloatAsState(volume?.fraction ?: 0f, tween(120), label = "volumeLevel")
    if (alpha == 0f || volume == null) return

    Canvas(
        Modifier
            .matchParentSize()
            .graphicsLayer { this.alpha = alpha },
    ) {
        val stroke = 6.dp.toPx()
        val inset = edgeInset.toPx() + stroke / 2f
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        // 0° is three o'clock; the arc is centred on it and fills upwards.
        drawArc(
            color = Color.White.copy(alpha = 0.25f),
            startAngle = VOLUME_ARC / 2f,
            sweepAngle = -VOLUME_ARC,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        if (fraction > 0f) {
            drawArc(
                color = Color.White,
                startAngle = VOLUME_ARC / 2f,
                sweepAngle = -VOLUME_ARC * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
    Row(
        Modifier
            .align(Alignment.CenterEnd)
            .padding(end = edgeInset + 16.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume),
            contentDescription = stringResource(R.string.volume),
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = (fraction * 100).roundToInt().toString(),
            style = WearType.caption,
            color = Color.White,
        )
    }
}

/** Crown travel per volume step, for crowns that report a stream of small events. */
private const val ROTARY_STEP_PX = 48f
private const val PROGRESS_TICK_MS = 500L

private const val VOLUME_SHOWN_MS = 1_400L
private const val VOLUME_ARC = 64f
private const val SCRUB_STEP_MS = 5_000L
private const val SCRUB_SETTLE_MS = 1_200L
