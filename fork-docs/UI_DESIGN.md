# Custom UI: three design directions (UI-0)

Written 2026-11-02. Design only; nothing here is built. Read with `UI_AUDIT.md` (what exists), `UI_PLAN.md` (how it would be built), and open the boards in `ui-concepts/` (static HTML, one per direction). The owner picks in `UI_DECISIONS.md`.

**Goal.** Keep Liquid Glass, but make a UI that is recognisably Violence's own, easy to use one-handed, accessible, and smooth on a mid-range phone. It is a second UI, switched by a toggle; the current UI is untouched.

**Not copying Apple Music.** The current app is deliberately Apple-Music-like. For a public release the new UI must not reuse that trade dress. Everything below is built on different structure: a dock with a centre orb instead of a four-tab bar with an inline mini player, a violet accent instead of Apple's red, different type, different icons, different player layout. "Inspired by" ideas that remain are generic (translucent controls, large artwork, a bottom navigation), not Apple's specific layouts.

**Constraint check.** Free tools only. Fonts are all SIL Open Font License 1.1 (free to bundle and redistribute; confirm each font's licence file when downloading it, UI-0 did not download anything). Icons: an open set (Lucide, ISC; or Material Symbols, Apache-2.0). Nothing needs a service, an account or a card.

---

## 0. The Violence identity (used by all three)

| Token | Dark | Light | Contrast (computed, WCAG formula, solid colours) |
|---|---|---|---|
| Background | `#0B0B12` Void | `#F6F4FB` Paper | |
| Surface | `#14121D` | `#FFFFFF` | |
| Glass tint | `#14121D` at 40 to 55 % | `#EDE9F7` at 55 to 70 % | |
| Ink (primary text) | `#F4F2FA` | `#15131F` | on Void about 17:1; on Paper about 17:1 |
| Ink-2 (secondary text) | `#A7A5B8` | `#5B586B` | 8.14:1 on Void; 6.31:1 on Paper; 5.76:1 on light glass `#EDE9F7` |
| **Voltage** (accent fill) | `#7C3AED` | `#7C3AED` | white on it 5.70:1 |
| Voltage text/icon | `#C4A8FF` | `#6D28D9` | 9.70:1 on Void; 6.51:1 on Paper |
| Ember (alerts, like) | `#FF6B8A` | `#B4234A` | 7.21:1 on Void; 5.87:1 on Paper |
| Mint (Jam / live) | `#5EEAD4` | `#0F766E` | 13.26:1 on Void; 5.02:1 on Paper |
| Focus ring | `#C4A8FF` 3 dp | `#6D28D9` 3 dp | 3:1 or better against any neighbour |

Notes. Voltage `#8B5CF6` was rejected as a fill: white on it is 4.23:1 (below 4.5:1). The rim highlight and UI outlines must hit 3:1 against what they sit on; `#8B5CF6` on Void is 4.63:1. These ratios are for solid colours; glass over artwork is handled by the safeguards above.

**Adaptive colour from album art.** Each song yields one accent and one wash from the artwork palette (the app already has `ArtworkPalette.kt` and mesh code to build on). Safeguards, applied in this order, so text never depends on the art:
1. Take the dominant and a secondary colour; convert to OKLCH; clamp chroma so it never exceeds a cap, and keep lightness within a band per theme.
2. Compute the contrast of the theme's text colour against the *worst* colour of the area behind it (sample the region on an 8 by 8 grid, take the lightest cell in dark theme and the darkest in light theme). If below 4.5:1 (3:1 for large text and icons), add a scrim whose alpha is solved to reach the target (never more than 0.72); if even that fails, fall back to a solid surface for that element.
3. The accent used for buttons or progress must reach 3:1 against its neighbours and 4.5:1 for any text on it; otherwise use Voltage.
4. Tinting is state, not decoration: a control's state is never shown by colour alone (a shape, glyph or label also changes).

**Type.** All free fonts are bundled as resources (no download at runtime). Non-Latin scripts (Hebrew, Hindi, Thai, CJK, Cyrillic where a font lacks it) fall back to the system font; the type scale is the same.

| Role | A: Lumen | B: Aurora | C: Tide |
|---|---|---|---|
| Display / titles | Sora (OFL 1.1) | Bricolage Grotesque (OFL 1.1) | Figtree (OFL 1.1) |
| Body / UI | Inter (OFL 1.1) | Inter (OFL 1.1) | Figtree (OFL 1.1) |
| Numerals (time, counts) | Inter tabular figures | Inter tabular figures | Figtree tabular figures (UNCLEAR if the font ships them; fall back to Inter) |

Type scale (sp), identical for all three; every size grows with the system font size to 200 % and layouts reflow rather than clip: Display 32 / Title 22 / Heading 18 / Body 16 / Label 14 / Caption 12 (nothing smaller than 12 sp). No text sits in a fixed-height container (use minimum heights); rows become two-line; chips wrap; at font scale 1.6 and above the dock/bar switches to icon-over-label with labels allowed two lines, and a "Menu" list replaces any control that no longer fits.

---

## 1. Glass system (shared vocabulary)

Four levels, mapped onto code that already exists, so upstream performance fixes carry over:

| Level | Name | What it is | Existing primitive |
|---|---|---|---|
| G0 | Solid | Opaque surface, hairline edge | `reduceDynamicBlur` branch of `liquidGlass()`; plain `Surface` |
| G1 | Tint | Tint plus rim light plus hairline, no capture | `lightweightLiquidGlass`, `liquidGlassRim` |
| G2 | Frost | Blurred still or live backdrop, tint, rim, no lens | `SnapshotBackdrop`/`menuBackdropFrost`, or `liquidGlass` without the lens (API 31 and 32) |
| G3 | Liquid | Blur plus edge lens refraction plus highlight plus shadow | `liquidGlass()`, `AppGlass.kt` panel glass |

Budget (all directions): at most **3 live backdrop surfaces** on screen at once (dock, one floating control cluster, one panel), plus any number of G0 and G1 surfaces; **no glass inside scrolling lists**; a G3 surface covers at most about a third of the screen; stills are quarter-scale and live backdrops one-third; blur radius 8 to 18 dp. Solid surfaces are always used for: body text over 3 lines, forms and text entry, settings, anything inside a list, and anywhere the contrast safeguard fails.

Tiers, chosen automatically (and overridable by the user): **Full** (G3 where the direction calls for it), **Simplified** (G2 for floating bars, G1 elsewhere, no lens), **Solid** (G0 everywhere). Details in section 5.2.

---

## 2. Direction A: "Lumen"

**Concept.** A floating dock of light. Everything you do often lives in the bottom 40 % of the screen, and the one glowing circle in the middle is your music: the orb. Tap it and the song opens out of it; hold it and it fans out quick actions. The top of the screen stays quiet, so artwork and titles carry the page. Glass is used where it matters (dock, orb, player panels, hub) and nowhere else.

**Navigation model.**
- *Dock* (G3, floating, 16 dp above the bottom inset): Home, Explore, **orb**, Library, Search. Five 48 dp targets with labels; the orb is 64 dp.
- *Orb* = the mini player. With music: album art in a progress ring; tap opens the player, swipe up opens it, swipe sideways skips, hold opens a quick-action fan (Like, Add to Jam, Share (tap/QR), Queue). With nothing playing: "Play my mix". Every gesture has a visible button inside the opened player; the orb's fan items are also in the player's menu.
- *Hub*: the account avatar at the top right opens a glass sheet from the bottom: Profile and Friends, Jam, Receive (NFC/QR/link), Downloads, Recent shares, History, Replay, Imports (Spotify, Apple Music), Settings. The Library page also carries a "You" row with the same destinations, so nothing depends on reaching the top corner one-handed.
- *Deep screens* (album, artist, playlist, Settings pages): pushed with a shared-element hero. A back chip sits at the **bottom left** above the dock (thumb zone) and the system back gesture works; the dock stays on release pages and tucks to the orb alone on Settings.
- *Thumb zone*: dock, orb, back chip and the Hub's primary rows are all in the lower half. Search is a tab, not a top field; the keyboard opens with the field above it.

**Glass system.** G3: dock, orb, Hub sheet, dialogs. G2: quick-action fan, player queue/lyrics panels (stills). G1: chips (Jam presence, filters). G0: all pages, lists, settings, forms. Live surfaces on screen: dock + orb (one shared surface, the orb is cut into the dock) + at most one panel = 2 to 3.

**Colour and type.** Violence palette above; the dark theme is the hero. Home headers take a faint wash of the playing song's accent (alpha 0.12 on the page background only, under the contrast rules). Sora titles, Inter body.

**Motion.** Springs only for movement (damping 0.82, stiffness 380 for the orb and dock; 0.9/260 for sheets); fades 120 to 180 ms. Shared-element: orb art ⇄ player art; card ⇄ release hero. Never animated: the dock while a list scrolls (it only collapses after a deliberate scroll, as today), text, anything while idle. The orb's progress ring updates once a second, not per frame.

**Signature interactions.**
1. *Orb morph*: the player does not slide up as a sheet; it grows from the orb (art scales and rounds out, the dock dips). A 6 dp "grab line" shows it can be dragged.
2. *Jam presence chips*: while in a Jam, each member is a small glass chip (preset avatar, colour, first letter for unknown avatars) under the song title; a chip pulses once when that person adds a song or skips; tap opens the members sheet; the host's chip has a crown glyph (plus a text label for non-visual users).
3. *Quick-action fan* from holding the orb or any song row (three to four actions, big targets, visible alternative in the row's ⋮ menu).
4. *Dock peek*: pulling the dock upward reveals the queue strip (next three songs) as a glass shelf.
5. *Receive anywhere*: a "Receive" action in the Hub and in the fan; the NFC read mode needs the Receive screen to be open, so it opens that screen directly.

**How features are surfaced.**
- Jam: chips in the player; "Jam" row in the Hub; the invite sheet from the fan. Members sheet and queue unchanged underneath.
- Profile and friends: avatar in the dock-adjacent header chip when in a Jam; Profile/Friends in the Hub; the "add you back" prompt after adding a friend stays.
- Sharing (NFC, QR, link): fan → Share; the share sheet is the existing logic with the new glass wrapper.
- Imports: Hub → Imports (Spotify, Apple Music) and Library → "Bring your music".
- Downloads: a ring on the avatar while downloading; Hub → Downloads; Library → Downloads.
- Update flow, alternate app icon, language: Settings (unchanged pages, re-skinned).

**Pros.** Closest to existing code (the tab bar already morphs between inline and expanded; shared elements already used); lowest glass cost of the glass-forward designs; strong one-handed reach; a memorable centre object. **Cons.** A centre orb competes with the usual "search at the top" habit; five targets need care at 200 % font; the Hub concentrates many features behind one tap, so labels and order must be good. **Performance cost:** 2 to 3 live surfaces, a bit under the current UI's six-at-once worst case.

---

## 3. Direction B: "Aurora"

**Concept.** The whole app is lit by the music. A soft aurora of the playing song's colours sits behind every page, and content rides over it on frosted sheets that bend the light at their edges. Navigation collapses to one pill; the player is a full-screen scene. The most expressive and the most expensive.

**Navigation model.**
- *Pill* (G3) at the bottom holds a swipeable destination rail: Home, Explore, Library, Search. Swipe the pill sideways to change tab (the tabs are also tapped, labelled and in the accessibility tree), and a mini-player strip lives above it.
- *Sheets*: lists live on large frosted sheets (G2) that slide over the aurora; sheets stack for deeper screens (album over Library), each with a visible handle and a back chip at the bottom left.
- *Player* is a takeover scene; lyrics, queue and Jam are cards you swipe between, with page dots and labelled buttons for the same.
- *Hub*: long-press the pill or tap the avatar chip at the top right.
- Thumb zone: pill and chips low; scenes use top-to-bottom drags only as accelerators.

**Glass system.** G3: pill, player controls cluster, one hero sheet header. G2: content sheets (large), menus. G1: chips. G0: forms, settings, long text. Live surfaces: pill + one or two large sheets = 3, with large areas (more fill rate than A).

**Colour and type.** The aurora is the palette: Voltage as the default wash when nothing plays, the song's colours otherwise. Strict contrast scrims under all text on sheets (section 0). Bricolage Grotesque titles (characterful), Inter body.

**Motion.** Slow parallax of the aurora layers (only while playing, screen on, foreground, reduce-motion off, not battery saver); sheet spring 0.88/300; shared hero transitions between cards and scenes; a beat-less "breathing" scale (no audio analysis) on the artwork while playing, off when paused.

**Signature interactions.** *Jam halo*: members' colours form a ring of light around the artwork (with chips and text names for non-visual users); *refracting sheets* (the lens at sheet edges); *peel back*: pulling a stacked sheet down reveals the previous screen like a page corner; *lyrics as light*: the current line lifts and brightens while others fall into the aurora (contrast rules still apply).

**How features are surfaced.** Jam: halo + card in the player + Hub. Profile/friends, sharing, imports, downloads: Hub, long-press menus on cards, and a "Send" button on the player scene; Receive on the pill's long-press and in the Hub.

**Pros.** The most distinctive and "gorgeous" in screenshots; strongest identity. **Cons.** Highest GPU and battery cost: an animated full-screen layer plus large frosted sheets; contrast is hardest to guarantee on bright art; sheets are large so they cost fill rate; the swipe-the-pill gesture needs the visible alternative; the most new code (a custom page container). **Performance cost:** high; on the Simplified tier it becomes a static wash and G1 sheets, which loses most of its identity.

---

## 4. Direction C: "Tide"

**Concept.** Calm, fast and reachable. Pages are solid and legible; glass is kept for the moments that deserve it (the player, menus, the command bar). A single command bar combines search, navigation and "do anything": type "jam", "friends", "downloads", a song, a `violence://` link or a Jam code, and it takes you there. Designed to be the lightest and the most accessible.

**Navigation model.**
- *Command bar* (G2) at the bottom: three destination icons (Home, Explore, Library) and a wide search field. Tapping the field expands it upward into a command palette: recent items, quick actions (Receive, Jam, Friends, Downloads, Settings, Replay, Imports), results as you type. Pasting or scanning a link opens the existing incoming-link confirmation.
- *Now chip*: a small capsule above the bar's left end (art, title, play/pause). Tap expands it into a card with transport and queue peek; swipe up opens the full player; visible "expand" button on the card.
- *Deep screens*: ordinary pushed pages with a bottom-left back chip; the bar stays.
- *Landscape/tablet*: the bar becomes a left rail (icons + labels), the Now chip sits at the rail's bottom.
- Thumb zone: everything is on the bottom bar; the top has titles only.

**Glass system.** G2 for the command bar and Now chip, G3 only for the full player's transport cluster and menus, G1 chips, G0 for every page. Live surfaces: bar + chip + at most one panel = 2 to 3, all small.

**Colour and type.** Violence palette with Voltage used sparingly (selection, progress). Pages are flat Void/Paper with the art-derived accent used only on the playing song's row and the Now chip. Figtree throughout (one family keeps it quick and small).

**Motion.** Short fades and small springs (0.9/420); no parallax; the palette expands with a 220 ms spring; nothing runs while idle.

**Signature interactions.** *Command palette* (search plus actions plus links in one field); *contextual ribbon*: after a long-press on a song, a ribbon above the keyboard or bar offers Play next, Add to Jam, Share, Like; *haptic progress*: scrubbing ticks every 10 s; *Now chip peek* showing the next song title.

**How features are surfaced.** Everything is reachable by name in the palette (a built-in complete feature index), which is also the best screen-reader and switch-access path. Jam: chip in the Now card, palette entry, invite from the ribbon. Profile/friends, sharing, imports, downloads: palette entries and the existing pages.

**Pros.** Lowest cost and the best accessibility, contrast and battery; the palette makes every feature one query away; easy to build and to keep in step with upstream. **Cons.** The least "liquid" and the closest in structure to the current app, so it is the least distinctive; a command palette is less discoverable for casual users (needs a visible hint); fewer showcase moments. **Performance cost:** lowest; the only constant live glass is two small surfaces.

---

## 5. Requirements shared by all directions

### 5.1 Accessibility

- **Contrast.** Text at least 4.5:1 over any glass or image (3:1 for large text, 18 sp and up or 14 sp bold, and for UI components and icons), computed in code from the tokens and the sampled backdrop (section 0, safeguards); unit tests cover the token palette in both themes; a failing element falls back to a solid surface.
- **Touch targets** at least 48 dp square (including the dock, top controls (today 44 dp), chips and the profile avatar), with spacing of at least 8 dp between adjacent targets.
- **TalkBack.** Every control has a label (decorative images are explicitly marked decorative); headings use `heading()`; toggles expose `stateDescription` and `Role`; focus order follows reading order, and the player's order is: song title, artist, progress, transport, secondary actions; live updates (Jam member joined, song changed) use polite live regions. Every gesture (swipe orb, swipe pill, hold fan, pull dock, swipe rows) has a visible control or menu entry, and when touch exploration is on, gestures that conflict with TalkBack (swipes) are replaced by those controls automatically.
- **Font scaling to 200 %.** No fixed-height text containers; reflow rules in section 0; tested at 100, 150, 200 % on a small phone (360 dp wide) and a large one.
- **Motion.** Honour the system "Remove animations" (animator duration scale 0) in addition to a new in-app setting; with either on: no parallax, no spring overshoot, fades only, mesh/aurora static.
- **Transparency.** A new in-app "Reduce transparency" switch forces the Solid tier. High contrast: use `UiModeManager.getContrast()` (API 34) where present, and bold text via `Configuration.fontWeightAdjustment` (API 31). The system "high contrast text" flag is not a public API (UNVERIFIED), so it is not relied on. High contrast raises tint alpha, thickens the rim and forces solid surfaces for text.
- **Not by colour alone.** Selected tab, like, Jam host, download state, errors all pair colour with a shape, glyph or text.
- **Orientation and size.** Portrait first; landscape and tablets supported by window size classes (compact, medium, expanded): dock → rail from medium width, player two-column from expanded or landscape, foldable half-open postures keep the player controls on the lower half (UNCLEAR if posture APIs are available on the target phones; fall back to size classes).
- **Language.** All 17 shipped locales including Hebrew (RTL): the dock, orb gestures and back chip mirror.

### 5.2 Performance

- **Frame budget.** Reference device: a mid-range phone (Snapdragon 6/7-series or Dimensity 7000-class, 6 GB RAM, Android 13). UNCLEAR what the owner's phones are; the budget is set against that class. Target: at 60 Hz p95 frame time at most 12 ms and fewer than 3 % janky frames while scrolling Home with the glass on; the player opening and the orb morph at least 55 fps; at 90 or 120 Hz the UI must still hold 60 fps, and the app asks for a lower rate while glass is moving if it cannot hold the higher one (the existing `highPerformanceMode` path). Measured with the existing `FrameStats` (`adb logcat -s VioFrames`) in the same procedure as before: scroll Home 5 s with glass off, then on, compare p95/p99.
- **Layers.** At most 3 live blurred surfaces, 1 additional still per open panel; lists carry no glass; backdrops at one third resolution, stills at a quarter; effect chains remembered; a surface that is fully covered or off screen records nothing.
- **Tiers and automatic fallback.** Full → Simplified → Solid, chosen at start and re-evaluated when conditions change: API below 31 → Solid (or Haze frost, as today); API 31 to 32 → Simplified (no lens); `ActivityManager.isLowRamDevice` → Simplified; battery saver (`PowerManager.isPowerSaveMode`) → Simplified; thermal status severe or worse (`PowerManager.getCurrentThermalStatus`, API 29) → Solid; sustained jank (for example p95 over 20 ms across 10 s of scrolling, measured with `FrameMetrics`) → step down one tier with a one-line notice and a way back in Settings. The user can pin a tier.
- **Idle.** No continuous animation while the screen is idle: the aurora/mesh drift and any glow only run when the song is playing, the app is foreground, the screen is on, and reduce-motion and battery saver are off; the progress ring updates at 1 Hz; stills are released after use.
- **Memory.** Stills are small (a quarter-scale bitmap) and released when their panel closes (existing `AppStill` behaviour).

### 5.3 Robustness

- **Same state holders, same service.** The new UI reads `MainViewModel`, `AppSettings`, `ListenTogether`, `ProfileManager`, `Downloads`, `IncomingLinkHub` and the same `PlaybackService` controller; it adds no second source of truth. Playback runs in the service, so a UI problem cannot stop the music.
- **Switching never interrupts playback or loses the queue.** The queue and position live in the service; the toggle swaps chrome inside the existing composition (see `UI_PLAN.md`), so the media controller is not recreated and the player's open or closed state is kept.
- **Safe mode.** Two startup crashes in a row with the new UI on revert to the current UI and tell the user (`UI_PLAN.md`).
- **Features stay reachable.** Every feature in the brief appears in the direction's reachability table; UI-1 adds a test that lists the destinations and fails if one disappears from the new UI.

### 5.4 Where each feature lives (all three)

| Feature | A: Lumen | B: Aurora | C: Tide |
|---|---|---|---|
| Playback, queue, lyrics | Orb → player panels | Mini strip → scene cards | Now chip → card → player |
| Search | Dock tab | Pill tab | Command bar field / palette |
| Library, playlists | Dock tab; Library → You | Pill tab | Bar icon |
| Downloads | Hub, Library, avatar ring | Hub, sheet header chip | Palette, Library |
| Lyrics (sources, offset, translate, share) | Player panel menu | Lyrics card menu | Player menu |
| Jam | Chips, Hub, fan | Halo, card, Hub | Now card, palette |
| Profile, friends | Hub | Hub | Palette |
| Sharing (NFC, QR, link), Receive | Fan, Hub | Send button, pill long-press | Ribbon, palette |
| Imports (Spotify, Apple Music) | Hub, Library | Hub | Palette |
| Settings, icon picker, update flow | Hub | Hub | Palette |
| Replay, History, Recent shares | Hub | Hub | Palette |

---

## 6. Recommendation

**Build A "Lumen", with two borrowings: Tide's rule that pages stay solid and glass is for focus layers, and Aurora's colour wash on the Now Playing scene only (using the mesh the player already draws).**

Why:
- *It fits the code.* The app already has a floating bar that morphs between an expanded and an inline form, shared-element transitions and a mini player pull gesture; Lumen's dock and orb are an evolution of those, not a new engine. Aurora would need a new page container, an always-on animated layer and large live glass.
- *It is the most "liquid" per unit of cost.* Two or three live surfaces give it a clear identity without the six-at-once peak the owner already found laggy.
- *It is one-handed by construction* and gives the owner a signature object (the orb) that is not Apple's layout.
- *Tide's solid pages and tiers are the performance and accessibility floor.* They also become the Simplified and Solid tiers, so the lower tiers are a designed look, not a degraded one.
- *Risk to watch:* the five-target dock at 200 % font scale; the plan includes the large-text layout and a test for it.

If the priority is battery and accessibility above visual impact, pick C. Choose B only if a visibly higher GPU cost is acceptable and the Simplified tier is allowed to look much plainer.

---

## 7. Fonts and assets (all free)

| Item | Licence | Use |
|---|---|---|
| Sora | SIL OFL 1.1 | A titles |
| Bricolage Grotesque | SIL OFL 1.1 | B titles |
| Figtree | SIL OFL 1.1 | C all text |
| Inter | SIL OFL 1.1 | A and B body, numerals |
| Lucide icons (or Material Symbols) | ISC (Lucide) / Apache-2.0 (Material Symbols) | All icons, replacing the SF Symbols port and hand-drawn strokes where needed |
| Concept boards | Hand-written HTML, CSS and inline SVG; system font stacks, no files | Boards only |

Each OFL font's licence text must be added to `NOTICE` (or a `licenses` note) when bundled. The currently bundled SF Pro Display and the SF Symbols port are flagged in `UI_AUDIT.md` section 5 and should not appear in the new UI.
