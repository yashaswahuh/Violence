# Wear OS Assessment

> **Update 2026-11-05 (read this first).** This assessment was first written against the old non-git copy (`Violence-main/Violence-main`), not the real clone. Differences found in `Violence-clone` (branch `violence-nk`):
> - `CLAUDE.md`, `FROZEN_TARGETS.md`, `PROJECT_STATE.md` and `UPSTREAM_SYNC_LOG.md` **do exist** here; `CLAUDE.md` kept `:wearos` frozen until the owner said "unfreeze". The owner did so on 2026-11-05.
> - The watch module was already partly rebranded here: Kotlin package `com.music.violence.wear`, strings, `Theme.ViolenceWear`, capability and path names (`violence_*`, `/violence/*`), APK name `ViolenceWear`. Wherever the text below says `com.music.bitchord.*`, `bitchord_*` or `/bitchord/*`, read the `violence` equivalent.
> - Still wrong when the assessment was written, and fixed on 2026-11-05: the watch `applicationId` (`com.music.bitchord` / `com.dev.bitchord`, so it could not pair with the phone's `com.yashaswahuh.violencemusic` / `.dev`) and the launcher icon (BitChord's three-wave mark).
> - Changed on 2026-11-05: watch application IDs now equal the phone's; launcher icon is the phone's Violence V; old BitChord `mipmap-*dpi` webp icons removed; new Violence watch UI (Voltage `#7C3AED` palette, bezel progress ring, Voltage-tinted queue/toggle/output cells). See `FROZEN_TARGETS.md` and the git log.
> - Everything else below (features, architecture, completeness, quality notes, effort) was read from the same source and still holds. Nothing was run on a watch.

**Type:** read-only assessment, not a numbered phase.
**Scope:** `wearos/` module, its manifest, Gradle files and resources, the shared protocol it compiles, and the phone-side code that talks to it.
**Method:** code reading only. Nothing was built, run, linted or tested. Where a claim needs a running watch, it says "cannot judge without running". Where the files needed to answer don't exist, it says UNCLEAR.

## 0. Caveats about this assessment

- `CLAUDE.md`, `FROZEN_TARGETS.md`, `PROJECT_STATE.md` and `UPSTREAM_SYNC_LOG.md` do **not exist** anywhere in this checkout. Read instead: `AGENT_RULES.md`, `ARCHITECTURE.md`, `PHASES.md`, `MODULE_PRUNING_PLAN.md`, `REBRAND_LOG.md`, `CI_DEPLOY.md`, `TV_TARGET_AUDIT.md`. Anything that depends on frozen-target or upstream-sync records is **UNCLEAR**.
- `PHASES.md` documents Phases 1 and 2 as completed and an early CI step. Phases 3-9 are plans only. No Phase 10 is documented. "Effects of Phase 1-10" below means Phases 1-2 plus the Phase 3 plan.
- `AGENT_RULES.md:59` says "commit and push directly to main". This conflicts with the branch rule for this task (`violence-nk` only). The task's rule was followed.

## 1. What it is

**A phone companion remote, not a standalone app.** The manifest sets `com.google.android.wearable.standalone=false` (`wearos/src/main/AndroidManifest.xml:37-39`). The module README calls it "a watch remote for the BitChord phone app" (`wearos/README.md:3`). Minimum is Wear OS 3: `minSdk = 30`, "the oldest a Galaxy Watch 4 ever shipped with" (`wearos/build.gradle.kts:39-40`). Target SDK 36.

### Screens and features that exist

