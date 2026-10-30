# Rebrand Log: Violence (Phase 2)

**Date:** 2026-10-30  
**Phase:** 2 (Identity & Rebranding)  
**Status:** Completed  
**Branch:** `violence-main`  

---

## 1. Identity & Identifiers

| Item | Old Value (BitChord) | New Value (Violence) | Implementation Details |
|---|---|---|---|
| **Phone App Label** | `BitChord` | `Violence` | `resValue "string", "app_name", "Violence"` in `app/build.gradle.kts` and all 19 `strings.xml` |
| **TV App Label** | `BitChord TV` | `Violence TV` | `resValue "string", "app_name", "Violence TV"` in `tv/build.gradle.kts` |
| **Phone Application ID** | `com.music.bitchord` | `com.yashaswahuh.violencemusic` | `defaultConfig.applicationId` in `app/build.gradle.kts` |
| **Phone Dev Application ID** | `com.music.bitchord.dev` | `com.yashaswahuh.violencemusic.dev` | `productFlavors.getByName("dev").applicationIdSuffix` |
| **TV Application ID** | `com.music.bitchord.tv` | `com.yashaswahuh.violencemusic.tv` | `defaultConfig.applicationId` in `tv/build.gradle.kts` |
| **TV Dev Application ID** | `com.music.bitchord.tv.dev` | `com.yashaswahuh.violencemusic.tv.dev` | `productFlavors.getByName("dev").applicationIdSuffix` |
| **Deep Link Scheme** | `bitchord://` | `violence://` | `AndroidManifest.xml` intent filters for both `:app` and `:tv`, `backend/main.go`, `JamInviteLink.kt`, `invite.ts` |
| **FileProvider Authority** | `com.music.bitchord.provider` | `${applicationId}.provider` (`com.yashaswahuh.violencemusic.provider`) | `AndroidManifest.xml`, dynamic in `AppUpdateChecker.kt` & `ReplayShareSheet.kt` |
| **Notification Channel** | `bitchord_playback` | `violence_playback` | `PlaybackService.kt` |
| **Download Folder** | `Music/BitChord` | `Music/Violence` | `DownloadStore.kt` & `strings.xml` |
| **Web Path & Origin** | `https://bitchord.in` | `https://yashaswahuh.is-a.dev/violence` | `invite.ts`, `site.config.ts`, `index.html` |
| **Web Extension ID/Name** | `BitChord` | `Violence` | `wxt.config.ts` |
| **Scrobbling Client** | `BitChord` | `Violence` | `ListenBrainzManager.kt`, `LastFM.kt`, `BitChordApplication.kt` |
| **Discord RPC** | `BitChord` | `Violence` | `DiscordRPC.kt`, `discord.ts`, `IntegrationSettings.tsx` |

---

## 2. Package Renaming Rationale (Task 2)

### Decision
Internal Kotlin package names remain `com.music.bitchord.*` for Phase 2. Android device identity and external interfaces have been 100% updated to `com.yashaswahuh.violencemusic`.

### Technical Justification
1. **JNI C++ Symbol Coupling:**  
   The project contains native C++ modules in `app/src/main/cpp/` (`DsdBlockDecoder.cpp`, `TrackFeatures.cpp`, etc.) that implement JNI method signatures hardcoded to the JNI naming convention:
   - `Java_com_music_bitchord_playback_dsd_DsdBlockDecoder_create`
   - `Java_com_music_bitchord_playback_smart_TrackFeatures_nativeAnalyze`
   Renaming Kotlin package hierarchies without simultaneously refactoring all C/C++ source trees, headers, CMake configs, and dynamic linking declarations poses a severe risk of `UnsatisfiedLinkError` at runtime.
2. **AGP Decoupling:**  
   Android Gradle Plugin (AGP) cleanly separates `applicationId` (the device-level package name and OS identity) from `namespace` and internal Kotlin packages. By updating `applicationId`, `resValue app_name`, intent filters, FileProvider authorities, notification channel IDs, download folders, and user-facing strings, the application achieves complete identity separation from BitChord on device without touching internal class paths.
