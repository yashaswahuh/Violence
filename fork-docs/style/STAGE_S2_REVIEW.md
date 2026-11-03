Z# Violence Style S2: on-device review checklist

Stage S2 adds the **live background** behind the app: three styles (Aurora, Corner, Haze; Arcs was dropped by you), drifting slowly, tinted by the accent. Everything else stays as in S1: new headings, glass tinting and rims come in S3 and S4. With the style OFF the app must be identical to before.

Build: the phone debug APK from `.\scripts\build-active.ps1 -Target phone` (`app/build/outputs/apk/dev/debug/`, install the `arm64-v8a` one on most phones).

## 0. Baseline (style OFF)
Turn the style OFF (Settings > Advanced & experimental options). Open Home, Explore, Library, Search, Settings, the player, an album page. They must look exactly as before S2. Optional screenshots (needs `adb`, a free Google download): `adb exec-out screencap -p > off-home.png` and the same for the other screens.

## 1. The four-style comparison (style ON)
Settings > Advanced > Violence Style: ON. For each Background option, go to Home and compare with the reference picture (they are in `fork-docs/style-reference/`, local only):

| Background | Compare with | What to look for |
|---|---|---|
| Aurora (default) | `ref2-blue-aurora.jpg` | Glow top-left and a second at mid-right, fading to black at the bottom |
| Corner | `ref3-blue-corner.jpg` | One strong glow in the top-left corner, the rest dark |
| Haze | `ref4-pink-haze.jpg` | A wide soft glow behind the header and the middle, darker toward the bottom |
| Off | plain black | Nothing drawn |

(Arcs and `ref1-red-arcs.jpg` are not part of S2: you dropped Arcs.)

Try each style with: Accent **Song** (play songs with a blue cover, a red cover, a green cover, a black-and-white cover), Accent **Device**, Accent **Custom** with Blue, Red, Pink, Green, Orange, Yellow, Cyan, Purple and White. Pink with Haze should look like `ref4`; Blue with Aurora like `ref2`. Try Low, Medium and High intensity.

Judge: is the glow soft (no visible edges or banding)? Is the brightness close to the mockups? Is all text readable everywhere, including song artist lines at the mid-right edge? Report any place where it is not.

## 2. Motion and smoothness
- Motion **Normal**: watch for 30 seconds. The glow drifts very slowly, never jumps or stops. **Subtle**: slower and smaller. **Off**: completely still.
- Scroll Home up and down for a minute with Normal motion, Aurora and High intensity: no stutter you can feel. If you do feel it, tell me the phone model, whether Liquid Glass is on, and which style.
- Frame statistics (optional, with adb): scroll Home for 10 seconds and run `adb shell dumpsys gfxinfo com.yashaswahuh.violencemusic.dev framestats`. Simple reading: near the top it prints "Total frames rendered", "Janky frames" and a percentage, plus 50th, 90th, 95th and 99th percentile frame times in ms. On a 60 Hz phone a smooth app has the 90th percentile under about 16 ms and fewer than 5 percent janky frames. Write the numbers down for me; run it once with the style OFF and once ON (same screen, same scroll) to see what the style costs. (`...dev` is the package of the dev debug build; the release package is `com.yashaswahuh.violencemusic`.) You can also reset the counters first with `adb shell dumpsys gfxinfo com.yashaswahuh.violencemusic.dev reset`.

## 3. Switching songs and accents
- Accent **Song**: skip songs quickly: the glow colour changes smoothly (about a second) and never flickers. A song with a gray cover keeps the previous colour.
- Change the accent source or the preset in Settings: the colour fades.
- Turn the style OFF and ON again while a song plays: the music never stops, the app does not restart.

## 4. Power and motion rules
- **Battery saver** on: the Settings line says "Mode: Lite (battery saver is on)" and the background is still (it does not drift).
- Android's **Remove animations** (Developer options > Animator duration scale = off, or Settings > Accessibility > Remove animations): the background is still. (Without developer options, skip this check.)
- Leave the app (home button) for a minute and return: it is fine and does not jump. Switch the screen off and on: same.
- Open the full player: the page behind is not animating (the clock stops while the player covers the page).
- Reduce transparency ON (Violence Style settings) or Reduce dynamic blur ON: the glow becomes a very dark, subtle tint of the accent at the top, still.

## 5. Every screen type (is anything opaque, unreadable, or odd?)
Open each of: Home, Explore, Library, Search (type something, look at the "Top result" card: it should have no black block; the song rows below keep a plain dark strip by design), Settings and a settings category, Downloads, History, Profile, Friends, Receive, Jam, an album page, an artist page, a playlist page, the Replay page, the equalizer. Note: **album, artist and playlist pages and Replay paint their own artwork colours, so they show no glow in S2** (S5 decides). Dialogs and sheets are separate windows and show a dark still: that is expected.
Check with Liquid Glass **on** and **off** (Settings > Appearance): with it on, the glass bars at the bottom and the top-right pill should show a blurred version of the glow behind them, and the glow behind the bars must line up with the glow on the page (a shifted or doubled glow near the bars is a bug: send a screenshot).

## 6. Rotate the phone
Rotate to landscape and back on Home and on the player. The glow stretches to the new size, nothing flashes white, nothing crashes.

## 7. OFF parity
Turn the style OFF again. Capture the same screens as in step 0 (`on-off-home.png` ...) and compare: pixel-identical apart from the clock and live content. The font must be SF Pro again and the page fully black as before.

## 8. Report back
Tick "S2 approved" and "S2 OFF-parity verified on device" in `STYLE_APPROVALS.md` only when sections 0, 1, 3, 5 and 7 pass and you are content with 2 and 4. Otherwise tell me what failed (screen, style, accent, Liquid Glass on or off, and a screenshot).

## Known limits of S2 (not bugs)
- Arcs is not available (your decision). The background does not react to scrolling (a parallax shift was skipped: see `STYLE_PLAN.md`).
- Album, artist, playlist pages and Replay show no glow yet. The top status-bar fade and bottom fade still fade to plain black while you scroll.
- Heading fonts, glass tinting and rims are still S3 and S4. Strings are English only in other languages.
- Frame times were not measured on a device by me (no adb on the build PC); the numbers in `STYLE_PLAN.md` are reasoning, yours are the real ones.

## Update after your first device review (2026-11-03)
- Glow peak brightness raised about 25 percent (Low 0.38, Medium 0.62, High 0.85; before 0.30, 0.50, 0.70).
- Motion made visible: Normal now drifts 0.15 of the screen width with loops of about 16 to 24 s (before 0.06 and 27 to 39 s); Subtle 0.07 with 27 to 40 s. If the background still does not move, read the "Mode:" line in the Violence Style settings (battery saver, Reduce dynamic blur and Remove animations freeze it on purpose).
- Search: the "Recent searches" rows no longer have a black fill while the style is on. Check that the rows are readable and that swiping a row right still shows the Queue reveal.
