> **CANCELLED (2026-11-01, Phase 3).** Wear OS and desktop are no longer to be removed. They are DORMANT: kept intact and revivable, not built or tested in routine work. See FROZEN_TARGETS.md. Do NOT execute any removal step below; this file is kept only as a record of the old analysis. (Facts such as `wearos/protocol` being compiled by `:app` and `/native/analyzer` being shared are still true.)

# Module Pruning Plan: Wear OS & Desktop Targets

**Audit Date:** 2026-10-29  
**Goal:** Safely remove `:wearos`, `:desktopApp`, and `:desktopUi` without breaking the three required targets: **Android (`:app`)**, **Android TV (`:tv`)**, and **Web (`/web`)**.  
**Execution Phase:** Scheduled for Phase 3 (DO NOT execute in Phase 0).

---

## 1. Safety Principles & Invariants

Before removing any module, the following invariants must be preserved:

1. **Native Analyzer Preservation:** Root directory `/native/analyzer` must **NOT** be deleted. Although `desktopApp/native/CMakeLists.txt` compiles it for desktop, `app/src/main/cpp/CMakeLists.txt` also compiles it for Android and TV. Deleting `/native/analyzer` would completely break Android Automix and DSP analysis.
2. **TV Stubs Awareness:** The TV module (`:tv`) currently maintains stubs for `WatchInstaller.kt` and `WearBridge.kt` (`tv/src/main/java/com/music/violence/wear/`). When Wear OS is removed from `:app`, those stubs in `:tv` must be handled concurrently to avoid orphaned files or compilation errors.
3. **Step-by-Step Verification:** Wear OS and Desktop must be pruned in separate, isolated steps. After each removal, `./gradlew assembleDevDebug` and `./gradlew :tv:assembleDevDebug` must pass with zero errors.

---

## 2. Target 1: Pruning Wear OS (`:wearos`)

### 2.1 Dependencies and Linkages to be Cleared

Wear OS is entangled with `:app` in five distinct places:

1. **Protocol Source Sharing:**
   `app/build.gradle.kts:100-102`:
   ```kotlin
   sourceSets {
       getByName("main") { java.srcDir("../wearos/protocol") }
   }
   ```
2. **Phone-to-Watch Gradle Tasks:**
   `app/build.gradle.kts:480-522`:
   Task `BundleWearApk` compiles `:wearos:assemble*` and injects `ViolenceWear.apk` and `ViolenceWear.sha256` into phone assets.
3. **External Dependencies in `:app`:**
   `app/build.gradle.kts:403-421`:
   - `com.google.android.gms:play-services-wearable:20.0.1` (Google Wearable Data Layer)
   - `androidx.wear:wear-remote-interactions:1.1.0` (Remote interactions)
   - `com.github.MuntashirAkon:libadb-android:3.1.1` (In-app ADB client for wireless debugging sideload)
   - `org.conscrypt:conscrypt-android:2.5.3` (ADB TLS crypto)
   - `org.bouncycastle:bcpkix-jdk18on:1.85` (ADB certificate generator)
4. **Phone Application Code:**
   - `app/src/main/java/com/music/violence/wear/WatchInstaller.kt` (Full ADB client and pairing state machine)
   - `app/src/main/java/com/music/violence/wear/WearBridge.kt` (Data layer sync)
   - `app/src/main/java/com/music/violence/ui/screens/SettingsSheet.kt` (UI settings rows for "BitChord for Wear OS")
   - `app/src/main/java/com/music/violence/playback/PlaybackService.kt` (Hooks calling `WearBridge`)
5. **TV Stubs:**
   - `tv/src/main/java/com/music/violence/wear/WatchInstaller.kt`
   - `tv/src/main/java/com/music/violence/wear/WearBridge.kt`

### 2.2 Execution Steps for Wear OS Pruning

Execute in this exact sequence:

- [ ] **Step W1: Clean Calling Code in `:app`**
  - In `app/src/main/java/com/music/violence/playback/PlaybackService.kt`, remove calls pushing state to `WearBridge`.
  - In `app/src/main/java/com/music/violence/ui/screens/SettingsSheet.kt`, remove the "BitChord for Wear OS" settings group and ADB wireless pairing dialogs.
  - Delete `app/src/main/java/com/music/violence/wear/WatchInstaller.kt` and `WearBridge.kt`.
- [ ] **Step W2: Clean Calling Code in `:tv`**
  - Delete `tv/src/main/java/com/music/violence/wear/WatchInstaller.kt` and `WearBridge.kt`.
  - In `tv/build.gradle.kts:483-497`, verify that `replacedByTv` exclusion logic continues to work cleanly (it will simply find zero overlapping files).
