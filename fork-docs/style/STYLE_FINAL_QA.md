# Violence Style: final QA checklist (real devices)

Run this on real hardware before any wider release. Mark each line OK, or write what happened (device, Android version, setting, screenshot). Build: the phone debug APK, or a release build if you have one. Screenshots: `adb exec-out screencap -p > name.png`. Frame stats: `adb shell dumpsys gfxinfo <package> framestats` (package `com.yashaswahuh.violencemusic.dev` for the dev build).

## 1. Devices
| Device | Check |
|---|---|
| A low-end phone (about 3 to 4 GB RAM, older chip) | Style ON, Aurora, Normal, Medium: scroll Home for a minute; is it smooth? Does the Settings "Mode" line say Lite? If it janks, tell me the model |
| An older Android (8 to 11) | The Mode line says "Android 11 or older" (Lite); the background is still; Liquid Glass is unavailable; the app is readable; nothing crashes |
| Android 12 or later with dynamic colours | Accent "Device" follows your wallpaper colour; change the wallpaper colour and re-open: the accent changes |
| Android 13 or later | The glass lens effect is present on the bars; compare with 12 (no lens) |

## 2. Settings matrix (style ON)
- Backgrounds: Aurora, Corner, Haze, Off, each with Motion Off, Subtle, Normal and Intensity Low, Medium, High. Nothing blanks, the text is readable everywhere, nothing crashes.
- Accent: Song (a colourful cover, a **black** cover, a **white** cover, a gray cover, a song with no artwork), Device, Custom with all nine presets and a hand-picked colour (a very dark or gray pick is refused with a message).
- Heading font: Bodoni Moda, Cormorant, Anton.
- Reduce transparency ON and OFF; Reduce dynamic blur ON and OFF; Liquid Glass ON and OFF; the glow-on-album-pages switch ON and OFF.

## 3. Safety (these must never fail)
- **Music keeps playing** while you switch the style ON and OFF, change the background, motion, accent and font.
- **The queue is intact** after toggling (same songs, same position).
- **You keep your place** after toggling: the page you were on, scroll position and open dialogs stay (S6 fix).
- **A Jam session survives** a toggle: stay in a Jam on two phones, toggle on one, the session continues and sync holds.
- **Fresh install:** the style is OFF by default and the app looks as it always did. **After an update** from an earlier version: settings survive, the style stays as you left it (OFF unless you turned it on).
- **Safe mode:** you cannot easily force it; instead confirm that a normal restart with the style ON keeps it ON (the counter clears after the first screen).
- **Deep links, notifications, widgets, media controls (lock screen, headset, notification) and the Quick Settings Receive tile** all still work with the style ON: open a `violence://song/...` link, a Jam invite, tap a notification, use the widget.
- **Sharing and imports:** Tap to share, QR scan, Spotify and Apple Music import still run with the style ON.

## 4. Accessibility
- **TalkBack ON:** swipe through Home (headings announced as headings, hairline and dot silent), the bottom bar (tabs named, the selected one "selected"), the mini player, a dialog and the Violence Style settings (preset swatches announced with their colour name and selected state; the three sliders named Hue, Saturation, Lightness).
- **Font size at the maximum (200 percent):** Home, Library, Settings, a dialog, the Violence Style block: nothing cut off or overlapping, headings wrap or end in "...", "Show all" still tappable.
- **Bold text** and **high-contrast text** (Android Accessibility settings) ON: text stays readable; high contrast makes the glass solid.
- **Remove animations** (developer options or accessibility): the background is still; nothing animates by itself.
- **Battery saver** ON: the Mode line says battery saver, the background is still.

## 5. Visual
- Rotate to landscape and back on Home and the player: nothing flashes or breaks.
- Artwork edge cases: a very bright cover, a very dark cover, a non-square cover (a music video), a song with no cover: the hero card text, rows and accent stay readable.
- Compare Home with the mockups (`ref2` to `ref4`) one more time: heading and dot, hero card and rim light that follows the front card, Recents rows, top pill, mini player, nav capsule with its glow line.
- Style OFF: everything exactly as before the skin (screenshots against your earlier OFF screenshots).

## 6. Performance numbers to write down
Home scroll, 10 seconds, style OFF then ON (Normal motion, Aurora, Medium): "Janky frames" percentage and the 90th and 95th percentile frame time from `framestats`. Battery: 30 minutes of idle music with the screen on, motion Normal against motion Off.

## 7. Sign-off
Tick "S6 approved" and "S6 OFF-parity verified on device" in `STYLE_APPROVALS.md` when sections 3 and 5 pass and the rest are acceptable. Report every failure with the device, Android version and the setting combination.
