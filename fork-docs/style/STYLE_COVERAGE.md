# Violence Style: screen coverage (after S5)

Written 2026-11-03. Source of the screen list: `UI_AUDIT.md` section 2 (re-checked against the code where a claim matters). "Inherits" means the screen picks the style up through a shared component or through the root, without a change of its own. **UNCLEAR** means I could not confirm it without running the app (no device tooling on the build PC). Wear OS and desktop: dormant, not checked.

## What every screen gets for free (the mechanisms)
| Mechanism | Stage | Reaches |
|---|---|---|
| Live background at the root, page root fill made see-through | S2 | every tab and route page that paints no fill of its own |
| Inter body typography, dark theme, accent tokens | S1 | all text drawn from `MaterialTheme.typography` (almost everything) |
| `SectionHeader` and `RecentSectionHeader` (serif, hairline, dot) | S3 | Home, Explore, Library, Find your mood and every shelf header |
| Hero card shape, scrim, rim, caption; Recents row | S3 | Home Top Picks and editorial hero cards, `CompactTrackRow` |
| Glass top-bar pill, back button, search accessory | S3 | every page's top bar |
| Nav pill, selected capsule with glow line, mini player, Search circle | S4 | the whole app (one bar) |
| `IosSwitch` accent track | S4 | every switch (Settings, Profile, Jam, ...) |
| `PillSegmentedControl` accent segment | S4 | Search tabs, settings segmented rows |
| `ThinSlider` accent track | S4 | player seek and volume bars, any `ThinSlider` |
| `GlassSurface` rim, accent panel tint, near-solid under Reduce transparency; `GlassAlertDialog` accent text buttons | S5 | every glass dialog and bottom sheet that goes through `GlassAlertDialog`, `GlassBottomSheet` or `GlassSurface` |
| ~~Accent ring around the play control~~ | S5, **removed** after the owner's review (2026-11-03) | nothing: the play button is back to its original look (the unused `accentRing` option stays in `TransportGlyph`) |
| Accent rim light follows the front Top Picks card | S5 fix | the card at the front of the row carries the rim light and it moves as the row is swiped (`frontIndex` in `TopPicksShelf`) |
| Glow above the artwork (own switch) | S5 | album, playlist and artist pages' base page (`DetailScreen`) |
| Search rows see-through | S2 and a later fix | Search "Top result", recent-search rows, song rows with swipe-to-queue |

## Tabs and pages
| Screen | Inherits | Still plain | Plan |
|---|---|---|---|
| Home | background, headings, Top Picks and hero cards, Recents rows, top bar, nav, mini player | other shelf cards (`ShelfCard`, `NewShelfCard`, `VideoShelfCard`): old 12 dp corners, no rim | cosmetic polish after the owner's review |
| Explore | background, `SectionHeader` (incl. Find your mood and its Show all), nav | mood tiles (`MoodTile`) keep the old look | polish |
| Library | background, headers with Show all, nav | playlist/album tiles, "Library link" rows | polish |
| Search | background, see-through rows, chips, top-bar field in glass | song/artist result cards' art corners | polish |
| Now Playing (full player) | accent seek bar (no ring on play: removed), own artwork mesh background (follows the song already), glass chips as before | **Serif title treatment not applied** (the title lives inside the 4,000-line `NowPlayingScreen`); the player has its own window and mesh background, so the live S2 glow is not drawn there | title and lyrics typography as a later polish; left alone on purpose to avoid touching the player layout |
| Queue, lyrics panels | typography (Inter) | panel chrome | polish |
| Album, playlist pages | glow above the artwork (switch), typography, switches, chips | own artwork-tinted fills stay opaque by design | none |
| Artist pages | base page glow; the photograph covers most of it | the hero photograph | none |
| Settings home and categories | background, Inter, switches, chips, the Violence Style block, nav | group cards keep their `surface` fill and rim, group titles keep the old style | decide with the owner whether group cards should become glass |
| Downloads, History, Equalizer, Receive, Recent shares, Friends, Profile, Jam, Sources/Accounts | background (transparent pages), typography, switches, buttons as before | their cards and one-off buttons | polish |
| Replay | none by design | own full-screen backdrop and stories | excluded on purpose |
| Onboarding / welcome | none by design | own full-screen | excluded on purpose |
| Login screens (YT Music, Spotify, Discord) | none | full-screen web views on `Surface(background)` | excluded: a web view must stay opaque and legible |
| Spotify / Apple Music import and review | dialogs through `GlassAlertDialog`/sheets inherit the panel style | custom review pages | UNCLEAR which of these use `GlassAlertDialog`; check on a device |
| Update dialog, language dialog, song info, playlist picker, account dialogs | those built on `GlassAlertDialog`/`GlassSurface` inherit; `AlertScaffold` (account alerts) and menus built on Haze keep the old surface | `AccountAlerts.kt` `AlertScaffold`, `IosMenu`, `HeldContextMenu` | restyle the two containers in a follow-up |
| Snackbars, permission prompts | none | system or Material defaults | not restyled |
| Icon picker, About (fork attribution) | background, typography | their rows | untouched; attribution text and links are unchanged |

