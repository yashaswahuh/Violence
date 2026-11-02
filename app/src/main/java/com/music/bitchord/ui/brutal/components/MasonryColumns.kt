package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.theme.BrutalDimens

/** Content padding that leaves room for a tile's under-glow (21 dp) inside the grid's clip. */
val MASONRY_GLOW_ROOM: Dp = 24.dp

/**
 * The mockup's broken grid: columns of tiles with different heights and a small, deliberate overlap.
 *
 * **Approach.** One `LazyVerticalStaggeredGrid` with fixed columns, which is the platform's own
 * staggered layout: every item is placed in the shortest column, the heights come from each item's
 * own aspect ratio (give [ArtTile] a `heightToWidth` between 1.0 and 1.8), and only the visible
 * items are composed. Items have **stable keys** ([key]), so scroll position and item state survive
 * insertions, removals and reordering. One scroll container, no nested scrolling.
 *
 * **The "broken" part is cosmetic and bounded.**
 * - Each item is nudged by a deterministic offset derived from its key ([masonryJitter]: at most
 *   [overlap] vertically and half of it sideways), applied as a `graphicsLayer` translation. It
 *   moves pixels, not layout, so tiles may kiss or slightly overlap and scrolling and keys are
 *   unaffected; touches follow the moved tile.
 * - The first item of every odd column starts [columnLead] lower, so the tops are not level.
 *
 * **Limits (read before using it for more).**
 * - The overlap is drawn, not laid out: an item cannot cover a large part of its neighbour, and
 *   overlapped corners are painted in item order (later items on top).
 * - Heights come from aspect ratios, not from measuring content: an item whose content needs more
 *   room than its ratio gives will clip or must not be placed here (use a list row instead).
 * - Items cannot span columns and the column count is fixed per call; choose it from the window
 *   width (two on a phone, more on a tablet).
 * - The vertical label lane ([LabeledArtTile]) takes width from each item; at large fonts the tiles
 *   get narrower.
 * - Staggered grids do not support sticky headers, and a [columns] change rebuilds the layout.
 * - A lazy grid clips to its bounds, and a tile's under-glow reaches 21 dp past its edge: keep the
 *   content padding at [MASONRY_GLOW_ROOM] (24 dp, the default) or more, or the glow is cut off at the
 *   top, bottom and sides of the grid.
 */
@Composable
fun <T> MasonryColumns(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(MASONRY_GLOW_ROOM),
    spacing: Dp = BrutalDimens.MasonryGap,
    overlap: Dp = 6.dp,
    columnLead: Dp = 24.dp,
    /** An optional full-width block above the first tile (a page heading); it scrolls with the grid. */
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalItemSpacing = spacing,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        if (header != null) {
            item(key = "masonry-header", span = StaggeredGridItemSpan.FullLine) { header() }
        }
        items(items = items, key = { key(it) }) { item ->
            val k = key(item)
            val jitter = remember(k) { masonryJitter(k.hashCode(), overlap.value) }
            val index = items.indexOf(item)
            val lead = if (index in 0 until columns && index % 2 == 1) Modifier.padding(top = columnLead) else Modifier
            itemContent(
                item,
                lead.graphicsLayer {
                    translationX = jitter.xDp * density
                    translationY = jitter.yDp * density
                },
            )
        }
    }
}
