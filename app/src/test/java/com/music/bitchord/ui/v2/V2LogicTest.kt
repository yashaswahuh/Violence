package com.music.bitchord.ui.v2

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.music.bitchord.ui.v2.glass.GlassBudget
import com.music.bitchord.ui.v2.glass.GlassInputs
import com.music.bitchord.ui.v2.glass.GlassLevel
import com.music.bitchord.ui.v2.glass.decideGlassLevel
import com.music.bitchord.ui.v2.safemode.SafeModeStore
import com.music.bitchord.ui.v2.safemode.UiSafeModeLogic
import com.music.bitchord.ui.v2.theme.ArtworkAccent
import com.music.bitchord.ui.v2.theme.ContrastMath
import com.music.bitchord.ui.v2.theme.V2Colors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastMathTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test fun blackOnWhiteIs21() = assertEquals(21.0, ContrastMath.ratio(black, white), 0.01)
    @Test fun sameColourIs1() = assertEquals(1.0, ContrastMath.ratio(0xFF336699.toInt(), 0xFF336699.toInt()), 0.001)

    @Test fun bestTextPicksTheReadableEnd() {
        assertEquals(white, ContrastMath.bestText(0xFF101010.toInt()))
        assertEquals(black, ContrastMath.bestText(0xFFF0F0F0.toInt()))
    }

    @Test fun guardIsFineWhenAlreadyReadable() {
        assertEquals(ContrastMath.Guard.Fine, ContrastMath.guard(white, 0xFF202020.toInt(), black))
    }

    @Test fun guardAsksForAScrimOverBrightArtwork() {
        // White text on a bright backdrop is under 4.5:1; a black scrim fixes it, within the cap.
        val g = ContrastMath.guard(white, 0xFFF0F0F0.toInt(), black)
        assertTrue(g is ContrastMath.Guard.Scrim)
        val alpha = (g as ContrastMath.Guard.Scrim).alpha
        assertTrue(alpha <= ContrastMath.MAX_SCRIM)
        val result = ContrastMath.over(black, alpha, 0xFFF0F0F0.toInt())
        assertTrue(ContrastMath.ratio(white, result) >= ContrastMath.TEXT)
    }

    @Test fun guardFallsBackToSolidWhenNoScrimCanHelp() {
        // Pure white text over pure white with a white "scrim" can never improve.
        assertEquals(ContrastMath.Guard.Solid, ContrastMath.guard(white, white, white))
    }

    @Test fun ensureContrastReachesTheTarget() {
        val bg = 0xFF0B0B12.toInt()
        val fixed = ContrastMath.ensureContrast(0xFF3A2A6A.toInt(), bg)
        assertTrue(ContrastMath.ratio(fixed, bg) >= ContrastMath.TEXT)
    }

    @Test fun paletteMeetsItsStatedRatios() {
        val d = V2Colors.Dark
        val l = V2Colors.Light
        fun r(a: Color, b: Color) = ContrastMath.ratio(a.toArgb(), b.toArgb())
        assertTrue(r(d.ink, d.background) >= 4.5)
        assertTrue(r(d.ink2, d.background) >= 4.5)
        assertTrue(r(d.voltageText, d.background) >= 4.5)
        assertTrue(r(d.mint, d.background) >= 4.5)
        assertTrue(r(d.ember, d.background) >= 4.5)
        assertTrue(r(Color.White, d.voltage) >= 4.5)
        assertTrue(r(d.focusRing, d.background) >= 3.0)
        assertTrue(r(l.ink, l.background) >= 4.5)
        assertTrue(r(l.ink2, l.background) >= 4.5)
        assertTrue(r(l.voltageText, l.background) >= 4.5)
        assertTrue(r(l.mint, l.background) >= 4.5)
        assertTrue(r(l.ember, l.background) >= 4.5)
        assertTrue(r(l.focusRing, l.background) >= 3.0)
    }
}

class ArtworkAccentTest {
    private val bg = V2Colors.Dark.background.toArgb()
    private val fallback = V2Colors.Dark.voltage.toArgb()

    @Test fun neonArtworkStillGivesReadableAccentText() {
        for (seed in listOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt(), 0xFF0000FF.toInt(), 0xFFFFFF00.toInt(), 0xFFFF00FF.toInt())) {
            val a = ArtworkAccent.from(seed, dark = true, background = bg, fallback = fallback)
            assertTrue(ContrastMath.ratio(a.text, bg) >= ContrastMath.TEXT)
            assertTrue(ContrastMath.ratio(a.onFill, a.fill) >= ContrastMath.TEXT)
        }
    }

    @Test fun greysAndNearBlackFallBack() {
        for (seed in listOf(0xFF808080.toInt(), 0xFF050505.toInt(), 0xFFF8F8F8.toInt(), null)) {
            val a = ArtworkAccent.from(seed, dark = true, background = bg, fallback = fallback)
            assertTrue(a.fromFallback)
            assertEquals(fallback, a.fill)
        }
    }

    @Test fun lightThemeAccentTextReadsOnLightBackground() {
        val lightBg = V2Colors.Light.background.toArgb()
        val a = ArtworkAccent.from(0xFFFFCC00.toInt(), dark = false, background = lightBg, fallback = fallback)
        assertTrue(ContrastMath.ratio(a.text, lightBg) >= ContrastMath.TEXT)
    }
}

