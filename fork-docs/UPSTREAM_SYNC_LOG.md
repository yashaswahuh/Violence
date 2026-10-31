# Upstream sync log

## Sync 1: BitChord v1.9 (2026-10-31)

- **Target:** official tag `v1.9` on `kushagrasinghx/BitChord` (`04f64c9`, 2026-10-30). 283 upstream commits since the base, 829 files changed (+52.8k/-95.7k lines, mostly the desktop deletion).
- **Branch:** `violence-nk`. Backup tag: `pre-upstream-v1.9-2026-10-31` (pushed).
- **History bridge:** the repo had no common ancestor with upstream (it was created from a source copy). One `-s ours --allow-unrelated-histories` merge of upstream `edc1e6f` recorded the base with zero file changes (see `UPSTREAM_SYNC.md`). The v1.9 merge then ran as a normal 3-way merge.
- **Version:** `versionName 1.9.0-violence.1` (phone via root `build.gradle.kts`, TV in `tv/build.gradle.kts`), phone `versionCode 35` (was 28), TV `29` (was 26). The "-violence" suffix is treated as pre-release by `AppUpdateChecker.isNewer()`; the updater is still disabled until Phase 8.

### What upstream changed
- **New features:** podcasts; bit-perfect audio (USB DAC via Android 14 mixer, AAudio exclusive, direct libusb driver, DAC hardware volume + HUD); loop-mix / Automix on web and native `loopmix`; Liquid Glass UI polish on Android; Song Info panel; Spotify import language filter and Download All for local Spotify playlists; "keep playlists updated" (WorkManager `PlaylistSync`); music video quality up to 4K on web; track menus and mouse-wheel volume on web; swipe-to-queue; Vazirmatn font; open-with for local audio files (new intent filter); 9 Neon + 1 Monochrome launcher icons; star instead of heart for favorites.
- **Fixes:** 72 fix commits (history removal, continuation tokens, player/queue/UI fixes).
- **Modules touched:** app 247 files, sharedUi 70, shared 37, web 90, native 23 (new `third_party/signalsmith-*`, libusb, loopmix), tv 8, backend 5, `.github` 5, wearos 0.
- **Dependencies:** Android `androidx.work:work-runtime-ktx:2.10.1`, `sfsymbols-android:1.1.1` (also added to tv). Web: `essentia.js`, `idb`, `onnxruntime-web`, `signalsmith-stretch`. Removed: `beat_this_int8.onnx` asset.
- **Network endpoints / domains:** no new hosts in added code except a web presence ping (see below). New credit links in docs/headers only (`heydavidf/automix`, `mornyeapp/Mornye`, `SFG5453/Orchard`).
- **Backend / Jam:** only presence (adds a `browser` platform and Origin rules) and `deploy/Caddyfile` (Cloudflare trusted proxies). `backend/protocol`, `party`, `hub` unchanged. Client Jam files changed only in UI (`ListenTogetherScreen.kt`, web `ListenTogetherPage.tsx`, `listenTogether.ts`, `partySync.ts`); `PartyModels.kt` and `ListenTogether.kt` unchanged.
- **Manifest / permissions:** adds `MODIFY_AUDIO_SETTINGS`, `BLUETOOTH` (max SDK 30), USB host feature (optional), `profileable`, audio "open with" filter, 10 new activity-aliases. No applicationId change.
- **Workflows:** android/check/tv/web modified, `release.yml` deleted.
- **Desktop:** upstream DELETED `desktopApp`, `desktopUi`, `DESKTOP.md`, `docs/DESKTOP_RELEASES.md` (commit `e4dd63d`, "web player replaces it") and removed their `settings.gradle.kts` includes.
- **Wear OS:** unchanged upstream.

### Conflicts and resolutions
~126 files were changed by both sides. 106 reported conflicts:
| Area | Resolution |
|---|---|
| ~80 desktopApp/desktopUi modify/delete | Kept OURS (whole modules restored from our HEAD, incl. `DESKTOP.md`, `docs/DESKTOP_RELEASES.md`, `.github/workflows/release.yml`). `upgradeUuid`, `packageName`, bundle ID unchanged. Settings includes kept. |
| 19 `app/.../values*/strings.xml` | Took upstream text, renamed BitChord -> Violence (167 new occurrences incl. non-conflicting). |
| `SettingsSheet.kt` | Kept our fork-attribution colophon (no author links); adopted upstream's new `section == null &&` condition. |
| `README.md`, `MAINTAINERS.md` | Kept OURS (fork section, no badges/donation/contributor list). |
| `.github/workflows/android.yml`, `web.yml` | Kept OURS. Added `VITE_PARTY_SERVER_URL` to the web build (see below). |
| `web/app/src/config.ts` | Ours (base-aware URLs, empty `androidAppUrl`) + upstream `partyServerUrl`. |
| `web/app/vite.config.ts` | Upstream structure (party-server build guard, essentia/worker options) + our `base` `/Violence/`. |
| Upstream deletions we accepted | `.claude/launch.json`, USB/AAudio files superseded upstream, `ArtworkBackdrop.kt`, beat_this asset. |

