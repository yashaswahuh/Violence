package com.music.bitchord.ui.brutal

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import com.music.bitchord.ui.brutal.glass.GlassBudget
import com.music.bitchord.ui.brutal.glass.GlassInputs
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.brutal.glass.decideGlassLevel
import com.music.bitchord.ui.brutal.safemode.BrutalSafeModeLogic
import com.music.bitchord.ui.brutal.safemode.SafeModeStore
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalMotion
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.ContrastMath
import com.music.bitchord.ui.brutal.theme.ScriptProfile
import com.music.bitchord.ui.brutal.theme.withDisplayFontScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------
// Settings parsing
// ---------------------------------------------------------------------------------------------

class BrutalSettingsParsingTest {
    @Test fun variantDefaultsToCurrent() {
        assertEquals(UiVariant.CURRENT, UiVariant.parse(null))
        assertEquals(UiVariant.CURRENT, UiVariant.parse(""))
    }

    @Test fun variantUnknownValuesReadAsCurrent() {
        // "NEW" is what the removed earlier experiment wrote: it must never turn the new UI on.
        for (raw in listOf("NEW", "new", "brutal", "Brutal", "BRUTAL ", "1", "true", "garbage")) {
            assertEquals("'$raw'", UiVariant.CURRENT, UiVariant.parse(raw))
        }
    }

    @Test fun variantKnownValuesRoundTrip() {
        UiVariant.entries.forEach { assertEquals(it, UiVariant.parse(it.name)) }
        assertEquals(UiVariant.BRUTAL, UiVariant.parse("BRUTAL"))
    }

    @Test fun railSideDefaultsToRight() {
        assertEquals(BrutalRailSide.RIGHT, BrutalRailSide.parse(null))
        assertEquals(BrutalRailSide.RIGHT, BrutalRailSide.parse("CENTER"))
        assertEquals(BrutalRailSide.LEFT, BrutalRailSide.parse("LEFT"))
        BrutalRailSide.entries.forEach { assertEquals(it, BrutalRailSide.parse(it.name)) }
    }

    @Test fun accentDefaultsToYellow() {
        assertEquals(BrutalAccent.Preset(AccentPreset.YELLOW), BrutalAccent.parse(null))
        assertEquals(BrutalAccent.Default, BrutalAccent.parse(""))
        assertEquals(0xFFFAD60A.toInt(), BrutalAccent.Default.argb)
    }

    @Test fun accentPresetsRoundTripById() {
        AccentPreset.entries.forEach { p ->
            val parsed = BrutalAccent.parse(p.id)
            assertEquals(BrutalAccent.Preset(p), parsed)
            assertEquals(p.id, parsed.serialize())
        }
        assertEquals(8, AccentPreset.entries.size)
    }

    @Test fun accentCustomRoundTripsAndIsOpaque() {
        val a = BrutalAccent.Custom(0x00336699)
        assertEquals(0xFF336699.toInt(), a.argb)
        assertEquals("custom:FF336699", a.serialize())
        assertEquals(a, BrutalAccent.parse(a.serialize()))
    }

    @Test fun accentCustomAcceptsSixOrEightDigits() {
        assertEquals(0xFF112233.toInt(), BrutalAccent.parse("custom:112233").argb)
        assertEquals(0xFF112233.toInt(), BrutalAccent.parse("custom:80112233").argb)
        assertTrue(BrutalAccent.parse("custom:112233") is BrutalAccent.Custom)
    }

    @Test fun accentUnreadableValuesAreYellow() {
        for (raw in listOf("custom:", "custom:12", "custom:ZZZZZZ", "custom:1234567", "magenta", "#FF0000", "custom:FF00FF00FF")) {
            assertEquals("'$raw'", BrutalAccent.Default, BrutalAccent.parse(raw))
        }
    }

    @Test fun pickerResultOnAPresetIsStoredAsThePreset() {
        assertEquals(BrutalAccent.Preset(AccentPreset.RED), BrutalAccent.fromArgb(AccentPreset.RED.argb))
        assertEquals(BrutalAccent.Preset(AccentPreset.RED), BrutalAccent.fromArgb(0x00FF3B3B))
        assertTrue(BrutalAccent.fromArgb(0xFF123456.toInt()) is BrutalAccent.Custom)
    }

