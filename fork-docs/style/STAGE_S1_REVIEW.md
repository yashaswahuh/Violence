# Violence Style S1: on-device review checklist

Stage S1 adds the foundation only: the toggle and settings, the accent engine, the fonts, safe mode and a preview card. **Nothing else in the app should look different**, with the style on or off. The app uses the dark theme while the style is on.

You do not have adb or an emulator on this PC; the steps below work with a phone and, optionally, `adb` if you install platform-tools (`adb` is a free Google download). Build artifact: the phone debug APK from `.\scripts\build-active.ps1 -Target phone` (`app/build/outputs/apk/dev/debug/`).

## 0. Before you start (identical-when-off baseline)
1. Install the **previous** build (tag `pre-skin-s1`) or keep screenshots from before. With the new build installed and the toggle OFF, open Home, Explore, Library, Search, Settings, the player and a playlist. They must look exactly the same as before.
2. Capture the baseline (optional, needs adb): `adb exec-out screencap -p > off-home.png` (repeat per screen: `off-explore.png`, `off-library.png`, `off-settings.png`).

## 1. Turn it on
1. Settings > "Advanced & experimental options". The first row is **Violence Style (experimental)** with a short description.
2. Switch it on. Options appear: Background (Aurora, Corner, Haze, Off), Motion (Off, Subtle, Normal), Glow intensity, Accent colour (Song, Device, Custom), Heading font (Bodoni Moda, Cormorant), Reduce transparency, Glow on album and artist pages (no effect yet), a "Mode: Full" or "Mode: Lite (reason)" line, and a **Preview** card.
3. Defaults you should see: Aurora, Normal, Medium, Song, Bodoni Moda, Reduce transparency off. The app switches to the dark theme even if the system or app theme is light.

## 2. The preview card
The card shows a mini background in the chosen style, a coloured dot with its hex code, the words **TOP PICKS** and *for You* in the heading font with a hairline ending in a glowing dot, and a glass "Play" button tinted with the accent.
- **Accent: Song** - play a song with a colourful cover; the dot and button tint take that colour (about a second to fade). Skip songs quickly: no flicker. Play a song with a gray or black-and-white cover: the colour stays as it was (it is held). Nothing playing and no earlier song: the device colour (Android 12 and later) or Blue.
- **Accent: Device** - Android 12 and later: your system accent colour (Settings > Wallpaper and style). Android 11 and older: Blue.
- **Accent: Custom** - tap each of the nine presets (Blue, Red, Pink, Green, Orange, Yellow, Cyan, Purple, White). White gives a light gray, neutral look. Move the three sliders: very dark or gray colours are refused with a red message and the last good colour stays.
- **Background** - Aurora (glow top-left and right), Corner (one glow top-left), Haze (glow in the middle), Off (plain black). Intensity changes how strong the glow is. Compare with the reference pictures: `ref2-blue-aurora.jpg` (Aurora), `ref3-blue-corner.jpg` (Corner), `ref4-pink-haze.jpg` (Haze). The preview is static and small; the real live background arrives in S2.
- **Heading font** - switch between Bodoni Moda (sharp, high contrast) and Cormorant (thinner, softer). Compare with the headings in `ref2`, `ref3`, `ref4` ("TOP PICKS" upright capitals, "for You" italic).

## 3. Fonts and text size
Settings > Display > Font size (and Display size) to the **largest**. Then, with the style ON, open Settings (Advanced), Home, Explore, Library and Search.
- Text everywhere is now Inter (cleaner, a little wider than before). No text may overlap, and no button may become unreachable. Long titles end in "...".
- At the largest size, the preview heading must not break out of the card (it shows on one line or is cut with "...").
- Report any screen where something is cut off or overlaps. Capture it: `adb exec-out screencap -p > big-font-<screen>.png`.
- Compare with the same screens at the normal size (`ref3` shows the intended general feel; the layouts are not supposed to change).

## 4. Confirm nothing else changed with the style ON
- Home, Explore, Library, Search, the player, mini player and nav bar look as before apart from the **font** (Inter) and the dark theme. No background glow yet, no new headings, no new buttons (those arrive in S2 to S4).
- Playback, search, playlists, downloads, Jam, sharing and imports behave as before (play a song, open a playlist, open Friends).

## 5. Safe mode
You cannot easily provoke a crash on purpose. What you can check: turn the style on, close the app with the back button and open it again twice: it stays on (a normal start clears the counter after the first screen). If the app ever fails to start twice in a row with the style on, the third start has the style off, shows a message "Violence Style was turned off ...", and Settings shows a note; switch it on again in the same place.

## 6. Confirm the app is identical when OFF
Turn the style off. The theme returns to what you had chosen (System, Light or Dark; your choice was never overwritten), the font returns to SF Pro, and every screen matches the baseline from step 0. With adb: capture the same screens as in step 0 (`on-off-home.png` and so on) and compare them with the baseline pictures; they should be pixel-identical apart from the clock and live content.

## 7. Report back
Tick "S1 approved" and "S1 OFF-parity verified on device" in `STYLE_APPROVALS.md` only when sections 0, 1, 2, 4 and 6 pass and you are happy with section 3. Otherwise tell me what failed (screen name plus a screenshot if you can).

## Known limits of S1 (not bugs)
- The live background, new headings, rims and glass tinting are S2 to S4. The preview is the only new visual.
- The 17 other languages show these new strings in English.
- The "Glow on album and artist pages" switch does nothing until S5.
- The Mode line can show "Lite" on an older or low-memory phone or with Reduce dynamic blur on; that is expected.
- Frame times were not measured (no device tooling on the build PC).