| Area | What the code does | Where |
| --- | --- | --- |
| Now Playing | Cover disc with progress ring, title/artist (marquee), prev / play-pause / next, mesh-gradient backdrop in the phone player's colours | `ui/NowPlayingScreen.kt`, `ui/MeshBackground.kt` |
| Seeking | Hold the disc, then drag or turn the bezel (5 s per detent); commits after a 1.2 s settle | `ui/NowPlayingScreen.kt` (`PlayDisc`, `SCRUB_*`) |
| Volume | Crown / bezel changes the phone's volume (or the watch's when it is the speaker); arc readout | `ui/NowPlayingScreen.kt`, `data/PhoneLink.kt` (`adjustVolume`) |
| Like | Heart, shown only when the phone says the track can be liked | `NowPlaying.canLike`, `ui/NowPlayingScreen.kt` |
| Shuffle / Repeat | In "More"; same off, all, one cycle as the phone | `ui/ListScreens.kt` (`MoreScreen`), `PhoneLink.cycleRepeat` |
| Queue | "Up Next" list with thumbnails; tap jumps to that track | `ui/ListScreens.kt` (`QueueScreen`), `PhoneLink.applyQueue` |
| Output | Picker listing the phone's outputs and "This watch" | `ui/ListScreens.kt` (`OutputScreen`) |
| Always-on | Ambient player: title, artist, logo, burn-in shifting, low-bit handling | `ui/Ambient.kt`, `WearActivity.kt` (`AmbientLifecycleObserver`) |
| Watch-face icon | Ongoing Activity while the phone plays, opens the watch app | `data/NowPlayingListener.kt` |
| Self-update | Phone sends a newer watch APK over a Wearable channel; watch hands it to PackageInstaller | `data/SelfUpdate.kt` |
| Error / empty states | Connecting, "Phone not connected" with Try again, "Not Playing" with Resume | `ui/WearApp.kt` |

### Not implemented at all

Search, browse/home, library, playlists, downloads / offline, lyrics, Jam, profile / account, settings, Spotify, scrobbling, EQ / crossfade / Automix, sleep timer. A grep for `jam|party|listentogether|profile` over `wearos/` and the phone's `app/.../wear/` returns **no hits**.

### How it gets audio

Two modes, both phone-owned:

1. **Remote (default).** The watch only shows state and sends commands. Audio plays on the phone.
2. **"This watch" as the speaker.** The phone keeps the queue (paused) and drives a watch ExoPlayer with `/bitchord/speaker/*` messages. The watch holds only the playing track plus the next two. Bytes come through the phone's own resolver and cache over a Wearable `ChannelClient` (`playback/PhoneStreamDataSource.kt`), so the watch needs no network or account. The README states tracks the phone serves as a DASH manifest can't play on the watch yet (`wearos/README.md:14-15`).

## 2. How it works

### Architecture

- **Single activity** (`WearActivity.kt`) with a `WearViewModel` owning one `PhoneLink` (`data/PhoneLink.kt`). `PhoneLink` exposes `StateFlow`s (link, nowPlaying, artwork, volume, queue, outputs, watchSpeaker). Compose observes them with `collectAsStateWithLifecycle`.
- It listens **only while the activity is started** (`WearActivity.kt:81-86`, `PhoneLink.start/stop`). There is one small `WearableListenerService`, `NowPlayingListener`, that handles the watch-face icon, speaker messages, update info and the APK channel.
- Playback for speaker mode: `playback/Speaker.kt` (object, drives the player and reports status back), `playback/WatchPlaybackService.kt` (`MediaSessionService` + ExoPlayer with a 30-90 s buffer), `PhoneStreamDataSource`.
- Navigation: `SwipeDismissableNavHost` with player, queue, more, output routes (`ui/WearApp.kt`).

### Talking to the phone

All traffic is the Wearable Data Layer carried by Google Play services. There is no direct network use: the manifest has **no INTERNET permission** (`AndroidManifest.xml:19-22`).

| Direction | Mechanism | Paths |
| --- | --- | --- |
| Phone to watch, state | DataItems | `/bitchord/now_playing`, `/bitchord/queue`, `/bitchord/outputs` |
| Watch to phone, commands | Messages | `/bitchord/command/{sync,toggle,next,previous,like,shuffle,repeat,volume,queue,seek,skip_to,outputs,select_output,speaker_watch,speaker_status}` |
| Phone to watch, speaker | Messages | `/bitchord/speaker/{load,ahead,jump,play,pause,seek,repeat,volume,stop}` |
| Audio bytes | Channel (watch opens, phone serves) | `/bitchord/stream` |
| Watch APK | Channel (phone opens) | `/bitchord/watch/apk` |
| Discovery | Capabilities | `bitchord_phone`, `bitchord_watch` |

