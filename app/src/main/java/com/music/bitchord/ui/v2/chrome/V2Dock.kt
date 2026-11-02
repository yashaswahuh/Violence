package com.music.bitchord.ui.v2.chrome

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.v2.a11y.v2Interactive
import com.music.bitchord.ui.v2.glass.v2Glass
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Dimens
import com.music.bitchord.ui.v2.theme.V2Shapes
import com.music.bitchord.ui.v2.theme.V2Type

/**
 * The new UI's navigation: Home, Explore, the **orb** (the mini player), Library, Search, in a
 * floating glass dock in the thumb zone (direction A, Lumen).
 *
 * - The four tabs are plain 48 dp targets with a label (labels wrap to two lines instead of
 *   clipping, and the dock grows with the font size).
 * - The orb is cut into the dock: tap opens the player, long-press shares the song, and the
 *   transport (play or pause, next, previous) is offered to TalkBack as custom actions; every one
 *   of these also exists as a visible control in the player.
 * - The orb sits outside the glass surface so the glass's clip does not cut it.
 *
 * Tab indexes are the app's own (0 Home, 1 Explore, 2 Library, 3 Search), so the dock calls the
 * same selection logic as the current bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun V2Dock(
    tabLabels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navLabel = stringResource(R.string.v2_dock_label)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = V2Dimens.Gap4, end = V2Dimens.Gap4, bottom = V2Dimens.Gap3, top = V2Dimens.Gap5)
            .semantics { contentDescription = navLabel },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = V2Dimens.DockMinHeight)
                .v2Glass(V2Shapes.Dock)
                .padding(horizontal = V2Dimens.Gap1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DockTab(Icons.Rounded.Home, tabLabels.getOrElse(0) { "" }, selectedIndex == 0) { onTabSelected(0) }
            DockTab(Icons.Rounded.Explore, tabLabels.getOrElse(1) { "" }, selectedIndex == 1) { onTabSelected(1) }
            Spacer(Modifier.width(V2Dimens.OrbSize + V2Dimens.Gap2))
            DockTab(Icons.Rounded.LibraryMusic, tabLabels.getOrElse(2) { "" }, selectedIndex == 2) { onTabSelected(2) }
            DockTab(Icons.Rounded.Search, tabLabels.getOrElse(3) { "" }, selectedIndex == 3) { onTabSelected(3) }
        }
        V2Orb(
            song = song,
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onExpand = onExpand,
            onLongPress = onLongPress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -(V2Dimens.OrbSize / 3)),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DockTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalV2Colors.current
    val tint = if (selected) colors.voltageText else colors.ink2
    Column(
        modifier = Modifier
            .weight(1f)
            .v2Interactive(label = label, role = Role.Tab, onClick = onClick, selected = selected)
            .padding(vertical = V2Dimens.Gap2, horizontal = V2Dimens.Gap1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = V2Type.caption,
            color = tint,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The mini player as a circle: artwork in a ring, 64 dp, with the transport as accessibility actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun V2Orb(
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalV2Colors.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val description = if (song != null) {
        stringResource(R.string.v2_dock_orb_playing, song.title, song.artist)
    } else {
        stringResource(R.string.v2_dock_orb_idle)
    }
    val hint = stringResource(R.string.v2_dock_orb_share_hint)
    val playLabel = stringResource(if (isPlaying) R.string.v2_action_pause else R.string.v2_action_play)
    val nextLabel = stringResource(R.string.v2_action_next)
    val previousLabel = stringResource(R.string.v2_action_previous)
    val ring = if (isPlaying) colors.voltageText else colors.outline
    Box(
        modifier = modifier
            .size(V2Dimens.OrbSize)
            .defaultMinSize(V2Dimens.MinTouch, V2Dimens.MinTouch)
            .clip(CircleShape)
            .background(colors.surface, CircleShape)
            .drawBehind {
                val w = 3.dp.toPx()
                drawArc(
                    color = ring,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    style = Stroke(width = w, cap = StrokeCap.Round),
                )
            }
            .then(if (focused) Modifier.border(V2Dimens.FocusRingWidth, colors.focusRing, CircleShape) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (song != null) stateDescription = hint
                if (song != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(playLabel) { onPlayPause(); true },
                        CustomAccessibilityAction(nextLabel) { onNext(); true },
                        CustomAccessibilityAction(previousLabel) { onPrevious(); true },
                    )
                }
            }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = song != null,
                role = Role.Button,
                onClick = onExpand,
                onLongClick = onLongPress,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (song?.thumbnailUrl != null) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CircleShape),
            )
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(26.dp))
        }
    }
}


/**
 * [V2Dock] wired to the media controller. Exists so `BitChordApp` (already close to the JVM's
 * method-size limit) carries one small call instead of a block of lambdas.
 */
@Composable
fun V2DockHost(
    tabLabels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    controller: androidx.media3.session.MediaController?,
    onExpand: () -> Unit,
    onShare: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    V2Dock(
        tabLabels = tabLabels,
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        song = song,
        isPlaying = isPlaying,
        onPlayPause = onPlayPause,
        onNext = { controller?.seekToNextMediaItem() },
        onPrevious = { controller?.seekToPrevious() },
        onExpand = onExpand,
        onLongPress = { song?.let(onShare) },
        modifier = modifier,
    )
}

/** Whether the placeholder Home gives way to the classic one (UI-1 only; process-wide, resets with the variant). */
object V2HomeState {
    var classic by androidx.compose.runtime.mutableStateOf(false)
}
