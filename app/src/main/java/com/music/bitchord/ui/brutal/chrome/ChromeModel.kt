package com.music.bitchord.ui.brutal.chrome

import androidx.annotation.StringRes
import com.music.bitchord.R
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.ui.navigation.AppRoute

/**
 * The pure rules of the new navigation chrome and pages. Nothing here touches Compose, so the plain
 * JVM tests can pin them down.
 */

/** Every non-tab page the Hub lists, in the order it lists them, with the route it opens. */
enum class HubTarget(@StringRes val label: Int, val route: AppRoute) {
    SETTINGS(R.string.brutal_hub_settings, AppRoute.Settings),
    DOWNLOADS(R.string.brutal_hub_downloads, AppRoute.Downloads),
    HISTORY(R.string.brutal_hub_history, AppRoute.History),
    RECENT(R.string.brutal_hub_recent, AppRoute.Recent),
    REPLAY(R.string.brutal_hub_replay, AppRoute.Replay),
    MOODS(R.string.brutal_hub_moods, AppRoute.Moods),
    EQUALIZER(R.string.brutal_hub_equalizer, AppRoute.Equalizer),
    SOURCES(R.string.brutal_hub_sources, AppRoute.Sources),
    LISTEN_TOGETHER(R.string.brutal_hub_listen_together, AppRoute.ListenTogether),
    FRIENDS(R.string.brutal_hub_friends, AppRoute.Friends),
    PROFILE(R.string.brutal_hub_profile, AppRoute.Profile),
}

/** One tile of the masonry grid: a shelf item plus the shelf it came from and the height it gets. */
data class PageTile(
    val key: String,
    val item: ShelfItem,
    val shelfTitle: String,
    /** Height over width, inside the mockup's 1.0..1.8 range. */
    val aspect: Float,
)

/** The heights tiles take in turn. Deterministic, so a tile keeps its shape when the list grows. */
internal val TILE_ASPECTS = floatArrayOf(1.0f, 1.27f, 1.5f, 1.15f, 1.4f, 1.0f, 1.65f)

/** The aspect of the tile at [index] in the page, not in its shelf: neighbours differ, the cycle repeats. */
fun tileAspect(index: Int): Float = TILE_ASPECTS[Math.floorMod(index, TILE_ASPECTS.size)]

/**
 * Shelves flattened into one list of tiles, [perShelf] items at most from each, in shelf order.
 * Keys are unique even when the feed repeats an item (the same album on two shelves), so the grid's
 * stable-key rule holds: a key is the shelf position, the item's own id and its position in the shelf.
 * Items with nothing to open or play (no video id and no browse id) are left out.
 */
fun tilesFor(shelves: List<HomeShelf>, perShelf: Int = 8): List<PageTile> {
    val out = ArrayList<PageTile>()
    shelves.forEachIndexed { shelfIndex, shelf ->
        shelf.items.asSequence()
            .filter { it.videoId != null || it.browseId != null }
            .take(perShelf)
            .forEachIndexed { i, item ->
                out += PageTile(
                    key = "$shelfIndex:${item.videoId ?: item.browseId}:$i",
                    item = item,
                    shelfTitle = shelf.title,
                    aspect = tileAspect(out.size),
                )
            }
    }
    return out
}

/** How many columns the grid gets at [widthDp]: two on a phone, more as the window widens. */
fun masonryColumnsFor(widthDp: Float): Int = when {
    widthDp >= 840f -> 4
    widthDp >= 600f -> 3
    else -> 2
}

/**
 * Whether the rail has to give way to the dock: at a font scale of 1.5 and up the vertical labels get
 * too tall for the height, so the spec's fallback applies (spec 6.4). The dock itself is not built yet
 * (PENDING.md), so for now this only decides that the rail scrolls and keeps its labels short.
 */
fun railNeedsDock(fontScale: Float): Boolean = fontScale >= com.music.bitchord.ui.brutal.theme.BrutalDimens.RAIL_TO_DOCK_FONT_SCALE

/** The list has run low: time to ask for the next page. */
fun nearEnd(lastVisibleIndex: Int, totalItems: Int, threshold: Int = 4): Boolean =
    totalItems > 0 && lastVisibleIndex >= totalItems - 1 - threshold