class GlassLevelTest {
    private fun inputs(
        sdk: Int = 34, lowRam: Boolean = false, saver: Boolean = false,
        transparency: Boolean = false, motion: Boolean = false, blur: Boolean = false,
    ) = GlassInputs(sdk, lowRam, saver, transparency, motion, blur)

    @Test fun fullOnCapableDevice() = assertEquals(GlassLevel.FULL, decideGlassLevel(inputs()))
    @Test fun solidBelowAndroid12() = assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(sdk = 30)))
    @Test fun solidWhenTransparencyReduced() = assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(transparency = true)))
    @Test fun solidWhenBlurReduced() = assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(blur = true)))
    @Test fun simplifiedOnLowRam() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(lowRam = true)))
    @Test fun simplifiedOnBatterySaver() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(saver = true)))
    @Test fun simplifiedWhenMotionReduced() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(motion = true)))
    @Test fun simplifiedOnAndroid12WithoutLens() = assertEquals(GlassLevel.SIMPLIFIED, decideGlassLevel(inputs(sdk = 31)))
    @Test fun solidBeatsSimplified() =
        assertEquals(GlassLevel.SOLID, decideGlassLevel(inputs(lowRam = true, transparency = true)))

    @Test fun budgetCapsLiveLayersAtThree() {
        val b = GlassBudget()
        assertTrue(b.tryAcquire()); assertTrue(b.tryAcquire()); assertTrue(b.tryAcquire())
        assertFalse(b.tryAcquire())
        b.release()
        assertTrue(b.tryAcquire())
        assertEquals(3, b.inUse)
    }

    @Test fun budgetReleaseNeverGoesNegative() {
        val b = GlassBudget()
        b.release()
        assertEquals(0, b.inUse)
    }
}

class UiVariantTest {
    @Test fun unknownAndMissingAreCurrent() {
        assertEquals(UiVariant.CURRENT, UiVariant.parse(null))
        assertEquals(UiVariant.CURRENT, UiVariant.parse("FUTURE_THING"))
        assertEquals(UiVariant.CURRENT, UiVariant.parse(""))
    }

    @Test fun namesRoundTrip() {
        for (v in UiVariant.entries) assertEquals(v, UiVariant.parse(v.name))
    }
}

class UiSafeModeLogicTest {
    private class FakeStore : SafeModeStore {
        override var startMarker = false
        override var failedStarts = 0
        override var revertNotice = false
    }

    private val store = FakeStore()
    private fun launch(wanted: UiVariant, reachesFirstScreen: Boolean): UiVariant {
        val logic = UiSafeModeLogic(store)
        val run = logic.startupCheck(wanted)
        if (run == UiVariant.NEW) {
            logic.begin()
            if (reachesFirstScreen) logic.shown()
        }
        return run
    }

    @Test fun currentUiNeverFails() {
        repeat(5) { assertEquals(UiVariant.CURRENT, launch(UiVariant.CURRENT, false)) }
        assertFalse(store.revertNotice)
    }

    @Test fun healthyStartsStayNew() {
        repeat(5) { assertEquals(UiVariant.NEW, launch(UiVariant.NEW, true)) }
        assertEquals(0, store.failedStarts)
        assertFalse(store.startMarker)
    }

    @Test fun oneFailedStartIsForgiven() {
        assertEquals(UiVariant.NEW, launch(UiVariant.NEW, false)) // dies before the first screen
        assertEquals(UiVariant.NEW, launch(UiVariant.NEW, true)) // next one is fine
        assertEquals(0, store.failedStarts)
        assertEquals(UiVariant.NEW, launch(UiVariant.NEW, true))
    }

    @Test fun twoConsecutiveFailedStartsRevertWithANotice() {
        assertEquals(UiVariant.NEW, launch(UiVariant.NEW, false)) // fails
        assertEquals(UiVariant.NEW, launch(UiVariant.NEW, false)) // fails again
        assertEquals(UiVariant.CURRENT, launch(UiVariant.NEW, false)) // the third start sees two failures
        assertTrue(store.revertNotice)
        assertEquals(0, store.failedStarts)
    }

    @Test fun noticeIsShownOnce() {
        launch(UiVariant.NEW, false); launch(UiVariant.NEW, false); launch(UiVariant.NEW, false)
        val logic = UiSafeModeLogic(store)
        assertTrue(logic.consumeRevertNotice())
        assertFalse(logic.consumeRevertNotice())
    }

    @Test fun failuresMustBeConsecutive() {
        launch(UiVariant.NEW, false) // fail
        launch(UiVariant.NEW, true) // success resets the count
        launch(UiVariant.NEW, false) // fail
        assertEquals(UiVariant.NEW, launch(UiVariant.NEW, false)) // second since the reset, still allowed
        assertEquals(UiVariant.CURRENT, launch(UiVariant.NEW, false))
    }
}
