# Violence Style: S0 audit (phone app)

Written 2026-11-03 on branch `violence-nk`. Scope: the Android phone app (`:app`) and the shared UI it renders (`:sharedUi`). Everything was read from the code unless marked **UNCLEAR** (could not be verified from source) or **ESTIMATE** (computed or eyeballed, not measured on a device). No code was changed. Wear OS and desktop: dormant, not checked (desktop shares `:sharedUi`, which is why section 7 flags it).

Background, not authority: `UI_AUDIT.md` (2026-11-02, the reverted "custom UI" attempt) was reused where its facts still hold and re-checked against the code. Its designs are not followed.

## Corrections to the S0 brief (found while reading)

1. **There is no "Experimental" tab in Settings.** `SettingsCategory` (SettingsSheet.kt:2674) has ten values; the nearest is `ADVANCED`, titled "Advanced & experimental options" (`R.string.advanced_options`). The other `Experimental` hits in the code are Compose `@Experimental...` opt-in annotations. The toggle therefore goes into `ADVANCED` or into `APPEARANCE` (where Liquid Glass and Reduce dynamic blur live), or a new category is added. Owner decision, see `STYLE_DECISIONS.md`.
2. **The brief and CLAUDE.md disagree on the branch rule.** The brief allows `violence-nk` or `main`; CLAUDE.md and AGENT_RULES.md allow only `violence-nk` (and say the project is never published, while the brief says a public release is planned). This work follows the stricter rule: `violence-nk` only. The public-release plan is recorded in section 5 (licences) and needs the owner to update CLAUDE.md before any public step.
3. **The work folder is `E:\Violence-clone`**, the git clone. `E:\Violence-main\Violence-main` is a plain copy without git, scripts or CLAUDE.md. The reference images were saved in both.
4. **`PROJECT_STATE.md` and `UPSTREAM_SYNC.md` exist; `UI_PLAN.md`, `UI_DESIGN.md`, `UI_DECISIONS.md`, `UI_TEST_PLAN.md` and `ui-concepts/` belong to the reverted attempt.**
5. **Home already has the mockup's structure.** "Top Picks for You" cards (3:4, 60 percent of the row), a "Recents" list, a floating mini player over a floating nav pill with a separate round Search circle, and a top-right action pill all exist today. The skin is a re-dress, not a rebuild.

## (a) Residue check of the reverted attempts

Searched code, resources, Gradle, strings, settings keys and docs for `UiVariant`, `uiVariant`, `New UI`, `NewUi`, safe mode, `Brutal`, `Lumen`, `Aurora`, `Tide`, `ui-concepts`, `ui2`.

| Finding | Where | Kind | Action (not done in S0) |
|---|---|---|---|
| UI-0 documents `UI_AUDIT.md`, `UI_DESIGN.md`, `UI_PLAN.md`, `UI_DECISIONS.md`, `UI_TEST_PLAN.md` | `fork-docs/` | Docs of a scrapped design | Keep as history; mark superseded in S6 |
| Concept boards `A-lumen.html`, `B-aurora.html`, `C-tide.html` | `fork-docs/ui-concepts/` | Static HTML | Same |
| "Extra: Custom UI (UI-0 to UI-4)" section | `fork-docs/PHASES.md` | Doc | Leave; the new "Extra: Violence Style" section sits next to it |
| `HomeTitle` composable: a "VIOLENCE" text wordmark, kept after UI-1 was removed | `HomeScreen.kt:493` (used at :200) | Live code, intentional | Replaced by the V mark in S3 behind the toggle |
| `LOGO_TO_FONT` and `LOGO_ASPECT` private constants, no references | `HomeScreen.kt:512,515` | Unused constants (leftover of the old logo image) | Remove in a cleanup commit, or ignore |
| `// Aurora.` comment | `MeshGradient.kt:253` (sharedUi and a copy under desktopUi) | Unrelated (a mesh-gradient style name) | None |
| Reverted commits "Brutal UI ...", "N-1 ..." | git history only (`1a3905d8` back to `02226af7`) | History | None |

