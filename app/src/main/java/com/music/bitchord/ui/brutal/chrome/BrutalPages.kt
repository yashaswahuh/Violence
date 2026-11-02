@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.music.bitchord.ui.brutal.chrome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.BrutalRailSide
import com.music.bitchord.ui.brutal.components.ArtTile
import com.music.bitchord.ui.brutal.components.BrutalButton
import com.music.bitchord.ui.brutal.components.BrutalEmptyState
import com.music.bitchord.ui.brutal.components.BrutalErrorState
import com.music.bitchord.ui.brutal.components.BrutalLoadingGroup
import com.music.bitchord.ui.brutal.components.BrutalSkeletonTile
import com.music.bitchord.ui.brutal.components.MASONRY_GLOW_ROOM
import com.music.bitchord.ui.brutal.components.MasonryColumns
import com.music.bitchord.ui.brutal.components.StickerLabel
import com.music.bitchord.ui.brutal.components.WordmarkMasthead
import com.music.bitchord.ui.brutal.components.brutalClickable
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import com.music.bitchord.ui.screens.LibraryLink
import com.music.bitchord.ui.components.PullToRefresh

/**
 * Listen now in the new look: the masthead and a sticker heading, then the feed's shelves flattened
 * into the broken masonry grid. The shelf each tile came from travels with the tap, as the classic
 * page does it, so the queue is named for its shelf.
 *
 * What it does not carry over (see the plan): the big lead cards, Top Picks, Find Your Mood and the
 * recents rows. Those need their own designs; every one of them is still reachable on the classic UI.
 */
@Composable
fun BrutalHomePage(
    state: UiState<List<HomeShelf>>,
    title: String,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    onLoadMore: (() -> Unit)?,
    loadingMore: Boolean,
    modifier: Modifier = Modifier,
) {
    val tiles = remember(state) { (state as? UiState.Success)?.data?.let { tilesFor(it) }.orEmpty() }
    TilePage(
        title = title,
        loading = state is UiState.Loading,
        error = (state as? UiState.Error)?.message,
        tiles = tiles,
        onItemClick = onItemClick,
        onItemLongPress = onItemLongPress,
        onRetry = onRetry,
        refreshing = refreshing,
        onRefresh = onRefresh,
        pullState = pullState,
        contentPadding = contentPadding,
        onLoadMore = onLoadMore.takeIf { !loadingMore },
        modifier = modifier,
    )
}

/**
 * Library in the new look: the same tile grid over the account's playlists, albums and the albums on
 * this device, with "New playlist" and the folder links (downloads, local files) above it. Signed out
 * it asks to sign in instead. Replay's entry card is not drawn here; Replay is in the Hub.
 */
@Composable
fun BrutalLibraryPage(
    signedIn: Boolean,
    state: UiState<LibraryPage>,
    title: String,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    links: List<LibraryLink>,
    deviceItems: List<ShelfItem>,
    modifier: Modifier = Modifier,
) {
    if (!signedIn && deviceItems.isEmpty()) {
        Box(modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            BrutalEmptyState(
                title = stringResource(R.string.brutal_library_signed_out_title),
                body = stringResource(R.string.brutal_library_signed_out_body),
                actionLabel = stringResource(R.string.brutal_sign_in),
                onAction = onSignIn,
            )
        }
        return
    }
    val tiles = remember(state, deviceItems) {
        val shelves = (state as? UiState.Success)?.data?.shelves.orEmpty()
        val device = if (deviceItems.isEmpty()) emptyList() else listOf(HomeShelf(title = "", items = deviceItems))
        tilesFor(shelves + device, perShelf = 24)
    }
    TilePage(
        title = title,
        loading = state is UiState.Loading,
        error = (state as? UiState.Error)?.message,
        tiles = tiles,
        onItemClick = { item, _ -> onItemClick(item) },
        onItemLongPress = onItemLongPress,
        onRetry = onRetry,
        refreshing = refreshing,
        onRefresh = onRefresh,
        pullState = pullState,
        contentPadding = contentPadding,
        onLoadMore = null,
        modifier = modifier,
        headerExtra = {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BrutalButton(text = stringResource(R.string.brutal_library_new_playlist), onClick = onNewPlaylist)
                links.forEach { link -> LinkRow(link, onClick = { onItemClick(link.item) }) }
            }
        },
    )
}

