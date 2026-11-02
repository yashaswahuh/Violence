package com.music.bitchord.ui.brutal.glass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.theme.BrutalAccentRules
import com.music.bitchord.ui.brutal.theme.ContrastMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val ALL_LEVELS = GlassLevel.entries
private val BOOLS = listOf(false, true)

class GlassPlanTest {
    @Test fun solidDrawsNoGlassLayersOnAnyKind() {
        for (kind in GlassKind.entries) for (glow in BOOLS) for (scrim in BOOLS) {
            val p = planGlass(kind, GlassLevel.SOLID, glow, scrim)
            val label = "$kind glow=$glow"
            assertEquals(label, BackdropSource.NONE, p.backdrop)
            assertFalse(label, p.samplesBackdrop)
            assertFalse(label, p.scrim || p.tint || p.gloss || p.glint || p.bevel || p.arcs || p.rim || p.edge || p.frame)
            assertFalse(label, p.bleed || p.rimTint || p.halo)
            assertTrue(label, p.hairline)
            assertEquals(label, kind != GlassKind.Tile, p.solidFill)
            assertEquals("$label accent hairline follows glow", glow, p.accentHairline)
            assertEquals("$label static bleed only on bars and panels", glow && (kind == GlassKind.Floating || kind == GlassKind.Panel), p.staticBleed)
        }
    }

    @Test fun tilesAndChipsNeverSampleABackdrop() {
        for (kind in listOf(GlassKind.Tile, GlassKind.Chip)) for (level in ALL_LEVELS) for (glow in BOOLS) {
            assertFalse("$kind $level", planGlass(kind, level, glow, textScrim = true).samplesBackdrop)
        }
    }

    @Test fun backdropSourceFollowsKindAtFull() {
        assertEquals(BackdropSource.LIVE, planGlass(GlassKind.Floating, GlassLevel.FULL, true, true).backdrop)
        assertEquals(BackdropSource.STILL, planGlass(GlassKind.Panel, GlassLevel.FULL, true, true).backdrop)
        assertEquals(BackdropSource.NONE, planGlass(GlassKind.Tile, GlassLevel.FULL, true, true).backdrop)
        assertEquals(BackdropSource.NONE, planGlass(GlassKind.Chip, GlassLevel.FULL, true, true).backdrop)
    }

    @Test fun simplifiedNeverSamplesAnything() {
        for (kind in GlassKind.entries) for (glow in BOOLS) {
            assertEquals(BackdropSource.NONE, planGlass(kind, GlassLevel.SIMPLIFIED, glow, true).backdrop)
        }
    }

    @Test fun noGlowNeverUsesTheAccent() {
        for (kind in GlassKind.entries) for (level in ALL_LEVELS) {
            val p = planGlass(kind, level, glow = false, textScrim = true)
            val label = "$kind $level"
            assertFalse("$label bleed", p.bleed)
            assertFalse("$label rimTint", p.rimTint)
            assertFalse("$label halo", p.halo)
            assertFalse("$label accentHairline", p.accentHairline)
            assertFalse("$label staticBleed", p.staticBleed)
        }
    }

    @Test fun glowAddsTheThreeAccentLayersWhereTheyFit() {
        for (level in listOf(GlassLevel.FULL, GlassLevel.SIMPLIFIED)) {
            val bar = planGlass(GlassKind.Floating, level, glow = true, textScrim = true)
            assertTrue(bar.bleed && bar.rimTint && bar.halo)
            val panel = planGlass(GlassKind.Panel, level, glow = true, textScrim = true)
            assertTrue(panel.bleed && panel.rimTint && panel.halo)
            val tile = planGlass(GlassKind.Tile, level, glow = true, textScrim = false)
            assertTrue("tile frame glows at its rim, not over the artwork", tile.rimTint && tile.halo && !tile.bleed)
            val chip = planGlass(GlassKind.Chip, level, glow = true, textScrim = false)
            assertTrue("a chip only gets a tinted rim", chip.rimTint && !chip.halo && !chip.bleed)
        }
    }

    @Test fun tileIsAFrameNotABody() {
        val p = planGlass(GlassKind.Tile, GlassLevel.SIMPLIFIED, glow = false, textScrim = true)
        assertTrue(p.frame)
        assertFalse(p.tint)
        assertFalse("artwork is not scrimmed", p.scrim)
        assertFalse(p.gloss)
    }