The whole contract is one file, `wearos/protocol/com/music/bitchord/wear/WearProtocol.kt`, compiled into both `:app` (`app/build.gradle.kts:99-101`) and `:wearos` (`wearos/build.gradle.kts:49`).

**Phone side** (must exist for the watch to do anything): `app/src/main/java/com/music/bitchord/wear/WearBridge.kt` (1129 lines: state publisher, `WearCommandService`, `WatchSpeaker`, stream server), `WatchInstaller.kt` (420 lines, in-app wireless-ADB installer using `libadb-android`), `WatchInstallTypes.kt`, `WatchOutputState.kt`. Hooks in `playback/PlaybackService.kt` (about lines 751, 968, 1084, 1315, 1661, 5881, 7116) and `ui/player/AndroidPlayerHost.kt`. `app/src/main/AndroidManifest.xml:436-456` registers `WearCommandService`. The phone's Settings already has "Violence for Wear OS" strings (`app/src/main/res/values/strings.xml:1172-1205`). The TV build carries no-op stubs (`tv/src/main/java/com/music/bitchord/wear/`).

### APIs and services it depends on

- **Google Play services Wearable** (`play-services-wearable:20.0.1`): free, no account or billing needed by the app. Present on every Wear OS 3+ watch.
- Compose for Wear OS Material 3 1.6.2, Media3 1.11.0 (exoplayer, session), `androidx.wear` 1.4.0, `wear-ongoing` 1.1.0, `profileinstaller` 1.4.1 (`wearos/build.gradle.kts:144-167`).
- **Author-hosted services: none in the watch app.** It has no network. The phone resolves audio (YouTube, addons, downloads), so any author-hosted dependency would be the phone's, not the watch's. Jam's backend is phone-only and unrelated to the watch.

### Shared modules it reuses

Only `wearos/protocol/` (a plain source directory shared with `:app`, not a Gradle module). It does **not** use `:shared` or `:sharedUi`. Its UI, theme and data classes are its own.

## 3. Completeness versus the phone app

| Phone feature | Watch | Notes (from code) |
| --- | --- | --- |
| Now playing (title, artist, art, progress) | **Full** | Art is a 240 px JPEG and the colours are computed by the phone (`WearProtocol.ART_PX`, `KEY_COLORS`) |
| Play / pause / next / previous | **Full** | `PhoneLink.kt` |
| Seek | **Full** | Hold-to-scrub, `NowPlayingScreen.kt` |
| Volume | **Full** | Crown/bezel |
| Like / Love | **Full** (conditional) | Only when `canLike` |
| Shuffle / Repeat | **Full** | `MoreScreen` |
| Queue | **Partial** | A window (1 played + `QUEUE_AHEAD = 40`, `WearBridge.kt:106`) with tap-to-jump. No reorder, remove or add |
| Output / device routing | **Full** | Phone outputs and the watch itself |
| Play audio on the watch | **Partial** | Works via the phone's stream; DASH-served tracks unsupported (README) |
| Always-on / ambient | **Full** (watch extra) | Not a phone feature |
| Search | **Missing** | |
| Browse / Home / Explore | **Missing** | |
| Library, playlists, albums, artists | **Missing** | |
| Downloads / offline | **Missing** | Speaker mode streams from the phone, it does not store |
| Lyrics (synced) | **Missing** | |
| Jam / Listen Together | **Missing** | No code. It would only appear indirectly as the phone's now-playing |
| Profile / in-app account | **Missing** | |
| Settings | **Missing** | Watch-side settings do not exist. Install/update is managed from the phone's Settings |
| Spotify import, scrobbling, Discord presence | **Missing** | |
| EQ, crossfade, Automix, speed, sleep timer | **Missing** | |
| Translations | **Missing** | One `values/strings.xml` only (`wearos/src/main/res/values/`) |

