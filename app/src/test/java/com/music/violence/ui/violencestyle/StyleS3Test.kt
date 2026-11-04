package com.music.violence.ui.violencestyle

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolenceStyleTextTest {

    @Test fun splitsTheMockupHeadingAtFor() {
        val h = ViolenceStyleText.split("Top Picks for You")
        assertEquals("TOP PICKS", h.caps)
        assertEquals("for You", h.italic)
    }

    @Test fun splitIsCaseInsensitiveAndKeepsTheOriginalRemainder() {
        val h = ViolenceStyleText.split("Made For Your Mood")
        assertEquals("MADE", h.caps)
        assertEquals("For Your Mood", h.italic)
    }

    @Test fun titlesWithoutForAreSingleCapsLines() {
        val h = ViolenceStyleText.split("Recents")
        assertEquals("RECENTS", h.caps)
        assertNull(h.italic)
        assertNull(ViolenceStyleText.split("Listen again").italic)
    }

    @Test fun degenerateTitlesDoNotSplit() {
        assertNull(ViolenceStyleText.split("for You").italic)       // nothing before "for"
        assertNull(ViolenceStyleText.split("Top picks for ").italic) // nothing after
        assertNull(ViolenceStyleText.split("Perform").italic)
        assertEquals("", ViolenceStyleText.split("   ").caps)
    }

    @Test fun headingFontScaleIsCappedAtOnePointThree() {
        assertEquals(1.0f, ViolenceStyleText.cappedFontScale(1.0f), 0f)
        assertEquals(1.3f, ViolenceStyleText.cappedFontScale(1.3f), 0f)
        assertEquals(1.3f, ViolenceStyleText.cappedFontScale(2.0f), 0f)
        assertEquals(0.85f, ViolenceStyleText.cappedFontScale(0.85f), 0f)
    }

    @Test fun hairlineShortensThenDisappearsBeforeTheTitleIsSqueezed() {
        // 360 px row, 12 px gaps, 24 px minimum.
        assertEquals(360 - 100 - 12, ViolenceStyleText.hairlineWidth(360, 100, 0, 12, 24))
        assertEquals(360 - 100 - 60 - 24, ViolenceStyleText.hairlineWidth(360, 100, 60, 12, 24))
        assertEquals(0, ViolenceStyleText.hairlineWidth(360, 330, 0, 12, 24))
        assertEquals(0, ViolenceStyleText.hairlineWidth(360, 100, 230, 12, 24))
    }

    @Test fun cardScrimAlphaIsMonotonicAndStrongEnoughForWhiteTextOnWhiteArtwork() {
        var prev = -1f
        var f = 0f
        while (f <= 1f) {
            val a = ViolenceScrim.alphaAt(f)
            assertTrue(a >= prev - 1e-6f); prev = a; f += 0.01f
        }
        assertEquals(0f, ViolenceScrim.alphaAt(0.2f), 0f)
        assertEquals(ViolenceScrim.END_ALPHA, ViolenceScrim.alphaAt(1f), 1e-6f)
        // Worst case: pure white artwork under the scrim, caption label at 70 percent of the card height
        // (white at 90 percent), title below it. Small text needs 4.5:1, the large title 3:1.
        for (fraction in listOf(0.70f, 0.78f, 0.9f)) {
            val grey = (255 * (1f - ViolenceScrim.alphaAt(fraction))).toInt()
            val bg = AccentMath.argb(255, grey, grey, grey)
            val label = AccentMath.argb(255, (0.9f * 255 + 0.1f * grey).toInt(), (0.9f * 255 + 0.1f * grey).toInt(), (0.9f * 255 + 0.1f * grey).toInt())
            assertTrue("label at $fraction: ${AccentMath.contrastRatio(label, bg)}", AccentMath.contrastRatio(label, bg) >= 4.5)
            assertTrue("title at $fraction", AccentMath.contrastRatio(AccentMath.WHITE, bg) >= 3.0)
        }
    }
}

class ViolenceChromeTest {

