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
