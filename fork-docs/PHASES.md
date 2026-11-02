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
Phase 7: TV verification, release signing, private in-app updates, App Links   (merged the old 7, 8 and 9)
                   │
                   ▼
Phase 8: In-app profiles (local profile, friends list, profile in Jam)
                   │
                   ▼
Phase 9: NFC, QR and link sharing of songs, profile cards and Jam invites          (done)
                   │
                   ▼
Phase 10: Final audit and handover                                                 (planned)
                   │
                   ▼
Phase 11: Playback sources and login flows log                                     (planned)
```

> **Numbering note (2026-11-01).** Phase 8 now means **In-app profiles**. The numbers after it moved so that profile sharing could be its own phase: the old "Phase 8" (final audit and handover) is now **Phase 10** and the old "Phase 9" (playback sources and login flows log) is now **Phase 11**. In this repository's earlier PHASES text, "Phase 8" and "Phase 9" were the app updater and release signing; both were already merged into Phase 7 (see Phase 7 below), so nothing of them is lost by the renumbering.

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
- **Branch:** `violence-nk`. **Commits:** `ab920f7` (audit), `6d17044` (backend), `39bc1ea` (phone and TV), `ae76291` (web), `8cdfc60` (docs).
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

## Phase 6: Web on GitHub Pages, Cloud APK Builds, Extension Origin and Last.fm [COMPLETED in the repo; first GitHub run pending the owner]

- **Status:** **DONE in the repository** (2026-11-02). The workflows have never run on GitHub (they cannot run from here); everything that could be checked locally was (below). The first real run is the owner's, see `CI_DEPLOY.md`.
- **Branch:** `violence-nk`. **Commits:** `a3bf609` (web base path and 404 fallback), `b7fef4a` (workflows), `2963051` (local Pages check script), `905a04e` (docs).
- **Refines the earlier "Early CI" work** (`web.yml`, `android.yml` added on 2026-10-30): both files were rewritten in place, not recreated.
- **Differences from the Phase 6 brief and the old PHASES text:** the old text said Cloudflare Pages; the owner chose GitHub Pages. The brief wrote the folder as `/violence/`; GitHub Pages names it after the repository and is case sensitive, so the real, owner-confirmed folder is **`/Violence/`** (the workflow takes it from `actions/configure-pages`). The brief says "if Gradle builds the web target": it does not, the web is built with pnpm (`pnpm build:site`), so no Java is set up in `web.yml`.
- **What changed:**
  - **Web base path:** the ONNX Automix models were fetched from a root-absolute `/models/...` (a 404 on the deployed site); now `${BASE_URL}models/...`. `404.html` no longer rewrites `/party/<CODE>` to `/invite/<CODE>` (it would have bypassed the Phase 5 landing page); it now redirects every path (`/party/..`, `/settings/..`, later `/song/..`, `/profile/..`) into the app and restores the exact path, always to the canonical `/Violence/` (no redirect loop on wrong-case addresses). `noindex` meta and `robots.txt` (disallow all) were already present and are checked by the workflow. The PWA manifest uses relative `start_url`/`scope` (so `/Violence/`); there is no service worker.
  - **`web.yml`:** push to `violence-nk` (path filters) and `workflow_dispatch` only; official actions pinned to major versions (`checkout@v4`, `pnpm/action-setup@v4`, `setup-node@v4`, `configure-pages@v5`, `upload-pages-artifact@v3`, `deploy-pages@v4`); permissions `contents: read, pages: write, id-token: write`; `github-pages` environment; concurrency group with `cancel-in-progress: false`; typecheck + tests, build, a sanity check of the output, URL in the job summary. Optional repo variables `JAM_BACKEND_URL` and `JAM_INVITE_BASE_URL` (empty defaults; this wires the Phase 5 `VITE_INVITE_BASE_URL`); `BITCHORD_STORE_BUILD=1` so the published extension zips never answer pages on localhost. **Removed** the unused `VITE_LASTFM_API_KEY` (no web code reads it, and a key in a public bundle is public). No secrets.
  - **`android.yml`:** push to `violence-nk` (path filters; docs-only and web-only pushes ignored) and `workflow_dispatch`; JDK 21 + 17 (the dormant desktop module needs a Java 21 toolchain even for a phone build), Android SDK via `android-actions/setup-android@v3` plus explicit `sdkmanager` packages, Gradle caching, explicit `:app:assembleDevDebug :tv:assembleDevDebug`, APKs renamed `violence-phone-debug-<sha>.apk` / `violence-tv-debug-<sha>.apk`, 30-day artifacts, `contents: read` only, summary note about debug signing keys. Last.fm and Jam values come from optional secrets/variables through the step environment (no `local.properties` written).
  - **Extension:** the built manifests (checked) allow exactly `https://yashaswahuh.is-a.dev/*` (dev builds also `http://localhost:5173`); name "Violence"; no author branding beyond GPL attribution and code comments.
  - **Docs:** `CI_DEPLOY.md` (one page, rewritten), `WEB_USER_GUIDE.md`, `LASTFM_SETUP.md`. New local tool `scripts/serve-pages-local.mjs` (Pages-like server and `--check`).
