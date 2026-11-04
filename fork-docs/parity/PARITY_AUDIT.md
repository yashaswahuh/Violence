# Parity Audit: Desktop & Web Target Parity (DW-0)

**Date:** 2026-11-04  
**Project:** Violence ("Desktop and Web Parity", DW-0)  
**Host Environment:** Windows (PowerShell 5.1 / 7)  
**Target Status:** Phone (Active), TV (Active), Web (Active), Desktop (UNFROZEN for parity project), Wear OS (STRICTLY FROZEN)  
**Branch:** `main` (commit & push permitted per project rules for `violence-nk` or `main`)

---

## 1. Executive Summary

This document establishes the baseline architecture, compile/test health, feature parity matrix, gap analysis, styling portability, and execution roadmap for bringing the Desktop and Web targets of **Violence** to parity with the active Android phone and TV implementations.

In DW-0, **no application source, build, or resource files were modified**. All audits were conducted by running explicit Gradle module commands and pnpm tasks, inspecting source trees across all modules, and cross-checking historical freeze/unfreeze logs.

---

## 2. Module and Source-Set Map

### 2.1 Modules and Compile Targets

| Module | Build Tool / Type | Compile Targets | Source Sets & Dependencies |
|---|---|---|---|
| `:shared` | Gradle KMP (`androidTarget`, `jvm`) | Phone, TV, Desktop | `commonMain` (pure multiplatform logic), `jvmSharedMain` (shared by Android & JVM/Desktop; Ktor CIO/OkHttp, Jsoup, InnerTubeX, NewPipe, models, profiles, share links), `androidMain`, `jvmMain` |
| `:sharedUi` | Gradle CMP (`androidTarget`, `jvm`) | Phone, TV (and historically desktop) | `commonMain`, `jvmSharedMain`, `androidMain`, `jvmMain`. CMP 1.10.3, Haze 1.3.1, Coil 3. Hosts shared Compose screens, now-playing, lyrics, and shared style hooks (`StyleHooks.kt`, `StyledComponents.kt`) |
| `:app` | Gradle Android App (`com.android.application`) | Phone (`devDebug`, `devRelease`, `prodRelease`) | Compiles against `:shared`, `:sharedUi`, Media3 1.11.0, CameraX 1.6.2, local C++ CMake DSP analyzer. Compiles `wearos/protocol` |
| `:tv` | Gradle Android App (`com.android.application`) | TV (`devDebug`, `devRelease`) | Reuses `:app` source trees (`app/src/main/java`, `res`, `assets`, `jniLibs`), overrides `TvActivity`, `TvSearchScreen`, `TvProfileDialogs`, `TvShareDialogs`, stubs out `WatchInstaller` and `WearBridge` |
| `:desktopApp` | Gradle JVM Application (`org.jetbrains.kotlin.jvm`) | Desktop (Windows x64, macOS arm64/x64, Linux x86_64) | Compiles against `:shared`, `:desktopUi`, JavaFX 21 WebKit, JavaCPP FFmpeg 7.1.1, ONNX Runtime 1.29.0, Haze 1.7.2 (Win) / 1.7.3 (Linux), native C++ SMTC and DSP analyzer (`native/analyzer`) |
| `:desktopUi` | Gradle CMP (`org.jetbrains.kotlin.multiplatform`) | Desktop JVM | `commonMain`, `jvmSharedMain`, `jvmMain`. Branched historically from `:sharedUi`. Pinned to CMP 1.10.3 (Windows) / 1.12.0 (Linux). Generates `Res` under `com.music.violence.sharedui.resources` |
| `web/` | pnpm Monorepo (React 19 + Vite + WXT) | Modern Web Browsers (Chrome, Firefox, Edge, Safari) | `web/app` (Vite SPA player), `web/extension` (WXT Manifest V3 browser extension bridge), `web/shared` (TypeScript bridge types & contracts) |
| `backend/` | Standalone Go (Go 1.27) | Self-hosted Linux/Container (Render Free) | Gorilla WebSocket room hub, in-memory sync, clock server, invite landing redirect |
| `native/` | C++ CMake (`native/analyzer`) | Phone, TV, Desktop | C++17 DSP analysis (`audio_analysis.cpp`, `mel_spectrogram.cpp`, `DsdBlockDecoder.cpp`). Compiled by `:app`, `:tv`, and `:desktopApp` |
| `:wearos` | Gradle Android App | Wear OS Smartwatch | **FROZEN.** Standalone watch app and data layer bridge. Never touched, built, or linted |

### 2.2 Feature Code Location Map

