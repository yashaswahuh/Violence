# Android TV: manual test plan

Written in Phase 7 (2026-11-02). Everything the TV app does is meant to work with **only a remote**: the four arrow keys, OK/Enter, Back, and (if the remote has them) Play/Pause, Fast-forward, Rewind. No touch screen, no mouse, no keyboard.

**Honest status:** the TV app has been *read through and built*, never run on a TV or TV emulator by the maintainer tooling (there is none on the build machine). The "Reviewed in code" notes below are what was checked by reading the source; the checkboxes are for a person with a TV. Anything marked UNVERIFIED needs a real test.

## 0. What you need
- An Android TV / Google TV / Fire TV-style device or box (Android 8.0 or newer), or the Android Studio **Television** emulator image.
- The TV APK `violence-tv-<version>.apk` (see `RELEASE_GUIDE.md`, "Install on a TV"). Package: `com.yashaswahuh.violencemusic.tv` (the dev flavor installs next to it as `.tv.dev`).
- A YouTube Music account to sign in with (the app asks on first run) and a Wi-Fi/Ethernet connection.
- For Jam: a phone on the same Jam server (`JAM_BACKEND_URL` in the build).

How to read this plan: do each step, tick the box if the result matches. A step that cannot be done with the remote alone (you needed a mouse, got stuck, or Back did the wrong thing) is a **focus trap**: write down the screen and the exact buttons you pressed.

## 1. Install and launch
- [ ] The app appears in the TV launcher row (Apps) with the **Violence banner** (wide image) and the name "Violence", not as a blank tile.
- [ ] Launching from the launcher opens the setup/welcome screen the first time and the home screen afterwards, landscape.
- [ ] The app is also found through the TV's search "Open Violence" voice/typed search (nice to have, UNVERIFIED).
- [ ] On a device **without a touch screen** the install is not refused (the manifest says touch screen is not required).

Reviewed in code: the manifest declares `LEANBACK_LAUNCHER`, `android:banner="@drawable/tv_banner"`, a launcher icon, landscape orientation, and `touchscreen`/`faketouch` as **not required**. `android.software.leanback` is also declared as **not required** on purpose: required would make a plain Android box (no Google TV launcher) refuse the install when side-loaded, and the app does not need the leanback *library* to run.

## 2. First run (setup)
Steps are Welcome, Sign in, Appearance, Preferences, Done.
- [ ] **Continue** on Welcome is focused and moves on; Back on a later step goes to the previous step (Back on Welcome leaves the app).
- [ ] **Sign in** step: signing in to YouTube Music works using only the remote (UNVERIFIED: how the sign-in page behaves on a TV was not run; if it needs a mouse or keyboard you cannot reach, note it). Skipping reaches the next step.
- [ ] Appearance and Preferences: every option is reachable and changeable; **Continue** is reachable from the last option.
- [ ] Done leads to the home screen.

## 3. Navigation: every screen reachable and escapable
For each screen: a focus ring/lift is visible on exactly one element at all times; arrows move to the neighbour in that direction; Back returns to where you came from; focus never disappears (no moment where arrows do nothing).
- [ ] **Home**: Up/Down between shelves, Left/Right within a shelf, OK on a card plays or opens it, long-press OK opens the song actions (if the remote can long-press).
- [ ] **Explore**: same.
- [ ] **Search**: typing uses the TV keyboard; after results appear, Down moves from the field into the results and Up returns.
- [ ] **Library**: switching between tabs/sections, then into an album/playlist and Back.
- [ ] **Album / playlist / artist page**: Play and Shuffle are focusable, the list scrolls with Down, Back returns to the previous list **at the same position**.
- [ ] **Now Playing**: Play/Pause with OK, Left/Right on the progress bar seeks, Down to the buttons (lyrics, queue, repeat, shuffle), Back returns to the previous screen and keeps the music playing.
- [ ] **Queue / lyrics sheet**: opens and closes with Back; focus returns to the button that opened it.
- [ ] **Settings**: every row reachable; rows that open dialogs return focus to the row when the dialog closes.
- [ ] Leaving by Back from the **home** screen exits the app to the launcher (music keeps playing in the background if it was playing).

