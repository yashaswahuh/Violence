# System Architecture & Technical Audit

**Audit Date:** 2026-10-29  
**Upstream Repository:** `https://github.com/kushagrasinghx/BitChord`  
**Fork:** Private fork for personal use (Android, Android TV, and Web targets)

---

## 1. Module Map & Dependency Graph

The repository is structured as a multi-target Kotlin Multiplatform and Compose Multiplatform project, accompanied by a native C++ audio engine, an independent Go synchronization server, and a web player with a browser extension.

```
                    ┌─────────────────────────┐
                    │      root Project       │
                    │   (settings.gradle.kts) │
                    └────────────┬────────────┘
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     │                           │                           │
┌────┴────────┐            ┌─────┴───────┐             ┌─────┴───────┐
│   :shared   │◄───────────┤  :sharedUi  │             │   backend   │ (Go 1.27)
│ (KMP Lib)   │            │  (CMP Lib)  │             │  (Standalone│  Listen Together
└────┬───▲────┘            └─────┬───────┘             │   Server)   │  & Presence
     │   │                       │                     └─────────────┘
     │   ├───────────────────────┤
     │   │                       │
┌────┴───┴────┐            ┌─────┴───────┐             ┌─────────────┐
│    :app     │            │     :tv     │             │     web     │ (pnpm monorepo)
│(Android App)│◄───────────┤(Android TV) │             │├─ app/      │  React 19 + Vite
└────┬───▲────┘ (sourceSets│(Reuses :app)│             │├─ extension/│  WXT MV3 Ext.
     │   │       inclusion)└─────────────┘             │└─ shared/   │  Bridge protocol
     │   │                                             └─────────────┘
     │   └───────────────────────┐
     │                           │
┌────┴────────┐            ┌─────┴───────┐             ┌─────────────┐
│  :wearos    │            │ :desktopApp │             │   native    │ (C++ CMake)
│ (Wear OS)   │            │(Desktop JVM)│             │  Analyzer & │ (Compiled by
│(Bundled APK)│            └─────┬───────┘             │   FFmpeg    │  :app, :tv,
└─────────────┘                  │                     └─────────────┘  :desktopApp)
                           ┌─────┴───────┐
                           │ :desktopUi  │
                           │(Desktop CMP)│
                           └─────────────┘
```

### Module Descriptions & Target Alignment

