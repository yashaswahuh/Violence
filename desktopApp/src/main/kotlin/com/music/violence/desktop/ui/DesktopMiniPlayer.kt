package com.music.violence.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.music.violence.desktop.DesktopAppearanceSettings
import com.music.violence.desktop.DesktopArtwork
import com.music.violence.desktop.DesktopMiniPlayerManager
import com.music.violence.desktop.DesktopPlatform
import com.music.violence.ui.icons.ViolenceIcons
import org.jetbrains.compose.resources.painterResource
import violence.desktopapp.generated.resources.Res
import violence.desktopapp.generated.resources.logo
import violence.desktopapp.generated.resources.logo_mark
import java.awt.MouseInfo
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

private val DefaultViolenceCrimson = Color(0xFFE22D4B)
private val DefaultViolenceBright = Color(0xFFFF3B5C)
private val MiniSecondary = Color(0xFF9E9EA8)

/**
 * Undecorated, always-on-top floating PiP Mini-Player Window.
 * Size: 360.dp × 92.dp.
 */
@Composable
fun DesktopMiniPlayerWindow(
    onClose: () -> Unit,
) {
    val initialPos = remember {
        DesktopMiniPlayerManager.getInitialPosition(width = 360, height = 92)
    }
    val state = rememberWindowState(
        width = 360.dp,
        height = 92.dp,
        position = WindowPosition(initialPos.x.dp, initialPos.y.dp),
    )

    Window(
        onCloseRequest = onClose,
        visible = true,
        title = "Violence Mini Player",
        icon = painterResource(Res.drawable.logo),
        state = state,
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = false,
    ) {
        val composeWindow = window

        // Continuous bounds clamping listener: prevents any system or move event from sliding the
        // window outside the usable display area.
        DisposableEffect(composeWindow) {
            val componentListener = object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent) {
                    val loc = composeWindow.location
                    val w = composeWindow.width.takeIf { it > 0 } ?: 360
                    val h = composeWindow.height.takeIf { it > 0 } ?: 92
                    val clamped = DesktopMiniPlayerManager.clampToScreen(loc.x, loc.y, w, h)
                    if (clamped.x != loc.x || clamped.y != loc.y) {
                        composeWindow.setLocation(clamped.x, clamped.y)
                    }
                }
            }
            composeWindow.addComponentListener(componentListener)

            // Clamp immediately on launch
            val initialClamped = DesktopMiniPlayerManager.clampToScreen(
                composeWindow.location.x,
                composeWindow.location.y,
                composeWindow.width.takeIf { it > 0 } ?: 360,
                composeWindow.height.takeIf { it > 0 } ?: 92,
            )
            composeWindow.setLocation(initialClamped.x, initialClamped.y)

            onDispose {
                composeWindow.removeComponentListener(componentListener)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        val isCommand = if (DesktopPlatform.isMac) event.isMetaPressed else event.isCtrlPressed
                        when {
                            (isCommand && event.key == Key.M) || event.key == Key.Escape -> {
                                DesktopMiniPlayerManager.exitMiniMode()
                                return@onPreviewKeyEvent true
                            }
                            event.key == Key.Spacebar || event.key == Key.MediaPlayPause -> {
                                DesktopMiniPlayerManager.onPlayPause()
                                return@onPreviewKeyEvent true
                            }
                            event.key == Key.MediaNext -> {
                                DesktopMiniPlayerManager.onNext()
                                return@onPreviewKeyEvent true
                            }
                            event.key == Key.MediaPrevious -> {
                                DesktopMiniPlayerManager.onPrevious()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
        ) {
            // Sibling drag handle underneath: non-interactive areas drag the window with zero feedback loop
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .miniPlayerDrag(composeWindow),
            )

            // Interactive Mini-Player UI
            DesktopMiniPlayerCard(
                onExpand = { DesktopMiniPlayerManager.exitMiniMode() },
                onClose = onClose,
            )
        }
    }
}