Not found: `UiVariant`/`uiVariant` keys, a safe-mode marker, extra font files (the New UI fonts were removed in `0d59aa70`; `res/font` holds only the five `sf_pro_display_*.otf`, `sharedUi` holds `vazirmatn_regular.ttf`), unused strings, Gradle entries, settings keys, tests. **No code residue.** Nothing was deleted.

## (b) Map of the current phone UI

Legend: "reuse" is the number of call sites found by grep (ESTIMATE of how many screens it reaches).

| Mockup element | Renders today | File : line | Theme tokens | Reuse |
|---|---|---|---|---|
| Page background | No page paints its own background. The root `BoxWithConstraints` does: `.background(windowBackground)` with `windowBackground = MaterialTheme.colorScheme.background` (pure black in dark). The same colour is drawn first into the glass recording layer (`paintBackdrop`) | `MainActivity.kt:455-461, 491-495` | `colorScheme.background` | All tab and route pages |
| Top bar | `FrostedTopBar` (Back or wordmark slot, optional accessory pill, one actions pill: Receive/NFC, Downloads, update, account avatar `TopBarAccountButton`) | `ui/components/FrostedTopBar.kt:157, 585` | `surface`, `outline`, glass tokens | Every page |
| Section header | `SectionHeader(title, subtitle, onShowAll)`: `titleMedium` + `bodyMedium` subtitle, optional "Show all" | `HomeScreen.kt:525` | `typography.titleMedium`, `onBackground`, `onSurfaceVariant`, `primary` | ESTIMATE: about 8 grep hits (Home 4, Library 1, TopPicksShelf 2, `PlayerLyrics` 1; the Home count also matches `RecentSectionHeader`). `RecentSectionHeader` (`HomeScreen.kt:378`) is a private near-duplicate |
| Hero carousel | `TopPicksShelf` over `ShelfRow` (a snapping `LazyRow`); `TopPickCard` | `TopPicksShelf.kt:147, 273`; `ShelfRow.kt` | `surfaceVariant`; hard-coded `Color.White` text | Home only (card is `private`) |
| Hero card shape and size | `TopPickShape = RoundedCornerShape(12.dp)`, ratio 0.75, fraction 0.60, max width 280 dp, gap 14 dp, `thumbnailBorder` rim | `TopPicksShelf.kt:134-142, 172` | none (constants) | Home, mood tiles use 12 dp too |
| Hero caption | `CardCaption`: eyebrow 13 sp, title 17 sp SemiBold white, subtitle 13 sp | `TopPicksShelf.kt:372` | typography + fixed colours | Home |
| Generic shelf cards | `Shelf`, `ShelfCard`, `NewShelfCard`, `VideoShelfCard`, `HeroShelf`/`HeroCard` | `HomeScreen.kt:735-1100` | `surfaceVariant` etc. | Home, Explore, Library |
| List row (Recents) | `RecentShelf` (4 rows per column pager) -> `RecentTrackRow` -> `CompactTrackRow`: 48 dp art, 7 dp corners, `titleMedium`/`bodyMedium`, trailing `RowMoreButton` | `HomeScreen.kt:343-482` | `onBackground`, `onSurfaceVariant`, `surfaceVariant` | Home (2 uses); other lists use other row composables (UNCLEAR how many) |
| Mini player | `MiniPlayer` (stand-alone, Haze fill) and, with glass on, `GlassNowPlaying` inside `GlassNavBar` | `MiniPlayer.kt:390`; `GlassNavBar.kt:300` | `surface`, `glassContentColor()` | One bar for the whole app |
| Bottom navigation | `FloatingBottomBar` (no glass) or `GlassNavBar` -> `FloatingTabBar`; Search is a separate circle (`standaloneIndex = tabs.lastIndex`) | `FloatingBottomBar.kt:178`; `GlassNavBar.kt:92,135`; `floatingtabbar/` | `glassIndicatorColor()`, `glassContentColor()` | One bar |
| Search button | The standalone circle of the tab bar | `GlassNavBar.kt` | as above | One |
| Icon buttons (top controls) | `TOP_CONTROL_SIZE = 44.dp` circles via `liquidGlass`/Haze | `FrostedTopBar.kt` | | All pages |
| Chips and segmented control | `PillSegmentedControl` | `sharedUi/.../components/PillSegmentedControl.kt` | `surfaceVariant` | UNCLEAR (not counted) |
| Switches | `IosSwitch` | UNCLEAR file (called from `SettingsSheet.kt`) | | Settings, Profile, Jam |
| Dialogs and sheets | `GlassBottomSheet`, `AlertScaffold` (Haze) in `AccountAlerts.kt`, glass dialogs (`GlassPanels.kt`, `AppStill`), `IosMenu`, `HeldContextMenu` | `GlassPanels.kt`; `AccountAlerts.kt:887`; `IosMenu.kt` | `surface`, `background` | 20+ dialogs |
| Now Playing | `NowPlayingScreen` (4,068 lines) in its own dialog window with its own artwork mesh | `sharedUi/.../player/NowPlayingScreen.kt` | artwork palette | One |
| Settings rows | `SettingsRow` inside `SearchableSettingsGroup`, switch via `IosSwitch` | `SettingsSheet.kt` (2,913 lines) | | Settings |