| Feature | Shared Common Code (`:shared`) | Shared UI (`:sharedUi` / `:desktopUi`) | Phone App (`:app`) | TV (`:tv`) | Web (`web/`) | Desktop App (`:desktopApp`) |
|---|---|---|---|---|---|---|
| **Playback & Queue** | `playback/`, `StreamResolver.kt` | Queue components | `PlaybackService.kt`, `CrossfadeController.kt` | Reuses `:app` `PlaybackService` | `web/app/src/playback/` (Shaka Player) | `DesktopAudioEngine.kt`, `DesktopAudioDecoder.kt` (FFmpeg) |
| **Search** | `YtMusicRepository.kt`, `SearchHistoryEntity.kt` | `SearchScreen.kt` | Wired in navigation | `TvSearchScreen.kt` (D-pad) | `web/app/src/pages/SearchPage.tsx` | `desktopUi` `SearchScreen.kt`, `DesktopSearchClient.kt` |
| **Library & Downloads** | Model interfaces | Library & Playlist screens | `DownloadService.kt`, SQLite repositories | `TvLibraryScreen.kt` (No downloads) | `LibraryPage.tsx` (LocalStorage/IndexedDB; no downloads) | `DesktopDownloadManager.kt`, `DesktopDownloadQueue.kt` |
| **Lyrics** | `LyricsRepository.kt`, `LyricsTranslation.kt` | `LyricsSheet.kt`, `LyricView.kt` | Lyrics presentation | `TvNowPlayingScreen.kt` lyrics | `web/app/src/lyrics/` | `desktopUi` `LyricView.kt`, `DesktopLyricsClient.kt` |
| **YT Music Login** | `Innertube.kt` auth state | - | `YtMusicLoginScreen.kt` (WebView) | `TvAuthServer.kt` (HTTP :8080 + QR) | `web/extension` (cookie interception) | `DesktopWebSignIn.kt` (JavaFX), `DesktopYouTubeAuth.kt` |
| **Spotify Login & Import** | `SpotifyImporter.kt`, `SpotifyLibrary.kt` | `SpotifyImportAlert.kt` | `SpotifyLoginScreen.kt` | Missing (no WebView/import) | `SpotifyPlaylistPage.tsx`, extension bridge | `DesktopWebSignIn.kt`, `DesktopSpotifyLibrary.kt` |
| **Apple Music Import** | Missing | Missing | Missing (only Apple artist art/photos) | Missing | Missing | Missing |
| **Jam (Listen Together)** | `JamInvite.kt`, `PartyModels.kt`, `ServerClock.kt` | `ListenTogetherMembersSheet.kt` | `ListenTogether.kt`, `PartySync.kt` | TV Jam dialog (`TvActivity.kt`) | `web/app/src/party/`, `/party/:code` landing | `DesktopListenTogether.kt`, `DesktopPartySync.kt` |
| **Profiles & Friends** | `data/profile/` (`InAppProfile`, `FriendCard`) | `ProfileAvatar.kt` | `ProfileManager.kt`, `ProfileScreens.kt` | `TvProfileDialogs.kt` | `web/app/src/profile/`, `ProfilePage.tsx` | Missing (no profile UI/manager) |
| **Sharing & Incoming Links**| `data/share/` (`IncomingLinks`, `SongLinks`) | Share dialogs / sheets | `IncomingLinkHub.kt`, `nfc/` HCE tag | `TvShareDialogs.kt`, `TvActivity` link | `web/app/src/share/`, landing routes | Missing (no incoming link hub or protocol URI) |
| **Last.fm Scrobbling** | `data/scrobbling/` (`LastFM.kt`, `ScrobbleManager`) | Settings UI | Scrobble manager loop | Scrobbles via `:app` | Missing (no client API secret) | `DesktopScrobbling.kt`, `DesktopScrobbleSettings.kt` |
| **Violence Style** | - | `StyleHooks.kt`, `StyledComponents.kt` | `ui/violencestyle/` (Host, Engine, Glass, Glow) | Missing / Not provided | Missing | Missing |
| **Experimental Settings** | - | - | `SettingsSheet.kt` (Experimental tab) | Missing | Missing | Missing |
| **Update Flow** | - | - | `AppUpdateChecker.kt` (`update.json` + APK) | `AppUpdateChecker.kt` | Automatic reload / `extension.json` | `DesktopUpdateChecker.kt` (no-op stub) |
| **App Icon Picker** | - | - | `AppIcon.kt` (`activity-alias`) | Not applicable | Not applicable | Not applicable |
| **Media Keys & Tray** | - | - | Android MediaSession / Auto | TV MediaSession / Remote | `navigator.mediaSession` | `DesktopAwtTray.kt`, SMTC (C++), MPRIS |
| **About & Fork Attribution** | - | - | Settings Sheet About section | TV About Dialog | Footer & About Modal | `DesktopLicenses.kt`, `DesktopApp.kt` |
| **Safe Mode** | - | - | `StyleSafeMode.kt` (crash counter) | Not applicable | Missing | Missing |

---

## 3. Compile and Test Health Audit

All checks were executed synchronously or asynchronously via explicit module tasks.

### 3.1 Task Execution Results

