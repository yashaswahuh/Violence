# Custom UI: implementation plan, UI-1 to UI-4

Written 2026-11-02 (UI-0). Nothing in this plan has been built. The direction is not chosen yet, so the plan names the parts that are the same for A, B and C and marks what depends on the choice (`UI_DECISIONS.md`). Rules for every phase: work on `violence-nk` only; small commits; explicit-module builds through `scripts/build-active.ps1` and `scripts/test-active.ps1`; Wear OS and desktop dormant, not checked; shared-code changes (`:sharedUi`) only where listed here and each logged in `FROZEN_TARGETS.md`; no new paid or account-bound dependency; no secrets.

## 1. Architecture for the toggle

### 1.1 The setting

- `enum class UiVariant { CURRENT, CUSTOM }` in a new file `app/.../ui/v2/UiVariant.kt`.
- Persisted in the **existing** settings store: `AppSettings` (SharedPreferences `bitchord_settings` through the same helpers as `liquidGlass`), key `ui_variant`, **default `CURRENT`**. `AppSettings.uiVariant: MutableStateFlow<UiVariant>` and `setUiVariant()`, in an append-only block at the end of `AppSettings.kt` so upstream merges stay simple. It is not in the settings backup beyond what the file already exports (UNCLEAR: the backup exports the whole `bitchord_settings` file; a restored `CUSTOM` on a build without the new UI must fall back to `CURRENT`, which the reader does for unknown values).
- The toggle row sits in the Experimental place the owner names. **That tab does not exist today** (see `UI_AUDIT.md` section 3): either add `SettingsCategory.EXPERIMENTAL` (one enum value plus its icon and title, and the category list picks it up) or add one row to the `ADVANCED` group ("Advanced & experimental options"). Default in this plan: a one-row group at the top of `ADVANCED`, because it touches the fewest lines upstream also edits. The owner can change this in `UI_DECISIONS.md`.

### 1.2 Where the switch is (and why it is not the whole app)

`BitChordApp` is one composable of about 5,800 lines that creates the media controller (`rememberMediaController()`), owns all routes and nearly every dialog. Swapping it wholesale would force a copy of all that logic or a risky extraction of it from the file upstream edits most. So the switch swaps **the theme and the chrome**, and leaves pages, routes, dialogs and state exactly as they are:

| # | Entry point | Edit in `MainActivity.kt` | New code |
|---|---|---|---|
| 1 | `setContent`, around `BitChordTheme` | Read `AppSettings.uiVariant`; if `CUSTOM`, wrap the content in `V2Theme { ... }` (palette, type, shapes, tier, `LocalUiVariant`); otherwise unchanged | `ui/v2/theme/V2Theme.kt` |
| 2 | Top bar call (`FrostedTopBar(...)`, about line 3,840) | `if (variant == CUSTOM) V2TopBar(...) else FrostedTopBar(...)` | `ui/v2/chrome/V2TopBar.kt` |
| 3 | Bottom area (`GlassNavBar`/`MiniPlayer`/`FloatingBottomBar`, about lines 4,261 to 4,330) | the same one-line `if`, passing the same arguments | `ui/v2/chrome/V2Dock.kt` (dock, orb or command bar) |
| 4 | Player presentation (the sheet that hosts `nowPlaying(...)`) | `if (variant == CUSTOM)` wraps it in `V2PlayerFrame { ... }` (the morph, the Jam chips overlay) | `ui/v2/player/V2PlayerFrame.kt` |
| 5 | Settings | one row | `ui/v2/settings/V2SettingsRow.kt` |

Why this is safe: the controller, view model, navigator, dialogs and overlays are in the same composition either way, so flipping the setting recomposes the chrome only. The queue and playback live in `PlaybackService`; the player's open state is a field of `BitChordApp`; neither is recreated. UI-1 must prove this with a test on a device (toggle while a song plays, with a queue and the player open) before UI-2 builds on it.

### 1.3 Code layout (new files only, upstream-friendly)

