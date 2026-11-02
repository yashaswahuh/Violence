# UI audit: phone app (UI-0)

Written 2026-11-02 on branch `violence-nk`. Scope: the Android phone app (`:app`) and the shared UI it renders (`:sharedUi`). Everything below was read from the code unless marked **UNCLEAR** (could not be verified from source) or **ESTIMATE** (computed here, not measured on a device). No code was changed. Wear OS and desktop: dormant, not checked (desktop shares `:sharedUi`, which is why section 7 flags it).

Companion documents: `UI_DESIGN.md` (three directions), `UI_PLAN.md` (phases UI-1 to UI-4), `UI_DECISIONS.md` (owner template), `ui-concepts/` (static boards).

## 1. How the UI is built

| Layer | Where | Notes |
|---|---|---|
| App shell | `app/.../MainActivity.kt` (6,499 lines). `onCreate` calls `setContent`, which builds `BitChordTheme`, the glass `LayerBackdrop`, then one `BitChordApp(...)` composable | `BitChordApp` is one very large function (it starts near line 547 and runs to about line 6,400). It owns the tab pages, routes, top bar, bottom bar, mini player, the player sheet, and nearly every dialog. There is no smaller "screen" seam around it |
| State | `ui/MainViewModel.kt` (3,639 lines, about 44 observable properties), `AppSettings` (singleton of `MutableStateFlow`s), `ListenTogether`, `ProfileManager`, `Downloads`, `IncomingLinkHub`, `RecentShares`, the Media3 `controller` to `PlaybackService` | Screens read them with `collectAsStateWithLifecycle()`. Nothing is injected; state holders are objects or the activity-scoped view model |
| Navigation | `ui/navigation/AppNavigator.kt` (one back stack of `AppRoute` entries) plus `MainViewModel.detailStack` (album, artist, playlist pages) interleaved by depth; the four tabs are plain indexes (`TAB_HOME=0`, `TAB_EXPLORE=1`, `TAB_LIBRARY=2`, `TAB_SEARCH=3`, private constants in `MainActivity.kt`) | Release pages deliberately are not routes (they load in place on the view model's stack) |
| Shared pages | `sharedUi/.../ui/screens/` `HomeScreen`, `ExploreScreen`, `LibraryScreen`, `SearchScreen`, `TopPicksShelf` | Shared with the dormant desktop app. They reach the platform through `AppUiHost` (`AndroidAppUiHost` on the phone) |
| Player | `sharedUi/.../ui/player/` (`NowPlayingScreen` 4,068 lines, `LandscapePlayer`, `PlayerControls`, `PlayerDrawer`, `PlayerQueue`, `PlayerLyrics`, `SyncedLyricsPanel`, `ListenTogetherMembersSheet`, `PlayerFrost`, `PlayerDock`) plus `app/.../ui/player/` (`AndroidPlayerHost`, `PlayerSheetMotion`) | Hosted in a separate dialog window (`DialogWindowProvider` is read in `MainActivity.kt`), driven by `PlayerSheetMotion`/`PlayerDock`. Platform parts go through the `PlayerHost` interface |
| Theme | `ui/theme/Theme.kt` | See section 5 |
| Glass | `ui/components/` `LiquidGlass.kt`, `AppGlass.kt`, `GlassPanels.kt`, `GlassNavBar.kt`, `FloatingBottomBar.kt`, `FrostedTopBar.kt`, `MiniPlayer.kt`, `IosMenu.kt`, `floatingtabbar/`, `backdrop/` | See section 4 |

## 2. Screen and navigation map

### 2.1 The four tabs (bottom bar)

| Screen | Purpose | Entry points | Key components | State source | Glass usage |
|---|---|---|---|---|---|
| Home (`HomeScreen`) | Recently played, Top Picks hero shelf, recommendation shelves | Tab 0; tapping Home again scrolls to top and refreshes | `TopPicksShelf`, `HeroShelf`, `Shelf`, `RecentShelf`, `CompactTrackRow` | `MainViewModel` feed `HOME` | Floating top controls and the bottom bar are glass; cards are not |
| Explore (`ExploreScreen`) | Moods and genres, charts-style shelves | Tab 1; a mood opens `MoodGenrePlaylistsScreen` in place (`selectedMoodGenre`); "Show all" opens route `Moods` | `MoodGenreCard`/`MoodGenreSleeve`, shelves | `MainViewModel` feed `EXPLORE` | As Home |
| Library (`LibraryScreen`) | Playlists, albums, artists, songs, local, Spotify, downloads links | Tab 2; "Show all" opens route `LibraryShowAll(shelf)`; pinned playlists first | `PlaylistShelf`, `LibraryGridShelf`, `LibraryLinkList`; `LibraryPhoneParts.kt` | `MainViewModel` feed `LIBRARY`, `AppSettings.pinnedPlaylists`, `librarySort` | As Home |
| Search (`SearchScreen`) | Typeahead, recent searches, filters, top result, library search, add-on search | Tab 3 (the field is hosted in the top bar as an accessory pill) | `SearchField`, `TopResultCard`, `SuggestionRow`, `BrowseRow` | `MainViewModel` search state | The search pill is glass |

### 2.2 Pages pushed over a tab (`AppRoute`)

| Route | Purpose | Entry points |
|---|---|---|
| `Settings`, `SettingsSection(category)`, `SettingsChoice(kind)` | Settings home, ten categories (Downloads, Playback, Appearance, Performance, Local music, Storage, Your data, Miscellaneous, Language, Advanced), and option pages (audio quality, download quality, Automix, bit-perfect, font) | Account avatar at the top right of every page |
| `AccountScrobbling`, `Sources`, `Discord`, `Spotify`, `WearOs` | Accounts and integrations (YT Music/Spotify logins, Last.fm/ListenBrainz, add-on sources, Discord, Spotify import library, watch) | Settings rows |
| `ListenTogether` | Jam: create/join, members, queue, server editor | Settings; the player's output/Jam pill; invite links |
| `Profile`, `Friends`, `Receive`, `Recent` | In-app profile, friends list, tap/QR/paste receiver, recent shares (Phases 8 and 9 and later) | Settings (Profile, Friends, Receive rows); the NFC button in the top bar opens `Receive` |
| `Downloads`, `History`, `Replay` | Download manager page, listening history, yearly Replay (stories and share) | Download button in the top bar; Settings; Home |
| `Equalizer`, `EqualizerPresets` | EQ and presets | Settings, player menu |
| `LibraryShowAll`, `ArtistShelf`, `Moods` | "Show all" grids | Shelf headers |
| Release pages (not routes): album, artist, playlist | `DetailScreen.kt` (2,428 lines) | Any card or row, search results, shared links |

### 2.3 Player and overlays

| Surface | Purpose | Entry points | Glass usage |
|---|---|---|---|
| Mini player | Current song, transport, pull-up to expand; long-press shares the song | Sits inside the floating bar when the bar is collapsed, above it when expanded | Glass (live backdrop) or Haze/solid |
| Now Playing | Artwork or Spotify Canvas, transport, volume, lyrics, queue, output, Jam pill, nerd stats | Tap or pull up on the mini player | Mesh gradient from the artwork; panels use `PlayerFrost` stills; round buttons are glass over a still (since Part 4) |
| Landscape player | Two-column layout when `windowWidth > windowHeight` and wide enough (`landscapePlayerAvailable`) | Automatic | As above |
| Player sheets | Queue, lyrics, lyrics providers/offset/share, audio output, cast, audio pipeline, Jam members, ⋯ menu | Player controls | `IosMenu` glass or `PlayerFrost` panels |
| Song/browse menus | `IosMenuPopover`, `HeldContextMenu` (long-press), `SongActionsMenu`, `BrowseActionsMenu`, `PlaylistPickerSheet` | ⋮ on rows, long-press | Glass from a still of the page |
| Share and receive | `ShareSheet` (tap/QR/system share), `TapToShareDialog`, `QrScannerDialog`, `IncomingSongDialog`, `IncomingPlaylistDialog`, `AddFriendDialog` | Song menu, profile, Jam invite, incoming links, NFC | Glass dialogs and sheets (`GlassPanels.kt`) |
| Onboarding | Welcome pages (name, YouTube sign-in) | First run | None |
| Update dialog | `UpdateAvailableDialog`, top-bar update icon | Update check | Dialog |
| Home-screen widget | `widget/` | Launcher | Not part of this UI |

### 2.4 Chrome

- **Top bar** (`FrostedTopBar`): floating circles over a page gradient. Back (on pushed pages), an optional accessory (the search field), and one pill of actions (Receive/NFC, Downloads, optional update, account avatar). The NFC and Downloads actions fold in under the avatar on scroll (`CollapsingTopAction`).
- **Bottom bar** (`FloatingBottomBar` → `GlassNavBar` → `FloatingTabBar`): the four tabs; collapses to an inline pill that holds the mini player when the page scrolls down (`FloatingTabBarScrollConnection`). Swiping the avatar vertically switches account profile (`onSwipeProfile`; the avatar is described as "switch account").
- The glass/no-glass choice is made per surface from `LocalLiquidGlassEnabled`, `isGlassSupported()` and `AppSettings.reduceDynamicBlur`.

## 3. Settings that affect appearance

All in `data/settings/AppSettings.kt`, edited in `ui/screens/SettingsSheet.kt`.

| Setting | Default | Effect |
|---|---|---|
| `themeMode` (System/Light/Dark) | Dark | Colour scheme |
| `appFont` (SF Pro default, Legacy) | Default | Weight shift of the type scale |
| `liquidGlass` | **false** | Real backdrop glass; row is disabled below API 31 |
| `progressiveBlur` | false | Variable blur under the status bar and top controls; only with glass |
| `reduceDynamicBlur` | false | Replaces every blur with solid fills and stops recording the backdrop layer; turns `highPerformanceMode` off |
| `highPerformanceMode`, `performanceRefreshRate` | false | Requests a sustained refresh rate; turns `reduceDynamicBlur` off |
| `reduceAnimation` | false | In the code: freezes the player's mesh gradient and makes the player open/close without the sheet motion (`reducePlayerMotion` in `MainActivity.kt`). It is an in-app switch, not the system setting |
| `legacy_mesh_gradient`, `animatedCanvas`, `hideStatusBarInPlayer`, `hideVolumeBar`, `hideSongStatus`, `showQueueHistory`, `showListenTogetherButton` | various | Player layout and visuals |
| App icon picker (Advanced group) | default icon | Launcher alias switch (`AppIcon`) |
| Language | system | 17 locales (es fr de pl pt id hi ja ru zh zh-rTW he it tr th vi ko plus English) |

**There is no tab or category called "Experimental".** `SettingsCategory` has ten values and the nearest is `ADVANCED`, titled "Advanced & experimental options". UI-1 must either add a new category (a small edit to the `SettingsCategory` enum and the category list) or put the toggle in the Advanced group. This is a decision for the owner (see `UI_DECISIONS.md`). UNCLEAR whether the owner is thinking of a different build's tab.

## 4. Liquid Glass: exact structure

### 4.1 Libraries and licences

| Piece | Source | Licence | Local changes |
|---|---|---|---|
| `ui/components/backdrop/` (backdrop capture, blur, colour matrix, lens/refraction shader, highlight, shadow) | Kyant0/backdrop v2.0.0, vendored as source | Apache-2.0 | Package renamed; `backdropScale` (render the backdrop at a fraction of resolution); the exported-backdrop path re-draws the layer it already recorded; shadow and highlight recordings cached; `SnapshotBackdrop` added (a still of a source instead of the live source) |
| `LiquidGlass.kt` glue (`liquidGlass()`, `GlassExport`, constants) | Adapted from EchoMusicApp/Echo-Music `GlassEffectConfig`/`Modifier.liquidGlass` | GPL-3.0 | Cut down to one on/off switch |
| `floatingtabbar/` (`FloatingTabBar`, scroll connection) | Elyes Mansour `compose-floating-tab-bar` v1.0.1, vendored; modifications taken from Echo-Music | Apache-2.0 (header says so) | Row/Column instead of ConstraintLayout, centred pill, centred morph |
| `GlassNavBar.kt`, `GlassSelectionPill.kt`, `MiniPlayer.kt` structure | Echo-Music `AppFloatingNavBar`/`FloatingMiniPlayer` | GPL-3.0 | Wired to BitChord's tabs and player |
| Haze (`optimizedHazeEffect`, `HazeMaterials`) | `dev.chrisbanes.haze` 1.3.1 (per `ARCHITECTURE.md`) | Apache-2.0 | Used when glass is off or unsupported |

### 4.2 Pipeline

1. `MainActivity.setContent` creates `appBackdrop = rememberLayerBackdrop(onDraw = paintBackdrop)`. `paintBackdrop` fills the window background colour first and then draws the content, because a layer records only what is drawn into it and an unpainted page recorded as transparent (an earlier bug: text showed through the bar unblurred).
2. The page switcher (`AnimatedContent`) carries `Modifier.layerBackdrop(appBackdrop)` only when glass is active and "reduce dynamic blur" is off. This means **the page is drawn twice per frame while glass is on**: once to the screen and once into the layer.
3. A glass surface calls `Modifier.liquidGlass(shape)` → `drawBackdrop(...)`: it draws the backdrop layer at `backdropScale = 0.33`, then applies, in order, colour controls (saturation 1.5), a blur (8 dp, scaled), and, on API 33+, a lens refraction shader (height 24 dp, amount 24 dp, depth effect, chromatic aberration), then a theme tint (`0xFFFAFAFA` or `0xFF121212` at 0.4 alpha), the directional edge highlight and a shadow. The modifier is wrapped in `remember(...)` so the effect chain is not rebuilt on every recomposition.
4. The tab bar's travelling selection pill reads a second layer (`LocalGlassExport`, the bar's own glass) so it refracts the bar rather than the page.
5. Surfaces that cannot sample the live page use a **still**: `SnapshotBackdrop` records one quarter-scale image when asked (`capture()`), and panels blur and lens that. Used for the ⋯ menu and player drawers (`PlayerFrost`), the player's round buttons (`snapshotGlassDisc`), and, since Part 4, dialogs and sheets over the page (`AppStill`, `GlassPanels.kt`).
6. Fallbacks, in order: glass off or API < 31 → Haze (`optimizedHazeEffect`) or a solid `surface` fill; API 31 or 32 → blur and highlight but no lens (the lens branch is guarded by `SDK_INT >= TIRAMISU`); `reduceDynamicBlur` → solid fill plus a 0.5 dp hairline (`GLASS_EDGE_COLOR`).
7. `lightweightLiquidGlass` (tint, highlight and hairline with no capture) and `liquidGlassRim` (highlight only) exist for controls that sit inside the recorded page, where sampling the layer from inside it would loop.