## Screens that were deliberately left unchanged, and why
- **Now Playing title and background:** the player is its own window with its own mesh; its layout is a 4,000-line file shared with the dormant desktop app. A serif title there was not done: it needs a layout-aware change and a device to judge.
- **Replay, onboarding, login web views:** full-screen surfaces that must stay opaque.
- **`AlertScaffold` and the Haze-based menus:** separate containers with their own blur; restyling them safely needs the same device check as the glass panels.
- **Snackbars and system prompts:** not ours to style.

## Brief items not done in S5, honestly
No Compose or Robolectric smoke tests ("open every screen with the style on and off") exist, because the project has no such test infrastructure; the owner's screen-by-screen checklist (`STAGE_S5_REVIEW.md`) is the substitute. The pure rules that were added (panel tint contrast, detail-glow contrast and OFF parity) are unit-tested.

## Update (2026-11-03, after the owner approved the S5 leftovers)
- **Now Playing:** the song title uses the heading serif (22 sp, not upper-cased) and an accent glow sits behind the artwork backdrop (two soft blobs on the top and right edges, one slow 28 s loop, still when motion is off, LITE or reduce motion). The glow is drawn on the player's blurred-artwork backdrop; if the player uses the legacy mesh backdrop instead (setting `legacy_mesh_gradient`) the glow is not drawn there (UNCLEAR on a device).
- **Settings:** group cards are translucent dark accent-tinted panels with the thin rim (near-solid under Reduce transparency). They cannot sample the page live (they sit inside the recorded page), so this is the translucent look, not the lens.
- Still unstyled: `AlertScaffold` account alerts, Haze menus, other shelf cards, snackbars, Replay, onboarding, login web views.

## Update (2026-11-03, account menu and Replay)
- **Account menu ("Switch account"):** glass with a lighter wash and a faint accent tint, accent rim, anchored left (82 percent width); near-solid under Reduce transparency or Reduce dynamic blur. Other Haze menus and alerts are still unstyled.
- **Replay cards:** lit by the app accent, so all cards agree (before, each took its own cover's colours). The share poster (`ReplayPoster`) is not changed. UNVERIFIED on a device.
- **Account menu, correction (2026-11-03):** it drops from the account picture at the top right (under the top bar, 80 percent width, right-aligned), not from the left; lighter wash, a top sheen and the accent tint for a glassier look. UNVERIFIED on a device.
- **Liquid Glass account menu (2026-11-03):** a switch in Settings, Advanced & experimental options, Violence Style ("Liquid Glass account menu", off by default) draws the account menu with the bars' pipeline (live page blur, lens on Android 13+, highlight) over a thin accent tint. It needs the app's Liquid Glass on, Android 12 or later, and transparency not reduced; otherwise the frosted menu is used. UNVERIFIED on a device.