    @Test fun hsvRoundTripsThePresets() {
        AccentPreset.entries.forEach { p ->
            val back = Hsv.fromArgb(p.argb).toArgb()
            for (shift in listOf(16, 8, 0)) {
                val a = (p.argb shr shift) and 0xFF
                val b = (back shr shift) and 0xFF
                assertTrue("${p.id} channel@$shift $a vs $b", kotlin.math.abs(a - b) <= 1)
            }
        }
    }

    @Test fun hsvPrimaries() {
        assertEquals(0xFFFF0000.toInt(), Hsv(0f, 1f, 1f).toArgb())
        assertEquals(0xFF00FF00.toInt(), Hsv(120f, 1f, 1f).toArgb())
        assertEquals(0xFF0000FF.toInt(), Hsv(240f, 1f, 1f).toArgb())
        assertEquals(0xFF000000.toInt(), Hsv(200f, 0.5f, 0f).toArgb())
        assertEquals(0xFFFFFFFF.toInt(), Hsv(200f, 0f, 1f).toArgb())
    }
}

// ---------------------------------------------------------------------------------------------
// Contrast engine and accent derivation
// ---------------------------------------------------------------------------------------------

class BrutalContrastTest {
    private val white = ContrastMath.WHITE
    private val black = ContrastMath.BLACK

    @Test fun blackOnWhiteIs21() = assertEquals(21.0, ContrastMath.ratio(black, white), 0.01)
    @Test fun sameColourIs1() = assertEquals(1.0, ContrastMath.ratio(0xFF336699.toInt(), 0xFF336699.toInt()), 1e-9)
    @Test fun ratioIsSymmetric() {
        val a = 0xFF336699.toInt()
        val b = 0xFFEEDD22.toInt()
        assertEquals(ContrastMath.ratio(a, b), ContrastMath.ratio(b, a), 1e-12)
    }

    @Test fun measuredPairsMatchTheSpecTable() {
        // Spec 3.1, computed for the page #070708 in the spec; the token is #08080A, within 0.1.
        val page = BrutalAccentRules.PAGE
        assertEquals(14.08, ContrastMath.ratio(AccentPreset.YELLOW.argb, page), 0.15)
        assertEquals(5.70, ContrastMath.ratio(AccentPreset.RED.argb, page), 0.1)
        assertEquals(3.89, ContrastMath.ratio(AccentPreset.BLUE.argb, page), 0.1)
        assertEquals(20.14, ContrastMath.ratio(white, page), 0.3)
        // Spec 3.1 against the worst glass, exactly as tabulated.
        val glass = BrutalAccentRules.GLASS_WORST
        assertEquals(9.77, ContrastMath.ratio(AccentPreset.YELLOW.argb, glass), 0.02)
        assertEquals(3.95, ContrastMath.ratio(AccentPreset.RED.argb, glass), 0.02)
        assertEquals(2.70, ContrastMath.ratio(AccentPreset.BLUE.argb, glass), 0.02)
        assertEquals(4.31, ContrastMath.ratio(AccentPreset.PINK.argb, glass), 0.02)
        // Ink on the accents.
        assertEquals(13.85, ContrastMath.ratio(BrutalAccentRules.INK, AccentPreset.YELLOW.argb), 0.02)
        assertEquals(5.17, ContrastMath.ratio(white, AccentPreset.BLUE.argb), 0.02)
    }

    @Test fun overBlendsInSrgbSpace() {
        assertEquals(0xFF737373.toInt(), ContrastMath.over(black, 0.55, white))
        assertEquals(white, ContrastMath.over(black, 0.0, white))
        assertEquals(black, ContrastMath.over(black, 1.0, white))
    }

    @Test fun bestTextPicksTheReadableEnd() {
        assertEquals(white, ContrastMath.bestText(0xFF101010.toInt()))
        assertEquals(black, ContrastMath.bestText(0xFFF0F0F0.toInt()))
    }

    @Test fun guardIsFineWhenAlreadyReadable() {
        assertEquals(ContrastMath.Guard.Fine, ContrastMath.guard(white, 0xFF202020.toInt(), black))
    }

