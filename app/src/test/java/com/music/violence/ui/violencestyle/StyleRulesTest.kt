package com.music.violence.ui.violencestyle

import androidx.compose.material3.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleCapabilityTest {
    private fun reason(
        sdk: Int = 34, lowRam: Boolean = false, battery: Boolean = false, thermal: Boolean = false,
        blur: Boolean = false, forced: Boolean = false,
    ) = StyleCapabilityRules.liteReason(sdk, lowRam, battery, thermal, blur, forced)

    @Test fun fullWhenNothingAsksForLess() {
        assertNull(reason())
        assertEquals(StyleCapability.FULL, StyleCapabilityRules.capability(reason()))
    }

    @Test fun eachSignalMakesLite() {
        assertEquals(LiteReason.OLD_ANDROID, reason(sdk = 30))
        assertEquals(LiteReason.OLD_ANDROID, reason(sdk = 26))
        assertEquals(LiteReason.LOW_RAM, reason(lowRam = true))
        assertEquals(LiteReason.BATTERY_SAVER, reason(battery = true))
        assertEquals(LiteReason.THERMAL, reason(thermal = true))
        assertEquals(LiteReason.REDUCE_BLUR, reason(blur = true))
        assertEquals(LiteReason.USER_REDUCED_TRANSPARENCY, reason(forced = true))
        assertEquals(StyleCapability.LITE, StyleCapabilityRules.capability(reason(battery = true)))
    }

    @Test fun api31IsTheGlassFloor() {
        assertNull(reason(sdk = 31))
        assertEquals(LiteReason.OLD_ANDROID, reason(sdk = 30))
    }

    @Test fun oldAndroidWinsOverOtherReasons() {
        assertEquals(LiteReason.OLD_ANDROID, reason(sdk = 29, lowRam = true, battery = true))
    }

    @Test fun animatorScaleZeroMeansReduceMotion() {
        assertTrue(StyleCapabilityRules.systemReducesMotion(0f))
        assertFalse(StyleCapabilityRules.systemReducesMotion(1f))
        assertFalse(StyleCapabilityRules.systemReducesMotion(0.5f))
    }

    @Test fun reduceTransparencyIsForcedOrFollowsBlur() {
        assertTrue(StyleCapabilityRules.reduceTransparency(forced = true, reduceDynamicBlur = false))
        assertTrue(StyleCapabilityRules.reduceTransparency(forced = false, reduceDynamicBlur = true))
        assertFalse(StyleCapabilityRules.reduceTransparency(forced = false, reduceDynamicBlur = false))
    }

    private val on = StyleState.DISABLED.copy(enabled = true)

    @Test fun effectiveMotionIsOffInLiteAndUnderReduceMotion() {
        assertEquals(StyleMotion.NORMAL, on.effectiveMotion)
        assertEquals(StyleMotion.OFF, on.copy(capability = StyleCapability.LITE).effectiveMotion)
        assertEquals(StyleMotion.OFF, on.copy(reduceMotion = true).effectiveMotion)
        assertEquals(StyleMotion.OFF, on.copy(motion = StyleMotion.OFF).effectiveMotion)
    }

    @Test fun backgroundClockRunsOnlyWhenEverythingAllowsIt() {
        assertTrue(StyleCapabilityRules.backgroundClockMayRun(on, appInForeground = true, playerCoversPage = false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on, appInForeground = false, playerCoversPage = false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on, appInForeground = true, playerCoversPage = true))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on.copy(background = StyleBackground.OFF), true, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on.copy(reduceMotion = true), true, false))
        assertFalse(StyleCapabilityRules.backgroundClockMayRun(on.copy(capability = StyleCapability.LITE), true, false))
    }
}

class StyleSafeModeTest {
    @Test fun styleOffNeverTriggersAndResetsTheCounter() {
        val d = StyleSafeModeLogic.onStart(pending = 5, styleEnabled = false)
        assertEquals(0, d.pending); assertFalse(d.disableStyle); assertFalse(d.showNotice)
    }

    @Test fun firstAndSecondFailedStartsAreTolerated() {
        val first = StyleSafeModeLogic.onStart(0, true)
        assertEquals(1, first.pending); assertFalse(first.disableStyle)
        val second = StyleSafeModeLogic.onStart(first.pending, true)
        assertEquals(2, second.pending); assertFalse(second.disableStyle)
    }

