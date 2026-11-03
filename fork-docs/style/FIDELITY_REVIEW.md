# Violence Style: fidelity review (S6)

Compared: the mockups in `fork-docs/style-reference/` (local only), the owner's on-device screenshots seen during S2 to S5 (Search, Home, Now Playing, the nav bar; not stored in the repository), the code, and the S1 to S5 review notes. No device screenshots were placed in `fork-docs/style/device-screenshots/` (the folder is gitignored in case the owner adds some). Nothing here was measured pixel by pixel.

| Element | Mockup | Implementation | Deviation, cause | Status |
|---|---|---|---|---|
| Background: Aurora, Corner glow, Haze | soft accent glows, near-black base, fading to black | the three styles built from radial gradients (S2); brightness raised 25 percent and motion made visible after the owner's first review | the glow is capped by the contrast guard, so High is slightly dimmer where it would hurt text | matches; owner approved S2 |
| Background: Arcs (`ref1`) | thin curved light streaks | **not built** | owner decision (Arcs dropped) | by decision |
| Top bar: wordmark and pill | bold white "V" at the left, glass pill with cast and avatar | the existing "VIOLENCE" wordmark; the glass pill holds the existing actions (Receive, Downloads, avatar) with accent tint and rim | owner chose to keep the wordmark; the app has no cast action and a new one would be a new feature | by decision |
| Section headings | serif caps, italic second line, hairline, glowing dot (first heading) | same (S3); the hero heading has the dot, the others a plain hairline | the all-caps variant of `ref1` was not offered as a setting (owner chose Mixed) | matches `ref2` to `ref4` |
| Hero cards | about 26 dp corners, scrim, label, serif caps title, subtitle, rim; accent rim light and bloom on the first card, next card peeks | same (S3); the rim light now follows the card at the front of the row | card width stays at the app's 60 percent (mockup about 58, flexible); corner radius is an estimate | matches |
| Hero card art cards (Discovery Station, Made for You) | own artwork | keep their own artwork plus rim, scrim, caption | none | matches |
| List rows (Recents) | about 48 dp art, 9 dp corners, white title, gray artist, three dots | same (S3) | rows outside Home's Recents (Search, Library, playlists) keep the old corner and text style | partial (S5 coverage) |
| Mini player | floating dark glass pill, thin rim, solid white transport icons | the existing glass pill with the accent tint and rim (S4) | the app has no progress bar in the mini player, so no accent progress | matches |
| Bottom navigation | floating glass pill, selected tab as an accent capsule with a glowing bottom line, round Search button | same (S4); the capsule glow is clipped to the capsule after the owner's report | the mockup's capsule is a vertical gradient; ours is a flat translucent accent with a bottom wash | close; judged acceptable |
| Buttons, switches, chips, seek bar | glass buttons, accent states | switch track, chip segment and seek bar take the accent (S4); the play control is unchanged (the ring was removed after review) | Material sliders in Settings and the full player's other buttons are not restyled | partial (S5 coverage) |
| Dialogs and sheets | not in the mockups | dark glass with a thin rim, faint accent tint, accent buttons (S5) | `AlertScaffold` and Haze menus keep the old surface | partial |
| Typography | display serif headings, Inter body | Bodoni Moda, Cormorant Garamond or Anton for headings, Inter body | Anton has no italic: the italic line is a synthesised slant | matches; Anton by request |
| Dark only | dark | the skin forces its dark look while on (a light version is deferred by decision) | none | by decision |

## Fixed during S6
- None of the visual fidelity items needed a code change in S6; the visual fixes came from the owner's device reviews in S2 to S5 (glow brightness and motion, black search rows, capsule rectangle, High-glow motion, neutral accent for black and white covers, rim following the front card, play ring removed).

## Still open (need the owner's decision or a device)
1. Whether the player should get a serif title and a live glow (S5 left it alone).
2. Whether Settings group cards should become glass.
3. Whether the hero card width should be reduced from 60 to 58 percent.
