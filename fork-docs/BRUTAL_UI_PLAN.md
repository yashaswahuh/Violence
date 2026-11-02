# Brutal UI: plan, licences and rules as built

Design source: `BRUTAL_UI_SPEC.md` (measured from the two mockups) and `brutal-ui/type-board.html`.
Scope: the Android phone app (`:app`) only. TV, web, Wear OS and desktop keep their UI. Wear OS and desktop: dormant, not checked.
The CURRENT UI is the default and behaves as before when the new UI is off.

## 1. Phases

| Phase | What | Status |
|---|---|---|
| **B-1 (this document's first entry)** | Plumbing only: settings, root switch, safe mode, glass-level decision, tokens, contrast engine, fonts, strings, tests. Visual result deliberately minimal: the existing app in the new colours and type | **built 2026-11-02**, see section 5 for what was and was not verified |
| B-2 | Navigation chrome: rail, mini player bar, dock, header (sticker, masthead), Hub | not started |
| B-3 | Components: tiles, rows, chips, stamps, buttons; Glow layers (spec section 2) | not started |
| B-4 | Pages, performance, accessibility pass, translations | not started |

## 2. Licences (every library, font and asset the brutal look adds or reuses)

All free, all GPL-3.0 compatible. No network fonts or assets at runtime. No new runtime library was added in B-1.

**New fonts, bundled in `app/src/main/res/font/`** (unmodified files from https://github.com/google/fonts, directory `ofl/`; texts in `NOTICE`):

| File | Font | Licence | Copyright | Size |
|---|---|---|---|---|
| `brutal_display.ttf` | Archivo Black (`ArchivoBlack-Regular.ttf`) | SIL OFL 1.1 | 2017 The Archivo Black Project Authors | 90,988 B |
| `brutal_stamp.ttf` | Archivo Narrow variable (`ArchivoNarrow[wght].ttf`) | SIL OFL 1.1 | 2019 The Archivo Narrow Project Authors | 93,304 B |
| `brutal_body.ttf` | Figtree variable (`Figtree[wght].ttf`) | SIL OFL 1.1 | 2022 The Figtree Project Authors | 62,712 B |
| `brutal_mono.ttf` | IBM Plex Mono SemiBold (`IBMPlexMono-SemiBold.ttf`) | SIL OFL 1.1 | 2017 IBM Corp. | 140,216 B |

Sizes were checked against the downloaded files (byte-exact with the sizes read from the repository API when the spec was written). Total 387,220 B.

**New test-only libraries** (`app/build.gradle.kts`, never shipped in a release APK; `ui-test-manifest` is `debugImplementation`, so it is in debug builds only):

| Library | Version | Licence |
|---|---|---|
| `org.robolectric:robolectric` | 4.14.1 | Apache-2.0 |
| `androidx.compose.ui:ui-test-junit4` | BOM-managed | Apache-2.0 |
| `androidx.compose.ui:ui-test-manifest` | BOM-managed | Apache-2.0 |

**Reused, already in the app** (not changed): the vendored Kyant0 backdrop library (`ui/components/backdrop`, Apache-2.0), `LiquidGlass.kt`, `AppGlass.kt`, `GlassPanels.kt` (GPL-3.0, adapted from Echo-Music), Jetpack Compose and Material 3 (Apache-2.0), `ContinuousCornerShape` (ours). The three Settings rows' icons come from the app's existing `SfIcons` set. That set is a third-party port of Apple's SF Symbols with an unclear licence: `UI_AUDIT.md` section 5 already flags it for the current UI, and B-2 onward should replace it in the new UI only (Material icons, Apache-2.0, or Lucide, ISC).

**Assets**: none added besides the fonts. No images, no network requests.

## 3. Rules as built (B-1)

### 3.1 Settings (`AppSettings`, SharedPreferences `bitchord_settings`)

Appended next to `liquidGlass`; nothing existing was changed. Every new value is read in `readAll()` and written by a setter.

| Flow / setter | Key | Type and default | Parsing |
|---|---|---|---|
| `uiVariant` / `setUiVariant` | `ui_variant` | `UiVariant` `CURRENT` or `BRUTAL`, default **CURRENT** | by name; anything else (null, `NEW` from the removed earlier experiment, wrong case) reads as CURRENT |
| `brutalGlow` / `setBrutalGlow` | `brutal_glow` | Boolean, default **true** | |
| `brutalAccent` / `setBrutalAccent` | `brutal_accent` | `BrutalAccent`, default **Yellow** | preset id (`yellow red blue green orange pink cyan white`) or `custom:AARRGGBB` (six digits also accepted; always stored opaque); anything unreadable is Yellow |
| `brutalRailSide` / `setBrutalRailSide` | `brutal_rail_side` | `BrutalRailSide` `LEFT`/`RIGHT`, default **RIGHT** | by name; else RIGHT |
| `reduceTransparency` / `setReduceTransparency` | `ui_reduce_transparency` | Boolean, default **false** | |

### 3.2 Settings screen

Rows are at the **top of the ADVANCED group** ("Advanced & experimental options", there is no Experimental tab), in `ui/brutal/settings/BrutalSettingRows.kt`. `SettingsSheet.kt` has one statement: `brutalSettingRows().forEach { r -> row(*r.keywords) { r.content() } }`, so each row still goes through `row(...)` (searchable, divided like the rest) while all code stays in the brutal package.
- **New UI (experimental)** switch, subtitle says the current UI is unchanged and stays the default, dark theme only, music keeps playing. Always visible.
- Only while it is on: **Glow** switch, **Accent colour** (current name and a colour dot; opens the sheet), **Rail side** (Left / Right segmented control), **Reduce transparency** switch.
- All apply immediately (a `StateFlow` write; the theme reads it).
- **Accent sheet** (`GlassBottomSheet`, the existing glass sheet): eight 48 dp preset swatches (radio semantics, labelled, selected = white ring and tick), and a custom picker built here from three sliders (hue, saturation, brightness; no library). A preset applies on tap; a custom colour applies on "Use this colour" so dragging does not re-theme the app every frame. The preview shows what the app will actually draw with (fill with its text colour, accent text on glass, stroke colour) and says when the colour had to be lightened.

### 3.3 Root switch (`ui/brutal/BrutalRoot.kt`)

`MainActivity` has four marked touch points (`UPSTREAM_SYNC.md`): the safe-mode check in `onCreate`; the effective dark flag; the window background; and `BrutalRoot(dark = darkTheme) { BoxWithConstraints { BitChordApp(...) } }`.
- **Same composition shape in both looks**: `BrutalRoot` calls `BrutalTheme(enabled, ...)` from one place, which always provides the same locals and always calls `MaterialTheme(...)`; only the provided values differ. With the current UI it re-provides the current scheme, typography and shapes, and the current `LocalLiquidGlassEnabled`, unchanged, and registers no receiver or observer. A Robolectric test (`switchingTheVariantDoesNotRecreateTheContent`) toggles CURRENT, BRUTAL, CURRENT, BRUTAL and asserts the content below is composed once and never disposed: that is the property that keeps the media controller (inside `BitChordApp`) alive across a switch.
- The brutal look is **dark only** in this first version (spec open question 3): while on, `brutalEffectiveDark` forces dark and the Theme setting is ignored. With the current UI the dark flag is exactly the user's choice.
- The backdrop floor and window background use the page colour `#08080A` while on (`brutalWindowBackground`), so the glass blur floor matches.
- `BitChordApp`, `MainViewModel`, `SettingsSheet` internals, `:sharedUi` and every glass file are not edited.

### 3.4 Theme and tokens (`ui/brutal/theme/`)

- **Palette** (`BrutalPalette`): page `#08080A`, ink `#0A0A0A`, white, acid cyan `#1FDEDB`, muted text `#A8A8B0`, glass body `#1D1D1D`, brightest glass `#2C2C2C`, hairline white 10 percent, and the derived accent. Mapped to a Material dark scheme: `primary` = accent fill with `onPrimary` = its ink-or-white, `secondary` = acid, `background` = page, `surfaceVariant` (grouped Settings cards) = glass body, `onSurfaceVariant` = muted. Existing pages wear it through `MaterialTheme`.
- **Accent derivation** (`BrutalAccentRules.derive`, spec 3.2), all pure Kotlin: `ui` = chosen colour, lightened in 5 percent steps toward white until at least 3:1 on the page; `text` = `ui` lightened until at least 4.5:1 on the brightest glass; `onAccent` = ink or white, whichever is higher on `ui`; `fill` = `ui`, moved (toward white if ink won, toward black if white won, 4 percent steps) only if neither reached 4.5:1. `glowGain` 1.0 / 1.15 / 1.35 by luminance (>= 0.5 / >= 0.2 / below). `bleedAlphaCap` steps the bleed alpha down so white text on it stays 4.5:1 (yellow: 0.45).
- **Contrast engine** (`ContrastMath`): WCAG ratio, `over` (sRGB alpha blend), `bestText`, `guard` (Fine / Scrim / Solid), `ensureContrast`, the compounding `walk`, and the fixed `TEXT_SCRIM_ALPHA = 0.55` (black behind white text: at least 4.5:1 over any backdrop, asserted over white, yellow and cyan).
- **Spacing and sizes** (`BrutalDimens`), **angles** (`BrutalAngles`: sticker -12, masthead -8.5, tag -4, vertical labels -90), **shapes** (`BrutalShapes`: continuous corners 19 and 20 percent for tiles and list artwork, stadium, sticker square), all from spec section 1.
- **Motion** (`BrutalMotion`): springs; with reduced motion (the app's Reduce animation, or the system animator scale 0) a 90 ms fade, an instant rail fold, **no tilt settle and no glow pulse**; never anything continuous while idle. `Density.withDisplayFontScale()` caps display text at a font scale of 1.3.
- **Type** (`BrutalType.kt`): fonts as in section 2; the full spec scale as `TextStyle`s (`sticker 48 sp`, `wordmark 16 sp at 1.3 em`, `listTitle 20`, `listArtist 13`, `chip 12`, `stamp 11`, ...) and a Material typography that **keeps the current app's sizes** (34/30/22/20/16/14/12/11) with the new families and weights, so existing pages do not blow up at 200 percent font. `ScriptProfile`: `LATIN` languages get the designed type; every other language (ru, vi, he, hi, th, ja, ko, zh) uses the system sans with no letter-spacing, no uppercase and no synthetic italic. The wordmark and tag stay Latin everywhere.

### 3.5 Glass level (`ui/brutal/glass/GlassLevel.kt`)

`decideGlassLevel(GlassInputs)` is a pure function:
1. **SOLID** if `sdkInt < 31`, or Reduce transparency, or the existing Reduce dynamic blur.
2. else **SIMPLIFIED** if low-RAM, battery saver, reduced motion (app switch or system animator scale 0) or `sdkInt < 33` (no lens shader).
3. else **FULL**.

`GlassBudget` allows 3 live layers (`tryAcquire` / `release`). Provided as `LocalGlassLevel` and `LocalGlassBudget`. While the new UI is on, `LocalLiquidGlassEnabled` = (level == FULL) so the existing glass machinery (page backdrop recording, glass top controls and bars) stays consistent: it is live at FULL and falls back to its non-glass materials below. **Consequence to know**: with the new UI on and a FULL device, the existing bars turn into live glass even if the old "Liquid Glass" setting is off, because the brief asks the new UI to own that switch.

### 3.6 Safe mode (`ui/brutal/safemode/`)

A marker (own file `violence_brutal_boot`, written with `commit()`) is set when the new UI first composes and cleared two frames after it is displayed. A marker still set at the next launch is a failed start; **two consecutive failed starts** persist `uiVariant = CURRENT` and show a one-time dialog ("Back to the current UI"); a start that reaches its first screen resets the count; the current UI clears any old state. The logic (`BrutalSafeModeLogic`) is behind a store interface and unit-tested for 0, 1 and 2 failures, success resetting, non-consecutive failures and the notice showing once. Note: the marker also catches a process kill in the first two frames, not only a crash.

### 3.7 Strings

29 `brutal_*` keys (46 after the components phase, section 10: 17 more) with **English placeholders** in all 20 `strings.xml` files (default plus 19 locale folders, including the 17 supported languages and the `zh-rHK` and `b+zh+Hant` extras). **Translations are not done**: those rows read in English in every language until a translation pass. `LocaleStringsTest` guards that no locale misses a key.

## 4. Deviations from the brief and from the spec in B-1 (decided, not hidden)

1. Settings rows: one `row(...)` call per row, but made by a `forEach` over `brutalSettingRows()` in a single upstream statement, instead of five literal `row(...)` calls. Reason: the four conditional rows need a state read, which the 1,500-line `SettingsScreen` function should not grow.
2. `primary` is the accent **fill** (equal to the chosen accent for every preset), not the raw accent, so `onPrimary` is always at least 4.5:1. They differ only for unusual custom colours.
3. The Material typography keeps the current sizes; the 48 sp sticker size exists as `BrutalTypeSet.sticker` for B-3.
4. Dark only (open question 3 of the spec): a Light theme choice is ignored while the new UI is on.
5. `ui-test-manifest` is a `debugImplementation`, as it was in the earlier attempt; it adds a test activity entry to debug builds only.

## 5. Verification (what ran, with its result)

All Gradle work went through `scripts/` (explicit module paths, never a root task). Dates are 2026-11-02.

| Check | Command | Result |
|---|---|---|
| Phone build | `.\scripts\build-active.ps1 -Target phone` (`:app:assembleDevDebug`) | **BUILD SUCCESSFUL in 3m 50s**, "All requested active-target builds passed (phone)" |
| TV build | `.\scripts\build-active.ps1 -Target tv` (`:tv:assembleDevDebug`) | **BUILD SUCCESSFUL in 11m 14s** (TV compiles `app/src/main/java`, so it compiled the new package and the new fonts and strings) |
| Phone unit tests | `.\scripts\test-active.ps1 -Target phone` (`:app:testDevDebugUnitTest`) | **passed**. Report files read afterwards: **1,375 tests, 0 failures, 0 errors, 1 skipped** (the skip is `GeniusTest`; none of the 89 new tests is skipped) |
| Shared unit tests | same script (`:shared:jvmTest`) | **passed**, 234 tests, 0 failures |
| Locale test | part of the phone suite | `LocaleStringsTest`: 17 of 17 passed (every locale has every new key) |
| New tests alone | `:app:testDevDebugUnitTest --tests "com.music.bitchord.ui.brutal.*"` | **89 tests, 0 failures**: settings parsing 12, contrast 16, accent rules 11, glass level 11, glass budget 5, safe mode 10, tokens 7, Robolectric UI 17 |
| Web | not run | **No file under `web/` changed** (`git status`); `build-active.ps1 -Target web` was not run in this phase. Treat "web unaffected" as an inference from the diff, not a build result |
| Wear OS and desktop | not checked | dormant, not touched |

Test history, so the numbers are not read as first-try: the first test run failed to compile (an experimental-API opt-in in the accent sheet), the second had 9 failures, the third 1, the fourth passed. They found and fixed a **real bug**: `BrutalAccent.Custom` compared by its raw input, so a custom colour read back from storage was not equal to the one handed in (now equality is on the opaque colour). The other failures were test setup (a grey that was not actually mid-tone, the shared segmented control needing the player host installed, off-screen controls); one Robolectric test invokes the segmented control's click action instead of a simulated touch because a touch missed the control's measured segments under Robolectric.

**"Toggle on and off while a song plays: audio and queue unaffected"**, how it was verified and what was not:
- **Verified by test (JVM, Robolectric):** `switchingTheVariantDoesNotRecreateTheContent` switches CURRENT, BRUTAL, CURRENT, BRUTAL under `BrutalRoot` and asserts the content below it is composed exactly once and never disposed. `BitChordApp`, and the media controller created inside it, are that content in the real app, so a switch cannot recreate them. `theCurrentUiReProvidesItsOwnValues` asserts the current UI gets its own colour scheme and glass flag back unchanged.
- **Verified by reading code:** nothing in the new package touches the media controller, the player service, the queue, Jam or downloads; the only upstream edits are the ones in `UPSTREAM_SYNC.md`.
- **NOT verified (needs a real phone):** playing a real song through a switch, and watching audio, queue, the open player and a running Jam. The emulator launch below did not include playback.

## 6. Visual acceptance

**What was captured.** Two screenshots in `brutal-ui/screens/`:
- `home-current-ui-font100.png`: the current UI Home, for comparison (emulator 720 x 1600).
- `home-brutal-yellow-glow-on-font100-profile-prompt.png`: the new UI, yellow accent, Glow on, font scale 100 percent, dark (emulator 1080 x 2400). A one-time "Make your Violence profile?" dialog is on top of it; that dialog is part of the app, not a mock-up.

**What was NOT captured, and why.** The requested matrix (Glow on and off, yellow and a second colour, font scale 100 and 200 percent, for every screen built) was **not completed**. The emulator ran out of host memory (about 0.7 GB free of 8 GB), the app hit startup ANRs (also with the current UI selected, so not caused by the new code), and the run was stopped by the owner. Missing: the Settings Advanced page with the new rows, the accent sheet, the second accent, Glow off, and every 200 percent font scale capture. **Nothing has been seen on a real phone.** Also, in this phase the Glow switch is stored but changes nothing visible (the glow layers are B-3), so Glow on and off would look the same apart from the Settings row itself.

**Side-by-side with the mockups.** Compared on what exists. The mockups are in the owner's attachments (and described in `BRUTAL_UI_SPEC.md`); this table is against the one new-UI screenshot above and the code.

| Aspect | Mockup | B-1 result | Verdict |
|---|---|---|---|
| Page colour | black, about `#08080A` | black page `#08080A` (screenshot shows black) | matches (token; not pixel-sampled from the screenshot) |
| Accent | yellow `#FAD60A` on dialog buttons, chips, discs | the dialog's "Not now" and "Create profile" buttons are yellow in the screenshot | matches for text that reads `primary`; chips, discs and tabs do not exist yet. (The "Top Picks" card is yellow in the current UI too; its colour was not checked against the setting) |
| Body type | Figtree-like humanist | Figtree is in use (screenshot: "Make your Violence profile?", "Home", "Explore", "Library") | approximate: looks right, not compared glyph by glyph |
| Display type | Archivo Black-like heavy caps for stickers and titles | `displayLarge` and `headlineLarge` are mapped to Archivo Black in the theme; no page title was captured to confirm it on screen; no sticker exists | unverified on screen; the sticker is **not built** |
| Wordmark | "V I O L E N C E" very widely spaced, in a rotated glass tile with an EXPERIMENTAL tag | the existing plain text wordmark from before (not restyled) | **not built** |
| Glass | thick refractive glass with white rims; Neon variant with accent bleed, rim tint, under-glow | the existing bottom bar is live glass (the new UI turned the app's glass machinery on at FULL); no accent bleed, rim tint or under-glow | **not built** (B-3); what shows is the old glass |
| Rail, mini player, dock, header, Hub | right-edge rotated tabs, PLAYING bar, transport dock | the existing bottom tab bar and top bar | **not built** (B-2) |
| Tiles, rows, chips, stamps, vertical labels | masonry tiles with frosted frame, list rows with duration chips | the existing cards and rows | **not built** (B-3) |
| Settings rows | not in the mockup | built and tested (Robolectric), **not seen on a device** | built, unverified visually |

## 7. Needs a real phone (in order)

1. Settings, Advanced: turn "New UI (experimental)" on while a song plays; confirm audio, queue and the open player do not blink, then turn it off. (The one thing the design promises that tests can only prove by construction.)
2. With it on: Home, Library and Settings in the new colours and type; flip Accent through the eight presets and a custom colour; confirm text on the accent stays readable.
3. Font scale 200 percent: the Settings rows and the accent sheet (no clipped text, sliders still usable).
4. A phone on Android 12 (or 13), battery saver on, and Reduce transparency on: the bars should drop from live glass to the simpler looks.
5. Safe mode: only checkable by forcing a failed start (not done); the logic is unit-tested, the file wiring is not.
6. Startup time with the new UI on a real device: the emulator ANRs say nothing about a phone, but nobody has measured it there.

## 8. Not done in B-1

Translations of the 29 strings; the full screenshot matrix; a web build; any chrome or component work (B-2, B-3); the light theme (dark only for now); the "Glass tier override" the spec proposed.

---

## 9. Glass kit (G-1): `ui/brutal/glass/`, no screens yet

Built on 2026-11-02 on top of the app's existing glass, for both looks: **GLOW** (the Neon look: accent light inside, accent rim, under-glow) and **NO-GLOW** (white and grey only: the accent never enters the glass).

### 9.1 API

`Modifier.brutalGlass(shape: CornerBasedShape, kind: GlassKind, glow: Boolean = palette.glow, textScrim: Boolean = kind is Floating or Panel, level: GlassLevel = LocalGlassLevel.current)`

| Kind | For | Backdrop at FULL | Notes |
|---|---|---|---|
| `Floating` | bars, round buttons, rail tabs | **live**: the app's own `liquidGlass()` (blur, lens, highlight) | counts against the 3-surface `GlassBudget` |
| `Panel` | dialogs, sheets | **a still of the page**: the app's own panel glass (`rememberPanelGlass`, shared `AppStill`) | the app's panel glass already paints the scrim and tint, so the kit adds only the rest |
| `Tile` | the frame around artwork inside scrolling content | **none, ever** | gradients and strokes only: a frosted frame ring with a dark inner line |
| `Chip` | tiny pills and labels | **none, ever** | tint, rim, small arcs |

Also: `BrutalGlassListScope { ... }` marks a scrolling container (a Floating surface inside it is drawn SIMPLIFIED, never live), `LocalGlassPlanObserver` (tests and the harness can watch each surface's decision), `BrutalGlassGallery` and `BrutalGlassGalleryPair` (the comparison gallery, with nine `@Preview`s: FULL, SIMPLIFIED and SOLID at yellow, SIMPLIFIED and FULL at red and blue, FULL and SIMPLIFIED on a light page, each showing NO-GLOW and GLOW side by side), `BrutalGlassPerfHarness` (see 9.6).

**What it reuses and the one upstream edit.** `liquidGlass()` and `rememberPanelGlass()` are called, not copied, so upstream tuning of the lens, blur, scale or panel constants carries over. `rememberPanelGlass` was `private` in `AppGlass.kt`; it is now `internal` (one word, logged in `UPSTREAM_SYNC.md`). No other existing file was changed for the kit.

### 9.2 The layers (bottom to top), what provides them and what each costs

All new layers are `drawRect` / `drawPath` with brushes, paths and strokes **built once per size** (`drawWithCache`; the `Path`, every `Brush` and every `Stroke` is created in the cache block, none in the draw pass). No `RenderEffect`, no extra graphics layer, nothing allocated per frame. The Glow strength is a `State` read in the draw pass (a change of the setting fades over 150 ms and recomposes nothing).

| # | Layer | Provided by | Draw ops |
|---|---|---|---|
| 0 | **Under-glow** (GLOW): 9 nested strokes outside the shape (4 on tiles), nudged 6 dp down, the shape cut out; summed alpha falls like `0.30 x gain x (1 - d/21dp)^2` | new (`haloSteps`, `drawGlassHalo`) in a modifier *before* the clip, so it can draw outside the surface | 9 `drawPath` + 1 `clipPath` |
| 1 | **Backdrop**: saturate, blur, lens, the app's highlight and shadow | existing `liquidGlass()` (Floating FULL) or `rememberPanelGlass()` (Panel FULL) | one `RenderEffect` chain, as the app already pays |
| 2 | **Body**: black scrim 0.55 behind text, then a white gradient 0.13 to 0.06; without a backdrop (SIMPLIFIED) a dense near-black fill under the gradient | new | 2 to 3 `drawRect` |
| 3 | **Gloss**: a diagonal sheen across the end 45 percent | new | 1 `drawRect` |
| 3b | **Glint**: a bubble highlight in the start cap | new | 1 `drawRect` |
| 4 | **Accent bleed** (GLOW): a linear wash from the bottom edge, a strong hot spot in the start corner, a smaller one in the end corner; peak alpha 0.55 x gain, lowered so white text on it stays 4.5:1 (yellow 0.45), hot spot up to 0.80 and halved on surfaces of 48 dp or less | new | 3 `drawRect` |
| 5 | **Content** | the caller | |
| 6 | **Bevel band** (inner edge gradient, 6 dp on bars, 7 on panels, 4 on tiles), **frame ring** (tiles) | new | 1 to 3 `drawPath` |
| 7 | **Rim** (white, 1.5 dp, brighter at the top), **edge** (a dark outermost line), **rim tint** (GLOW: the accent mixed a quarter toward white, 1.5 dp, brightest at the bottom), **two specular arcs** (top-left 3.5 dp, alpha 1.0; bottom-right 2.5 dp, alpha 0.6; each is a radial-gradient stroke centred on its bounding corner, so it follows the real corner shape and fades along both edges) | new (the arcs and the rim follow the shape's own path, so a continuous corner is followed exactly) | 4 to 6 `drawPath` |

Per surface that is at most 7 `drawRect` + 6 `drawPath` inside the surface (GLOW bar), 4 + 5 without Glow, 7 `drawPath` for a tile frame, plus the halo outside. Measured on a device: **not yet** (9.6).

### 9.3 Levels

| Level | Floating | Panel | Tile | Chip |
|---|---|---|---|---|
| FULL | live backdrop + layers 0 to 7 | page still + layers 0, 3 to 7 | layers (no backdrop) | layers (no backdrop) |
| SIMPLIFIED | dense body + layers 0, 2 to 7: no capture, no lens | same | same as FULL | same as FULL |
| SOLID | opaque `#1D1D1D` + a hairline; GLOW: accent hairline and a static half-strength bleed on bars and panels (never over artwork) | same | hairline only | opaque + hairline |

A request for FULL is lowered when: a Floating surface got no slot from the budget (**the 4th live surface drops to SIMPLIFIED**, asserted in a test), a Floating or Panel surface sits inside `BrutalGlassListScope`, or a Panel has no page still. The decision is a pure function (`effectiveLevel`, `planGlass`) so every combination is unit tested. A budget slot is taken when a surface first composes and returned when it leaves the composition, also if the composition is thrown away unused (`RememberObserver.onAbandoned`).

### 9.4 Shapes

Every shape is a `CornerBasedShape` (the lens throws otherwise). The brutal tokens use `ContinuousCornerShape` (the app's own squircle-like corner, tiles at 19 percent, bars as stadiums). The kit does not approximate it: it takes the shape's real outline as a `Path` once per size, and the bevel, arcs, rim and halo are strokes along that path.

### 9.5 Verification of the kit

- Unit tests (`GlassPlanTest`, `GlassGeometryTest`, `GlassStyleTest`, `GlassDrawCacheTest`): the plan for every kind, level, glow and scrim combination; effective level; corner radii (start/end swap in RTL, capping); arc reach; the halo falls like its stated curve and sums to the total; bleed and gloss geometry; the style numbers (including that white text on the bleed stays readable for all eight presets); the cache builds no accent brush without Glow.
- Robolectric UI tests (`BrutalGlassUiTest`, 15): every kind renders at FULL (with `LocalAppBackdrop` from `rememberLayerBackdrop`), SIMPLIFIED and SOLID without crashing; **SOLID, SIMPLIFIED, tiles and chips never read the backdrop** (the test does not provide one, and reading it throws); the budget gives 3 live slots and drops the 4th to SIMPLIFIED, returns a slot when a surface leaves and hands it to the next; a Floating surface in a list scope is SIMPLIFIED and takes no slot; a Panel without a still is SIMPLIFIED; NO-GLOW surfaces carry no accent layer; clicks still work and the glass adds no label, role, state or action (Compose's own `Shape` clip entry is the only extra, listed in the test).
- Pixel tests (`BrutalGlassRenderTest`, 10, Robolectric native graphics, **no emulator**): the real draw functions are rendered into an offscreen bitmap. Asserted on the pixels: NO-GLOW has **zero** accent-coloured pixels at SIMPLIFIED, SOLID and FULL; GLOW has accent pixels inside the glass and an under-glow below it, strongest at the edge and back to the page at 30 dp; SOLID with Glow has the hairline and no under-glow; Glow on and off differ in every gallery.
- **Results (2026-11-02, through `scripts/`):** `build-active.ps1 -Target phone` BUILD SUCCESSFUL (1m 59s); `build-active.ps1 -Target tv` BUILD SUCCESSFUL (1m 9s); `test-active.ps1 -Target phone`: `:app:testDevDebugUnitTest` and `:shared:jvmTest` both passed. Report files read afterwards: **app 1,440 tests, 0 failures, 0 errors, 1 skipped** (`GeniusTest`, not ours), of which **65 are the glass kit's** (unit 40, Robolectric UI 15, pixel 10) and 89 are the B-1 plumbing tests; **shared 234 tests, 0 failures**. Web: not run (no web file changed). Wear OS and desktop: dormant, not checked.

### 9.6 Performance: NOT measured

The brief asked for `FrameStats` numbers (p95 at most 12 ms at 60 Hz) for 3 live Floating surfaces over a list of 30 Tiles, on a reference phone. **That was not done in this phase**: no emulator or adb was used (the owner's instruction), and nothing else here can produce frame times. What exists instead:
- `BrutalGlassPerfHarness(liveSurfaces = 3, tiles = 30)`: exactly that screen, scrolling itself up and down, with the list inside `BrutalGlassListScope`. It is not reachable from the app.
- To measure: put it behind a debug-only entry on the reference phone, install a dev debug build, run `adb logcat -s VioFrames` for a minute (the app's `FrameStats` prints average, p95, p99 and jank every 5 s), repeat with Glow off and with `Reduce transparency` on, and record the phone model and Android version with the numbers. Emulator numbers must be labelled as such.
- What is known by design, not measured: the new layers are cached draw calls (counts in 9.2); the only RenderEffect cost is the app's own `liquidGlass` (one chain per live surface, at most 3); tiles and chips never sample; the budget and the list guard cap the live surfaces.

### 9.7 Comparison with the mockups

Compared on rendered pictures (`brutal-ui/screens/glass-*.png`, drawn with the real kit code at density 2.625, labelled in section 9.8). The mockups are the owner's attachments and the crops measured for `BRUTAL_UI_SPEC.md`.

| Aspect | Mockup | Kit | Verdict |
|---|---|---|---|
| Glass body | near-black `#1D1D1D` (white 0.09 over the page) | `#1D1D1D` at SOLID; the SIMPLIFIED body is tuned to land on it (white 0.13 to 0.06 over a near-black fill) | matches in tone (checked by eye on the renders, not sampled) |
| Thick bevel band | a visible bright inner band, brightest top-left | 6 dp band, top 0.45 to bottom 0.20 | matches in character; approximate in width and falloff |
| Specular arcs | bright top-left, softer bottom-right | radial strokes at each bounding corner, 3.5 dp at 1.0 and 2.5 dp at 0.6 | matches in character; approximate in length (arc reach is 3.2 corner radii) |
| Gloss sheen | a diagonal light streak across the end of each bar | a linear sheen across the end 45 percent, alpha 0.24 (0.18 GLOW) | approximate: softer and straighter than the mockup's curved streak |
| Bubble highlight in the start cap | a crescent along the left cap | a round glint | approximate (round, not a crescent) |
| Accent bleed (GLOW) | a bright yellow swath from the bottom-left with a sharp diagonal edge | a soft linear and radial wash with a strong hot spot in the corner | approximate: the colours match the measured olive (`#887623`) in the middle and the bright core at the corner; the diagonal terminator is soft, not sharp |
| Rim tint (GLOW) | yellow, brightest along the bottom | accent mixed a quarter toward white, brightest at the bottom | matches |
| Under-glow (GLOW) | a soft yellow bloom below the surfaces | nested strokes, falls like the measured tail (0.25, 0.19, 0.17 at 13, 21, 28 px) | matches in strength; **stepped** (9 bands) instead of a true blur, visible only when zoomed |
| NO-GLOW | clear glass, white rims | white and grey only | **deviation by instruction**: image 1 also has a small yellow wash in the lower corner; the brief says no accent in NO-GLOW, so it is omitted |
| Refraction at the edges, blur of what is behind | strong lens and blur | the app's `liquidGlass()` at FULL (lens 24 dp x 24 dp, blur 8 dp) | **not seen**: needs a GPU, was never rendered in this phase; the mockup's bevel band is nearer 10 dp than the 24 dp lens height the app uses |
| Squircle corners | continuous corners | `ContinuousCornerShape` outline | matches (the kit follows the real path) |
| Round buttons and rail tabs | bubbles with a highlight and a tinted rim | stadiums with glint, bevel, arcs and a half-strength bleed | approximate |
| Tiles | frosted frame around artwork | 5 dp ring, dark inner line, rim, arcs | approximate: the mockup's frame refracts the artwork; this one is gradients |

### 9.8 What the pictures are and are not

`brutal-ui/screens/glass-*.png` come from the pixel tests: real Skia drawing of the kit's draw functions on the JVM. They show every layer at SIMPLIFIED and SOLID, and the layers of FULL **without its backdrop** (`...-full-nobackdrop`). They are **not** screenshots of a phone or an emulator, show no text (the gallery draws surfaces only), and say nothing about FULL's blur and lens. The Home screenshots from B-1 are the only emulator pictures in that folder.

### 9.9 Deviations and limits

1. Performance numbers were not produced (9.6).
2. FULL's live backdrop was never rendered; only its decisions and its absence of crashes (Robolectric legacy graphics) were tested.
3. The under-glow is nested strokes, not a blur or a `Shadow`: cheaper, slightly stepped.
4. Glow off removes the accent layers at once; only Glow on fades in (150 ms).
5. `liquidGlass()` fixes its lens at 24 dp x 24 dp and its blur at 8 dp; the mockup's bevel suggests a smaller lens. Left as is so the app's glass fixes carry over; a defaulted parameter on `liquidGlass()` would be the upstream-friendly change if the look needs it on a device.
6. A Panel at FULL in the preview gallery falls to SIMPLIFIED (no page still is provided there), as it would anywhere without one.
7. The previews compile but were not opened in Android Studio; the same gallery is what the pixel tests render.

---

## 10. Components (C-1): `ui/brutal/components/`, no screens yet

Built on 2026-11-02 on the glass kit (section 9). Every component of the mockup that is not a glass surface in itself, plus the composites. All text is in `sp`, nothing sits in a fixed-height box, every control has a 48 dp target, a label and a role, and a focus ring in the accent.

### 10.1 What exists

| Component | File | What it is | Accessibility |
|---|---|---|---|
| `StickerLabel` | `StickerLabel.kt` | solid accent plate, about -12 degrees (token `BrutalAngles.STICKER`), heavy italic caps rows (the first row Archivo Black, later rows a lighter Figtree 800), 2 dp ink rules, a last row with a label and a mono value ("VERSION 0.4") | **one heading node** read as one phrase ("About, Privacy, Version 0.4"); rows and tilt are hidden from TalkBack. The tilt is a static `graphicsLayer`. At 200 percent font the plate wraps and grows taller, never clips (tested) |
| `Wordmark`, `ExperimentalTag`, `WordmarkMasthead` | `Wordmark.kt` | "V I O L E N C E" at 1.3 em tracking, optically centred; the tag (text from a string, tilted -4 degrees) shows while `BrutalFlags.EXPERIMENTAL`; the masthead is the tilted (-8.5 degrees) glass tile around it, drawn without a live backdrop | one heading node "Violence, Experimental"; the word is a Latin brand name in every language and, as a logo, exempt from font scaling |
| `VerticalLabel` | `Labels.kt`, `RotatedLayout.kt` | text turned -90 degrees **with its layout size swapped** (`verticalText()`): the parent measures a lane as wide as one line is tall and as tall as the text is long; one line, ellipsised at the incoming height | an ordinary text node, read once; at 200 percent the lane widens (tested) |
| `DateStamp` | `Labels.kt` | accent mono `dd mm yy`, turned bottom to top | the date is spoken in words in the device language ("November 28, 2022"), not as digits |
| `DurationChip` | `Labels.kt` | accent pill with the length (`03:45`, `1:02:03`), at least 20 dp, grows with the font | read as "3 minutes 45 seconds" (string resources, 17 languages once translated); not a separate stop inside a row |
| `PlayButton` | `ArtTile.kt` | solid accent disc, on-accent triangle, optional glow, drawn at 28 dp | **48 dp touch target** even when drawn smaller (tested at 20 dp); "Play <title>" |
| `ArtTile` | `ArtTile.kt` | artwork in the glass kit's **Tile** frame (never a live backdrop), aspect clamped to 1.0 to 1.8, Coil `AsyncImage` through the app's loader and cache (or an `artwork` slot for previews and tests), the play button at the bottom end | the tile is a button "Open <title>" with a long-press hook; the play button is a **sibling**, not a child (`clickable` merges children into one node), so tile then play button are two stops (tested) |
| `LabeledArtTile` | `ArtTile.kt` | the tile with its lane: date above title, turned bottom to top along the start edge, bottom aligned, as tall as the tile | the lane repeats what the tile says, so it is hidden from TalkBack; pass the spoken date as the tile's subtitle |
| `TrackRow`, `OverflowButton` | `TrackRow.kt` | title (white, up to two lines), artist (accent), duration chip, rounded-square thumbnail at the end, a visible 48 dp "more options" button | the row is one phrase ("Too Young, Post Malone, 3 minutes 45 seconds"), a button "Open <title>", long-press hook, the menu also as a TalkBack custom action; the overflow button is a sibling; at least 72 dp, grows with the font |
| `GenreRailTab` | `RailControls.kt` | vertical glass pill, label turned bottom to top; selected = solid accent plate with on-accent text and the accent under-glow | role Tab, selected state, position (`CollectionItemInfo`, "tab 2 of 4"); 48 dp wide, at least 48 dp tall and as tall as its label needs; no live backdrop |
| `RoundGlassButton` | `RailControls.kt` | round (or tall, for search) glass button with an accent icon | button, label, optional state ("expanded"); 48 x 48 dp (72 dp tall for search) |
| `BrutalButton` | `RailControls.kt` | solid accent stadium button | button, 48 dp at least, grows with the text |
| `MasonryColumns` | `MasonryColumns.kt` | the broken grid (10.3) | stable keys; collection semantics come from the lazy grid |
| `BrutalSkeleton`, `BrutalSkeletonTile`, `BrutalSkeletonRow`, `BrutalLoadingGroup` | `States.kt` | glass-body fill with a static diagonal hatch; no shimmer (nothing animates while idle) | the group says "Loading" once as a polite live region; single skeletons are silent |
| `BrutalEmptyState`, `BrutalErrorState` | `States.kt` | the sticker as the title, a line of text, an accent button | the error is an assertive live region with a retry button |
| `brutalClickable`, `brutalSelectable`, `Modifier.brutalHalo` | `BrutalInteraction.kt`, glass kit | press (slightly less opaque, no ripple), the accent focus ring, roles and labels; the optional `interactionSource` lets a caller observe or drive focus | the focus ring is drawn in the accent-text colour (the lightened accent), so it keeps 3:1 even on the brightest glass |
| `brutalContrastPairs` | `ComponentContrast.kt` | every foreground and background pair the components draw (sticker, chip, tab, button, artist, date, titles, muted text, icons, focus ring, text over the bleed) | used by the contrast tests |
| `BrutalComponentsGallery`, `BrutalPreviewHost` | `BrutalComponentsGallery.kt` | every component on one page with ten `@Preview`s: yellow glow and no-glow at 1x and 2x font, red and blue, light page at 1x and 2x | |

### 10.2 Layout rules that make them safe

- **Rotation takes part in layout** (`rotatedLayout`, `verticalText`). `rotate` and `graphicsLayer` alone only move pixels, so the parent measures the unrotated box and a tilted sticker clips. These two modifiers report the rotated size. A child may be measured only once in Compose, so `rotatedLayout` finds the width with intrinsic measurements and measures once (the first version measured twice and Compose threw: caught by the tests).
- **Display text** (sticker rows) is drawn at a font scale capped at 1.3 (spec 5.2); the sticker still grows because lines wrap. Everything else scales fully. The wordmark is a logo and does not scale.
- **No fixed-height box holds text.** Rows, chips and pills use minimum heights; the thumbnail (a square, not text) is the only fixed size.
- **Non-Latin languages** get no italic and no uppercase (the `ScriptProfile` rule of section 3.4).

### 10.3 Masonry: approach and limits

One `LazyVerticalStaggeredGrid` (platform staggered layout, foundation 1.10): items go to the shortest column, heights come from each tile's aspect (1.0 to 1.8), only visible items are composed, items have stable keys, one scroll container (no nested scrolling). The "broken" part is cosmetic and bounded: each item is nudged by a deterministic offset from its key (at most 6 dp vertically, 3 dp sideways, a `graphicsLayer` translation, so layout, scrolling and keys are unaffected) and the first item of every odd column starts 24 dp lower. **Limits:** the overlap is drawn, not laid out (tiles can kiss, not cover each other); heights come from aspect ratios, not content; items cannot span columns; the column count is fixed per call; the label lane takes width and tiles narrow at large fonts; no sticky headers; the grid clips to its bounds, so the content padding must stay at 24 dp or more for the 21 dp tile glow to show.

### 10.4 Contrast

`ComponentContrastTest` runs all 15 pairs for every one of the eight presets with Glow on and off, for 3,000 random custom colours, and for ten edge colours (black, white, mid grey, the page colour, the pure primaries, the ink and the two glass greys). Text pairs must reach 4.5:1 and icon, stroke and focus pairs 3:1. They all do: the accent derivation (spec 3.2) repairs whatever the user picks. While listing the pairs I noticed that the raw blue accent (`#2F5BFF`) is only 2.70:1 on the brightest glass, below the 3:1 a focus ring needs, so the ring is drawn in the lightened accent-text colour instead (the test then passes for every colour).

### 10.5 How it was checked (no emulator)

- **JVM renders.** The whole gallery is drawn to PNG on the JVM with Robolectric native graphics (a real Compose layout and Skia drawing, with the bundled fonts); `captureToImage` times out there, so the test draws the activity's decor view into a bitmap (`JvmRender.kt`). The ten renders in `brutal-ui/screens/components-*.png` are those, at 360 dp wide: yellow glow and no-glow at 1x and 2x font, red glow 1x, blue glow 1x, blue no-glow 2x, yellow glow and no-glow on a light page, red no-glow on a light page at 2x. `focus-ring.png` shows a focused control. They are **not** emulator or device screenshots.
- Pixel assertions: the accent plate really is drawn in the derived fill colour for every render; a focused control really shows its ring and loses it again.
- Layout and semantics tests (Robolectric, native graphics): one node and a heading for the sticker, text not separately reachable, the plate never wider than its window at 200 percent and taller there, the vertical label swapping its size and ellipsising at its height, the lane widening at 200 percent, 48 dp targets (play at a 20 dp visual, overflow, rail, round buttons), tile and play button as two controls with clicks reaching the right one, aspect clamping, long-press and overflow hooks, the TalkBack custom action, selected tabs with their role and position, the masonry in two columns with the second lower, loading announced once, the error state assertive with a working retry.

### 10.6 Comparison with the mockups

Compared on the JVM renders against the crops used for `BRUTAL_UI_SPEC.md`.

| Aspect | Mockup | Built | Verdict |
|---|---|---|---|
| Sticker | yellow plate about -12 degrees, bleeding off the top-start corner, heavy caps, black rules, a small version row | the same, with italic caps (the brief's "heavy italic"), the second row lighter, "0.4" in mono | matches in character; the font is Archivo Black, a wider face than the mockup's (see spec 4.2); the mockup's perspective skew is not reproduced |
| Wordmark and tag | very wide tracking, white, in a tilted glass tile, yellow tilted tag | the same, tag from a string | matches; the glass is the SIMPLIFIED look here (no refraction, section 9.7) |
| Vertical labels and date stamps | yellow condensed dates and white titles along the tile edge | accent mono dates (as the brief says; the spec measured a condensed face) and white Figtree titles, date above title, bottom aligned | approximate: the mono digits are wider than the mockup's condensed ones |
| Tiles | glass frame, artwork, round yellow play button at the bottom end | the Tile frame (ring, dark inner line, rim, arcs), accent play disc with a glow | matches in character; the mockup's frame refracts the artwork, this one is gradients |
| Rail | glass pills with rotated labels, the selected one solid yellow, round collapse and search buttons | the same, no live backdrop | matches; labels are Home, Explore, Library, Search in the gallery |
| List rows | white title, yellow artist, yellow chip, thumbnail at the end | the same, plus a visible overflow button the mockup does not have | matches; the overflow button is a deliberate addition (a long-press needs a visible equivalent) |
| Broken grid | tiles of different heights, offset columns, overlaps | staggered columns, a 24 dp lead on the odd column, up to 6 dp of overlap | approximate: the mockup overlaps more and with more rotation |
| Skeleton, empty, error | not in the mockup | designed in the same language | n/a |
| NO-GLOW | clear glass | no accent in glass; accent still appears in plates, text and icons | by design |

**Not built here** (section 6 chrome and the player are the next phase): the PLAYING and PAUSE bars and the transport bar, the "Components" oval and the hand-drawn arrow of the mockup (illustration, not a component), the rail and dock as screens, and any real page.

### 10.7 Deviations and limits

1. The sticker angle is the spec's measured -12 degrees, not the brief's "about -10".
2. The date stamp is mono as the brief says; the spec preferred a condensed face (Archivo Narrow, bundled, used by the `stamp` style but not by `DateStamp`).
3. Italic is synthesised by Compose (no italic font file is bundled), Latin only.
4. The wordmark ignores the font scale (it is a logo); the sticker caps at 1.3 but grows by wrapping.
5. A long-press is the "menu hook" and always paired with a visible overflow button on rows; tiles take the hook but have no overflow button of their own (a tile is not a row: add one in the screen if a tile needs it).
6. The skeleton does not shimmer: no continuous animation while idle is a rule of this look.
7. Whether rotating right-to-left text by -90 degrees reads well is UNCLEAR until checked in Hebrew on a device.
8. **At 200 percent font the rail does not fit its container** (the gallery draws it in a 560 dp box and the lower tabs are cut off in `components-*-2x.png`). That is the case the spec gives to the rail-to-dock fallback (section 6.9: font scale of 1.5 or more, or not enough height): it is chrome, so it is not built here. The tabs and buttons themselves are never clipped by their own size.
9. On a short tile at a large font the date stamp (eight characters) can take the whole lane; it then ellipsises and the title is dropped from the lane (TalkBack still hears both through the tile).
10. Previews compile but were not opened in Android Studio; the JVM renders are the visual check. Nothing here was run on an emulator or a phone.

### 10.8 Results (2026-11-03, on the final code, through `scripts/`)

`build-active.ps1 -Target phone` BUILD SUCCESSFUL (1m 15s); `build-active.ps1 -Target tv` BUILD SUCCESSFUL (43s); `test-active.ps1 -Target phone`: `:app:testDevDebugUnitTest` and `:shared:jvmTest` both passed. Report files read afterwards: **app 1,508 tests, 0 failures, 0 errors, 1 skipped** (`GeniusTest`, not ours), of which **68 are the components'** (logic and contrast 23, Robolectric structure 33, JVM pixel renders 11, the render smoke test 1); **shared 234 tests, 0 failures**. `LocaleStringsTest` passed with the 17 new strings in all locales. Web: not run (no web file changed). Wear OS and desktop: dormant, not checked.

How it got here, so the numbers are not read as first-try: the first run threw "measure() may not be called multiple times" (a real bug in `rotatedLayout` and `LabeledArtTile`, fixed by predicting sizes with intrinsics and measuring once); the masonry showed no tiles until the lane width came from the type styles instead of an intrinsic query through the turned text; the legacy Robolectric graphics gave fake text metrics and no path hit-testing, so the structure tests run in native graphics mode; one test expectation (rail tab width at 200 percent) was wrong and was corrected, not the component.

## 11. Navigation and two pages (N-1): `ui/brutal/chrome/`, visible behind the New UI switch

### 11.1 What exists
- `BrutalChrome` (rail, PLAYING bar, Hub), `BrutalHomePage`, `BrutalLibraryPage`, and the pure rules in `ChromeModel.kt` (`tilesFor`, `tileAspect`, `masonryColumnsFor`, `nearEnd`, `railNeedsDock`, `HubTarget`).
- **Rail** on the right edge (left by the `brutalRailSide` setting, mirrored in RTL because it follows start/end): collapse chevron, a tall search button, rotated tabs Home, Explore, Library, and an Apps button for the Hub. The search tab is the search button, so the rail draws three rotated tabs, not four (the spec's open question about the duplicated search is settled this way until the owner says otherwise). Collapsing leaves only the chevron. The rail scrolls when it is taller than the screen.
- **PLAYING bar** at the foot: art, PLAYING tag, title, artist, round accent play/pause. The bar opens the player; play/pause is a sibling control. Its measured height sets the rail's bottom clearance, so a taller bar (large font) pushes the rail up instead of covering it.
- **Hub**: a glass panel over a scrim, listing Settings, Downloads, History, Recently played, Replay, Moods and genres, Equalizer, Sources, Listen together, Friends, Profile. A row opens its route (`navigator.push`) and closes the panel; back or a tap outside closes it.
- **Home** and **Library** pages: the masthead and a sticker heading, then the feed or the account's shelves flattened into the broken masonry grid (`MasonryColumns` got an optional full-width `header`). Loading, error (with retry, in place of the masthead so it is on screen) and empty states use the component kit. Library adds New playlist and the folder links (downloads, local files) and, signed out, a sign-in prompt. Pull to refresh and load-more (Home) are kept.
- Wiring: four edits in `MainActivity.kt` inside `BitChordApp` (listed in `UPSTREAM_SYNC.md`). With the switch off none of this code runs and the old bars and pages are exactly as before.

### 11.2 Deviations and limits (read before judging the screens)
1. **Only Home and Library are redrawn.** Explore, Search, album/artist pages, the queue, Settings and every route keep their current layouts, in the new colours and fonts, with the rail and PLAYING bar drawn over them. The rail therefore overlaps the right edge of those pages; the collapse button is the way to clear it.
2. **Home lost the lead cards, Top Picks, Find Your Mood and the recents rows** on the new look (all still on the current UI). They need their own designs. Replay's entry card is not on Library either; Replay is in the Hub.
3. **No dock.** The spec's dock while the player is expanded, and the rail-to-dock fallback at a font scale of 1.5 and up, are not built: the player is a separate window, and at large fonts the rail scrolls instead. `railNeedsDock` only records the rule.
4. **The classic top bar still draws** (avatar, download button) above the new pages; the new masthead sits under it as the first item of the grid. The new header (masthead with the avatar) is not built.
5. **Scroll position is not kept across tab switches** on the two new pages (the grid state lives in the page; the classic pages hoist theirs).
6. **Tile heights are a repeating pattern** (`tileAspect`), not the artwork's own shape; 'play' on a tile is shown for tracks only.
7. **The masthead is still a little wider than the lane beside the rail** on a 360 dp screen: its right end meets the collapse button (the button is on top). The render is in `brutal-ui/screens/chrome-home-rail-right-1x.png`.
8. A faint rectangular shade is visible behind the rail in the 1x and 2x renders where the page's own glow is clipped by the rail's scroll area; not yet traced.
9. Dark theme only, as before. Strings are English in all 20 locales (27 new keys).

### 11.3 How it was checked (no emulator, no adb)
JVM only. 27 new tests: 10 plain logic (tile keys unique and stable, caps, aspects, columns, load-more rule, Hub list) and 17 Robolectric native (rail tabs and selection, tab and search taps, collapse and restore, play/pause and expand, no bar when nothing plays, Hub opens a route and closes, Home tiles and shelf name on tap, loading, error with retry, Library signed out, and four PNG renders). Renders are in `fork-docs/brutal-ui/screens/chrome-*.png`: **JVM renders, not device screenshots**; tiles are empty because no artwork is loaded, and the glass is SIMPLIFIED (the live FULL backdrop is still never rendered).

### 11.4 Results (2026-11-03, through `scripts/`)
`build-active.ps1 -Target all`: phone, TV, web typecheck and web site build all OK. `test-active.ps1 -Target phone`: app 1,535 tests, 0 failures, 1 skipped (`GeniusTest`, not ours); shared passed. First run of the new UI tests had 5 failures, all fixed: rail labels are upper-cased by design (test looked for the mixed case), tiles are read by their spoken description, and an error state sat below the masthead where a small screen could not show it (now it replaces the masthead). The renders then showed three real defects, also fixed: the rail's glow cut off in a rectangle, the masthead running under the rail's buttons, and the taller PLAYING bar covering the rail at 200 percent font.

### 11.5 Test first on a phone
Settings, Advanced, New UI (experimental) on. Then: Home pulls and scrolls; a tile plays; the rail tabs change page; the Apps button opens the Hub and Settings opens from it; collapse and the left/right setting; the PLAYING bar's play/pause and its tap to open the player; the same at 200 percent font and in Hebrew.
