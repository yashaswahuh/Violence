package com.music.violence.ui.theme

import com.music.violence.ui.graphics.Swatch
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkPaletteTest {
    @Test
    fun grayscaleDoesNotAcquireTheDefaultRedHue() {
        assertEquals(0f, adaptedArtworkSaturation(0f, minimum = 0.20f, maximum = 0.62f), 0f)
        assertEquals(0f, adaptedArtworkSaturation(0f, minimum = 0.55f, maximum = 1f), 0f)
    }

    @Test
    fun nearlyNeutralArtworkKeepsItsSubtleSaturation() {
        assertEquals(0.08f, adaptedArtworkSaturation(0.08f, minimum = 0.20f, maximum = 0.62f), 0f)
    }

    @Test
    fun genuinelyChromaticArtworkIsStillStrengthenedAndCapped() {
        assertEquals(0.55f, adaptedArtworkSaturation(0.25f, minimum = 0.55f, maximum = 1f), 0f)
        assertEquals(0.62f, adaptedArtworkSaturation(0.90f, minimum = 0.20f, maximum = 0.62f), 0f)
    }

    @Test
    fun grayscaleCoverWithTinyPurpleNoiseRejectsPurple() {
        val darkDominant = Swatch(rgb = 0xFF141414.toInt(), population = 15000)
        // High saturation, vivid purple noise (pop = 50, ~0.3% of total)
        val tinyPurpleNoise = Swatch(rgb = 0xFF8C14DC.toInt(), population = 50)
        val swatches = listOf(darkDominant, tinyPurpleNoise)

        val selected = selectVibrantSwatch(swatches, darkDominant)
        assertEquals(darkDominant.rgb, selected.rgb)
    }

    @Test
    fun darkCoverWithTwoPercentPurpleFringeRejectsPurple() {
        val darkDominant = Swatch(rgb = 0xFF181818.toInt(), population = 14000)
        // 2.1% purple fringe (pop = 300 out of 14300)
        val purpleFringe = Swatch(rgb = 0xFFA028F0.toInt(), population = 300)
        val swatches = listOf(darkDominant, purpleFringe)

        val selected = selectVibrantSwatch(swatches, darkDominant)
        assertEquals(darkDominant.rgb, selected.rgb)
    }

    @Test
    fun warmAcousticCoverWithSmallPurpleElementChoosesWarmAccent() {
        val darkBackground = Swatch(rgb = 0xFF1E1914.toInt(), population = 8000)
        // Warm amber / acoustic guitar wood (hue ~32°, pop = 4000, ~33%)
        val warmAmber = Swatch(rgb = 0xFFD28228.toInt(), population = 4000)
        // Stray 1% purple detail
        val purpleStray = Swatch(rgb = 0xFF961EE6.toInt(), population = 120)
        val swatches = listOf(darkBackground, warmAmber, purpleStray)

        val selected = selectVibrantSwatch(swatches, darkBackground)
        assertEquals(warmAmber.rgb, selected.rgb)
    }

    @Test
    fun genuinePurpleCoverSelectsPurple() {
        val darkBackground = Swatch(rgb = 0xFF201528.toInt(), population = 5000)
        // Real purple album theme: 25% of cover, high chroma and saturation
        val genuinePurple = Swatch(rgb = 0xFF9632E6.toInt(), population = 3500)
        val deepViolet = Swatch(rgb = 0xFF3C145A.toInt(), population = 5500)
        val swatches = listOf(deepViolet, darkBackground, genuinePurple)

        val selected = selectVibrantSwatch(swatches, deepViolet)
        assertEquals(genuinePurple.rgb, selected.rgb)
    }

    @Test
    fun vibrantElementBeatsMutedDominantBackground() {
        val darkGreyBackground = Swatch(rgb = 0xFF303030.toInt(), population = 10000)
        // Prominent vibrant red title/artwork (4% of cover, high chroma)
        val vibrantRed = Swatch(rgb = 0xFFE02020.toInt(), population = 600)
        // Muted dark blue (pop = 3000, 20%, low chroma)
        val mutedBlue = Swatch(rgb = 0xFF253545.toInt(), population = 3000)
        val swatches = listOf(darkGreyBackground, mutedBlue, vibrantRed)

        val selected = selectVibrantSwatch(swatches, darkGreyBackground)
        assertEquals(vibrantRed.rgb, selected.rgb)
    }
}
