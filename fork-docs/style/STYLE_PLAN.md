# Violence Style: implementation plan (S0 to S6)

Violence Style is a **skin** on top of the existing UI, switched by one toggle. It keeps every layout, route, screen and feature and changes only typography, a live background, decorative elements, glass buttons and bars, and accent colouring. Toggle OFF = the app exactly as today. Spec: `STYLE_SPEC.md`. Element map: `STYLE_MAPPING.md`. Audit: `STYLE_AUDIT.md`. Owner choices: `STYLE_DECISIONS.md`. Gates: `STYLE_APPROVALS.md`.

Phone app only. TV, web, Wear OS and desktop are out of scope (Wear OS and desktop: dormant, not touched).

## Owner decisions applied (confirmed 2026-11-03; they override the spec and the settings table below)

- **Heading font:** a Settings toggle between **Bodoni Moda** and **Cormorant Garamond**, both Regular 400; both bundled. Body: Inter.
- **Backgrounds:** **Corner glow, Aurora, Haze** (plus Off). **Arcs is dropped** for now (its construction stays in the spec for later); the ref1 streak terminator goes with it, so the hero heading always uses the dot. Default: **Aurora**, selectable in Settings.
- **Defaults** (all changeable in Settings): intensity Medium, accent From song, motion Normal, custom accent Blue, heading style Mixed, card labels sentence case.
- **Light theme:** a light version is for later. Until then the skin shows its dark look in any theme (spec section 9, option A).
- **Toggle location:** "Advanced & experimental options".
- **Top-left mark:** the existing "VIOLENCE" wordmark (`HomeTitle`) restyled; no new V asset.
- **Album, artist and playlist pages:** the glow shows above the artwork, with its own on/off toggle (and the master toggle).
- **Reduce transparency:** follows the in-app "Reduce dynamic blur".
- **MUST match:** glow colours and placement, heading look, italic "for You", card corner radius, first-card rim, row thumbnail, mini player, nav pill and capsule, top-right pill, heading and gutter margins. **FLEX:** hairline terminator, hero card width.

## Global rules for every stage