### 4.3 Performance characteristics and known problems

- Cost drivers: the second full draw of the page into the layer; one RenderEffect chain (colour matrix, blur, runtime shader) per glass surface; up to six surfaces at once during a bar fold (the code comment in `LiquidGlass.kt` says so); the lens shader runs per pixel of every surface.
- Mitigations already in the code: backdrop at one third resolution (`GLASS_RESOLUTION_SCALE`), recorded layers cached across frames, modifier chains remembered, quarter-scale stills for panels, page held as one finished picture while the player slides (`CompositingStrategy.Offscreen`), the layer not recorded under "reduce dynamic blur".
- Measurement: `FrameStats.kt` (debug builds only, logcat tag `VioFrames`) reports average, p95 and p99 frame time and jank count every 5 s. **No baseline numbers exist in the repo.** Whether the glass is smooth on the owner's phone is UNCLEAR; the owner reported lag before Part 2 and the fixes since have not been profiled.
- Known limits: a surface drawn inside the recorded page (list cards, Settings groups) cannot use live glass; dialogs and sheets are separate windows, so they use a still, not the live page; the lens needs API 33; below API 31 there is no glass at all; the progressive-blur option costs frames of its own (its own comment says so).
- The player's separate dialog window means page-level glass modifiers do not carry into it; the player has its own stills.