| Module Directory | Type / Build System | Depends On | Targets Fed | Current Role & Audit Notes |
|---|---|---|---|---|
| `:shared` | KMP Library (`androidTarget`, `jvm`) | Ktor 3.5.2, InnerTubeX 0.7.0, NewPipeExtractor 0.26.3, Jsoup 1.22.2, Kotlinx Serialization | `:app`, `:tv`, `:desktopApp`, `:desktopUi` | Core domain, playback contracts, lyrics repository & providers, Spotify match models, Jam protocol models, server clock sync, presence client. |
| `:sharedUi` | CMP Library (`androidTarget`, `jvm`) | `:shared`, Compose Multiplatform 1.10.3, Haze 1.3.1, Coil 3.0.4 | `:app`, `:tv` | Cross-platform UI components, now-playing screen, synced lyrics rendering, album mesh gradients, queue management views. |
| `:app` | Android Application (`com.android.application`) | `:shared`, `:sharedUi`, Media3 1.11.0, CMake `app/src/main/cpp`, WearOS protocol & bundled APK | Android (Phone / Tablet) | Full Android client. Hosts playback foreground service, audio caching, DSP JNI bindings, download manager, settings, deep linking. |
| `:tv` | Android TV Application (`com.android.application`) | `:shared`, `:sharedUi`, Media3 1.11.0, CMake `app/src/main/cpp`, reuses `:app` source trees | Android TV / Google TV | Leanback TV interface. Compiles directly against `app/src/main/java`, `res`, `assets`, `jniLibs`; patches `WatchInstaller.kt` & `WearBridge.kt` with stubs. |
| `:wearos` | Wear OS Application (`com.android.application`) | Compose for Wear OS, Play Services Wearable | Wear OS Watch | Standalone watch interface with local speaker playback and phone streaming bridge. Packaged inside phone APK assets via `:app:bundleWearApk`. |
| `:desktopApp` | Desktop Application (`org.jetbrains.kotlin.jvm`) | `:shared`, `:desktopUi`, JavaFX 21 (WebKit), JavaCPP FFmpeg 7.1.1, ONNX Runtime 1.29.0 | Desktop (Windows, Linux, macOS) | Desktop launcher, system tray, SMTC Windows media controls, JavaFX BotGuard WebView, package bundler (MSI, EXE, AppImage, DEB, RPM, DMG). |
| `:desktopUi` | Desktop UI (`org.jetbrains.kotlin.multiplatform`) | `:shared`, CMP 1.10.3 / 1.12.0, Haze, Coil 3 | `:desktopApp` | Desktop window layout, desktop navigation sidebar, wide-screen now-playing view. Originally branched from `:sharedUi`. |
| `native/` | Raw C++ sources (CMake) | Standard C++17, Android NDK log lib | `:app`, `:tv`, `:desktopApp` | Whole-track DSP analysis, beat & downbeat spectrogram front-end (`mel_spectrogram`), STFT vocal spectrogram, audio resampler, DSD/DST decoder. |
| `backend/` | Standalone Go service (Go 1.27) | `github.com/gorilla/websocket v1.5.3` | All clients (Listen Together, Live Stats) | In-memory room hub, state heartbeat ticker, monotonic clock, REST endpoints, WebSocket event broker, presence tracking, live SVG badge renderer. |
| `web/` | Standalone pnpm monorepo | React 19, Vite, WXT, Shaka Player, TanStack Query | Web Browsers | Web player replicating Apple Music web interface. `web/app` (Vite SPA), `web/extension` (browser extension bridge), `web/shared` (protocol types). |

---

## 2. Tech Stack & Version Specifications

### Build Tooling & Compilers
- **Gradle:** 8.10.x compatible (via `gradlew` wrapper)
- **Android Gradle Plugin (AGP):** `8.10.1` (configured in root `build.gradle.kts`)
- **D8/R8 Compiler:** `8.13.23` (explicit classpath override in root `build.gradle.kts:32` to fix Compose 36-parameter phi register allocation bugs in debug dexing)
- **Kotlin:** `2.3.20` (`org.jetbrains.kotlin.android`, `multiplatform`, `jvm`, `plugin.compose`, `plugin.serialization`)
- **Compose Multiplatform (CMP):** `1.10.3` (with desktop Linux resolving `1.12.0`)
- **CMake:** `3.22.1` (declared in `app/build.gradle.kts` and `tv/build.gradle.kts`)
- **Java / JDK:**
  - Android & TV builds: **JDK 17** (Eclipse Temurin recommended)
  - Desktop builds: **JDK 21** (required by JavaFX 21 and modern jpackage)
- **Node.js:** Node `22+` with `pnpm` (configured in `web/package.json`)
- **Go:** `go 1.27.0` (declared in `backend/go.mod`; backward compatible to `go 1.22+`)

### Android SDK Target Specifications
- **Compile SDK (`compileSdk`):** `37` (InnerTubeX AAR requires compiling against SDK 37 platform headers; silenced via `android.suppressUnsupportedCompileSdk=36,37` in `gradle.properties`)
- **Target SDK (`targetSdk`):** `36` (Android 15 runtime behavior)
- **Minimum SDK (`minSdk`):** `26` (Android 8.0 Oreo, ensuring wide device reach with hardware render effects on API 31+)
- **Version Code:** `28` (app) / `26` (tv)
- **Version Name:** `1.8.1` (with pre-release suffix `beta1` in `extra["violenceBetaSuffix"]`)