Windows and sizes: layout reads measured `BoxWithConstraints` width/height (`MainActivity.kt:491`). No `WindowSizeClass` (grep found none). Gutter `PAGE_GUTTER = 16.dp` (`Common.kt:117`); bars use `BAR_GUTTER`.

## (c) Existing Liquid Glass implementation

Structure is documented in `UI_AUDIT.md` section 4 and was re-verified against `LiquidGlass.kt` (258 lines). Summary:

- **Libraries:** Kyant0/backdrop v2.0.0 vendored (`ui/components/backdrop/`, Apache-2.0, locally modified); glue adapted from Echo-Music (GPL-3.0); `floatingtabbar/` vendored (Apache-2.0); Haze 1.3.1 (Apache-2.0) as the fallback.
- **API:** `Modifier.liquidGlass(shape: CornerBasedShape, surfaceTint: Color? = null)`, `Modifier.lightweightLiquidGlass(shape, fallbackColor)`, `Modifier.liquidGlassRim(shape)`, `isGlassSupported()` (API 31+), `LocalLiquidGlassEnabled`, `LocalAppBackdrop`, `glassContentColor()`, `glassIndicatorColor()`.
- **Can do today:** backdrop sampling at 0.33 scale, saturation boost (1.5), blur 8 dp, a lens refraction shader with chromatic aberration (API 33+), a flat tint (`#FAFAFA` or `#121212` at 0.4 alpha), a directional edge highlight, a drop shadow, hairline `GLASS_EDGE_COLOR` (white 10 percent, 0.5 dp), stills (`SnapshotBackdrop`) for windows that cannot sample the page.
- **Cannot do today:** accent-coloured tint or rim (the tint is theme-driven; `surfaceTint` is the only hook and is used by a few callers), a glow or bloom, a coloured rim, per-surface intensity, a light/dark mismatch with the system. Glass below API 31 does not exist (Haze or solid). The lens is absent on API 31-32. Surfaces inside the recorded page (list cards) cannot sample it. Dialogs and sheets use a still, not the live page.
- **Performance:** the page is drawn twice per frame while glass is on (screen + layer); up to six glass surfaces during a bar fold; one RenderEffect chain per surface. Mitigations exist (third-resolution backdrop, remembered chains, cached recordings). `FrameStats.kt` logs frame times in debug builds (`VioFrames`). **No baseline numbers exist** and there is no emulator or adb on the owner's PC, so smoothness on the owner's phone is UNCLEAR.
- **Setting:** `AppSettings.liquidGlass` (default **false**), `reduceDynamicBlur`, `progressiveBlur`; the Liquid Glass row sits in `APPEARANCE` (`SettingsSheet.kt:975`), disabled below API 31.
- **Key consequence for the skin:** the live background must be drawn **inside** `paintBackdrop` (`MainActivity.kt:456-461`, which already draws `windowBackground` into the recording first). Then every glass surface blurs and refracts the glow for free, which is what makes the mockups' tinted glass appear. With glass off, the same background is drawn by the root box and the bars use Haze or a solid fill.

