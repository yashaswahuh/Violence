package com.music.violence.wear.ui

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
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
    val volume: (Int) -> Unit,
    val seekTo: (Long) -> Unit,
    val openOutput: () -> Unit,
    val openMore: () -> Unit,
)

/**
 * The phone's player, on a round screen, in the Pixel Watch's Material 3
 * Expressive layout.
 *
 * The cover is the scene: a lightly blurred copy fills the screen behind
 * everything ([ArtBackdrop]) and every control takes the song's own tones
 * ([paletteFor]). The play button is a scalloped "cookie" while the music
 * plays and relaxes into a circle on pause, with a wavy progress ring round it
 * ([WavyRing]) that ripples while playing and goes flat when paused. Previous
 * and next are tall pills either side; Output and More sit as glass pills
 * below, with the crown-volume badge on the Output pill's corner. Hold the play
 * button to scrub; the crown is the phone's volume.
 *
 * Everything is placed from a 384-unit-wide grid scaled to the screen (`u`
 * below), so it lands the same on a 40 mm and a 47 mm watch.
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
    // The playhead, drawn round the play button. Read only in the draw pass.
    val progress = rememberProgress(np)
    val palette = remember(np.colors) { paletteFor(np.colors) }
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
            // focus back whenever More or Output is swiped away.
            .requestFocusOnHierarchyActive()
            .focusable(),
    ) {
        // One grid unit: the screen is 384 of them across.
        val u = maxWidth / 384f

        ArtBackdrop(blur = artworkBlur, colors = np.colors)

        key(np.mediaId) {
            Text(
                text = np.title,
                style = WearType.display.copy(fontSize = (u.value * 27f).sp, shadow = TextShadow),
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = u * 60)
                    .widthIn(max = u * 290)
                    // Twice, then rest: a title that scrolls forever is a
                    // screen that never stops redrawing.
                    .basicMarquee(iterations = 2, initialDelayMillis = 1_200, repeatDelayMillis = 2_000),
            )
        }
        Text(
            text = np.artist,
            style = WearType.subtitle.copy(fontSize = (u.value * 18f).sp, shadow = TextShadow),
            color = Color.White.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = u * 94)
                .widthIn(max = u * 250),
        )

        // Previous and next: tall tonal pills, either side.
        TransportPill(
            icon = R.drawable.ic_previous,
            description = stringResource(R.string.previous),
            palette = palette,
            iconSize = u * 42,
            corner = u * 34,
            onClick = actions.previous,
            modifier = Modifier.offset(x = u * 22, y = u * 144).size(width = u * 74, height = u * 96),
        )
        TransportPill(
            icon = R.drawable.ic_next,
            description = stringResource(R.string.next),
            palette = palette,
            iconSize = u * 42,
            corner = u * 34,
            enabled = np.hasNext,
            onClick = actions.next,
            modifier = Modifier.offset(x = u * 288, y = u * 144).size(width = u * 74, height = u * 96),
        )

        PlayButton(
            np = np,
            palette = palette,
            progress = progress,
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
            modifier = Modifier.offset(x = u * 128, y = u * 128).size(u * 128),
        )

        // Output and More: glass pills below the button.
        BottomPill(
            icon = R.drawable.ic_headphones,
            description = stringResource(R.string.output),
            palette = palette,
            iconSize = u * 42,
            onClick = actions.openOutput,
            modifier = Modifier.offset(x = u * 80, y = u * 284).size(width = u * 116, height = u * 52),
        )
        BottomPill(
            icon = R.drawable.ic_more,
            description = stringResource(R.string.more),
            palette = palette,
            iconSize = u * 38,
            onClick = actions.openMore,
            modifier = Modifier.offset(x = u * 208, y = u * 284).size(width = u * 96, height = u * 52),
        )
        // The crown-volume badge: on the Output pill's top corner and nowhere else.
        Box(
            Modifier
                .offset(x = u * 163, y = u * 271)
                .size(u * 30)
                .border(u * 2.5f, Color.Black.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume),
                contentDescription = stringResource(R.string.volume),
                tint = palette.onAccent,
                modifier = Modifier.size(u * 16),
            )
        }

        VolumeReadout(volume = volume, nudge = volumeNudge, edgeInset = 5.dp)
    }
}

/** A soft shadow so white text holds on any cover. */
private val TextShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 8f)