    @Test fun aPanelOnItsStillLeavesTheBodyToTheAppsOwnPanelGlass() {
        val full = planGlass(GlassKind.Panel, GlassLevel.FULL, glow = false, textScrim = true)
        assertFalse("the app's panel glass paints the scrim", full.scrim)
        assertFalse("and the tint", full.tint)
        val simple = planGlass(GlassKind.Panel, GlassLevel.SIMPLIFIED, glow = false, textScrim = true)
        assertTrue(simple.scrim && simple.tint)
    }

    @Test fun textScrimIsOptional() {
        assertFalse(planGlass(GlassKind.Floating, GlassLevel.FULL, false, textScrim = false).scrim)
        assertTrue(planGlass(GlassKind.Floating, GlassLevel.FULL, false, textScrim = true).scrim)
    }

    @Test fun everyNonSolidSurfaceHasBevelArcsRimAndEdgeExceptChipBevel() {
        for (kind in GlassKind.entries) for (level in listOf(GlassLevel.FULL, GlassLevel.SIMPLIFIED)) {
            val p = planGlass(kind, level, false, true)
            assertTrue("$kind $level arcs", p.arcs)
            assertTrue("$kind $level rim", p.rim)
            assertTrue("$kind $level edge", p.edge)
            assertEquals("$kind $level bevel", kind != GlassKind.Chip, p.bevel)
        }
    }

    // ---- effectiveLevel ----

    @Test fun simplifiedAndSolidAreKeptAsAsked() {
        for (kind in GlassKind.entries) for (granted in BOOLS) for (still in BOOLS) for (list in BOOLS) {
            assertEquals(GlassLevel.SIMPLIFIED, effectiveLevel(GlassLevel.SIMPLIFIED, kind, granted, still, list))
            assertEquals(GlassLevel.SOLID, effectiveLevel(GlassLevel.SOLID, kind, granted, still, list))
        }
    }

    @Test fun aFloatingSurfaceNeedsASlotAndNoListToStayFull() {
        assertEquals(GlassLevel.FULL, effectiveLevel(GlassLevel.FULL, GlassKind.Floating, true, false, false))
        assertEquals(GlassLevel.SIMPLIFIED, effectiveLevel(GlassLevel.FULL, GlassKind.Floating, false, false, false))
        assertEquals(GlassLevel.SIMPLIFIED, effectiveLevel(GlassLevel.FULL, GlassKind.Floating, true, false, true))
    }

    @Test fun aPanelNeedsAStill() {
        assertEquals(GlassLevel.FULL, effectiveLevel(GlassLevel.FULL, GlassKind.Panel, false, true, false))
        assertEquals(GlassLevel.SIMPLIFIED, effectiveLevel(GlassLevel.FULL, GlassKind.Panel, false, false, false))
    }

    @Test fun tilesAndChipsAreNeverFull() {
        for (kind in listOf(GlassKind.Tile, GlassKind.Chip)) {
            assertEquals(GlassLevel.SIMPLIFIED, effectiveLevel(GlassLevel.FULL, kind, true, true, false))
        }
    }
}

class GlassGeometryTest {
    private val density = Density(1f)

    @Test fun stadiumRadiiAreHalfTheShortSide() {
        val r = cornerRadiiPx(RoundedCornerShape(percent = 50), Size(300f, 80f), density, LayoutDirection.Ltr)
        assertEquals(40f, r.topLeft, 0.01f)
        assertEquals(40f, r.bottomRight, 0.01f)
        assertEquals(40f, r.largest, 0.01f)
    }

    @Test fun radiiAreCappedAtHalfTheShortSide() {
        val r = cornerRadiiPx(RoundedCornerShape(500.dp), Size(300f, 80f), density, LayoutDirection.Ltr)
        assertEquals(40f, r.topLeft, 0.01f)
    }

    @Test fun startAndEndSwapInRightToLeft() {
        val shape = RoundedCornerShape(topStart = 10.dp, topEnd = 20.dp, bottomEnd = 30.dp, bottomStart = 40.dp)
        val ltr = cornerRadiiPx(shape, Size(300f, 300f), density, LayoutDirection.Ltr)
        assertEquals(10f, ltr.topLeft, 0.01f)
        assertEquals(20f, ltr.topRight, 0.01f)
        assertEquals(30f, ltr.bottomRight, 0.01f)
        assertEquals(40f, ltr.bottomLeft, 0.01f)
        val rtl = cornerRadiiPx(shape, Size(300f, 300f), density, LayoutDirection.Rtl)
        assertEquals(20f, rtl.topLeft, 0.01f)
        assertEquals(10f, rtl.topRight, 0.01f)
        assertEquals(40f, rtl.bottomRight, 0.01f)
        assertEquals(30f, rtl.bottomLeft, 0.01f)
    }

