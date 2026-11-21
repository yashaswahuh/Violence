package com.music.violence.ui.violencestyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unslop's values and copy rules (adapted from heydamir/unslop, CC BY 4.0). */
class UnslopTest {

    @Test fun primaryAndSecondaryTextReadOnEverySurfaceOfTheRamp() {
        for (light in listOf(false, true)) {
            for (surface in listOf(UnslopTokens.page(light), UnslopTokens.card(light), UnslopTokens.raised(light))) {
                val primary = AccentMath.contrastRatio(UnslopTokens.text(light), surface)
                val secondary = AccentMath.contrastRatio(UnslopTokens.textSecondary(light), surface)
                assertTrue("primary $primary light=$light", primary >= 7.0)
                assertTrue("secondary $secondary light=$light", secondary >= AccentMath.SMALL_TEXT_CONTRAST - 0.01)
            }
        }
    }

    @Test fun mutedTextStaysNearTheSmallTextContrastOnThePage() {
        // Muted is for meta and captions: close to 4.5:1, as the ramp's own note says.
        for (light in listOf(false, true)) {
            val muted = AccentMath.contrastRatio(UnslopTokens.textMuted(light), UnslopTokens.page(light))
            assertTrue("muted $muted light=$light", muted >= 4.0)
        }
    }

    @Test fun surfacesStepUpInOrder() {
        // Darkest to lightest on the dark ramp, and the reverse on the light one.
        assertTrue(AccentMath.relativeLuminance(UnslopTokens.DARK_DEEPEST) < AccentMath.relativeLuminance(UnslopTokens.DARK_PAGE))
        assertTrue(AccentMath.relativeLuminance(UnslopTokens.DARK_PAGE) < AccentMath.relativeLuminance(UnslopTokens.DARK_CARD))
        assertTrue(AccentMath.relativeLuminance(UnslopTokens.DARK_CARD) < AccentMath.relativeLuminance(UnslopTokens.DARK_RAISED))
        assertTrue(AccentMath.relativeLuminance(UnslopTokens.LIGHT_RAISED) < AccentMath.relativeLuminance(UnslopTokens.LIGHT_PAGE))
    }

    @Test fun theGraphiteRampHasNoColourInIt() {
        // Surfaces stay grey: the song supplies the colour. Allow a step of 3 in a channel at most.
        for (c in listOf(UnslopTokens.DARK_PAGE, UnslopTokens.DARK_CARD, UnslopTokens.DARK_RAISED, UnslopTokens.LIGHT_PAGE, UnslopTokens.LIGHT_RAISED)) {
            val r = AccentMath.red(c); val g = AccentMath.green(c); val b = AccentMath.blue(c)
            assertTrue("$c", maxOf(r, g, b) - minOf(r, g, b) <= 3)
        }
    }

    @Test fun everythingSitsOnTheFourDpGrid() {
        assertEquals(12, UnslopTokens.onGrid(10))
        assertEquals(8, UnslopTokens.onGrid(8))
        assertEquals(4, UnslopTokens.onGrid(1))
        assertEquals(28, UnslopTokens.onGrid(27))
        assertEquals(16, UnslopTokens.RADIUS_CARD)
    }

    @Test fun nestedRadiusIsTheOuterLessThePaddingAndNeverBelowFour() {
        assertEquals(8, UnslopTokens.nestedRadius(16, 8))
        assertEquals(12, UnslopTokens.nestedRadius(16, 4))
        assertEquals(4, UnslopTokens.nestedRadius(10, 8))
    }

    @Test fun aLabelEndsWithoutAnExclamationMarkOrAPeriod() {
        assertEquals("Saved", UnslopCopy.clean("Saved!"))
        assertEquals("Saved", UnslopCopy.clean("Saved!!"))
        assertEquals("Done", UnslopCopy.clean("Done."))
        assertEquals("Search", UnslopCopy.clean("  Search  "))
    }

    @Test fun aParagraphOfSeveralSentencesKeepsItsPeriods() {
        val text = "Plays in the background. Pauses on a call."
        assertEquals(text, UnslopCopy.clean(text))
        assertEquals("Wait...", UnslopCopy.clean("Wait..."))
    }

    @Test fun onlyAShoutedLabelIsSetInSentenceCase() {
        assertEquals("Paid this month", UnslopCopy.sentenceCase("PAID THIS MONTH"))
        assertEquals("Top picks", UnslopCopy.sentenceCase("TOP PICKS"))
        // Mixed case may hold a name, so it is left alone.
        assertEquals("Songs by Seedhe Maut", UnslopCopy.sentenceCase("Songs by Seedhe Maut"))
        // Short acronyms are not shouted labels.
        assertFalse(UnslopCopy.isShouted("TV"))
    }

    @Test fun theStyleStateIsNotUnsloppedByDefault() {
        assertFalse(StyleState.DISABLED.unslop)
    }
}
