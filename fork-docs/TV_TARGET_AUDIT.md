# Android TV Target Audit & Build Architecture

**Audit Date:** 2026-10-29  
**Module Directory:** `/tv`  
**Application ID:** `com.music.bitchord.tv` (prod) / `com.dev.bitchord.tv` (dev)  
**SDK Versions:** `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`  
**Primary Configuration:** `tv/build.gradle.kts` (497 lines) and `tv/src/main/AndroidManifest.xml`

---

## 1. Module Contents & Structural Design

The `tv` module provides an Android TV / Google TV experience with a remote-first, 10-foot UI designed for d-pad navigation.

Rather than maintaining a duplicated copy of the phone application's business logic, the TV target is engineered as a **specialized overlay** on top of the `:app` module.

```
tv/
├── build.gradle.kts              # Build config; links :app sourceSets and CMake
├── README.md                     # Module documentation
└── src/
    ├── dev/
    │   └── AndroidManifest.xml   # Dev build applicationId and flags
    └── main/
        ├── AndroidManifest.xml   # Android TV manifest (LEANBACK_LAUNCHER, TvActivity)
        ├── java/
        │   └── com/music/bitchord/
        │       ├── TvActivity.kt # Main Android TV Leanback activity
        │       ├── data/         # TV-specific startup, USB scanning, PIN throttling
        │       ├── ui/
        │       │   ├── TvSignIn.kt
        │       │   └── tv/       # Full Compose for TV interface
        │       │       ├── TvApp.kt
        │       │       ├── screens/   # Home, Explore, Library, Search, Settings, NowPlaying
        │       │       ├── components/# D-pad focus handlers, cards, buttons, lists
        │       │       ├── dialogs/   # TV-specific modals (About, Refresh Rate, Sleep Timer)
        │       │       ├── auth/      # QrCodeGenerator, embedded TvAuthServer
        │       │       └── audio/     # TvSpatialAudioEngine
        │       └── wear/         # REPLACEMENT STUBS: WatchInstaller.kt & WearBridge.kt
        └── res/                  # TV-only assets: tv_banner.png, fonts, tv_themes.xml
```

---

## 2. Key Differences from the Phone App (`:app`)

### 2.1 Remote-First UI & Focus Management
- Uses **Compose for TV** (`androidx.tv:tv-material:1.0.0` and `androidx.tv:tv-foundation:1.0.0-alpha12`).
- Implements custom d-pad directional navigation, programmatic focus restoration, and scale/glow states on focused elements (`TvFocus.kt`, `TvCard.kt`).
- Features a wide-screen 10-foot now-playing screen (`TvNowPlayingScreen.kt`) with large artwork, full-screen synced lyrics, and backdrop blur.

### 2.2 Source Set Re-Use (The Zero-Duplication Engine)
In `tv/build.gradle.kts:124-142`, the TV build directly injects the phone app's source trees:
```kotlin
sourceSets {
    getByName("main") {
        java.srcDir("../app/src/main/java")
        res.srcDir("../app/src/main/res")
        assets.srcDir("../app/src/main/assets")
        jniLibs.srcDir("../app/src/main/jniLibs")
    }
    getByName("debug") { java.srcDir("../app/src/debug/java") }
}

externalNativeBuild {
    cmake {
        path = file("../app/src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}
```
All core services—`PlaybackService`, `AudioCache`, `CrossfadeController`, `StreamResolver`, `LyricsRepository`, `SourceRegistry`, and native C++ DSP—are compiled directly from `:app` without duplicate code files.

### 2.3 The Class Patching / Override Mechanism
When the TV build needs to change behavior from the phone app, it places a replacement Kotlin file in `tv/src/main/java/` at the exact same relative package path as the phone file.

In `tv/build.gradle.kts:483-497`, a custom Gradle exclusion detects these duplicates and excludes the phone copy from compilation:
```kotlin
val phoneJava = file("../app/src/main/java")
val tvJava = file("src/main/java")
val replacedByTv: Set<String> = fileTree(tvJava) { include("**/*.kt") }.files
    .map { it.relativeTo(tvJava).invariantSeparatorsPath }
    .filter { File(phoneJava, it).isFile }
    .toSet()

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude { el ->
        el.file.startsWith(phoneJava) && el.relativePath.pathString in replacedByTv
    }
}
```