    @Test fun guardAsksForAScrimOverBrightArtworkWithinTheCap() {
        val g = ContrastMath.guard(white, 0xFFF0F0F0.toInt(), black)
        assertTrue(g is ContrastMath.Guard.Scrim)
        val alpha = (g as ContrastMath.Guard.Scrim).alpha
        assertTrue(alpha <= ContrastMath.MAX_SCRIM)
        assertTrue(ContrastMath.ratio(white, ContrastMath.over(black, alpha, 0xFFF0F0F0.toInt())) >= ContrastMath.TEXT)
    }

    @Test fun guardFallsBackToSolidWhenNoScrimCanHelp() {
        assertEquals(ContrastMath.Guard.Solid, ContrastMath.guard(white, white, white))
    }

    @Test fun theFixedTextScrimGuaranteesWhiteTextOverAnyBackdrop() {
        // Spec 7.4: black at 0.55 behind white text is at least 4.5:1 even over pure white.
        for (backdrop in listOf(white, 0xFFF0F0F0.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(), 0xFFFAD60A.toInt(), 0xFF808080.toInt(), black)) {
            val under = ContrastMath.over(black, ContrastMath.TEXT_SCRIM_ALPHA, backdrop)
            val r = ContrastMath.ratio(white, under)
            assertTrue("backdrop ${backdrop.toUInt().toString(16)}: $r", r >= ContrastMath.TEXT)
        }
    }

    @Test fun ensureContrastLeavesAGoodColourAlone() {
        assertEquals(white, ContrastMath.ensureContrast(white, black))
    }

    @Test fun ensureContrastReachesTheTarget() {
        val bg = 0xFF08080A.toInt()
        val fixed = ContrastMath.ensureContrast(0xFF3A2A6A.toInt(), bg)
        assertTrue(ContrastMath.ratio(fixed, bg) >= ContrastMath.TEXT)
    }

    @Test fun ensureContrastMovesTowardTheDarkEndOnALightBackground() {
        val bg = 0xFFF0F0F0.toInt()
        val fixed = ContrastMath.ensureContrast(0xFFB0B0B0.toInt(), bg)
        assertTrue(ContrastMath.ratio(fixed, bg) >= ContrastMath.TEXT)
        assertTrue(ContrastMath.luminance(fixed) < ContrastMath.luminance(0xFFB0B0B0.toInt()))
    }

    @Test fun walkTakesNoStepsWhenAlreadyDone() {
        val w = ContrastMath.walk(0xFF123456.toInt(), white, 0.05) { true }
        assertEquals(0, w.steps)
        assertEquals(0xFF123456.toInt(), w.color)
    }

    @Test fun walkStopsAtTheStepLimit() {
        val w = ContrastMath.walk(black, white, 0.001) { false }
        assertEquals(ContrastMath.MAX_STEPS, w.steps)
    }

    // ---- The palette's own pairs ----

    @Test fun paletteMeetsItsStatedRatios() {
        val p = BrutalPalette.of(BrutalAccent.Default, glow = true)
        fun r(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color) =
            ContrastMath.ratio(a.toArgb(), b.toArgb())
        assertTrue(r(p.white, p.page) >= 4.5)
        assertTrue(r(p.textMuted, p.page) >= 4.5)
        assertTrue(r(p.textMuted, p.glassBodyBright) >= 4.5)
        assertTrue(r(p.white, p.glassBodyBright) >= 4.5)
        assertTrue(r(p.acid, p.glassBodyBright) >= 4.5)
        assertTrue(r(p.accentText, p.glassBodyBright) >= 4.5)
        assertTrue(r(p.onAccent, p.accentFill) >= 4.5)
    }
}

class BrutalAccentRulesTest {
    private val page = BrutalAccentRules.PAGE
    private val glass = BrutalAccentRules.GLASS_WORST
    private val ink = BrutalAccentRules.INK
    private val white = ContrastMath.WHITE

    private fun near(expected: Int, actual: Int, tol: Int = 2): Boolean =
        listOf(16, 8, 0).all { kotlin.math.abs(((expected shr it) and 0xFF) - ((actual shr it) and 0xFF)) <= tol }