## (d) Accent and artwork colour handling

- **Dynamic system colour (Material You): none.** No `dynamicDarkColorScheme` or `dynamicLightColorScheme` anywhere in `app/src` or `sharedUi/src`. The theme has no accent at all: `primary` is white (dark) or black (light). `AccentRed #FA2D48` is used only in a few places (Replay badge).
- **Artwork colour extraction: exists.** `sharedUi/.../ui/theme/ArtworkPalette.kt` (463 lines): `rememberArtworkPalette(imageUrl, dark, artPx, keyColors)` returns `ArtworkPalette(background, wash, elevated, accent, onBackground, onBackgroundVariant, divider)`, theme-aware, cached (`seedCache`, LRU), crossfades with `tween(TINT_FADE_MS)` and snaps under `reduceAnimation`. Also `rememberPlayerTint`, `rememberArtworkTopBandLuminance`, saturation clamping (`adaptedArtworkSaturation`) and a gray-artwork rule (`TINT_MIN_CHROMA = 0.08`). It is used by `MainActivity` (detail pages), `DetailScreen`, `SongActionsMenu`, `ReorderPlaylistPage`, `LocalFolder`. It is the right base for "accent from the song"; the skin needs a thin wrapper that keeps only the accent and applies the skin's own clamp.
- **Detail pages** (album, artist, playlist) paint an opaque `palette.background` or `palette.wash` over the whole page (`DetailScreen.kt:527, 2322`), so the live background is hidden there by design.
- The player has its own artwork mesh gradient (`MeshGradient.kt`, `ArtworkMeshBackdrop.kt`) in a separate dialog window.

## (e) Opaque backgrounds that would block a live background

The window background is the only floor; there is no `Scaffold`. Page composables are transparent by design (the comment at `MainActivity.kt:446-454` says so), so Home, Explore, Library and most routes need no change.

| Place | Evidence | How it becomes transparent safely |
|---|---|---|
| Root box | `MainActivity.kt:495` `.background(windowBackground)` | Keep as the floor; draw the live layer on top of it (same `drawRect` then layer in `paintBackdrop`). Guarded by the toggle |
| Search results header block | `SearchScreen.kt:522, 948` `.background(colorScheme.background)` | Use `Color.Transparent` when the skin is on. These are sticky rows over scrolling content (the opaque fill hides rows scrolling under them): replace with a short fade scrim. Shared with desktop, see (g) |
| Detail pages (album, artist, playlist) | `DetailScreen.kt:527, 2322` | Leave opaque (they carry artwork tint). Option: draw the skin glow only above the artwork. Owner decision |
| Jam screen block | `ListenTogetherScreen.kt:1174` `surface` fill | UNCLEAR whether it is a card or a page; inspect in S5 |
| Bottom sheets and dialogs | `MainActivity.kt:4953, 5375` `containerColor = colorScheme.background`; `GlassBottomSheet`; `AlertScaffold` | These are separate windows, so the live page cannot show through; they use `AppStill`. Skin tints them via accent only (S5) |
| Player | Own dialog window with artwork mesh | Left alone. The skin may supply the accent only |
| Top bar fill | `FrostedTopBar.kt:210` solid `surface` only under reduce dynamic blur | No change |
| Mini player fill | `MiniPlayer.kt:440` solid `surface` only under reduce dynamic blur | No change |
| Replay, Onboarding, Spotify canvas auth | `route == Replay`, onboarding flag; `SpotifyCanvasAuthScreen.kt` | Excluded: the skin background is off on these routes |

