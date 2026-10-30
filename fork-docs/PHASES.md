# Project Roadmap & Implementation Phases

**Audit Date:** 2026-10-29  
**Execution Model:** Phased implementation. Each phase is scoped to be executed independently in a fresh AI chat session.  
**Strict Rule:** Every phase must end with working, verifiable code. No phase may leave the repository in a broken build state.

---

## Phase Summary Overview

```
Phase 0 (Current) ──► Phase 1: Repo Hygiene & Author Decoupling
                   │
                   ▼
Phase 2: Complete Rebrand & Visual Identity
                   │
                   ▼
Phase 3: Module Pruning (Wear OS & Desktop)
                   │
                   ▼
Phase 4: Free Backend Deployment & Jam Binding
                   │
                   ▼
Phase 5: Jam Resilience & Protocol Hardening
                   │
                   ▼
Phase 6: Static Web & Extension Deployment (Cloudflare Pages)
                   │
                   ▼
Phase 7: Android TV Target Verification & Remote Polish
                   │
                   ▼
Phase 8: APK Update Distribution (GitHub Releases)
                   │
                   ▼
Phase 9: Production Signing, Release Automation & End-to-End Audit
```

---

## Phase 1: Repository Hygiene, Upstream Remote & Author Decoupling [COMPLETED]

- **Status:** **DONE**  
- **Completion Date:** 2026-10-30  
- **Branch:** `violence-main`  
- **Commits:**
  - `9381955`: chore: harden .gitignore and establish fork-docs documentation
  - `fce6c69`: feat: disable open-app presence telemetry pings
  - `010b9d2`: feat: cleanly disable author in-app update checker
  - `742832e`: chore: remove author promotional and donation links from README and settings
  - `9328228`: chore: disable default promotional button in Discord rich presence
  - `a0b928f`: chore: suppress author release download links on web target
- **Primary Goal:** Establish upstream tracking, gitignore hardening, and clean decoupling of author telemetry, analytics, update checks, and donation prompts, while keeping all music playback, search, and party/jam services operational.
- **Differences from Initial Phase 1 Specification:**
  - Retained party/jam networking (`bitchord-listen-together.onrender.com` / `api.bitchord.kushagrasingh.in`) untouched as **REPLACE-LATER** (to be replaced in Phase 5 by Firebase) as requested in Phase 1 directives, rather than clearing server defaults now.
  - Branching targeted dedicated development branch `violence-main`.
  - Upstream push disabled with `git remote set-url --push upstream DISABLED`.
  - Added complete classification catalog in `/fork-docs/ALLOWED_EXTERNAL_SERVICES.md` and decoupling actions in `/fork-docs/DECOUPLING_LOG.md`.
- **Files Touched:**
  - `.gitignore`: Hardened with `google-services.json`, `*/google-services.json`, `.env`, `.env.*`, and `*service-account*.json`.
  - `shared/.../Presence.kt`: Set `ENDPOINT = ""` and inserted early-return no-op guards.
  - `app/.../AndroidPresence.kt` & `desktopApp/.../DesktopPresence.kt`: Made `install()` a clean no-op.
  - `app/.../AppUpdateChecker.kt` & `desktopApp/.../DesktopUpdateChecker.kt`: Set `LATEST_RELEASE_URL = ""` with early returns.
  - `app/.../SettingsSheet.kt` & `desktopApp/.../DesktopApp.kt`: Stripped promotional developer/Discord/website links.
  - `README.md`: Stripped live stats badge, Trendshift/FMHY badges, and `# Support` donation section.
  - Deleted files: `upi_support.jpg`, `.github/FUNDING.yml`, `.github/workflows/deploy-backend.yml`, `.github/workflows/close-issue.yml`.
  - Discord RPC: Set default `button2Visible` to `false` across Android, Desktop, and Web.
  - Web config: Suppressed author release APK download button when unconfigured.
  - `gradle.properties`: Added compileSdk 37.0 suppression to silence AGP warning.
- **Definition of Done Achieved:**
  - `git remote -v` shows `origin` (`https://github.com/yashaswahuh/Violence.git`) and read-only `upstream` (`DISABLED` push).
  - Zero outgoing network calls to author telemetry or update endpoints.
  - Clean compilation and build verification across Android Phone (`:app:assembleDevDebug`), Android TV (`:tv:assembleDevDebug`), and Web (`build:site`).
  - Unit tests verified (`:shared:jvmTest` passing, 1072 of 1074 unit tests passing with only pre-existing upstream Chinese strings failing).

---

## Phase 2: Visual Identity, Strings & Rebranding [COMPLETED]

- **Status:** **DONE**  
- **Completion Date:** 2026-10-30  
- **Branch:** `violence-main`  
- **Commits:**
  - `f7a0813`: `feat(rebrand): update application IDs, schemes, authorities, manifests, and build config`
  - `a21d8c2`: `feat(rebrand): replace BitChord branding and visual assets with Violence placeholder artwork`
  - `35f55ee`: `feat(rebrand): update user-visible strings, labels, and service identifiers across Android and Web`
  - Upcoming: `docs(rebrand): update README with GPL notice, update documentation and REBRAND_LOG`