- [ ] **Step W3: Clean `app/build.gradle.kts`**
  - Delete `sourceSets { getByName("main") { java.srcDir("../wearos/protocol") } }`.
  - Delete dependencies: `play-services-wearable`, `wear-remote-interactions`, `libadb-android`, `conscrypt-android`, and `bcpkix-jdk18on`. (This saves ~4.5 MB in final APK size).
  - Delete `abstract class BundleWearApk` and the `androidComponents.onVariants` bundling hook (`lines 480-522`).
- [ ] **Step W4: Update Root Project Configuration**
  - In `settings.gradle.kts`, delete line 41: `include(":wearos")`.
  - In root `build.gradle.kts`, clean comments referencing watch version synchronization.
  - In `.github/workflows/android.yml`, delete step "Upload Wear OS APK Artifact" (`lines 48-55`).
- [ ] **Step W5: Delete Wear OS Module Directory**
  - Delete directory `/wearos` and all contents.
- [ ] **Step W6: Verification**
  - Run `./gradlew assembleDevDebug` (Phone build).
  - Run `./gradlew :tv:assembleDevDebug` (TV build).
  - Verify that neither build references `:wearos`.

---

## 3. Target 2: Pruning Desktop (`:desktopApp` & `:desktopUi`)

### 3.1 Dependencies and Linkages to be Cleared

Desktop targets are contained within `:desktopApp` and `:desktopUi`, but leave footprint in shared libraries and CI:

1. **Gradle Project Inclusions:**
   `settings.gradle.kts:21-22`:
   ```kotlin
   include(":desktopUi")
   include(":desktopApp")
   ```
   Lines 24–44 currently contain fallback logic: if no Android SDK is detected, it logs `"No Android SDK found — building the desktop targets only."`
2. **Shared UI Constraints (`:sharedUi`):**
   `sharedUi/build.gradle.kts:25-30, 85-90`:
   Defines `val CMP_DESKTOP = ...` and imports `org.jetbrains.compose.ui:ui-desktop:$CMP_DESKTOP` in `jvmMain`.
3. **Shared Core Dependencies (`:shared`):**
   `shared/build.gradle.kts:31`:
   `compileOnly("com.microsoft.onnxruntime:onnxruntime:1.20.0")` was included for desktop JVM compatibility.
4. **GitHub Workflows:**
   - `.github/workflows/check.yml`: Runs `desktop-linux`, `desktop-windows`, and `desktop-macos` jobs.
   - `.github/workflows/release.yml`: Entire release pipeline is currently geared towards desktop packaging (WiX Toolset 3, AppImage, Debian, RPM, DMG, PKG).
5. **Documentation Files:**
   - `/DESKTOP.md`
   - `/docs/DESKTOP_RELEASES.md`

### 3.2 Execution Steps for Desktop Pruning

Execute in this exact sequence:

- [ ] **Step D1: Clean Shared UI (`:sharedUi`)**
  - In `sharedUi/build.gradle.kts`:
    - Remove `CMP_DESKTOP` variable definition.
    - Remove `jvmMain.dependencies { api("org.jetbrains.compose.ui:ui-desktop:$CMP_DESKTOP") }`.
    - Note: Keep `jvmTarget` and `jvmSharedMain` intact, as Android and KMP unit tests rely on the common JVM source set.
- [ ] **Step D2: Clean Shared Library (`:shared`)**
  - In `shared/build.gradle.kts`:
    - Remove desktop-only comments and dependencies.
  - In `shared/src/jvmSharedMain/kotlin/com/music/violence/data/presence/Presence.kt`:
    - `enum class PresencePlatform`: Can remove `Pc("pc")` or leave as legacy no-op.
- [ ] **Step D3: Clean Settings and Build Scripts**
  - In `settings.gradle.kts`:
    - Delete `include(":desktopUi")` and `include(":desktopApp")`.
    - Simplify the Android SDK check: require `androidSdk != null` or fail with a clear message: `"Android SDK required to build this project."`
- [ ] **Step D4: Clean CI Workflows**
  - In `.github/workflows/check.yml`:
    - Remove `desktop-linux`, `desktop-windows`, and `desktop-macos` jobs.
    - Retain `android` and `backend` jobs.
  - In `.github/workflows/release.yml`:
    - Replace the desktop release matrix with an Android Release APK & TV Release APK artifact publishing job.
- [ ] **Step D5: Delete Desktop Directories**
  - Delete directory `/desktopApp` and all contents.
  - Delete directory `/desktopUi` and all contents.
  - Delete `/DESKTOP.md`.
  - Delete `/docs/DESKTOP_RELEASES.md`.
- [ ] **Step D6: Verification**
  - Run `./gradlew assembleDevDebug` (Android).
  - Run `./gradlew :tv:assembleDevDebug` (TV).
  - Run `corepack pnpm --dir web build:site` (Web).
  - Confirm all three target builds succeed cleanly.
