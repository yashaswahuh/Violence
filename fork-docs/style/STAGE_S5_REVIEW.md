# Violence Style S5: screen-by-screen on-device review

S5 is a **rollout**: it carries the style to dialogs, sheets, the player and the album pages through shared pieces, and records in `STYLE_COVERAGE.md` what each screen has and lacks. Layouts, controls and behaviour are unchanged; with the style OFF everything must be as before.

Build: the phone debug APK (`.\scripts\build-active.ps1 -Target phone`). Screenshots: `adb exec-out screencap -p > name.png`.

## How to run this review
For each screen below take an OFF screenshot (`off-<screen>.png`) and an ON screenshot with each of: accent **Song**, **Device**, **Custom (Red)** and two backgrounds (Aurora and Haze). You do not need all combinations for every screen: do the full set on Home, Settings, the player and one dialog; one ON and one OFF for the rest. Mark each row OK or describe what is off.

## What is new in S5 (check these first)
| What | Where | Expect |
|---|---|---|
| Dialog and sheet look | Any dialog (language, delete confirm, update, playlist picker, share sheet), bottom sheets | Dark translucent glass with a thin rim, a hint of the accent in the tint, accent-coloured text buttons. Reduce transparency makes them nearly solid. All text and buttons unchanged and readable |
| Play control | Full player | **No ring** (it was removed after your review): the play/pause button looks as before. The seek bar uses the accent |
| Rim light on Top Picks | Home | The accent rim light sits on the card at the front of the row and moves to the next card as you swipe past halfway |
| Glow above the artwork | Album page, playlist page, (artist page: mostly hidden by the photo) | A soft accent glow at the top edge over the page colour, no clock. Turn the switch "Glow on album and artist pages" (Violence Style settings) off: the glow goes |
| Search rows | Search results and Recent searches | No black bars (from the earlier fix); swiping a row right still shows Queue |

## Screen list
| Screen | ON check | Done |
|---|---|---|
| Home | glow, serif headings, Top Picks, Recents, nav, mini player | |
| Explore (+ a mood) | glow, "Find your mood" header and Show all | |
| Library (+ Show all) | headers, tiles still work | |
| Search (type, filters, results) | chips accent, rows see-through | |
| Now Playing: artwork, controls, volume, lyrics, queue, output, Jam pill, ... menu | seek accent (no ring on play), everything readable over bright and dark covers | |
| Album / playlist / artist page | glow at top, track list readable | |
| Downloads, History, Equalizer + presets | readable, switches accent | |
| Settings home + every category (Downloads, Playback, Appearance, Performance, Local music, Storage, Your data, Miscellaneous, Language, Advanced) | rows readable, the Violence Style block easy to find in Advanced | |
| Accounts, Sources, Spotify, Discord, Wear OS page | readable | |
| Jam (create, join, members, queue, server editor) | readable; Jam join still works | |
| Profile, Friends, Receive, Recent shares, Tap to share, QR scanner | readable; a shared link still opens the confirmation | |
| Spotify / Apple Music import and review pages | dialogs in glass; import still runs | |
| Update dialog, icon picker, About (attribution intact) | readable; the fork attribution unchanged | |
| Replay, onboarding, YT Music / Spotify login | unchanged (excluded on purpose) | |

## Accessibility and performance
- Font size to the largest: open Home, Settings, a dialog and the player; nothing may be cut off that was not before.
- TalkBack: swipe through the bar and a dialog: order unchanged, buttons announced as before.
- Reduce transparency ON: dialogs, sheets, nav and top pill become solid dark. Reduce dynamic blur ON: same plus the background goes still.
- Scroll a long list (Library songs) with Normal motion: no stutter beyond S2's baseline. Frame stats if you like: `adb shell dumpsys gfxinfo com.yashaswahuh.violencemusic.dev framestats`.

## OFF parity
Style OFF again: capture the same screens and compare with the first OFF screenshots; identical. A dialog must look exactly as before (no rim, original tint, white buttons).

## Report back
Tick "S5 approved" and "S5 OFF-parity verified on device" in `STYLE_APPROVALS.md` when the "new in S5" items and the OFF checks pass and nothing is unreadable anywhere. List everything that looks off (screen, accent, background, screenshot) and I will fix it before S6.

## Known limits of S5 (not bugs)
See `STYLE_COVERAGE.md`: the player's serif title, the player's own background, `AlertScaffold` account alerts, the Haze menus, other shelf cards and some one-off buttons are not restyled; Replay, onboarding and login screens are excluded on purpose. No automated screen-open smoke tests exist in this project.