## 5. Theme and design tokens

- `Theme.kt`: `DarkColors`/`LightColors` (near-black `#000000` background in dark, `#0D0D0F` surface; white/black primary; greys from the iOS set: `#8E8E93`, `#6E6E73`, `#1C1C1E`, `#2C2C2E`). `AccentRed #FA2D48` is kept for a few spots only (Replay rank badge). There is **no brand accent in the colour scheme**: primary is white/black.
- Typography: `SFProDisplay` (five bundled `.otf` files in `res/font`: regular, medium, semibold, bold, heavy) drawn one weight lighter than requested, or a "legacy" scale. Sizes are in `sp` (34/30/22/20/16/16/14/12/11).
- Shapes: `ContinuousCornerShape`, `SquirclePillShape`; sizes are inline `dp` literals scattered across files (for example `TopBarContentHeight = 52.dp`, `TOP_CONTROL_SIZE = 44.dp`, avatar 28 dp). There is no token file.
- Icons: `ui/icons/SfIcons.kt` imports `com.composables.sfsymbols` (dependency `com.github.cosmictaserdev-creator.Jetpack_SF_Symbols:sfsymbols-android:1.1.1`); `BitChordIcons` are hand-drawn strokes; some Material icons remain.
- Artwork colour: `ui/theme/ArtworkPalette.kt` (in `:sharedUi`), `MeshGradient.kt`, `ArtworkMeshBackdrop.kt`.