    @Test fun arcReachIsAboutTwoAndAHalfRadiiWithinBounds() {
        val size = Size(300f, 80f)
        assertEquals(40f * ARC_REACH_RADII, arcReachPx(40f, size, minReachPx = 28f), 0.01f)
        assertEquals("small corners still get a readable arc", 28f, arcReachPx(4f, size, minReachPx = 28f), 0.01f)
        assertEquals("never longer than half the long side", 150f, arcReachPx(400f, size, minReachPx = 28f), 0.01f)
    }

    @Test fun haloAlphaSumsToTheTotalAtTheEdgeAndFallsToZero() {
        val steps = haloSteps(totalAlpha = 0.30f, reachPx = 21f)
        assertEquals(HALO_STEPS, steps.size)
        assertEquals(0.30f, steps.sumOf { it.alpha.toDouble() }.toFloat(), 1e-4f)
        // Alpha never increases going out, and the bands widen to twice the reach.
        for (i in 1 until steps.size) {
            assertTrue("alpha non-increasing at $i", steps[i].alpha <= steps[i - 1].alpha + 1e-6f)
            assertTrue("width increasing at $i", steps[i].widthPx > steps[i - 1].widthPx)
        }
        assertEquals(42f, steps.last().widthPx, 1e-3f)
        assertTrue(steps.all { it.alpha >= 0f })
    }

    @Test fun haloFollowsTheQuadraticFalloffItPromises() {
        val total = 0.4f
        val steps = haloSteps(total, reachPx = 20f, count = 5)
        // Summed alpha inside band i is total * (1 - i/5)^2.
        for (i in 0 until 5) {
            val summed = steps.drop(i).sumOf { it.alpha.toDouble() }.toFloat()
            val d = 1f - i / 5f
            assertEquals("band $i", total * d * d, summed, 1e-4f)
        }
    }

    @Test fun noHaloWithoutAlphaOrReach() {
        assertTrue(haloSteps(0f, 20f).isEmpty())
        assertTrue(haloSteps(0.3f, 0f).isEmpty())
    }

    @Test fun bleedSitsAtTheBottomStartAndScalesWithTheSurface() {
        val small = bleedSpec(Size(300f, 80f))
        assertEquals("the wash starts on the bottom edge", 80f, small.linearStart.y, 0.01f)
        assertTrue("and climbs to the upper half", small.linearEnd.y < 80f / 2f)
        assertTrue(small.linearStart.x < small.hotCenter.x + 1f && small.hotCenter.x < 300f / 2f)
        assertTrue("the end hot spot is in the end corner", small.endCenter.x > 300f / 2f)
        assertTrue(small.hotRadius > 0f && small.endRadius > 0f)
        val big = bleedSpec(Size(600f, 160f))
        assertEquals(160f, big.linearStart.y, 0.01f)
        assertTrue(big.hotRadius > small.hotRadius)
    }

    @Test fun smallSurfacesGetLessBleed() {
        assertEquals(1f, bleedScaleFor(80f), 1e-6f)
        assertEquals(1f, bleedScaleFor(120f), 1e-6f)
        assertEquals(0.5f, bleedScaleFor(48f), 1e-6f)
        assertEquals(0.5f, bleedScaleFor(24f), 1e-6f)
        assertEquals(0.75f, bleedScaleFor(64f), 1e-6f)
    }

    @Test fun glossStaysInTheEndOfTheSurface() {
        val (start, end) = glossLine(Size(300f, 80f))
        assertEquals(300f, start.x, 0.01f)
        assertEquals(0f, start.y, 0.01f)
        assertTrue("it never reaches the start half", end.x >= 300f / 2f)
    }
}

class GlassStyleTest {
    private val yellow = BrutalAccentRules.derive(AccentPreset.YELLOW.argb)

