package com.music.bitchord.ui.brutal.chrome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.BrutalRailSide
import com.music.bitchord.ui.brutal.components.GenreRailTab
import com.music.bitchord.ui.brutal.components.RoundGlassButton
import com.music.bitchord.ui.brutal.components.brutalClickable
import com.music.bitchord.ui.brutal.components.nonLiveGlassLevel
import com.music.bitchord.ui.brutal.glass.GlassKind
import com.music.bitchord.ui.brutal.glass.brutalGlass
import com.music.bitchord.ui.brutal.glass.brutalHalo
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import com.music.bitchord.ui.navigation.AppRoute

/** What the PLAYING bar shows. Plain values, so the chrome has no idea where the player lives. */
data class BrutalNowPlaying(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
)

/**
 * The new look's navigation, drawn over the pages: the right-edge (or left, by setting) rail of
 * rotated tabs with the collapse and search buttons, the PLAYING bar at the foot, and the Hub that
 * lists every page that is not a tab. It owns no player and no navigation state: everything comes in
 * as plain values and plain lambdas, so a variant switch recreates nothing that matters.
 *
 * Touch: only the controls themselves take touches; the space between them falls through to the page.
 * The rail scrolls when the height is short (large fonts) instead of clipping.
 */
@Composable
fun BrutalChrome(
    tabLabels: List<String>,
    selectedTab: Int,
    searchTab: Int,
    onTabSelected: (Int) -> Unit,
    nowPlaying: BrutalNowPlaying?,
    onPlayPause: () -> Unit,
    onExpandPlayer: () -> Unit,
    onOpenRoute: (AppRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val side by AppSettings.brutalRailSide.collectAsStateWithLifecycle()
    var collapsed by rememberSaveable { mutableStateOf(false) }
    var hubOpen by rememberSaveable { mutableStateOf(false) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    val barHeight = with(LocalDensity.current) { barHeightPx.toDp() }
    Box(modifier.fillMaxSize()) {
        BrutalRail(
            tabLabels = tabLabels,
            selectedTab = selectedTab,
            searchTab = searchTab,
            collapsed = collapsed,
            onToggleCollapsed = { collapsed = !collapsed },
            onTabSelected = onTabSelected,
            onOpenHub = { hubOpen = true },
            side = side,
            bottomClearance = if (nowPlaying != null) barHeight + 8.dp else 12.dp,
            modifier = Modifier.align(if (side == BrutalRailSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd),
        )
        if (nowPlaying != null) {
            BrutalPlayingBar(
                nowPlaying = nowPlaying,
                onPlayPause = onPlayPause,
                onExpand = onExpandPlayer,
                modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { barHeightPx = it.height },
            )
        }
        if (hubOpen) {
            BrutalHub(onClose = { hubOpen = false }, onOpen = { hubOpen = false; onOpenRoute(it) })
        }
    }
}

@Composable
private fun BrutalRail(
    tabLabels: List<String>,
    selectedTab: Int,
    searchTab: Int,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onOpenHub: () -> Unit,
    side: BrutalRailSide,
    bottomClearance: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val left = side == BrutalRailSide.LEFT
    val collapseLabel = stringResource(if (collapsed) R.string.brutal_nav_show else R.string.brutal_nav_hide)
    val collapsedState = stringResource(R.string.brutal_nav_collapsed)
    Box(
        modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 64.dp, bottom = bottomClearance),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            // The scroll clips to its bounds and the glass's glow reaches past each control: the
            // padding is inside the clip, so the glow is not cut off in a hard rectangle.
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RAIL_GLOW_ROOM, vertical = RAIL_GLOW_ROOM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BrutalDimens.RailGapTabs),
        ) {
            RoundGlassButton(
                icon = if (collapsed == left) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
                description = collapseLabel,
                onClick = onToggleCollapsed,
                state = if (collapsed) collapsedState else null,
            )
            if (!collapsed) {
                RoundGlassButton(
                    icon = Icons.Rounded.Search,
                    description = tabLabels.getOrNull(searchTab) ?: stringResource(R.string.brutal_nav_search),
                    onClick = { onTabSelected(searchTab) },
                    tall = true,
                    modifier = Modifier.padding(top = BrutalDimens.RailGapSearchToTabs - BrutalDimens.RailGapTabs),
                )
                tabLabels.forEachIndexed { index, label ->
                    if (index != searchTab) {
                        GenreRailTab(
                            text = label,
                            selected = index == selectedTab,
                            onClick = { onTabSelected(index) },
                            index = index,
                            count = tabLabels.size,
                            modifier = Modifier.padding(top = if (index == 0) BrutalDimens.RailGapSearchToTabs - BrutalDimens.RailGapTabs else 0.dp),
                        )
                    }
                }
                RoundGlassButton(
                    icon = Icons.Rounded.Apps,
                    description = stringResource(R.string.brutal_hub_open),
                    onClick = onOpenHub,
                    modifier = Modifier.padding(top = BrutalDimens.RailGapSearchToTabs - BrutalDimens.RailGapTabs),
                )
            }
        }
    }
}