    private fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)

    @Test fun yellowIsUsedAsChosen() {
        val d = BrutalAccentRules.derive(AccentPreset.YELLOW.argb)
        assertEquals(AccentPreset.YELLOW.argb, d.ui)
        assertEquals(AccentPreset.YELLOW.argb, d.text)
        assertEquals(ink, d.onAccent)
        assertEquals(d.ui, d.fill)
        assertFalse(d.adjusted)
        assertEquals(1.0f, d.glowGain)
    }

    @Test fun everyPresetMeetsTheRules() {
        AccentPreset.entries.forEach { p ->
            val d = BrutalAccentRules.derive(p.argb)
            assertTrue("${p.id} ui on page", ContrastMath.ratio(d.ui, page) >= 3.0)
            assertTrue("${p.id} text on glass", ContrastMath.ratio(d.text, glass) >= 4.5)
            assertTrue("${p.id} onAccent on fill", ContrastMath.ratio(d.onAccent, d.fill) >= 4.5)
            // The presets were chosen so the fill never has to move and the UI colour is used as chosen.
            assertEquals("${p.id} fill", d.ui, d.fill)
            assertEquals("${p.id} ui", p.argb, d.ui)
        }
    }

    @Test fun textColourOnAccentMatchesTheSpecTable() {
        assertEquals(white, BrutalAccentRules.derive(AccentPreset.BLUE.argb).onAccent)
        for (p in AccentPreset.entries.filter { it != AccentPreset.BLUE }) {
            assertEquals("${p.id}", ink, BrutalAccentRules.derive(p.argb).onAccent)
        }
    }

    @Test fun presetsThatFailAsTextOnGlassGetALightenedTextVariant() {
        // Spec 3.1: red #FF5F5F, blue #6E8CFF, pink #FF51AE (within rounding).
        val red = BrutalAccentRules.derive(AccentPreset.RED.argb)
        assertTrue("red ${hex(red.text)}", near(0xFFFF5F5F.toInt(), red.text))
        assertEquals(AccentPreset.RED.argb, red.ui)
        val blue = BrutalAccentRules.derive(AccentPreset.BLUE.argb)
        assertTrue("blue ${hex(blue.text)}", near(0xFF6E8CFF.toInt(), blue.text))
        val pink = BrutalAccentRules.derive(AccentPreset.PINK.argb)
        assertTrue("pink ${hex(pink.text)}", near(0xFFFF51AE.toInt(), pink.text))
        // And the ones that pass are left alone.
        for (p in listOf(AccentPreset.YELLOW, AccentPreset.GREEN, AccentPreset.ORANGE, AccentPreset.CYAN, AccentPreset.WHITE)) {
            assertEquals(p.id, p.argb, BrutalAccentRules.derive(p.argb).text)
        }
    }

    @Test fun customWorkedExamplesFromTheSpec() {
        // Spec 3.2 table: chosen -> accentUi, accentText (within rounding), text on a fill.
        data class Case(val chosen: Int, val ui: Int, val text: Int, val onAccent: Int)
        val cases = listOf(
            Case(0xFF2040C0.toInt(), 0xFF3653C6.toInt(), 0xFF8092DB.toInt(), white),
            Case(0xFF7A1FA2.toInt(), 0xFF8D3FAF.toInt(), 0xFFB380CA.toInt(), white),
            Case(0xFF1B5E20.toInt(), 0xFF316E36.toInt(), 0xFF769F79.toInt(), white),
            Case(0xFF8B0000.toInt(), 0xFFA53A3A.toInt(), 0xFFC68383.toInt(), white),
            Case(0xFFFF00FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF19FF.toInt(), ink),
            Case(0xFF808080.toInt(), 0xFF808080.toInt(), 0xFF989898.toInt(), ink),
        )
        for (c in cases) {
            val d = BrutalAccentRules.derive(c.chosen)
            assertTrue("${hex(c.chosen)} ui ${hex(d.ui)} vs ${hex(c.ui)}", near(c.ui, d.ui))
            assertTrue("${hex(c.chosen)} text ${hex(d.text)} vs ${hex(c.text)}", near(c.text, d.text, 3))
            assertEquals("${hex(c.chosen)} onAccent", c.onAccent, d.onAccent)
            assertEquals(d.ui != (c.chosen or 0xFF000000.toInt()), d.adjusted)
        }
    }

    @Test fun lowContrastChoiceIsLightenedNotRefused() {
        val d = BrutalAccentRules.derive(0xFF8B0000.toInt())
        assertTrue(d.adjusted)
        assertEquals(0xFF8B0000.toInt(), d.chosen)
        assertTrue(ContrastMath.luminance(d.ui) > ContrastMath.luminance(d.chosen))
    }

    @Test fun midToneFillMovesWhenNeitherInkNorWhiteReachesTheTarget() {
        // A mid grey where ink and white are both under 4.5:1 on it (the product of the two ratios
        // is about 19.8, so there is a narrow band of luminance where neither reaches 4.5).
        val chosen = (100..140).map { (0xFF shl 24) or (it shl 16) or (it shl 8) or it }
            .first { maxOf(ContrastMath.ratio(ink, it), ContrastMath.ratio(white, it)) < 4.5 }
        val onInk = ContrastMath.ratio(ink, chosen)
        val onWhite = ContrastMath.ratio(white, chosen)
        assertTrue("setup: ink $onInk white $onWhite", maxOf(onInk, onWhite) < 4.5)
        val d = BrutalAccentRules.derive(chosen)
        assertTrue(ContrastMath.ratio(d.onAccent, d.fill) >= 4.5)
        assertNotEquals(d.ui, d.fill)
    }

    @Test fun invariantsHoldForAnyColour() {
        val rnd = Random(20261102)
        repeat(3000) {
            val c = (0xFF shl 24) or rnd.nextInt(0x1000000)
            val d = BrutalAccentRules.derive(c)
            assertTrue("${hex(c)} ui ${hex(d.ui)} ${ContrastMath.ratio(d.ui, page)}", ContrastMath.ratio(d.ui, page) >= 3.0)
            assertTrue("${hex(c)} text ${hex(d.text)} ${ContrastMath.ratio(d.text, glass)}", ContrastMath.ratio(d.text, glass) >= 4.5)
            assertTrue("${hex(c)} onAccent ${ContrastMath.ratio(d.onAccent, d.fill)}", ContrastMath.ratio(d.onAccent, d.fill) >= 4.5)
        }
    }

    @Test fun deriveIgnoresAlphaAndIsPure() {
        assertEquals(BrutalAccentRules.derive(0xFF336699.toInt()), BrutalAccentRules.derive(0x10336699))
    }

    @Test fun glowGainFollowsLuminance() {
        assertEquals(1.0f, BrutalAccentRules.glowGain(0.68))
        assertEquals(1.0f, BrutalAccentRules.glowGain(0.5))
        assertEquals(1.15f, BrutalAccentRules.glowGain(0.35))
        assertEquals(1.15f, BrutalAccentRules.glowGain(0.2))
        assertEquals(1.35f, BrutalAccentRules.glowGain(0.15))
        // The spec's examples through the real derivation.
        assertEquals(1.0f, BrutalAccentRules.derive(AccentPreset.YELLOW.argb).glowGain)
        assertEquals(1.0f, BrutalAccentRules.derive(AccentPreset.WHITE.argb).glowGain)
        assertEquals(1.15f, BrutalAccentRules.derive(AccentPreset.ORANGE.argb).glowGain)
        assertEquals(1.15f, BrutalAccentRules.derive(AccentPreset.RED.argb).glowGain)
        assertEquals(1.35f, BrutalAccentRules.derive(AccentPreset.BLUE.argb).glowGain)
    }

    @Test fun bleedAlphaIsCappedSoWhiteTextStaysReadable() {
        val yellow = AccentPreset.YELLOW.argb
        val cap = BrutalAccentRules.bleedAlphaCap(yellow, white, requested = 0.55)
        assertEquals(0.45, cap, 0.001)
        assertTrue(ContrastMath.ratio(white, ContrastMath.over(yellow, cap, BrutalAccentRules.GLASS_BODY)) >= 4.5)
        // Under accent-coloured text the cap is lower (spec 3.4: 0.35).
        val textCap = BrutalAccentRules.bleedAlphaCap(yellow, yellow, requested = 0.55)
        assertTrue("accent text cap $textCap", textCap <= 0.351)
        // Dark accents are not limited by white text.
        assertEquals(0.55, BrutalAccentRules.bleedAlphaCap(AccentPreset.BLUE.argb, white, requested = 0.55), 0.001)
        // White accent under white text: capped at 0.35.
        assertEquals(0.35, BrutalAccentRules.bleedAlphaCap(white, white, requested = 0.55), 0.06)
    }
}