**Public-release licence flags (found while reading; not legal advice):**
1. SF Pro Display is bundled as font files. Apple's SF fonts are not under an open licence and Apple limits their use to work for Apple platforms. UNCLEAR whether that covers an Android app; treat as a blocker for a public release and replace with an OFL font (see `UI_DESIGN.md`).
2. The SF Symbols vector library is a third-party port of Apple's symbols; Apple's SF Symbols licence restricts use similarly. Its own licence is UNCLEAR from the repo. Plan to replace the glyphs used with an open icon set (for example Lucide, ISC, or Material Symbols, Apache-2.0) when the new UI is built.
3. `NOTICE` says "The user interface is inspired by Apple Music", which is fine; the trade dress (layouts, icons) is what must not be copied.

## 6. Accessibility: what the code shows

Counts are `grep` hits over `app/src/main/java` and `sharedUi/src` (so they include comments and can miss indirect uses).

**Strengths**
- All text is in `sp`, so it scales with the system font size.
- 54 + 31 `contentDescription = stringResource(...)` uses; 21 + 15 `.semantics` uses; the app is localised in 17 languages.
- The system animator scale is honoured in three places (`IosMenu`, `HeldContextMenu`, `AudioPipelineDialog`).
- Back handling is centralised (`AppNavigator`), and gestures such as the mini player pull have tap equivalents.
- Haptics are centralised (`Haptics.kt`).
- The in-app "Reduce dynamic blur" and "Reduce animation" switches exist.

