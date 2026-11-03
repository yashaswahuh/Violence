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

## S2 built (2026-11-03)

**What exists** (new files in `app/.../ui/violencestyle/` unless noted):
- `StyleBackgroundGeometry.kt`: pure geometry (`BackgroundGeometry`: blob positions, radii, alphas, drift, falloff, vignette, `compositeAt`), the contrast guard (`GlowGuard`), the frame throttle levels (`StyleFrameThrottle`).
- `StyleBackground.kt`: `ViolenceBackground()` (root layer), `ViolenceBackgroundHolder` and `LocalViolenceBackground`, `Modifier.violenceBackground(style, color)` (the one see-through helper), `StyleState.showsBackground()`, the draw function and the clock.
- `sharedUi/.../ui/violencestyle/StyleHooks.kt`: `LocalViolenceSeeThrough` (default false).
- Edits: `MainActivity.kt` (the glass floor draws the background; `ViolenceBackground()` is the first child of the root box; the page root box uses `violenceBackground`; the SideEffect also reports whether the full player is open), `SearchScreen.kt` (Top result card), `StyleHost.kt` (provides the holder and the see-through flag).

**Styles:** Aurora, Corner glow, Haze, Off, built as STYLE_SPEC.md section 2 (blob table, vignettes). **Arcs is not built** (owner decision), although the S2 brief lists it; its construction stays in the spec. Intensity Low, Medium and High scale the peak alpha (0.30, 0.50, 0.70) and the blob radius (x0.8, x1.0, x1.15). Reduce transparency replaces the glow with one static 16 percent accent gradient over the top 60 percent of the height.

**Motion:** each blob centre moves on an ellipse driven by one clock (sin and cos of time over a per-blob period: Normal 27 to 39 s, Subtle x1.75; amplitude Normal 0.06 and Subtle 0.025 of the width; the radius breathes 3 and 1.5 percent). The accent crossfade is the 800 ms colour animation from S1.

**Performance design (reasoned, not measured):**
- One `drawBehind` node on its own `graphicsLayer`: a clock tick redraws that node only, not the page. The picture is recorded into one `GraphicsLayer` and drawn from it; with Liquid Glass on, the same layer is drawn again as the floor of the glass recording (so the glass blurs the glow).
- Per frame: one base rect, up to 3 unit-radius radial gradient rects (the same cached brush, moved and scaled by the canvas transform) and at most 1 linear gradient rect. No blur, no RenderEffect, no bitmap, no per-frame allocation (the float array and brushes are cached; brushes are rebuilt only when the colour, size or a setting changes).
- Clock: 30 fps (`withFrameNanos`, one tick per two frames at 60 Hz). It runs only while the style shows a background, motion is not Off, the mode is FULL (not LITE: API below 31, low RAM, battery saver, thermal, Reduce dynamic blur), reduce motion is off, the app is RESUMED (`rememberIsForeground`) and the full player is not covering the page. When it does not run, nothing invalidates: a static background costs no frames.
- Throttle: the real tick interval is averaged over 30 ticks; if it is more than 1.4x the intended interval the rate steps 30, 20, 10 fps, then static, and never steps back up.
- Not done: a 30 s idle stop (it needs a global touch hook), and a scroll-linked shift (skipped: it needs the page scroll position at the root and would invalidate the background on every scroll frame, which are exactly the frames that matter; the visual gain is small).
- Cost to expect with Liquid Glass on: while the clock runs the background changes 30 times a second, so the 2 to 3 glass surfaces (nav bar, mini player, top pill) re-evaluate their blur at that rate. This is the largest unknown and the reason the throttle and LITE exist. UNVERIFIED on a device.

**Legibility:** `GlowGuard` samples a 12 x 24 grid at the base pose and at the four extremes of the drift. Upper 30 percent of the height: white text at least 3:1. Below it: the app's secondary text colour `#8E8E93` at least 4.5:1 (which also covers white). A blob causing a violation is dimmed in 10 percent steps down to 5 percent. Unit tests run it for every preset and for extreme colours (pure blue, red, yellow, cyan, near-white, near-black, gray) x 3 styles x 3 intensities x 3 motions. A Python replica of the same maths shows Medium untouched for Blue, Red and Pink in every style, and only High on Aurora (mid-right blob x0.73) and Haze (x0.73 and x0.9) held back. A 200 percent font scale does not change colours, so contrast holds; fit and wrapping at 200 percent are S3.

