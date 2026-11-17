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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
 * Dynamically switches between Apple Music sleek compact mode (380.dp × 78.dp) and
 * expanded synchronized live lyrics mode (380.dp × 400.dp).
 */
@Composable
fun DesktopMiniPlayerWindow(
    onClose: () -> Unit,
) {
    val isLyricsExpanded by DesktopMiniPlayerManager.isLyricsExpanded.collectAsState()
    val targetWidth = 380.dp
    val targetHeight = if (isLyricsExpanded) 400.dp else 78.dp

    val initialPos = remember {
        DesktopMiniPlayerManager.getInitialPosition(width = 380, height = 78)
    }
    val state = rememberWindowState(
        width = targetWidth,
        height = targetHeight,
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

        // Dynamic resize animation and bounds clamping on mode transition
        LaunchedEffect(isLyricsExpanded) {
            state.size = DpSize(targetWidth, targetHeight)
            val loc = composeWindow.location
            val w = 380
            val h = if (isLyricsExpanded) 400 else 78
            val clamped = DesktopMiniPlayerManager.clampToScreen(loc.x, loc.y, w, h)
            if (clamped.x != loc.x || clamped.y != loc.y) {
                composeWindow.setLocation(clamped.x, clamped.y)
            }
        }

        // Continuous bounds clamping listener: prevents any system move/resize from sliding
        // the miniplayer outside the usable display area.
        DisposableEffect(composeWindow) {
            val componentListener = object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent) {
                    val loc = composeWindow.location
                    val w = composeWindow.width.takeIf { it > 0 } ?: 380
                    val h = composeWindow.height.takeIf { it > 0 } ?: (if (isLyricsExpanded) 400 else 78)
                    val clamped = DesktopMiniPlayerManager.clampToScreen(loc.x, loc.y, w, h)
                    if (clamped.x != loc.x || clamped.y != loc.y) {
                        composeWindow.setLocation(clamped.x, clamped.y)
                    }
                }

                override fun componentResized(e: ComponentEvent) {
                    val loc = composeWindow.location
                    val w = composeWindow.width.takeIf { it > 0 } ?: 380
                    val h = composeWindow.height.takeIf { it > 0 } ?: (if (isLyricsExpanded) 400 else 78)
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
                composeWindow.width.takeIf { it > 0 } ?: 380,
                composeWindow.height.takeIf { it > 0 } ?: 78,
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

                val w = composeWindow.width.takeIf { it > 0 } ?: 380
                val h = composeWindow.height.takeIf { it > 0 } ?: 78
                val clamped = DesktopMiniPlayerManager.clampToScreen(
                    rawX,
                    rawY,
                    w,
                    h,
                    pointer,
                )
                lastClamped = clamped
                composeWindow.setLocation(clamped.x, clamped.y)
            }

            // Save last clamped position on drag release
            val w = composeWindow.width.takeIf { it > 0 } ?: 380
            val h = composeWindow.height.takeIf { it > 0 } ?: 78
            DesktopMiniPlayerManager.savePosition(
                lastClamped.x,
                lastClamped.y,
                w,
                h,
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
    val isLyricsExpanded by DesktopMiniPlayerManager.isLyricsExpanded.collectAsState()
    val lyricLines by DesktopMiniPlayerManager.lyricLines.collectAsState()
    val activeLyricIndex by DesktopMiniPlayerManager.activeLyricIndex.collectAsState()

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
                Color.White.copy(alpha = 0.22f),
                accentColor.copy(alpha = 0.36f),
            ),
        )
    }
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFA111117),
                Color(0xFE0A0A0E),
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
        if (!isLyricsExpanded) {
            // Apple Music Compact Mini-Player Mode
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork Squircle (48dp x 48dp) with radiosity underglow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            if (isPlaying) {
                                drawCircle(
                                    color = accentColor.copy(alpha = 0.35f),
                                    radius = 26.dp.toPx(),
                                    center = center,
                                )
                            }
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF181820))
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!song?.thumbnailUrl.isNullOrBlank()) {
                        DesktopArtwork(
                            url = song?.thumbnailUrl,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                            px = 128,
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.logo_mark),
                            contentDescription = "Violence",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Metadata Column with inline scrub line
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = song?.title?.ifBlank { "Nothing Playing" } ?: "Violence",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    )

                    Spacer(Modifier.height(2.dp))

                    if (activeLyric != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(accentBright),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = activeLyric!!,
                                color = accentBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            )
                        }
                    } else {
                        Text(
                            text = song?.artist ?: "Not playing",
                            color = MiniSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Minimal inline scrubber line
                    Box(
                        modifier = Modifier
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
                                val trackHeight = 2.dp.toPx()
                                val trackY = (size.height - trackHeight) / 2
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.12f),
                                    topLeft = Offset(0f, trackY),
                                    size = Size(size.width, trackHeight),
                                    cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
                                )
                                val filledWidth = size.width * animatedProgress
                                if (filledWidth > 0f) {
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(listOf(accentBright, accentColor)),
                                        topLeft = Offset(0f, trackY),
                                        size = Size(filledWidth, trackHeight),
                                        cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
                                    )
                                }
                            },
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Transport & Utility Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MiniIconButton(
                        icon = if (isLiked) ViolenceIcons.HeartFilled else ViolenceIcons.Heart,
                        description = if (isLiked) "Remove from liked" else "Like",
                        tint = if (isLiked) accentBright else MiniSecondary,
                        onClick = { DesktopMiniPlayerManager.onToggleLike() },
                        iconSize = 13.dp,
                    )

                    MiniControlButton(
                        icon = Icons.Rounded.FastRewind,
                        description = "Previous",
                        onClick = { DesktopMiniPlayerManager.onPrevious() },
                        size = 17.dp,
                    )

                    HeroPlayButton(
                        isPlaying = isPlaying,
                        accentBright = accentBright,
                        accentDark = accentColor,
                        onClick = { DesktopMiniPlayerManager.onPlayPause() },
                    )

                    MiniControlButton(
                        icon = Icons.Rounded.FastForward,
                        description = "Next",
                        onClick = { DesktopMiniPlayerManager.onNext() },
                        size = 17.dp,
                    )

                    // Lyrics toggle button (Apple Music quote bubble)
                    MiniIconButton(
                        icon = ViolenceIcons.LyricsQuote,
                        description = "Live Lyrics",
                        tint = MiniSecondary,
                        onClick = { DesktopMiniPlayerManager.toggleLyricsExpanded() },
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
            }
        } else {
            // Apple Music Expanded Mini-Player Mode with Live Synchronized Lyrics
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF181820))
                            .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!song?.thumbnailUrl.isNullOrBlank()) {
                            DesktopArtwork(
                                url = song?.thumbnailUrl,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                px = 96,
                            )
                        } else {
                            Image(
                                painter = painterResource(Res.drawable.logo_mark),
                                contentDescription = "Violence",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title?.ifBlank { "Nothing Playing" } ?: "Violence",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        )
                        Text(
                            text = song?.artist ?: "Not playing",
                            color = MiniSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        MiniControlButton(
                            icon = Icons.Rounded.FastRewind,
                            description = "Previous",
                            onClick = { DesktopMiniPlayerManager.onPrevious() },
                            size = 15.dp,
                        )

                        HeroPlayButton(
                            isPlaying = isPlaying,
                            accentBright = accentBright,
                            accentDark = accentColor,
                            onClick = { DesktopMiniPlayerManager.onPlayPause() },
                            size = 26.dp,
                            iconSize = 15.dp,
                        )

                        MiniControlButton(
                            icon = Icons.Rounded.FastForward,
                            description = "Next",
                            onClick = { DesktopMiniPlayerManager.onNext() },
                            size = 15.dp,
                        )

                        // Highlighted active lyrics button: clicking collapses back to compact mode
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.35f))
                                .border(0.75.dp, accentBright.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable { DesktopMiniPlayerManager.toggleLyricsExpanded() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = ViolenceIcons.LyricsQuote,
                                contentDescription = "Close Lyrics",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp),
                            )
                        }

                        MiniIconButton(
                            icon = Icons.Rounded.OpenInFull,
                            description = "Restore Full Player",
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
                }

                // Scrubber Bar with Timestamps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTime(positionMs),
                        color = MiniSecondary,
                        fontSize = 10.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
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
                                val trackHeight = 2.dp.toPx()
                                val trackY = (size.height - trackHeight) / 2
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.12f),
                                    topLeft = Offset(0f, trackY),
                                    size = Size(size.width, trackHeight),
                                    cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
                                )
                                val filledWidth = size.width * animatedProgress
                                if (filledWidth > 0f) {
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(listOf(accentBright, accentColor)),
                                        topLeft = Offset(0f, trackY),
                                        size = Size(filledWidth, trackHeight),
                                        cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2),
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 3.dp.toPx(),
                                        center = Offset(
                                            filledWidth.coerceIn(3.dp.toPx(), size.width - 3.dp.toPx()),
                                            trackY + trackHeight / 2,
                                        ),
                                    )
                                }
                            },
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatTime(durationMs),
                        color = MiniSecondary,
                        fontSize = 10.sp,
                    )
                }

                // Subtle dividing hairline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )

                // Synchronized Live Lyrics View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (lyricLines.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = ViolenceIcons.LyricsQuote,
                                contentDescription = null,
                                tint = MiniSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "No lyrics available",
                                color = MiniSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()

                        LaunchedEffect(activeLyricIndex) {
                            if (activeLyricIndex >= 0) {
                                listState.animateScrollToItem(
                                    index = (activeLyricIndex - 1).coerceAtLeast(0),
                                    scrollOffset = 0,
                                )
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(lyricLines) { index, line ->
                                val isActive = index == activeLyricIndex
                                if (line.isGap) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Text(
                                            text = "•••",
                                            color = if (isActive) accentBright else MiniSecondary.copy(alpha = 0.35f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                } else {
                                    val lineInteraction = remember { MutableInteractionSource() }
                                    val isLineHovered by lineInteraction.collectIsHoveredAsState()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(interactionSource = lineInteraction, indication = null) {
                                                DesktopMiniPlayerManager.onSeek(line.timeMs)
                                            }
                                            .background(
                                                if (isLineHovered && !isActive) Color.White.copy(alpha = 0.05f)
                                                else Color.Transparent,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = line.text,
                                            color = when {
                                                isActive -> Color.White
                                                isLineHovered -> Color.White.copy(alpha = 0.85f)
                                                else -> Color.White.copy(alpha = 0.45f)
                                            },
                                            fontSize = if (isActive) 15.5.sp else 13.5.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            lineHeight = if (isActive) 21.sp else 19.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPlayButton(
    isPlaying: Boolean,
    accentBright: Color,
    accentDark: Color,
    onClick: () -> Unit,
    size: Dp = 28.dp,
    iconSize: Dp = 17.dp,
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
            .size(size)
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
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun MiniIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    iconSize: Dp,
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
    size: Dp,
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
