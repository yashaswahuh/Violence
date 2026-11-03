# Violence Style: specification (S0)

Authority: the four mockups in `fork-docs/style-reference/` (local only, gitignored, copyrighted covers) win over this text. Numbers marked **ESTIMATE** were read off the images by eye, assuming the 842 px wide mockups are a 390 dp wide phone (about 2.16 px per dp); confirm them on a device in S3. Everything here applies only while the master toggle is ON. With it OFF the app is exactly what it is today.

## 1. Verified observations (and corrections to the brief)

The brief's ten observations were checked against the images. Confirmed unless listed here.

| # | Brief said | Images show |
|---|---|---|
| 1 | Background styles per ref | Confirmed. ref1: warm red/maroon glow, **two thin curved light streaks** (top-right entering and curving down-left, bottom-right rising). ref2: blue glow top-left plus a second at mid-right, fading to black toward the bottom. ref3: one strong blue glow in the top-left corner only. ref4: magenta haze behind the header and the middle, darker vignette at the bottom |
| 3 | Hairline ends in a dot or streak, "RECENTS" same treatment | **Only the first (hero) heading has the glowing terminator** (dot in ref2, ref4; short red streak in ref1). "RECENTS" has a plain thin gray hairline with no terminator in all four |
| 3 | ref2-4 "for You" lighter italic | Confirmed (italic, same size as "TOP PICKS", not lighter in colour). ref1 sets both lines in caps and a heavier weight than ref2-4 |
| 4 | Corners 28 to 32 dp | About 22 to 28 dp (ESTIMATE). Default 26 dp. Width about 58 percent of the screen (today 60 percent) |
| 4 | Label sentence case (ref2-4) or letter-spaced caps (ref1) | Confirmed; the same applies to the subtitle ("Single . 2026" vs "SINGLE . 2026") |
| 5 | Thumbnail about 56 dp, corners about 10 dp | **About 44 to 48 dp, corners about 8 to 9 dp (ESTIMATE)**: the same size as today's 48 dp row art (7 dp corners). Keep 48 dp, corners 9 dp |
| 7 | Selected tab is an accent-tinted capsule with a bright bottom line | True for ref2 (blue), ref3 (blue) and ref4 (pink). **In ref1 the selected capsule is neutral dark gray** with a faint rim and no coloured line. So the capsule is accent-tinted whenever a glow/accent is active, neutral when the accent is "White" or the background is Off |
| 8 | Accent drives glow, capsule, hairline terminators, card rim | Confirmed. In ref1 the rim of the first card and the streak are red; the nav is neutral |
| 2 | Bold white "V" mark at the left | Confirmed. The app has no such mark today (Home shows a "VIOLENCE" text wordmark, `HomeScreen.kt:493`). The V needs an asset or a drawn path: owner decision |
| 2 | Cast button in the pill | The app's pill holds Receive/NFC, Downloads, update and the account avatar; a cast action is not there. The skin restyles the existing actions and **adds no cast button** (that would be a new feature) |

Layout in the mockups equals today's layout. Nothing in this spec moves, adds or removes a screen, a control or a feature.

## 2. Background styles

All styles are drawn by one function into one layer behind every page, using only: radial gradients, linear gradients, and thin stroked cubic curves. **No blur effect, no RenderEffect, no bitmap, no shader** (the glass above it already blurs the layer; a second blur costs frames). Base colour: black tinted 4 percent toward the accent (about `#05050A` for blue).