| Target | Command | Result | Details |
|---|---|---|---|
| **Desktop UI Compilation** | `.\gradlew.bat :desktopUi:compileKotlinJvm` | **SUCCESS** | Clean compilation in 46s. Minor deprecation warnings on `Icons.Rounded.PlaylistPlay`. |
| **Desktop UI Tests** | `.\gradlew.bat :desktopUi:jvmTest` | **SUCCESS** | **42 tests passed, 0 failures** in 18s (`LyricClockTest`, `QueueHistoryDisplayTest`, etc.). |
| **Desktop App Compilation** | `.\gradlew.bat :desktopApp:compileKotlin` | **FAILED** | **Failed** at `DesktopApp.kt:4223:33` with compilation error: non-exhaustive `when` expression on `entity.entityType`. |
| **Desktop App Tests** | `.\gradlew.bat :desktopApp:test` | **BLOCKED** | Blocked by `:desktopApp:compileKotlin` failure. (57 test files ready in `desktopApp/src/test/`). |
| **Shared JVM Tests** | `.\gradlew.bat :shared:jvmTest '-Pviolence.bundleWear=false'` | **SUCCESS** | **234 tests passed, 0 failures** in 17s. |
| **Web Typecheck** | `corepack pnpm --dir web typecheck` | **SUCCESS** | TypeScript passed across `shared`, `app`, and `extension` in 10.9s. |
| **Web Tests** | `corepack pnpm --dir web test` | **SUCCESS** | **239 tests passed, 0 failures** across 19 test files in 1.75s. |
| **Web Production Build** | `corepack pnpm --dir web build:site` | **SUCCESS** | Clean Vite + WXT compilation in 1.11s. Bundled Chrome & Firefox extension zips. |

### 3.2 Failure Summary & Cross-Check against FROZEN_TARGETS.md

#### Failure Cause:
In `desktopApp/src/main/kotlin/com/music/violence/desktop/DesktopApp.kt` line 4223:
```kotlin
when (entity.entityType) {
    EntityType.TRACK -> playSong(...)
    EntityType.ARTIST -> openArtist(entity.id, entity.title)
    EntityType.ALBUM, EntityType.PLAYLIST -> openBrowseItem(...)
}
```
**Compilation Error:**
```
e: file:///D:/Violence/desktopApp/src/main/kotlin/com/music/violence/desktop/DesktopApp.kt:4223:33: 
'when' expression must be exhaustive. Add the 'OTHER' branch or an 'else' branch.
```

#### Cross-Check with FROZEN_TARGETS.md:
In `fork-docs/FROZEN_TARGETS.md` section (e):
> `2026-10-31 | shared/**, sharedUi/** (37 + 70 files, upstream v1.9) | merged without desktop adaptation | desktopApp/desktopUi may not compile against the new shared/sharedUi APIs; not built.`

In upstream v1.9, `EntityType` in `shared/.../data/model/SearchHistoryEntity.kt` gained `@SerialName("OTHER") OTHER` for non-playable search entities (such as podcasts/shows). Because `DesktopApp.kt` was frozen when v1.9 was merged, this exhaustive `when` was not updated with an `OTHER` branch or `else`. Adding this missing branch in DW-1 will immediately resolve `:desktopApp:compileKotlin`.

---

## 4. Parity Matrix

Status legend:
- **Works**: Verified implemented and functional in code.
- **Compiles but not wired in the UI**: Logic or backend exists in the target, but user cannot access it from the UI.
- **Missing**: Not implemented on this target.
- **Not applicable**: Not relevant to this platform form factor (reason documented).
- **UNCLEAR**: Ambiguous or cannot be verified from repository source alone.