- **Primary Goal:** Rebrand every user-visible and identifier-level occurrence of BitChord to "Violence", without breaking the build and without removing Wear OS or desktop modules yet.
- **Differences from Initial Phase 2 Specification:**
  - **Internal Package Renaming:** Kept internal Kotlin packages (`com.music.bitchord`) and only changed `applicationId` (`com.yashaswahuh.violencemusic`), dev flavors, and resource IDs. This prevented `UnsatisfiedLinkError` issues with native JNI C++ methods (`DsdBlockDecoder.cpp`, `TrackFeatures.cpp`) and avoided touching 800+ files before Phase 3 module pruning.
  - **Build/Test Targets:** Per user standing directive, testing and builds were strictly limited to Android App (`:app:assembleDevDebug`) and Web (`pnpm --dir web build:site`). No Wear OS or Android TV builds/tests were run.
  - **Locale String Completeness:** Added 7 missing string keys in Chinese locales (`values-zh`, `values-zh-rTW`) that were missing upstream, allowing `LocaleStringsTest` to achieve 100% pass rate.
  - **GPL Compliance:** Retained `LICENSE` and original copyright headers; added NOTICE in `README.md` acknowledging upstream BitChord by kushagrasinghx under GPL-3.0.
- **Files Touched:**
  - `app/build.gradle.kts` & `tv/build.gradle.kts`: `applicationId` to `com.yashaswahuh.violencemusic`, `app_name` to `Violence` / `Violence TV`, `UPDATE_APK_PREFIX` to `Violence-`.
  - Manifests: FileProvider authority `${applicationId}.provider`, deep link scheme `violence://`, host `yashaswahuh.is-a.dev`.
  - `JamInviteLink.kt`, `invite.ts`, `backend/main.go`: Scheme `violence://`, origin `https://yashaswahuh.is-a.dev/violence`.
  - Assets: `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_notification_logo.xml`, `tv_banner.png`, `favicon.svg`, `Logo.svg`, `LogoTransparent.svg`, `Logo.png`, `Banner.png`, extension icons.
  - Strings: 19 locale `strings.xml` files updated, Discord RPC, ListenBrainz, Last.fm scrobbling, web UI pages.
  - Documentation: Created `fork-docs/REBRAND_LOG.md`, updated `README.md`, `MAINTAINERS.md`, `AGENT_RULES.md`, `PHASES.md`.
- **Definition of Done Achieved:**
  - Phone app label "Violence", TV app label "Violence TV".
  - Application ID `com.yashaswahuh.violencemusic` installs independently of BitChord.
  - Clean build for `:app:assembleDevDebug` and `pnpm --dir web build:site`.
  - All unit tests passing: 1,074 Android unit tests (`:app:testDevDebugUnitTest`) and 119 web unit tests (`pnpm --dir web test`).

---

## Phase 3: Module Pruning (Wear OS & Desktop)

- **Primary Goal:** Safely delete `:wearos`, `:desktopApp`, and `:desktopUi` along with all unused dependencies, shrinking project complexity and build times while preserving `/native/analyzer`.
- **Files Touched:**
  - `app/build.gradle.kts`: Delete Wear OS dependencies (`libadb-android`, `play-services-wearable`), delete `BundleWearApk` task, remove `../wearos/protocol` sourceSet.
  - `app/src/main/java/com/music/bitchord/wear/`: Delete `WatchInstaller.kt` and `WearBridge.kt`.
  - `tv/src/main/java/com/music/bitchord/wear/`: Delete stub files.
  - `sharedUi/build.gradle.kts`: Remove `ui-desktop` and `CMP_DESKTOP`.
  - `settings.gradle.kts`: Remove `:wearos`, `:desktopApp`, `:desktopUi`.
  - CI workflows: Remove desktop jobs in `.github/workflows/check.yml` and `.github/workflows/release.yml`.
  - File deletions: Delete directories `/wearos`, `/desktopApp`, `/desktopUi`, `/DESKTOP.md`, `/docs/DESKTOP_RELEASES.md`.
- **Definition of Done:**
  - Repository contains only `:app`, `:tv`, `:shared`, `:sharedUi`, `/native`, `/backend`, and `/web`.
  - Phone and TV APK sizes reduced by ~5 MB.
- **Verification:**
  - Run `./gradlew assembleDevDebug` (Phone).
  - Run `./gradlew :tv:assembleDevDebug` (TV).
  - Run `corepack pnpm --dir web -r typecheck` (Web).
  - Confirm all three targets compile cleanly.

---

## Phase 4: Free Backend Deployment & Jam Server Binding

- **Primary Goal:** Deploy the Go backend to Render Free tier (or set up Firebase RTDB), configure automated free keepalive pings to prevent spin-down, and wire client defaults to the new backend.
- **Files Touched:**
  - `backend/render.yaml` or Render dashboard deployment configuration.
  - Optional: Set up free uptime monitor (cron-job.org or GitHub Actions cron) pinging `/healthz` every 12 minutes.
  - `app/build.gradle.kts` & `tv/build.gradle.kts`: Set `LISTEN_TOGETHER_SERVER` default to the new live Render URL.
  - `web/app/src/party/listenTogether.ts`: Update default server URL.