### Key Frameworks & Dependencies
- **Audio Playback Engine:**
  - Android & TV: AndroidX Media3 `1.11.0` (`media3-exoplayer`, `media3-session`, `media3-common`, `media3-datasource-okhttp`, `media3-exoplayer-hls`, `media3-exoplayer-dash`)
  - Web: Shaka Player (DASH/SABR streaming) + Web Audio API
  - Desktop: JavaCPP FFmpeg `7.1.1-1.5.12` + custom audio sink
- **Networking & Serialization:**
  - Ktor Client: `3.5.2` (OkHttp engine, ContentNegotiation, WebSockets)
  - Kotlinx Serialization: `1.11.0` (JSON serializer)
  - OkHttp: `5.3.2` / `4.12.0` (core HTTP engine with connection pooling)
  - Jsoup: `1.22.2` (HTML parsing for lyric scrapers & NewPipe fallback)
- **YouTube Extraction Layer:**
  - InnerTubeX: `v0.7.0` (`com.github.MetrolistGroup.innertubex:innertubex-android`)
  - NewPipeExtractor: `v0.26.3` (`com.github.TeamNewPipe:NewPipeExtractor`, stripped jar removing duplicate `Utils.class`)
  - Rhino & Rhino-Engine: `1.8.1` (ECMAScript engine solving player cipher signatures)
  - QuickJS: `1.0.14` (`io.github.dokar3:quickjs-kt-android` for JavaScript module execution)
- **UI & Graphics:**
  - Jetpack Compose BOM: `2024.12.01`
  - Compose Foundation: `1.10.0` (pinned for `OverscrollFactory`)
  - Compose Material 3: `1.3.1` (Android) / `1.7.3` (CMP)
  - Compose for TV: `androidx.tv:tv-material:1.0.0`, `androidx.tv:tv-foundation:1.0.0-alpha12`
  - Blur / Translucency: Haze `1.3.1` (Android/TV), Haze `1.7.2` (Windows desktop), Haze `1.7.3` (Linux desktop)
  - Image Loading: Coil 3 `3.0.4` (`coil-compose`, `coil-network-okhttp`, `coil-svg`)
  - Rich Text: `com.halilibo.compose-richtext:richtext-ui-material3:0.20.0`
- **Machine Learning & DSP:**
  - ONNX Runtime: `1.28.0` (Android), `1.29.0` (Desktop)
  - Bundled Models: Beat This! beat/downbeat tracker, Open-Unmix vocal separation
- **Hardware Integration & Utilities:**
  - Google Cast: `play-services-cast-framework:22.2.0`, `androidx.mediarouter:1.8.1`
  - SMB Shares: SMBJ `0.15.0` (pure-Java SMB2/SMB3 client)
  - QR Generator: ZXing Core `3.5.3` (matrix encoding only, custom Compose rendering)

---

## 3. End-to-End Playback Architecture

### 3.1 Search Flow
1. User enters text in the search bar (`SearchScreen.kt` or TV `TvSearchScreen.kt`).
2. Search request routes through `YtMusicRepository.search(query, filter)` in `shared`.
3. Calls YouTube Music InnerTube endpoint (`/youtubei/v1/search`) via Ktor using client identity `WEB_REMIX`.
4. Response is parsed into `SearchResult.Track`, `SearchResult.Album`, `SearchResult.Artist`, and `SearchResult.Playlist`.
5. If pluggable sources are enabled (`SourceRegistry.kt`), `SourceResolver` concurrently searches active addons and JioSaavn (`JioSaavnSource.kt`), interleaving or ranking results according to user configuration.

### 3.2 Stream URL Resolution Pipeline
Resolution converts a track `videoId` or addon track ID into an playable media stream without routing audio through any intermediary server:

