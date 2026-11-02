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

29 `brutal_*` keys with **English placeholders** in all 20 `strings.xml` files (default plus 19 locale folders, including the 17 supported languages and the `zh-rHK` and `b+zh+Hant` extras). **Translations are not done**: those rows read in English in every language until a translation pass. `LocaleStringsTest` guards that no locale misses a key.

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