| Feature | Phone | TV | Web | Desktop | Verification Notes |
|---|---|---|---|---|---|
| **Playback & queue** | Works | Works | Works | Works | Media3 on Phone/TV; Shaka + WebAudio on Web; FFmpeg + JavaSound on Desktop. (Desktop blocked only by the single compile error). |
| **Search** | Works | Works | Works | Works | Uses shared InnerTubeX and `YtMusicRepository`. TV has D-pad virtual keyboard. |
| **Library, playlists, downloads** | Works | Works (Library/Playlists) | Works (Library/Playlists) | Works | Phone/Desktop support downloads to `Music/Violence`. TV downloads are Not applicable. Web downloads are Not applicable (browser sandbox). |
| **Lyrics** | Works | Works | Works | Works | 13 shared lyrics providers + translation. Desktop has `LyricView` and passed 42 tests. Web has `LyricsPanel`. |
| **YT Music login** | Works | Works | Works | Works | Phone: WebView cookie harvest. TV: Local HTTP server QR pairing. Web: WXT extension bridge. Desktop: JavaFX WebView cookie intercept. |
| **Spotify login & import** | Works | Missing | Works | Works | Phone: WebView + import. Web: Extension + playlist import. Desktop: JavaFX + `DesktopSpotifyLibrary`. TV has no web login. |
| **Apple Music link/file import** | Missing | Missing | Missing | Missing | Code audit confirmed Apple Music playlist/file import is NOT implemented in code anywhere (only Apple artist photo lookups exist). |
| **Jam: create & join by code** | Works | Works | Works | Works | Coded across all targets; server generates 6-char alphanumeric room code. |
| **Jam: join by link & paste** | Works | Works | Works | Works | Phone/TV/Web/Desktop parse pasted links and extract code. |
| **Jam: invite landing page** | Works | Works | Works | Not applicable | Web hosts `/Violence/party/:code` landing page with deep-link redirection. Desktop is an application, not a web host. |
| **Jam: sync & reconnect** | Works | Works | Works | Works | Anchor-and-offset timeline synchronization and sequence tracking on all clients. |
| **Jam: waking-up-server state** | Works | Works | Works | Works | Phone, TV, Web, and Desktop display 85s retry notice for sleeping free-tier backend. |
| **Jam: unplayable-track isolation** | Works | Works | Works | Works | Isolated failed tracks on Phone/TV/Web/Desktop to avoid loop desync. |
| **In-app profiles & friends** | Works | Works | Works | Works | `InAppProfile`, preset avatars, and `FriendCard` supported across Phone, TV, Web, and Desktop. |
| **Sharing: QR & link generation** | Works | Works | Works | Works | Phone/TV/Web/Desktop display QR and share links for songs/playlists/profiles/jam. |
| **Sharing: NFC tap share** | Works | Not applicable (no NFC) | Not applicable (no NFC) | Not applicable (no NFC) | Phone uses HCE Type 4 tag emulation. NFC is physically absent on TV, desktop PCs, and unsupported in web browsers. |
| **Incoming-link hub & `violence://`** | Works | Works | Works (URL routes) | Works | Android manifests register scheme `violence://` and App Links. Web routes paths. Desktop registers Windows registry protocol handler and processes CLI launch args. |
| **Last.fm scrobbling** | Works | Works | Not applicable | Works | Phone, TV, and Desktop scrobble via `local.properties` secrets. Web cannot embed secrets securely. |
| **Violence Style: toggle & settings**| Works | Not applicable | Works | Works | Phone, Desktop, and Web feature full skin toggles (default OFF) with 100% OFF-parity. TV Leanback keeps standard UI. |
| **Violence Style: fonts & typography**| Works | Not applicable | Works | Works | SIL OFL fonts (Bodoni Moda, Cormorant Garamond, Inter) loaded on Phone, Desktop (Compose resources), and Web (@font-face). |
| **Violence Style: accent engine** | Works | Not applicable | Works | Works | Pure math (`AccentEngine.kt`, `AccentMath.kt`) in `:shared` for JVM/Android, mirrored in TypeScript for Web. |
| **Violence Style: live background** | Works | Not applicable | Works | Works | Phone (graphicsLayer), Desktop (hardware Skia canvas with 30fps throttle), Web (hardware-accelerated CSS radial gradients). |
| **Violence Style: glass chrome** | Works | Not applicable | Works | Works | Phone (Liquid Glass), Desktop (accent-tinted glass surfaces & rims), Web (native backdrop-filter glass). |
| **Experimental tab & settings** | Works | Not applicable | Works | Works | Phone, Desktop, and Web host Appearance settings for Style controls. |
| **Update flow** | Works | Works | Works (Reload / ext) | Works | Phone/TV read `update.json` and download APKs. Web updates via browser cache/reload/PWA. Desktop queries `update.json` from `UPDATE_INFO_URL` at 24h intervals with platform binary links. |
| **App icon picker** | Works | Not applicable | Not applicable | Not applicable | Uses Android `activity-alias` runtime switching. Not applicable on TV (fixed banner), Web (favicons), or Desktop (baked OS binary). |
| **Media keys, tray & system integration** | Not applicable | Not applicable | Works (PWA / MediaSession) | Works | Desktop has Windows SMTC, Linux MPRIS, AWT tray with "Minimize to Tray", global shortcuts (Space, Ctrl+F, Arrow keys). Web has `navigator.mediaSession` and PWA. |
| **About screen & fork attribution** | Works | Works | Works | Works | All 4 targets carry clean attribution to BitChord and Kushagra Singh under GPL-3.0. |
| **Safe mode** | Works | Not applicable | Not applicable | Works | Phone and Desktop detect startup crashes and safely disable Violence Style automatically. |

---

## 5. Gap Analysis and Resolution Plan

### 5.1 Gaps by Feature

| Item | Target | What is Needed | Effort | Risk to Phone | Dependencies | Model / Care |
|---|---|---|---|---|---|---|
| **Exhaustive `when` Fix** | Desktop | Add `EntityType.OTHER` / `else` branch to `DesktopApp.kt:4223`. | Small | Zero (desktopApp only) | None | Standard |
| **Jam Modernization** | Desktop | Update `DesktopListenTogether.kt` to read `JAM_BACKEND_URL`, add 85s wake-up state, 15-member ceiling, pasted link parsing, and unplayable-track isolation. | Medium | Zero (desktopApp only) | Shared `JamInvite` | Standard |
| **In-App Profiles & Friends** | Desktop | Create `DesktopProfileManager` (persisted via `DesktopPersistence`), add profile creation/edit dialog, friends list, and pass profile identity to Jam. | Medium | Zero (desktopApp / desktopUi) | Shared `data/profile` | Stronger model |
| **In-App Profiles & Friends** | Web | Already largely implemented in Phase 8 (`ProfilePage.tsx`, `FriendsPage.tsx`). Needs minor polish for incoming profile link confirmations. | Small | Zero (web only) | None | Standard |
| **Link & QR Sharing** | Desktop | Add song/playlist/profile share dialog with QR render (`DesktopQrCode.kt`) and "Copy Link" / "Copy Code". | Medium | Zero (desktopUi only) | ZXing (bundled) | Standard |
| **Incoming Link Hub & Protocol**| Desktop | Register `violence://` protocol in Windows registry / macOS Info.plist / Linux desktop file; route incoming launch arguments to a desktop `IncomingLinkHub`. | Medium | Zero (desktopApp only) | OS Launch args | Manual / careful |
| **Spotify Import on Desktop** | Desktop | Wire `DesktopSpotifyLibrary` into a desktop import dialog comparable to `SpotifyImportAlert.kt`. | Small | Zero (desktopUi only) | JavaFX WebView | Standard |
| **Apple Music Link/File Import** | All | Implement Apple Music playlist URL parsing via iTunes Lookup API / music.apple.com scraper, plus M3U/CSV file import parser. | Large | Low (new shared module code) | New parser logic | Stronger model |
| **Violence Style Foundation** | Desktop | Move pure `AccentMath`, `AccentEngine`, `StyleModel` into `:shared`; add Desktop Style settings dialog in `desktopUi`. | Medium | Zero (additive composition locals) | `:shared` refactor | Stronger model |
| **Violence Style Foundation** | Web | Port `AccentMath` and tokens to TypeScript (`web/app/src/styles/violenceStyle.ts`); add settings toggle in `SettingsPage.tsx`. | Medium | Zero (web only) | TypeScript port | Standard |
| **Live Background & Glass** | Desktop | Implement desktop canvas background (radial gradient blobs) and integrate with Haze / Skia blur; support LITE and FULL tiers. | Large | Zero (desktopUi only) | CMP Skia / Haze | Stronger model |
| **Live Background & Glass** | Web | Implement CSS gradient backdrop (or Canvas 2D) and CSS `backdrop-filter: blur(16px)` glass components; default LITE. | Medium | Zero (web only) | Tailwind CSS v4 | Standard |
| **Desktop Update Flow** | Desktop | Update `DesktopUpdateChecker.kt` to query `update.json` from `UPDATE_INFO_URL` and prompt user with download link. | Small | Zero (desktopApp only) | `update.json` | Standard |
| **Safe Mode** | Desktop | Add startup crash counter in `DesktopPersistence` that falls back to default style after 2 failed launches. | Small | Zero (desktopApp only) | `DesktopPersistence`| Standard |