/**
 * The mini player: art, the title over the artist, a PLAYING tag and the round accent play/pause
 * button. The bar is one tap target that opens the player; play/pause is a sibling control (a
 * clickable merges its children's semantics, so a nested button would be swallowed).
 */
@Composable
private fun BrutalPlayingBar(
    nowPlaying: BrutalNowPlaying,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val shape = BrutalShapes.Tile
    val openLabel = stringResource(R.string.brutal_open_player)
    val playingTag = stringResource(if (nowPlaying.isPlaying) R.string.brutal_playing else R.string.brutal_paused)
    val toggleLabel = stringResource(if (nowPlaying.isPlaying) R.string.brutal_action_pause else R.string.brutal_action_play)
    val spoken = "${nowPlaying.title}, ${nowPlaying.artist}, $playingTag"
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = BrutalDimens.ScreenEdge, vertical = 12.dp)
            .heightIn(min = BrutalDimens.MiniPlayerHeight)
            .brutalHalo(shape)
            .brutalGlass(shape, GlassKind.Floating),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = BrutalDimens.MiniPlayerHeight)
                .brutalClickable(onClick = onExpand, onClickLabel = openLabel, role = Role.Button, shape = shape)
                .padding(start = 12.dp, end = 12.dp + BrutalDimens.PauseCircle + 8.dp, top = 8.dp, bottom = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(BrutalDimens.BarArtwork)
                    .clip(BrutalShapes.ListArtwork)
                    .background(palette.glassBody),
            ) {
                AsyncImage(
                    model = nowPlaying.artworkUrl.artworkAt(BAR_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = playingTag.uppercase(java.util.Locale.getDefault()),
                    style = type.chip.copy(color = palette.accentText),
                    maxLines = 1,
                )
                Text(
                    text = nowPlaying.title,
                    style = type.barTitle.copy(color = palette.white),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nowPlaying.artist,
                    style = type.barArtist.copy(color = palette.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(BrutalDimens.PauseCircle)
                .brutalHalo(BrutalShapes.Stadium)
                .background(palette.accentFill, CircleShape)
                .brutalClickable(onClick = onPlayPause, role = Role.Button, shape = CircleShape)
                .semantics { contentDescription = toggleLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = palette.onAccent,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

private const val BAR_ART_PX = 160

/** Room around the rail's controls for their under-glow (21 dp) inside the scroll's clip. */
internal val RAIL_GLOW_ROOM = 12.dp

/**
 * The Hub: every page that is not a tab, in one glass panel over a scrim. Back or a tap outside
 * closes it. A row opens its route and closes the panel in one step.
 */
@Composable
private fun BrutalHub(onClose: () -> Unit, onOpen: (AppRoute) -> Unit) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    BackHandler(onBack = onClose)
    val closeLabel = stringResource(R.string.brutal_hub_close)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = closeLabel,
                role = Role.Button,
                onClick = onClose,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(BrutalDimens.ScreenEdge)
                // Taps inside the panel must not reach the scrim.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .brutalGlass(BrutalShapes.Tile, GlassKind.Panel, level = nonLiveGlassLevel())
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.brutal_hub_title),
                style = type.listTitle.copy(color = palette.accentText),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).semantics { heading() },
            )
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                HubTarget.entries.forEach { target ->
                    val label = stringResource(target.label)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .brutalClickable(onClick = { onOpen(target.route) }, role = Role.Button)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = type.listTitle.copy(color = palette.white))
                    }
                }
            }
        }
    }
}