// ---------------------------------------------------------------------------------------------
// Glass level, budget
// ---------------------------------------------------------------------------------------------

class BrutalGlassLevelTest {
    private fun inputs(
        sdk: Int = 34,
        lowRam: Boolean = false,
        battery: Boolean = false,
        transparency: Boolean = false,
        motion: Boolean = false,
        blur: Boolean = false,
    ) = GlassInputs(sdk, lowRam, battery, transparency, motion, blur)

    @Test fun aCapableDeviceWithNothingSetIsFull() = assertEquals(GlassLevel.FULL, decideGlassLevel(inputs()))
    @Test fun android13AndUpAreFull() {
        for (sdk in listOf(33, 34, 35, 36, 37)) assertEquals("sdk $sdk", GlassLevel.FULL, decideGlassLevel(inputs(sdk = sdk)))
    }

    @Test fun belowAndroid12IsSolid() {
        for (sdk in listOf(26, 28, 29, 30)) assertEquals("sdk $sdk", GlassLevel.SOLID, decideGlassLevel(inputs(sdk = sdk)))
    }

    @Test fun android12HasNoLensSoItIsSimplified() {
        assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(sdk = 31)))
        assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(sdk = 32)))
    }

    @Test fun reduceTransparencyIsSolid() = assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(transparency = true)))
    @Test fun reduceDynamicBlurIsSolid() = assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(blur = true)))
    @Test fun lowRamIsSimplified() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(lowRam = true)))
    @Test fun batterySaverIsSimplified() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(battery = true)))
    @Test fun reducedMotionIsSimplified() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(motion = true)))

    @Test fun solidWinsOverSimplified() {
        assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(lowRam = true, battery = true, motion = true, transparency = true)))
        assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(sdk = 30, lowRam = true)))
        assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(sdk = 31, blur = true)))
    }

    @Test fun everyCombinationFollowsTheRulesInOrder() {
        for (sdk in listOf(26, 30, 31, 32, 33, 36)) for (mask in 0 until 32) {
            val i = inputs(sdk, mask and 1 != 0, mask and 2 != 0, mask and 4 != 0, mask and 8 != 0, mask and 16 != 0)
            val expected = when {
                sdk < 31 || i.reduceTransparency || i.reduceDynamicBlur -> GlassLevel.SOLID
                i.lowRamDevice || i.batterySaver || i.reduceMotion || sdk < 33 -> GlassLevel.SIMPLIFIED
                else -> GlassLevel.FULL
            }
            assertEquals("sdk $sdk mask $mask", expected, decideGlassLevel(i))
        }
    }
}