private fun Modifier.miniPlayerDrag(composeWindow: ComposeWindow): Modifier =
    pointerInput(composeWindow) {
        var lastPressAt = 0L
        var lastPressPosition = Offset.Zero
        awaitEachGesture {
            val down = awaitFirstDown()
            val double = down.uptimeMillis - lastPressAt <= viewConfiguration.doubleTapTimeoutMillis &&
                (down.position - lastPressPosition).getDistance() <= viewConfiguration.touchSlop
            if (double) {
                lastPressAt = 0L
                down.consume()
                DesktopMiniPlayerManager.exitMiniMode()
                return@awaitEachGesture
            }
            lastPressAt = down.uptimeMillis
            lastPressPosition = down.position

            val windowAtStart = composeWindow.location
            val pointerAtStart = MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
            var lastClamped = windowAtStart

            drag(down.id) { change ->
                change.consume()
                val pointer = MouseInfo.getPointerInfo()?.location ?: return@drag
                val rawX = windowAtStart.x + pointer.x - pointerAtStart.x
                val rawY = windowAtStart.y + pointer.y - pointerAtStart.y

                val clamped = DesktopMiniPlayerManager.clampToScreen(
                    rawX,
                    rawY,
                    composeWindow.width.takeIf { it > 0 } ?: 360,
                    composeWindow.height.takeIf { it > 0 } ?: 92,
                    pointer,
                )
                lastClamped = clamped
                composeWindow.setLocation(clamped.x, clamped.y)
            }

            // Save last clamped position on drag release
            DesktopMiniPlayerManager.savePosition(
                lastClamped.x,
                lastClamped.y,
                composeWindow.width.takeIf { it > 0 } ?: 360,
                composeWindow.height.takeIf { it > 0 } ?: 92,
            )
        }
    }