- **Definition of Done:**
  - Backend is live over HTTPS with valid SSL certificate.
  - `curl https://<backend>/healthz` returns `{"ok": true, ...}`.
  - Android client and web client successfully create and join room codes.
- **Verification:**
  - Run `go test -v ./...` inside `backend/`.
  - Open app, create a party room, verify 6-character code is minted by new backend.

---

## Phase 5: Jam Feature Resilience & Stalled-Track Isolation

- **Primary Goal:** Enhance the Jam client synchronization layer to handle edge cases where a track is unavailable or region-restricted on one device without disrupting other listeners.
- **Files Touched:**
  - `app/src/main/java/com/music/bitchord/playback/PartySync.kt`: Add error trapping for ExoPlayer failures during Jam playback to prevent outgoing pause frames.
  - `app/src/main/java/com/music/bitchord/data/listentogether/ListenTogether.kt`: Implement `playbackStalled` state and auto-resume on track advance.
  - `app/src/main/java/com/music/bitchord/data/listentogether/JamInviteLink.kt`: Update deep link parsing for new domain.
  - `shared/src/commonTest/kotlin/.../JamInviteTest.kt`: Update unit tests.
- **Definition of Done:**
  - When one device experiences a playback error on track $N$, it mutes locally and waits; when track $N+1$ begins, it automatically re-syncs and plays.
- **Verification:**
  - Run unit tests: `./gradlew testDevDebugUnitTest --tests "*Jam*"`.

---

## Phase 6: Web Target Static Deployment & Browser Extension

- **Primary Goal:** Build and deploy the web player statically to Cloudflare Pages (or GitHub Pages) and package the companion browser extension.
- **Files Touched:**
  - `web/extension/wxt.config.ts`: Set `BITCHORD_SITE_ORIGINS` to the Cloudflare Pages URL.
  - `web/app/.env.production`: Set release APK and backend server links.
  - Deploy script / Cloudflare Pages integration: `pnpm build:site`.
- **Definition of Done:**
  - Web player loads cleanly over HTTPS on Cloudflare Pages.
  - Browser extension zips are downloadable directly from `<site>/downloads/`.
  - Loading unpacked extension enables YouTube Music audio streaming in the browser.
- **Verification:**
  - Run `corepack pnpm --dir web build:site`.
  - Verify static output in `web/app/dist` (including `downloads/extension.json`).

---

## Phase 7: Android TV Target Optimization & Polish

- **Primary Goal:** Verify all TV-specific features on an Android TV device or emulator.
- **Files Touched:**
  - `tv/src/main/java/com/music/bitchord/ui/tv/auth/TvAuthServer.kt`: Ensure QR code auth works with rebranded app.
  - `tv/src/main/java/com/music/bitchord/ui/tv/dialogs/TvDialogs.kt`: Clean attribution strings in `TvAboutDialog`.
  - `tv/src/main/java/com/music/bitchord/ui/tv/screens/TvNowPlayingScreen.kt`: Validate D-pad focus in Jam mode.
- **Definition of Done:**
  - Android TV APK installs and renders properly on a 1080p/4K Leanback display.
  - Remote control d-pad navigation and QR-code login function without crash.
- **Verification:**
  - Run `./gradlew :tv:assembleDevDebug`.
  - Install and launch on Android TV emulator or physical TV.

---

## Phase 8: In-App Self-Updating via GitHub Releases

- **Primary Goal:** Reconfigure the sideloaded APK update mechanism to check the user's private/personal GitHub releases.
- **Files Touched:**
  - `app/src/main/java/com/music/bitchord/data/AppUpdateChecker.kt`:
    Update `LATEST_RELEASE_URL` to `https://api.github.com/repos/<owner>/<repo>/releases/latest`.
  - `tv/src/main/java/com/music/bitchord/data/TvStartup.kt`: Verify TV update checker uses same endpoint with TV prefix.
- **Definition of Done:**
  - When a new GitHub release tag is published with signed APKs, the app detects it on startup, prompts the user, downloads the APK, and prompts installation.
- **Verification:**
  - Mock or publish a draft release tag and verify update dialog appears in app.

---

## Phase 9: Release Keystore Setup & Automated GitHub Actions

- **Primary Goal:** Generate a private release keystore, configure CI workflow to sign release APKs on tag push, and perform full end-to-end audit.
- **Files Touched:**
  - Generate `fork-release.jks` (local only, gitignored).
  - Update `keystore.properties.example` with instructions.
  - `.github/workflows/android.yml`: Set up automated signing using GitHub Secrets (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD`).
  - `.github/workflows/tv.yml`: Add signed release build job for TV.
- **Definition of Done:**
  - Tagging `v1.0.0` in git automatically triggers GitHub Actions, compiles signed Phone and TV APKs, and publishes them as assets on GitHub Releases.
  - Final signed APK installed and verified on personal devices.