```
Track videoId
     │
     ▼
[SourceResolver.resolve()]
     │
     ├─► If Addon Source ──► HTTP Addon API /stream endpoint
     ├─► If JioSaavn      ──► JioSaavn API decryptUrl (DES) ──► AAC 320kbps CDN
     └─► If YouTube Music (Default):
              │
              ▼
       [StreamResolver.kt]
              │
              ├─► Tier 1: InnerTubeX Resolver (Fast path, live-benchmarked clients)
              │         ├─ Client walk: VISIONOS ──► WEB_REMIX (SABR) ──► WEB ──► MWEB
              │         ├─ BotGuard PO Token Generator (PoTokenGenerator.kt via WebView)
              │         └─ Signature & 'n' parameter deciphering via Rhino JS engine
              │
              ├─► Tier 2: NewPipeExtractor (Failsafe fallback)
              │         └─ Scrapes watch page HTML, resolves ciphered audio streams
              │
              └─► Tier 3: Pre-playback HTTP Probe
                        └─ Dispatches HTTP HEAD / Range request to googlevideo.com CDN
                        └─ Validates 200/206 response before committing to ExoPlayer
```

### 3.3 Audio Playback & Media Session
- **Playback Engine:** AndroidX Media3 `ExoPlayer` instance initialized in `PlaybackService.kt`.
- **Media Session:** `MediaLibraryService` / `MediaSessionService` exposing platform `MediaSession` to Android OS, lock screen controls, notification shade, Bluetooth metadata, and Android Auto (`CarAppService`).
- **Decoder Hierarchy:**
  1. Platform hardware decoders via MediaCodec (Opus, AAC, FLAC).
  2. Software decoders for missing platform formats.
  3. Bundled native `libffmpegJNI.so` (custom build of librempeg) registered as the final audio renderer, providing decoding for ALAC, AC-4, Dolby Atmos (E-AC-3 / TrueHD), and DTS.
- **Audio Processors:** Inserted into ExoPlayer's `AudioSink` chain:
  - `EqualizerProcessor.kt`: System and in-app 10-band equalization.
  - `LoudnessProcessor.kt`: ReplayGain / EBU R128 loudness normalization.
  - `SpatialAudioProcessor.kt`: Headphone spatialization and multi-channel downmixing.

### 3.4 True Crossfade & Automix [Beta]
- **Dual-Player Architecture:** Managed by `CrossfadeController.kt`. Holds two independent ExoPlayer instances (`primaryPlayer` and `secondaryPlayer`).
- **Standard Crossfade:** 0–12 seconds adjustable. Approaching the end of the current track, the secondary player buffers the next track, starts playback silently, and ramps up volume using equal-power sine/cosine or exponential curves while the primary player ramps down.
- **Automix Transition Engine:**
  - Background audio analysis via native C++ (`native/analyzer/audio_analysis.cpp`, `mel_spectrogram.cpp`) and ONNX models (`BeatTracker.kt`, `VocalTracker.kt`).
  - Evaluates track tempo (BPM), beat grids, downbeats, key, and vocal entry/exit boundaries.
  - `TransitionPlanner.kt` generates an optimal transition schedule: beat-matched tempo ramping, EQ bass swap (high-pass on outgoing, low-pass on incoming), and vocal-collision avoidance.

### 3.5 Offline Downloads
- Handled by `DownloadService.kt` utilizing Media3 `DownloadManager`.
- Downloads are executed as background chunks with network constraints (Wi-Fi only preferences).
- Completed streams are tagged with ID3v2.4 / MP4 metadata (title, artist, album, embedded artwork, ISRC) and stored in encrypted or standard application-scoped storage.

### 3.6 Synced Lyrics & On-Demand Translation
- **Lyrics Retrieval:** Handled by `LyricsRepository.kt` querying 13 independent lyrics providers in parallel/cascade (LRCLIB, BiniLyrics, lrc.red, BetterLyrics, PaxSenix, LyricsPlus, SimpMusic, Unison, Megalobiz, KuGou, Musixmatch, Genius, and YouTube timed text).
- **Format Support:** TTML XML and Enhanced LRC parsing with millisecond word-level and syllable-level timestamps (`LyricLine.kt`, `TtmlLyrics.kt`).
- **Animation Renderer:** Custom Compose canvas animation (`am-lyrics` port) rendering Apple Music-style dynamic fluid highlighting, vocal bloom, and blur on inactive lines.
- **On-Demand Translation:** `LyricsTranslation.kt` batches lyric text slots (up to 3,500 characters) and sends them to Google's lightweight web translation endpoint (`https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=<target>&dt=t`). Retains line timestamps, vocal envelopes, and projects character progress onto translated glyphs.