**Screens made see-through (only when the style shows a background; one helper, `violenceBackground`):**
1. The page root in `BitChordApp` (`MainActivity.kt`): a full-screen `Box` filled with `colorScheme.background`. This was the one fill that covered everything.
2. The Search "Top result" card (`SearchScreen.kt`, a shared file, through `LocalViolenceSeeThrough`).

Left opaque on purpose: Search song rows (their fill hides the swipe-to-queue reveal; the guard keeps the glow under them near black), album, artist and playlist pages (own artwork colours, S5), Replay and onboarding (own full-screen backdrops), full-screen sign-in surfaces, dialogs and sheets (separate windows), the Jam activity-log card (a card, not a page). A solid floor (`windowBackground`, then the base tint of the layer) stays behind everything, so nothing flashes. The top-bar fade and the bottom fade (`BottomFadeScrim`) still fade to plain black; the top one is invisible on Home at rest and the glow is near black at the bottom. Polishing them is S4.

**Differences from the S2 brief:** no Arcs (owner); no scroll shift (reason above); no idle stop; no Compose or Robolectric UI test (no such infrastructure in `:app`; the toggle-while-playing check is on the owner checklist instead); the OFF-parity proof is `violenceBackground(DISABLED, c)` equal to `background(c)`, `ViolenceBackground()` returning before it composes anything, and `LocalViolenceBackground` defaulting to null.

**UNCLEAR, to verify on a device:** that the glow on the glass floor lines up with the glow on the page (the floor assumes the page area starts at the window origin and has the window size); that the glass blur updates on every tick; smoothness with Liquid Glass on; behaviour of `GraphicsLayer.record` inside `drawBehind` on every supported Android version.

**S2 tuning after the first device review (2026-11-03):** peak alpha x1.25 (0.38, 0.62, 0.85); motion amplitude Normal 0.15 and Subtle 0.07 of the width, period scale Normal 0.6 and Subtle 1.0, radius breathing 6 and 3 percent; the recent-search row fill (`RecentSearchEntityRow`, `SearchScreen.kt`) now also uses `LocalViolenceSeeThrough` (its swipe reveal is clipped to the swiped strip and draws nothing at rest). The Python replica of the guard shows Medium now trims only the mid-right Aurora blob (x0.81) on Blue and Red, and Haze/Aurora High more (x0.59). Unit tests: 1357 phone, 0 failures.

## S3 built (2026-11-03)

**What exists.**
- `sharedUi/.../ui/violencestyle/StyledComponents.kt` (new, shared): `ViolenceSharedStyle` and `LocalViolenceShared` (default null), `ViolenceSectionHeader`, `Modifier.violenceCardRim`, `ViolenceCardScrim` and `ViolenceScrim`, `ViolenceStyleText` (title split, font-scale cap, hairline width). The phone app provides the local from the style host, so with the style off, or on desktop, every shared component keeps its original code path.
- `app/.../ui/violencestyle/StyleChrome.kt` (new): `violenceGlassTint()` and `Modifier.violenceGlassEdge(shape, width, color)` for the top bar; `ViolenceChromeColors.tint`.
- Edited call sites (each one an early branch or a swapped expression; the else path is the original code): `HomeScreen.kt` (`SectionHeader`, `RecentSectionHeader`, `CompactTrackRow`, `HeroCard`), `TopPicksShelf.kt` (`TopPickCard`, `CardCaption`, skeleton, hero heading flag), `FrostedTopBar.kt` (`artworkPageSurface`), `StyleHost.kt` (provides the shared style).