@Composable
private fun LinkRow(link: LibraryLink, onClick: () -> Unit) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = BrutalDimens.MinTouch)
            .brutalClickable(onClick = onClick, role = Role.Button),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(link.icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(24.dp))
        Text(link.item.title, style = type.listTitle.copy(color = palette.white))
    }
}

/** The one grid the two pages share: heading, optional extra header, tiles or a state. */
@Composable
private fun TilePage(
    title: String,
    loading: Boolean,
    error: String?,
    tiles: List<PageTile>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    onLoadMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
    headerExtra: (@Composable () -> Unit)? = null,
) {
    val side by AppSettings.brutalRailSide.collectAsStateWithLifecycle()
    val edge = MASONRY_GLOW_ROOM
    val lane = BrutalDimens.RailItemWidth + RAIL_GLOW_ROOM * 2
    val padding = PaddingValues(
        start = edge + if (side == BrutalRailSide.LEFT) lane else 0.dp,
        end = edge + if (side == BrutalRailSide.RIGHT) lane else 0.dp,
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateBottomPadding(),
    )
    val columns = masonryColumnsFor(LocalConfiguration.current.screenWidthDp.toFloat())
    val grid: LazyStaggeredGridState = rememberLazyStaggeredGridState()
    val more by rememberUpdatedState(onLoadMore)
    LaunchedEffect(grid, tiles.size) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { last ->
            if (nearEnd(last, tiles.size)) more?.invoke()
        }
    }
    val openLabel = stringResource(R.string.brutal_more_options)
    PullToRefresh(refreshing = refreshing, onRefresh = onRefresh, state = pullState, modifier = modifier) {
        MasonryColumns(
            items = tiles,
            key = { it.key },
            columns = columns,
            state = grid,
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
            header = {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    // An error is the one thing that must be on screen: it takes the masthead's place.
                    // Measured without a width limit and pulled left by the page gutter: the masthead is wider
                    // than the lane beside the rail, and its left end bleeds off the screen by design.
                    if (error == null) Box(
                        Modifier.fillMaxWidth().wrapContentWidth(Alignment.Start, unbounded = true).offset(x = -MASONRY_GLOW_ROOM),
                    ) { WordmarkMasthead() }
                    StickerLabel(
                        lines = listOf(title),
                        modifier = Modifier.padding(top = 12.dp).semantics { heading() },
                    )
                    headerExtra?.invoke()
                    when {
                        loading -> BrutalLoadingGroup(Modifier.padding(top = 16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(BrutalDimens.MasonryGap)) {
                                BrutalSkeletonTile(Modifier.weight(1f), heightToWidth = 1.27f)
                                BrutalSkeletonTile(Modifier.weight(1f), heightToWidth = 1.0f)
                            }
                        }
                        error != null -> BrutalErrorState(message = error, onRetry = onRetry)
                        tiles.isEmpty() -> BrutalEmptyState()
                    }
                }
            },
        ) { tile, tileModifier ->
            ArtTile(
                title = tile.item.title,
                subtitle = tile.item.subtitle.takeIf { it.isNotBlank() },
                artworkUrl = tile.item.thumbnailUrl,
                heightToWidth = tile.aspect,
                onClick = { onItemClick(tile.item, tile.shelfTitle) },
                onLongClick = onItemLongPress?.let { press -> { press(tile.item) } },
                onLongClickLabel = openLabel,
                onPlay = if (tile.item.videoId != null) ({ onItemClick(tile.item, tile.shelfTitle) }) else null,
                modifier = tileModifier,
            )
        }
    }
}
