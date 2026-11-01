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
Phase 3: Dormant targets, build scripts, CI re-wiring
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

## Early CI (pre-Phase 3) [COMPLETED]

- **Status:** **DONE**  
- **Date:** 2026-10-30  
- **Branch:** `violence-main`  
- **Workflows Added / Reconfigured:**
  - `.github/workflows/web.yml`: Automated GitHub Pages deploy (`pnpm build:site` publishing `web/app/dist` to `github-pages` environment with SPA deep-link routing and subpath support). Phase 6 will further refine the web deploy and custom domain integration.
  - `.github/workflows/android.yml`: Automated debug build compiling Phone (`:app:assembleDevDebug`) and TV (`:tv:assembleDevDebug`) APKs without touching frozen modules (`-Pbitchord.bundleWear=false`) and uploading artifacts with 30-day retention.

---

## Phase 3: Dormant Targets, Build Scripts, CI Re-wiring & Restoration Check [COMPLETED]

- **Status:** **DONE**
- **Completion Date:** 2026-11-01
- **Branch:** `violence-nk`
- **Commits:**
  - `50827b9`: Restore desktop wiring dropped by the v1.9 merge
  - `57849a7`: CI: keep dormant targets out of automatic runs
  - `8d6b20e`: Add active-target build/test scripts and BUILD_COMMANDS.md
  - `8739c24`: Document dormant targets, restoration check, and updated agent rules
  - (this PHASES.md update is the final Phase 3 commit)
- **Differences from the original Phase 3 plan ("Module Pruning"):** the plan to delete `:wearos`, `:desktopApp`, `:desktopUi` is **CANCELLED**. They are DORMANT: kept intact and revivable, not built or tested in routine work. `MODULE_PRUNING_PLAN.md` is marked cancelled. TV is an ACTIVE target (the old "never build TV" directive is cancelled).
- **What was done:**
  - `fork-docs/BUILD_COMMANDS.md`, `scripts/build-active.ps1`, `scripts/test-active.ps1`, `scripts/_env.ps1`: explicit task paths for phone, TV, web only; root-level Gradle tasks forbidden.
  - `fork-docs/FROZEN_TARGETS.md`: dormant modules, shared-code dependencies, history, leftover BitChord identifiers, the desktop `upgradeUuid` rule, unfreeze checklist, "Possible impact from later changes" log.
  - `fork-docs/AGENT_RULES.md` and `/CLAUDE.md`: DORMANT TARGETS section, branch rule, KEEP rule, secret/session rules.
  - `fork-docs/RESTORATION_LOG.md`: Phase 1/2 review found nothing wrongly disabled. **Regression from the v1.9 merge restored:** `settings.gradle.kts` desktop includes, desktop CI jobs (now `desktop-check.yml`, manual), `.gitignore` `/desktopApp/bin/`.
  - CI: `android.yml`, `tv.yml`, `web.yml`, `check.yml` now also trigger on `violence-nk`; `check.yml` Android job uses explicit `:app:` tasks; desktop jobs moved to manual-only `desktop-check.yml`; `release.yml` (desktop release) tag trigger removed, manual only.
  - `ALLOWED_EXTERNAL_SERVICES.md`: new row for the web presence ping (DISABLE).
- **Environment note:** because `:desktopApp` is a Gradle project, every build needs a JDK 21 toolchain; portable Temurin 21 was installed to `%USERPROFILE%\.jdks`.
- **Verification (active targets only, via the new scripts):** `build-active.ps1` passed phone `:app:assembleDevDebug`, TV `:tv:assembleDevDebug`, web typecheck and `build:site`. `test-active.ps1` passed `:app:testDevDebugUnitTest` and `:shared:jvmTest` (Gradle reported them up to date from the passing run earlier that day: 1209 + 122 tests, 0 failures) and 159 web tests. Wear OS and desktop: dormant, not checked.
- **No dormant module file was edited.**
## Phase 4: Self-hosted Jam Backend & Client Repoint [COMPLETED, deployment pending the owner]