```
app/src/main/java/com/music/bitchord/ui/v2/
  UiVariant.kt            enum + settings glue
  theme/                  V2Colors, V2Type, V2Shapes, V2Motion, V2Theme, contrast utilities
  glass/                  V2GlassTier, V2Glass (wraps the existing primitives), TierController
  chrome/                 V2Dock / V2Orb / V2Hub / V2TopBar / V2BackChip (names per chosen direction)
  player/                 V2PlayerFrame, V2JamChips
  safemode/               UiSafeMode (startup tracker, pure logic), message
  settings/               V2SettingsRow, "Reduce transparency" row
app/src/main/res/font/    v2_*.ttf   (names prefixed so they never clash with upstream)
app/src/main/res/values*/ strings_v2.xml (a separate file per locale, see 1.6)
app/src/test/.../ui/v2/   contrast, tier, safe-mode, reachability tests
```

Rules: `:sharedUi` is **not** edited except the single optional hook in UI-3 (section 3). Pages are re-skinned through `MaterialTheme` and CompositionLocals, not by editing them. `:tv` compiles `app/src/main/java`, so UI-1 reads `tv/build.gradle.kts` and either keeps the v2 code compile-clean for TV or adds one `exclude("**/ui/v2/**")` (UNCLEAR which is simpler until the file is read).

### 1.4 Safe mode (two startup crashes revert to the current UI)

Pure logic in `UiSafeMode` over a tiny key-value interface (so it is unit-tested without Android), stored in its own SharedPreferences file `violence_ui_boot` (not the settings file, so a settings problem cannot hide it):

1. At process start, if the variant is `CUSTOM`, read `consecutiveStartupCrashes`. If it is 2 or more: set `ui_variant = CURRENT`, reset the counter, set `showRevertNotice = true`. The app then starts in the current UI.
2. Otherwise mark `startupWindowOpen = true` (written with `commit()`, synchronous).
3. An uncaught-exception handler is installed (chained to the previous handler, so nothing else changes; local only, no reporting, no network, no telemetry) only while the variant is `CUSTOM`. If an exception arrives while `startupWindowOpen` is true, it increments the counter with `commit()` before the chained handler runs.
4. The startup window closes, and the counter resets to 0, when the first screen has drawn and 3 seconds have passed without a crash (a `LaunchedEffect` after the first composition).
5. A system kill or a user swipe-away is not a crash and does not count (only the handler counts).
6. On the next start, if `showRevertNotice` is set, a one-time dialog in the current UI says: "The new look closed unexpectedly twice while starting, so Violence went back to the classic look. You can turn it on again under Settings, Advanced." (strings in all locales). Turning it on again resets the counter.

Tests: counter logic (0, 1, 2 crashes; a successful start resets), the handler chain, the notice shown once.

### 1.5 Reusing the existing glass primitives

The new UI never copies the glass code. `V2Glass` is a thin layer that picks a primitive by tier and surface kind:

| V2 call | Calls | Tier behaviour |
|---|---|---|
| `Modifier.v2Floating(shape)` (dock, orb, top controls) | `liquidGlass(shape)` | Full: as now. Simplified: `liquidGlass` with the lens skipped (already guarded by API 33) or `lightweightLiquidGlass`. Solid: the existing "reduce dynamic blur" branch |
| `Modifier.v2Panel(shape)` (dialogs, sheets, Hub) | `GlassSurface`/`rememberPanelGlass` in `AppGlass.kt` (the quarter-scale `AppStill`) | Solid: the plain `Surface` already returned when glass is off |
| `Modifier.v2Disc(...)` (round buttons) | `snapshotGlassDisc` | Solid: the plain fill |
| `Modifier.v2Menu(...)` | `iosMenuPanel`/`menuBackdropFrost` | As above |
| Rim only | `liquidGlassRim` | |