- **Last.fm:** Android/TV already read `LASTFM_API_KEY`/`LASTFM_SECRET` from `local.properties` or the environment and show "not configured" when missing; unchanged. The web player has no Last.fm integration.
- **Verification:** web built with `VITE_BASE_URL=/Violence/` and with every optional value missing; the CI sanity checks pass on the real output; `serve-pages-local.mjs --check`: 16/16 (assets, manifest, robots, models, deep links get 404.html, wrong-case folder is not the site); a real headless Edge run through the 404 redirect rendered the landing page for `/Violence/party/AB12CD` (with and without `?server=`), `/invite/..`, `/settings/listen-together` and the home page; both workflows parse as YAML with every action pinned and no secret inside a script; phone and TV debug builds via `scripts/build-active.ps1` pass; web typecheck and 167 web tests pass. Wear OS and desktop: dormant, not checked.
- **UNVERIFIED until the first GitHub run:** `setup-android@v3` + `sdkmanager` on the runner, JDK 21 discovery through `JAVA_HOME_21_X64`, private-repo Pages deployment, the `github-pages` environment branch rule, and whether `yashaswahuh.is-a.dev` is the user site's custom domain.

## Phase 7: TV Verification, Release Signing, Private In-App Updates and App Links [COMPLETED in the repo; real-device and GitHub testing pending the owner]