#### Files Currently Patched by TV:
1. `com/music/bitchord/wear/WatchInstaller.kt`:
   - In `:app`: Implements an in-app wireless ADB client (`libadb-android`) to sideload the Wear OS APK onto a watch.
   - In `:tv`: A 33-line no-op stub object. TVs have no connected watches, and bundling the ADB engine into TV would waste binary size.
2. `com/music/bitchord/wear/WearBridge.kt`:
   - In `:app`: Connects to Google Play Services Wearable Data Layer.
   - In `:tv`: A no-op stub object preventing wearable sync routines from initializing on Android TV.

### 2.4 TV-Specific Features
- **Local QR Code Auth Server (`TvAuthServer.kt`):** Since typing complex Google passwords with a TV remote is cumbersome, the TV app spins up a tiny local HTTP server on port 8080 and displays a QR code (`QrCodeGenerator.kt`). The user scans the QR code with their phone, and the phone posts the YouTube authentication cookies directly to the TV over the local Wi-Fi network.
- **Display Refresh-Rate Switching (`TvRefreshRateController.kt`):** Automatically detects connected TV/projector display mode capabilities and switches HDMI refresh rates (e.g. 24Hz, 50Hz, 60Hz) to prevent judder.
- **USB OTG Storage Scanner (`TvUsbMusic.kt`):** Monitors Android TV USB ports for plugged-in flash drives and indexes local offline audio files.
- **Spatial Audio Engine (`TvSpatialAudioEngine.kt`):** Integrates with home theater audio receivers and soundbars for multi-channel audio output.

---

## 3. Requirements to Keep `:tv` Building Safely

Because of the tight coupling between `:tv` and `:app`, maintenance in the private fork requires adherence to the following rules:

### 1. Maintain Interface Compatibility in `:app`
- Any refactoring, renaming, or deleting of classes under `app/src/main/java` will directly affect `:tv`.
- Specifically, `TvActivity` and TV screens rely on:
  - `com.music.bitchord.playback.PlaybackService`
  - `com.music.bitchord.ui.MainViewModel`
  - `com.music.bitchord.data.YtMusicRepository`
  - `com.music.bitchord.data.settings.AppSettings`
  - `com.music.bitchord.data.sources.SourceRegistry`
  If public methods or constructors in these classes are altered during phone app updates, the TV screens calling them must be updated simultaneously.

### 2. Synchronization During Rebranding
- When renaming the application ID (`com.music.bitchord` ➔ `com.music.<newname>`) and namespace, `tv/build.gradle.kts` and `tv/src/main/AndroidManifest.xml` must be renamed to match (`com.music.<newname>.tv`).
- All package statements in `tv/src/main/java/...` must be updated to the new namespace.
- Deep link intent filters in `tv/src/main/AndroidManifest.xml:246` must be updated to the new domain.

### 3. Impact of Wear OS Pruning (Phase 3)
- When `:wearos` is pruned from the project, `WatchInstaller.kt` and `WearBridge.kt` will be removed from `:app`.
- Once removed from `:app`, the stubs in `tv/src/main/java/com/music/bitchord/wear/` will no longer be "replacements" for anything in `:app`.
- **Action:** Both stub files in `tv` should be cleanly deleted alongside the Wear OS pruning phase, and any lingering references in `:tv` removed.

### 4. Dependency Stripping: NewPipeExtractor
- `:tv` re-declares the `stripNewPipeExtractorUtils` task (`tv/build.gradle.kts:278-289`) to strip the duplicate `Utils.class` from the NewPipeExtractor jar.
- This task must remain intact to prevent D8/R8 release build collisions.

### 5. Native CMake Build
- `:tv` compiles `app/src/main/cpp/CMakeLists.txt` for 3 ABIs (`armeabi-v7a`, `arm64-v8a`, `x86_64`).
- Any changes to C++ analyzer files in `native/analyzer/` or CMake scripts must maintain support for all three ABIs, as many Android TV devices and emulators use `x86_64` or 32-bit `armeabi-v7a` chips.
