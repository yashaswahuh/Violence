# Brutal UI: design specification (phone app only)

Status: **spec only**. No app code, resource, string or Gradle change. Written 2026-11-02 on branch `violence-nk`.
Scope: the Android phone app (`:app`). TV, web, Wear OS and desktop keep their UI. Wear OS and desktop: dormant, not checked.
Companion docs: `UI_AUDIT.md` (how the current UI and glass are built), `UI_PLAN.md` (the earlier, removed "New UI" attempt; its toggle, safe-mode and glass-tier ideas are reused here), `ui-concepts/` (the earlier boards, not used for this look), `brutal-ui/type-board.html` (type and colour board for this spec).

Everything below was read from the code (paths given) or measured from the two mockups. Anything I could not establish is marked **UNCLEAR**. Anything estimated rather than measured is marked **APPROXIMATE**.

---

## 0. Read this first

### 0.1 The mockups, and one labelling conflict the owner must settle

Two images, 1254 x 1254 px each, saved by the session as `1.jpg` and `2.jpg`:

| File | Brief calls it | What it actually shows |
|---|---|---|
| image 1 | "Glow" | Clear, thick, refractive glass. White specular rims. Yellow appears only as a small wash inside the bottom of a surface (wordmark tile corner, bars, "+" tile) and in flat accents. **No outer glow, no yellow rim.** |
| image 2 | "No-Glow" | Neon glass: yellow bleed filling much of each surface, yellow rim tint, a yellow under-glow outside the bottom edge, glowing yellow strokes (the "Components" ring, the pause button). |

The brief says image 1 = Glow and image 2 = No-Glow. By appearance it is the other way round. **This spec names the two looks by appearance**:

- **Clear** = image 1 (no outer glow).
- **Neon** = image 2 (bleed, rim tint, under-glow).

and assumes the setting called **Glow** turns **Neon on** (Glow ON = image 2). If the owner meant the opposite, only the name-to-look mapping in section 3.5 changes. Open question 1 (section 8).

### 0.2 Scale: how mockup pixels become dp and sp (APPROXIMATE)

The mockup is a component board, not a screen: its content panel plus rail is 746 px wide, which is not a phone width at any scale that keeps the text readable. So there is no true px-to-dp factor. I chose one anchor and then applied the accessibility minimums on top:

- **Anchor: 1 mockup px = 0.75 dp (or sp).** It makes the smallest text in the mockup (the vertical date stamps, glyph height about 11 px, font about 15 px) come out at 11 sp, which is the smallest size the current app already uses (`labelSmall` 11 sp, `UI_AUDIT.md` section 6), and it puts the list title (26 px) at 20 sp, the artist line (17 px) at 13 sp and the chip text (16 px) at 12 sp, which are ordinary Android type steps.
- Where the result falls under a usability minimum (48 dp touch target, 11 sp text), the **token** is raised to the minimum and the raw conversion is shown next to it.
- Everything dimensionless (corner radius as a ratio, rotation angles, letter-spacing in em, alpha) is **scale free** and is the most reliable part of this spec.

How measured: both JPEGs were read pixel by pixel (System.Drawing in PowerShell, scripts kept in the session scratchpad, not committed). Flat colours are the mean of 85 to 3,200 pixels of one region, so JPEG noise is averaged out (about plus or minus 3 levels per channel). Edges and angles are least-squares fits to edge pixels. Sizes are bounding boxes of thresholded pixels. Typefaces were identified by eye from 2x to 3x crops and are APPROXIMATE.

---

## 1. Measured tokens

### 1.1 Colour (measured; hex are sRGB)

