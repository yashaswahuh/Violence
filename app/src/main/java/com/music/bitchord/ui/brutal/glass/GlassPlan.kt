package com.music.bitchord.ui.brutal.glass

/**
 * What a glass surface is for. It decides where the glass gets its backdrop from and how much of the
 * look it can afford (spec section 7.1).
 *
 * - [Floating]: bars, round buttons, rail tabs. Sits above the page, so it can sample it live.
 * - [Panel]: dialogs and sheets. Drawn in a window of its own, so it samples a still of the page.
 * - [Tile]: the frame around artwork inside scrolling content. **Never** a live backdrop.
 * - [Chip]: tiny pills and labels. Never a backdrop either.
 */
enum class GlassKind { Floating, Panel, Tile, Chip }

/** Where a surface's backdrop comes from. */
enum class BackdropSource {
    /** Nothing is sampled. */
    NONE,

    /** The page, live (`LocalAppBackdrop`, the app's `liquidGlass`). Counts against the budget. */
    LIVE,

    /** A still of the page (`LocalAppStill`, the app's panel glass). */
    STILL,
}

/**
 * The layers one surface draws, decided as a pure function of its kind, its level and the glow
 * setting ([planGlass]), so every combination can be asserted without rendering anything.
 *
 * Layer numbers are those of `BRUTAL_UI_SPEC.md` section 2.1.
 */
data class GlassPlan(
    /** The level actually drawn, after the budget, the list guard and a missing backdrop. */
    val level: GlassLevel,
    val backdrop: BackdropSource,
    /** SOLID only: an opaque fill instead of any translucency. */
    val solidFill: Boolean,
    /** SOLID only: a one-pixel edge in place of every bevel and arc. */
    val hairline: Boolean,
    /** Layer 2a: black scrim behind text (spec 7.4). */
    val scrim: Boolean,
    /** Layer 2b: the glass body, a white gradient (or the dense body without a backdrop). */
    val tint: Boolean,
    /** Layer 3: the diagonal gloss at the end of a bar. */
    val gloss: Boolean,
    /** The bubble highlight in the start cap of a bar or panel. */
    val glint: Boolean,
    /** Layer 6: the inner bevel band. */
    val bevel: Boolean,
    /** Layer 6: the two specular arcs, top-left bright and bottom-right softer. */
    val arcs: Boolean,
    /** Layer 7: the white rim stroke. */
    val rim: Boolean,
    /** The outermost darker hairline that separates glass from the page. */
    val edge: Boolean,
    /** Tile only: the frosted frame ring around artwork. */
    val frame: Boolean,
    /** GLOW, layer 4: accent light bleeding in from the lower edge and corner. */
    val bleed: Boolean,
    /** GLOW, layer 7: the accent-tinted rim. */
    val rimTint: Boolean,
    /** GLOW, layer 0: the outer under-glow. */
    val halo: Boolean,
    /** GLOW at SOLID: the accent hairline in place of the tinted rim. */
    val accentHairline: Boolean,
    /** GLOW at SOLID: a static bleed on bars and panels (never over a tile's artwork). */
    val staticBleed: Boolean,
) {
    /** True if this surface samples anything at all. */
    val samplesBackdrop: Boolean get() = backdrop != BackdropSource.NONE
}

/**
 * The level a surface really gets. A request for FULL is lowered when
 * - the kind never samples live (Tile, Chip): it is drawn as SIMPLIFIED;
 * - a Floating surface was not given a live slot by the budget, or sits inside a scrolling list;
 * - a Panel has no page still to sample.
 * SIMPLIFIED and SOLID are kept as asked.
 */
fun effectiveLevel(
    requested: GlassLevel,
    kind: GlassKind,
    liveSlotGranted: Boolean,
    stillAvailable: Boolean,
    inScrollingList: Boolean,
): GlassLevel {
    if (requested != GlassLevel.FULL) return requested
    return when (kind) {
        GlassKind.Tile, GlassKind.Chip -> GlassLevel.SIMPLIFIED
        GlassKind.Floating -> if (liveSlotGranted && !inScrollingList) GlassLevel.FULL else GlassLevel.SIMPLIFIED
        GlassKind.Panel -> if (stillAvailable && !inScrollingList) GlassLevel.FULL else GlassLevel.SIMPLIFIED
    }
}

/**
 * The layers for [kind] at [level] (already the *effective* level) with Glow [glow].
 *
 * NO-GLOW draws only white and grey: the accent never enters the glass. GLOW adds the three accent
 * layers (bleed, rim tint, under-glow); at SOLID it adds an accent hairline and a static bleed
 * instead. [textScrim] puts the black scrim behind text (spec 7.4); a Panel at FULL does not need
 * one because the app's own panel glass paints its scrim.
 */
fun planGlass(kind: GlassKind, level: GlassLevel, glow: Boolean, textScrim: Boolean): GlassPlan {
    val backdrop = when {
        level != GlassLevel.FULL -> BackdropSource.NONE
        kind == GlassKind.Floating -> BackdropSource.LIVE
        kind == GlassKind.Panel -> BackdropSource.STILL
        else -> BackdropSource.NONE
    }
    if (level == GlassLevel.SOLID) {
        return GlassPlan(
            level = level, backdrop = BackdropSource.NONE,
            solidFill = kind != GlassKind.Tile, hairline = true,
            scrim = false, tint = false, gloss = false, glint = false, bevel = false, arcs = false, rim = false,
            edge = false, frame = false, bleed = false, rimTint = false, halo = false,
            accentHairline = glow,
            staticBleed = glow && (kind == GlassKind.Floating || kind == GlassKind.Panel),
        )
    }
    val isTile = kind == GlassKind.Tile
    val isChip = kind == GlassKind.Chip
    val bar = kind == GlassKind.Floating || kind == GlassKind.Panel
    val panelOwnsBody = kind == GlassKind.Panel && backdrop == BackdropSource.STILL
    return GlassPlan(
        level = level,
        backdrop = backdrop,
        solidFill = false,
        hairline = false,
        scrim = textScrim && !isTile && !isChip && !panelOwnsBody,
        tint = !isTile && !panelOwnsBody,
        gloss = bar,
        glint = bar,
        bevel = !isChip,
        arcs = true,
        rim = true,
        edge = true,
        frame = isTile,
        bleed = glow && bar,
        rimTint = glow,
        halo = glow && !isChip,
        accentHairline = false,
        staticBleed = false,
    )
}
