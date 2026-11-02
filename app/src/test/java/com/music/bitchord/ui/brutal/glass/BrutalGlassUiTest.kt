package com.music.bitchord.ui.brutal.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * The glass kit on the JVM (Robolectric, legacy graphics: draws are no-ops, so these tests are about
 * decisions, lifecycles and crashes, not pixels; the pixel checks are in `BrutalGlassRenderTest`).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class BrutalGlassUiTest {
    @get:Rule val rule = createComposeRule()

    private val seen = mutableListOf<Pair<GlassKind, GlassPlan>>()

    @Before fun setUp() {
        AppSettings.reduceDynamicBlur.value = false
        seen.clear()
    }

    /** The kit's inputs. [withBackdrop] false means `LocalAppBackdrop` is NOT provided (it throws if read). */
    @Composable
    private fun Env(
        level: GlassLevel,
        glow: Boolean = true,
        budget: GlassBudget = GlassBudget(),
        withBackdrop: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val observer: (GlassKind, GlassPlan) -> Unit = { k, p -> seen += k to p }
        val provided = arrayOf(
            LocalBrutalPalette provides BrutalPalette.of(BrutalAccent.Default, glow),
            LocalGlassLevel provides level,
            LocalGlassBudget provides budget,
            LocalGlassPlanObserver provides observer,
            LocalLiquidGlassEnabled provides (level == GlassLevel.FULL),
        )
        MaterialTheme {
            CompositionLocalProvider(*provided) {
                if (withBackdrop) {
                    val backdrop = rememberLayerBackdrop()
                    CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
                        Box(Modifier.layerBackdrop(backdrop).background(Color(0xFF08080A))) { content() }
                    }
                } else {
                    Box(Modifier.background(Color(0xFF08080A))) { content() }
                }
            }
        }
    }

    @Composable
    private fun Surface(kind: GlassKind, tag: String = kind.name) {
        Box(
            Modifier
                .size(width = 200.dp, height = 60.dp)
                .brutalGlass(if (kind == GlassKind.Tile) BrutalShapes.Tile else BrutalShapes.Stadium, kind)
                .semantics { contentDescription = tag },
        ) { Text(tag) }
    }

    // ---- renders at every level without crashing ----

    @Test fun everyKindRendersAtFullWithABackdrop() {
        // Glow off and on, in one composition (a test may call setContent only once).
        rule.setContent {
            Column {
                for (glow in listOf(false, true)) Env(GlassLevel.FULL, glow) { Column { GlassKind.entries.forEach { Surface(it) } } }
            }
        }
        rule.waitForIdle()
        GlassKind.entries.forEach { rule.onAllNodesWithContentDescription(it.name).assertCountEquals(2) }
        // Four live-capable Floating surfaces would exceed the budget, but each Env has its own: all live.
        assertEquals(2, seen.count { it.first == GlassKind.Floating && it.second.backdrop == BackdropSource.LIVE })
    }

    @Test fun everyKindRendersAtSimplifiedAndSolid() {
        rule.setContent {
            Column {
                for (level in listOf(GlassLevel.SIMPLIFIED, GlassLevel.SOLID)) for (glow in listOf(false, true)) {
                    Env(level, glow, withBackdrop = true) { Column { GlassKind.entries.forEach { Surface(it) } } }
                }
            }
        }
        rule.waitForIdle()
        GlassKind.entries.forEach { rule.onAllNodesWithContentDescription(it.name).assertCountEquals(4) }
    }

    // ---- no backdrop needed where none is used ----

    @Test fun solidSimplifiedTilesAndChipsNeverReadTheBackdrop() {
        // LocalAppBackdrop is not provided, and reading it throws: a surface that touched it would crash here.
        rule.setContent {
            Env(GlassLevel.SOLID, withBackdrop = false) { Column { GlassKind.entries.forEach { Surface(it, "solid-${it.name}") } } }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("solid-Floating").assertExists()
    }

    @Test fun simplifiedNeverReadsTheBackdropEither() {
        rule.setContent {
            Env(GlassLevel.SIMPLIFIED, withBackdrop = false) { Column { GlassKind.entries.forEach { Surface(it) } } }
        }
        rule.waitForIdle()
        assertTrue(seen.all { it.second.backdrop == BackdropSource.NONE })
    }

    @Test fun tilesAndChipsAtFullStillReadNoBackdrop() {
        rule.setContent {
            Env(GlassLevel.FULL, withBackdrop = false) { Column { Surface(GlassKind.Tile); Surface(GlassKind.Chip) } }
        }
        rule.waitForIdle()
        assertEquals(2, seen.size)
        assertTrue(seen.all { it.second.backdrop == BackdropSource.NONE && it.second.level == GlassLevel.SIMPLIFIED })
    }

    // ---- SOLID draws no glass ----

    @Test fun solidPlansHaveNoGlassModifiers() {
        rule.setContent { Env(GlassLevel.SOLID, withBackdrop = false) { Column { GlassKind.entries.forEach { Surface(it) } } } }
        rule.waitForIdle()
        assertEquals(GlassKind.entries.size, seen.size)
        for ((kind, plan) in seen) {
            assertEquals("$kind", GlassLevel.SOLID, plan.level)
            assertFalse("$kind samples", plan.samplesBackdrop)
            assertFalse("$kind glass layers", plan.scrim || plan.tint || plan.gloss || plan.bevel || plan.arcs || plan.rim)
        }
    }

    // ---- the budget ----

    @Test fun theFourthLiveSurfaceDropsToSimplified() {
        val budget = GlassBudget()
        rule.setContent {
            Env(GlassLevel.FULL, budget = budget) {
                Column { repeat(4) { Box(Modifier.size(80.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Floating)) } }
            }
        }
        rule.waitForIdle()
        val live = seen.count { it.second.backdrop == BackdropSource.LIVE }
        val simplified = seen.count { it.second.level == GlassLevel.SIMPLIFIED && it.second.backdrop == BackdropSource.NONE }
        assertEquals("three live", 3, live)
        assertEquals("the fourth is simplified", 1, simplified)
        assertEquals(3, budget.inUse)
    }

    @Test fun leavingTheScreenGivesTheSlotBack() {
        val budget = GlassBudget()
        var showThird by mutableStateOf(true)
        rule.setContent {
            Env(GlassLevel.FULL, budget = budget) {
                Column {
                    repeat(2) { Box(Modifier.size(60.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Floating)) }
                    if (showThird) Box(Modifier.size(60.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Floating))
                }
            }
        }
        rule.waitForIdle()
        assertEquals(3, budget.inUse)
        rule.runOnIdle { showThird = false }
        rule.waitForIdle()
        assertEquals(2, budget.inUse)
        rule.runOnIdle { showThird = true }
        rule.waitForIdle()
        assertEquals("a new surface can take the freed slot", 3, budget.inUse)
        assertEquals(BackdropSource.LIVE, seen.last().second.backdrop)
    }

    @Test fun simplifiedSolidTilesAndPanelsDoNotUseTheBudget() {
        val budget = GlassBudget()
        rule.setContent {
            Env(GlassLevel.SIMPLIFIED, budget = budget) { Column { GlassKind.entries.forEach { Surface(it) } } }
        }
        rule.waitForIdle()
        assertEquals(0, budget.inUse)
    }

    // ---- never a live backdrop in a list ----

    @Test fun aFloatingSurfaceInAListScopeIsSimplifiedAndTakesNoSlot() {
        val budget = GlassBudget()
        rule.setContent {
            Env(GlassLevel.FULL, budget = budget) {
                BrutalGlassListScope { Column { repeat(5) { Surface(GlassKind.Floating) } } }
            }
        }
        rule.waitForIdle()
        assertEquals(5, seen.size)
        assertTrue(seen.all { it.second.backdrop == BackdropSource.NONE && it.second.level == GlassLevel.SIMPLIFIED })
        assertEquals(0, budget.inUse)
    }

    @Test fun aPanelWithoutAPageStillIsSimplified() {
        rule.setContent { Env(GlassLevel.FULL) { Surface(GlassKind.Panel) } }
        rule.waitForIdle()
        val plan = seen.single().second
        assertEquals(GlassLevel.SIMPLIFIED, plan.level)
        assertEquals(BackdropSource.NONE, plan.backdrop)
    }

    // ---- the plan the surface ended up with ----

    @Test fun noGlowSurfacesNeverCarryAccentLayers() {
        rule.setContent { Env(GlassLevel.FULL, glow = false) { Column { GlassKind.entries.forEach { Surface(it) } } } }
        rule.waitForIdle()
        assertTrue(seen.none { it.second.bleed || it.second.rimTint || it.second.halo || it.second.accentHairline })
    }

    @Test fun glowSurfacesCarryAccentLayers() {
        rule.setContent { Env(GlassLevel.SIMPLIFIED, glow = true) { Column { Surface(GlassKind.Floating) } } }
        rule.waitForIdle()
        val plan = seen.single().second
        assertTrue(plan.bleed && plan.rimTint && plan.halo)
    }

    // ---- semantics unaffected ----

    @Test fun theKitAddsNoSemanticsAndKeepsClicksWorking() {
        var clicks = 0
        var plainClicks = 0
        rule.setContent {
            Env(GlassLevel.SIMPLIFIED, withBackdrop = false) {
                Column {
                    Box(
                        Modifier
                            .size(120.dp, 48.dp)
                            // A plain rounded shape: the continuous-corner outline is a Path, and Path hit
                            // testing does not exist under Robolectric legacy graphics (it does on a device).
                            .brutalGlass(RoundedCornerShape(24.dp), GlassKind.Floating)
                            .clickable { clicks++ }
                            .semantics { contentDescription = "with glass" },
                    )
                    Box(
                        Modifier
                            .size(120.dp, 48.dp)
                            .clickable { plainClicks++ }
                            .semantics { contentDescription = "plain" },
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("with glass").assertHasClickAction().assertContentDescriptionEquals("with glass")
        rule.onNodeWithContentDescription("plain").performClick()
        assertEquals("the harness itself delivers clicks", 1, plainClicks)
        rule.onNodeWithContentDescription("with glass").performClick()
        assertEquals("and the glass surface still receives them", 1, clicks)
        val glass = rule.onNodeWithContentDescription("with glass").fetchSemanticsNode().config.map { it.key.name }.toSet()
        val plain = rule.onNodeWithContentDescription("plain").fetchSemanticsNode().config.map { it.key.name }.toSet()
        // Compose itself adds a `Shape` entry (the clip region, used for hit testing) to any clipped node.
        // It is not a label, role, state or action, so what TalkBack reads is unchanged.
        assertEquals("the glass modifier adds no label, role, state or action", plain, glass - "Shape")
    }

    @Test fun textInsideTheGlassStaysAvailableToTheSemanticsTree() {
        rule.setContent { Env(GlassLevel.SIMPLIFIED) { Box(Modifier.height(48.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Chip)) { Text("03:45") } } }
        rule.onNodeWithText("03:45").assertExists()
    }
}
