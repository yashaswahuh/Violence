package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.glass.GlassLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * The components' structure on the JVM (Robolectric **native** graphics: real text metrics and real
 * paths, so sizes, wrapping and hit testing are meaningful; the pixel checks are in `ComponentsRenderTest`): accessibility semantics, touch
 * targets, layout sizes, clicks, and behaviour at 200 percent font.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class ComponentsUiTest {
    @get:Rule val rule = createComposeRule()

    @Composable
    private fun Env(level: GlassLevel = GlassLevel.SIMPLIFIED, glow: Boolean = true, content: @Composable () -> Unit) {
        MaterialTheme {
            BrutalPreviewHost(BrutalAccent.Preset(AccentPreset.YELLOW), glow, level) { content() }
        }
    }

    @Composable
    private fun FontScale(scale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = scale)) { content() }
    }

    // ---- StickerLabel ----

    @Test fun theStickerIsOneHeadingNodeReadAsOnePhrase() {
        rule.setContent { Env { StickerLabel(listOf("About", "Privacy"), metaLabel = "Version", metaValue = "0.4") } }
        val node = rule.onNodeWithContentDescription("About, Privacy, Version 0.4")
        node.assertExists()
        node.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        // The rows and the tilt are invisible to TalkBack: the visible words are not separate nodes.
        rule.onAllNodesWithText("ABOUT").assertCountEquals(0)
        rule.onAllNodesWithText("0.4").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("About, Privacy, Version 0.4").assertCountEquals(1)
    }

    @Test fun aStickerDescriptionCanBeOverridden() {
        rule.setContent { Env { StickerLabel(listOf("Nope"), description = "Privacy page") } }
        rule.onNodeWithContentDescription("Privacy page").assertExists()
    }

    @Test fun theStickerGrowsAndNeverLeavesItsWindowAtTwoHundredPercent() {
        rule.setContent {
            Env {
                Column(Modifier.width(360.dp)) {
                    FontScale(1f) { StickerLabel(listOf("About the privacy of your data", "Privacy"), Modifier.testTag("s1"), "Version", "0.4") }
                    FontScale(2f) { StickerLabel(listOf("About the privacy of your data", "Privacy"), Modifier.testTag("s2"), "Version", "0.4") }
                }
            }
        }
        val one = rule.onNodeWithTag("s1").getUnclippedBoundsInRoot()
        val two = rule.onNodeWithTag("s2").getUnclippedBoundsInRoot()
        assertTrue("fits at 1x: ${one.width}", one.width <= 360.dp + 1.dp)
        assertTrue("fits at 2x: ${two.width}", two.width <= 360.dp + 1.dp)
        assertTrue("grows taller at 2x (${one.height} -> ${two.height})", two.height > one.height)
    }

    @Test fun aNarrowWindowWrapsTheStickerInsteadOfClippingIt() {
        rule.setContent { Env { Box(Modifier.width(200.dp)) { StickerLabel(listOf("Extraordinarily long heading"), Modifier.testTag("s")) } } }
        val b = rule.onNodeWithTag("s").getUnclippedBoundsInRoot()
        assertTrue("width ${b.width}", b.width <= 200.dp + 1.dp)
        assertTrue("it wrapped onto several lines: height ${b.height}", b.height > 100.dp)
    }

    // ---- Wordmark ----

    @Test fun theWordmarkIsOneHeadingWithTheTagWhileExperimental() {
        rule.setContent { Env { Wordmark(showTag = true) } }
        rule.onNodeWithContentDescription("Violence, Experimental").assertExists()
        rule.onNodeWithContentDescription("Violence, Experimental").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test fun theTagIsHiddenWhenTheNewUiIsNotExperimental() {
        rule.setContent { Env { Wordmark(showTag = false) } }
        rule.onNodeWithContentDescription("Violence").assertExists()
        rule.onAllNodesWithContentDescription("Violence, Experimental").assertCountEquals(0)
    }

    @Test fun theTagFollowsTheFlagByDefault() {
        assertTrue(BrutalFlags.EXPERIMENTAL)
        rule.setContent { Env { Wordmark() } }
        rule.onNodeWithContentDescription("Violence, Experimental").assertExists()
    }

    @Test fun theMastheadRendersWithoutAFixedHeight() {
        rule.setContent {
            Env { Column(Modifier.width(360.dp)) { FontScale(2f) { WordmarkMasthead(Modifier.testTag("m")) } } }
        }
        val b = rule.onNodeWithTag("m").getUnclippedBoundsInRoot()
        assertTrue("fits the window: ${b.width}", b.width <= 360.dp + 1.dp)
        assertTrue(b.height > 60.dp)
    }

    // ---- VerticalLabel and DateStamp ----

    @Test fun aVerticalLabelSwapsItsLayoutSize() {
        rule.setContent {
            Env {
                Column {
                    Text("Weekend", Modifier.testTag("flat"))
                    VerticalLabel("Weekend", Modifier.testTag("vertical"))
                }
            }
        }
        val flat = rule.onNodeWithTag("flat").getUnclippedBoundsInRoot()
        val v = rule.onNodeWithTag("vertical").getUnclippedBoundsInRoot()
        // Different fonts (the label is in Figtree, the plain Text in the default), so only the shape is compared:
        // wide and short became tall and narrow.
        assertTrue("flat is wider than tall: ${flat.width} x ${flat.height}", flat.width > flat.height)
        assertTrue("vertical is taller than wide: ${v.width} x ${v.height}", v.height > v.width)
    }

    @Test fun aVerticalLabelEllipsisesAtItsMaximumHeight() {
        rule.setContent {
            Env {
                Box(Modifier.height(60.dp)) {
                    VerticalLabel("A very long title that must not outgrow its tile", Modifier.testTag("v"))
                }
            }
        }
        val b = rule.onNodeWithTag("v").getUnclippedBoundsInRoot()
        assertTrue("height ${b.height} <= 60 dp", b.height <= 60.dp + 1.dp)
    }

    @Test fun theLaneWidensAtTwoHundredPercentFont() {
        rule.setContent {
            Env {
                Column {
                    FontScale(1f) { VerticalLabel("Weekend", Modifier.testTag("v1")) }
                    FontScale(2f) { VerticalLabel("Weekend", Modifier.testTag("v2")) }
                }
            }
        }
        val one = rule.onNodeWithTag("v1").getUnclippedBoundsInRoot()
        val two = rule.onNodeWithTag("v2").getUnclippedBoundsInRoot()
        assertTrue("lane ${one.width} -> ${two.width}", two.width > one.width * 1.6f)
    }

    @Test fun aDateStampSpeaksTheDateInWords() {
        rule.setContent { Env { DateStamp(LocalDate.of(2022, 11, 28)) } }
        rule.onNode(hasContentDescription("2022", substring = true)).assertExists()
        // The visible digits are not read out as "twenty-eight eleven twenty-two".
        rule.onAllNodesWithText("28 11 22").assertCountEquals(0)
    }

    // ---- DurationChip ----

    @Test fun theChipSpeaksMinutesAndSeconds() {
        rule.setContent { Env { DurationChip(225_000) } }
        rule.onNodeWithContentDescription("3 minutes 45 seconds").assertExists()
    }

    @Test fun theChipSpeaksHoursWhenLong() {
        rule.setContent { Env { DurationChip(3_723_000) } }
        rule.onNodeWithContentDescription("1 hours 2 minutes 3 seconds").assertExists()
    }

    @Test fun theChipIsNeverFixedInHeight() {
        rule.setContent {
            Env {
                Column {
                    FontScale(1f) { DurationChip(225_000, Modifier.testTag("c1")) }
                    FontScale(2f) { DurationChip(225_000, Modifier.testTag("c2")) }
                }
            }
        }
        val one = rule.onNodeWithTag("c1").getUnclippedBoundsInRoot()
        val two = rule.onNodeWithTag("c2").getUnclippedBoundsInRoot()
        assertTrue("min 20 dp: ${one.height}", one.height >= 20.dp)
        assertTrue("grows with the font: ${one.height} -> ${two.height}", two.height > one.height)
    }

    // ---- PlayButton and ArtTile ----

    @Test fun thePlayButtonHasAFortyEightDpTargetEvenWhenDrawnSmaller() {
        var played = 0
        rule.setContent { Env { PlayButton(onClick = { played++ }, description = "Play Weekend", visualSize = 20.dp) } }
        rule.onNodeWithContentDescription("Play Weekend").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).assertHasClickAction()
        rule.onNodeWithContentDescription("Play Weekend").performClick()
        assertEquals(1, played)
    }

    @Test fun aTileAndItsPlayButtonAreTwoSeparateControls() {
        var opened = 0
        var played = 0
        rule.setContent {
            Env {
                Box(Modifier.width(160.dp)) {
                    ArtTile(
                        title = "Weekend", subtitle = "The Weeknd", artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue),
                        onClick = { opened++ }, onPlay = { played++ },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Weekend, The Weeknd").assertHasClickAction()
        rule.onNodeWithContentDescription("Play Weekend").assertHasClickAction()
        rule.onNodeWithContentDescription("Play Weekend").performClick()
        assertEquals("the play button did not open the tile", 0, opened)
        assertEquals(1, played)
        rule.onNodeWithContentDescription("Weekend, The Weeknd").performClick()
        assertEquals(1, opened)
        assertEquals("the tile did not play", 1, played)
    }

    @Test fun aTileKeepsItsAspectWithinTheMockupsRange() {
        rule.setContent {
            Env {
                Column(Modifier.width(160.dp)) {
                    ArtTile("a", Modifier.testTag("sq"), artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), heightToWidth = 0.5f)
                    ArtTile("b", Modifier.testTag("tall"), artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), heightToWidth = 1.5f)
                    ArtTile("c", Modifier.testTag("max"), artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), heightToWidth = 9f)
                }
            }
        }
        fun ratio(tag: String) = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot().let { it.height.value / it.width.value }
        assertEquals("below 1.0 is lifted to square", 1.0f, ratio("sq"), 0.02f)
        assertEquals(1.5f, ratio("tall"), 0.02f)
        assertEquals("above 1.8 is capped", 1.8f, ratio("max"), 0.02f)
    }

    @Test fun aTileWithoutAnActionIsNotClickable() {
        rule.setContent { Env { Box(Modifier.width(120.dp)) { ArtTile("Static", artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue)) } } }
        rule.onNodeWithContentDescription("Static").assertExists()
        assertTrue(rule.onNodeWithContentDescription("Static").fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick) == null)
    }

    @Test fun theLabelLaneIsHiddenFromTalkBackAndTheTileSpeaksTheDate() {
        rule.setContent {
            Env {
                Box(Modifier.width(220.dp)) {
                    LabeledArtTile("Weekend", date = LocalDate.of(2023, 2, 1)) { m ->
                        ArtTile("Weekend", m, subtitle = spokenDate(LocalDate.of(2023, 2, 1)), artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), onClick = {})
                    }
                }
            }
        }
        // One "Weekend" the user can reach, and it carries the date in words.
        rule.onNode(hasContentDescription("Weekend, February 1, 2023")).assertExists()
        rule.onAllNodesWithText("Weekend").assertCountEquals(0)
    }

    // ---- TrackRow ----

    @Test fun aTrackRowIsOnePhraseWithTitleArtistAndDuration() {
        var clicks = 0
        rule.setContent { Env { TrackRow("Too Young", "Post Malone", onClick = { clicks++ }, durationMs = 225_000, artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue)) } }
        val row = rule.onNodeWithContentDescription("Too Young, Post Malone, 3 minutes 45 seconds")
        row.assertHasClickAction().performClick()
        assertEquals(1, clicks)
        // The chip is not a separate stop.
        rule.onAllNodesWithContentDescription("3 minutes 45 seconds").assertCountEquals(0)
    }

    @Test fun longPressIsTheMenuHookAndTheOverflowButtonIsItsVisibleEquivalent() {
        var longPressed = 0
        var overflowed = 0
        rule.setContent {
            Env {
                TrackRow(
                    "Too Young", "Post Malone", onClick = {}, durationMs = 225_000,
                    artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue),
                    onLongClick = { longPressed++ }, onOverflow = { overflowed++ },
                )
            }
        }
        rule.onNodeWithContentDescription("Too Young, Post Malone, 3 minutes 45 seconds").performTouchInput { longClick() }
        assertEquals(1, longPressed)
        val more = rule.onNodeWithContentDescription("More options for Too Young")
        more.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, overflowed)
    }

    @Test fun theRowOffersTheMenuAsACustomActionForTouchExploration() {
        rule.setContent {
            Env {
                TrackRow("T", "A", onClick = {}, artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), onOverflow = {})
            }
        }
        val config = rule.onNodeWithContentDescription("T, A").fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsActions.CustomActions))
    }

    @Test fun aTrackRowIsNeverAFixedHeight() {
        rule.setContent {
            Env {
                Column(Modifier.width(360.dp)) {
                    FontScale(1f) { TrackRow("A song with a rather long title indeed", "An artist", onClick = {}, durationMs = 1000, artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), modifier = Modifier.testTag("r1")) }
                    FontScale(2f) { TrackRow("A song with a rather long title indeed", "An artist", onClick = {}, durationMs = 1000, artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), modifier = Modifier.testTag("r2")) }
                }
            }
        }
        val one = rule.onNodeWithTag("r1").getUnclippedBoundsInRoot()
        val two = rule.onNodeWithTag("r2").getUnclippedBoundsInRoot()
        assertTrue("at least 72 dp: ${one.height}", one.height >= 72.dp)
        assertTrue("grows at 2x: ${one.height} -> ${two.height}", two.height > one.height)
        assertTrue(two.width <= 360.dp + 1.dp)
    }

    // ---- Rail controls ----

    @Test fun aRailTabIsASelectableTabWithItsPosition() {
        var picked = 0
        rule.setContent {
            Env {
                Column {
                    GenreRailTab("Jazz", selected = false, onClick = { picked++ }, index = 1, count = 5, modifier = Modifier.testTag("jazz"))
                    GenreRailTab("Hip-Hop", selected = true, onClick = {}, index = 4, count = 5, modifier = Modifier.testTag("hip"))
                }
            }
        }
        rule.onNodeWithTag("hip").assertIsSelected()
        rule.onNodeWithTag("jazz").assertIsNotSelected().assertHasClickAction()
        rule.onNodeWithTag("jazz").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))
        rule.onNodeWithTag("jazz").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo))
        rule.onNodeWithTag("jazz").performClick()
        assertEquals(1, picked)
    }

    @Test fun aRailTabIsFortyEightWideAndAsTallAsItsLabelNeeds() {
        rule.setContent {
            Env {
                Column {
                    GenreRailTab("R&B", false, {}, Modifier.testTag("short"))
                    GenreRailTab("Hip-Hop", false, {}, Modifier.testTag("long"))
                    FontScale(2f) { GenreRailTab("R&B", false, {}, Modifier.testTag("big")) }
                }
            }
        }
        val short = rule.onNodeWithTag("short").getUnclippedBoundsInRoot()
        val long = rule.onNodeWithTag("long").getUnclippedBoundsInRoot()
        val big = rule.onNodeWithTag("big").getUnclippedBoundsInRoot()
        assertEquals(48f, short.width.value, 0.6f)
        assertTrue("never under 48 dp tall: ${short.height}", short.height >= 48.dp)
        assertTrue("a longer label is taller: ${short.height} < ${long.height}", long.height > short.height)
        assertTrue("a larger font makes it taller: ${short.height} -> ${big.height}", big.height > short.height)
        // The width is the 48 dp touch target and stays it: the label's lane (its line height) fits inside.
        assertEquals("still 48 dp wide at 200 percent", 48f, big.width.value, 0.6f)
    }

    @Test fun roundGlassButtonsAreFortyEightAndTheSearchOneIsTall() {
        var clicked = 0
        rule.setContent {
            Env {
                Column {
                    RoundGlassButton(Icons.Rounded.Search, "Search", { clicked++ }, Modifier.testTag("round"), state = "off")
                    RoundGlassButton(Icons.Rounded.Search, "Search tall", {}, Modifier.testTag("tall"), tall = true)
                }
            }
        }
        val round = rule.onNodeWithTag("round").getUnclippedBoundsInRoot()
        val tall = rule.onNodeWithTag("tall").getUnclippedBoundsInRoot()
        assertEquals(48f, round.width.value, 0.6f)
        assertEquals(48f, round.height.value, 0.6f)
        assertEquals(72f, tall.height.value, 0.6f)
        rule.onNodeWithTag("round").assertHasClickAction().performClick()
        assertEquals(1, clicked)
        rule.onNodeWithTag("round").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "off"))
    }

    // ---- Masonry ----

    @Test fun masonryPlacesTilesInTwoColumnsWithALeadOnTheSecond() {
        val items = (1..6).toList()
        rule.setContent {
            Env {
                Box(Modifier.width(360.dp).height(900.dp)) {
                    MasonryColumns(items = items, key = { it }) { n, m ->
                        ArtTile("tile $n", m.testTag("t$n"), artwork = galleryArt(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), heightToWidth = 1f + (n % 3) * 0.3f)
                    }
                }
            }
        }
        val t1 = rule.onNodeWithTag("t1").getUnclippedBoundsInRoot()
        val t2 = rule.onNodeWithTag("t2").getUnclippedBoundsInRoot()
        assertTrue("two columns: ${t1.left} vs ${t2.left}", t2.left > t1.left + 60.dp)
        assertTrue("the second column starts lower: ${t1.top} vs ${t2.top}", t2.top > t1.top)
    }

    // ---- States ----

    @Test fun loadingIsAnnouncedOnceAndSkeletonsAreSilent() {
        rule.setContent { Env { BrutalLoadingGroup { BrutalSkeletonRow(); BrutalSkeletonRow(); BrutalSkeletonTile() } } }
        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(1)
        rule.onNodeWithContentDescription("Loading").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
    }

    @Test fun theErrorStateOffersARetryAndIsAnAssertiveLiveRegion() {
        var retried = 0
        rule.setContent { Env { BrutalErrorState(message = "The server did not answer.", onRetry = { retried++ }) } }
        rule.onNodeWithText("The server did not answer.").assertExists()
        rule.onNodeWithText("Try again").assertHasClickAction().performClick()
        assertEquals(1, retried)
        rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, androidx.compose.ui.semantics.LiveRegionMode.Assertive)).assertCountEquals(1)
    }

    @Test fun theEmptyStateShowsItsActionOnlyWhenGivenOne() {
        var acted = 0
        rule.setContent { Env { Column { BrutalEmptyState(title = "Nothing", body = "Play something.", actionLabel = "Browse", onAction = { acted++ }) } } }
        rule.onNodeWithText("Play something.").assertExists()
        rule.onNodeWithText("Browse").performClick()
        assertEquals(1, acted)
    }

    @Test fun anEmptyStateWithoutAnActionHasNoButton() {
        rule.setContent { Env { BrutalEmptyState(title = "Nothing", body = "Play something.") } }
        rule.onAllNodesWithText("Browse").assertCountEquals(0)
    }

    // ---- Every component reads the page colour and never a fixed height ----

    @Test fun everyComponentComposesAtTwoHundredPercentInAPhoneWidth() {
        rule.setContent {
            Env {
                FontScale(2f) {
                    BrutalComponentsGallery(BrutalAccent.Preset(AccentPreset.YELLOW), glow = true, modifier = Modifier.width(360.dp).testTag("gallery"))
                }
            }
        }
        val b = rule.onNodeWithTag("gallery").getUnclippedBoundsInRoot()
        assertTrue("fits a 360 dp phone: ${b.width}", b.width <= 360.dp + 1.dp)
    }
}