- Branch `violence-nk` only (CLAUDE.md wins over the brief's "or main"). `git branch --show-current` before starting and before every push; `git pull` (merge, no rebase) first; stop on uncommitted changes you did not make. Never force-push, never push to `main`, `violence-main` or `upstream`.
- **Backup tag:** each stage starts with `git tag pre-skin-<stage>` (for example `pre-skin-s2`) on the current head and `git push origin pre-skin-<stage>`. A tag, never a `v*.*.*` name.
- **Approval gate:** a stage may start only when `STYLE_APPROVALS.md` has the previous stage ticked **and** its "OFF-parity verified on device" line ticked, and `STYLE_DECISIONS.md` is filled in. Otherwise stop and say which box is empty.
- **Verification:** explicit module paths only. Phone: `.\scripts\build-active.ps1 -Target phone` and `.\scripts\test-active.ps1 -Target phone`. **Also build TV** (`-Target tv`): `:tv` compiles `app/src/main/java`, so a new file there must compile for TV. No root-level Gradle task. Never claim a build or test passed without its output. Wear OS and desktop: dormant, not checked.
- **Shared-code edits** (`:sharedUi`) are allowed only for the guards listed in `STYLE_MAPPING.md`; append a line to "Possible impact from later changes" in `FROZEN_TARGETS.md` for each; never touch `desktopApp`, `desktopUi` or `wearos`.
- Free only: new fonts are OFL files bundled as resources; no new dependency is planned. No secrets, no network call, no analytics.
- New strings are added to `values/strings.xml` **and all 17 locale files** (`LocaleStringsTest` fails otherwise); English placeholders are acceptable and are listed as a known gap.
- No frame-time claims without an on-device readout (no emulator or adb on the owner's PC).

## Architecture

1. **One style state.** `ViolenceStyleState` (immutable data class: `enabled`, `background`, `motion`, `intensity`, `accentSource`, `accent: Color`, `lite: Boolean`, `headingStyle`) read through `LocalViolenceStyle` (a `staticCompositionLocalOf` whose default is the OFF state). Built in one place from `AppSettings` flows plus `StylePerformance` (battery saver, low RAM, thermal, foreground, reduce motion) and the accent engine, and provided once in `MainActivity.setContent` around `BitChordApp`. Components never read settings directly.
2. **Style extensions return their input unchanged when off.** The family is `Modifier.violenceGlassRim(style, ...)`, `violenceCardRim`, `violenceGlassPill`, `violenceHairline`... Each takes the state as an argument (`fun Modifier.violenceX(style: ViolenceStyleState, ...): Modifier = if (!style.enabled) this else ...`), so it is a plain function and unit-testable on the JVM without Compose. Composable wrappers (`ViolenceSectionHeader`, `ViolenceBackground`, `ViolenceCapsule`) are only reached through an `if (style.enabled)` at the call site; the `else` branch is the untouched original code.
3. **Shared-UI hooks.** `:sharedUi` cannot import `:app`. It gets one new file, `ViolenceStyleHooks` (an interface with a default OFF implementation in a `staticCompositionLocalOf`), that `:app` implements. The three shared screens call it under an `if`.
4. **New code in new files.** Package `com.music.bitchord.ui.violencestyle` in `:app` (state, accent engine, background, fonts, settings UI, safe mode, performance); `:sharedUi` gets the one hooks file. Tests in `app/src/test/.../violencestyle/`.
5. **Minimal edited upstream files** (target): `MainActivity.kt` (provider + background layer + safe-mode hook, about 20 lines in three places), `AppSettings.kt` (append-only keys), `SettingsSheet.kt` (one call), `HomeScreen.kt`, `TopPicksShelf.kt`, `SearchScreen.kt` (guards), `GlassNavBar.kt` and `MiniPlayer.kt` and `FrostedTopBar.kt` (modifier and tint call sites), 18 `strings.xml`. Nothing else.

## Settings (new, all under one group "Violence Style")

| Setting | Values | Default |
|---|---|---|
| Violence Style (master) | On / Off | **Off** |
| Background | Corner glow, Aurora, Haze, Arcs, Off | per `STYLE_DECISIONS.md` |
| Motion | Off, Subtle, Normal | per decisions (proposed Subtle) |
| Glow intensity | Low, Medium, High | per decisions (proposed Medium) |
| Accent source | From song, From device, Custom | per decisions (proposed From song) |
| Custom accent | presets Blue, Red, Pink, Green, Orange, Yellow, Cyan, Purple, White + picker with contrast guard | Blue |
| Heading style | Mixed (caps + italic), All caps | Mixed |
| Reduce transparency | follows in-app "Reduce dynamic blur" (no public system setting found; confirm in S1) | follows |

The group lives in `ADVANCED` or `APPEARANCE` (there is no "Experimental" tab; see the audit). It also shows a read-only line "Mode: Full / Lite (reason)".

## OFF-parity guarantee

Definition: with the master toggle OFF, the composition tree, modifiers, colours, fonts, sizes and animations are identical to the build before the skin; no extra coroutine, no clock, no `collectAsState` of a skin flow in a hot path (the provider reads one `StateFlow<Boolean>`; when false it provides the default OFF state and starts nothing).

Tests (JVM, `:app:testDevDebugUnitTest` and `:sharedUi` jvm tests where possible):
- every `violenceX` modifier extension: `assertSame(Modifier, Modifier.violenceX(OFF, ...))` and the same for every argument combination used;
- the accent engine and contrast guard as pure functions (clamp ranges, gray hold, White preset, composited-luminance limit over the grid for all five styles x three intensities x all presets: this test is what stops a glow from overwhelming the text);
- settings defaults: master false, parsing of unknown stored values falls back to defaults;
- safe mode counter logic;
- background geometry: blob centres stay inside `[-0.3w, 1.3w]` for every time sample (no runaway drift).
- Screenshot parity (OFF build vs pre-skin build): only if Robolectric offscreen rendering is available in `:app` (UNCLEAR, checked in S1). Otherwise the owner's on-device check is the parity proof, which is why "OFF-parity verified on device" is its own approval line.

## Safe mode

At the start of `MainActivity.onCreate`, before any skin code: if the skin is on, write `skin_startup_pending = skin_startup_pending + 1` with `commit()` (synchronous: `apply()` can be lost in a crash). When the first frame has been drawn and 5 s have passed, write 0. At the next start, if `skin_startup_pending >= 2`, force the master toggle OFF, store `skin_safe_mode_notice = true`, and show a one-time message ("Violence Style was turned off because the app failed to start twice. You can turn it on again in Settings."). The counter never touches non-skin code paths and does nothing when the skin is off.

## Stages

### S1 Foundation
- **Scope:** the master toggle and settings group (strings in all locales), `ViolenceStyleState` and its provider, accent engine (three sources, clamp, hold rule), font files (display serif chosen in `STYLE_DECISIONS.md` and Inter, OFL, TTF, with licence texts in `NOTICE`/`fork-docs`), dark-only theme wrapper, safe mode, a **style preview card** inside the settings group (a small static composable showing a mini background, heading, hairline, capsule and glass chip in the current settings, using no page code), the `ViolenceStyleHooks` file, the OFF-parity test scaffolding. No visible change in the app itself except the settings group and preview.
- **Files:** new package; `MainActivity.kt` (provider and safe-mode hook), `AppSettings.kt`, `SettingsSheet.kt`, `strings.xml` x18, `res/font`, `NOTICE`, tests.
- **Risks:** `MainActivity` conflicts; TV compile of new files; locale test; font licence texts; Android has no public reduce-transparency setting (confirm).
- **Done when:** toggle persists; OFF changes nothing (tests green); preview card renders; safe mode unit-tested; phone and TV debug builds and phone tests pass (output shown); docs updated.
- **Owner checklist (device):** open Settings, find the Violence Style group; toggle on and off and restart the app; change background, motion, intensity and accent in the preview and see it update; pick each preset and the picker; check the preview text is readable at the biggest font size; with the toggle OFF, browse Home, Library, Search, the player and Settings and compare with how they looked yesterday (nothing changed); tick "OFF-parity verified on device" for S1.

### S2 Live background
- **Scope:** `ViolenceBackground` (five styles, motion, accent crossfade) drawn at the root, inside `paintBackdrop` so glass samples it, and in the root box when glass is off; `StylePerformance` (foreground, reduce motion, battery saver, low RAM, thermal, idle, player covering); LITE mode; contrast-guard wiring; the exclusion of Replay, onboarding and Spotify canvas auth; transparent Search headers are **not** in this stage.
- **Files:** `MainActivity.kt` (the layer, about 10 lines), new files, tests (geometry, guard).
- **Risks:** double cost with glass (the layer is recorded and drawn); battery; light theme (forced dark); hiding behind detail pages.
- **Done when:** all five styles render in the preview and the app; no clock runs when off, backgrounded, idle or reduced; guard tests pass; builds and tests green.
- **Owner checklist:** each style and intensity on Home, Explore, Library, Search, Settings; with glass on and off; scroll for a minute (no stutter you can feel; report if you do); change song and watch the glow change colour smoothly; skip fast (no flicker); battery saver on: the background goes static and a "Lite" note shows; turn Reduce dynamic blur on; leave the app for a minute and return; OFF-parity tick.

### S3 Typography, headings, hero cards, rows, top bar
- **Scope:** display serif headings with the hairline and terminator (`ViolenceSectionHeader`), hero card shape, rim, scrim and captions, list rows (art corner, type), the V mark, the top-right glass pill, Inter body type through a nested typography.
- **Files:** `HomeScreen.kt`, `TopPicksShelf.kt` (shared, guards only), `FrostedTopBar.kt`, new files, `FROZEN_TARGETS.md` lines.
- **Risks:** highest merge-conflict stage (shared files); 200 percent font; RTL; long titles.
- **Done when:** with the style on, Home matches the mockup structure; OFF-parity tests still green; builds and tests green.
- **Owner checklist:** compare Home against all four mockups; set the font size to the largest and open Home, Explore, Library; try Hebrew; check "Show all" still works; long-press cards and rows still work; tap everything once; OFF-parity tick.

### S4 Glass buttons, nav, mini player, accent states
- **Scope:** accent capsule on the selected tab, rims and tint on the nav pill, the search circle, the mini player and the round top buttons, 48 dp hit targets, the scroll-under fade, pressed states.
- **Files:** `GlassNavBar.kt`, `FloatingBottomBar.kt`, `MiniPlayer.kt`, `FrostedTopBar.kt`, `LiquidGlass.kt` only if `surfaceTint` is not enough; new files.
- **Risks:** vendored `floatingtabbar`; frame time with six glass surfaces; light/dark contrast on glass.
- **Done when:** the bar behaves exactly as today (fold, long-press share, pull-up player) with the style on and off; builds and tests green.
- **Owner checklist:** switch tabs; scroll to fold and unfold the bar; pull the mini player up; long-press it to share; with glass on, glass off, and Reduce dynamic blur; each accent preset (is the selected tab readable?); OFF-parity tick.

### S5 Rollout to remaining screens, dialogs, sheets
- **Scope:** transparent Search headers with a fade, Explore and Library cards, Settings rows and switches (accent on-state), dialogs, sheets and menus (accent tint, rim), Jam, Profile, Friends, Downloads, History, Equalizer; decide the detail pages (stay opaque or glow above the artwork) and the player (accent only).
- **Files:** per `STYLE_MAPPING.md`.
- **Risks:** many files; each must stay behind the guard.
- **Done when:** every screen in `UI_AUDIT.md` section 2 has been opened with the style on and off and noted in a table in this file's follow-up log; builds and tests green.
- **Owner checklist:** walk every screen in the list; open every dialog (share, receive, add friend, update, language, playlist picker); Jam and sharing still work; Spotify and Apple Music import dialogs; icon picker; OFF-parity tick.

### S6 Polish, accessibility, performance, QA
- **Scope:** headings semantics, selected-state announcements, 48 dp audit, 200 percent font pass, reduced-motion pass, a **debug on-screen frame readout** (so the owner can read frame times without adb), tuning of blob counts and the 30 fps clock from those numbers, battery check, release (R8) build check, docs, mark the UI-0 documents superseded, update `CLAUDE.md` and `PHASES.md`.
- **Risks:** nothing new may be added; only fixes and tuning.
- **Done when:** the spec's performance budget is met on the owner's phone with numbers shown in the readout, or the shortfalls are written down; all tests green; release build compiles.
- **Owner checklist:** a week of normal use; report jank, battery, readability, anything odd; read the frame readout on Home with Normal motion; OFF-parity tick.

## Approval gates

`STYLE_APPROVALS.md`: one unchecked line per stage S0 to S6 and one "OFF-parity verified on device" line per stage. The agent never ticks a box. The next stage refuses to start until both lines of the previous stage are ticked. The owner may also write "rollback S<n>" there; the agent then reverts to tag `pre-skin-s<n>` with a new revert commit (never a force-push).

## S1 built (2026-11-03)

**What exists.** New package `app/src/main/java/com/music/bitchord/ui/violencestyle/`:
- `StyleModel.kt`: `StyleState` (enabled, background, motion, intensity, accent source, custom colour, heading font, reduce transparency, detail glow, capability FULL/LITE, lite reason, reduce motion, high contrast), `LocalViolenceStyle` (default `StyleState.DISABLED`), the owner's defaults. Arcs is not an option (owner decision).
- `AccentMath.kt`, `AccentEngine.kt`: pure colour maths (WCAG contrast, HSL clamp 0.55-0.90 / 0.45-0.65, neutral and dull detection, picker guard `makeUsable`), `AccentTokens` (accent, accentSoft, glowColor, onAccent, textAccent, neutral), nine presets, source resolution with fallbacks (song, held previous song, device, custom, fixed Blue).
- `StyleCapability.kt`: FULL versus LITE rules (API below 31, low RAM, battery saver, thermal severe, Reduce dynamic blur, forced reduce transparency), reduce-motion rule, background-clock rule (used from S2).
- `StyleSafeMode.kt`, `StyleSettings.kt`: safe-mode logic and boot hooks (counter written with `commit()`, cleared after the first drawn frame), settings in the existing `bitchord_settings` file under `violence_style.*` keys, no `AppSettings.kt` edit.
- `StyleTypography.kt`: Inter body typography (only while on; `violenceTypography(base, false)` returns `base` itself), heading tokens (`ViolenceHeadingStyles`) for Bodoni Moda and Cormorant Garamond.
- `StyleHost.kt`: `ViolenceStyledTheme` (drop-in for `BitChordTheme`; with the style off it is exactly that call), system signals, accent crossfade (800 ms, 400 ms song debounce, cut under reduce motion).
- `StyleSettingsUi.kt`: the settings block (master toggle, options, picker, Mode line) and the preview card.
- Edited upstream files: `MainActivity.kt`, `SettingsSheet.kt` (listed in `UPSTREAM_SYNC.md`).

**Owner decisions implemented:** dark look while on (light version later); toggle in "Advanced & experimental options"; heading font toggle Bodoni Moda / Cormorant Garamond; Aurora, Corner glow, Haze, Off; defaults Aurora, Medium, From song, Normal, Blue.

**Differences from the S1 brief:** Arcs omitted (owner); the settings live in the existing settings file but in a new object, not in `AppSettings.kt`; the new strings are in `values/strings_violence_style.xml` (English; the other 17 languages fall back to English; `LocaleStringsTest` reads `values/strings.xml` only); the backup tag is `pre-skin-s1` (lower case, created in S0); Reduce transparency follows the in-app Reduce dynamic blur plus a forced switch (no public system switch found); the high-contrast signal reads the undocumented `high_text_contrast_enabled` secure setting (UNCLEAR, harmless if absent); no Compose or Robolectric test infrastructure exists in `:app`, so the "toggle changes the state" and "local defaults to DISABLED" checks are pure-logic unit tests (UNCLEAR: not run under a Compose runtime).

**Licences recorded (S1):**

| Item | Licence | Where |
|---|---|---|
| Inter (variable, `vs_inter.ttf`) | SIL OFL 1.1 | `fork-docs/style/licenses/OFL-Inter.txt`, `NOTICE` |
| Bodoni Moda (variable, regular and italic) | SIL OFL 1.1 | `OFL-BodoniModa.txt`, `NOTICE` |
| Cormorant Garamond (variable, regular and italic) | SIL OFL 1.1 | `OFL-CormorantGaramond.txt`, `NOTICE` |
| Libraries | none added (Compose, AndroidX Core already in the app) | |

Font files were fetched from the Google Fonts repository (`github.com/google/fonts`, `ofl/` folders) at build time of this stage and are bundled; nothing is downloaded at runtime. They add about 3.1 MB to the APK.