    @Test fun thirdStartAfterTwoFailuresDisablesTheStyleOnceWithANotice() {
        val d = StyleSafeModeLogic.onStart(pending = 2, styleEnabled = true)
        assertTrue(d.disableStyle); assertTrue(d.showNotice); assertEquals(0, d.pending)
        // The next start has the style off: no second notice.
        val after = StyleSafeModeLogic.onStart(d.pending, styleEnabled = false)
        assertFalse(after.showNotice); assertFalse(after.disableStyle)
    }

    @Test fun aSuccessfulFirstFrameClearsTheCounter() {
        assertEquals(0, StyleSafeModeLogic.onFirstFrame())
        // Start, succeed, start, succeed: never reaches the limit.
        var pending = 0
        repeat(10) {
            val d = StyleSafeModeLogic.onStart(pending, true)
            assertFalse(d.disableStyle)
            pending = StyleSafeModeLogic.onFirstFrame()
        }
    }

    @Test fun styleCanBeEnabledAgainAfterSafeMode() {
        val off = StyleSafeModeLogic.onStart(2, true)
        // Owner turns it back on (the counter was reset to 0): the next start counts from 1.
        val again = StyleSafeModeLogic.onStart(off.pending, true)
        assertEquals(1, again.pending); assertFalse(again.disableStyle)
    }
}

class StyleOffParityTest {

    @Test fun disabledStateIsDisabledWithTheOwnersDefaults() {
        val s = StyleState.DISABLED
        assertFalse(s.enabled)
        assertEquals(StyleBackground.AURORA, s.background)
        assertEquals(StyleIntensity.MEDIUM, s.intensity)
        assertEquals(StyleAccentSource.FROM_SONG, s.accentSource)
        assertEquals(StyleMotion.NORMAL, s.motion)
        assertEquals(StyleDefaults.CUSTOM_COLOR, s.customColor)
        assertEquals(StyleMotion.OFF, s.effectiveMotion)
    }

    @Test fun darkOnlyOverrideAppliesOnlyWhileOn() {
        assertNull(ViolenceStyleSettings.forceDark(false))
        assertEquals(true, ViolenceStyleSettings.forceDark(true))
    }

    @Test fun typographyIsTheSameObjectWhenOff() {
        val base = Typography()
        assertSame(base, violenceTypography(base, enabled = false))
    }

    @Test fun typographyChangesOnlyTheFamilyAndWeightWhenOn() {
        val base = Typography()
        val styled = violenceTypography(base, enabled = true)
        assertEquals(base.bodyLarge.fontSize, styled.bodyLarge.fontSize)
        assertEquals(base.titleMedium.letterSpacing, styled.titleMedium.letterSpacing)
        assertEquals(ViolenceInter, styled.bodyMedium.fontFamily)
        assertEquals(ViolenceInter, styled.labelSmall.fontFamily)
    }

    @Test fun unknownStoredValuesFallBackToDefaults() {
        assertEquals(StyleBackground.AURORA, StylePrefsParse.enumOr<StyleBackground>(null, StyleDefaults.BACKGROUND))
        assertEquals(StyleBackground.AURORA, StylePrefsParse.enumOr<StyleBackground>("ARCS", StyleDefaults.BACKGROUND))
        assertEquals(StyleBackground.HAZE, StylePrefsParse.enumOr<StyleBackground>("HAZE", StyleDefaults.BACKGROUND))
        assertEquals(StyleMotion.SUBTLE, StylePrefsParse.enumOr<StyleMotion>("SUBTLE", StyleDefaults.MOTION))
    }

    @Test fun arcsIsNotAnOption() {
        assertTrue(StyleBackground.entries.none { it.name == "ARCS" })
        assertEquals(4, StyleBackground.entries.size) // Aurora, Corner glow, Haze, Off
    }

    @Test fun intensityPeaksMatchTheSpec() {
        assertEquals(0.38f, StyleIntensity.LOW.peakAlpha, 0f)
        assertEquals(0.62f, StyleIntensity.MEDIUM.peakAlpha, 0f)
        assertEquals(0.85f, StyleIntensity.HIGH.peakAlpha, 0f)
    }
}
