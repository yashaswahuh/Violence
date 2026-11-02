package com.music.bitchord.ui.brutal.chrome

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.BrutalRailSide
import com.music.bitchord.ui.brutal.components.BrutalPreviewHost
import com.music.bitchord.ui.brutal.components.renderToBitmap
import com.music.bitchord.ui.brutal.components.saveRender
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/**
 * The navigation chrome and the two pages on the JVM (Robolectric native graphics): behaviour through
 * the accessibility tree, and PNG renders of the chrome over a page. No emulator, no device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w360dp-h740dp-xhdpi")
@ConscryptMode(ConscryptMode.Mode.OFF)
class ChromeUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val tabs = listOf("Home", "Explore", "Library", "Search")
    private val playing = BrutalNowPlaying("Too Young", "Post Malone", null, isPlaying = true, isLoading = false)

    @Before fun resetSide() { AppSettings.brutalRailSide.value = BrutalRailSide.RIGHT }

    @Composable
    private fun Env(content: @Composable () -> Unit) {
        MaterialTheme {
            BrutalPreviewHost(BrutalAccent.Preset(AccentPreset.YELLOW), glow = true, level = GlassLevel.SIMPLIFIED) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }

    private class Taps {
        var tab = -1
        var played = 0
        var expanded = 0
        var route: AppRoute? = null
    }

    @Composable
    private fun Chrome(taps: Taps, selected: Int = 0, now: BrutalNowPlaying? = playing) {
        BrutalChrome(
            tabLabels = tabs, selectedTab = selected, searchTab = 3,
            onTabSelected = { taps.tab = it },
            nowPlaying = now,
            onPlayPause = { taps.played++ },
            onExpandPlayer = { taps.expanded++ },
            onOpenRoute = { taps.route = it },
        )
    }

    @Test fun theRailListsTheTabsAndMarksTheSelectedOne() {
        rule.setContent { Env { Chrome(Taps(), selected = 2) } }
        rule.onNodeWithText("Home", ignoreCase = true).assertExists()
        rule.onNodeWithText("Library", ignoreCase = true).assertIsSelected()
    }

    @Test fun tappingATabReportsItsIndex() {
        val taps = Taps()
        rule.setContent { Env { Chrome(taps) } }
        rule.onNodeWithText("Explore", ignoreCase = true).performClick()
        assertEquals(1, taps.tab)
    }

    @Test fun theSearchButtonSelectsTheSearchTab() {
        val taps = Taps()
        rule.setContent { Env { Chrome(taps) } }
        rule.onNodeWithContentDescription("Search").performClick()
        assertEquals(3, taps.tab)
    }

    @Test fun theSearchTabIsNotDrawnTwiceInTheRail() {
        rule.setContent { Env { Chrome(Taps()) } }
        // The search button carries the word; the rail's rotated tabs are Home, Explore and Library.
        rule.onAllNodesWithText("Search", ignoreCase = true).assertCountEquals(0)
    }

    @Test fun collapsingHidesEverythingButTheCollapseButton() {
        rule.setContent { Env { Chrome(Taps()) } }
        rule.onNodeWithContentDescription("Hide navigation").performClick()
        rule.onAllNodesWithText("Home", ignoreCase = true).assertCountEquals(0)
        rule.onNodeWithContentDescription("Show navigation").performClick()
        rule.onNodeWithText("Home", ignoreCase = true).assertExists()
    }

    @Test fun thePlayingBarTogglesAndExpands() {
        val taps = Taps()
        rule.setContent { Env { Chrome(taps) } }
        rule.onNodeWithContentDescription("Pause").performClick()
        assertEquals(1, taps.played)
        rule.onNodeWithContentDescription("Too Young, Post Malone, Playing").performClick()
        assertEquals(1, taps.expanded)
    }

    @Test fun withNothingPlayingThereIsNoBar() {
        rule.setContent { Env { Chrome(Taps(), now = null) } }
        rule.onAllNodesWithContentDescription("Pause").assertCountEquals(0)
    }

    @Test fun theHubOpensARouteAndCloses() {
        val taps = Taps()
        rule.setContent { Env { Chrome(taps) } }
        rule.onNodeWithContentDescription("All pages").performClick()
        rule.onNodeWithText("Downloads").performClick()
        assertEquals(AppRoute.Downloads, taps.route)
        rule.onAllNodesWithText("Downloads").assertCountEquals(0)
    }

    @Test fun theHubListsEveryPage() {
        rule.setContent { Env { Chrome(Taps()) } }
        rule.onNodeWithContentDescription("All pages").performClick()
        for (t in listOf("Settings", "History", "Equalizer", "Sources")) rule.onNodeWithText(t).assertExists()
    }

    // ---- pages ----

    private fun shelves() = listOf(
        HomeShelf("Quick picks", listOf(
            ShelfItem("Alpha", "Artist A", null, "v1", null),
            ShelfItem("Beta", "Artist B", null, null, "MPRE_b"),
        )),
        HomeShelf("Mixes", listOf(ShelfItem("Gamma", "Artist C", null, "v3", null))),
    )

    @Composable
    private fun Home(state: UiState<List<HomeShelf>>, onClick: (ShelfItem, String) -> Unit = { _, _ -> }, onRetry: () -> Unit = {}) {
        BrutalHomePage(
            state = state, title = "Listen now", onItemClick = onClick, onItemLongPress = null, onRetry = onRetry,
            refreshing = false, onRefresh = {}, pullState = rememberPullToRefreshState(),
            contentPadding = PaddingValues(top = 80.dp, bottom = 140.dp), onLoadMore = null, loadingMore = false,
        )
    }

    @Test fun homeShowsTheTilesAndPassesTheShelfOnTap() {
        var got: Pair<String, String>? = null
        rule.setContent { Env { Home(UiState.Success(shelves()), onClick = { i, s -> got = i.title to s }) } }
        rule.onNodeWithContentDescription("Alpha, Artist A").assertExists()
        rule.onNodeWithContentDescription("Beta, Artist B").performClick()
        assertEquals("Beta" to "Quick picks", got)
    }

    @Test fun homeShowsLoading() {
        rule.setContent { Env { Home(UiState.Loading) } }
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test fun homeShowsAnErrorWithRetry() {
        var retried = 0
        rule.setContent { Env { Home(UiState.Error("No network"), onRetry = { retried++ }) } }
        rule.onNodeWithText("No network").assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test fun libraryWhenSignedOutAsksToSignIn() {
        var signIn = 0
        rule.setContent {
            Env {
                BrutalLibraryPage(
                    signedIn = false, state = UiState.Loading, title = "Library", onItemClick = {}, onItemLongPress = {},
                    onNewPlaylist = {}, onSignIn = { signIn++ }, onRetry = {}, refreshing = false, onRefresh = {},
                    pullState = rememberPullToRefreshState(), contentPadding = PaddingValues(), links = emptyList(), deviceItems = emptyList(),
                )
            }
        }
        rule.onNodeWithText("Sign in").performClick()
        assertEquals(1, signIn)
    }

    // ---- renders (PNG) ----

    @Composable
    private fun Screen(scale: Float, now: BrutalNowPlaying?) {
        MaterialTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = scale)) {
                BrutalPreviewHost(BrutalAccent.Preset(AccentPreset.YELLOW), glow = true, level = GlassLevel.SIMPLIFIED) {
                    Box(Modifier.fillMaxSize()) {
                        val many = (1..12).map { ShelfItem("Track $it", "Artist $it", null, "v$it", null) }
                        Home(UiState.Success(listOf(HomeShelf("Quick picks", many))))
                        Chrome(Taps(), now = now)
                    }
                }
            }
        }
    }

    private fun render(name: String, side: BrutalRailSide, scale: Float, now: BrutalNowPlaying? = playing) {
        AppSettings.brutalRailSide.value = side
        rule.setContent { Screen(scale, now) }
        val bmp = rule.renderToBitmap()
        saveRender(name, bmp)
        var lit = 0
        for (y in 0 until bmp.height step 4) for (x in 0 until bmp.width step 4) if ((bmp.getPixel(x, y) and 0xFFFFFF) != 0x08080A) lit++
        assertTrue("$name: the screen was drawn ($lit)", lit > 200)
    }

    @Test fun renderRailRight() = render("chrome-home-rail-right-1x", BrutalRailSide.RIGHT, 1f)
    @Test fun renderRailLeft() = render("chrome-home-rail-left-1x", BrutalRailSide.LEFT, 1f)
    @Test fun renderRailRight2x() = render("chrome-home-rail-right-2x", BrutalRailSide.RIGHT, 2f)
    @Test fun renderNothingPlaying() = render("chrome-home-no-player-1x", BrutalRailSide.RIGHT, 1f, now = null)
}