## 4. Quality assessment

### UI toolkit and design guidance

- **Compose for Wear OS Material 3** (`androidx.wear.compose:compose-material3:1.6.2`). Uses `AppScaffold`, `ScreenScaffold`, `TimeText`, `ScalingLazyColumn`, `SwipeDismissableNavHost`, `CircularProgressIndicator` (`ui/WearApp.kt`, `ui/ListScreens.kt`). This matches current Wear guidance for structure, scrolling and swipe-to-dismiss.
- It **departs from stock components on purpose**: hand-built `Pill`, `Cell`, `GlyphButton`, `ToggleCell` instead of M3 buttons/cards; `indication = null` (press = scale dip, no ripple); SF Pro Display fonts and iOS-style greys (`ui/Theme.kt`); a custom `ColorScheme` of black/white. So it follows the layout rules but not the stock Wear look. Whether that reads well is **cannot judge without running**.
- Round-screen handling is by fractions of the screen size (`NowPlayingScreen.kt`), and crown support uses `onRotaryScrollEvent` with `requestFocusOnHierarchyActive` and haptics.
- Accessibility: transport buttons have content descriptions (`GlyphButton`), but several icons pass `contentDescription = null` (e.g. `WearApp.kt` `Message`, `ListScreens.kt` toggle/output icons, where the row text carries the meaning). No TalkBack testing is visible.
- Battery/CPU care is explicit and deliberate: R8 enabled even for debug (`wearos/build.gradle.kts:86-96`), no blur (`MeshBackground.kt` comment), 500 ms progress tick only while the screen is on (`rememberProgress`), 2-iteration marquee, listening only while on screen.

### State and error handling

- Link state is an enum `Connecting / NoPhone / Connected` (`data/NowPlaying.kt`), with Retry on the no-phone screen.
- Optimistic updates with "hold" windows so a late phone update doesn't flick controls back (`PhoneLink.kt` `Hold`, `merge`).
- Sends retry once after rediscovering the node (`PhoneLink.sendNow`).
- Much error handling is **silent**: `runCatching { ... }.getOrNull()` for cached reads and bitmap decode (`PhoneLink.kt:278-284, 390-396`), `Speaker.report` ignores send failures (`Speaker.kt:213-220`), `SelfUpdate` only logs. The only user-visible symptom of a failed speaker stream is probably the buffering spinner. Actual behaviour: **cannot judge without running**.
- `PhoneStreamDataSource.phoneNode` is a process-wide static cache cleared only on an exception (`PhoneStreamDataSource.kt:113, 71`); a re-paired phone could leave it stale until a failure.

### TODO / FIXME markers

**None** in `wearos/` (grep for TODO, FIXME, HACK, XXX across `.kt`, `.kts`, `.xml`, `.md`). Comments are long and explanatory, which suggests finished work rather than leftovers.

### Tests

**None.** No test directory under `wearos/`, and no test in `app/src/test` or `app/src/androidTest` references the watch code. No baseline profile or macrobenchmark module for wear was found (it relies on `profileinstaller` and library-shipped profiles, `wearos/build.gradle.kts:164-166`).

### Signs of being unfinished or abandoned

None found. Dependencies are current (Media3 1.11.0, Wear Compose 1.6.2, same versions as the phone) and the module tracks the phone's version code and name (`wearos/build.gradle.kts:43-44`). The README is detailed and the phone has a full install/update UI for it. Honest caveat: the only evidence of quality is the source, since nothing here has been run.

### Obvious bugs or rough edges (from reading)

- **Manifest comment misplaced:** the "watch as the speaker" comment sits above the `REQUEST_INSTALL_PACKAGES` permission (`AndroidManifest.xml:12-14`), cosmetic only.
- **Reused-app-id coupling:** the watch only works if its package name and signing key equal the phone's (see section 5).
- `NowPlayingListener` is exported with Data Layer intent filters; this is the normal pattern for `WearableListenerService`, and the Data Layer itself restricts delivery to the same package + key.
- `WatchPlaybackService.onTaskRemoved` stops the service only when idle (`WatchPlaybackService.kt:66-69`). Looks intentional.
- Hardcoded English strings only. No localisation.