**Gaps**
1. 86 + 70 `contentDescription = null` uses. Many will be decorative; UNCLEAR how many are controls without a label. Needs a pass.
2. **Zero `heading()` semantics**, so TalkBack users cannot jump between sections; no `stateDescription` for toggles other than the few counted (Role/stateDescription/customActions/liveRegion total 7).
3. No use of `AccessibilityManager` (touch exploration) to simplify gestures. Swipe actions, long-press menus and the account-switch swipe on the avatar have no verified TalkBack alternative; UNCLEAR for each.
4. Touch targets: the floating top controls are `TOP_CONTROL_SIZE = 44.dp` and the code sets `LocalMinimumInteractiveComponentSize` to that, so they are below the 48 dp target. The mini player and `ThinSlider` hit areas were not measured (UNCLEAR).
5. Fixed-height chrome: `TopBarContentHeight = 52.dp`, menu quick actions `66.dp`, separators `17.dp`, `labelSmall` 11 sp. At 200 % font scale these clip or truncate (ESTIMATE from the code; not run with large fonts).
6. Contrast: `onSurfaceVariant` is fine on solid fills (computed: `#8E8E93` on `#000000` 6.44:1, on `#1C1C1E` 5.22:1; `#6E6E73` on white 5.07:1, on `#F2F2F7` 4.54:1). Glass content is pure white or black chosen from the theme only (`glassContentColor()`), with a 0.4 tint, so over bright artwork it is not guaranteed: white over an F0F0F0 backdrop after a 0.4 tint of `#121212` is about 3.3:1 (ESTIMATE; simple alpha blend, ignores blur and saturation). The accent red on white is 3.79:1, below 4.5:1 for small text.
7. System settings not read: bold text (`Configuration.fontWeightAdjustment`), high-contrast text, and "remove animations" outside the three places above. The in-app `reduceAnimation` does not follow the system setting.
8. No `heading`, `traversalIndex` or focus-order work was found in the player; focus order is whatever layout order gives (UNCLEAR).
9. Orientation and size: the manifest sets no `screenOrientation` and handles `orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden` itself. The layout decisions use measured `BoxWithConstraints` width and height, and a landscape two-column player; there is no `WindowSizeClass` and no foldable posture handling (UNCLEAR whether tablets get a navigation rail; the code shows none).
10. Right-to-left (Hebrew is shipped): UNCLEAR whether custom layouts (the tab bar, the top-bar folding, the mini player swipe) mirror.