class BrutalGlassBudgetTest {
    @Test fun allowsThreeLiveLayers() {
        val b = GlassBudget()
        assertEquals(3, b.limit)
        assertTrue(b.tryAcquire())
        assertTrue(b.tryAcquire())
        assertTrue(b.tryAcquire())
        assertEquals(3, b.inUse)
    }

    @Test fun aFourthLayerIsRefused() {
        val b = GlassBudget()
        repeat(3) { b.tryAcquire() }
        assertFalse(b.tryAcquire())
        assertEquals(3, b.inUse)
    }

    @Test fun releasingFreesASlot() {
        val b = GlassBudget()
        repeat(3) { b.tryAcquire() }
        b.release()
        assertEquals(2, b.inUse)
        assertTrue(b.tryAcquire())
        assertFalse(b.tryAcquire())
    }

    @Test fun releasingMoreThanAcquiredNeverGoesNegative() {
        val b = GlassBudget()
        b.release()
        b.release()
        assertEquals(0, b.inUse)
        assertTrue(b.tryAcquire())
        assertEquals(1, b.inUse)
    }

    @Test fun theLimitIsConfigurable() {
        val b = GlassBudget(limit = 1)
        assertTrue(b.tryAcquire())
        assertFalse(b.tryAcquire())
    }
}

// ---------------------------------------------------------------------------------------------
// Safe mode
// ---------------------------------------------------------------------------------------------

class BrutalSafeModeLogicTest {
    private class Store : SafeModeStore {
        override var startMarker = false
        override var failedStarts = 0
        override var revertNotice = false
    }