**Behaviour under the style.**
- Section headings: serif caps; a title containing the word "for" splits into caps plus an italic second line ("TOP PICKS" / "for You"); everything else is one caps line. The hero heading (Home's Top Picks) is 36 sp with the hairline ending in the glowing accent dot (accent plus two translucent halos, plain circles); every other heading is 24 sp with a plain hairline. The streak variant belongs to Arcs and is not built. The title is a TalkBack heading; the hairline is a Canvas without semantics; "Show all" has a 48 dp target and a button role. The serif scales at most 1.3x; the hairline shortens and then disappears (minimum 24 dp) before the title is squeezed; titles wrap to two lines then ellipsize.
- Hero cards (Top Picks and the Home editorial cards): 26 dp corners, a bottom scrim (stops 0.35 / 0.62 / 1.0 at 0 / 0.60 / 0.88 black), label in Inter sentence case (white 90 percent), title in serif caps (20 sp), subtitle white 86 percent, a 1 dp translucent rim. The first Top Picks card also gets the accent rim light: 1.2 dp accent line, a 6 dp bloom stroke scaled by the intensity (0.5x to 1.4x of the medium value), and a 35 percent accent edge over the bottom 12 percent. None with the neutral (White) accent. Strokes only: no blur, no per-item animation, brushes and strokes built in `drawWithCache`.
- Recents rows: art corners 9 dp (was 7), artist line white 60 percent, three-dot button at least 48 dp.
- Top bar: the glass pill, back button and accessory use `liquidGlass(shape, surfaceTint = accent tint)` (dark base with 12 percent accent, 45 percent opaque) and a 0.75 dp rim fading from white 22 percent to accent 14 percent. Every action is unchanged and in place.

**Contrast (computed, unit-tested).** Scrim: white artwork at the label position (70 percent of the card height) gives the label at least 4.5:1 and the title at least 3:1 (tests at 0.70, 0.78 and 0.90 of the height). Glass tint with white icons at least 7:1 for every preset. Row artist line white 60 percent on the guarded background is brighter than the old `#8E8E93`, so the S2 glow guard (which assumes `#8E8E93`) stays conservative.

**Decisions followed.** Heading style Mixed; card labels sentence case; no V mark (the existing VIOLENCE wordmark stays; `HomeTitle` is untouched); heading font toggle respected (the shared styles come from the chosen family); light theme later (dark while on).

**Differences from the S3 brief.**
- No V mark and no cast button (owner decision: keep the existing wordmark and actions).
- Only `CompactTrackRow` (Home Recents) is restyled among rows; the other song rows (Search, Library, playlists, album pages, `SongRowContent`) are S5.
- The top Home hero card widths stay at the app's 60 percent (FLEX).
- Compose and Robolectric tests were not added: `:app` has no such dependency (adding one is a build change outside this stage). Pure logic is unit-tested (title split, hairline width, font-scale cap, scrim contrast, tint, tokens, OFF parity of the modifiers); heading semantics, the 200 percent layout and the click targets are on the owner checklist (`STAGE_S3_REVIEW.md`).
- The hairline's vertical position uses a fixed line height (40 dp hero, 28 dp others) rather than measuring the last text baseline.

**UNCLEAR / to verify on a device.** The look against the mockups; the 48 dp three-dot button's position (it hangs into the gutter through `hangIntoGutter`, sized for 36 dp, so the icon may sit up to 6 dp further left); heading wrap at 200 percent; drawing cost of the rim on the first card (one `drawWithCache`, no clock).

## S4 built (2026-11-03)

**How the existing Liquid Glass was extended.** Nothing in `LiquidGlass.kt` or the vendored `backdrop/` renderer was edited. The glass already accepts a `surfaceTint` (used for the top bar since S3); S4 passes an accent-tinted colour through it (`violenceGlassTint()`: the dark base `#101018` with 12 percent of the accent, 45 percent opaque; 92 percent opaque under Reduce transparency) and adds the rim as a separate border (`violenceGlassRim` / `violenceGlassEdge`: 0.75 dp, white 22 percent at the top fading to the accent at 14 percent). Upstream improvements to the glass carry over because the renderer is untouched. FULL/LITE: the glass path keeps whatever the app already chooses (glass on, Reduce dynamic blur off: the existing glass; otherwise the existing Haze or solid fallback), and every fallback gets the same rim and capsule. No new blurred layer, so the app's limit on simultaneous glass surfaces is unchanged.

**New code** (`app/.../ui/violencestyle/StyleGlass.kt`, plus `violencePillColor` in `sharedUi/.../StyledComponents.kt`):
- `ViolenceGlassColors`: capsule fill (accent at most 32 percent alpha, lowered until white labels keep 4.5:1 over the dark glass; faint 6 percent fallback), the glow-line colour (accent lightened by 15 points), glow strength from the intensity (0.6x to 1.4x of Medium), the switch track (accent darkened until the white thumb keeps 3:1), the solid-tint alpha.
- `violenceCapsuleGlowFor(style, tokens)`: the bright bottom line (2 dp gradient line, 6 dp soft line, and an accent wash over the lower 45 percent of the capsule), one `drawWithCache`, strokes and gradients only; returns the same Modifier when the style is off or the accent is neutral (the White preset keeps the gray capsule of ref1).
- `violenceIndicatorColor()`, `violenceSwitchTrack()`, `violenceTabSemantics(selected)`.

**Call sites (each swaps one expression; the else path is the original):** `GlassNavBar.kt` (glass surface tint and rim, indicator colour), `floatingtabbar/FloatingTabBar.kt` (the glow modifier on both the resting and the glass selection capsule; `violenceTabSemantics` on the expanded tabs), `FloatingBottomBar.kt` (the plain bar: tint, rim, capsule, glow, tab semantics), `MiniPlayer.kt` (rim), `IosSwitch.kt` (track), `sharedUi/.../PillSegmentedControl.kt` (selected segment), `sharedUi/.../ThinSlider.kt` (default active colour). The mini player inside the glass bar (`GlassNowPlaying`) and the Search circle use the same glass surface as the bar, so they inherit the tint and rim.

**Accessibility.** Selected tab capsule: white icon and label at least 4.5:1 for every preset (computed and unit-tested, with the alpha lowered where needed, including Yellow); switch thumb at least 3:1; chips at least 4.4:1; the selected tab is also conveyed by shape (the capsule) and, with the style on, by `Role.Tab` and `selected = true` (the bars had no selected semantics before; with the style off nothing is added, so OFF is unchanged). TalkBack labels unchanged. Reduce transparency: the glass tint becomes 92 percent opaque dark, rim and accent kept. Reduce motion: S4 adds no animation of its own.

**Performance (reasoned, not measured).** No new layers or blur: the glow is three draws per selection capsule from cached brushes, and the tint and rim are parameters of the existing glass pass. The nav bar and mini player keep one glass surface each. The cost to watch remains S2's moving background behind the glass (the glass re-evaluates as it changes). The owner checklist includes a frame-stats comparison; no numbers have been measured on a device.

**Differences from the S4 brief.**
- The play button in the full player and the Material sliders in Settings are not restyled: they are inside the 4,000-line player and many screens; they move to S5.
- No pressed-scale, focus ring or disabled redefinition: tabs keep their existing press behaviour, switches keep their dimming, and focus is a TV concern.
- The mini player has no progress bar in this app, so there is no accent progress; its rim and glass tint follow the accent.
- No separate round-search restyle was needed: the Search circle is the bar's standalone tab and takes the same surface.
- No Compose or Robolectric tests (none in this project): selection semantics and "nothing was lost" are on the owner checklist; the colour and contrast rules and the OFF parity of the modifiers are unit-tested.

**UNCLEAR / to verify on a device.** The look against the mockups; that the glow line follows the capsule through the glass "lift" animation; that the 0.75 dp rim and the glass's own highlight do not double up visibly on the nav pill; TalkBack output of the new tab semantics.

## S5 built (2026-11-03)

**Approach.** S5 is a rollout through shared pieces, not a screen-by-screen rewrite. The inventory, what each screen inherits and what stays plain are in `STYLE_COVERAGE.md`.

**What was added.**
- Dialogs, sheets and menus that use `GlassSurface` (through `GlassAlertDialog`, `GlassBottomSheet`): a thin rim (`violenceGlassRim`), an accent-tinted dark panel colour (`#121214` with 8 percent accent, `violencePanelTint`), the near-solid alpha under Reduce transparency (`violencePanelSolid`), and accent text buttons (`violenceButtonColor`, the lightened accent). No change to any dialog's content or actions. The plain non-glass path gets the rim only.
- Player: an accent ring (6 dp translucent stroke plus a 1.5 dp line) around the play/pause control, drawn behind the existing glyph in `TransportGlyph` (a `drawBehind`, no layout change, no clock). The seek and volume bars already took the accent in S4.
- Album, playlist and artist pages: `violenceDetailGlow()` on the page root in `DetailScreen`: a static radial accent wash from the top edge (alpha 0.30 at Medium, 0.19 to 0.42 over the intensities), controlled by its own settings switch (whose subtitle now says what it does). Computed contrast: white text keeps at least 3:1 over the strongest glow on near-black and mid-dark page colours (unit-tested for every preset).

**Not done, and why** (details and the full table in `STYLE_COVERAGE.md`): the player's serif title and a live glow behind the player (own window and mesh, large shared file); `AlertScaffold` account alerts and the Haze-based menus (separate containers); other shelf cards and one-off buttons; Replay, onboarding and login web views (excluded on purpose); snackbars and system prompts. No Compose or Robolectric screen-open smoke tests: the project has none, so `STAGE_S5_REVIEW.md` is the substitute; the new rules are unit-tested (panel tint contrast, detail glow alpha and contrast, OFF parity of the detail-glow modifier).

**Performance.** Nothing new animates. The detail glow is one cached radial brush per page size; the ring is two circle strokes; the panel tint is a parameter of the existing panel glass pass.

**UNCLEAR, verify on a device.** How the accent tint looks on each dialog and sheet; whether any dialog bypasses `GlassSurface` and stays plain; the ring's size against the play glyph; the glow over an artist page.