## 7. Files a UI change would touch (upstream conflict risk)

`UPSTREAM_SYNC.md` says upstream v1.9 was merged on 2026-10-31. Risk is for the next sync.

| File | Why it would be touched | Conflict risk | Mitigation in `UI_PLAN.md` |
|---|---|---|---|
| `app/.../MainActivity.kt` | The switch point and the four chrome call sites (top bar near line 3,840; bottom area 4,261–4,330) | **High**: 6,499 lines that upstream edits often | Keep edits to a few lines each that call into a new package; one commit per call site |
| `app/.../ui/screens/SettingsSheet.kt` | The toggle row (and a new category if wanted) | Medium: 2,913 lines | One new row calling a new composable; the enum addition kept to one line |
| `app/.../data/settings/AppSettings.kt` | A `uiVariant` key | Medium | Append-only block at the end of the file |
| `app/src/main/res/values*/strings.xml` (18 files) | New strings | Low (appended) | New strings in a new file `strings_ui2.xml` per locale if the build allows it (UNCLEAR for locale folders; appending is the fallback) |
| `app/.../ui/theme/Theme.kt` | Not edited; the new UI wraps `MaterialTheme` itself | None if untouched | New theme in a new package |
| `app/.../ui/components/{LiquidGlass,GlassPanels,AppGlass,IosMenu,FrostedTopBar,MiniPlayer,GlassNavBar,FloatingBottomBar}.kt` | Reuse, not edit | High if edited (upstream tunes glass) | The new UI calls the primitives; any needed parameter is added as a defaulted argument |
| `sharedUi/.../ui/**` (pages, player) | Re-skinning pages and the player | **High and wider**: shared with the dormant desktop app, and logged in `FROZEN_TARGETS.md` for every change | Do not edit; theme through `MaterialTheme`/CompositionLocals, and write new composables in `:app` |
| `app/build.gradle.kts` | New font/icon dependencies | Medium (identity lines are ours) | Prefer bundling font files as resources over new dependencies |
| `tv/` | `:tv` compiles `app/src/main/java`; new files must compile there | Medium | Keep new UI files free of phone-only types, or keep them in a folder `:tv` excludes (UNCLEAR how `:tv` selects files; check `tv/build.gradle.kts` in UI-1) |

## 8. Things this audit could not establish

- Frame times on any real device; thermal behaviour; memory of the page layer.
- Which `contentDescription = null` are decorative.
- TalkBack behaviour end to end (no device was used).
- Whether `:tv` would try to compile new files placed in `app/src/main/java`.
- RTL mirroring.
- The owner's meaning of "Experimental tab" (section 3).