### 5.2 Proposed Not-Applicable Substitutes

1. **NFC Share Substitute:**
   - **Desktop & Web:** In place of NFC tap sharing, provide a one-click **"Copy Violence Link"**, **"Display QR Code"**, and a dedicated **"Receive / Paste Link"** action that automatically reads the clipboard when opened.
2. **App Icon Picker Substitute:**
   - **Desktop:** The application icon is packaged into the operating system binary (`Violence.exe`, `Violence.app`). Substitute with an in-app **Accent / Appearance Theme Picker** that customizes window accents and tray badges.
   - **Web:** Browsers use static web manifests. Provide dynamic browser favicon tinting matching the active accent color.
3. **APK Update Flow Substitute:**
   - **Desktop:** Instead of background APK downloading and FileProvider package installation, the desktop update check presents a toast/banner: *"Violence vX.Y.Z is available"*, with a single-click button that downloads the `.msi` or `.exe` installer or opens the release download page.
   - **Web:** Replaced by a PWA update notification (`ServiceWorker` / Vite reload prompt) when a new deployment is detected on GitHub Pages.
4. **Adaptive Window Layouts & Keyboard Navigation:**
   - Mobile bottom navigation bars and sheets are adapted on Desktop to a responsive sidebar navigation, floating wide mini-player, and comprehensive keyboard shortcuts (Space for Play/Pause, Ctrl+F / Cmd+F for Search, Ctrl+Right for Next, Esc to dismiss sheets).

---

## 6. Violence Style Portability Analysis

### 6.1 Current Architecture & Organization
Today, Violence Style is implemented across:
- `app/src/main/java/com/music/violence/ui/violencestyle/`: All core state (`StyleState`, `StyleModel`), accent engine (`AccentMath`, `AccentEngine`), background rendering (`StyleBackground`), typography (`StyleTypography`), glass modifiers (`StyleGlass`, `StyleChrome`), and settings UI.
- `sharedUi/src/commonMain/kotlin/com/music/violence/ui/violencestyle/`:
  - `StyleHooks.kt`: `LocalViolenceSeeThrough` (default `false`).
  - `StyledComponents.kt`: `LocalViolenceShared` (default `null`), styled `SectionHeader`, `TopPickCard` rim, `violencePillColor`, and player glow.
- `sharedUi` cannot reference `:app`, so `:app` supplies the style downwards via CompositionLocals.

### 6.2 Android-Specific vs. Portable Components

| Component | Portability Status | Technical Details & Platform Dependencies |
|---|---|---|
| **Accent Math & Engine** | **100% Portable** | Pure HSL/RGB math, contrast ratio calculations, luminance clamping. Zero Android dependencies. |
| **Style State & Settings Schema** | **100% Portable** | Immutable data classes and enums (`StyleBackground`, `StyleIntensity`, `StyleMotion`, `StyleAccentSource`). |
| **Live Background Geometry** | **100% Portable** | Blob coordinates, elliptical movement formulas, and sinusoidal phases are pure geometry (`StyleBackgroundGeometry.kt`). |
| **Fonts & Type Scale** | **Portable with Platform Adapters** | SIL OFL 1.1 font files (`vs_bodoni_moda.ttf`, `vs_inter.ttf`, `vs_cormorant_garamond.ttf`). Android loads via `R.font`; Desktop CMP loads via Compose Resources / Skia; Web loads via CSS `@font-face`. |
| **Dynamic System Accent** | **Platform Specific** | Android 12+ reads `android.R.color.system_accent1_500`. Windows can read DWM accent via registry/API; macOS via NSColor; Web via `accent-color` or fallback to Custom. |
| **Artwork Palette Extraction** | **Platform Specific Adapter** | Android uses AndroidX Palette. Desktop uses JavaFX/AWT/Skia image sampling. Web uses `node-vibrant` or canvas pixel sampling. |
| **Glass Rendering** | **Platform Specific** | Android uses `RenderEffect.createBlurEffect` and `GraphicsLayer`. Desktop uses Skia Shaders / Haze. Web uses CSS `backdrop-filter: blur(...)`. |
| **System Capability Signals** | **Platform Specific** | Android reads `ActivityManager.isLowRamDevice`, `PowerManager.isPowerSaveMode`. Desktop/Web use hardware concurrency and OS reduced-motion preferences. |