| Token | Value | Derivation |
|---|---|---|
| Background, Clear | `#070708` | Mean of dark pixels in two page areas of image 1: `#080809` (4,941 px) and `#070709` (42,441 px). |
| Background, Neon | `#0C0C0F` | Same areas of image 2: `#0E0F10` and `#0B0B0E`. It is about 5 levels lighter and cooler than image 1; the spec uses **one** token, **`#08080A`**, and flags the difference as APPROXIMATE (it may be the image's vignette). |
| **Accent, yellow** | **`#FAD60A`** | Mean of six flat patches: sticker (3,188 px) `#F9D508`, sticker (2,707) `#FBD605`, sticker in image 2 `#F8D70C`, selected genre tab `#FAD90A`, play disc `#FADA0E`, duration chip `#F3D813`. Mean = (250, 215, 10). The token is rounded to `#FAD60A`. All contrast numbers in section 3 use this value. |
| Ink (black on accent) | `#0A0A0A` | The sticker's black text measured `#070501` (mean of 4,007 px; the warm cast is JPEG). Token is neutral near-black. |
| Secondary acid, cyan emphasis | `#1FDEDB` | Mean of the cyan word "colors" in image 1 (484 px). Image 2 gives `#1DD5BF` (greener); the token takes image 1. |
| Lemon yellow for thin strokes and emphasis words | `#F1E024` | "Acidic" word `#F1E024` (image 1), `#F2E226` (image 2); the "+" glyph `#EEE12B`. This is **greener than the flat accent** (`#FAD60A`). In code: draw thin strokes and emphasis words in the accent too; the difference is a rendering effect of thin glyphs plus glow, not a second brand colour. Logged as a deviation. |
| White text | `#F9F9F9` | Mean of the bold "BRUTALISM" word (293 px). Token `#FFFFFF` is fine; `#F9F9F9` is what the mockup measured. |
| Glass body, over the page (Clear) | `#1D1D1D` | Mean of 25 px inside the mini-player bar. Equals white at **alpha 0.09** over `#070708` (computed `#1D1D1E`). The transport bar measured `#2C2C2C`, alpha about 0.17. |
| Specular rim peak (Clear) | `#707073` | Top rim line of the bar. White at alpha about 0.40 over the glass. |
| Neon rim, bottom edge | `#F9ED3A` | Brightest rim pixel of the bar in image 2. About the accent mixed 25 percent toward white. |
| Neon under-glow, outside the bottom edge | `#3F3301` at +13 px, `#312902` at +21 px, `#2C2302` at +28 px | Over the page. Alpha of the accent: 0.25, 0.19, 0.17. A long, flat tail: about 28 px (21 dp) until it is indistinguishable. |
| Neon bleed, peak | `#887623` (bar), `#897826` (PLAYING bar) | Inside the glass. Accent alpha about 0.50 to 0.55 over the glass body. Image 1's wash peaks lower: `#786C30`, accent alpha about 0.45, and it is less saturated. |

### 1.2 Shapes

| Item | Measured | Result |
|---|---|---|
| Tile (album or playlist square) | 135 x 142 px. Corner profile (dy:dx from the corner) image 1: 5:14, 10:8, 15:4, 20:2, 25:0; the straight edge starts at dy of about 25 px. | **Corner radius about 26 px = 0.19 of the tile width.** The profile is flatter near the corner than a circle (a circle of r = 26 gives dx 10.7 at dy 5; measured 14), so it is a **continuous (squircle) corner**, not a circular arc. The app already has `ContinuousCornerShape` (`app/.../ui/components/ContinuousCornerShape.kt`). Token: `0.19 x shortSide`, clamped to 12 to 28 dp. |
| Artwork inside a tile | Glass frame visible around it, about 7 px (read by eye from 2x crops: APPROXIMATE) | Frame thickness token **5 dp**; inner artwork radius = outer radius minus 5 dp. |
| List-row artwork | about 90 px square, radius about 18 px | Ratio **0.20**. |
| Play disc on a tile | 33 x 33 px | Full circle. Raw 25 dp; visual token **28 dp**. |
| Duration chip | 63 x 24 px (5 characters) | Stadium (full radius). Raw 18 dp tall; token: **min height 20 dp**, wraps with the font. |
| Rail collapse button | 61 x 59 px | Circle, raw 45 dp; token **48 dp**. |
| Rail search button | 62 x 99 px | Stadium, raw 46 x 74 dp; token **48 x 72 dp**. |
| Rail tab | 56 px wide. Heights 71 to 82 px unselected, 109 px selected (HIP-HOP) | Stadium. Width token **48 dp** (touch target). Height = rotated label length + 2 x 16 dp (the 16 dp end padding is APPROXIMATE, inferred from the pill heights; label lengths were not measured); the selected tab is not taller by design, it is just the longest label. |
| Rail gaps | 5 px between tabs; 52 px collapse to search; 37 px search to first tab | **4 dp; 40 dp; 28 dp.** |
| Mini-player bar (PLAYING) | about 415 x 105 px; artwork 73 px | Stadium. Raw 79 dp; token **80 dp** tall, artwork **56 dp**. |
| Dock bar (PAUSE + transport) | left part 480 x 125 px; whole 822 px wide; pause circle 92 px | Raw 94 dp; token **96 dp** tall, pause circle **68 dp** visual, prev and next glyphs 24 dp in 48 dp targets. |
| Wordmark glass tile | about 470 px wide (cropped by the canvas), corner radius about 0.33 x height | Masthead token: 300 x 128 dp, may bleed 24 dp past the top and end edges. |
| Sticker | Sharp corners (radius 0). Rule lines 2 px black. | Radius 0. Rule token **2 dp**. Three rows: 62 dp, 62 dp, 47 dp (perpendicular; rule spacing read from a 3x crop as 85, 85 and 65 px: APPROXIMATE). |
| Masonry gap | about 15 px | **12 dp** (4 dp grid). |
| Vertical label lane (stamp + title) | 45 px between the tile column and the next | Lane token **28 dp** plus 6 dp gap. |
| Date stamp, white vertical title | glyph height 11 px | Lane width needs 11 sp x line height; at 200 percent the lane grows (section 5). |

### 1.3 Rotation angles (fitted)

| Element | Image 1 | Image 2 | Token |
|---|---|---|---|
| Sticker (bottom edge) | -12.1 deg | -11.8 deg | **-12 deg** (counter-clockwise; Compose `rotationZ = -12f`). Its right edge fits -10.9 and -5.7 deg: the sticker also has a slight perspective skew. Not reproduced. |
| Wordmark glass tile (bottom edge) | -8.6 deg | -8.1 deg | **-8.5 deg**. |
| "EXPERIMENTAL" tag | -3.5 deg | -4.8 deg | **-4 deg** (so it sits about 4.5 deg off the tile it is on: a deliberate broken-grid detail). |
| Vertical labels, rail tab labels, "Components" | -90 deg | -90 deg | **-90 deg** (reads bottom to top). RTL handling in section 5.5. |

### 1.4 Type (identified by eye; sizes measured)

| Role | Mockup look | Measured | Token |
|---|---|---|---|
| Sticker row 1 and 2 (ABOUT, PRIVACY) | Heavy neutral grotesque, uppercase, upright. In image 2 PRIVACY is a lighter weight than ABOUT | Cap height about 47 px | **48 sp** display, weight Black (row 1), Black or Bold (row 2). Tracking 0 to -0.01 em. |
| Sticker "0.4" and "VERSION" | Bold numerals; regular caps | cap 17 px; cap 13 px | 18 sp Bold; 12 sp Medium, tracking +0.04 em. |
| Wordmark "VIOLENCE" | Geometric sans, semibold, white, extremely tracked | Cap height 15 px (rows 52 to 66); letter pitch 42 px; word spans 845 to 1149 px for 8 letters | **16 sp SemiBold, letter-spacing about 1.3 em (APPROXIMATE)**. Derivation: natural advance sum of the 8 letters at 21 px is about 103 px; (304 - 103) / 7 gaps = 28.7 px = 1.37 em; rounded down to 1.3. Equivalent rule: the gap between letters is about twice the cap height. |
| Tag "EXPERIMENTAL" | Geometric sans, bold, black on accent | Text about 145 px wide for 12 letters, cap 10 px, box about 165 x 22 px | **11 sp Bold, tracking 0.25 em**, padding 8 dp x 2 dp, rotated -4 deg. |
| Statement line ("BRUTALISM is all about") | Humanist sans; keyword 800 weight; one serif-mono word ("Big") and one very heavy word ("BOLD") | Cap about 20 px | 20 sp. |
| List title ("Too Young") | Humanist geometric sans, Medium | 26 px | **20 sp Medium**. |
| List artist | Same family, accent colour | 17 px | **13 sp Medium, accent text colour**. |
| Duration chip text | Same family, Bold, tabular | 16 px | **12 sp Bold**, tabular figures. |
| Grid caption (under a tile) | Same family, Regular | 15 px | **12 sp** (11.25 raw, raised). |
| Bar title / artist ("Betrayed", "Lil Xan") | Same family | 27 px / 18 px | **20 sp Medium / 14 sp Regular**. |
| Rail tab label | Same family, Bold, uppercase, tracked | glyph height 12 px | **12 sp Bold, tracking +0.04 em**. |
| Date stamp ("01 02 23") | **Condensed** grotesque, accent colour | Digit about 7.5 px wide x 11 px tall (ratio 0.68) | **11 sp SemiBold, condensed**, tracking +0.06 em. |
| Vertical title ("Weekend", "Ed Sheeran") | Humanist sans, white, Medium | ascender height 11 px | **12 sp Medium**. |

### 1.5 Strokes and other numbers

Rim stroke: about 3 px = 2 dp. Sticker rules: 2 px = 2 dp. "Components" ring in image 2: about 7 px = 5 dp (illustration, not a component). Bar corner radius: stadium (radius = height / 2). Tile frame bevel width: 5 dp.

---

## 2. Glass anatomy of one surface

The surface used in the walk-through is the **mini-player bar** (80 dp tall stadium). Layers are listed **bottom to top, in the order they are actually drawn** by the vendored backdrop library:

`ShadowElement` (outside, below) -> `DrawBackdropNode` { `onDrawBehind`, backdrop layer with effects, `onDrawSurface`, children (`drawContent`), `onDrawFront` } -> `HighlightElement` (above) -> `InnerShadowElement` (above). Read from `DrawBackdropModifier.kt` (`draw()`), `ShadowModifier.kt`, `HighlightModifier.kt`, `InnerShadowModifier.kt`.

Important consequences of that order: the node clips to the shape (`layoutLayerBlock` sets `clip = true`), so **anything drawn in `onDrawBehind`, `onDrawSurface` or `onDrawFront` cannot spill outside the surface**; the only way to draw outside is the `shadow` parameter. And `onDrawFront` is drawn over the content (text, art), so it must stay light-handed.

### 2.1 Layer table

Legend: **E** = existing primitive reused as is; **E+** = existing primitive with different arguments (allowed: arguments, not edits); **NEW** = new drawing, written in `:app`, drawn with gradients and strokes inside `onDrawSurface` / `onDrawFront`, no extra `RenderEffect`.

| # | Layer | Clear (image 1) | Neon (image 2, adds the rows marked N) | Provided by |
|---|---|---|---|---|
| 0 | **Depth shadow** (outside) | `Shadow(radius 24 dp, offset (0, 4 dp), black alpha 0.10)` which is `Shadow.Default`. On a `#070708` page it is invisible; it matters when the bar floats over artwork. | **N: under-glow.** `Shadow(radius 20 dp, offset (0, 5 dp), color = accentUi at alpha 0.30 x glowGain)`. Measured tail 0.25 / 0.19 / 0.17 of the accent at 13 / 21 / 28 px matches one soft shadow of radius about 20 dp plus the rim line. | **E+** (`drawBackdrop(shadow = { ... })`, the `Shadow` data class takes `color`). |
| 1 | **Backdrop refraction + blur** | `colorControls(saturation 1.5)`, `blur(8 dp x 0.33)`, `lens(refractionHeight, refractionAmount, depthEffect = true, chromaticAberration = true)`, backdrop recorded at `backdropScale = 0.33`. The mockup's bevel band is about 14 px = 10 dp wide: `refractionHeight = 12 dp`, `refractionAmount = 24 dp` (the app's `liquidGlass()` uses 24 dp / 24 dp). The lens needs API 33; on API 31 and 32 it is skipped (the code already guards that). | Same. | **E+** (`drawBackdrop` with the same effect chain as `liquidGlass()`; `liquidGlass()` itself hard-codes the lens constants and has no hooks for layers 2 to 7, so the new surface calls `drawBackdrop` directly and the plain tier calls `liquidGlass()`). |
| 2 | **Tint (glass body)** | `onDrawSurface`: black scrim alpha 0.55 **only where text sits** (guarantees 4.5:1 for white text on any backdrop, section 7.4), then white vertical gradient from alpha 0.13 at the top to 0.06 at the bottom elsewhere. On the black page this reads as the measured `#1D1D1D`. | Same, plus layer 4N. | **NEW**, `Brush.verticalGradient` in `drawRect`, one draw call. |
| 3 | **Gloss sweep** (diagonal sheen visible at the end of each bar) | A wedge: `Brush.linearGradient` white alpha 0.10 to 0 along a line about 35 deg to the bar's long axis, covering the last 25 percent of the width on the end side, never under the title. | Same, alpha 0.08. | **NEW**, `onDrawSurface`. |
| 4 | **Accent bleed (inner)** | A wash from the bottom-start corner: `Brush.linearGradient(accentUi alpha 0.45 -> 0)`, start at the bottom edge near the artwork, end at about 55 percent of the height, axis rotated about -25 deg so the terminator is diagonal as in the mockup. Covers the lower 45 percent of the surface, start side only. | **N**: peak alpha **0.55**, covers about 80 percent of the width and 70 percent of the height; second, smaller hot spot at the end side. Cap rule: peak alpha is limited so white text on it stays 4.5:1 (section 3.4). | **NEW**, `onDrawSurface`. |
| 5 | **Content** | Artwork (the app's existing artwork image composable; UNCLEAR which one the new bar will reuse, check `MiniPlayer.kt`), title, artist, chevron. | Same. | existing composables |
| 6 | **Inner bevel + specular arcs** | `onDrawFront`: one `drawRoundRect` stroke (width 2 dp) with a `Brush.linearGradient` running from the top-start corner (white alpha 0.55) through transparent at the middle to the bottom-end corner (white alpha 0.55). That gives the **top-left and bottom-right specular arcs** of the mockup in a single stroke. | Same stroke; on top of it layer 7N. | **NEW** (one stroke). A true inner shadow exists (`innerShadow` parameter, `InnerShadow`) but it uses a blurred layer; use it only on FULL tier and only if the stroke is judged too flat on a device. |
| 7 | **Rim (edge highlight)** | `Highlight(width = 1 dp, blurRadius = 0.5 dp, alpha = 0.9)` with `HighlightStyle.Default` (angle 45 deg). In `Shaders.kt` the default shader's intensity is `pow(abs(dot(edgeNormal, (cos 45, sin 45))), falloff)`, so it already lights **both** the top-left and the bottom-right edge (the `abs`). | **N: rim tint.** Add one more `onDrawFront` stroke (1.5 dp) in `rimTint = mix(accentUi, white, 0.25)` (measured `#F9ED3A` bottom rim), alpha 1.0 along the bottom half, fading to 0.35 along the top (`Brush.verticalGradient`). The white Highlight stays on top. | **E+** (`Highlight` arguments) and **NEW** (tint stroke). |
| 8 | **Inner accent bleed on the edge** (N) | - | The bleed (4N) is also brightest where it meets the bottom rim; achieved by letting the gradient of layer 4N reach alpha 0.55 exactly at the bottom edge. | part of layer 4N |

### 2.2 Text diagram (one bar, side view, top = what the user sees)

```
 Clear (image 1)                                 Neon (image 2)
 -------------------------------------------     -------------------------------------------
 7  Highlight.Default rim (TL + BR arcs)         7  white Highlight + accent rim tint stroke
 6  gradient stroke: white TL / BR arcs          6  same gradient stroke
 5  content: art, title, artist, chevron         5  content
 4  accent wash, lower-start, alpha 0.45         4  accent bleed, alpha 0.55, wide, 2 hot spots
 3  gloss wedge, end side, white 0.10            3  gloss wedge, white 0.08
 2  tint: scrim 0.55 under text + white 0.13/0.06  2  same
 1  backdrop: saturate 1.5 > blur > lens         1  same
 0  Shadow.Default (black 0.10)                  0  accent under-glow, radius 20 dp, alpha 0.30
```

### 2.3 Cheap drawing rules for the NEW layers

- All NEW layers are `drawRect` / `drawRoundRect` with a `Brush` (linear or radial gradient) or one `Stroke`. **No `RenderEffect`, no blur, no extra graphics layers.** The only blurred things on a surface are the ones the library already does (the backdrop chain, the `Shadow`, the `Highlight`).
- Brushes are built in `remember(size, accentUi, glowOn)` and read in the draw lambda; nothing allocates per frame. The accent is a **State read at draw time** (as `rememberPanelGlass` does with its tint alpha in `AppGlass.kt`), so an accent change redraws without rebuilding the effect chain.
- The `drawBackdrop` modifier compares its lambdas by identity (comment in `LiquidGlass.kt`). Wrap the whole `Modifier.drawBackdrop(...)` in `remember(backdrop, shape, scaleKeys, tintKeys)` exactly as `liquidGlass()` does, or every recomposition rebuilds the RenderEffect chain.
- Non-live surfaces (list tiles, rail items, section 7) use **only layers 2, 3, 4, 6, 7 (rim as a stroke, no `Highlight` shader needed)**, drawn over the artwork or the page. This is the "frosted frame".

### 2.4 Rim and bevel for the small round and pill surfaces

Rail buttons and tabs in the mockup are glass **bubbles**: a thick white rim, a soft highlight ellipse near the top-start, a dark body. They sit over the solid page (the rail lane is reserved, section 6.2), so nothing is behind them to refract. Draw them as non-live frosted surfaces: layer 2 (white 0.09 body), a radial highlight (`Brush.radialGradient` white 0.35 to 0, centred at 30 percent / 25 percent of the bubble, radius 40 percent), layer 6 stroke, layer 7 `Highlight` rim. In Neon add the rim tint and a short accent glow (stroke-only halo, section 7.3).

---

## 3. Accent system

### 3.1 Presets

Default: Yellow. Hex values other than yellow (measured) and cyan (measured) are **chosen here**, not measured. Contrast uses the WCAG 2.x formula: relative luminance L = 0.2126 R + 0.7152 G + 0.0722 B (each channel linearised: c/12.92 if c <= 0.03928 else ((c + 0.055)/1.055)^2.4), ratio = (L1 + 0.05) / (L2 + 0.05). Computed with a script (PowerShell, kept in the session scratchpad).

Reference backgrounds: page `#070708`; **worst-case glass body `#2C2C2C`** (the brightest glass measured); ink `#0A0A0A`.

| Preset | Hex | Accent on `#070708` | Accent on `#2C2C2C` | Ink on accent | White on accent | Text colour **on** accent | Accent **as text** on glass (>= 4.5 needed) | Accent as UI component on page (>= 3 needed) |
|---|---|---|---|---|---|---|---|---|
| Yellow (default) | `#FAD60A` | 14.08 | 9.77 | 13.85 | 1.43 | ink | passes (9.77) | passes |
| Red | `#FF3B3B` | 5.70 | **3.95** | 5.60 | 3.53 | ink | **fails** -> derived text variant `#FF5F5F` | passes |
| Blue | `#2F5BFF` | 3.89 | **2.70** | 3.83 | 5.17 | **white** | **fails** -> `#6E8CFF` | passes (3.89) |
| Green (acid lime) | `#B8FF1F` | 16.65 | 11.55 | 16.37 | 1.21 | ink | passes | passes |
| Orange | `#FF7A00` | 7.71 | 5.34 | 7.58 | 2.61 | ink | passes | passes |
| Pink | `#FF3EA5` | 6.22 | **4.31** | 6.11 | 3.24 | ink | **fails** -> `#FF51AE` | passes |
| Cyan | `#1FDEDB` | 12.01 | 8.33 | 11.81 | 1.68 | ink | passes | passes |
| White | `#FFFFFF` | 20.14 | 13.97 | 19.80 | 1.00 | ink | passes | passes |

Reading it: "Ink on accent" and "White on accent" decide the text colour used on filled accent shapes (selected rail tab, duration chip, play disc, sticker, tag): the higher of the two, and it is always >= 5.17 for the presets. "Accent as text" is for the accent-coloured text on glass (artist line, date stamp); it is checked against the **worst** glass (`#2C2C2C`), not the page.

### 3.2 Custom colour: the rule

The picker returns an sRGB colour `c`. The app derives **three** values from it, so a bad choice is repaired instead of refused:

1. **`accentUi`** (fills, strokes, rims, glow, icons): if contrast(c, `#070708`) < 3.0, mix toward white in 5 percent steps until it is >= 3.0. Otherwise `accentUi = c`.
2. **`accentText`** (accent-coloured text on glass): if contrast(accentUi, `#2C2C2C`) < 4.5, mix toward white in 5 percent steps until it is >= 4.5. Otherwise `accentText = accentUi`.
3. **`onAccent`** (text on a surface filled with `accentUi`): pick `#0A0A0A` or `#FFFFFF`, whichever has the higher ratio against `accentUi`. If the best is still below 4.5, move the **fill** (not the text) toward white (when ink wins) or toward black (when white wins) in 4 percent steps until >= 4.5, and use that fill for text-carrying shapes (`accentFill`).

Worked examples (computed the same way):

| Chosen | On page | On `#2C2C2C` | `accentUi` | `accentText` | Text on a fill of `accentUi` |
|---|---|---|---|---|---|
| `#2040C0` (deep blue) | 2.45 (fails 3.0) | 1.70 | `#3653C6` (2 steps, 3.07) | `#8092DB` (11 steps, 4.71) | white 6.57 (ink 3.01) |
| `#7A1FA2` (purple) | 2.44 | 1.69 | `#8D3FAF` (3 steps, 3.32) | `#B380CA` (11 steps, 4.55) | white 6.06 |
| `#1B5E20` (dark green) | 2.56 | 1.77 | `#316E36` (2 steps, 3.26) | `#769F79` (10 steps, 4.65) | white 6.17 |
| `#8B0000` (dark red) | 2.01 | 1.40 | `#A53A3A` (5 steps, 3.13) | `#C68383` (14 steps, 4.63) | white 6.43 |
| `#FF00FF` (magenta) | 6.42 | 4.45 (fails 4.5) | unchanged | `#FF19FF` (2 steps, 4.54) | ink 6.31 (white 3.14) |
| `#808080` (grey) | 5.10 | 3.54 | unchanged | `#989898` (4 steps, 4.81) | ink 5.01 (white 3.95) |

The preview in the picker shows `accentUi`, `accentText` and the text-on-accent pairing live, so the user sees what the app will actually use. The user's raw choice is stored; the derivation runs at read time (so the stored value survives a later change of the background token).

### 3.3 Where each derived colour is used

- `accentUi`: rail selected-tab fill, play disc, duration chip fill, sticker fill, tag fill, all rims and bleeds, round-button icons, sliders, focus rings.
- `accentText`: artist lines, date stamps, rail tab labels (unselected), links.
- `onAccent`: text and icons on any `accentUi` fill.
- Theme: `MaterialTheme.colorScheme.primary = accentUi`, `onPrimary = onAccent` so existing pages pick the accent up through the theme without editing `:sharedUi` pages.

### 3.4 Text on a bleed (the bleed must not break contrast)

White text on the yellow bleed, base `#1D1D1D`:

| Bleed alpha | Blended colour | White text | Ink text | Accent text |
|---|---|---|---|---|
| 0.25 | `#544B18` | 8.73 | 2.27 | 6.10 |
| 0.35 | `#6A5E16` | 6.52 | 3.04 | 4.56 |
| 0.45 | `#807014` | 4.92 | 4.02 | 3.44 |
| 0.55 | `#978313` | **3.78** | 5.23 | 2.65 |

Rule: **the bleed's alpha under white text is capped so the blended colour keeps white text >= 4.5:1** (yellow: 0.45; the same check for any accent: compute the blend, step the alpha down). **Under accent-coloured text the cap is 0.35.** Where the bleed is stronger than the cap (the hot spots), no text is placed: the layout keeps the title and artist in the start-middle area and the hot spots at the bottom edge and the end corner, which is also where the mockup puts them. For White as the accent, the cap is 0.35 (white text on white at alpha 0.55 over `#1D1D1D` is only 2.84:1). Alternative when a hot spot must carry text: switch that text to ink (yellow at 0.55: ink 5.23).

### 3.5 How the Glow (Neon) tints follow the accent, exactly

Let `A = accentUi`, `L` = its relative luminance.

| Neon part | Colour | Alpha |
|---|---|---|
| Inner bleed | `A` | peak 0.55, capped by 3.4 |
| Rim tint | `mix(A, white, 0.25)` | 1.0 bottom half, 0.35 top half |
| Under-glow (shadow) | `A` | 0.30 x `glowGain(L)` |
| Round and pill buttons: icon | `accentText` | 1 |
| Strokes (stamp-style, ring) | `A` | 1, with a stroke halo of 0.2 |

`glowGain` compensates for dark accents glowing less: **1.0** if L >= 0.5 (yellow 0.68, green 0.82, cyan 0.58, white 1.0), **1.15** if 0.2 <= L < 0.5 (orange 0.35, pink 0.27, red 0.25), **1.35** if L < 0.2 (blue 0.15, and every derived dark custom colour). Values of L are from the table in 3.1 (L = ratio x (L_bg + 0.05) - 0.05 with L_bg = 0.0021).

**Clear** keeps the white rims and uses `A` only for the small wash (layer 4, alpha 0.45) and flat fills. **Neon** adds everything in the table. Both variants share the same layout and geometry, so switching Glow does not move anything (and creates no new composition nodes beyond the extra draw lambdas).

The Glow switch is independent of the glass tier: if the tier is SIMPLIFIED or SOLID, Neon still draws its **gradient** layers (cheap), and replaces only the shadow under-glow by a stroke halo (section 7.3).

---

## 4. Type

### 4.1 Choices (all SIL Open Font License 1.1; sizes of the real files from the `google/fonts` repository, read via its public API on 2026-11-02)

| Role | Choice | Files bundled | Size | Axes / subsets (from `METADATA.pb`) | Licence |
|---|---|---|---|---|---|
| **Display, heavy** | **Archivo Black** | `ArchivoBlack-Regular.ttf` | **90,988 B** | static; latin, latin-ext | OFL 1.1, (c) 2017 The Archivo Black Project Authors |
| **Condensed stamps** (dates, vertical meta) | **Archivo Narrow** (variable) | `ArchivoNarrow[wght].ttf` | **93,304 B** | wght 400 to 700; latin, latin-ext, vietnamese | OFL 1.1, (c) 2019 The Archivo Narrow Project Authors |
| **Body and UI** | **Figtree** (variable) | `Figtree[wght].ttf` | **62,712 B** (+ `Figtree-Italic[wght].ttf` 62,632 B, optional) | wght 300 to 900; latin, latin-ext | OFL 1.1, (c) 2022 The Figtree Project Authors |
| **Mono** (tabular times, version, rare accent word) | **IBM Plex Mono** SemiBold | `IBMPlexMono-SemiBold.ttf` | **140,216 B** | static; cyrillic, latin, latin-ext, vietnamese | OFL 1.1, (c) 2017 IBM Corp. |

Total bundled: 90,988 + 93,304 + 62,712 + 140,216 = **387,220 B (about 378 KiB)**; with the optional Figtree Italic 449,852 B. For comparison the five SF Pro files in `app/src/main/res/font` are 2.2 to 2.3 MB each (about 11.5 MB). Nothing is loaded over the network; the fonts go into `res/font` and the licence texts into `NOTICE`. `minSdk` is 26 (`app/build.gradle.kts`), so variable-font axes (`FontVariation`) work on every supported device.

Not chosen, with the reason:

| Candidate | File size | Why not |
|---|---|---|
| Archivo (full variable, wdth + wght) | 658,596 B (+ italic 741,368 B) | 7x the size of Archivo Black for the same heavy look; the wdth axis is not needed. Revisit only if a heavy **italic** is wanted. |
| Anton | 170,812 B | Very condensed, all caps feel; the sticker's letters are a normal-width grotesque. |
| Bebas Neue | 61,400 B | Caps only; the sticker needs lowercase for the statement line. |
| Big Shoulders Display (variable) | 219,532 B | Squarer and narrower than the sticker; its character is "Chicago industrial", not the neutral heavy grotesque. |
| Inter (variable) | 876,576 B | Larger than the other three fonts combined; the mockup's UI text is rounder than Inter. Its coverage (Cyrillic, Greek, Vietnamese) is better, see 4.3. |
| Open Sans (variable) | 532,636 B | Best coverage (Cyrillic, Greek, Hebrew, Vietnamese) but wider and more neutral than the mockup. Revisit as the per-locale body font if the owner wants one font for all Latin, Cyrillic and Hebrew locales (open question 6). |
| Space Mono | 98,232 B (Bold) | Closer to a "quirky" mono, but no Cyrillic. IBM Plex Mono is nearer the mockup's slab-serif mono word and covers Cyrillic. |
| JetBrains Mono (variable) | 187,208 B | A coding face; too technical. |

### 4.2 Justification against the mockup

- **Sticker display = Archivo Black (and Figtree 800/900 for the lighter second row).** The mockup's ABOUT / PRIVACY is a heavy, neutral, upright grotesque with flat-cut terminals. Archivo Black is close but **wider and heavier** than the mockup's face (my best identification of the mockup's face is a Roboto/Inter-class ExtraBold). APPROXIMATE. If the sticker looks too wide on a device, set Figtree 900 at the same size; it is closer in width.
- **Body and UI = Figtree.** "Too Young", "Betrayed", the chips and the statement line have round bowls, a single-storey `g`, and open apertures: Figtree matches closely. It is also tiny (62 KB) and variable, so Medium (list titles), Bold (chips, tabs) and ExtraBold (keywords such as BRUTALISM) cost nothing extra.
- **Condensed stamps = Archivo Narrow.** The date digits are 0.68 as wide as they are tall; Figtree's digits are about 0.85, which would make "01 02 23" visibly longer and wider in the vertical lane. Archivo Narrow keeps the lane at 28 dp.
- **Mono = IBM Plex Mono SemiBold.** The mockup uses a serif-mono once ("Big") and the version number. Plex Mono has slab serifs on several letters, the nearest of the three listed mono faces (none of them is a true match for the mockup's Courier-like word; APPROXIMATE). Used for: tabular durations where alignment matters (queue times), the version label, the odd accent word. **Not** for general UI.
- **Wordmark and tag** (VIOLENCE, EXPERIMENTAL) use Figtree SemiBold / Bold with the measured tracking; no separate face.

`brutal-ui/type-board.html` documents the choices. It makes **no external requests** and loads no font files: it names the real fonts first in each `font-family` stack and falls back to common system faces (Arial Black, Impact, Segoe UI, Courier New). If the reader has the real fonts installed locally they show; otherwise the stand-ins show, and the board says so on the page.

### 4.3 Locales: coverage and how the heavy display style degrades

The app ships 17 locales (`es fr de pl pt id hi ja ru zh zh-rTW he it tr th vi ko` plus English; the `values-*` folders in `app/src/main/res`). Coverage of the chosen files, from the `METADATA.pb` subsets:

| Script / locales | Archivo Black | Archivo Narrow | Figtree | Plex Mono | Outcome |
|---|---|---|---|---|---|
| Latin + Latin-ext (en es fr de it pt pl tr id) | yes | yes | yes | yes | Full design. |
| Vietnamese (vi) | **no** | yes | **no** | yes | Base letters from the bundled face, stacked tone marks from the system fallback: **looks uneven**. Use the system family for body and display in `vi` (rule below). |
| Cyrillic (ru) | **no** | **no** | **no** | yes | Whole line falls back to the system font (Roboto/Noto). Use the system family explicitly (below). |
| Hebrew (he) | no | no | no | no | System font. RTL: section 5.5. |
| Devanagari (hi), Thai (th) | no | no | no | no | System font. **Letter-spacing must be 0** for these scripts (tracking breaks conjunct and mark placement); no uppercase transform; no rotated text longer than the lane (section 5). |
| CJK (ja zh zh-rTW ko) | no | no | no | no | System font (Noto Sans CJK). No uppercase; letter-spacing 0; weight 700 or 900 maps to the system bold. |

Rules for degradation (decided here so that nothing looks half-broken):

1. A `ScriptProfile` per locale: `LATIN` (design as specified), `SYSTEM_SANS` (ru, vi, he, hi, th, ja, ko, zh), chosen from the app language.
2. In `SYSTEM_SANS` the display and body roles both use `FontFamily.Default` with the same weights (700 / 900 for display, 500 for titles). **Heavy italic is not requested** (no synthetic skew: a skewed Cyrillic or CJK glyph looks wrong). Uppercase transforms are applied only in `LATIN`.
3. The sticker, tag and wordmark keep their **geometry** (rotation, rules, boxes) in every locale; only the glyphs change. The wordmark "VIOLENCE" is a brand name, not translated: it stays Latin in every locale and therefore stays Archivo/Figtree everywhere.
4. Letter-spacing is forced to 0 outside `LATIN`, **except** the wordmark.
5. If a translated sticker word does not fit the sticker row at 48 sp, the row shrinks the text (`autoSize`-style: reduce in 2 sp steps down to 28 sp) before it clips; below 28 sp it wraps to a second line and the sticker grows (it has no fixed height).

---

## 5. Component inventory

Terms: **target** = touch area; **hit slop** = the extra invisible area added so a smaller visual meets 48 dp (`Modifier.minimumInteractiveComponentSize()` or an equivalent padded `clickable`). All components use `Modifier.semantics` through standard Compose; none of these building blocks need an edit in `:sharedUi`.

### 5.1 The list

| # | Component | Building blocks (Compose, `:app`) | Accessibility semantics |
|---|---|---|---|
| 1 | **Sticker label** (3-row yellow sticker: ABOUT / PRIVACY / VERSION 0.4) | `Column` of three `Box` rows with 2 dp black `drawBehind` rules; `Modifier.graphicsLayer { rotationZ = -12f; transformOrigin = TopStart }` on the whole sticker; texts in `TextStyle` display/label. Bleeds past the top-start edge by letting the box be larger than the window inset (`offset`), clipped by the screen. | One node: `semantics(mergeDescendants = true) { heading(); contentDescription = "About privacy, version 0.4" }` (the three rows read as one phrase, not three). Not focusable as a control. Rotation does not affect TalkBack. **At 200 percent**: display text is capped (below), row heights are `wrap`, the sticker grows downward; rotation pivot stays at the top-start so growth does not push it off screen. |
| 2 | **Wordmark + tag** (VIOLENCE glass tile, EXPERIMENTAL tag) | Glass masthead (section 2) rotated -8.5 deg; wordmark `Text` with `letterSpacing = 1.3.em`; tag `Box` accent fill + `Text`, rotated -4 deg inside. | One node `contentDescription = "Violence, experimental"`, `heading()`. The **trailing letter-space** must not count: set the text's `textAlign = Center` and add `padding(start = letterSpacing)` so the word is optically centred. Not a button (see 6.4). |
| 3 | **Vertical label** (title, stamp, rail tab text) | `Text` + a layout modifier that **swaps width and height** and places the rotated placeable (`graphicsLayer { rotationZ = -90f }` with a custom `layout` so the parent measures the rotated size; this is the standard Compose pattern, UNCLEAR whether the repo already has a helper: grep before writing one). | Parent carries the label: `semantics { contentDescription = "<real text, horizontal reading order>" }`; the rotated `Text` itself `clearAndSetSemantics {}` so TalkBack reads it once. **No clipping at 200 percent**: the lane's cross-size is `lineHeight` of the scaled text, so it grows; the length is capped by the tile height with `maxLines = 1`, `overflow = Ellipsis` (the full text is still in the parent's description). Letter-spacing 0 outside LATIN. |
| 4 | **Date stamp** (yellow "28 11 22") | Vertical label in Archivo Narrow SemiBold 11 sp, `accentText`. | Description read as a date in the locale: `DateFormat`, e.g. "28 November 2022", not "28 11 22". Merged into the tile's description (see 5). |
| 5 | **Glass tile + play button** | `Box` with the tile shape (`ContinuousCornerShape`, ratio 0.19); artwork `AsyncImage` inset 5 dp; frame drawn as a non-live frosted frame (section 7); play disc `Box` 28 dp visual with a 48 dp hit slop, bottom-end, 8 dp inset. | Tile = `Role.Button`, `onClickLabel = "Open <title>"`, merged description "<title>, <artist or type>, <date>". The play disc is a **separate** node: `Role.Button`, `contentDescription = "Play <title>"`, target 48 dp (slop extends over the artwork, never outside the tile). TalkBack order: tile, then its play button. Focus indicator: 2 dp accent ring outside the frame. |
| 6 | **Masonry column** | `LazyVerticalStaggeredGrid` (the platform component; UNCLEAR whether the repo's Compose version has it stable: check the BOM when implementing, fallback is two `LazyColumn`s with shared scroll), tile height / width clamped to 1.0 to 1.8 (the mockup's range: 134/135 for the Chilling tile up to 240/135 for the Ed Sheeran tile), gap 12 dp, label lane 28 dp on the start side of each column. | `collectionInfo` (rows/columns) so TalkBack announces position; reading order = top to bottom, column by column. |
| 7 | **List row + duration chip** | `Row`: text column (title 20 sp Medium, artist 13 sp `accentText`, chip) and 68 dp artwork (ratio 0.20) at the end; the row is one `clickable`. Chip: `Box` stadium, `accentUi` fill, `onAccent` text 12 sp Bold tabular, `minHeight 20 dp`. | Row `Role.Button`, merged: "<title>, <artist>, <duration spoken: 3 minutes 45 seconds>". The chip is **not** a separate stop (informational; merged). Target = the whole row, at least 56 dp. |
| 8 | **Genre rail tab** (also the nav tab, section 6) | Vertical label inside a stadium; selected = `accentUi` fill + `onAccent` text; unselected = non-live glass bubble + `accentText`; `Modifier.selectable(role = Role.Tab)` inside `Modifier.selectableGroup()`. | `Role.Tab`, `selected` state announced, `contentDescription = "<name>, tab <n> of <N>"` through `collectionItemInfo`. Width 48 dp = target; height >= 48 dp always (min). At 200 percent the label length doubles, the pill grows; when the rail would not fit, it becomes the dock (6.8). |
| 9 | **Floating round glass button** (collapse, search; also the avatar and back) | 48 dp circle (search 48 x 72 stadium) non-live bubble (2.4), icon 24 dp in `accentText`. | `Role.Button`, label from `strings.xml` ("Collapse navigation", "Expand navigation", "Search"), `stateDescription` for collapse ("expanded"/"collapsed"). 48 dp target met by size; no slop needed. |
| 10 | **Player bar (mini player, state PLAYING)** | Glass bar 80 dp (live surface #1), `PLAYING` / `PAUSE` vertical label chip at the start (stadium, ink fill, accent text, rotated -90), artwork 56 dp (radius 0.19), title/artist, chevron. Keeps the current gestures: pull up to expand, long-press shares, swipe skips (`MiniPlayer.kt`); every gesture also has a visible control. | Bar = `Role.Button`, `onClickLabel = "Open player"`; description "Playing: <title> by <artist>"; `stateDescription = playing/paused`; `liveRegion = Polite` **off** (no per-track chatter; announce only on user action). `customActions`: Play/Pause, Next, Previous, Share (for touch-exploration users who cannot swipe or long-press). The vertical state chip is `clearAndSetSemantics` (its content is in the bar's state). |
| 11 | **Transport bar** (dock, shown only while the player is expanded) | Glass bar 96 dp (live surface #1, replacing the mini player), `PAUSE` chip, art, title, chevron (collapse), previous / pause (68 dp circle, accent fill in Neon) / next. | Previous, Play/Pause, Next: `Role.Button` with labels, targets 48 dp or larger (pause 68 dp). Pause toggles `contentDescription` between "Pause" and "Play". Order: bar info, previous, play/pause, next. |

### 5.2 Display text must not explode at 200 percent

`sp` text scales with the system font scale, so the 48 sp sticker word would become 96 sp. Display roles (sticker row 1 and 2, wordmark, masthead) are drawn under a `Density` copy with `fontScale = min(fontScale, 1.3f)` (the standard `LocalDensity` override); this is a documented large-text exception (display text is already large, WCAG large text is 18.66 px bold / 24 px regular). **Every other role scales fully to 200 percent**; none of them has a fixed height. The tile's vertical labels grow in lane width and truncate in length (row 3 of the table).

### 5.3 Touch targets, summarised

| Visual | Visual size | Target |
|---|---|---|
| Round buttons, rail tabs | 48 dp | 48 dp (no slop needed) |
| Play disc | 28 dp | 48 dp (slop) |
| Prev / next glyph | 24 dp | 48 dp |
| Chevron on bars | 24 dp | 48 dp |
| Duration chip | 20 dp | not a control |
| Tile, row, bar | large | whole surface |

### 5.4 TalkBack order of a typical page

Top to bottom, start to end: header (sticker or wordmark as a heading) -> avatar / Hub button -> page content (tiles in reading order) -> rail (collapse, search, tabs) -> player bar. Implemented with `isTraversalGroup = true` on the content, the rail and the player, and `traversalIndex` 0, 1, 2 (header 0, content 1, rail 2, player 3). The rail comes after the content because it is a navigation control, not the page; the player is last because it is a persistent control (the earlier audit, `UI_AUDIT.md` section 6, found no traversal work anywhere in the app: this is new).

### 5.5 Right-to-left (Hebrew is shipped)

- The rail's default edge is the **end** edge (right in LTR, left in RTL) with a setting to force left or right (6.5).
- Rotation is **-90 deg in both directions** (Latin reads bottom to top; a Hebrew line, which starts at its right end, then starts at the top of the label and reads downward). **UNCLEAR whether that looks right to Hebrew readers; check on a device with the `he` locale** and be ready to flip to +90 deg for RTL.
- The sticker and the masthead rotate counter-clockwise in both directions, and bleed off the **start** edge in LTR and the **end** edge in RTL (use start/end, not left/right).
- Chevrons and the pull direction: not mirrored (vertical).

---

## 6. Navigation model (owner chose HYBRID)

### 6.1 Parts

```
   +------------------------------------------------+
   | status bar inset                               |
   | [sticker / title]        [wordmark tile] [avatar]|   header zone, about 96 dp, page-owned
   |                                                |
   |   page content (solid page, tiles, rows)   [v] |   v = collapse (round)
   |                                            [Q] |   Q = search (round)
   |                                            |J| |   |J| = rail tabs, rotated
   |                                            |E| |
   |                                            |L| |   (Home, Explore, Library, Search,
   |                                            |S| |    names written out, rotated -90)
   | [PLAYING  art  Title / Artist          ^ ]      |   mini player glass bar (always, if a song is loaded)
   +------------------------------------------------+
   Player expanded:  the mini player is replaced by the dock:
   [PAUSE art Title  v]  [ |<   (||)   >| ]
```

- **Rail**: a column at the end edge, below the header, above the player zone. Top to bottom: collapse button, search button, four vertical tabs **Home, Explore, Library, Search** (the four existing tab indexes `TAB_HOME=0, TAB_EXPLORE=1, TAB_LIBRARY=2, TAB_SEARCH=3`, `MainActivity.kt`). The rail calls the same tab-selection code the current bottom bar calls; no state changes hands.
- **Mini player** = the mockup's PLAYING bar. It exists whenever a song is loaded, exactly as today.
- **Dock** = the mockup's PAUSE bar plus transport. It appears **only while the full player is expanded** and **replaces** the mini player in place (never both on screen), so the live-glass count stays low.
- Tap on the search round button: opens the Search tab **and focuses the field**. Tap on the Search tab: opens the Search tab without forcing the keyboard. (This duplicates one destination on purpose: the round button is the quick action, the tab is the page. Open question 2 asks whether to keep both.)

### 6.2 The rail lane

The rail is not drawn over scrolling content: a **lane of 56 dp** (48 dp rail + 8 dp edge inset) is reserved at the end edge and the page's content end padding is raised by the lane width while the rail is expanded. That is also why the rail buttons can be non-live glass (nothing to sample behind them), and why they cost almost nothing. When folded (6.3) the lane is released and only the single round button (top of the lane) overlaps content.

The player zone sits **below** the rail's last tab, full width, so the bar keeps its full width. (The mockup lets the rail continue down beside the bars and the bars stop before the lane. That costs 56 dp of bar width on a 360 dp phone and truncates titles; I did not follow it. Deviation, listed in section 8.)

### 6.3 Collapse behaviour

- The chevron folds the rail into **the single round button** (the same chevron, rotated to point toward the edge, `stateDescription = collapsed`), kept at its top position. The tabs, the search button and the lane disappear (animated 200 ms, instant under reduce-motion). Tapping the button expands.
- The folded/expanded state is **remembered** (`AppSettings`, one boolean) and survives restarts. It is **forced folded** on deep pages (6.6) without overwriting the remembered value.
- The folded button is a **non-live** bubble; content scrolls under it. It has a small scrim ring (black 0.45, 2 dp wider than the bubble) so the icon keeps 3:1 against any artwork behind it.

### 6.4 Where the things the old top bar held go

The current top bar (`FrostedTopBar`) holds Back, an optional search field, Receive (NFC), Downloads, an update icon, and the account avatar. In the new chrome:

- **Back**: 48 dp round glass button at the start edge of the header, on pushed pages only (as today).
- **Avatar** (48 dp round, end edge of the header): opens the **Hub**, a bottom sheet built with the existing `GlassBottomSheet` (`GlassPanels.kt`) with four headed groups. A small accent dot on the avatar shows an update is available (replaces the update icon).
- The **wordmark masthead** is a brand element on Home, **not** a button.
- Search field: lives on the Search tab page (a plain solid field), not in the header.

Hub contents (every route that is not a tab; same list the earlier UI-2 Hub used, `UI_PLAN.md` section 9): People (Profile, Friends), Share (Receive via NFC/QR/link, Recent shares), Music (Downloads, History, Replay, Equalizer, Spotify import, Sources), You (Accounts and scrobbling, Jam, Settings). Settings stays the one place that opens the existing Settings pages unchanged.

### 6.5 Rail position for left-handed users (a setting)

New setting `railSide`: **Auto (default) / Left / Right**. Auto = end edge (right in LTR, left in RTL). Right and Left are **physical**. Stored in `AppSettings` (SharedPreferences `bitchord_settings`, a `MutableStateFlow` plus setter, appended at the end of the block). Mirrors: the lane, the bleed direction of the masthead, the order of the buttons in the header (avatar goes to the side opposite the rail, so the thumb that holds the rail does not cross the avatar).

### 6.6 Thumb reachability

Reachability bands on a 360 x 800 dp phone, one-handed (APPROXIMATE, standard ergonomic bands):

| Zone | Share of height | Used for |
|---|---|---|
| Easy (bottom 35 percent) | y 520 to 800 dp | player bar / dock, the lowest rail tabs |
| Reachable (middle 35 percent) | y 240 to 520 dp | rail tabs, search button |
| Stretch (top 30 percent) | y 0 to 240 dp | header, avatar, collapse button (rarely used) |

The rail is anchored to the **bottom** of its area, so the four tabs sit in the easy and reachable bands; the collapse and search buttons sit above them, in the reachable band. The mockup also stacks the most-used item (selected tab) at the bottom. The header and avatar are in the stretch band, which is acceptable because the Hub is not a frequent action, and the player bar is the most-used control and is at the bottom.

Estimated rail height at 100 percent font: collapse 48 + 40 gap + search 72 + 28 gap + 4 tabs x (label about 50 dp + 32 dp padding = 82 dp) + 3 x 4 dp gaps = 48 + 40 + 72 + 28 + 328 + 12 = **528 dp**. A 360 x 800 phone has 800 - 96 (header) - 96 (player zone) - 48 (insets) = **560 dp**. It fits with 32 dp to spare at 100 percent; it does **not** at larger fonts or short screens (6.8).

### 6.7 Back behaviour

Unchanged from today, because the rail only calls existing code. The order of what Back does is:

1. If the full player is expanded: collapse it (existing handler).
2. If a sheet or dialog is open (Hub, share, menus): dismiss it.
3. `AppNavigator.pop()` (`ui/navigation/AppNavigator.kt`): a release page (album, artist, playlist) or a route on top is closed.
4. At the bare tab: the existing behaviour (UNCLEAR what it is on every tab; the spec does not change it; the rail does not add a "back to Home" step).

The rail tabs behave like the current bottom tabs: a tap on another tab clears the stack and selects it; a tap on the **current** tab scrolls to top and refreshes (as today).

### 6.8 Deep screens (album, artist, playlist, Settings, any route)

- On any page where `AppNavigator.visibleRoute() != null` or a release page is open, the rail **auto-folds** to the round button and the lane is released, so album art and Settings lists get the full width. The Back button appears in the header (6.4).
- Tapping a rail tab (after expanding the button) behaves like a current tab tap: `AppNavigator.clear()` then select the tab.
- Settings and its sub-pages keep the current pages and layout (solid). The new theme and fonts apply through `MaterialTheme`; the Settings page itself is not redesigned in this spec.
- Album, artist and playlist pages (`DetailScreen.kt`, 2,428 lines, in `:sharedUi`) are **not edited**: they pick up colour, fonts and shapes through the theme and `CompositionLocal`s; a heading/hero built with the sticker is a later, optional wrapper in `:app`.

### 6.9 Landscape and tablet

- **Landscape phone** (height < 480 dp): the rail does not fit (6.6). The rail becomes a **vertical scrolling column** (`verticalScroll`) of the same items with tabs at minimum height (48 dp + label), the collapse button stays pinned at its top, and the player bar becomes the dock-style bar docked at the bottom with the lane at the end edge. If the content still does not fit, the rail folds by default in landscape.
- **Large font or short screen: rail -> dock fallback.** When `fontScale >= 1.5` **or** the required rail height exceeds the available height, the four tabs are shown in a **horizontal bottom dock** (glass, 56 dp, four 48 dp-high targets with horizontal labels, same destinations, same selected style); the collapse and search buttons stay as round buttons at the end edge. This is the answer to rotated text at 200 percent: nothing is clipped because nothing is rotated.
- **Tablet / wide window** (width >= 600 dp, from `BoxWithConstraints`, no new dependency; the app has no `WindowSizeClass`, `UI_AUDIT.md` section 6): rail on the end edge as on a phone; content max width 840 dp, centred; masonry columns = `floor(width / (tile 104 dp + 12 dp))`; the player bar is capped at 560 dp and centred. A two-pane detail layout is **not specified** (not in the mockup; UNCLEAR whether wanted).

### 6.10 Every existing route stays reachable

From `AppRoute` in `ui/navigation/AppNavigator.kt`:

| Route | How it is reached in the new chrome |
|---|---|
| `Settings`, `SettingsSection(category)`, `SettingsChoice(kind)` | Hub -> Settings (the existing Settings pages; the new rows go at the top of `SettingsCategory.ADVANCED`, "Advanced & experimental options") |
| `AccountScrobbling`, `Sources`, `Discord`, `Spotify`, `WearOs` | Hub (Accounts and scrobbling, Sources, Spotify import) and the existing Settings rows (Discord, Watch). `WearOs` stays as today (dormant target; not touched) |
| `ListenTogether` (Jam) | Hub -> Jam; the player's Jam pill; invite links (unchanged) |
| `Profile`, `Friends`, `Receive`, `Recent` | Hub (People / Share groups); the NFC receive entry that sat in the top bar is now in the Hub |
| `Downloads`, `History`, `Replay` | Hub (Music group) |
| `Equalizer`, `EqualizerPresets` | Hub (Music), Settings, and the player menu (unchanged) |
| `LibraryShowAll(shelf)`, `ArtistShelf`, `Moods` | "Show all" on shelves (page content is unchanged) |
| Album, artist, playlist (not routes; `MainViewModel.detailStack`) | Tiles, rows, search results, shared links (unchanged) |
| The four tabs | The rail (or the dock fallback) |
| Incoming links (`IncomingLinkHub`) and dialogs | Unchanged (they live in `BitChordApp`, which is the same composition in both UIs) |

**Verification to require in the implementation phase**: an automated test that enumerates every `AppRoute` subtype and asserts a visible entry point exists in the new chrome (the earlier attempt had such a test for its Hub).

### 6.11 Switch mechanics (carried over from the lessons of the last attempt)

`BitChordApp` (`app/.../MainActivity.kt`) is about 6,500 lines and sits at the JVM method-size limit; the media controller is created inside it. So: the variant switch changes **theme and chrome only**; the composition shape is **identical** in both variants (same composables in the same order; only the values passed differ), so a switch recreates nothing, playback is untouched, and the Hub, dialogs and incoming links keep their state. Every edit in `MainActivity.kt` is **one small call** into new code with plain parameters. The new UI is **off by default** and "the current UI behaves exactly as before when the new UI is OFF". Settings go at the top of `SettingsCategory.ADVANCED` (no Experimental tab exists). New strings go into **all 17 locale files** (English placeholders first, then translated; `LocaleStringsTest` enforces it).

---

## 7. Performance and accessibility budget

### 7.1 Live-surface budget

- **At most 3 live backdrop surfaces on screen at once.** The design needs **at most 2** in the normal case:
  1. the player bar **or** the dock (never both),
  2. one sheet or masthead while it is visible (the Hub sheet uses the existing still-based `GlassBottomSheet`; the masthead sits over a solid area and is non-live).
  Everything else is non-live: rail buttons and tabs, folded button, round header buttons, tiles, chips.
- A `GlassBudget` counter (a counter of acquired live surfaces; the earlier attempt had one) hands out up to 3 live slots; a fourth surface draws at the SIMPLIFIED tier.
- **No glass inside scrolling lists.** Tiles and rows use the **frosted frame** (layers 2, 3, 6, 7 as gradients and strokes over the artwork; no `drawBackdrop`). A surface drawn inside the page that the backdrop records cannot sample it (it would loop); the existing code already says so (`lightweightLiquidGlass`, `liquidGlassRim` in `LiquidGlass.kt`).
- Windowed surfaces (dialogs, bottom sheets) keep using the page **still** (`AppStill`, `GlassSurface`, `GlassBottomSheet`).

### 7.2 Glass tiers and what picks them

| Tier | What is drawn | Chosen when |
|---|---|---|
| **FULL** | Live backdrop (`drawBackdrop`: saturate, blur, lens), all NEW layers, Highlight, Shadow (under-glow in Neon). | API >= 33, not low-RAM, not battery saver, no reduce-motion, no reduce-transparency, "Reduce dynamic blur" off. |
| **SIMPLIFIED** | No backdrop capture. Tint + gloss + bleed + stroke rim (the `lightweightLiquidGlass` approach: tint, highlight, hairline) and a **stroke-based halo** in place of the Shadow. | Android 12 (API 31 and 32: the backdrop pipeline works but the lens shader is not available) **or** low-RAM device (`ActivityManager.isLowRamDevice`) **or** battery saver (`PowerManager.isPowerSaveMode`) **or** reduce-motion (the app's `reduceAnimation` or system animator scale 0). |
| **SOLID** | Opaque fill `#1D1D1D` (white 0.09 over the page), 0.5 dp hairline (`GLASS_EDGE_COLOR`), Neon's bleed as a static gradient and the accent hairline as the rim. | API < 31 (no `RenderEffect`; `isGlassSupported()` is false), **or** "Reduce transparency" on, **or** the existing "Reduce dynamic blur" on. |

Rules in order: SOLID if any SOLID condition holds, else SIMPLIFIED if any SIMPLIFIED condition holds, else FULL. The decision is a **pure function** of its inputs (so it can be unit tested). It is re-evaluated when battery saver or the settings change. The existing `LocalLiquidGlassEnabled` is provided as `tier == FULL` so the existing primitives (`liquidGlassRim`, the page-backdrop recording in `MainActivity`) stop recording below FULL.

**Android 12 / 13 limits**: API 31 and 32: `RenderEffect` blur works, the AGSL lens shader does not (the code already guards `SDK_INT >= TIRAMISU`) -> SIMPLIFIED. API 33 and up: lens available. **Android 11 and below** (minSdk 26 to 30): SOLID. Android has **no** system "Reduce transparency"; it is the app's own switch (default off; proposed default **on** for a low-RAM device. `UI_DECISIONS.md` section 6 item 4 asks the owner exactly this and is still unticked in the repo, so it is open question 11 below).

### 7.3 Neon cost control

The under-glow is a `Shadow` (a blurred layer): **at most 2 on screen at once** (player bar and one more). Everything else in Neon is gradients and strokes. In SIMPLIFIED and for every non-live Neon item the glow is a **stroke halo**: two strokes outside-in, 6 dp at alpha 0.12 and 3 dp at alpha 0.20 in `accentUi`, drawn in `onDrawFront` of the **parent** (so the glow is not clipped by the item's own shape; the rail lane's 8 dp inset gives the space).

### 7.4 Contrast: 4.5:1 for text over glass, with a scrim fallback

- Text colour on glass is **white** (`#FFFFFF`) in the dark-only first version (section 8, question 3). Required ratio **4.5:1**, UI components and large text **3:1**.
- **Guarantee by construction**: a black scrim of alpha **0.55** in front of the backdrop and behind the text gives white text >= 4.5:1 over **any** backdrop including pure white (computed: white backdrop -> `#737373` -> white text 4.7:1). Applied (layer 2) on every text-bearing glass surface (player bar, dock, sheets). The earlier audit measured white text over bright artwork at about 2.9:1 with the old 0.4 tint; this is what fixes it.
- Where the surface is over a known-dark area (the rail lane, the masthead), the scrim is dropped and the lighter `#1D1D1D` body is used (matches the mockup).
- **SOLID** tier: fill `#1D1D1D`, white text 16.86:1 (computed).
- Bleed caps from 3.4 keep white text >= 4.5:1 on the bleed.
- The mockup's own body (`#1D1D1D`) is darker than the scrim over a black page; the scrim over the black page is nearly black. Deviation, listed in section 8.

### 7.5 Targets, font scale, motion

- **48 dp targets**: every control in 5.3 meets it by size or slop. The current top controls are 44 dp (`TOP_CONTROL_SIZE`, `UI_AUDIT.md` section 6); the new buttons are 48.
- **200 percent font scale**: no fixed-height text container; display roles capped at 1.3x (5.2); rail -> dock fallback at >= 1.5x (6.9); player bar title one line with ellipsis, full text in semantics; Hub rows grow; sticker grows downward. Screenshots at 100 and 200 percent are part of the acceptance in every UI phase.
- **Reduce motion**: when the system animator scale is 0 or the app's Reduce animation is on: sticker tilt is static (no entry settle), the rail fold is instant, no glow pulse, the player open/close keeps the existing reduced path (`reducePlayerMotion`).
- **No continuous animation while idle.** Nothing loops: no shimmering glow, no breathing bleed, no parallax. Allowed one-shots (each skipped under reduce-motion): rail fold 200 ms; Neon glow pulse **once** when playback starts (alpha 0.30 -> 0.45 -> 0.30 in 600 ms); sticker settle -16 -> -12 deg once, 240 ms, when a page opens. A pulse is a state-change cue, not decoration.
- Bold text (`fontWeightAdjustment`) and high-contrast text settings are not read by the app today (`UI_AUDIT.md` section 6, gap 7). The new UI reads `fontWeightAdjustment` and bumps Figtree one weight step; it does not read high-contrast text (no Android API for it that the app can rely on: UNCLEAR).

---

## 8. Risks and open questions

### 8.1 Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | `BitChordApp` / `MainViewModel` / `SettingsSheet` are at or near size limits ("Method too large") | One small call per edit, host composables with plain parameters, new rows in a new file. |
| R2 | The variant switch recreating the media controller or the player | Same composition shape in both variants; only theme and chrome values change; a device test with music playing and a Jam in progress (the earlier test plan, `UI_TEST_PLAN.md`, has the checklist). |
| R3 | `:sharedUi` pages (Home, Library, Settings, player) carry hard-coded colours and fixed heights (`AccentRed`, iOS greys; `TopBarContentHeight = 52.dp`) that ignore the theme | Theme reaches only what reads `MaterialTheme`; list offenders as found; do not edit `:sharedUi` (shared with the dormant desktop); log any unavoidable change in `FROZEN_TARGETS.md`. |
| R4 | Masonry Home/Library needs new pages in `:app`, duplicating shared Home/Library logic | Re-skin first (theme, tiles as a wrapper for existing shelves); only build masonry pages if the owner asks (open question 9). |
| R5 | Glass cost: a second full draw of the page is already paid while Liquid Glass is on (`UI_AUDIT.md` 4.3); no baseline frame numbers exist | Budget in section 7; `FrameStats` (debug logcat tag `VioFrames`) before and after on the owner's phone; non-live everything except the player bar. |
| R6 | Variable fonts: `FontVariation` weights must be set per `Font(...)`; a wrong setup falls back to the default weight silently | A small test that resolves each role to a `Font` and a screenshot check per weight. |
| R7 | Rotated text: Compose rotation does not change layout, and `rotate` clips at the unrotated bounds | The swap-size layout modifier (5.1 row 3); a UI test at fontScale 2.0. |
| R8 | The mockup is a board, not a screen; layouts for narrow phones (360 dp) are **my composition**, not measured | Marked wherever it happens (6.1 header, 6.2 lane); owner review of screenshots in the first UI phase. |
| R9 | Heavy display styling on non-Latin locales looks wrong or breaks shaping | `ScriptProfile` rules (4.3); one screenshot per script at the end of the first UI phase. |
| R10 | Colour pickers produce unreadable choices | The three derived values (3.2) with live preview. |
| R11 | `Highlight` and `Shadow` are blurred layers: stacking the new strokes over them can double-draw edges | The tint stroke is the **only** extra stroke on Neon; check on device that rim width stays about 2 dp total. |
| R12 | Rail text length depends on translations (Home, Explore, Library, Search in 17 languages): some are much longer (German, Turkish) | The tab height is computed from the measured text; the dock fallback triggers on measured height, not just on font scale. |
| R13 | Predictive back: the manifest does not set `enableOnBackInvokedCallback`; changing it would affect the current UI (`UI_PLAN.md` section 9) | Not changed. UNCLEAR how the Hub sheet behaves under the predictive-back gesture. |
| R14 | `:tv` compiles `app/src/main/java` | UNCLEAR how `:tv/build.gradle.kts` selects files; new files must either avoid phone-only types or sit in a folder `:tv` excludes. Check first in the implementation phase. |
| R15 | Private fork: the SF Pro and SF Symbols licence flags in `UI_AUDIT.md` section 5 stay for the **current** UI; the new UI uses only OFL fonts and Material icons (Apache-2.0) | Keep the new UI's assets OFL/Apache only; list every one in `BRUTAL_UI_PLAN.md` when it exists. |

### 8.2 Open questions for the owner

1. **Which image is Glow?** The brief says image 1 = Glow, image 2 = No-Glow, but image 2 is the one with the glow (section 0.1). This spec assumes **Glow ON = image 2 look (Neon)** and **Glow OFF = image 1 look (Clear)**. Confirm or reverse.
2. **Search twice.** The rail has a round Search button **and** a Search tab. Keep both (quick action vs page, as drawn here), or replace the fourth tab with a "You" tab that opens the Hub?
3. **Light theme.** The mockups are dark only. Proposal: the new UI is **dark-only in its first version** (Settings theme choice is ignored while it is on) and a light variant (inverted sticker palette) is a later phase. Agree?
4. **Header content.** The mockup shows no avatar or back button, but the app needs them (Hub entry, back). Proposed: avatar at the end of the header, back at the start, wordmark masthead on Home only. OK?
5. **Rail lane vs player bar width.** The mockup lets the bars stop at the lane; this spec keeps bars full width below the rail (6.2). Prefer the mockup's way (narrower bars)?
6. **Body font and Cyrillic / Vietnamese / Hebrew.** Figtree has no Cyrillic or Vietnamese; the spec falls back to the system font in `ru`, `vi`, `he` (4.3). Alternative: bundle **Open Sans** (532,636 B variable, covers Cyrillic, Greek, Hebrew, Vietnamese) as the body font for **every** locale (looks less like the mockup, +470 KB). Which?
7. **Heavy italic.** The brief lists "heavy italic" as a candidate; the sticker in the mockup is upright and only "Acidic colors" in image 2 is italic. This spec has no bundled italic (Compose synthesises a slant when asked, Latin only). Add the 449 KB italic files (Archivo italic is 741 KB, Figtree italic is 63 KB), or skip?
8. **Where does the Glow switch live?** A row at the top of `SettingsCategory.ADVANCED` (as the owner's rule says) with sub-rows: New UI on/off, Glow on/off, Accent, Rail side, Reduce transparency. Also a "Glass tier" override (Auto / Full / Simplified / Solid) for testing? Proposed: yes, hidden under a long-press for the owner only.
9. **Scope of the first UI phase.** Re-skin only (theme + chrome + tiles as wrappers for the current pages) or also **new masonry Home and Library pages** in `:app` (more work, duplicates shared page logic)? The spec works either way; this changes the risk R4.
10. **Reference phone.** Which phone and Android version should frame times be judged on? (`UI_DECISIONS.md` section 6 item 3 is still blank.)
11. **Reduce transparency default.** On by default for low-RAM phones (proposed), or off? (`UI_DECISIONS.md` section 6 item 4.)
12. **Sticker content.** The mockup's ABOUT / PRIVACY / VERSION 0.4 is example copy. Proposed use: row 1 = page name, row 2 = section or count, row 3 = meta (date, count, version). Any specific screens that must have a sticker?

### 8.3 What this spec did not do

- It did not build anything or touch app code, resources, strings or Gradle. The type board is a static documentation file.
- It did not measure perspective skew, the glass refraction profile across a bevel (only its width), blur radii, or any behaviour; those need a device and the implementation.
- Fonts were identified by eye from crops; they were not matched by a font-recognition tool. Sizes come from the Google Fonts repository metadata read on 2026-11-02.
- Frame times, TalkBack behaviour and RTL appearance are untested (no device).
