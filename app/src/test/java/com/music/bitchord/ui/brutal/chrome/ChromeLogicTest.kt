package com.music.bitchord.ui.brutal.chrome

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeLogicTest {
    private fun item(title: String, video: String? = title, browse: String? = null) =
        ShelfItem(title = title, subtitle = "sub", thumbnailUrl = null, videoId = video, browseId = browse)

    @Test fun tilesKeepShelfOrderAndCarryTheirShelf() {
        val tiles = tilesFor(listOf(HomeShelf("A", listOf(item("a1"), item("a2"))), HomeShelf("B", listOf(item("b1")))))
        assertEquals(listOf("a1", "a2", "b1"), tiles.map { it.item.title })
        assertEquals(listOf("A", "A", "B"), tiles.map { it.shelfTitle })
    }

    @Test fun keysAreUniqueEvenWhenTheFeedRepeatsAnItem() {
        val same = item("dup")
        val tiles = tilesFor(listOf(HomeShelf("A", listOf(same, same)), HomeShelf("B", listOf(same))))
        assertEquals(3, tiles.size)
        assertEquals(3, tiles.map { it.key }.toSet().size)
    }

    @Test fun keysSurviveGrowthOfTheListAtTheEnd() {
        val a = listOf(HomeShelf("A", listOf(item("x"), item("y"))))
        val grown = a + HomeShelf("B", listOf(item("z")))
        assertEquals(tilesFor(a).map { it.key }, tilesFor(grown).take(2).map { it.key })
    }

    @Test fun anItemWithNothingToOpenIsLeftOut() {
        val dead = item("dead", video = null, browse = null)
        val album = item("album", video = null, browse = "MPRE1")
        assertEquals(listOf("album"), tilesFor(listOf(HomeShelf("A", listOf(dead, album)))).map { it.item.title })
    }

    @Test fun eachShelfIsCapped() {
        val shelf = HomeShelf("A", (1..30).map { item("t$it") })
        assertEquals(8, tilesFor(listOf(shelf)).size)
        assertEquals(12, tilesFor(listOf(shelf), perShelf = 12).size)
    }

    @Test fun tileAspectsStayInTheMockupRangeAndVary() {
        val aspects = (0..40).map { tileAspect(it) }
        assertTrue(aspects.all { it in 1.0f..1.8f })
        assertTrue(aspects.toSet().size >= 5)
        assertEquals(tileAspect(3), tileAspect(3 + TILE_ASPECTS.size), 0f)
        assertTrue(tileAspect(-1) in 1.0f..1.8f)
    }

    @Test fun columnsGrowWithTheWindow() {
        assertEquals(2, masonryColumnsFor(360f))
        assertEquals(2, masonryColumnsFor(599f))
        assertEquals(3, masonryColumnsFor(600f))
        assertEquals(4, masonryColumnsFor(900f))
    }

    @Test fun theNextPageIsAskedForNearTheEnd() {
        assertFalse(nearEnd(0, 0))
        assertFalse(nearEnd(2, 30))
        assertTrue(nearEnd(26, 30))
        assertTrue(nearEnd(29, 30))
    }

    @Test fun theRailGivesWayToTheDockAtLargeFonts() {
        assertFalse(railNeedsDock(1.0f))
        assertFalse(railNeedsDock(1.49f))
        assertTrue(railNeedsDock(1.5f))
        assertTrue(railNeedsDock(2.0f))
    }

    @Test fun theHubListsEachPageOnceWithItsOwnLabel() {
        val routes = HubTarget.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
        val labels = HubTarget.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
        assertEquals(HubTarget.SETTINGS, HubTarget.entries.first())
    }
}