## (f) Accessibility (verified strengths and gaps)

Carried over from `UI_AUDIT.md` section 6 and spot-checked (the code has not changed in the areas that matter):

Strengths: all text in `sp`; 54 + 31 localised content descriptions; the in-app Reduce dynamic blur and Reduce animation switches; the system animator scale honoured in three places; 17 locales.

Gaps the skin must not worsen and should help fix:
1. Contrast: `onSurfaceVariant #8E8E93` on black is 6.44:1 (fine); on a coloured glow it can fall. The skin must keep body text at or above 4.5:1 against the *brightest part of the glow* (spec section 4.5: glow alpha caps).
2. Fixed-height chrome (`TopBarContentHeight = 52.dp`, 44 dp top controls below the 48 dp target, 11 sp labels) clips at 200 percent font. A display serif at 200 percent needs `maxLines` and `autoSize`/ellipsis rules; the spec sets them.
3. No `heading()` semantics anywhere; the skin's new section headings should add it (cheap win).
4. `reduceAnimation` follows an in-app switch only, not the system animator scale or "Remove animations". The skin reads both.
5. No `AccessibilityManager`, no `WindowSizeClass`, RTL mirroring UNCLEAR.
6. Light theme is a supported mode (`ThemeMode` System/Light/Dark, default Dark); the skin is dark-only (spec section 10).

## (g) Upstream files a skin must touch, ranked by merge-conflict risk

| Rank | File | Why | Risk |
|---|---|---|---|
| 1 | `app/.../MainActivity.kt` (6,499 lines) | Provide the style state (one `CompositionLocalProvider` line), draw the live layer in `paintBackdrop`, wrap the root box. Three small edits | High: upstream edits it often |
| 2 | `sharedUi/.../screens/HomeScreen.kt` (shared with desktop) | `SectionHeader`, `RecentSectionHeader`, `CompactTrackRow`, `HomeTitle` | High: shared and dormant-affecting (log in `FROZEN_TARGETS.md`) |
| 3 | `sharedUi/.../screens/TopPicksShelf.kt` (shared) | Card shape, caption, rim | High: shared |
| 4 | `app/.../ui/components/GlassNavBar.kt`, `FloatingBottomBar.kt`, `MiniPlayer.kt`, `FrostedTopBar.kt`, `floatingtabbar/` | Accent capsule, rims | High: upstream tunes glass. Prefer new wrapper modifiers |
| 5 | `app/.../ui/components/LiquidGlass.kt` | An accent tint parameter | Medium-High. Prefer calling `surfaceTint`, which already exists |
| 6 | `app/.../ui/screens/SettingsSheet.kt` (2,913 lines) | One new group of rows | Medium |
| 7 | `app/.../data/settings/AppSettings.kt` (2,256 lines) | New keys, append-only | Medium |
| 8 | `app/.../ui/theme/Theme.kt` | Typography swap | Medium; prefer a separate `ViolenceStyleTheme` wrapper |
| 9 | `app/src/main/res/values*/strings.xml` (21 files) | New strings; `LocaleStringsTest` needs the key in all 17 locale files | Low (append) |
| 10 | `app/build.gradle.kts` | Only if a dependency is added; none planned | Low |

`:tv` compiles `app/src/main/java` (`tv/build.gradle.kts:144`) and overrides same-path files, so new skin files in `:app` must compile for TV (keep them free of phone-only types or confirm TV excludes them; UNCLEAR until checked in S1).

## Other things this audit could not establish

- Frame times, thermals, memory on any device (no emulator or adb on this PC).
- Whether Robolectric is available for offscreen renders in `:app` tests (no match in `app/build.gradle.kts`; UNCLEAR, check in S1).
- TalkBack behaviour, RTL mirroring, how many `contentDescription = null` are decorative.
- Whether the `IosSwitch`, chips and dialogs are used from the TV or web builds (web is not Compose and is out of scope).