    /** One launch whose first screen never appears (a crash): the marker stays set. */
    private fun crashedLaunch(logic: BrutalSafeModeLogic, wanted: UiVariant = UiVariant.BRUTAL): UiVariant {
        val run = logic.startupCheck(wanted)
        if (run == UiVariant.BRUTAL) logic.begin()
        return run
    }

    /** One launch that reaches its first screen. */
    private fun healthyLaunch(logic: BrutalSafeModeLogic, wanted: UiVariant = UiVariant.BRUTAL): UiVariant {
        val run = logic.startupCheck(wanted)
        if (run == UiVariant.BRUTAL) {
            logic.begin()
            logic.shown()
        }
        return run
    }

    @Test fun zeroFailuresKeepsTheNewUi() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        assertEquals(UiVariant.BRUTAL, healthyLaunch(logic))
        assertEquals(UiVariant.BRUTAL, healthyLaunch(logic))
        assertEquals(0, s.failedStarts)
        assertFalse(s.revertNotice)
    }

    @Test fun oneFailedStartIsToleratedAndCounted() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        assertEquals(UiVariant.BRUTAL, crashedLaunch(logic))
        assertTrue(s.startMarker)
        // The next launch sees the leftover marker: one failure, still the new UI.
        assertEquals(UiVariant.BRUTAL, logic.startupCheck(UiVariant.BRUTAL))
        assertEquals(1, s.failedStarts)
        assertFalse(s.revertNotice)
    }

    @Test fun twoConsecutiveFailedStartsRevertAndRaiseTheNoticeOnce() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        assertEquals(UiVariant.BRUTAL, crashedLaunch(logic)) // launch 1 crashes
        assertEquals(UiVariant.BRUTAL, crashedLaunch(logic)) // launch 2 sees failure 1, crashes again
        assertEquals(UiVariant.CURRENT, logic.startupCheck(UiVariant.BRUTAL)) // launch 3 sees failure 2
        assertTrue(logic.consumeRevertNotice())
        assertFalse("the notice is shown once", logic.consumeRevertNotice())
        assertEquals(0, s.failedStarts)
    }

    @Test fun aSuccessfulStartResetsTheCount() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        crashedLaunch(logic)
        assertEquals(UiVariant.BRUTAL, healthyLaunch(logic)) // sees failure 1, then reaches its first screen
        assertEquals(0, s.failedStarts)
        crashedLaunch(logic)
        assertEquals(UiVariant.BRUTAL, logic.startupCheck(UiVariant.BRUTAL)) // only one failure again
        assertEquals(1, s.failedStarts)
    }

    @Test fun failuresMustBeConsecutive() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        crashedLaunch(logic)
        healthyLaunch(logic)
        crashedLaunch(logic)
        healthyLaunch(logic)
        assertEquals(UiVariant.BRUTAL, logic.startupCheck(UiVariant.BRUTAL))
        assertFalse(logic.consumeRevertNotice())
    }

    @Test fun shownClearsTheMarker() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        logic.begin()
        assertTrue(s.startMarker)
        logic.shown()
        assertFalse(s.startMarker)
    }

    @Test fun theCurrentUiIsNeverAffectedAndClearsOldState() {
        val s = Store().apply { startMarker = true; failedStarts = 1 }
        val logic = BrutalSafeModeLogic(s)
        assertEquals(UiVariant.CURRENT, logic.startupCheck(UiVariant.CURRENT))
        assertFalse(s.startMarker)
        assertEquals(0, s.failedStarts)
        assertFalse(logic.consumeRevertNotice())
    }

    @Test fun noticeIsFalseWhenNothingHappened() {
        assertFalse(BrutalSafeModeLogic(Store()).consumeRevertNotice())
    }

    @Test fun afterARevertTheUserCanTurnItBackOnWithAFreshCount() {
        val s = Store()
        val logic = BrutalSafeModeLogic(s)
        crashedLaunch(logic)
        crashedLaunch(logic)
        assertEquals(UiVariant.CURRENT, logic.startupCheck(UiVariant.BRUTAL))
        logic.consumeRevertNotice()
        // The caller persisted CURRENT; later the user flips it on again and the next launch is clean.
        assertEquals(UiVariant.CURRENT, logic.startupCheck(UiVariant.CURRENT))
        assertEquals(UiVariant.BRUTAL, healthyLaunch(logic))
        assertEquals(0, s.failedStarts)
    }

    @Test fun maxFailedStartsIsTwo() = assertEquals(2, BrutalSafeModeLogic.MAX_FAILED_STARTS)
}