If a primitive needs a new knob (for example a corner shape or tint), it is added as a **defaulted parameter**, so the current UI is bit-for-bit unchanged and an upstream fix to the primitive reaches the new UI without a merge conflict. `V2GlassTier` is decided by `TierController` (section 5.2 of `UI_DESIGN.md`: API level, low-RAM flag, battery saver, thermal status, measured jank, the user's pinned tier and "Reduce transparency") and provided as a CompositionLocal. The setting `reduceDynamicBlur` always wins (forces Solid), as it does today.

### 1.6 Strings, fonts, icons

- Strings: every new string goes into the 17 locale folders. `LocaleStringsTest` requires each key in every locale; UI-1 checks whether it reads only `strings.xml`; if so a separate `strings_v2.xml` needs the test extended (small), otherwise strings are appended to `strings.xml` as in earlier phases. English placeholders are acceptable at first; translate the new keys before the public release.
- Fonts: the three families and Inter from `UI_DESIGN.md` section 7 as `v2_*.ttf` resources, with their OFL texts added to `NOTICE`. The current SF Pro files stay for the current UI only.
- Icons: an open set added as vector resources or Kotlin vectors in `ui/v2/icons/`; the SF Symbols port stays for the current UI only.

## 2. Phase UI-1: foundations and the switch

**Scope.** The `UiVariant` setting and toggle row; `V2Theme` (palette, typography with the new fonts, shapes, motion, contrast utilities) applied to the *existing* pages when `CUSTOM` is on, so the first visible result is the app in the new colours and type with the old chrome; `V2GlassTier` and `TierController` (inputs wired, the primitives chosen); the "Reduce transparency" setting; `UiSafeMode`; strings; unit tests; proof that toggling does not disturb playback.

**Files and modules.** New: `ui/v2/**`, fonts, `strings_v2.xml` or appended strings, tests. Edited: `AppSettings.kt` (append), `SettingsSheet.kt` (one row), `MainActivity.kt` (entry point 1 and the safe-mode start), `tv/build.gradle.kts` only if an exclude is needed. `:sharedUi`: none.

**Risks.** Re-theming shared pages may expose hard-coded colours (the audit found `AccentRed` and iOS greys in places) and fixed heights that break with the wider fonts. The `MaterialTheme` override reaches only code that reads the theme. `:tv` compile of new code. Recomposition cost of wrapping the whole app in a new theme (verify with `FrameStats`).

**Definition of done.** Toggle off: byte-identical behaviour (a diff of the UI tree is not feasible; instead the same screens are walked and the existing tests pass). Toggle on: the app shows the new palette and fonts on Home, Explore, Library, Search, Settings, and the player, with no crash; toggling during playback does not interrupt audio, lose the queue or close the player (device test, recorded in `UI_TEST_PLAN.md`); safe mode unit tests pass, and a forced-crash debug switch reverts after two launches; contrast unit tests for every token pair pass in both themes; `:app` and `:tv` build; `:app` and `:shared` tests pass; web unaffected; `PHASES.md` updated.

## 3. Phase UI-2: navigation chrome

**Scope.** The chosen direction's bottom navigation (A: dock and orb; B: pill; C: command bar and Now chip), the top bar, the back chip, the Hub (A and B) or the command palette (C), the responsive rail from medium widths, the reachability table implemented, TalkBack semantics for all chrome, large-text layout, reduce-motion handling, and the glass tiers on the chrome.

**Files and modules.** New: `ui/v2/chrome/**`, `ui/v2/icons/**`. Edited: `MainActivity.kt` entry points 2 and 3 (an `if` each). It reuses `AppNavigator` and the tab indexes unchanged (the dock calls the same `onTabSelected`), `MainViewModel` unchanged. `:sharedUi`: none.

**Risks.** The mini-player gestures (pull to expand, long-press to share, swipe to skip) are implemented in `MiniPlayer.kt` and `GlassNavBar.kt`; the orb needs equivalents without editing those files, so some logic is re-implemented (the long-press fix from Part 3 is the reference). Scroll-driven collapse needs the `navBarScroll` connection the page switcher already installs only when glass is active (`glassActive`); UI-2 must make sure the new chrome has its own connection or that the existing one is installed for `CUSTOM` too. 200 % font layout of the five-target dock (A). Touch exploration conflicts with swipes.

**Definition of done.** Every row of the reachability table in `UI_DESIGN.md` section 5.4 is reachable in at most 2 taps from any tab, verified by an automated test that enumerates destinations; all targets are 48 dp or more; TalkBack walkthrough of Home to Player to Hub to Settings with correct labels, headings and order; screenshots at 100, 150 and 200 % font on a 360 dp and a 411 dp wide emulator; landscape and a tablet-size emulator work (rail); Jam, NFC receive, sharing and downloads reachable; `FrameStats` p95 within budget on the owner's phone (or the emulator, labelled as such); builds and tests pass.

## 4. Phase UI-3: Now Playing, Jam and sharing surfaces

**Scope.** The player presentation (A: the orb morph, B: the scene, C: the Now card), Jam presence (A: chips; B: halo; C: card chip), the quick-action fan or ribbon, share and receive entry points, the adaptive colour with the contrast safeguards, dialogs and sheets re-skinned through `GlassSurface`/`V2Panel`.

**Files and modules.** New: `ui/v2/player/**`. Edited: `MainActivity.kt` entry point 4. **One permitted `:sharedUi` change:** a defaulted CompositionLocal hook (for example `LocalPlayerChrome` with a null default) at the point in `NowPlayingScreen` where the title is drawn, so the Jam chips can be placed without overlaying guesses. It is optional (the fallback is an overlay positioned from the player's layout, which is fragile), null on desktop, and must be logged in `FROZEN_TARGETS.md`. Desktop is not built or tested.

**Risks.** The player is in its own dialog window, so locals provided outside it do not all reach it (the glass stills are provided inside it today for this reason). The morph depends on `PlayerSheetMotion` and `PlayerDock`, which are tuned code. Contrast on bright artwork. The Jam chips need the member list and avatars from `PlayerHost.party` (`PartyUi`), and unknown avatars fall back to a letter, as in `PROFILE_DESIGN.md`.

**Definition of done.** Open, drag, close and the morph hold 55 fps or better on the reference device; every gesture has a visible control; the TalkBack order of the player is as specified; Jam chips update with joins, leaves and host changes and announce them politely; the like, share (NFC, QR, link) and add-to-Jam actions work from the new surfaces; contrast tests with synthetic bright and dark artwork pass; switching variant with the player open keeps it open.

## 5. Phase UI-4: pages, polish, performance and release hardening

**Scope.** Page-level components in `:app` (hero cards, shelf headers, row styles) built from the tokens where theme-only re-skinning is not enough; settings pages re-skinned; the automatic tier controller with measured jank (`FrameMetrics`); performance tuning to the budget; a full accessibility pass (labels for the `contentDescription = null` list from the audit, `heading()`, state descriptions, focus order, high-contrast and bold-text handling); translation of new strings; documentation (`UI_TEST_PLAN.md`, update `PHASES.md`, `UPSTREAM_SYNC.md` watch list for the new switch points); an upstream-merge rehearsal on a throwaway branch; removal or keeping of the "Experimental" label per the owner.

**Files and modules.** New: more of `ui/v2/**`. Edited: `UPSTREAM_SYNC.md`, `FROZEN_TARGETS.md` (if anything shared changed), docs. `:sharedUi` pages are still not edited.

**Risks.** Scope creep into the 2,400-line `DetailScreen.kt` and the 2,900-line `SettingsSheet.kt`; those are re-skinned by theme and only the highest-traffic elements get wrappers. Translation volume. Licence replacement of fonts and icons completed in the new UI only; the current UI keeps SF Pro and the SF Symbols port (a separate decision before any public release, flagged in the audit).

**Definition of done.** The performance budget in `UI_DESIGN.md` section 5.2 is met on the reference device with numbers from `FrameStats` recorded in the test plan; automatic tier step-down and manual pinning work; the accessibility checklist passes (contrast tests, 48 dp scan, font scale screenshots, TalkBack walkthrough, reduce-motion and reduce-transparency behaviour); all strings translated or flagged; the toggle off still equals today's app; a rehearsal merge of upstream shows conflicts only in the five listed switch points; `PHASES.md` marked; the owner signs off in `UI_DECISIONS.md`.

## 6. Order, effort and what waits

| Phase | Size (relative) | Depends on |
|---|---|---|
| UI-1 | Small to medium | Owner's choice in `UI_DECISIONS.md` (palette and fonts can start before it, since they are shared) |
| UI-2 | Large | UI-1 |
| UI-3 | Large | UI-2 |
| UI-4 | Medium to large | UI-3 |

**Phase 10 (Final Audit and Handover) waits until the custom UI work is finished**, as the owner asked. Phase 11 (playback sources and login flows log) is independent of this work.

## 7. Open items the owner should decide (also in `UI_DECISIONS.md`)

1. Direction A, B, C or a mix. 2. Where the toggle goes (new "Experimental" category or the Advanced group). 3. Whether the new UI should also carry the current UI's Apple-style icon set (no, per the public-release goal) and which open set to use. 4. Whether to replace SF Pro and the SF Symbols port in the *current* UI before a public release. 5. Which phone is the performance reference.

---

## 8. What UI-1 actually built (2026-11-02)

Direction: **A Lumen with Tide's solid pages and Aurora's wash on Now Playing only** (chosen in `UI_DECISIONS.md`). The Aurora wash itself is not built yet (UI-3).

**Libraries added: none.** New resources: Sora and Inter variable fonts (SIL OFL 1.1; texts appended to `NOTICE`; bundled, no network). Icons use the Material icons already in the app (Apache-2.0).

| Piece | File (under `app/src/main/java/com/music/bitchord/ui/v2/`) |
|---|---|
| Variant enum (CURRENT/NEW), parse | `UiVariant.kt`; stored as `ui_variant` in `bitchord_settings` via `AppSettings.uiVariant`/`setUiVariant`; default CURRENT |
| Root switch | `V2Root.kt` (always the same composition shape; only values change, so the media controller, queue, Jam and downloads are untouched), `rememberNewUi()` |
| Theme and tokens | `theme/V2Tokens.kt` (colours, dimens, shapes, elevation, motion with reduced-motion rule, type scale), `theme/V2Theme.kt` |
| Contrast and adaptive accent | `theme/ContrastMath.kt` (ratio, `guard` = fine / scrim / solid, `ensureContrast`), `theme/ArtworkAccent.kt` |
| Glass | `glass/GlassLevel.kt` (levels, `decideGlassLevel`, `GlassBudget`), `glass/V2Glass.kt` (`v2Glass`, `v2Solid`) |
| Accessibility defaults | `a11y/V2A11y.kt` (`v2Interactive`: 48 dp minimum, label, role, state, focus ring) |
| Shell | `chrome/V2Dock.kt` (dock, orb, `V2DockHost`), `chrome/V2HomePlaceholder.kt`, previews in `chrome/V2Previews.kt` |
| Safe mode | `safemode/UiSafeModeLogic.kt` (pure), `safemode/UiSafeMode.kt` (Android store `violence_ui_boot`, `commit()`) |
| Settings | `settings/V2SettingsRows.kt`: "New UI (experimental)" and "Reduce transparency", at the top of Settings, Advanced (there is no separate Experimental tab) |

### Glass-level rules (as built)
`decideGlassLevel`: **SOLID** if Android below 12 (API 31), or "Reduce transparency", or the existing "Reduce dynamic blur"; else **SIMPLIFIED** if low-RAM device, battery saver, reduced motion (the app's "Reduce animation" or system animator scale 0) or Android 12 (no lens shader, API below 33); else **FULL**. A `GlassBudget` allows 3 live backdrop surfaces at once; a fourth draws SIMPLIFIED. FULL maps to the existing `liquidGlass()`, SIMPLIFIED to a tint plus the existing rim (`HighlightElement`) plus hairline, SOLID to a surface and hairline. Under the New UI the existing `LocalLiquidGlassEnabled` is true only at FULL, so the old live-glass pieces (page backdrop recording, glass top controls) stop recording below FULL. "Reduce transparency" has no system equivalent on Android, so it is the app's own switch (default off); reduced motion defaults to the system setting.

### Safe mode as built
A marker is set when the New UI first composes and cleared two frames after it is displayed. A marker still set at the next launch counts as a failed start; after two failed starts in a row the next launch persists CURRENT, shows a one-time dialog, and the count resets. Healthy starts reset the count. (This is by marker, not by crash handler, as the UI-1 brief asked; so a kill during the first seconds also counts.)

### What the New UI shows today
Existing pages in the new palette and fonts; the dock and orb in place of the bars (tap the orb: open player; long-press: share; TalkBack custom actions: play/pause, next, previous); a placeholder Home with a tile for each non-tab destination (Profile, Friends, Jam, Receive, Downloads, Recent, History, Replay, Settings) and a button into the classic Home. Everything else is the current screens.

### Known gaps (UI-1)
The Hub avatar sheet, back chip, top bar and player morph are UI-2 and UI-3. The page backdrop floor in `setContent` (`paintBackdrop`) still uses the classic background colour, so under glass FULL the blurred floor can differ from the new palette by a shade. Compose UI tests: the project has no Compose test setup (no Robolectric, no ui-test dependency); see `PHASES.md`.

---

## 9. What UI-2 built, and what is left for UI-3 (2026-11-02)

**Libraries added (test only):** Robolectric 4.14.1 (Apache-2.0), `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` (Apache-2.0, BOM-managed). `isIncludeAndroidResources = true` for unit tests. Tests need `@Config(application = android.app.Application::class)` (the app's own Application pulls in a Java 21 dependency that JDK 17 cannot load) and `@ConscryptMode(OFF)` on Windows.

**Built** (`ui/v2/chrome/`):
- `V2Hub.kt`: the **Hub** sheet (glass via `GlassBottomSheet`), opened by the account avatar when the New UI is on; every non-tab destination in four headed groups (People, Share, Music, You): Profile, Friends, Jam, Receive, Recent shares, Downloads, History, Replay, Spotify import, Sources, Equalizer, Accounts and scrobbling, Settings. The avatar's old action (account selector, or Settings when signed out) is replaced by the Hub in the New UI; the account selector is reached through Settings, Accounts.
- `V2NowPanel.kt`: **Now Playing panel**, opened by tapping the orb: artwork, title, artist, Jam presence chips with profile avatars and "host", accessible seek bar (`Slider` in a 48 dp box; state "1:24 of 3:30"; actions Back/Forward 10 s), transport (48 dp or more; disabled and explained when the Jam host has locked controls), actions (full player, share, Jam). Colour from the artwork through `ArtworkAccent` (contrast guaranteed) plus the artwork wash behind the sheet. Seek goes to `MediaController.seekTo` once, when the finger lifts.
- `V2Frames.kt`: **shortcut strips** pinned above the current Home and Library pages (solid, scrolls sideways, chips 48 dp), with the page's top padding raised by the strip's height. The pages are unchanged and keep their own skeletons, empty and error states, pull to refresh, sorting and filtering.
- `V2Dock.kt`: `V2DockHost` now also hosts the panel and the Hub.
- Deep links and incoming links: no change needed. `IncomingLinkHub` and its dialogs, the route stack and `AppNavigator` live in `BitChordApp`, which is the same composition in both UIs, so a link lands where it does today (to be confirmed on a device: `UI_TEST_PLAN.md` 2.8). State restoration: the selected tab is `rememberSaveable` as before; the Hub and panel are not restored (UNCLEAR whether that matters).

**Fallbacks to replace in UI-3 (each is still reachable):**
1. Queue, lyrics, audio output and quality, add to playlist, the track menu: via "Queue, lyrics and more", which opens the current full player.
2. Search: the current Search page, no changes.
3. Library: the current page plus the shortcut strip.
4. The orb-to-player **morph**: the panel is a glass sheet rising from the bottom (not a shape morph from the orb). The full player is the current one.
5. Top bar: the current floating top bar (with the avatar now opening the Hub).
6. Predictive back: not changed; the manifest does not set `enableOnBackInvokedCallback` (changing it affects the current UI too). UNCLEAR how the sheets behave under the predictive-back gesture.
7. Two-pane layouts: none. The dock is capped in width and centred, and sheets use the standard maximum width, so wide windows do not stretch; no window-size-class logic yet.
8. Home: the current Home content, not a redesign.

**Performance notes.** No device measurement was possible here. By reasoning: the panel recomposes once a second at most (it reads `PlaybackPosition.positionMs`; no per-frame work); the seek drag keeps its value in local state and seeks once; lists in the pages are the existing lazy lists with their own keys; the dock and the panel add at most 2 to 3 live glass surfaces (budget in `GlassBudget`); image loading is Coil with the app's cache. Hotspots to check on a phone: the Full-glass page backdrop recording (existing cost, now also on under the New UI), the glass sheet's backdrop still being captured on open, and the shortcut strip's `onSizeChanged` causing one extra layout pass when it first measures. Compose compiler metrics and layout inspector were not run.

**Risks seen.** `BitChordApp` is at the JVM method-size limit: keep additions single calls. The `Slider` track is 44 dp, so the 48 dp touch area is the wrapping box, not the slider.