Reviewed in code: all clickable TV elements go through one helper (`tvClick`), so they are focusable; there are no raw touch-only clickables in the TV module. Dialogs, Now Playing, the setup flow and Explore install their own Back handlers. One known weak spot was found and fixed (section 5, Jam keypad); another is on the list below.

## 4. Jam (Listen Together) on the TV
Open Settings > Listen Together.
- [ ] No Jam server in the build: a message says so, **OK** is focused and closes the dialog.
- [ ] Not signed in: a message says to sign in first, **OK** is focused.
- [ ] Idle: **Start a Party** is focused first. OK creates a party; the code and the list of members show; **Leave Party** is focused first and **Done** below it.
- [ ] While the server wakes up (free hosting can take up to a minute) the dialog says it is waking, focus stays on the button, and nothing needs a mouse.
- [ ] **Join with a Code** opens the key grid. The first key is focused. Move with the arrows, OK enters a character. **Delete**, **Clear**, **Join**, **Back** are reachable with the arrows. Delete until the code is empty: focus **stays** on Delete (it used to vanish).
- [ ] **Join** is dimmed until all 6 characters are entered, then OK joins. A wrong code shows a message and focus is still on the grid.
- [ ] Open `violence://party/ABC123` on the TV (for example with `adb shell am start -a android.intent.action.VIEW -d "violence://party/ABC123" com.yashaswahuh.violencemusic.tv`): the Jam dialog opens with the code filled in and **Join** focused.
- [ ] In a party started on a phone, the TV follows song changes and pause/play.
- [ ] A song the TV cannot play: a notice is shown and it rejoins on the next song; the dialog is still closable.

## 5. Focus problems found and fixed in Phase 7
| Where | Problem (found by reading the code) | Fix |
|---|---|---|
| Jam dialog, code keypad | Buttons were switched to *disabled* while a join was running and when the code was empty. A disabled button cannot hold focus, so after pressing Delete on the last character, or Join, the focus ring vanished and the first arrow press had to find a new target. | The keys, Delete, Clear, Back and the dialog's Start a Party / Join with a Code buttons are never disabled; they ignore a press while busy. Join stays dimmed until the code is complete (focus is never on it before that). |
| Update dialog | Said "BitChord ... Is Available" and sent people to GitHub. | Rebranded; says the TV may ask once to allow installs from Violence (see section 7). |

**Still open (not changed, needs a TV to judge):** the Play/Shuffle buttons on a detail page are disabled while the page is still loading (`canPlay`); if you hold the focus on one at the moment it enables or disables, focus may drop once. Re-test section 3 "Album / playlist" and report.

## 6. Playback and the media session
- [ ] Start a song: the TV's system UI (or the remote's Play/Pause button) controls it. Play/Pause, Next, Previous on the remote work.
- [ ] While music plays and you go to the launcher (Home), music continues; the media notification/now-playing tile shows the title and artwork.
- [ ] HDMI-CEC / the TV power button: the TV going to standby pauses or stops music rather than playing to a dark screen (device dependent, UNVERIFIED).
- [ ] After a long pause, Play still works (the playback service is not killed by the OS).

Reviewed in code: playback runs in the same media service as the phone app, which publishes a Media3 media session (the remote's media keys and the system Now Playing tile use it).

## 7. In-app update on a TV
Needs a build with `UPDATE_INFO_URL` set and a newer version published (`RELEASE_GUIDE.md`).
- [ ] After launching, within a day of the last check, Settings > Software Update shows the new version and what changed.
- [ ] **Download and Install** shows progress; **Hide** keeps downloading; **Cancel Download** stops.
- [ ] **Install Now** opens the system installer. The first time, the TV says the app is not allowed to install apps: allow Violence under *Install unknown apps*, go Back, select **Install Now** again.
- [ ] A TV with no such settings screen (some Fire TV / boxes): the installer says why; the manual way is `RELEASE_GUIDE.md`, "Install on a TV".
- [ ] A new version published *without* a TV APK: the dialog shows a QR code to open on a phone instead.

## 8. Report template
```
Device / Android version:
Build (versionName):
Step number and what happened:
Which buttons were pressed before it happened:
```