@Composable
fun DesktopMiniPlayerCard(
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song by DesktopMiniPlayerManager.currentSong.collectAsState()
    val isPlaying by DesktopMiniPlayerManager.isPlaying.collectAsState()
    val positionMs by DesktopMiniPlayerManager.positionMs.collectAsState()
    val durationMs by DesktopMiniPlayerManager.durationMs.collectAsState()
    val activeLyric by DesktopMiniPlayerManager.activeLyricText.collectAsState()
    val isLiked by DesktopMiniPlayerManager.isLiked.collectAsState()
    val style by DesktopAppearanceSettings.styleState.collectAsState()

    val progressFraction = remember(positionMs, durationMs) {
        if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progressFraction)

    val cardShape = remember { RoundedCornerShape(20.dp) }

    // Dynamic accent resolution: uses user-selected custom color / style accent or Violence crimson
    val accentColor = remember(style.enabled, style.customColor) {
        if (style.enabled && style.customColor != 0) Color(style.customColor) else DefaultViolenceCrimson
    }
    val accentBright = remember(accentColor) {
        if (accentColor == DefaultViolenceCrimson) DefaultViolenceBright else accentColor
    }

    val rimBrush = remember(accentColor) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                accentColor.copy(alpha = 0.38f),
            ),
        )
    }
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xF2111117),
                Color(0xF90A0A0E),
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(bgBrush)
            .border(0.75.dp, rimBrush, cardShape)
            .drawBehind {
                // Top inner specular highlight sheen (Violence signature)
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(20.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 20.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Thumbnail with Violence glowing rim and radiosity underglow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .drawBehind {
                        if (isPlaying) {
                            // Soft radiosity glow behind artwork
                            drawCircle(
                                color = accentColor.copy(alpha = 0.35f),
                                radius = 28.dp.toPx(),
                                center = center,
                            )
                        }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF191920))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!song?.thumbnailUrl.isNullOrBlank()) {
                    DesktopArtwork(
                        url = song?.thumbnailUrl,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)),
                        px = 128,
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.logo_mark),
                        contentDescription = "Violence",
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(Modifier.width(11.dp))

            // Metadata & Controls Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Header: Title marquee & Caption buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = song?.title?.ifBlank { "Nothing Playing" } ?: "Violence",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                    )

                    Spacer(Modifier.width(4.dp))

                    // Heart / Favorite button
                    MiniIconButton(
                        icon = if (isLiked) ViolenceIcons.HeartFilled else ViolenceIcons.Heart,
                        description = if (isLiked) "Remove from liked" else "Like",
                        tint = if (isLiked) accentBright else MiniSecondary,
                        onClick = { DesktopMiniPlayerManager.onToggleLike() },
                        iconSize = 13.dp,
                    )

                    MiniIconButton(
                        icon = Icons.Rounded.OpenInFull,
                        description = "Restore Full Player (Ctrl+M)",
                        onClick = onExpand,
                        iconSize = 12.dp,
                    )

                    MiniIconButton(
                        icon = Icons.Rounded.Close,
                        description = "Close",
                        onClick = onClose,
                        iconSize = 13.dp,
                    )
                }

                // Middle: Word-synced Lyric line OR Artist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (activeLyric != null) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentBright),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = activeLyric!!,
                            color = accentBright,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = song?.artist ?: "Not playing",
                            color = MiniSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Bottom: Playback Controls & Time Stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniControlButton(
                        icon = Icons.Rounded.FastRewind,
                        description = "Previous",
                        onClick = { DesktopMiniPlayerManager.onPrevious() },
                        size = 18.dp,
                    )

                    Spacer(Modifier.width(6.dp))

                    // Hero Play/Pause button with signature Violence gradient
                    HeroPlayButton(
                        isPlaying = isPlaying,
                        accentBright = accentBright,
                        accentDark = accentColor,
                        onClick = { DesktopMiniPlayerManager.onPlayPause() },
                    )

                    Spacer(Modifier.width(6.dp))

                    MiniControlButton(
                        icon = Icons.Rounded.FastForward,
                        description = "Next",
                        onClick = { DesktopMiniPlayerManager.onNext() },
                        size = 18.dp,
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                        color = MiniSecondary.copy(alpha = 0.85f),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                    )
                }
            }
        }

        // Inset interactive seekbar along bottom edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.5.dp)
                .height(8.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            DesktopMiniPlayerManager.onSeek((fraction * durationMs).toLong())
                        }
                    }
                }
                .drawBehind {
                    val trackHeight = 2.5.dp.toPx()
                    val trackY = size.height - trackHeight
                    // Inset dark track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
                    )
                    // Active Violence gradient fill
                    val filledWidth = size.width * animatedProgress
                    if (filledWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    accentBright,
                                    accentColor,
                                ),
                            ),
                            topLeft = Offset(0f, trackY),
                            size = Size(filledWidth, trackHeight),
                            cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
                        )
                        // Playhead glowing dot
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = Offset(filledWidth.coerceIn(3.5.dp.toPx(), size.width - 3.5.dp.toPx()), trackY + trackHeight / 2),
                        )
                    }
                },
        )
    }
}

@Composable
private fun HeroPlayButton(
    isPlaying: Boolean,
    accentBright: Color,
    accentDark: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val gradient = remember(hovered, accentBright, accentDark) {
        if (hovered) {
            Brush.linearGradient(
                listOf(
                    accentBright,
                    accentDark.copy(alpha = 0.90f),
                ),
            )
        } else {
            Brush.linearGradient(
                listOf(
                    accentBright.copy(alpha = 0.92f),
                    accentDark,
                ),
            )
        }
    }

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(gradient)
            .border(0.75.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MiniIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp,
    tint: Color = MiniSecondary,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (hovered) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (hovered && tint == MiniSecondary) Color.White else tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun MiniControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (hovered) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (hovered) Color.White else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(size),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