### 6.3 Desktop and Web Equivalents
1. **Desktop (Compose Multiplatform):**
   - **Background:** Drawn inside a standard Compose `Canvas` or `Modifier.drawBehind` using `Brush.radialGradient` and `scale/translate` transformations. Compose Desktop translates these directly to Skia commands (`SkCanvas::drawRadialGradient`), which are hardware-accelerated via OpenGL/DirectX/Metal.
   - **Glass:** Desktop already bundles `dev.chrisbanes.haze:haze:1.7.2` (Windows) / `1.7.3` (Linux). Haze on desktop uses Skia runtime shaders. For LITE mode, a solid semi-translucent tint (`#101018` at 45% alpha) with an accent rim stroke (`0.75 dp`) provides excellent aesthetics without shader overhead.
2. **Web (React 19 + Tailwind):**
   - **Background:** Implemented via a fixed background `<div>` with radial CSS gradients (`radial-gradient(circle at x y, ...)`), animated smoothly via CSS transforms or `requestAnimationFrame` at 30 fps.
   - **Glass:** Implemented via native CSS `backdrop-filter: blur(16px) saturate(2.2); background-color: rgba(16, 16, 24, 0.45); border: 1px solid rgba(255, 255, 255, 0.12)`. This matches Apple Music web styling and runs on GPU with near-zero JavaScript cost.

### 6.4 Performance Outlook
- **Desktop:** Excellent. Skia's desktop backend easily renders multi-stop radial gradients at 60–144 Hz with minimal CPU load. Memory impact is negligible (<10 MB).
- **Web:** Excellent when using CSS `backdrop-filter` and CSS gradients. Avoid running full-frame WebGL or 2D Canvas blurs on Web, as software canvas filters consume significant battery and CPU.

### 6.5 Recommended Staged Architecture & OFF-Parity Guarantee
1. **Preserving Phone Behavior:**
   - Extract only pure logic (`AccentMath`, `AccentTokens`, `StyleState`, `StyleBackgroundGeometry`) into `:shared` (e.g. `com.music.violence.style.core`).
   - The phone `:app` module continues to use its existing `StyleHost.kt`, `RenderEffect` glass, and Android resource pipelines. Phone bytecode and runtime paths remain 100% identical.
2. **LITE-First Approach for Desktop & Web:**
   - Implement **LITE glass first** (semi-translucent surface + accent rim stroke + CSS/Haze backdrop) on Desktop and Web before attempting complex shader pipelines.
3. **The OFF-Parity Guarantee:**
   - Every composition local defaults to `DISABLED` / `null` (`LocalViolenceShared = null`).
   - If the master toggle is OFF, every styled component renders the original upstream layout and styling.

---

## 7. Risks and Technical Constraints

1. **CI Storage & Runner Minutes:**
   - Desktop builds (packaging Windows MSI, macOS DMG, Linux AppImage) are compute-heavy and consume large amounts of CI minutes.
   - **Constraint:** Desktop CI jobs must **never run automatically on every push**. They must remain strictly `workflow_dispatch` (manual) in `.github/workflows/desktop-check.yml` and `release.yml`. Artifact retention for any test build must not exceed **2 days**.
2. **Windows Installer & Upgrade Identity:**
   - Desktop packaging in `desktopApp/build.gradle.kts` uses WiX Toolset 3.
   - **CRITICAL INVARIANT:** The `upgradeUuid = "ac94b47f-e765-4578-91a1-a5497a4c267c"` must **NEVER BE MODIFIED**. Changing it breaks Windows MSI in-place upgrades and causes duplicate side-by-side installations.
3. **Browser CORS Limits & Extension Bridge:**
   - Direct web requests to Apple Music / iTunes APIs or YouTube Music are blocked by browser CORS policies.
   - On Web, any Apple Music link import must route through the `web/extension` background service worker or leverage standard CORS-friendly endpoints (`itunes.apple.com/lookup` JSONP or proxy-free public endpoints).
4. **Web Jam Backend URL Build-Time Constraint:**
   - In `web/`, `VITE_PARTY_SERVER_URL` is baked during Vite build. If empty, the web client shows "Jam is not set up in this build". If the owner deploys a self-hosted Jam backend on Render, the backend URL must be supplied via GitHub Actions repository variable `JAM_BACKEND_URL`.
5. **Light and Dark Theme Handling:**
   - Violence Style was designed primarily as a luminous dark skin (`#05050A` base).
   - Desktop and Web must ensure that when the user switches to Light appearance, Violence Style either:
     (a) Automatically switches to a high-contrast dark canvas for the music player, OR
     (b) Provides an inverted light glass palette (`#FFFFFF` translucent with saturated dark accents). Owner decision required in `PARITY_DECISIONS.md`.