/** Previous / next: a tall tonal pill in the song's accent whose corners tighten as it is pressed. */
@Composable
private fun TransportPill(
    @DrawableRes icon: Int,
    description: String,
    palette: Palette,
    iconSize: Dp,
    corner: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = IconButtonDefaults.animatedShapes(
            shape = RoundedCornerShape(corner),
            pressedShape = RoundedCornerShape(corner * 0.55f),
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = palette.accent,
            contentColor = palette.onAccent,
            disabledContainerColor = palette.accent.copy(alpha = 0.30f),
            disabledContentColor = palette.onAccent.copy(alpha = 0.45f),
        ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Output and More: a wide pill of dark glass tinted by the song's hue, with a light glyph. */
@Composable
private fun BottomPill(
    @DrawableRes icon: Int,
    description: String,
    palette: Palette,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        shapes = IconButtonDefaults.animatedShapes(
            shape = RoundedCornerShape(50),
            pressedShape = RoundedCornerShape(30),
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = palette.glass,
            contentColor = palette.text,
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
 * The play button: a cookie in the song's accent with the glyph over it and a
 * wavy progress ring round it. The cookie relaxes into a circle on pause, on a
 * spring, and dips with a bounce when pressed. While the phone is still finding
 * a stream — play pressed, nothing audible yet — the ring turns instead of
 * showing progress, so a slow YouTube resolve reads as "working" rather than as
 * a tap that didn't land.
 *
 * Held, it becomes a scrubber, as on Spotify's watch player: drag sideways
 * through the song, or turn the bezel. The centre reads out the target
 * ([scrubMs]); the caller commits it.
 */
@Composable
private fun PlayButton(
    np: NowPlaying,
    palette: Palette,
    progress: State<Float>,
    scrubMs: Long?,
    onToggle: () -> Unit,
    onScrubStart: () -> Unit,
    onScrubBy: (Long) -> Unit,
    onScrubRelease: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
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
            pressed -> 0.92f
            else -> 1f
        },
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    // Scalloped while playing, a plain circle when paused.
    val lobeDepth by animateFloatAsState(
        if (np.playing) 0.075f else 0f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "cookie",
    )
    // Read by the gesture blocks, which live across recompositions.
    val latestScrubbing by rememberUpdatedState(scrubbing)
    val latestDuration by rememberUpdatedState(np.durationMs)
    val toggleLabel = stringResource(if (np.playing) R.string.pause else R.string.play)

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
        val cookie = maxWidth * 0.72f
        val spinning = spin
        if (spinning == null) {
            WavyRing(
                progress = progress,
                scrubMs = scrubMs,
                durationMs = np.durationMs,
                playing = np.advancing,
                color = palette.text,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * (4f / 128f)
                val inset = size.minDimension * (5f / 128f) + stroke / 2f
                drawArc(
                    color = palette.text,
                    startAngle = spinning.value - 90f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Box(
            Modifier
                .size(cookie)
                .clip(CookieShape(lobes = 10, amplitude = lobeDepth))
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            if (scrubMs != null) {
                val from = remember(np) { np.positionAt(System.currentTimeMillis()) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(scrubMs),
                        style = WearType.title.copy(fontFeatureSettings = "tnum"),
                        color = palette.onAccent,
                    )
                    Text(
                        text = formatDelta(scrubMs - from),
                        style = WearType.caption.copy(fontFeatureSettings = "tnum"),
                        color = palette.onAccent.copy(alpha = 0.7f),
                    )
                }
            } else {
                Crossfade(targetState = np.playing, animationSpec = tween(140), label = "glyph") { playing ->
                    Icon(
                        painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null,
                        tint = palette.onAccent,
                        modifier = Modifier.size(cookie * 0.48f),
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