- **Status:** **DONE in the repository** (2026-11-02). Nothing was run on a phone, TV, or on GitHub.
- **Branch:** `violence-nk`. **Commits:** `bed8fda5` (update check), `a0dc30e1` (App Links), `25405c72` (signing template, `release.ps1`, `web.yml` update files), `661c3cdc` (TV Jam focus), `60610ba4` (per-CPU update APKs), `92715997` (TV plan, App Links, release guide), `8259f288` (project docs).
- **Differences from the old PHASES text:** this phase **merges the old Phase 7 (TV polish), Phase 8 (updates via GitHub Releases) and Phase 9 (keystore + CI signing)**. Changed on purpose: updates do **not** use the GitHub API (a private repo's release assets need a login); the app reads an `update.json` that the web workflow copies from the newest release onto GitHub Pages. Signing stays on the owner's machine (`scripts/release.ps1`); **no keystore or password goes into GitHub Secrets or CI**, so there is no signed-release CI job. The App Links prefix is `/Violence/party/` (capital V), not `/violence/party/`. Old Phase 9's "install on personal devices" remains the owner's.
- **What changed:** see `RELEASE_GUIDE.md` (keystore, releasing, installing), `APP_LINKS.md`, `TV_TEST_PLAN.md`, `PROJECT_STATE.md` section 18. Version is read from the root `build.gradle.kts` by phone and TV; TV release no longer falls back to the debug key; `UPDATE_INFO_URL` (local.properties/env, empty = off) and `UPDATE_APK_KEY` replace `UPDATE_APK_PREFIX`.
- **Verification:** `scripts/release.ps1` built the phone and TV release variants (R8 minified, `-dontobfuscate`) signed with a **throwaway keystore created outside the repo for the test only (deleted afterwards)**; `apksigner` verified all 8 APKs; `apkanalyzer` shows the Jam, Last.fm, login, lyrics, Ktor, NewPipe and Rhino classes present in both release dex files and the merged manifest has the verified invite filter, `REQUEST_INSTALL_PACKAGES` and the `com.yashaswahuh.violencemusic.provider` FileProvider. Sizes: universal 183 MB (phone) / 170 MB (TV); per-CPU 65 to 92 MB. `scripts/build-active.ps1` (phone and TV dev debug) and `test-active.ps1` (phone incl. 9 new update tests, shared, 167 web) pass. The dev flavor was built as debug only, not as minified release. Dormant modules: not touched or built.
- **UNVERIFIED:** class presence is not runtime behavior (Jam, Last.fm, playback, search, lyrics, downloads and both logins in a release build need a device); TV focus (static review only); installer flow; the release-triggered site deploy and the `v*` environment rule; Pages per-file limit; assetlinks verification; whether `yashaswahuh.is-a.dev` is the user site's custom domain.

---

## Phase 8: In-App Profiles [COMPLETED in the repo; real-device testing and a Jam server redeploy pending the owner]

- **Status:** **DONE in the repository** (2026-11-01, the build PC's date; Phases 5 to 7 above were dated 2026-11-02). Nothing was run on a phone, TV or browser.
- **Branch:** `violence-nk`. **Commits:** `f9e87087` (audit), `70fbc291` (design), `077981f1` (shared model, card codec, friends, storage, 55+ tests), `8b83564c` (Go backend), `ac22b686` (phone), `3ae03cac` (TV), `53f70420` (web), `81c1f5ae` (Jam script), plus the docs commit that holds this entry.
- **Differences from the Phase 8 brief:** the repo's PHASES text did not contain a "final audit" or a "playback sources log" phase to renumber (its old Phases 8 and 9 were the updater and signing, already merged into Phase 7); they are renumbered as the brief says (10 and 11) and the numbering note above says so. The brief's `/violence/profile` example path is `/Violence/profile` (capital V, like every other web path).
- **What it is:** a local-first profile (`InAppProfile`: random UUID made once, name 1 to 24 characters, one of 24 preset avatars, a colour; an optional photo that stays on the phone) and a local friends list (`FriendCard` with a local nickname), separate from the YouTube Music, Spotify, Discord and Last.fm logins. Jam uses the profile as the member identity and, with a profile, **no longer needs a YouTube Music sign-in**. A strict, versioned profile-card link (`violence://profile?v=1&id=...&n=...&a=...&c=...` and the https twin under `/Violence/profile`) is defined, parsed and tested, and "paste a profile link" adds a friend after "Add <name> as a friend?". No server, account or analytics was added.
- **Audit decision:** the app's existing "profile" concept is the YouTube profile/channel switcher (with the TV PIN). It was **not** extended: it is tied to the Google account. See `PROFILE_AUDIT.md`.
- **Docs:** `PROFILE_AUDIT.md`, `PROFILE_DESIGN.md` (data model, card format and limits, validation, what leaves the device), `JAM_DESIGN.md` section 9 (protocol change), `JAM_TEST_PLAN.md` sections 12 and 13, `JAM_BACKEND_DEPLOY.md` section 6, `TV_TEST_PLAN.md` section 9, `PROJECT_STATE.md` section 19.
- **Jam protocol change (backward compatible):** `JoinRequest` and the member objects gain optional `profileId`, `avatarId`, `avatarColor` (invalid values are dropped, never an error; the preview never shows `profileId`). **Redeploy the Jam server to make avatars and colours show**; names and joining work against the old server.
- **Verification (active targets only, via the scripts):** phone `:app:assembleDevDebug` and TV `:tv:assembleDevDebug` pass; web typecheck and `build:site` pass; phone unit tests 1231 (0 failures, 1 skipped), shared `jvmTest` 182 (0 failures), web 218 (0 failures); **Go `go vet` clean and `go test ./...` passes** (first time the Go tests ever ran: portable Go 1.27.3); the real backend was built and run locally and `scripts/jam-test.mjs` passed 18/18 (old-style clients), 23/23 (mixed old and profile clients) and 34/34 (one room of 15), and against the pre-Phase-8 server old-style clients pass 18/18 and mixed clients 21/23 (only the two profile-display checks fail, as expected). Wear OS and desktop: dormant, not checked.
- **UNVERIFIED (needs your devices):** every screen on a real phone, TV (D-pad focus of the new dialogs) and browser; the avatar emoji rendering on your TV; Jam without a YouTube Music sign-in (playback while signed out); the `Dockerfile` and the Render build; the Go race detector (no C compiler here); the release (R8) build with the new `@Serializable` classes (covered by the existing keep rule `-keep @kotlinx.serialization.Serializable class com.music.bitchord.**`, but not rebuilt in this phase).
- **Known gaps:** the 56 new strings are English placeholders in the 17 other language files (the locale test only requires the keys); no photo on TV or web; the profile-link landing page and the NFC/QR/share transports are Phase 9 (the App Links manifest line `/Violence/profile` is added there).

---

## Phase 9: NFC, QR and Link Sharing [COMPLETED in the repo; every NFC and camera path still needs two real phones]

- **Status:** **DONE in the repository** (2026-11-02). Nothing was run on a real NFC chip, camera, TV or deployed server.
- **Branch:** `violence-nk`. **Commits:** `5c717e27` (design), `d1377be2` (shared links, parser, gate, 40 tests), `39f1a352` (NFC Type 4 tag emulator/reader, HCE service, manifest, 33 tests), `17b9d569` (phone sharing UI, hub, QR scanner, 19 tests), `34c92f7a` (TV), `7888497f` (web), plus the docs commit that holds this entry.
- **Differences from the Phase 9 brief:** the web paths are `/Violence/...` (capital V, as everywhere); a Spotify track in this app is always matched to a YouTube id, so the song sources are `yt`, `q` (search) and an input-only `sp`; there is no "home menu", so **Receive** is in Settings and Friends; the brief's `required="false"` for `android.hardware.nfc.hce` and the NFC Forum AID/APDU details are UNVERIFIED against the spec text (the official Android HCE and tag-dispatch pages were read and are cited in `SHARE_DESIGN.md`).
- **What it does:** a **song** (`violence://song/<yt|q|sp>/<id>?v=1&t=&a=`, hints cut to fit 200 bytes), a **profile card** (Phase 8) or a **Jam invite** can be shared by **NFC tap** (the phone acts as a Type 4 NDEF tag only while the Tap to share screen is showing and the phone is unlocked; the HCE service is declared disabled and switched on and off around that screen), **QR code**, or **link**. The other phone reads it in the **Receive** screen (NFC reader mode, camera or picture QR scan, paste) or, with the app closed, through the system tag dispatch (UNVERIFIED). **Every** incoming link, from any source, goes through one parser and `IncomingLinkHub`: strictly validated, repeat/flood-gated, and **never acts without a confirmation** (song: Play now / Add to queue / Add to playlist; profile: "Add <name> as a friend?"; Jam: the existing join preview). TV: QR + link dialogs and the same confirmations for opened links. Web: `/song/...` and `/profile` landing pages, share dialog with a QR code, any-link paste on Friends. App Links gained `/Violence/song/` and `/Violence/profile`.
- **Privacy:** only the item's own link leaves the device, nothing is logged, no network call is made by sharing/scanning/tapping/pasting; a received **song** is looked up (the normal YouTube Music lookup) when its card opens, to show cover, artist and length (changed in the part 1 fixes; the first version waited for a choice).
- **New dependencies:** CameraX 1.6.2 (4 artifacts, Apache-2.0) in `:app` and `:tv`; `uqr` 0.1.3 (MIT) on the web; ZXing `core` 3.5.3 (Apache-2.0) was already present and now also decodes.
- **Docs:** `SHARE_DESIGN.md` (formats, the byte-level APDU/NDEF tables, flows, UNVERIFIED list), `SHARE_TEST_PLAN.md` (the two-phone plan; section 1 is the first test to do), `APP_LINKS.md`, `TV_TEST_PLAN.md` section 10, `WEB_USER_GUIDE.md`, `PROJECT_STATE.md` section 20.
- **Verification (active targets only, via the scripts):** phone `:app:assembleDevDebug` and TV `:tv:assembleDevDebug` pass; web typecheck and `build:site` pass; phone unit tests **1283** (0 failures, 1 skipped), shared `jvmTest` **222** (0 failures), web **236** (0 failures). New tests: shared 40 (links, parser, gate), app 52 (Type 4 emulator and reader byte-level incl. reader-against-emulator round trip and hostile tags, NDEF record, QR decode through the camera path incl. rotated and padded frames, hub), web 18. The local Pages check passes for the new link shapes and confirms the 404 hop keeps the query string and `noindex`. A **minified (R8) prod release build of phone and TV succeeded** (unsigned: no keystore on this PC; 52 minutes); in its merged manifest the HCE service is present and disabled, NFC/camera features are `required="false"`, the NDEF and `violence://song|profile|party` filters and the new App Links prefixes are present, and the TV package contains none of the NFC/camera permissions or the service. Wear OS and desktop: dormant, not checked.
- **UNVERIFIED (needs two phones):** that a second phone's reader mode, and its system dispatch, read the emulated tag, per model; the AID/APDU details against the NFC Forum text; `Le` handling; URI prefix codes; Secure NFC behaviour; the Android 17 `DISPATCH_NFC_MESSAGE` requirement (not added: the app targets API 36); camera scan speed; App Links verification of the new prefixes; R8 inlined or merged some small classes (`NfcShareSession`, `IncomingLinks`) in the release build, which is allowed and behaviour-preserving, but the release build was not run on a device.
- **Known limits:** NFC is Android phone to Android phone only (no iPhone); the new phone strings are English placeholders in the 17 other languages; a TV cannot add to a playlist from a shared song; the web has no scanner and no NFC; a card longer than 250 bytes (a 24-character CJK name) is shared by QR or link, not by tap.

---

## Phase 10: Final Audit and Handover [PLANNED]

The end-to-end audit on personal devices that the old Phase 9 left to the owner, plus a last pass over the docs and the repo. (Renumbered from the earlier "Phase 8".)

## Phase 11: Playback Sources and Login Flows Log [PLANNED]

A document of the playback sources and the login flows (YouTube Music, Spotify) as they are. (Renumbered from the earlier "Phase 9".)

---

## Upstream syncs

| Date | Upstream version | Branch | Result | Details |
|---|---|---|---|---|
| 2026-10-31 | BitChord v1.9 (`04f64c9`) | `violence-nk` | DONE: phone + TV + web build, tests pass | `fork-docs/UPSTREAM_SYNC_LOG.md`, playbook in `fork-docs/UPSTREAM_SYNC.md`. Kept desktop (upstream deleted it). Version now `1.9.0-violence.1`. |

> **Phase 9, part 2 (2026-11-02):** sender card in links ("From Alex"), opt-in auto-join of friends' Jams by NFC tap (default off), Add to Jam queue, mini-player long-press share, haptic tick, Copy app link. See `SHARE_DESIGN.md` section 17 and `SHARE_TEST_PLAN.md` section 11. Phone and TV debug builds, shared/phone/web tests pass; real-device behaviour UNVERIFIED.

> **Part 2 of Bug Fixes (2026-11-02):** Profile spacing; Replay fills the whole screen (backdrop edge to edge, content inside the system bars, 9:16-wide column on wide windows); a Receive button in the top bar (app) and the sidebar and mobile bar (web); playlists by Tap to share, QR and link (`violence://playlist/...`, a YouTube Music playlist by id or the first songs of a phone-only playlist); Liquid Glass: cached shadow/highlight recordings and a held (not rebuilt per recomposition) glass modifier for smoothness, and glass material on dialogs, bottom sheets and settings cards; long-press-to-share now also works on the glass nav bar. **UNVERIFIED on a device:** the smoothness gain (no profiler was run), and how the new glass panels look.


> **Part 3 (2026-11-02):** mini-player long-press now detected on the way down (works on the glass bar); Copy link rows removed from the share sheet; Recent shares page, vibration/chime switches, Quick Settings "Receive" tile, "Last read" note on Receive, My QR / Send song / Invite to Jam on Friends, playlist web landing page and cover/Add to Jam queue on the playlist card; debug frame-time logging (`adb logcat -s VioFrames`); credits reworded to "a fork of BitChord, created by Kushagra Singh"; top-bar Receive and Downloads buttons fold under the account photo on scroll; Home re-tap scrolls to top and refreshes; Liquid Glass on the player menu and its round buttons; sheet rim removed (it outlined the screen above the sheet). Newest sharing strings translated into the 17 other languages (older Phase 8/9 strings are still English). NOT done: shared Replay comparison between friends. UNVERIFIED on a device: all of it.

> **Part 4 (2026-11-02):** Liquid Glass rebuilt on the bars' own pipeline for dialogs and sheets (a small still of the page, taken once and shared: blur, edge lens, rim, shadow; plain Material when glass is off or under Reduce dynamic blur), the player's round buttons (glass over a still of the player, retaken on song change; lightweight look only over a live Canvas), and the player menu (thinner fill so the lens shows). Top-bar actions tuck under their neighbour with one critically damped spring (no clipping, no double animation). Adding a friend now offers your own card back, since friends are stored per phone. Unverified on device: glass look and frame times.