Notation: `I` = intensity cap (Low 0.30, Medium 0.50, High 0.70, the peak alpha of the strongest blob). `w`, `h` = layer width and height. Radii are in multiples of `w`. All blobs fade to alpha 0 at their radius with an eased falloff (stops at 0, 0.35, 0.7, 1.0 with alphas 1, 0.55, 0.18, 0 of the blob's peak) so there is no visible edge.

| Name | Construction | Matches |
|---|---|---|
| **Corner glow** | Blob A centre (0.05w, 0.02h) radius 0.95w peak I. Blob B centre (0.25w, 0.12h) radius 1.3w peak 0.35 I. Base below | ref3 |
| **Aurora** | Blob A (0.08w, 0.08h) r 0.85w peak I; Blob B (1.0w, 0.46h) r 0.75w peak 0.8 I; Blob C (0.1w, 0.70h) r 0.6w peak 0.25 I. A vertical linear gradient from transparent (0 to 60 percent of h) to base alpha 0.85 (100 percent) pulls the bottom to black | ref2 |
| **Haze** | Blob A (0.5w, 0.13h) r 1.0w peak 0.9 I; Blob B (0.5w, 0.58h) r 1.2w peak 0.55 I. A vignette: vertical linear gradient from transparent (70 percent of h) to base alpha 0.9 (100 percent) | ref4 |
| **Arcs** | Warm low blob (0.9w, 0.05h) r 0.9w peak 0.4 I plus (0.95w, 0.97h) r 0.7w peak 0.3 I. Two stroked curves: arc 1 from (0.55w, -0.02h) bending through (0.85w, 0.10h) to (1.05w, 0.16h); arc 2 from (0.7w, 1.02h) through (0.9w, 0.95h) to (1.05w, 0.92h). Each is drawn three times: 9 dp at alpha 0.07 I-scaled, 3 dp at 0.18, 1.2 dp at 0.85, with a linear gradient along the path (transparent at the start, accent at 70 percent, white-tinted at the end) | ref1 |
| **Off** | Plain base colour. Skin typography, glass and accent still apply | none |

Intensity levels also scale blob radii (Low x0.8, Medium x1.0, High x1.15). Stroke widths do not scale.

### Motion

Time-based, smooth, no keyframe jumps: each blob centre moves on an ellipse, `centre = base + (ax * sin(2 pi t / Tx + phase), ay * cos(2 pi t / Ty + phase2))`, so it never stops or reverses abruptly. Arcs translate along their normal, `offset = a * sin(2 pi t / T)`. The clock is read only in the draw phase (no recomposition per frame).

| Level | Amplitude (fraction of `w`) | Periods | Frame rate of the clock |
|---|---|---|---|
| Off | 0 | static, single frame | none |
| Subtle | 0.025 | 45 to 70 s, a different period per blob | 30 fps cap |
| Normal | 0.06 | 24 to 40 s, a different period per blob | 30 fps cap (the draw is a few gradient fills; 30 is smooth for motion this slow, and halves the cost on 60 Hz panels) |

Phases are fixed constants (not random) so tests and screenshots are repeatable. When the accent changes the glow crossfades its colour over **900 ms, FastOutSlowIn**, by animating one `Color` (all blobs derive from it); a change during a crossfade restarts from the current colour. Song-change accents are debounced 400 ms so fast skipping does not flicker. Reduce motion (section 9) makes the crossfade a cut.

## 3. Accent system

One `Color` (the "accent") drives the glow, the selected capsule, the hairline terminators and the card rim. Anything else stays neutral.

### Sources (setting "Accent source")
1. **From the current song.** Uses the existing `rememberArtworkPalette(...).accent` (`ArtworkPalette.kt`) of the playing song's artwork. Nothing playing: keep the last accent of this session; none yet: use the Custom colour (default Blue).
2. **From the device.** Android 12 (API 31) and later: the system accent, `android.R.color.system_accent1_500` (read from resources; `dynamicDarkColorScheme` is not needed and the app does not use Material You today). Android 8 to 11: fixed Blue `#3F7BFF`.
3. **Custom.** Presets plus a picker. Presets (ESTIMATE, tuned by eye): Blue `#3F7BFF`, Red `#E5384F`, Pink `#E04A9B`, Green `#2FBF71`, Orange `#F28C28`, Yellow `#F2C230`, Cyan `#26C6DA`, Purple `#8B5CF6`, White `#E8E8F0`. The picker (hue + saturation + lightness sliders; no new dependency) passes through the same clamp.

### Clamp (applied to every source, including presets and the picker)
Work in HSL: keep hue; saturation to [0.55, 0.90]; lightness to [0.45, 0.65]. The glow colour is that accent with lightness x0.8. For the **White** preset the clamp is skipped (saturation stays at most 0.08) and the capsule becomes neutral (ref1 look).

**Contrast guard** (a pure function, unit-tested in S2): evaluate the composited background (glow over base) at a grid of points, 12 by 24 over the layer. Required: in the top 35 percent of the height the composited relative luminance is at most 0.18 (white text 4.5:1); below it at most 0.05 (white at 70 percent alpha, the secondary text, stays at 4.5:1 or better). If a point fails, the peak alpha is reduced until all pass; the result is the "effective intensity", never higher than the chosen level. The check runs when the accent changes, not per frame.

### Gray, dark, dull artwork
If the artwork's chroma is below `TINT_MIN_CHROMA` (0.08, existing constant) or its saturation after the clamp is under 0.35 of the target: do not output a gray glow. Hold the previous accent; if none, fall back to the Custom colour. Very dark artwork is lifted by the lightness clamp; near-white artwork is dimmed by it. Same-hue changes smaller than 6 degrees and 4 percent lightness are ignored (no re-fade).

### Use per element
| Element | Accent use |
|---|---|
| Background | Glow colour (section 2) |
| Selected tab capsule | Vertical gradient accent 38 percent at the top to 16 percent at the bottom, plus a 2 dp bottom line in accent (lightness +0.15) at 90 percent and a 6 dp wider line at 22 percent. Neutral (white 14 percent) when accent is White or background is Off |
| Hero heading terminator | Dot or streak in accent, with a halo |
| First hero card rim | 1.2 dp accent line at 85 percent plus 6 dp at 22 percent; the bottom edge picks up a short gradient of accent (35 percent) |
| Other cards | Neutral rim only (white 16 percent, 1 dp) |
| Hairlines (non-hero) | Neutral white 24 percent |
| Switches, sliders, links | Unchanged in S1 to S4. S5 may tint "on" states with the accent |

## 4. Glass rules

Glass reuses the existing pipeline (`liquidGlass`, `lightweightLiquidGlass`, `liquidGlassRim`). The skin adds a tint, not a new renderer.

- **Rim:** 0.75 dp stroke. With glow: a vertical gradient white 22 percent (top) to accent 14 percent (bottom). Without glow (background Off, LITE mode, glass off): white 12 percent, no accent.
- **Top-right pill** (actions + avatar): a pill, `liquidGlass` with tint `#101018` at 0.45; rim as above; icon colour white; separator none. Contents are the existing actions.
- **Round icon buttons:** same recipe as the pill, 44 dp visual, hit target raised to 48 dp (section 8).
- **Bottom nav pill:** existing `GlassNavBar` glass; tint `#0C0C12` at 0.42; rim as above; unselected icon and label white 60 percent, selected white.
- **Search circle:** same as the pill.
- **Mini player:** full width pill, tint `#0C0C12` at 0.45; thumbnail 40 dp, title/artist as today; play and next as solid white glyphs.
- **Floating over content:** content scrolls under the bars with a bottom fade (section 7).
- **Glass off or below API 31:** Haze or a solid fill of `#0E0E14` plus the rim; the capsule and glow still work, since they do not need glass.

## 5. Typography

- **Headings: a light, high-contrast display serif** (finalist list in `FONT_SPECIMEN.html`; the choice is in `STYLE_DECISIONS.md`). Always SIL OFL. Bundled as TTF in `res/font`, never downloaded.
- **Body: Inter** (OFL), replacing SF Pro Display **only while the skin is on** (SF Pro stays for OFF; its licence is flagged in `UI_AUDIT.md` section 5 for any public release, which the skin neither fixes nor worsens).
- **Heading usage:** "TOP PICKS" in caps, upright; "for You" in the italic of the same family, sentence case (ref2-4). ref1 sets both lines in caps. A setting "Heading style: Mixed (caps + italic) or All caps" with Mixed as default is proposed (owner confirms).
- **Hairline and terminator** follow the title on the same baseline row (section 6).

Size scale (sp; line height in brackets). Sizes are ESTIMATES from cap heights.

| Style | Font | Size | Weight | Tracking |
|---|---|---|---|---|
| Heading L ("TOP PICKS / for You") | display serif | 36 (40) | Light/Regular (300 to 400) | +0.01 em |
| Heading M ("RECENTS") | display serif | 24 (28) | same | +0.02 em |
| Card title ("GUZAARISH") | display serif | 20 (24), caps | Regular | +0.02 em |
| Card label ("New from Karun") | Inter | 13 (16) | Regular; caps variant +0.08 em | |
| Card subtitle | Inter | 13 (16) | Regular, white 86 percent | |
| Row title | Inter | 16 (20) | Medium | 0 |
| Row subtitle | Inter | 14 (18) | Regular, white 60 percent | 0 |
| Tab label | Inter | 12 (14) | Medium | +0.01 em |
| Mini player title/artist | Inter | 16 / 14 | Medium / Regular | |

**200 percent font scaling:** body text scales fully. The display serif scales at most 1.3x (`min(fontScale, 1.3)`), because headings at 2x break the layout; every heading has `maxLines = 2` and ellipsis. The hairline is a weighted element with a 24 dp minimum and is hidden when it would be shorter (it is decoration). Card captions allow 3 lines at scale above 1.3. Fixed-height chrome (top bar 52 dp, nav pill) keeps today's behaviour; S6 tests 200 percent on real screens and widens the pill if clipped.

## 6. Section headers

Layout: `[Title (1 or 2 lines)] [12 dp gap] [hairline ---> right margin] [terminator]`, vertically centred on the last title line. Hairline 1 dp, white 24 percent (hero: white 32 percent fading to accent over the last 25 percent). Terminator, hero heading only: **Dot style** (Corner glow, Aurora, Haze): 6 dp solid accent circle with halos 14 dp at 28 percent and 28 dp at 10 percent. **Streak style** (Arcs): the last 56 dp of the hairline is a 2 dp gradient from transparent to accent to white-tinted, with a 6 dp 20 percent under-stroke. Right margin 24 dp. Headings use `Modifier.semantics { heading() }`. The "Show all" action, where it exists, stays at the right and the hairline stops 12 dp before it.

## 7. Hero cards, rows, spacing

- **Hero card:** 3:4, width 58 percent of the row (today 60; keep 60 unless the owner wants exactness; the next card must peek about 16 dp), corner 26 dp, gap 14 dp, full-bleed artwork, scrim vertical gradient transparent at 45 percent to black 0.80 at 100 percent, caption bottom-left with 16 dp padding, thin 1 dp neutral rim; first card rim light per section 3. Cards that are drawn art (Discovery Station) take the same rim. The current art, motion and long-press behaviour are untouched.
- **Rows:** art 48 dp, corners 9 dp, no dividers, row padding as today, three-dot at the right (existing `RowMoreButton`), title and subtitle per section 5. The current-track playing bars stay.
- **Spacing:** card gutter stays 16 dp. Display headings start at 24 dp and end 24 dp from the right edge (ESTIMATE from the images: heading x about 25 dp, cards 16 dp, row art 23 dp, hairline end 20 dp); flexible. Section gap 26 dp as today.
- **Scroll under bars:** a vertical gradient from transparent to the base colour (alpha 0.9), 72 dp tall, drawn behind the bars and above the page (S4).

## 8. States

- **Pressed:** scale 0.97 over 90 ms, plus white 8 percent overlay on glass and capsules; no ripple on glass.
- **Selected:** capsule (section 3). The selected tab is also announced (`selected` semantics).
- **Disabled:** 38 percent alpha, no rim glow.
- **Touch targets:** 48 dp minimum for every skinned control (the 44 dp top controls get a 48 dp hit area without changing their look).
- **Focus:** TV is out of scope.

## 9. Dark-only handling

While the skin is on, the app resolves its theme to **dark** regardless of the in-app theme and the system theme (recommended default, to be confirmed by the owner). The skin applies its own dark `ColorScheme` through a wrapper, status and navigation bar icons are forced light, and the theme row in Settings shows "Violence Style uses the dark theme" while it is on. Turning the skin off restores the previous theme choice (the stored `themeMode` is never overwritten). Alternatives, not recommended for v1: a light skin (needs inverted glows and gray text contrast rules) or skin off automatically in light.

## 10. Performance budget and LITE mode

Targets on a mid-range phone (about 4 GB RAM, Snapdragon 6-series class, 60 Hz): p95 frame time at or below 16.6 ms while scrolling Home with Normal motion and glass on; no jank burst when the accent changes. **No device numbers exist** and there is no emulator or adb on the owner's PC; the plan therefore adds an on-screen debug frame readout in S6 (the existing `FrameStats` logs only to logcat). Never claim a measured frame time without that readout.

- **Animated layers:** at most one continuously animating layer (the background clock). Temporary animations (accent crossfade 900 ms, press feedback) do not count. No shimmer or sweep effects. Rims, glows and terminators are static.
- **Drawing cost:** the background is a single `drawBehind` of at most 4 gradient fills and 2 strokes (x3 passes) per frame, with no offscreen buffer and no bitmap.
- **LITE mode** (automatic; shown as a note in Settings): static background (one frame, no clock), glass falls back to `lightweightLiquidGlass` or a solid fill, no bloom strokes, no halo, no accent crossfade (cut). Triggers, any of: API below 31; `ActivityManager.isLowRamDevice`; battery saver on (`PowerManager.isPowerSaveMode`); thermal status SEVERE or worse (API 29+); in-app "Reduce dynamic blur" on; Motion set to Off (static background only, glass unchanged).
- **No continuous animation when:** the app is not in the foreground (`rememberIsForeground()` exists in sharedUi); the full player covers the page; the screen is idle (no scroll or touch for 30 s: the clock stops and resumes on touch); reduce motion is on (the system animator duration scale is 0, or the in-app `reduceAnimation` is on).
- **Reduce transparency:** I found no public Android switch for it (UNCLEAR, to confirm in S1 against the Android documentation). The skin's reduce-transparency behaviour follows the existing in-app "Reduce dynamic blur" (solid fills plus rim); it cannot follow a system setting that may not exist.

## 11. What the skin never does

Change a layout, a route, a feature, a string's meaning, playback, sharing or Jam behaviour; add a network call, a tracker or a dependency; ship or load the mockup artwork; run when the toggle is OFF.
