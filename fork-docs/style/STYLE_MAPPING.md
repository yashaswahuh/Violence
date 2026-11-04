# Violence Style: mapping of mockup elements to code

Rule: restyle through the theme and shared components; every change is guarded so that the OFF state is untouched. Techniques: **Token** (theme or CompositionLocal value), **Modifier** (a `violenceStyled...` extension that returns its input unchanged when the style is off), **Wrapper** (a new composable that wraps an existing one), **Root layer** (drawn once at the app root). Risk: Low, Med, High (merge-conflict plus regression). "Shared" means the file lives in `:sharedUi` and also feeds the dormant desktop app; a change there is logged in `FROZEN_TARGETS.md` ("Possible impact") and the desktop is not touched.

| Mockup element | Existing composable(s) / file | Technique | Stage | Risk |
|---|---|---|---|---|
| Master toggle, options | `SettingsSheet.kt` (new rows in `ADVANCED` or `APPEARANCE`, owner decides); `AppSettings.kt` (new keys, append-only) | New composable `ViolenceStyleSettings()` in a new file, one call site | S1 | Med |
| Style state | none today | `LocalViolenceStyle` CompositionLocal (`staticCompositionLocalOf`, default OFF) provided in `MainActivity.setContent` | S1 | Med (one edit in MainActivity) |
| Accent engine | `ArtworkPalette.kt` (shared, read only), `AppSettings`, system colours | New `AccentEngine` in new package `ui/violencestyle/`; reads `rememberArtworkPalette` (no edit) | S1 | Low |
| Dark-only theme | `Theme.kt` `ViolenceTheme(darkTheme, font)` | Wrapper `ViolenceStyleTheme` applied around it when on; forces `darkTheme = true`; `SystemBarIcons(dark)` follows | S1 | Med |
| Fonts (serif, Inter) | `Theme.kt` typography, `res/font` | New font families in new file; typography swap by wrapper `MaterialTheme(typography = ...)` nested inside the existing theme when on | S1 (files), S3 (use) | Low |
| Safe mode | `ViolenceApplication` / `MainActivity.onCreate` | Counter in SharedPreferences, new file; a 3-line hook | S1 | Med |
| Style preview card | none | New composable shown in the style settings | S1 | Low |
| Live background (all five styles) | root `BoxWithConstraints` and `paintBackdrop` in `MainActivity.kt:455-495` | **Root layer**: `ViolenceBackground()` drawn after `drawRect(windowBackground)` inside `paintBackdrop` and in the root box when glass is off; style OFF draws nothing | S2 | High (MainActivity edit, glass interaction) |
| Background pause and LITE logic | `ForegroundState.kt` `rememberIsForeground()` (shared, read only); `reduceDynamicBlur`, `reduceAnimation` | New `StylePerformance` state holder | S2 | Low |
| "V" mark (top left) | `HomeTitle` in `HomeScreen.kt:493` (shared) and the wordmark slot of `FrostedTopBar.kt` | Wrapper: when on, `HomeTitle` draws the V (needs the asset decision); the guard is one `if (LocalViolenceStyle.current.enabled)` in shared code | S3 | High (shared file) |
| Top-right glass pill | `FrostedTopBar.kt` actions pill (`ActionsRow`, `TopBarAccountButton`) | Modifier `violenceGlassPill()` on the pill container; no change to actions | S3 | Med |
| Section heading (TOP PICKS, RECENTS) | `SectionHeader` (`HomeScreen.kt:525`), `RecentSectionHeader` (`:378`) | Wrapper `ViolenceSectionHeader` called from inside both under an `if`; display serif, hairline, terminator, `heading()` | S3 | High (shared file) |
| Hero carousel and cards | `TopPicksShelf` / `TopPickCard` (`TopPicksShelf.kt:147, 273`), `TopPickShape` | Replace the `private val TopPickShape` with a function of the style; rim and scrim as `Modifier.violenceCardRim(first = ...)`; caption fonts through typography | S3 | High (shared file, 1,717 lines) |
| Card caption (label, title, subtitle) | `CardCaption` (`TopPicksShelf.kt:372`) | Style-aware text styles inside the existing function | S3 | Med |
| Other shelf cards (Explore, Library) | `Shelf`, `ShelfCard`, `HeroCard` (`HomeScreen.kt`) | Only the shared rim modifier; no restructuring | S5 | Med |
| List rows (Recents and others) | `CompactTrackRow` (`HomeScreen.kt:429`) | Style-aware corner (9 dp) and text styles under an `if` | S3 | Med (shared) |
| Mini player | `MiniPlayer` (`MiniPlayer.kt:390`), `GlassNowPlaying` (`GlassNavBar.kt:300`) | Modifier `violenceGlassRim()` and tint through the existing `surfaceTint` argument; icons unchanged | S4 | Med |
| Bottom nav pill and tabs | `GlassNavBar.kt`, `FloatingBottomBar.kt`, `floatingtabbar/` (vendored) | Selected capsule: new `ViolenceCapsule` drawn through `glassIndicatorColor()`'s call site; rims via modifier. No edit inside `floatingtabbar/` if avoidable | S4 | High (vendored, tuned by upstream) |
| Search circle | standalone tab of `GlassNavBar` | Same modifier as the pill | S4 | Low |
| Scroll-under fade | none | Root layer: bottom gradient above the page, below the bars | S4 | Med |
| Icon buttons (44 dp top controls) | `FrostedTopBar.kt` | Modifier for rim and 48 dp hit area | S4 | Med |
| Accent tint of active states | switches (`IosSwitch`), sliders, chips (`PillSegmentedControl`) | Optional `accent` read from `LocalViolenceStyle`; unchanged when off | S5 | Med |
| Search page | `SearchScreen.kt:522, 948` opaque fills (shared) | `Color.Transparent` plus fade when on | S5 | High (shared) |
| Explore, Library, Settings, other routes | pages are transparent over the window background | Nothing, or per-page checks | S5 | Low |
| Dialogs, sheets, menus | `GlassPanels.kt`, `AlertScaffold` (`AccountAlerts.kt:887`), `IosMenu.kt`, `HeldContextMenu.kt`, `GlassBottomSheet` | Accent tint through existing still/tint parameters and rim modifier | S5 | Med |
| Detail pages (album, artist, playlist) | `DetailScreen.kt` palette backgrounds | Left opaque by design; optional glow above the artwork | S5 (owner decides) | Low |
| Now Playing and Replay | `NowPlayingScreen.kt`, Replay | Excluded; accent only supplied | S5 | Low |
| Accessibility, performance, QA | all of the above | Headings, 48 dp targets, 200 percent font pass, reduced motion, debug frame readout | S6 | Med |

## New code location

New package `com.music.violence.ui.violencestyle` in `:app` (`app/src/main/java/com/music/violence/ui/violencestyle/`), plus tests under `app/src/test/.../violencestyle/`. Shared-UI guards (`HomeScreen.kt`, `TopPicksShelf.kt`, `SearchScreen.kt`) cannot import `:app`, so they read a small `ViolenceStyleHooks` interface defined in `:sharedUi` (new file, `ui/violencestyle/StyleHooks.kt`, default = off) that `:app` implements and provides. The shared composables call it; when it is off they take the original path. This is the only new code in `:sharedUi`.