### Cannot judge without running

Smoothness on a real watch, always-on behaviour, Bluetooth stream stalls and buffering, the first-install ADB flow on Galaxy vs Pixel watches, crown feel, whether the pairing flow works end-to-end in this fork.

## 5. What reviving it would take

### Application ID and signing (pairing with the phone app)

- **Pairing is broken right now.** The phone is `com.yashaswahuh.violencemusic` (dev: `com.yashaswahuh.violencemusic.dev`) (`app/build.gradle.kts:65, 118`). The watch is still `com.music.bitchord` (prod) and `com.dev.bitchord` (dev) (`wearos/build.gradle.kts:39, 56`). The Wearable Data Layer only connects apps with the **same package name and the same signing key** (`WearProtocol.kt` header, `wearos/README.md:30-33`). As things stand the watch would never find the phone. Phase 2 changed the phone's IDs and left the watch untouched (`PHASES.md` Phase 2, `REBRAND_LOG.md`).
- **Fix:** set watch `applicationId` to `com.yashaswahuh.violencemusic` (prod) and `com.yashaswahuh.violencemusic.dev` (dev). Package/namespace `com.music.bitchord.wear` can stay, as Phase 2 kept internal packages.
- **Signing:** the watch must be signed with the phone's key. Debug: both use the machine's debug keystore (`C:\Users\DELL\.android\debug.keystore` exists on this PC), so phone and watch dev builds must be built on the same machine or share that key. Release: `keystore.properties` (gitignored) read by both Gradle files (`wearos/build.gradle.kts:26-29, 65-76`). `wearos/README.md:40-44` notes CI-signed phone APKs use `KEYSTORE_BASE64`, so the matching watch APK must come from the same CI. `PHASES.md` Phase 9 plans that CI signing but it is not done yet.

### Rebrand work remaining (watch app and its phone-side counterparts)

Still "BitChord" on the watch:
- `app_name` resValues "BitChord Dev" / "BitChord" (`wearos/build.gradle.kts:57, 61`).
- User strings: `no_phone_body`, `nothing_playing_body`, `update_ready` (`wearos/src/main/res/values/strings.xml:5, 8, 41`).
- `Theme.BitChordWear` (`themes.xml`), `BitChordWearTheme`, log tag `"BitChordWear"`, `base.archivesName "BitChordWear"` (`build.gradle.kts:142`).
- Launcher/notification icons and `ic_logo` (the BitChord mark, drawn in the ambient player, `ui/Ambient.kt:105`), mesh `BrandMesh` colours.
- Optional: wire names, `bitchord_phone` / `bitchord_watch` capabilities, `/bitchord/*` paths, and the `bitchord-wear://` scheme (`AndroidManifest.xml:63`, `app/.../WatchInstaller.kt:221`). These are invisible to users and must stay **identical on both sides** if changed. Leaving them is safe and cheaper.
- Phone side: `WatchInstaller.kt` uses cert CN / device name "BitChord" (lines 399, 418) and asset names `wear/BitChordWear.apk`. The phone's Settings strings are already rebranded.
- **Not a problem:** `bitchord://source` in `WearBridge.kt:415` is the phone's internal addon-source URI scheme, still used consistently in `SourceRegistry.kt:497`, `AudioCache.kt:555`.

### Jam and in-app profile on the watch

- **What exists:** nothing. No Jam or profile code on the watch or in the phone's `wear/` package.
- **What already works indirectly:** the watch mirrors the phone's player. If a Jam is running on the phone, its now-playing, play/pause, skip and queue display on the watch, and watch commands go to the same phone player. Whether Jam's sync rules (e.g. the Phase 5 stalled-track plan) accept watch-originated pauses or skips is **UNCLEAR** (would need `PartySync` / `ListenTogether` read and a run). Jam design doc: `fork-docs/JAM_DESIGN.md` (not read for this task).
- **What would be needed for real Jam support:** new protocol paths (room state, participants, join/leave), a `WearBridge` publisher for Jam state, new watch screens, and tests. Medium effort. A profile on the watch is optional for a remote; a phone-pushed name/avatar would be small.