    @Test fun noGlowHasNoAccentAnywhere() {
        for (kind in GlassKind.entries) {
            val s = glassStyle(kind, glow = false, accent = yellow)
            assertEquals(Color.Transparent, s.bleed)
            assertEquals(Color.Transparent, s.bleedEnd)
            assertEquals(Color.Transparent, s.rimTint)
            assertEquals(Color.Transparent, s.halo)
            assertEquals(0f, s.haloAlpha, 0f)
            // The hairline is a neutral white, not the accent.
            assertEquals(Color.White.copy(alpha = 0.10f), s.hairline)
        }
    }

    @Test fun glowYellowUsesTheSpecNumbers() {
        val s = glassStyle(GlassKind.Floating, glow = true, accent = yellow)
        // 0.55 requested, lowered by the contrast cap to 0.45 for white text on the bleed.
        assertEquals(0.45f, s.bleed.alpha, 0.011f)
        assertEquals(s.bleed.alpha * 0.6f, s.bleedEnd.alpha, 0.001f)
        assertEquals(0.30f, s.haloAlpha, 0.0001f)
        assertEquals(21f, s.haloReachDp, 0f)
        // The rim tint is the accent mixed a quarter toward white: lighter, same family.
        assertTrue(s.rimTint.red >= Color(yellow.ui).red - 0.001f && s.rimTint.blue > Color(yellow.ui).blue)
        assertEquals(1f, s.rimTint.alpha, 0f)
    }

    @Test fun darkAccentsGlowHarderButStayCapped() {
        val blue = glassStyle(GlassKind.Floating, true, BrutalAccentRules.derive(AccentPreset.BLUE.argb))
        assertEquals(0.55f * 1.35f, blue.bleed.alpha, 0.011f)
        assertEquals((0.30f * 1.35f), blue.haloAlpha, 0.001f)
        val red = glassStyle(GlassKind.Floating, true, BrutalAccentRules.derive(AccentPreset.RED.argb))
        assertEquals(0.30f * 1.15f, red.haloAlpha, 0.001f)
        // However dark the accent, the under-glow stays at or under 0.5.
        for (p in AccentPreset.entries) {
            assertTrue(glassStyle(GlassKind.Floating, true, BrutalAccentRules.derive(p.argb)).haloAlpha <= 0.5f)
        }
    }

    @Test fun whiteTextOnTheBleedIsAlwaysReadable() {
        // Spec 3.4: the bleed alpha is capped so white text on it is 4.5:1 or better.
        for (p in AccentPreset.entries) {
            val acc = BrutalAccentRules.derive(p.argb)
            val s = glassStyle(GlassKind.Floating, true, acc)
            val blended = ContrastMath.over(acc.ui, s.bleed.alpha.toDouble(), BrutalAccentRules.GLASS_BODY)
            assertTrue("${p.id}: ${ContrastMath.ratio(ContrastMath.WHITE, blended)}", ContrastMath.ratio(ContrastMath.WHITE, blended) >= 4.4)
        }
    }

    @Test fun sizesFollowTheKind() {
        assertEquals(0f, glassStyle(GlassKind.Chip, false, yellow).bevelDp, 0f)
        assertEquals(6f, glassStyle(GlassKind.Floating, false, yellow).bevelDp, 0f)
        assertEquals(7f, glassStyle(GlassKind.Panel, false, yellow).bevelDp, 0f)
        assertEquals(4f, glassStyle(GlassKind.Tile, false, yellow).bevelDp, 0f)
        assertEquals(5f, glassStyle(GlassKind.Tile, false, yellow).frameDp, 0f)
        assertEquals(0f, glassStyle(GlassKind.Floating, false, yellow).frameDp, 0f)
    }

    @Test fun scrimIsTheContrastGuarantee() {
        assertEquals(ContrastMath.TEXT_SCRIM_ALPHA.toFloat(), glassStyle(GlassKind.Floating, false, yellow).scrimAlpha, 0f)
        // And that alpha really does guarantee white text over white.
        val under = ContrastMath.over(ContrastMath.BLACK, glassStyle(GlassKind.Floating, false, yellow).scrimAlpha.toDouble(), ContrastMath.WHITE)
        assertTrue(ContrastMath.ratio(ContrastMath.WHITE, under) >= 4.5)
    }

