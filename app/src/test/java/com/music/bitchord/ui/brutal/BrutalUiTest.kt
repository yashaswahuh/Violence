package com.music.bitchord.ui.brutal

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.brutal.settings.BrutalAccentPicker
import com.music.bitchord.ui.player.AndroidPlayerHost
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.ui.brutal.settings.brutalSettingRows
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
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
 * Compose UI tests of the brutal look's plumbing on the JVM (Robolectric): the Settings rows and
 * that they flip the settings, the accent picker, and that switching the variant changes values and
 * never the shape of the composition. Not covered (needs a device): the real media controller,
 * playback, the glass pipeline; see `fork-docs/BRUTAL_UI_PLAN.md`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class BrutalUiTest {
    @get:Rule val rule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun s(id: Int) = context.getString(id)

    @Before fun setUp() {
        // AppSettings.init needs the app's encrypted AuthStore; the setters only need the prefs file.
        val field = AppSettings::class.java.getDeclaredField("prefs")
        field.isAccessible = true
        field.set(null, context.getSharedPreferences("brutal_ui_test_settings", Context.MODE_PRIVATE))
        context.getSharedPreferences("brutal_ui_test_settings", Context.MODE_PRIVATE).edit().clear().commit()
        AppSettings.uiVariant.value = UiVariant.CURRENT
        AppSettings.brutalGlow.value = true
        AppSettings.brutalAccent.value = BrutalAccent.Default
        AppSettings.brutalRailSide.value = BrutalRailSide.RIGHT
        AppSettings.reduceTransparency.value = false
        AppSettings.reduceDynamicBlur.value = false
        AppSettings.reduceAnimation.value = false
        // The shared segmented control reads the player's settings through the platform host.
        PlayerPlatform.install(AndroidPlayerHost(context))
    }

    @Composable
    private fun RowsHost() {
        MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Exactly what SettingsSheet draws, row by row, minus its card.
                brutalSettingRows().forEach { it.content() }
            }
        }
    }

    // ---- Settings rows ----

    @Test fun onlyTheNewUiRowShowsWhileTheNewUiIsOff() {
        rule.setContent { RowsHost() }
        rule.onNodeWithText(s(R.string.brutal_new_ui_title)).assertExists()
        rule.onNodeWithText(s(R.string.brutal_new_ui_subtitle)).assertExists()
        rule.onNodeWithText(s(R.string.brutal_glow_title)).assertDoesNotExist()
        rule.onNodeWithText(s(R.string.brutal_accent_title)).assertDoesNotExist()
        rule.onNodeWithText(s(R.string.brutal_rail_side_title)).assertDoesNotExist()
        rule.onNodeWithText(s(R.string.brutal_reduce_transparency_title)).assertDoesNotExist()
    }

    @Test fun theNewUiRowFlipsTheVariantAndRevealsTheOthers() {
        rule.setContent { RowsHost() }
        rule.onNodeWithText(s(R.string.brutal_new_ui_title)).performClick()
        rule.runOnIdle { assertEquals(UiVariant.BRUTAL, AppSettings.uiVariant.value) }
        rule.onNodeWithText(s(R.string.brutal_glow_title)).assertExists()
        rule.onNodeWithText(s(R.string.brutal_accent_title)).assertExists()
        rule.onNodeWithText(s(R.string.brutal_rail_side_title)).assertExists()
        rule.onNodeWithText(s(R.string.brutal_reduce_transparency_title)).assertExists()
        // And off again: the rows go and the variant is CURRENT.
        rule.onNodeWithText(s(R.string.brutal_new_ui_title)).performClick()
        rule.runOnIdle { assertEquals(UiVariant.CURRENT, AppSettings.uiVariant.value) }
        rule.onNodeWithText(s(R.string.brutal_glow_title)).assertDoesNotExist()
    }

    @Test fun theVariantIsPersistedByName() {
        rule.setContent { RowsHost() }
        rule.onNodeWithText(s(R.string.brutal_new_ui_title)).performClick()
        rule.runOnIdle {
            val prefs = context.getSharedPreferences("brutal_ui_test_settings", Context.MODE_PRIVATE)
            assertEquals("BRUTAL", prefs.getString("ui_variant", null))
        }
    }

    @Test fun glowFlipsItsSetting() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        rule.setContent { RowsHost() }
        assertTrue(AppSettings.brutalGlow.value)
        rule.onNodeWithText(s(R.string.brutal_glow_title)).performClick()
        rule.runOnIdle { assertFalse(AppSettings.brutalGlow.value) }
        rule.onNodeWithText(s(R.string.brutal_glow_title)).performClick()
        rule.runOnIdle { assertTrue(AppSettings.brutalGlow.value) }
    }

    @Test fun railSideFlipsItsSetting() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        rule.setContent { RowsHost() }
        // The shared pill control places its segments from measured text; under Robolectric a
        // simulated touch can miss them, so the segment's own click action is invoked instead
        // (it is the same lambda a touch ends in).
        rule.onNodeWithText(s(R.string.brutal_rail_left)).performSemanticsAction(SemanticsActions.OnClick)
        rule.runOnIdle { assertEquals(BrutalRailSide.LEFT, AppSettings.brutalRailSide.value) }
        rule.onNodeWithText(s(R.string.brutal_rail_right)).performSemanticsAction(SemanticsActions.OnClick)
        rule.runOnIdle { assertEquals(BrutalRailSide.RIGHT, AppSettings.brutalRailSide.value) }
    }

    @Test fun reduceTransparencyFlipsItsSetting() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        rule.setContent { RowsHost() }
        rule.onNodeWithText(s(R.string.brutal_reduce_transparency_title)).performClick()
        rule.runOnIdle { assertTrue(AppSettings.reduceTransparency.value) }
    }

    @Test fun theAccentRowShowsTheCurrentAccentName() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        rule.setContent { RowsHost() }
        rule.onNodeWithText(s(R.string.brutal_accent_yellow)).assertExists()
        rule.runOnIdle { AppSettings.brutalAccent.value = BrutalAccent.Preset(AccentPreset.CYAN) }
        rule.onNodeWithText(s(R.string.brutal_accent_cyan)).assertExists()
        rule.runOnIdle { AppSettings.brutalAccent.value = BrutalAccent.Custom(0xFF123456.toInt()) }
        rule.onNodeWithText(s(R.string.brutal_accent_custom), useUnmergedTree = true).assertExists()
    }

    @Test fun everyRowHasKeywordsForSettingsSearch() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        var rows = emptyList<com.music.bitchord.ui.brutal.settings.BrutalSettingRow>()
        rule.setContent { rows = brutalSettingRows() }
        rule.runOnIdle {
            assertEquals(5, rows.size)
            rows.forEach { assertTrue(it.keywords.size >= 2) }
        }
    }

    // ---- Accent picker ----

    @Test fun pickingAPresetStoresIt() {
        rule.setContent {
            MaterialTheme {
                BrutalAccentPicker(current = AppSettings.brutalAccent.value, onPick = AppSettings::setBrutalAccent)
            }
        }
        rule.onNodeWithContentDescription(s(R.string.brutal_accent_red)).performClick()
        rule.runOnIdle { assertEquals(BrutalAccent.Preset(AccentPreset.RED), AppSettings.brutalAccent.value) }
    }

    @Test fun theCurrentPresetIsMarkedSelected() {
        AppSettings.brutalAccent.value = BrutalAccent.Preset(AccentPreset.GREEN)
        rule.setContent { MaterialTheme { BrutalAccentPicker(current = AppSettings.brutalAccent.value, onPick = {}) } }
        rule.onNodeWithContentDescription(s(R.string.brutal_accent_green)).assertIsSelected()
    }

    @Test fun theCustomColourAppliesOnUseThisColour() {
        var picked: BrutalAccent? = null
        rule.setContent {
            MaterialTheme { BrutalAccentPicker(current = BrutalAccent.Preset(AccentPreset.BLUE), onPick = { picked = it }) }
        }
        rule.onNodeWithText(s(R.string.brutal_accent_use_custom)).performScrollTo().performClick()
        rule.runOnIdle {
            // The sliders start at the current colour, so "use" re-picks it (as the preset it equals).
            assertEquals(BrutalAccent.Preset(AccentPreset.BLUE).argb, picked?.argb)
        }
    }

    // ---- The root switch ----

    @Test fun switchingTheVariantDoesNotRecreateTheContent() {
        var created = 0
        var disposed = 0
        rule.setContent {
            MaterialTheme {
                BrutalRoot(dark = true) {
                    // Stands in for BitChordApp, which owns the media controller.
                    remember { created++ }
                    DisposableEffect(Unit) { onDispose { disposed++ } }
                    Text("app")
                }
            }
        }
        rule.runOnIdle { AppSettings.uiVariant.value = UiVariant.BRUTAL }
        rule.waitForIdle()
        rule.runOnIdle { AppSettings.uiVariant.value = UiVariant.CURRENT }
        rule.waitForIdle()
        rule.runOnIdle { AppSettings.uiVariant.value = UiVariant.BRUTAL }
        rule.waitForIdle()
        rule.onNodeWithText("app").assertExists()
        assertEquals("content composed once", 1, created)
        assertEquals("content never disposed", 0, disposed)
    }

    @Test fun theCurrentUiReProvidesItsOwnValues() {
        val outer = androidx.compose.material3.lightColorScheme(background = Color(0xFF123456))
        var inside: Color? = null
        var glass: Boolean? = null
        rule.setContent {
            MaterialTheme(colorScheme = outer) {
                CompositionLocalProvider(LocalLiquidGlassEnabled provides false) {
                    BrutalRoot(dark = false) {
                        inside = MaterialTheme.colorScheme.background
                        glass = LocalLiquidGlassEnabled.current
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(Color(0xFF123456), inside)
            assertEquals(false, glass)
        }
    }

    @Test fun theNewUiPaintsTheBlackPageAndTurnsLiquidGlassOnAtFull() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        var background: Color? = null
        var primary: Color? = null
        var glass: Boolean? = null
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLiquidGlassEnabled provides false) {
                    BrutalRoot(dark = true) {
                        background = MaterialTheme.colorScheme.background
                        primary = MaterialTheme.colorScheme.primary
                        glass = LocalLiquidGlassEnabled.current
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(BrutalPalette.Page, background)
            assertEquals(Color(AccentPreset.YELLOW.argb), primary)
            // Robolectric reports a normal device on API 34: the level is FULL.
            assertEquals(true, glass)
        }
    }

    @Test fun reduceTransparencyTurnsTheLegacyGlassBackOff() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        AppSettings.reduceTransparency.value = true
        var glass: Boolean? = null
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLiquidGlassEnabled provides true) {
                    BrutalRoot(dark = true) { glass = LocalLiquidGlassEnabled.current }
                }
            }
        }
        rule.runOnIdle { assertEquals(false, glass) }
    }

    @Test fun theAccentSettingReachesTheThemeImmediately() {
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        var primary: Color? = null
        rule.setContent { MaterialTheme { BrutalRoot(dark = true) { primary = MaterialTheme.colorScheme.primary } } }
        rule.runOnIdle { assertEquals(Color(AccentPreset.YELLOW.argb), primary) }
        rule.runOnIdle { AppSettings.setBrutalAccent(BrutalAccent.Preset(AccentPreset.PINK)) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(Color(AccentPreset.PINK.argb), primary) }
    }

    @Test fun withTheNewUiOffEffectiveDarkAndBackgroundAreUnchanged() {
        var dark: Boolean? = null
        var bg: Color? = null
        rule.setContent {
            dark = brutalEffectiveDark(false)
            bg = brutalWindowBackground(Color.White)
        }
        rule.runOnIdle {
            assertEquals(false, dark)
            assertEquals(Color.White, bg)
        }
        AppSettings.uiVariant.value = UiVariant.BRUTAL
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(true, dark)
            assertEquals(BrutalPalette.Page, bg)
        }
    }
}