### Effects of Phase 1-10 changes and upstream syncs

- **Phase 1** (presence, update checker, promo links, `.gitignore`): did not touch wear code.
- **Phase 2** (IDs, strings, assets): changed the phone only. Caused the pairing break above. It explicitly deferred module pruning.
- **Phase 3 as planned** (`PHASES.md`, `MODULE_PRUNING_PLAN.md`): deletes `/wearos`, the phone's `wear/WearBridge.kt` and `WatchInstaller.kt`, the `BundleWearApk` Gradle task, the `../wearos/protocol` source set, the wearable dependencies and settings rows, and the TV stubs. **Reviving Wear OS means cancelling or narrowing that part of Phase 3** (keep `:wearos`, `WearBridge`, protocol; the TV stubs stay as they are).
- **Early CI:** `.github/workflows/android.yml:52` builds with `-Pbitchord.bundleWear=false`, so the phone APKs carry **no** watch APK today, and the Settings install flow would show nothing to install (`WatchInstaller` checks for the bundled asset). `settings.gradle.kts` still includes `:wearos` when an Android SDK is present.
- **`AGENT_RULES.md:21, 50-53`** declare Wear OS and Desktop pruned and forbid building or testing `:wearos` unless the user explicitly asks. That standing directive would need to be lifted for any real verification.
- **Frozen targets and upstream syncs:** **UNCLEAR.** `FROZEN_TARGETS.md` and `UPSTREAM_SYNC_LOG.md` do not exist. Upstream merges that touch `PlaybackService.kt`, `WearBridge.kt` or `WearProtocol.kt` would need a manual check, because both apps compile the same protocol file.

### Free-only compatibility

Compatible. No paid service, no sign-up, no network in the watch app, and the Wearable Data Layer is free. The self-update path sends the APK phone-to-watch directly, with no hosting. It needs Google Play services on the watch, which standard Wear OS 3+ watches have. Nothing is published, so the private-fork rule is unaffected.

### Effort estimate: small to medium

| Goal | Size | Reason |
| --- | --- | --- |
| Make the watch app pair and run again | **Small** | Two application IDs, user-visible strings and icon, stop pruning it, build once on the same key as the phone |
| Reliable install for a friend, plus a verified build | **Medium** | Needs CI to build and sign a matching watch APK (the phone's install path expects it bundled), real-device testing on the friend's watch model, and lifting the "do not build wearos" directive. The module has no tests |
| Jam controls or participants on the watch | **Medium** | New protocol, publisher and screens, and a check of Jam sync behaviour |
| Search, library or offline on the watch | **Large** | Not in the original design; would invert "remote" into a second client |

## 6. Recommendation

**Worth reviving, at the minimum-viable level.** The original is not a stub: it is a deliberately small, performance-tuned remote with now playing, transport, seek, volume, like, shuffle, repeat, queue, output picker, always-on, and an optional watch-speaker mode. It has no TODOs and tracks the phone's versions. The costs are that it has no tests, has never been built or run in this fork, and its pairing is currently broken by the rebrand.

### Minimum viable version (basic controls and now playing)

1. Change the two watch `applicationId`s to match the phone's, keeping the same signing key.
2. Change visible names (`app_name`, the three strings, icons/logo).
3. Keep `:wearos`, `WearBridge`, `WearProtocol` and the TV stubs out of Phase 3 pruning.
4. With the user's explicit go-ahead, build the watch once and test on the friend's watch: pairing, now playing, controls, queue.
5. Leave speaker mode and self-update as shipped, since they need no changes, and test them last. If install trouble appears, sideloading the APK over ADB (per `wearos/README.md`) avoids the phone-bundling path.
6. Do not start Jam, search, library or offline on the watch until the basics are verified on hardware.