    @Test fun theHotSpotIsStrongerThanTheWashAndCappedAtPointEight() {
        for (p in AccentPreset.entries) {
            val st = glassStyle(GlassKind.Floating, true, BrutalAccentRules.derive(p.argb))
            assertTrue("${p.id}", st.bleedHot.alpha >= st.bleed.alpha && st.bleedHot.alpha <= 0.80f + 1e-4f)
        }
        assertEquals(Color.Transparent, glassStyle(GlassKind.Floating, false, yellow).bleedHot)
    }

    @Test fun tilesGetAFewerBandUnderGlowBecauseTheySitInLists() {
        assertEquals(4, glassStyle(GlassKind.Tile, true, yellow).haloSteps)
        assertEquals(HALO_STEPS, glassStyle(GlassKind.Floating, true, yellow).haloSteps)
        assertTrue(glassStyle(GlassKind.Tile, true, yellow).haloSteps < glassStyle(GlassKind.Floating, true, yellow).haloSteps)
    }

    @Test fun glossIsFainterWithGlow() {
        assertTrue(glassStyle(GlassKind.Floating, true, yellow).glossAlpha < glassStyle(GlassKind.Floating, false, yellow).glossAlpha)
    }

    @Test fun arcTopLeftIsBrighterAndThickerThanBottomRight() {
        val s = glassStyle(GlassKind.Floating, false, yellow)
        assertTrue(s.arcTopLeftAlpha > s.arcBottomRightAlpha)
        assertTrue(s.arcTopLeftDp > s.arcBottomRightDp)
    }
}

class GlassDrawCacheTest {
    private val density = Density(2f)
    private val shape = RoundedCornerShape(percent = 50)
    private val yellow = BrutalAccentRules.derive(AccentPreset.YELLOW.argb)

    private fun cache(kind: GlassKind, level: GlassLevel, glow: Boolean, size: Size = Size(600f, 160f)): GlassDrawCache {
        val plan = planGlass(kind, level, glow, textScrim = true)
        return buildGlassCache(size, density, LayoutDirection.Ltr, shape, plan, glassStyle(kind, glow, yellow))
    }

    @Test fun solidBuildsNoBrushes() {
        val c = cache(GlassKind.Floating, GlassLevel.SOLID, glow = false)
        assertNull(c.tint); assertNull(c.gloss); assertNull(c.glint); assertNull(c.bevel); assertNull(c.rim); assertNull(c.arcTopLeft)
        assertNull(c.arcBottomRight); assertNull(c.rimTint); assertNull(c.bleedLinear)
    }

    @Test fun noGlowBuildsNoAccentBrushes() {
        val c = cache(GlassKind.Floating, GlassLevel.SIMPLIFIED, glow = false)
        assertNull(c.rimTint); assertNull(c.bleedLinear); assertNull(c.bleedHot); assertNull(c.bleedEnd)
        assertNotNull(c.tint); assertNotNull(c.gloss); assertNotNull(c.glint); assertNotNull(c.bevel); assertNotNull(c.rim)
        assertNotNull(c.arcTopLeft); assertNotNull(c.arcBottomRight)
    }

    @Test fun glowBuildsTheAccentBrushes() {
        val c = cache(GlassKind.Floating, GlassLevel.SIMPLIFIED, glow = true)
        assertNotNull(c.rimTint); assertNotNull(c.bleedLinear); assertNotNull(c.bleedHot); assertNotNull(c.bleedEnd)
    }

    @Test fun strokeWidthsAreBuiltOnceInPixels() {
        // 3.5 dp arcs at a density of 2 are 2 * 3.5 * 2 = 14 px wide strokes (the outer half is clipped).
        val c = cache(GlassKind.Floating, GlassLevel.SIMPLIFIED, glow = false)
        assertEquals(2f * 3.5f * 2f, c.arcTopLeftStroke.width, 0.01f)
        assertEquals(2f * 6f * 2f, c.bevelStroke.width, 0.01f)
    }

    @Test fun tileGetsAnInnerOutlineForItsFrame() {
        val c = cache(GlassKind.Tile, GlassLevel.SIMPLIFIED, glow = false, size = Size(300f, 300f))
        assertNotNull(c.frameRing)
        assertNotNull(c.innerPath)
        assertEquals(10f, c.frameInsetPx, 0.01f)
    }

    @Test fun chipHasNoBevel() {
        assertNull(cache(GlassKind.Chip, GlassLevel.SIMPLIFIED, glow = false, size = Size(120f, 48f)).bevel)
    }
}
