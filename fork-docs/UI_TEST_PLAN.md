# Custom UI: test plan (UI-2)

Written 2026-11-02. Two parts: what the automated tests cover (and do not), then a manual checklist for a real phone. Nothing in the manual list has been run yet.

## 1. Automated (run by `.\scripts\test-active.ps1 -Target phone`)

| Test file | What it proves |
|---|---|
| `app/src/test/.../ui/v2/V2LogicTest.kt` (JVM) | contrast maths and the colour tokens' stated ratios, adaptive accent safeguards, glass-level rules and the 3-layer budget, variant parsing, safe-mode logic |
| `app/src/test/.../ui/v2/V2UiTest.kt` (Compose UI on Robolectric, SDK 34) | dock tabs are Tab-role nodes with labels, selected state, 48 dp targets; orb opens the player, is inert with nothing playing, exposes play/pause, next, previous as TalkBack custom actions; seek bar reads "m:ss of m:ss", has skip back/forward actions that clamp, a 48 dp track; Jam chips announce the count and the host and open the Jam page; every non-tab destination (13 routes) is reachable from the Hub; shortcut strips only point at Hub destinations; Hub routes open on the shared `AppNavigator` |

Not covered automatically (needs a device or is outside what Robolectric renders): the real media controller and playback, the glass pipeline (blur, lens), the sheets' animations, deep links through `IncomingLinkHub` (their parsing is in the shared tests; landing is the same code for both UIs), predictive back, TalkBack speech, thermal/battery behaviour, frame times.

## 2. Manual checklist

Build: `.\scripts\build-active.ps1 -Target phone`, install the dev debug APK. Settings, Advanced, **New UI (experimental)** on.

### 2.1 Each screen at 100 % and 200 % font size
Set Android Settings, Display, Font size to the largest, and Display size to the largest if available. For Home, Library, Search (current page inside the shell), the Hub, the Now Playing panel, and Settings: nothing clipped or overlapping; the dock grows (labels wrap to two lines) and every control is still reachable; the shortcut strip scrolls sideways. Record failures with a screenshot.

### 2.2 TalkBack walkthrough
1. Turn TalkBack on. Swipe through the dock: Home, Explore, (orb), Library, Search. Each says its name and "tab"/selected state. 2. Focus the orb with a song playing: it reads "Now playing: title by artist. Opens the player." Open the actions menu: Pause, Next song, Previous song work. 3. Double-tap the orb: the Now Playing panel opens; focus order is artwork (skipped), title (heading), artist, Jam chips (if in a Jam), seek bar, previous, play/pause, next, then the chips. 4. **Play/pause** works. 5. **Seek**: the bar says "Song position, 1:24 of 3:30"; adjust with the volume-key or swipe up/down gesture; the custom actions "Back 10 seconds" and "Forward 10 seconds" work. 6. **Queue**: "Queue, lyrics and more" opens the full player; the queue is reachable there. 7. **Jam**: join a Jam, open the panel: chips read each member's name and "host"; the row reads "Jam: N listening" and opens the Jam page. 8. Avatar (top right): opens the Hub; headings People, Share, Music, You are announced; every row is a button.

### 2.3 Reduce motion and reduce transparency
- Settings, Appearance, **Reduce animation** on: no overshoot or travel in sheets; the glass drops to Simplified (no lens). Off again: spring motion returns.
- Settings, Advanced, **Reduce transparency** on: dock, orb, sheets and dialogs become solid; text stays readable. Off: glass returns.
- System "Remove animations" (Developer options, animator scale 0): same as Reduce animation.

### 2.4 Battery saver and an older Android
- Turn Battery saver on: the dock changes to the Simplified look without a restart (tint and rim, no blur or lens). Off: back to Full (Android 13 or later).
- On an Android 12 device or emulator: Simplified (no lens). On Android 11 or older: Solid everywhere (the app's own minimum is Android 8). If none is available, say so; the rules are unit-tested (`GlassLevelTest`).

### 2.5 Switching UIs while music plays
1. Start a song, add several to the queue, leave it playing. 2. Settings, Advanced: turn **New UI** off and on three times. Audio never stutters or stops, the queue is unchanged (open the full player to check), the position keeps going, the player stays open if it was open. 3. Download a song (watch the progress), switch UIs: the download continues. 4. Switch with the full player open and with a menu open: nothing crashes; the menu may close.

### 2.6 A Jam in progress while switching
Host or join a Jam (two phones). Switch UI on one phone: it stays in the Jam, the member list is unchanged, the other phone sees no leave/rejoin. Host-only control: with controls locked, transport in the Now Playing panel is disabled and says so; the host's phone can still control.

### 2.7 Safe mode
Debug only: temporarily make the New UI throw at start (a local edit, not committed) and launch three times: the third launch is the current UI with the "Back to the classic look" dialog, and the switch is off. Turn it on again and confirm the dialog does not return.

### 2.8 Deep links and incoming links (with the New UI on)
Open each from another app or a second phone, and confirm the right screen or dialog: a `violence://song/...` link (Play now / queue / add to playlist); a `violence://playlist/...` link; a `violence://profile?...` card (Add friend dialog; after adding, the "add you back" offer); a Jam invite (preview and join); the Receive tile and NFC tap (Receive page); a Spotify or Apple Music import link if you use them. Expected: identical to the current UI (the handlers are the same code in the same composition).

### 2.9 Landscape and a large window
Rotate: dock stays centred and capped in width, Hub and Now Playing panels cap at the standard sheet width; the full player's landscape layout is the current one. On a tablet or foldable (or the resizable emulator): same, nothing stretched edge to edge. (No two-pane layout exists yet; UI-3.)

## 3. Performance spot check
`adb logcat -s VioFrames` (debug builds): scroll Home 5 s with the New UI off, then on (Full glass), then with Reduce transparency on; compare p95 and p99. Record the numbers here:

| Case | avg ms | p95 ms | p99 ms | jank |
|---|---|---|---|---|
| Classic UI, glass off | | | | |
| New UI, Full | | | | |
| New UI, Simplified (battery saver) | | | | |
| New UI, Solid (Reduce transparency) | | | | |
