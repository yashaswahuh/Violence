package com.music.bitchord.ui.v2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.v2.chrome.JamChip
import com.music.bitchord.ui.v2.chrome.V2Dock
import com.music.bitchord.ui.v2.chrome.V2HubGroups
import com.music.bitchord.ui.v2.chrome.V2JamChips
import com.music.bitchord.ui.v2.chrome.V2NowState
import com.music.bitchord.ui.v2.chrome.V2SeekBar
import com.music.bitchord.ui.v2.chrome.V2Shortcuts
import com.music.bitchord.ui.v2.chrome.formatClock
import com.music.bitchord.ui.v2.chrome.v2ShortcutRoutes
import com.music.bitchord.ui.v2.glass.GlassLevel
import com.music.bitchord.ui.v2.glass.LocalGlassLevel
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Colors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Compose UI tests of the custom UI's components on the JVM (Robolectric). They cover navigation
 * between destinations, the semantics of the main controls (what TalkBack reads) and the
 * orb-to-Now-Playing state. Not covered here: the real media controller, the glass pipeline and
 * anything needing a device; see `fork-docs/UI_TEST_PLAN.md`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
@org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)
class V2UiTest {
    @get:Rule val rule = createComposeRule()

    private val song = Song(videoId = "abc", title = "Neon Harbor", artist = "Sample Artist", thumbnailUrl = null)

    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalV2Colors provides V2Colors.Dark,
            LocalGlassLevel provides GlassLevel.SOLID,
        ) { MaterialTheme(content = content) }
    }

    private fun navigator() = AppNavigator(detailDepth = { 0 }, popDetail = { false }, clearDetails = {})

    // ---- the dock ----

    @Test fun dockTabsAreSelectableTabsWithLabels() {
        var picked = -1
        rule.setContent {
            Themed {
                V2Dock(
                    tabLabels = listOf("Home", "Explore", "Library", "Search"),
                    selectedIndex = 2, onTabSelected = { picked = it },
                    song = null, isPlaying = false, onPlayPause = {}, onNext = {}, onPrevious = {},
                    onExpand = {}, onLongPress = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Library").assertIsSelected()
        rule.onNodeWithContentDescription("Search").performClick()
        assertEquals(3, picked)
        rule.onNodeWithContentDescription("Home").performClick()
        assertEquals(0, picked)
    }

    @Test fun dockTargetsAreAtLeast48dp() {
        rule.setContent {
            Themed {
                V2Dock(
                    tabLabels = listOf("Home", "Explore", "Library", "Search"),
                    selectedIndex = 0, onTabSelected = {},
                    song = null, isPlaying = false, onPlayPause = {}, onNext = {}, onPrevious = {},
                    onExpand = {}, onLongPress = {},
                )
            }
        }
        for (label in listOf("Home", "Explore", "Library", "Search")) {
            rule.onNodeWithContentDescription(label).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
    }

    @Test fun orbOpensThePlayerAndExposesTransportToTalkBack() {
        var expanded = false
        var played = 0
        rule.setContent {
            Themed {
                V2Dock(
                    tabLabels = listOf("Home", "Explore", "Library", "Search"),
                    selectedIndex = 0, onTabSelected = {},
                    song = song, isPlaying = true, onPlayPause = { played++ }, onNext = {}, onPrevious = {},
                    onExpand = { expanded = true }, onLongPress = {},
                )
            }
        }
        val orb = rule.onNodeWithContentDescription("Now playing: Neon Harbor by Sample Artist. Opens the player.")
        val actions = orb.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)
        assertNotNull(actions)
        assertEquals(listOf("Pause", "Next song", "Previous song"), actions!!.map { it.label })
        actions.first().action()
        assertEquals(1, played)
        orb.performClick()
        assertTrue(expanded)
    }

    @Test fun orbIsInertWithNothingPlaying() {
        var expanded = false
        rule.setContent {
            Themed {
                V2Dock(
                    tabLabels = listOf("Home", "Explore", "Library", "Search"),
                    selectedIndex = 0, onTabSelected = {},
                    song = null, isPlaying = false, onPlayPause = {}, onNext = {}, onPrevious = {},
                    onExpand = { expanded = true }, onLongPress = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Nothing is playing").performClick()
        assertFalse(expanded)
    }

    @Test fun nowPlayingPanelStartsClosed() = assertFalse(V2NowState.open)

    // ---- seek bar ----

    @Test fun seekBarReadsPositionAndOffersSkipActions() {
        val position = PlaybackPosition().apply { report(30_000L) }
        var sought = -1L
        rule.setContent {
            Themed {
                V2SeekBar(position = position, durationMs = 180_000L, accent = androidx.compose.ui.graphics.Color.Magenta, enabled = true, onSeek = { sought = it })
            }
        }
        val node = rule.onNodeWithContentDescription("Song position").fetchSemanticsNode()
        assertEquals("0:30 of 3:00", node.config.getOrNull(SemanticsProperties.StateDescription))
        val custom = node.config.getOrNull(SemanticsActions.CustomActions)!!
        assertEquals(listOf("Back 10 seconds", "Forward 10 seconds"), custom.map { it.label })
        custom[1].action()
        assertEquals(40_000L, sought)
        custom[0].action()
        assertEquals(20_000L, sought)
        rule.onNodeWithContentDescription("Song position").assertHeightIsAtLeast(48.dp)
    }

    @Test fun seekBarSkipsClampToTheSong() {
        val position = PlaybackPosition().apply { report(2_000L) }
        var sought = -1L
        rule.setContent {
            Themed { V2SeekBar(position, 5_000L, androidx.compose.ui.graphics.Color.Magenta, true, onSeek = { sought = it }) }
        }
        val custom = rule.onNodeWithContentDescription("Song position").fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions)!!
        custom[0].action()
        assertEquals(0L, sought)
        custom[1].action()
        assertEquals(5_000L, sought)
    }

    @Test fun clockFormat() {
        assertEquals("0:00", formatClock(-5))
        assertEquals("1:05", formatClock(65_000))
        assertEquals("1:01:01", formatClock(3_661_000))
    }

    // ---- Jam chips ----

    @Test fun jamChipsAnnounceCountAndHost() {
        var opened = false
        rule.setContent {
            Themed {
                V2JamChips(
                    listOf(JamChip("Mira", "headphones", "7c3aed", null, true), JamChip("Rob", null, null, null, false)),
                    onOpenJam = { opened = true },
                )
            }
        }
        rule.onNodeWithText("Mira · host", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Jam: 2 listening. Opens the Jam page.").performClick()
        assertTrue(opened)
    }

    // ---- destinations ----

    @Test fun everyDestinationIsReachableFromTheHub() {
        val required = listOf(
            AppRoute.Profile, AppRoute.Friends, AppRoute.ListenTogether, AppRoute.Receive, AppRoute.Recent,
            AppRoute.Downloads, AppRoute.History, AppRoute.Replay, AppRoute.Spotify, AppRoute.Sources,
            AppRoute.Equalizer, AppRoute.AccountScrobbling, AppRoute.Settings,
        )
        val inHub = V2HubGroups.flatMap { it.second }.map { it.route }
        for (r in required) assertTrue("$r is missing from the Hub", r in inHub)
    }

    @Test fun shortcutStripsOnlyPointAtRealRoutesAndTheHub() {
        val hubRoutes = V2HubGroups.flatMap { it.second }.map { it.route }.toSet()
        for (kind in V2Shortcuts.entries) {
            val chips = v2ShortcutRoutes(kind)
            assertTrue(chips.any { it.second == null }) // every strip can open the Hub
            for ((_, route) in chips) if (route != null) assertTrue("$route not in the Hub", route in hubRoutes)
        }
    }

    @Test fun hubRowsOpenTheirRoutesOnTheSharedNavigator() {
        val nav = navigator()
        for ((_, entries) in V2HubGroups) for (e in entries) {
            nav.clear()
            nav.push(e.route)
            assertEquals(e.route, nav.visibleRoute())
            assertTrue(nav.pop())
            assertEquals(null, nav.visibleRoute())
        }
    }

    @Test fun hubLabelsAreAllNonEmptyStrings() {
        rule.setContent {
            Themed {
                for ((heading, entries) in V2HubGroups) {
                    assertTrue(androidx.compose.ui.res.stringResource(heading).isNotBlank())
                    for (e in entries) assertTrue(androidx.compose.ui.res.stringResource(e.labelRes).isNotBlank())
                }
            }
        }
        rule.waitForIdle()
    }
}