3. **Module Pruning Ahead (Phases 3+):**  
   Desktop and Wear OS modules will be pruned or decoupled in future phases. Deferring any purely aesthetic package restructuring avoids touching 800+ files twice.

---

## 3. Visual Assets & Drop-in Locations for Final Artwork (Task 3)

Original BitChord logos and author graphics were replaced with minimalist geometric placeholder assets featuring a clean "V" wordmark on solid dark backgrounds (`#18181B`).

### Drop-in File Locations for User's Final Artwork

When final artwork is ready, drop the assets into these exact paths:

1. **Android App Launcher Icons:**
   - Foreground vector (adaptive): `app/src/main/res/drawable/ic_launcher_foreground.xml`
   - Background vector/color (adaptive): `app/src/main/res/drawable/ic_launcher_background.xml`
   - Notification silhouette icon: `app/src/main/res/drawable/ic_notification_logo.xml` (monochrome 24x24dp vector)
   - Legacy PNG drawables (if needed): `app/src/main/res/mipmap-*/ic_launcher.png`
2. **Android TV Banners & Icons:**
   - TV Banner (320x180 PNG): `tv/src/main/res/drawable/tv_banner.png`
   - TV Dev Banner: `tv/src/dev/res/drawable/tv_banner.png`
   - TV Launcher Foreground: `tv/src/main/res/drawable/ic_launcher_foreground.xml`
3. **Web & PWA Icons:**
   - Web Favicon: `web/app/public/favicon.svg`
   - Web Logo SVG (standard): `web/app/public/Logo.svg`
   - Web Logo SVG (transparent): `web/app/public/LogoTransparent.svg`
   - Web Mark Component: `web/app/src/components/icons/LogoMark.tsx`
4. **Web Extension Toolbar Icons:**
   - 16x16 PNG: `web/extension/public/icon/16.png`
   - 32x32 PNG: `web/extension/public/icon/32.png`
   - 48x48 PNG: `web/extension/public/icon/48.png`
   - 96x96 PNG: `web/extension/public/icon/96.png`
   - 128x128 PNG: `web/extension/public/icon/128.png`
5. **Repository Root Artwork:**
   - App Logo PNG (200x200): `Logo.png`
   - Banner PNG (1200x400): `Banner.png`

---

## 4. GPL Compliance & Legal Notices (Tasks 4 & 5)

- Retained `LICENSE` (GNU GPL-3.0) and all original copyright and license headers across all source files.
- Added explicit `Notice` section in root `README.md` acknowledging upstream BitChord by kushagrasinghx under GPL-3.0 with repository link.
- Replaced personal author links and maintainer promotions in `MAINTAINERS.md` and `README.md`.
- Removed all crowdfunding/donation links (Ko-fi, PayPal, UPI) and promotional badges.

---

## 5. Build and Test Verification

### Standing Constraint
**DO NOT test or build anything other than Android APP (`:app:assembleDevDebug`) and Web (`pnpm --dir web build:site`). No Wear OS or Android TV builds or tests.**

### Results
1. **Android App Debug Build:**
   - Command: `.\gradlew.bat :app:assembleDevDebug`
   - Result: `BUILD SUCCESSFUL` (APK generated at `app/build/outputs/apk/dev/debug/Violence-dev-debug.apk`)
   - Merged Manifest: Verified `package="com.yashaswahuh.violencemusic.dev"`, `android:authorities="com.yashaswahuh.violencemusic.dev.provider"`, `android:scheme="violence"`, `app_name="Violence"`.
2. **Android App Unit Tests:**
   - Command: `.\gradlew.bat :app:testDevDebugUnitTest`
   - Result: `BUILD SUCCESSFUL` (1,074 tests passed, including `LocaleStringsTest` across 16 language catalogs).
3. **Web Unit Tests:**
   - Command: `pnpm --dir web test`
   - Result: `119 passed` (vitest unit test suite).
4. **Web Site & Extension Package Build:**
   - Command: `pnpm --dir web build:site`
   - Result: Clean build; produced `violence-extension-chrome (0.4.4).zip`, `violence-extension-firefox (0.4.4).zip`, and `app/dist/`.
