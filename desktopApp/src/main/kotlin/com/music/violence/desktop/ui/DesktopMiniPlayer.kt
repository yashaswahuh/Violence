package com.music.violence.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.window.rememberWindowState
import com.music.violence.desktop.DesktopArtwork
import com.music.violence.desktop.DesktopMiniPlayerManager
import com.music.violence.desktop.DesktopPlatform
import org.jetbrains.compose.resources.painterResource
import violence.desktopapp.generated.resources.Res
import violence.desktopapp.generated.resources.logo
import kotlin.math.roundToInt

private val ViolenceRimRed = Color(0xFFE22D4B)
private val MiniBackground = Color(0xF2141417)
private val MiniBorder = Color.White.copy(alpha = 0.12f)
private val MiniSecondary = Color(0xFF9E9EA8)

/**
 * Undecorated, always-on-top floating PiP Mini-Player Window.
 * Size: 340.dp × 96.dp.
 */
@Composable
fun DesktopMiniPlayerWindow(
    onClose: () -> Unit,
) {
    val state = rememberWindowState(
        width = 340.dp,
        height = 96.dp,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        val isCommand = if (DesktopPlatform.isMac) event.isMetaPressed else event.isCtrlPressed
                        if (isCommand && event.key == Key.M) {
                            DesktopMiniPlayerManager.exitMiniMode()
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val currentLoc = composeWindow.location
                        composeWindow.setLocation(
                            currentLoc.x + dragAmount.x.roundToInt(),
                            currentLoc.y + dragAmount.y.roundToInt(),
                        )
                    }
                },
        ) {
            DesktopMiniPlayerCard(
                onExpand = { DesktopMiniPlayerManager.exitMiniMode() },
                onClose = onClose,
            )
        }
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

    val progressFraction = remember(positionMs, durationMs) {
        if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progressFraction)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MiniBackground)
            .border(1.dp, MiniBorder, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Thumbnail with glowing Violence rim
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, ViolenceRimRed.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .background(Color(0xFF202026)),
                contentAlignment = Alignment.Center,
            ) {
                if (!song?.thumbnailUrl.isNullOrBlank()) {
                    DesktopArtwork(
                        url = song?.thumbnailUrl,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
                        px = 128,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Artwork",
                        tint = MiniSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

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
                        text = song?.title?.ifBlank { "Nothing Playing" } ?: "Nothing Playing",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                    )

                    Spacer(Modifier.width(4.dp))

                    MiniIconButton(
                        icon = Icons.Rounded.OpenInFull,
                        description = "Restore Full Player (Ctrl+M)",
                        onClick = onExpand,
                        iconSize = 13.dp,
                    )

                    MiniIconButton(
                        icon = Icons.Rounded.Close,
                        description = "Close",
                        onClick = onClose,
                        iconSize = 14.dp,
                    )
                }

                // Middle: Word-synced Lyric line OR Artist
                Text(
                    text = activeLyric ?: song?.artist ?: "Violence",
                    color = if (activeLyric != null) ViolenceRimRed else MiniSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (activeLyric != null) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Bottom: Playback Controls & Time Stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniControlButton(
                        icon = Icons.Rounded.SkipPrevious,
                        description = "Previous",
                        onClick = { DesktopMiniPlayerManager.onPrevious() },
                        size = 20.dp,
                    )

                    Spacer(Modifier.width(4.dp))

                    // Play/Pause button with circular highlight
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { DesktopMiniPlayerManager.onPlayPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    MiniControlButton(
                        icon = Icons.Rounded.SkipNext,
                        description = "Next",
                        onClick = { DesktopMiniPlayerManager.onNext() },
                        size = 20.dp,
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                        color = MiniSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }

        // Slim interactive progress bar pinned along the bottom edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(6.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            DesktopMiniPlayerManager.onSeek((fraction * durationMs).toLong())
                        }
                    }
                }
                .drawBehind {
                    // Gray track
                    drawRect(
                        color = Color.White.copy(alpha = 0.12f),
                        topLeft = Offset(0f, size.height - 2.5.dp.toPx()),
                        size = Size(size.width, 2.5.dp.toPx()),
                    )
                    // Active accent fill
                    drawRect(
                        color = ViolenceRimRed,
                        topLeft = Offset(0f, size.height - 2.5.dp.toPx()),
                        size = Size(size.width * animatedProgress, 2.5.dp.toPx()),
                    )
                },
        )
    }
}

@Composable
private fun MiniIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (hovered) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (hovered) Color.White else MiniSecondary,
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

