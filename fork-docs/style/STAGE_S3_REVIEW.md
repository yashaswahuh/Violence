# Violence Style S3: on-device review checklist

S3 restyles the existing Home screen and the shared components other screens reuse: **section headings, hero cards, the Recents rows and the top bar's glass pill**. Layouts, actions and behaviour are unchanged; nothing was added or moved. With the style OFF everything must be exactly as before.

Build: the phone debug APK from `.\scripts\build-active.ps1 -Target phone` (`app/build/outputs/apk/dev/debug/`).
Screenshots (needs `adb`): `adb exec-out screencap -p > name.png`.

## 0. OFF baseline
Style OFF (Settings > Advanced & experimental options). Open Home, Explore, Library, Search, an album page, Settings. They must look as before S3: SF Pro text, the old headings (plain bold title, no hairline), 12 dp cards, 7 dp row art. Capture `off-home.png`, `off-library.png`, `off-explore.png`.

## 1. Home, element by element (style ON, Aurora, accent Song, Medium)
Compare with the mockups in `fork-docs/style-reference/` (local only): `ref2-blue-aurora.jpg` (blue), `ref4-pink-haze.jpg` (pink; also shows "TOP PICKS / for You"), `ref3-blue-corner.jpg` (corner glow). `ref1-red-arcs.jpg` is the all-caps variant; Arcs and the all-caps heading are not part of S3 (your decisions).

| Element | What you should see | Compare with |
|---|---|---|
| Top heading "Top Picks for You" | "TOP PICKS" in upright serif caps, "for You" in italic on the second line, both white, 36 sp; a thin hairline to the right at the italic line, ending in a small glowing accent dot | ref2, ref3, ref4 |
| "RECENTS" and other shelf titles | Serif caps (24 sp, one line), a plain thin gray hairline to the right, **no dot** | ref2 to ref4 "RECENTS" |
| Hero cards | Rounded about 26 dp, dark gradient at the bottom, small label (sentence case, e.g. "New from Karun"), the title in serif caps, a subtitle, a thin translucent rim. The **first** card has an accent rim with a soft glow and an accent edge along its bottom. The next card peeks in from the right | ref2 to ref4 |
| Recents rows | Art 48 dp with rounder 9 dp corners, white title, softer gray artist line, the three-dot button at the right | ref2 to ref4 rows |
| Top bar | The existing "VIOLENCE" wordmark (your decision) and the glass pill at the right with **all** your actions (Receive, Downloads, avatar): now with a thin rim that fades from white to the accent and a faint accent tint | ref2 to ref4 pill |

Heading font: switch Bodoni Moda / Cormorant in the settings and look again (Cormorant is thinner).

## 2. Accent and intensity
- Accent **Song**: play songs with a blue, a red, a green cover; the dot, the first card's rim and the pill tint follow (about a second to fade). A gray cover keeps the previous colour.
- Accent **Device** and **Custom** (try White: the first card gets **no** coloured rim, the dot is white).
- Intensity Low, Medium, High: the first card's rim bloom gets stronger with it.

## 3. Font size 100 and 200 percent
Settings (Android) > Display > Font size: largest, then back to default. At the **largest** size, on Home and Library:
- Section headings stay on screen: the serif stops growing at 1.3x, long titles wrap to two lines or end with "...", the hairline gets shorter and then disappears; "Show all" (Library, Find your mood) stays tappable.
- Card captions do not overflow the card (3 lines of subtitle at most; long titles end in "...").
- Row titles do not clip. Report any screen where text is cut off or overlaps (screenshot please).

## 4. Everything else that shares these components
Open: Explore (Find your mood, its "Show all"), Library (its shelf headers and "Show all"), Home's other shelves (Quick picks style rows and hero cards), Search. Headings there should have the new look; **song rows in Search, Library lists, playlists and the player are not restyled yet** (S5). Check that tapping a card, long-pressing a card (menu), tapping a row, tapping its three dots, tapping "Show all" and tapping the avatar all still do what they did.

## 5. TalkBack (optional but valuable)
Turn on TalkBack: each section title is announced as a heading ("Recents, heading"), swipe moves heading to heading with the headings control; the hairline and the dot are **not** announced; "Show all" is announced as a button; the three-dot button as "More".

## 6. OFF parity
Style OFF again: capture the same screens as step 0 and compare. They must be identical (apart from live content). Also check the 48 dp three-dot button on Recents is back to its old size.

## 7. Report back
Tick "S3 approved" and "S3 OFF-parity verified on device" in `STYLE_APPROVALS.md` only when sections 0, 1, 2, 4 and 6 pass and 3 is acceptable. Otherwise tell me what failed (screen, accent, font size, a screenshot).

## Known limits of S3 (not bugs)
- **Where the layout prevented an exact match:** (a) the hero heading's hairline is placed at the italic line by a fixed line height (40 dp), so it can sit a few dp off the mockup at other font scales; (b) the hero card width stays at the app's 60 percent of the row (the mockup is about 58 percent: FLEX) and the corner is 26 dp (estimate); (c) rows keep the app's 48 dp art and 4 dp padding, so row spacing can differ a little from the mockups; (d) the top bar has no V mark and no cast button: you chose to keep the existing wordmark and the existing actions; (e) the app's "Top Picks" cards that are drawn artwork (Discovery Station, Made for You) keep their own artwork and only get the rim, scrim and caption.
- Not restyled until S4 and S5: nav bar, mini player, search field, song rows outside Recents, dialogs, the player.
- No Compose UI tests exist in this project (no Robolectric or compose-ui-test dependency); the heading semantics, the 200 percent layout and the click targets are checked by you in sections 3 to 5, and the pure logic behind them by unit tests.