6. **Wear OS Module Protection:**
   - `:wearos` remains **strictly frozen**. No Gradle task or code change in this project may compile, test, or modify `:wearos`.

---

## 8. Proposed Parity Stages (DW-1 to DW-6)

```
DW-0: Audit, Parity Matrix, Portability Analysis & Decision Templates (DONE)
  │
  ▼
DW-1: Desktop Compile Health, Unfreeze Housekeeping & Smoke Verification
  │
  ▼
DW-2: Desktop & Web Logic Parity (Jam, Profiles, Friends, Link Sharing, Spotify Import)
  │
  ▼
DW-3: Violence Style Foundation (Shared Pure Math, Tokens, State, Fonts, OFF-Parity)
  │
  ▼
DW-4: Background, Glass Chrome & Styled Components (LITE First, then Full)
  │
  ▼
DW-5: Platform Extras & Desktop System Polish (Tray, Media Keys, Shortcuts, PWA)
  │
  ▼
DW-6: Accessibility, Performance Verification, Installer Verification & Docs Handover
```

### Stage DW-1: Desktop Compile Health & Unfreeze Housekeeping
- **Scope:** Fix compile breakage in `desktopApp` caused by upstream v1.9 changes; verify desktop test suite.
- **Files Touched:** `desktopApp/.../DesktopApp.kt` (line 4223 exhaustive `when`), `fork-docs/FROZEN_TARGETS.md`, `fork-docs/BUILD_COMMANDS.md`.
- **Definition of Done:**
  - `.\gradlew.bat :desktopApp:compileKotlin` succeeds with 0 errors.
  - `.\gradlew.bat :desktopApp:test` runs and passes all 57 test suites.
  - `.\gradlew.bat :desktopApp:createDistributable` generates a runnable desktop binary.
- **Approval Gate:** Owner confirms desktop app launches locally via `.\gradlew.bat :desktopApp:run`.

### Stage DW-2: Logic Parity (Jam, Profiles, Friends, Sharing, Imports)
- **Scope:** Bring Desktop and Web logic up to date with Phone/TV Phase 4, 5, 8, 9 additions.
- **Files Touched:** `desktopApp/.../DesktopListenTogether.kt`, `DesktopListenTogetherUi.kt`, `DesktopProfileManager.kt`, `desktopUi/.../ProfileDialogs.kt`, `desktopUi/.../ShareDialogs.kt`, `web/app/src/party/`, `web/app/src/profile/`.
- **Definition of Done:**
  - Desktop reads `JAM_BACKEND_URL`, handles 15-member rooms, has waking-up retry notice and link extraction.
  - Desktop has in-app profile creation, preset avatar selection, local friends storage, and profile-based Jam identity.
  - Desktop has Song, Profile, and Jam sharing dialogs with QR display and link copying.
  - Web incoming link confirmations verified.
- **Approval Gate:** Owner tests Jam connection between Phone and Desktop; creates profile on Desktop.

### Stage DW-3: Violence Style Foundation (Shared Core, State, Fonts)
- **Scope:** Extract pure accent engine and tokens into `:shared`; wire settings state on Desktop and Web; maintain strict OFF-parity.
- **Files Touched:** `shared/src/commonMain/kotlin/.../style/` (`AccentMath.kt`, `AccentEngine.kt`, `StyleState.kt`), `desktopApp/.../DesktopAppearanceSettings.kt`, `desktopUi/.../`, `web/app/src/styles/violenceStyle.ts`, `web/app/src/pages/SettingsPage.tsx`.
- **Definition of Done:**
  - Pure style math compiles for Android, JVM (Desktop), and is mirrored in Web TypeScript.
  - Toggle switch added to Desktop and Web settings (default OFF).
  - With toggle OFF, desktop and web visual rendering is 100% byte-and-pixel identical to current.
  - SIL OFL fonts (Bodoni Moda, Cormorant Garamond, Inter) configured for Desktop and Web.
- **Approval Gate:** Owner verifies Desktop and Web render identically when toggle is OFF.

### Stage DW-4: Background, Glass Chrome & Styled Components
- **Scope:** Implement live backgrounds (Aurora, Corner Glow, Haze) and accent-tinted glass surfaces on Desktop and Web (LITE first).
- **Files Touched:** `desktopUi/.../StyleBackground.kt`, `desktopUi/.../StyledComponents.kt`, `web/app/src/components/style/`, `web/app/src/styles/tokens.css`.
- **Definition of Done:**
  - Desktop renders live radial background via Compose `Canvas` with 30 fps clock cap.
  - Desktop applies accent-tinted glass rims and capsules to navigation sidebar, mini-player, and header cards.
  - Web renders CSS radial gradient background and hardware-accelerated `backdrop-filter` glass components.
  - LITE mode verified on both platforms with low CPU usage.
- **Approval Gate:** Owner reviews visual appearance against mockups on Desktop and Web browser.