### Re-applied / newly disabled
- **DISABLED:** `web/app/src/playback/livePresence.ts` (new upstream web presence ping to `<party server>/api/presence`) is now a no-op via `PRESENCE_ENABLED = false`.
- **Still disabled and verified:** `Presence.kt` `ENDPOINT = ""`, `AppUpdateChecker.kt` `LATEST_RELEASE_URL = ""`.
- **Branding sweep:** BitChord -> Violence in all new/merged user-visible strings, including `sharedUi/.../composeResources` strings (previously missed: Replay "listener" name, Vietnamese "Replay"). Untouched on purpose: code identifiers, log tags, user-agent strings, JNI symbols, license/copyright headers (`Copyright (C) 2026 BitChord Project`).
- **Web party server:** upstream's web build now fails without `VITE_PARTY_SERVER_URL` (it removed the author's server from the repo). `web.yml` sets it from repo variable `JAM_SERVER_URL`, defaulting to the server the web app used before (`https://api.bitchord.kushagrasingh.in`) so Jam works as it did. Set `JAM_SERVER_URL` when the self-hosted backend exists (Phase 4).

### Jam backend compatibility
The Jam protocol (REST, WebSocket frames, models) is unchanged in v1.9, so a Jam server running the pre-sync code stays compatible with the new apps. The only server change is optional presence/Caddy handling; we do not use presence. **No redeploy is required.** The deployed/author server itself was not contacted or tested (UNCLEAR whether it is up).

### New external dependencies classification
Nothing new needing a decision: no analytics/telemetry/updater/donation code besides the web presence ping above (DISABLED). New libs are local (WorkManager, SF Symbols, essentia, onnxruntime-web, signalsmith, libusb).

### Logins
YT Music and Spotify login code (`auth/`, `data/canvas/SpotifyToken.kt`, `Innertube.kt`) is not in the upstream diff conflicts; compiled and unit-tested unchanged. No new code logs or sends session data elsewhere (checked added lines for new hosts). Interactive login not tested.

### Launcher icons
`LauncherDefault` is the only `android:enabled="true"` alias; 23 aliases now. Our 12 Violence glass icons are intact. Upstream's 10 new icons (`LauncherMonochrome`, `LauncherNeon{Silver,Cyan,Violet}`, `LauncherSamsungNeon*`, `LauncherPixelNeon*`) arrived with BitChord's three-bar mark. **Converted to Violence after the merge:** the three-bar mark was replaced with the Violence "V" (same glyph as the glass icons) in all 10 foregrounds x 5 densities (50 PNGs, same file names and pixel sizes). Tile shape, black body, coloured glow rim (silver/cyan/violet), Samsung squircle and Pixel circle shapes are untouched; the V uses the old bars' own highlight/shadow colours plus a soft glow, and the Monochrome icon keeps its dark glossy style with the soft drop shadow. The themed (monochrome-layer) icon for the Neon set was already the Violence V (`ic_glass_monochrome`). Generator script is not committed; the PNGs are the source of truth.
### Verification (explicit tasks, JDK 17.0.20, SDK platform 37.0 + 36, NDK 27, CMake 3.22.1)
| Check | Result |
|---|---|
| `:app:assembleDevDebug -Pbitchord.bundleWear=false` | BUILD SUCCESSFUL (15 min) |
| `:tv:assembleDevDebug -Pbitchord.bundleWear=false` | BUILD SUCCESSFUL (6.5 min) |
| `:app:testDevDebugUnitTest` | 1209 tests, 0 failures |
| `:shared:jvmTest` | 122 tests, 0 failures |
| web `typecheck`, `build:site`, `test` | pass; 159 tests, 0 failures |
Not built or tested (by rule): wearos, desktopApp, desktopUi.

### Frozen targets impact
- **Desktop:** kept as-is, but upstream changed `shared`/`sharedUi` (37 + 70 files) after deleting desktop, so nothing there was adapted. Desktop may not compile against v1.9 shared code. UNCLEAR, not built. Fix only if the owner asks.
- **Wear OS:** no upstream changes, no conflicts. `:wearos` still included in settings (builds are skipped with `-Pbitchord.bundleWear=false`).
- `release.yml` (desktop release on `v*.*.*` tags) kept; do not tag.

### Environment notes (machine, not repo)
Installed: Temurin JDK 17 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`), Android cmdline-tools, CMake 3.22.1, platform 36. Platform `android-37.0` builds fine with this AGP. In PowerShell, quote `'-Pbitchord.bundleWear=false'`. Web scripts need `pnpm` on PATH (corepack shim). Downloads from dl.google.com need `curl.exe -4`.

### UNCLEAR / owner follow-ups
- Neon/Monochrome icons are now Violence-branded (V); review them on a device.
- Desktop compile state against v1.9 shared code.
- Pre-existing, untouched: TV manifest still uses old `bitchord` scheme/host; `shared JamInvite.ORIGIN` still the author's domain; `/violence` vs `/Violence` casing in invite code (see PROJECT_STATE s.9 D1-D3).
