# CLAUDE.md: Violence

Read this first. Verified state: `fork-docs/PROJECT_STATE.md`. Roadmap: `fork-docs/PHASES.md`. Rules: `fork-docs/AGENT_RULES.md` (updated in Phase 3). Build commands: `fork-docs/BUILD_COMMANDS.md` and `scripts/`.

## What this is
"Violence": a private, modified fork of BitChord (https://github.com/kushagrasinghx/BitChord, GPL-3.0), an Android music player with an Apple Music style UI that plays YouTube Music and links Spotify. Application ID `com.yashaswahuh.violencemusic` (TV: `.tv`, dev flavors add `.dev`), deep link scheme `violence://`. Used privately by 4-6 core people (10-15 with friends/family). Never published. Everything must be 100% free: no paid plans, expiring trials, or credit cards. Kotlin Multiplatform + Compose; internal package is `com.music.violence` (renamed from `com.music.bitchord` on 2026-11-04, with the JNI symbols; storage names, the Jam protocol and attribution keep the old name on purpose).

## Targets
- ACTIVE: Android phone (`:app`), Android TV (`:tv`), web (`/web`), plus `:shared`, `:sharedUi`, `/native`, `/backend` (Go).
- DORMANT: Wear OS (`:wearos`), desktop (`:desktopApp`, `:desktopUi`). Frozen, not removed. See `fork-docs/FROZEN_TARGETS.md`.

## Decisions
- Keep the original's playback APIs and services, even ones the author hosts, and the in-app YT Music and Spotify web logins (WebView session cookies/tokens captured and used locally).
- Disabled: live listener stats, open-app presence pings (Android and web), analytics, crash reporting, the author's update checker, donation prompts.
- Jam (shared listening) uses a self-hosted copy of the Go backend in `/backend` on a free host (Render Free; steps in `fork-docs/JAM_BACKEND_DEPLOY.md`). The apps get its address from `JAM_BACKEND_URL` and shared invite links point at `JAM_INVITE_BASE_URL` (both in `local.properties` / GitHub repo variables, both empty by default: "Jam is not set up" and "share only the code"). Firebase RTDB is only a documented fallback and is not built. The owner has not deployed it yet as of Phase 4 (UNCLEAR whether they have since).
- Web is hosted on GitHub Pages at https://yashaswahuh.is-a.dev/Violence/ (capital V, confirmed by the owner; Pages is case sensitive and names the folder after the repository), deployed by `.github/workflows/web.yml` on pushes to `violence-nk`, and needs the browser extension (`web/extension`) which trusts exactly that origin. APKs are built in the cloud by `android.yml`. See `fork-docs/CI_DEPLOY.md`.
- Last.fm scrobbling stays; keys come from `local.properties` (`LASTFM_API_KEY`, `LASTFM_SECRET`).
- **Violence profile (Phase 8):** a local, optional name + preset avatar + colour (and a phone-only local photo), plus a local friends list, independent of every login (`fork-docs/PROFILE_DESIGN.md`). Jam uses it as the member identity and then needs no YT Music sign-in. The profile-card link `violence://profile?v=1&...` is strict, versioned and untrusted-input safe; photo, email, account ids and nicknames never leave the device. Never log profile data.
- **Sharing (Phase 9):** a song, a profile card or a Jam invite can be shared by NFC tap (HCE Type 4 tag, only while the Tap to share screen is showing), QR code or link (`fork-docs/SHARE_DESIGN.md`). Every incoming link, from any source, goes through `IncomingLinkHub` (shared `IncomingLinks` parser): untrusted, strictly validated, never acts without a confirmation. The `ShareHostApduService` stays disabled in the manifest except while that screen is up. Nothing is logged and sharing makes no network call.

## BRANCH RULE
All work, commits, and pushes go to the branch violence-nk, never main and never violence-main. Never force-push, never rewrite history, never push to the upstream remote.
Before starting and before every push run `git branch --show-current`; if it is not violence-nk, switch or stop. Remote `upstream` = Violence, push URL `DISABLED`. Upstream v1.9 was merged on 2026-10-31 (`fork-docs/UPSTREAM_SYNC_LOG.md`; playbook and watch list `UPSTREAM_SYNC.md`).

## Environment
- Owner is on Windows; use PowerShell. Git Bash is also available.
- Toolchain on the owner's PC (installed 2026-10-31/11-01): JDK 17 (Android) and JDK 21 (desktop toolchain; **required by Gradle for any build** because `:desktopApp` is in the build), Android SDK platforms 36 and 37.0, NDK 27, CMake 3.22.1. `ANDROID_HOME` or `local.properties sdk.dir` must point at the SDK.
- Easiest: `.\scripts\build-active.ps1 [-Target phone|tv|web]` and `.\scripts\test-active.ps1 [-Target phone|web]`. They set the toolchain for the session.
- Explicit Gradle task paths (never root-level):
  - Phone: `.\gradlew.bat :app:assembleDevDebug '-Pviolence.bundleWear=false'`
  - TV: `.\gradlew.bat :tv:assembleDevDebug '-Pviolence.bundleWear=false'`
  - Web: `corepack pnpm --dir web build:site` (tests: `corepack pnpm --dir web test`)
  - Tests: `:app:testDevDebugUnitTest`, `:shared:jvmTest`; backend: `go test -v ./...` in `backend/`
- PowerShell: quote the `-P` flag, or it is split at the dot. Web scripts need `pnpm` on PATH (corepack shim) and `VITE_PARTY_SERVER_URL` set (the scripts do both).
- **NEVER run root-level Gradle tasks** (`build`, `assemble`, `check`, `test`, `lint`, `clean`, or a bare `.\gradlew.bat`): they include the dormant modules.
- Secrets live in `local.properties`, `keystore.properties`, env vars. All gitignored.

## DORMANT TARGETS
Dormant: `:wearos`, `:desktopApp`, `:desktopUi` (details: `fork-docs/FROZEN_TARGETS.md`).
- Never edit, rebrand, refactor, build, test, or lint the dormant modules unless the owner explicitly says "unfreeze".
- Never run root-level Gradle tasks without explicit module paths; use `BUILD_COMMANDS.md` or the scripts in `/scripts`.
- Do not include dormant targets in verification or reports beyond one line: "Wear OS and desktop: dormant, not checked."
- Never remove code, dependencies, or resources that only dormant targets use (`wearos/protocol` is compiled by `:app`; keep `CMP_DESKTOP` in `sharedUi`, `/native/analyzer`, and the desktop `include`s in `settings.gradle.kts`).
- Reading dormant code is allowed only when a task truly requires it.
- If a change to shared code (`:shared`, `:sharedUi`, `/native`) could affect a dormant target: make the change if the active targets need it, do NOT touch the dormant target, and append a line to "Possible impact from later changes" in `fork-docs/FROZEN_TARGETS.md`.
- Keep desktop's Violence branding and Windows `upgradeUuid` (`ac94b47f-e765-4578-91a1-a5497a4c267c`, in `desktopApp/build.gradle.kts`) untouched, always.
- After any merge or upstream sync, check the dormant modules and their `settings.gradle.kts` includes survived (upstream v1.9 deleted desktop and Git silently dropped the includes once).

## KEEP rule
Never remove or break playback, search, stream resolution, lyrics, metadata, artwork, downloads, Last.fm/ListenBrainz, the YT Music and Spotify logins, the web extension support, or any service marked KEEP in `fork-docs/ALLOWED_EXTERNAL_SERVICES.md`, even if the original author hosts it. Services marked KEEP are untouchable. Only Jam is author-hosted and it is REPLACE-LATER.

## Security rules
- Never commit secrets, keystores (`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, `.env*`, `google-services.json`, or login cookies/tokens. Never `git add -f` ignored files.
- YT Music, Spotify and Discord cookies, tokens and session data are never logged, never committed, and never sent to any server except the service they belong to.
- Keep GPL-3.0: keep `LICENSE`, `NOTICE`, existing copyright headers and the fork attribution (README, NOTICE, About screens, web footer).

## How to work
1. One phase per session. Read `fork-docs/PHASES.md` and `fork-docs/AGENT_RULES.md` first. If the phase is already marked done, report and stop. Phases 0-9 are done; next is Phase 10 (final audit and handover).
2. Never guess. Write UNCLEAR when something cannot be verified in the repo.
3. Small commits on `violence-nk`; push to `origin violence-nk` only.
4. When a phase finishes, update PHASES.md and the relevant `fork-docs/` files.
5. Run only explicit-module builds and tests. Never claim a build passed without its output.

## Things that must never happen
- Pushing to `main`, `violence-main`, or `upstream`; force-pushing; rewriting history; opening PRs to Violence.
- A root-level Gradle task; building or testing Wear OS or desktop without being asked.
- Committing any secret, keystore, `local.properties`, cookie or token.
- Re-adding telemetry, presence pings, analytics, crash reporting, the author's updater, or donation links.
- Disabling or deleting a KEEP service, a login flow, or Jam wiring to "clean up".
- Using a paid service, a trial, or anything needing a credit card.
- Publishing the APKs, web build links, or code publicly. Pushing a `v*.*.*` tag is allowed to do nothing now (`release.yml` is manual-only) but still do not.
- Changing desktop branding or `upgradeUuid`; deleting `/wearos`, `/desktopApp`, `/desktopUi`; renaming the internal `com.music.violence` package; deleting `/native/analyzer`.
- Removing `NOTICE`, `LICENSE`, or attribution to Kushagra Singh.

## Known open issues (details in PROJECT_STATE.md s.9)
Jam fixes from Phase 5 (TV Jam dialog, web landing page, 15-member rooms, unplayable-track isolation) have never run on a real phone, TV or browser, the Go tests ran for the first time in Phase 8 and pass (portable Go; `go test -race` needs a C compiler and was not run); desktop not compiled against v1.9 shared code; the Go backend and Dockerfile have never been compiled on the owner's PC (no Go/Docker), so the first Render build is the first compile.

## Key docs (`fork-docs/`)
PROJECT_STATE, PHASES, AGENT_RULES, PROFILE_AUDIT, PROFILE_DESIGN, SHARE_DESIGN, SHARE_TEST_PLAN, BUILD_COMMANDS, CI_DEPLOY, WEB_USER_GUIDE, LASTFM_SETUP, FROZEN_TARGETS, RESTORATION_LOG, UPSTREAM_SYNC(_LOG), JAM_BACKEND_FEASIBILITY, JAM_BACKEND_DEPLOY, JAM_TEST_PLAN, ALLOWED_EXTERNAL_SERVICES (KEEP/DISABLE/REPLACE-LATER), DECOUPLING_LOG, REBRAND_LOG, ARCHITECTURE, JAM_DESIGN, BACKEND_AUDIT, WEB_TARGET_AUDIT, TV_TARGET_AUDIT, FREE_HOSTING_PLAN, CI_DEPLOY, EXTERNAL_DEPENDENCIES, REBRAND_CHECKLIST, OPEN_QUESTIONS. `MODULE_PRUNING_PLAN.md` is CANCELLED (nothing is removed). Also `NOTICE`, `README.md`, `web/README.md`, `backend/README.md`.