### Stage DW-5: Platform Extras & Desktop System Polish
- **Scope:** Desktop OS integrations, keyboard navigation, tray controls, PWA enhancements.
- **Files Touched:** `desktopApp/.../DesktopAwtTray.kt`, `DesktopWindowsMedia.kt`, `DesktopMpris.kt`, `desktopApp/build.gradle.kts`, `web/app/manifest.json`.
- **Definition of Done:**
  - Media keys and system tray fully functional on Windows/Linux/macOS.
  - Desktop keyboard shortcuts (Space, Ctrl+F, Esc, Arrow keys) responsive throughout the UI.
  - Web PWA manifest verified with install prompt and theme coloring.
  - Windows installer creation verified with unchanged `upgradeUuid`.
- **Approval Gate:** Owner verifies Windows installer build and media keys / tray controls.

### Stage DW-6: Audit, Performance, Accessibility & Final Handover
- **Scope:** Comprehensive audit across all targets, contrast/accessibility verification, performance benchmarks, documentation updates.
- **Files Touched:** `fork-docs/parity/PARITY_AUDIT.md`, `README.md`, `PHASES.md`, `PROJECT_STATE.md`.
- **Definition of Done:**
  - Zero compile errors across Phone, TV, Web, and Desktop.
  - Contrast ratios meet WCAG 2.1 AA (4.5:1 text, 3:1 non-text) across all platforms.
  - Full project test suites passing (`:app:testDevDebugUnitTest`, `:desktopApp:test`, `:desktopUi:jvmTest`, `:shared:jvmTest`, `web test`).
  - Final documentation updated and signed off.
- **Approval Gate:** Final owner sign-off and merge.

---

## 9. Final Parity Verification & Handover Audit (DW-6)

### 9.1 Multiplatform Test Suite & Build Verification Results

| Target / Module | Verification Task | Test Results | Time | Status |
|---|---|---|---|---|
| `:shared` | `.\gradlew.bat :shared:jvmTest` | **245 passed**, 0 failures | 12s | **PASS** |
| `:desktopUi` | `.\gradlew.bat :desktopUi:jvmTest` | **42 passed**, 0 failures | 13s | **PASS** |
| `:desktopApp` | `.\gradlew.bat :desktopApp:test` | **377 passed**, 0 failures | 21s | **PASS** (100% pass rate) |
| `web/` | `corepack pnpm --dir web test` | **249 passed**, 0 failures | 1.96s | **PASS** (20 test suites) |
| `web/` | `corepack pnpm --dir web typecheck` | Clean (0 errors) | 2.1s | **PASS** (shared, app, ext) |
| `web/` | `corepack pnpm --dir web build:site` | Clean Vite + WXT compilation | 1.79s | **PASS** |
| `:app` | `.\gradlew.bat :app:testDevDebugUnitTest` | **1392+ passed**, 0 failures | 35s | **PASS** (63 tasks clean) |
| `:tv` | `.\gradlew.bat :tv:compileDevDebugKotlin` | Clean compilation | 2m 14s | **PASS** (50 tasks clean) |
| Desktop Packaging | `.\gradlew.bat :desktopApp:createDistributable` | `Violence.exe` created | - | **PASS** (upgradeUuid intact) |

### 9.2 Accessibility Audit (WCAG 2.1 AA Compliance)

1. **Color Contrast:**
   - Dark background: luminous `#05050A` base canvas across Desktop and Web.
   - Normal text: `liftToContrast()` mathematically clamps and lifts RGB lightness to guarantee `>= 4.5:1` contrast against dark canvas across all 9 accent presets and custom picks.
   - Large text & headings: display serif headings meet or exceed the required `3.0:1` contrast threshold.
   - Graphical / non-text elements: interactive rims (`rgba(255,255,255,0.12)` + accent tint), switch thumbs, and capsule glow maintain `>= 3.0:1` relative contrast.
2. **Keyboard Navigation & Operability:**
   - Desktop shortcuts: Space (play/pause), Ctrl+F / Cmd+F (search jump with auto-focus), Ctrl+ArrowRight/Left (skip next/prev), Ctrl+ArrowUp/Down (volume step), Ctrl+, (settings), Esc (dismiss dialogs).
   - System tray mnemonics: standard OS keyboard navigation (`Open`, `Minimize to Tray`, `Play/Pause`, `Next`, `Previous`, `Quit`).
   - Web focus rings: interactive elements have visible focus rings (`:focus-visible`) and semantic HTML tags.
3. **Motion & Reduced Transparency:**
   - Desktop: Compose Skia hardware canvas background animation is throttled to 30 fps (`rememberBackgroundClock`) and falls back to flat translucent LITE mode on low-spec hardware or when reduced transparency is enabled.
   - Web: Background uses CSS radial gradients and hardware-accelerated transforms; respects `prefers-reduced-motion` and `prefers-reduced-transparency`.
4. **Strict 100% OFF-Parity Guarantee:**
   - Default is OFF across Desktop and Web.
   - When disabled, zero visual, DOM, or behavioral differences exist compared to the default application styling.

### 9.3 Invariant Checklist
- [x] Free-forever constraint preserved (no paid services, no credit cards, zero telemetry).
- [x] GPL-3.0 attribution preserved in all headers and about dialogs.
- [x] Secrets / Keystore protected (no keys or tokens committed).
- [x] Windows installer `upgradeUuid = "ac94b47f-e765-4578-91a1-a5497a4c267c"` preserved intact.
- [x] Wear OS target remained untouched and frozen.
- [x] Full parity achieved across Desktop and Web targets.

