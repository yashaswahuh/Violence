package com.music.violence.ui.violencestyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Unslop's copy rules (adapted from heydamir/unslop, CC BY 4.0). */
class UnslopTest {

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

    @Test fun aNameIsNeverChanged() {
        // A heading's own capitals and punctuation inside the text stay: only its ending is tidied.
        assertEquals("Top Picks for You", UnslopCopy.clean("Top Picks for You"))
        assertEquals("Songs by Seedhe Maut", UnslopCopy.clean("Songs by Seedhe Maut"))
    }

    @Test fun onlyAShoutedLabelIsSetInSentenceCase() {
        assertEquals("Paid this month", UnslopCopy.sentenceCase("PAID THIS MONTH"))
        assertEquals("Songs by Seedhe Maut", UnslopCopy.sentenceCase("Songs by Seedhe Maut"))
        assertFalse(UnslopCopy.isShouted("TV"))
    }

    @Test fun theStyleStateIsNotUnsloppedByDefault() {
        assertFalse(StyleState.DISABLED.unslop)
    }
}