    @Test fun glassTintIsTheDarkBaseWithAHintOfTheAccentAtFortyFivePercent() {
        for (preset in AccentPreset.entries) {
            val t = ViolenceChromeColors.tint(AccentTokens.from(preset.argb).accent)
            assertEquals(115.0, AccentMath.alpha(t).toDouble(), 1.0)
            // Dark enough for white icons: the tint over black keeps a high contrast ratio.
            val opaque = AccentMath.argb(255, AccentMath.red(t), AccentMath.green(t), AccentMath.blue(t))
            assertTrue(preset.name, AccentMath.contrastRatio(AccentMath.WHITE, opaque) >= 7.0)
        }
    }

    @Test fun glassTintMovesTowardTheAccent() {
        val blue = ViolenceChromeColors.tint(0xFF3F7BFF.toInt())
        val red = ViolenceChromeColors.tint(0xFFE5384F.toInt())
        assertTrue(AccentMath.blue(blue) > AccentMath.blue(red))
        assertTrue(AccentMath.red(red) > AccentMath.red(blue))
    }
}

class ViolenceHeadingTokensTest {
    private val styles = ViolenceHeadingStyles.build(ViolenceBodoniModa)

    @Test fun sizesMatchTheSpec() {
        assertEquals(36f, styles.displayCaps.fontSize.value, 0f)
        assertEquals(24f, styles.sectionCaps.fontSize.value, 0f)
        assertEquals(20f, styles.cardTitleCaps.fontSize.value, 0f)
        assertEquals(13f, styles.label.fontSize.value, 0f)
    }

    @Test fun italicVariantIsItalicAndTheOthersAreNot() {
        assertEquals(FontStyle.Italic, styles.displayItalic.fontStyle)
        assertTrue(styles.displayCaps.fontStyle == null || styles.displayCaps.fontStyle == FontStyle.Normal)
    }

    @Test fun theDisplaySerifIsOnlyUsedForHeadingsNeverForSmallBodyText() {
        // Every serif style is 20 sp or larger; the body styles use Inter.
        for (s in listOf(styles.displayCaps, styles.displayItalic, styles.sectionCaps, styles.cardTitleCaps)) {
            assertTrue(s.fontSize.value >= 20f)
            assertEquals(ViolenceBodoniModa, s.fontFamily)
        }
        assertEquals(ViolenceInter, styles.label.fontFamily)
        assertEquals(ViolenceInter, styles.labelCaps.fontFamily)
    }

    @Test fun antonIsAvailableAndHeavy() {
        assertSame(ViolenceAnton, StyleHeadingFont.ANTON.family())
        assertEquals(3, StyleHeadingFont.entries.size)
    }

    @Test fun cormorantAndBodoniAreBothAvailable() {
        assertSame(ViolenceCormorantGaramond, StyleHeadingFont.CORMORANT_GARAMOND.family())
        assertSame(ViolenceBodoniModa, StyleHeadingFont.BODONI_MODA.family())
    }
}

class ViolenceS3OffParityTest {

    private fun sharedStyle(): ViolenceSharedStyle {
        val h = ViolenceHeadingStyles.build(ViolenceBodoniModa)
        return ViolenceSharedStyle(
            accent = Color.Blue, accentSoft = Color.Blue, textAccent = Color.Blue, neutral = false, strength = 0.62f,
            displayCaps = h.displayCaps, displayItalic = h.displayItalic, sectionCaps = h.sectionCaps,
            cardTitleCaps = h.cardTitleCaps, label = h.label,
        )
    }

    @Test fun cardRimReturnsTheSameModifierWhenTheStyleIsOff() {
        val m: Modifier = Modifier
        assertSame(m, m.violenceCardRim(null, first = true, shape = RectangleShape))
        assertSame(m, m.violenceCardRim(null, first = false, shape = RectangleShape))
    }

    @Test fun cardRimAddsDrawingWhenOn() {
        val m: Modifier = Modifier
        assertFalse(m === m.violenceCardRim(sharedStyle(), first = true, shape = RectangleShape))
    }

    @Test fun glassTintAndEdgeAreOffWhenTheStyleIsOff() {
        // The composable helpers read LocalViolenceStyle, whose default is DISABLED: nothing is applied.
        assertFalse(StyleState.DISABLED.enabled)
        assertFalse(StyleState.DISABLED.showsBackground())
    }
}