// ---------------------------------------------------------------------------------------------
// Type, motion, shapes, spacing tokens
// ---------------------------------------------------------------------------------------------

class BrutalTokensTest {
    @Test fun theSeventeenLanguagesGetTheRightProfile() {
        val latin = listOf("en", "es", "fr", "de", "it", "pt", "pl", "tr", "id")
        val system = listOf("ru", "vi", "he", "hi", "th", "ja", "ko", "zh")
        latin.forEach { assertEquals(it, ScriptProfile.LATIN, ScriptProfile.forLanguage(it)) }
        system.forEach { assertEquals(it, ScriptProfile.SYSTEM_SANS, ScriptProfile.forLanguage(it)) }
        assertEquals(17, latin.size + system.size)
    }

    @Test fun profileToleratesRegionsCaseAndNull() {
        assertEquals(ScriptProfile.LATIN, ScriptProfile.forLanguage("pt-BR"))
        assertEquals(ScriptProfile.LATIN, ScriptProfile.forLanguage("EN_us"))
        assertEquals(ScriptProfile.SYSTEM_SANS, ScriptProfile.forLanguage("zh-TW"))
        assertEquals(ScriptProfile.SYSTEM_SANS, ScriptProfile.forLanguage(null))
        assertEquals(ScriptProfile.SYSTEM_SANS, ScriptProfile.forLanguage("xx"))
    }

    @Test fun onlyLatinUsesUppercase() {
        assertTrue(ScriptProfile.LATIN.usesUppercase)
        assertFalse(ScriptProfile.SYSTEM_SANS.usesUppercase)
    }

    @Test fun tileCornerIsNineteenPercentClampedTo12And28() {
        assertEquals(19.0f, BrutalShapes.tileCornerDp(100f), 0.001f)
        assertEquals(12f, BrutalShapes.tileCornerDp(40f), 0.001f)
        assertEquals(28f, BrutalShapes.tileCornerDp(400f), 0.001f)
    }

    @Test fun reducedMotionIsAShortFadeWithNoTiltAndNoPulse() {
        val full = BrutalMotion(reduced = false)
        val reduced = BrutalMotion(reduced = true)
        assertTrue(full.allowsTiltSettle)
        assertTrue(full.allowsGlowPulse)
        assertFalse(reduced.allowsTiltSettle)
        assertFalse(reduced.allowsGlowPulse)
        assertTrue(reduced.standard<Float>() is androidx.compose.animation.core.TweenSpec)
        assertTrue(full.standard<Float>() is androidx.compose.animation.core.SpringSpec)
        assertEquals(BrutalMotion.REDUCED_FADE_MS, (reduced.panel<Float>() as androidx.compose.animation.core.TweenSpec).durationMillis)
        assertEquals(0, (reduced.railFold<Float>() as androidx.compose.animation.core.TweenSpec).durationMillis)
        assertEquals(200, (full.railFold<Float>() as androidx.compose.animation.core.TweenSpec).durationMillis)
    }

    @Test fun displayFontScaleIsCappedAtOnePointThree() {
        val big = Density(density = 2.75f, fontScale = 2.0f).withDisplayFontScale()
        assertEquals(1.3f, big.fontScale, 0.0001f)
        assertEquals(2.75f, big.density, 0.0001f)
        val small = Density(density = 2f, fontScale = 1.0f).withDisplayFontScale()
        assertEquals(1.0f, small.fontScale, 0.0001f)
    }

    @Test fun touchTargetsAndRailTokensFollowTheSpec() {
        assertEquals(48f, BrutalDimens.MinTouch.value, 0.001f)
        assertEquals(56f, BrutalDimens.RailLane.value, 0.001f)
        assertEquals(80f, BrutalDimens.MiniPlayerHeight.value, 0.001f)
        assertEquals(96f, BrutalDimens.DockHeight.value, 0.001f)
        assertEquals(12f, BrutalDimens.MasonryGap.value, 0.001f)
        assertEquals(1.3f, BrutalDimens.DISPLAY_FONT_SCALE_CAP, 0.001f)
        assertEquals(1.5f, BrutalDimens.RAIL_TO_DOCK_FONT_SCALE, 0.001f)
    }
}