### 3.7 Spotify Linking & Library Import
- User provides their `sp_dc` cookie in Settings.
- `SpotifyToken.kt` runs an offscreen Android `WebView` pointing to `open.spotify.com`, harvesting short-lived bearer tokens and client tokens.
- `SpotifyLibrary.kt` fetches user's Saved Tracks ("Liked Songs") and custom playlists via Spotify Web API.
- Matches Spotify items to YouTube Music catalog using ISRC code matching with fallback to fuzzy title/artist string comparison (`SpotifyMatch.kt`).
- `SpotifyCanvas.kt` queries Spotify's Pathfinder GraphQL endpoint to fetch and cache animated vertical canvas videos (`.cnvs.mp4`).

### 3.8 Scrobbling Integration
- `ScrobbleManager.kt` monitors playback milestones (starts, 50% completion, track ends).
- **Last.fm:** `LastFM.kt` formats XML-RPC/REST calls with MD5 HMAC parameter signatures (`apiSig`), sending "now playing" pings and scrobble entries to `https://ws.audioscrobbler.com/2.0/`.
- **ListenBrainz:** `ListenBrainzManager.kt` submits JSON listen payloads to `https://api.listenbrainz.org/1/submit-listens` using the user's personal token.

### 3.9 Discord Rich Presence
- Implemented in `DiscordRPC.kt` (using KizzyRPC).
- Establishes a direct WebSocket connection to Discord Gateway (`wss://gateway.discord.gg/?v=9&encoding=json`).
- Authenticates using the user's Discord personal account token, masquerading as the Discord Android client via `SuperProperties`.
- Pushes live rich presence activities: track title, artist, album artwork, and playhead timestamps with countdown calculations.

---

## 4. Boundary Analysis: On-Device Logic vs. Backend Logic

A fundamental architectural principle of this codebase is that **all media handling and third-party API integration happens on the client device**. The backend has no role in media ingestion, transcoding, or proxying.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          ON-DEVICE LOGIC                               │
│                                                                        │
│  • YouTube Music / InnerTube API scraping & deciphering                │
│  • Direct media stream downloads from CDNs (googlevideo.com)           │
│  • Audio playback, decoding (MediaCodec + FFmpeg), DSP, EQ             │
│  • Crossfade, gapless buffering, Automix tempo/beat analysis           │
│  • Offline download management & file tag writing                      │
│  • Lyrics scraping, TTML/LRC parsing, on-demand translation            │
│  • Spotify session token harvesting, canvas scraping, library import   │
│  • Scrobbling to Last.fm and ListenBrainz                              │
│  • Discord Gateway WebSocket rich presence connection                  │
│  • Local caching (HTTP response cache, image cache, audio cache)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Small JSON sync frames
                                    │ (Room state, heartbeat, pings)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          BACKEND LOGIC                                 │
│                                                                        │
│  • Room creation, joining, authentication via bearer tokens            │
│  • Shared queue state holder & delta broadcast                         │
│  • Playback anchor-and-offset timeline synchronization                 │
│  • Monotonic server clock for NTP-style client offset calculations     │
│  • In-memory open-app presence tracking & live SVG stats badge         │
│  • Web invite landing page with deep link redirection                  │
│                                                                        │
│  * ZERO audio data handled                                             │
│  * ZERO third-party API proxying                                       │
│  * ZERO database persistence                                           │
└────────────────────────────────────────────────────────────────────────┘
```

This clean boundary guarantees that self-hosting the backend requires minimal compute, zero storage, and minimal bandwidth.
