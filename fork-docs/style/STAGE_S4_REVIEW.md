# Violence Style S4: on-device review checklist

S4 restyles the **bars and controls**: the bottom navigation pill (selected tab as an accent capsule with a glowing bottom line), the mini player, the Search circle, the top-bar pill (from S3), switches, chips (segmented controls) and the player's seek bar. It **extends** the existing Liquid Glass (an accent tint and a rim are handed to it; the glass renderer itself is unchanged). No tab, button, gesture or order changed. With the style OFF everything must be exactly as before.

Build: the phone debug APK from `.\scripts\build-active.ps1 -Target phone` (`app/build/outputs/apk/dev/debug/`).
Screenshots (needs `adb`): `adb exec-out screencap -p > name.png`.

## 0. OFF baseline
Style OFF. Open Home and the bottom bar with a song playing. Look at: the nav pill, the selected tab (plain white-ish capsule), the mini player, a switch (green when on), the seek bar in the player, a chips row (Search tabs "YouTube / Library / Addons"). Capture `off-nav.png`, `off-player.png`, `off-settings.png`.

## 1. Bottom navigation (style ON), compare with `ref2` to `ref4`
| Element | What you should see |
|---|---|
| Pill | A floating fully rounded dark glass pill with margins to the screen edges, a thin rim (white at the top fading to the accent at the bottom), a faint accent tint. Scroll content passes under it with the existing fade |
| Selected tab (Home etc.) | A filled **capsule tinted with the accent** (translucent), icon and label white, with a **bright glowing line along its bottom edge** and a soft wash above it. Blue accent like `ref2`/`ref3`, pink like `ref4`. `ref1` shows a neutral gray capsule: that is what the **White** accent gives |
| Unselected tabs | Gray icon and label |
| Search | The separate round glass button at the right, same rim and tint |
| Behaviour | Tap each tab, drag across the bar, long-press where it works, scroll to fold the bar and back: all as before. The capsule travels between tabs as before and keeps its glow line |

## 2. Mini player (play a song), compare with the mockups' mini player
A full-width pill above the nav pill (or inline when the bar folds) with the same tint and rim; thumbnail, title, artist; play/pause and next as solid white icons. Tap it: the full player opens as before. Pull it up, swipe it, long-press it (share): as before. The accent follows the song (change songs: the capsule, rim and tint fade to the new colour in about a second).

## 3. Controls
- **Switches** (Settings): "on" is the accent colour instead of green, thumb white. Turn several on and off. Pick the Yellow preset: the track is darkened so the white thumb stays visible.
- **Chips / segmented rows** (Search: YouTube / Library / Addons; Violence Style settings): the selected segment is tinted with the accent.
- **Seek bar** (full player): the played part uses the lightened accent. (The play button in the full player and the sliders in Settings are not restyled yet: S5.)

## 4. Accent sources, presets, intensity
Song, Device, Custom; try Blue, Red, Pink, Green, Orange, **Yellow** (icons must stay readable on the capsule), Cyan, Purple and **White** (neutral capsule, no glow line). Intensity Low / Medium / High changes the strength of the glow line and the wash.

## 5. Transparency, motion and power
- **Reduce transparency** (Violence Style settings): the nav pill, mini player and top pill become nearly solid dark with the rim and the accent capsule kept.
- **Reduce dynamic blur** ON, or Liquid Glass OFF: the plain bar is used; it still gets the rim, the tint (when glass is on elsewhere) and the capsule with its glow line. Check the capsule is there.
- **Remove animations** (developer options): nothing in S4 animates by itself; the capsule still moves between tabs like before.
- **Battery saver:** the bars look the same; the background goes still (S2).

## 6. TalkBack
Turn TalkBack on and swipe through the bottom bar: each tab is announced with its name and "selected" for the current one, then the Search button, then the mini player's play/pause and next. Nothing new is announced for the glow line.

## 7. Frame statistics (optional)
With adb: scroll Home for 10 seconds, then `adb shell dumpsys gfxinfo com.yashaswahuh.violencemusic.dev framestats` (reset counters first with `... gfxinfo <package> reset`). Compare style OFF and ON: S4 adds no new layers (the capsule glow is a few strokes drawn once per size); any extra cost comes from S2's moving background behind the glass. Write the numbers down for me.

## 8. OFF parity
Style OFF again; capture the same screens as step 0 and compare: identical. The selected tab capsule must be the old whitish one, switches green, seek bar white.

## 9. Report back
Tick "S4 approved" and "S4 OFF-parity verified on device" in `STYLE_APPROVALS.md` when sections 0 to 3 and 8 pass and 4 to 6 are acceptable. Otherwise tell me which screen, which accent, and send a screenshot.

## Known limits of S4 (not bugs)
- The **play button in the full player** and the **Material sliders in Settings** keep their old look (they are inside the 4,000-line player and many screens; S5).
- **Pressed, focused and disabled states** are not redefined: the tabs keep their existing press ripple and spring, switches keep their existing dimming when disabled, and a focus ring is a TV concern (out of scope). A pressed-scale was not added.
- The glow line is drawn on the capsule at rest and while it travels; during the glass "lift" animation of the travelling capsule it is drawn on the same moving shape.
- The mini player has no progress bar in this app, so there is no accent progress to tint.
- No Compose UI tests exist in this project; tab selection semantics, mini player controls and "nothing was lost" are checked by you in sections 1, 2 and 6.