- **Status:** **DONE in the repository** (2026-11-01). **Not deployed:** creating the Render account and service is manual (`JAM_BACKEND_DEPLOY.md`); until the owner sets `JAM_BACKEND_URL` the apps say Jam is not set up.
- **Branch:** `violence-nk`
- **Commits:** `11527e0` (backend), `7a6c27c` (clients, CI wiring, strings), `3496978` (`scripts/jam-test.mjs`), then the docs commit and this PHASES.md update.
- **Differences from the old Phase 4 text:** the plan said "Render Free (or Firebase RTDB)", a keepalive workflow and hardcoding the live URL into `LISTEN_TOGETHER_SERVER` and `web/app/src/party/listenTogether.ts`. Done instead: the host was chosen from official docs (Render Free primary, Northflank Sandbox fallback, Firebase documented only), the URL is **not** in the repo (empty default, `JAM_BACKEND_URL`), and no GitHub keepalive cron was added (a pinger is optional and documented, with its 750-hour caveat). Firebase is not built.
- **Go/no-go on free hosting:** GO. `JAM_BACKEND_FEASIBILITY.md` (endpoints/frames used vs implemented: no gaps; in-memory rooms; no login data in Jam messages; member identity fields; the existing custom-server setting is reused).
- **What changed:**
  - Backend: stats/presence routes off unless `JAM_ENABLE_STATS`, default allowed origins (owner's site + localhost), display name no longer logged, invite page says Violence with no author link, Go module path `github.com/yashaswahuh/Violence/backend`, `render.yaml` values, new `Dockerfile`/`.dockerignore`, author's `backend/deploy/` removed, README updated.
  - Android/TV: `JAM_BACKEND_URL` (empty default; old name still read), no crash on a malformed value, "Jam is not set up" message, "Waking up the Jam server..." with an 85 s retry for the built-in server (create, join, lookup). Existing reconnect loop (1 s doubling to 30 s, clock and queue resync) was already in place and is unchanged.
  - Web: `VITE_PARTY_SERVER_URL` from repo variable `JAM_BACKEND_URL`, build warns instead of failing, same wake-up and not-set-up behavior.
  - CI: `android.yml`, `check.yml`, `tv.yml`, `web.yml` pass the `JAM_BACKEND_URL` repo variable.
  - Docs: `JAM_BACKEND_FEASIBILITY.md`, `JAM_BACKEND_DEPLOY.md`, `JAM_TEST_PLAN.md`; `OPEN_QUESTIONS.md` and `FREE_HOSTING_PLAN.md` record the reversal; `ALLOWED_EXTERNAL_SERVICES.md`, `UPSTREAM_SYNC.md`, `FROZEN_TARGETS.md`, `PROJECT_STATE.md`, `CLAUDE.md` updated.
- **Verification (via the scripts, active targets only):** phone `:app:assembleDevDebug`, TV `:tv:assembleDevDebug`, web typecheck and `build:site` pass; `:app:testDevDebugUnitTest` 1209 tests, `:shared:jvmTest` 122, web 159: 0 failures (includes the locale test for the 2 new strings in 20 files). `scripts/jam-test.mjs` passes against a throwaway mock (18/18 with 2 clients, 57/57 with 15), which only validates the script. **Not run:** the Go tests, the Docker build, and the Jam test against the real backend (no Go/Docker installed; nothing was installed). Wear OS and desktop: dormant, not checked.
- **Owner follow-ups:** deploy per `JAM_BACKEND_DEPLOY.md`, then run `jam-test.mjs` (2 and 15 clients) and fill in `JAM_TEST_PLAN.md`; set `JAM_BACKEND_URL` in `local.properties` and as the GitHub repo variable.

## Phase 5: Jam Reliability, Invites and TV/Web Parity [COMPLETED in code; real-device and deployed-backend testing pending the owner]

- **Status:** **DONE in the repository** (2026-11-02). Never run on a phone, TV or browser, nor against the deployed backend, nor the new Go tests (no Go on the PC). The owner's checks: `JAM_TEST_PLAN.md` sections 8-11.
- **Branch:** `violence-nk`. **Commits:** `ab920f7` (audit), then the backend, Android/TV, web and docs commits listed at the end of the final report of the session (see `git log` after `ab920f7`).
- **Differences from the old Phase 5 text:** the old plan was stalled-track isolation plus a `JamInviteLink` domain update. This phase follows the larger brief: audit first (`JAM_AUDIT.md`), unplayable-track isolation, configurable invite base and share/copy code, paste-a-link join, a web landing page, the TV Jam dialog and manifest fix, 15-member rooms, host handover, web resync, tests and a manual checklist.
- **Fixed (per `JAM_AUDIT.md`):**
  - **Phone:** a track that cannot play on one device no longer loops or pulls the party to another song (`PartySync.onTrackUnplayable`, hook in `PlaybackService`; notice toast plus a line on the Jam screen); join field accepts a pasted link or share message; invite sheet offers Share link (only if `JAM_INVITE_BASE_URL` is set), Share code and Copy code; QR holds `violence://party/<CODE>` without a base; party size up to 15 (default 8); `violence://party/<CODE>` and the web paths handled by the manifest filters.
  - **TV:** manifest now `violence`/owner host (was the author's host and the `bitchord` scheme); cold-start invites are consumed (`TvActivity.onCreate`, previously only `onNewIntent`); new Listen Together dialog in Settings with a D-pad key grid, start/join/leave, two-column member list, "waking up" and stalled-track notices, opened automatically by an incoming invite. **Not run on a TV.**
  - **Web:** `/party/:code` landing page (Open in Violence app, Join in this browser, Copy code, no auto-redirect; `noindex` is site-wide); join field accepts links; "Copy code" always, "Copy invite link" only with `VITE_INVITE_BASE_URL`; stalled track no longer retries every tick; hidden-tab and `online` resync; blocked-autoplay banner anywhere in the app.
  - **Backend:** host handover picks the oldest connected member (was random map order, maybe an offline member); party size ceiling 15 (`config.MaxPartyMembers`; validate, create, set size); `render.yaml` 15; tests added. **Not compiled or run.**
  - **Shared:** `JamInvite` (dead code) points at the owner's site and `/party/`.
- **Protocol:** unchanged except the `maxMembers` ceiling 10 to 15 (client and backend changed together). Backend invite page `/invite/<CODE>` still served.
- **New config:** `JAM_INVITE_BASE_URL` (Android/TV `local.properties`/env/CI repo variable; web `VITE_INVITE_BASE_URL`), empty = share only the code. Web build and deploy workflows were **not** changed (the Pages workflow does not pass `VITE_INVITE_BASE_URL` yet: a later phase).
- **Verification (active targets, via the scripts):** phone `:app:assembleDevDebug`, TV `:tv:assembleDevDebug`, web typecheck and `build:site` pass. Tests: phone 1222 (+9 `JamInviteCodeExtractionTest`, rewritten `JamInviteLinkTest`), shared 122, web 167 (+8 invite/extractCode tests): 0 failures. `jam-test.mjs` against the throwaway mock (2, 15 clients, one room of 15): passes (script logic only). Wear OS and desktop: dormant, not checked.
- **Known limits:** drift/reconcile engine not refactored or unit-tested; TV ignores `?server=` custom servers in invites; TV UI untested; the first Render build is still the first Go compile.

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

---

## Upstream syncs

| Date | Upstream version | Branch | Result | Details |
|---|---|---|---|---|
| 2026-10-31 | BitChord v1.9 (`04f64c9`) | `violence-nk` | DONE: phone + TV + web build, tests pass | `fork-docs/UPSTREAM_SYNC_LOG.md`, playbook in `fork-docs/UPSTREAM_SYNC.md`. Kept desktop (upstream deleted it). Version now `1.9.0-violence.1`. |
