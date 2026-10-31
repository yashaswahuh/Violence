# CLAUDE.md: Violence

Read this first. Full verified state: `fork-docs/PROJECT_STATE.md`. Roadmap: `fork-docs/PHASES.md`. Rules: `fork-docs/AGENT_RULES.md` (parts of it are outdated, this file wins; see PROJECT_STATE s.9 D5).

## What this is
"Violence": a private, modified fork of BitChord (https://github.com/kushagrasinghx/BitChord, GPL-3.0), an Android music player with an Apple Music style UI that plays YouTube Music and links Spotify. Application ID `com.yashaswahuh.violencemusic` (TV: `.tv`, dev flavors add `.dev`), deep link scheme `violence://`. Used privately by 4-6 core people (10-15 with friends/family). Never published. Everything must be 100% free: no paid plans, expiring trials, or credit cards. Kotlin Multiplatform + Compose; internal package stays `com.music.bitchord` on purpose (JNI).

## Targets
- ACTIVE: Android phone (`:app`), Android TV (`:tv`), web (`/web`), `:shared`, `:sharedUi`, `/native`, `/backend` (Go).
- DORMANT: Wear OS (`:wearos`), desktop (`:desktopApp`, `:desktopUi`).

## Decisions
- Keep the original's playback APIs and services, even ones the author hosts, and the in-app YT Music and Spotify web logins (WebView session cookies/tokens captured and used locally).
- Disabled: live listener stats, open-app presence pings, analytics, crash reporting, the author's update checker, donation prompts.
- Jam (shared listening) will be re-pointed to a self-hosted copy of the Go backend in `/backend` on a free host. Firebase RTDB is only a documented fallback.
- Web is hosted on GitHub Pages at https://yashaswahuh.is-a.dev/Violence (capital V, confirmed by the owner; `yashaswahuh.is-a.dev` is the Pages custom domain) and needs the browser extension (`web/extension`).
- Last.fm scrobbling stays; keys come from `local.properties` (`LASTFM_API_KEY`, `LASTFM_SECRET`).

## BRANCH RULE
All work, commits, and pushes go to the branch violence-nk, never main and never violence-main. Never force-push, never rewrite history, never push to the upstream remote.
(Remote `upstream` = BitChord, push URL is `DISABLED`. Latest upstream tag seen: v1.9; fork is at 1.8.1 and has not been synced.)

## Environment
- Owner is on Windows; use PowerShell (`.\gradlew.bat`). Git Bash is also available.
- Android builds need JDK 17, Android SDK platform 37, NDK, CMake 3.22.1, and `ANDROID_HOME` or `local.properties sdk.dir`. `:app`, `:tv`, `:wearos` are only included in Gradle when an SDK is found. As of 2026-10-31 the owner's machine had only JDK 16/26 and no NDK/CMake, so baseline `:app:assembleDevDebug` failed (environment). Do not "fix" this unless asked.
- Explicit Gradle task paths (never root-level):
  - Phone: `.\gradlew.bat :app:assembleDevDebug -Pbitchord.bundleWear=false`
  - TV: `.\gradlew.bat :tv:assembleDevDebug -Pbitchord.bundleWear=false`
  - Web: `corepack pnpm --dir web build:site` (tests: `corepack pnpm --dir web test`)
  - Tests: `:app:testDevDebugUnitTest`, `:shared:jvmTest`; backend: `go test -v ./...` inside `backend/`
- NEVER run root-level Gradle tasks (`build`, `assemble`, `check`, `test`, `clean`, or a bare `.\gradlew.bat`): they include the dormant modules.
- `AGENT_RULES.md` says "do not build TV". That is outdated: TV is active, build it only with the explicit `:tv:` task when relevant.
- Secrets live in `local.properties`, `keystore.properties`, env vars. All gitignored.

## Dormant targets rule
Do not edit, build, test, or lint Wear OS or desktop unless the task explicitly says so. Keep desktop's Violence branding and Windows `upgradeUuid` (`ac94b47f-e765-4578-91a1-a5497a4c267c`, `packageName = "Violence"`, bundleID `com.yashaswahuh.violencemusic.desktop`) untouched. Do not delete `/wearos`, `/desktopApp`, `/desktopUi`: Phase 3 "pruning" in PHASES.md contradicts the dormant decision and must not be run as written. When a change touches shared code (`:shared`, `:sharedUi`, `/native`), log the possible impact on dormant targets in `fork-docs/FROZEN_TARGETS.md` (create it if missing). Never delete `/native/analyzer` (phone and TV need it).

## KEEP rule
Never remove or break playback, search, lyrics, metadata, artwork, downloads, Last.fm/ListenBrainz, the YT Music and Spotify logins, or any service marked KEEP in `fork-docs/ALLOWED_EXTERNAL_SERVICES.md`, even if the original author hosts it. Only Jam is author-hosted and it is REPLACE-LATER.

## Security rules
- Never commit secrets, keystores (`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, `.env*`, `google-services.json`, or login cookies/tokens. Never `git add -f` ignored files.
- Session data (Google cookies, `sp_dc`, Discord token, Last.fm session) is never logged and never sent anywhere except the service it belongs to.
- Keep GPL-3.0: keep `LICENSE`, `NOTICE`, existing copyright headers and the fork attribution (README, NOTICE, About screens, web footer).

## How to work
1. One phase per session. Read `fork-docs/PHASES.md` and `fork-docs/AGENT_RULES.md` first. If the phase is already marked done, report and stop.
2. Never guess. Write UNCLEAR when something cannot be verified in the repo.
3. Small commits on `violence-nk`; push to `origin violence-nk` only.
4. When a phase finishes, update PHASES.md and the relevant `fork-docs/` files (REBRAND_LOG, DECOUPLING_LOG, PROJECT_STATE...).
5. Run only explicit-module builds and tests. Never claim a build passed without its output.
6. Windows PowerShell scripts are preferred if scripts are added (`/scripts` does not exist yet).

## Things that must never happen
- Pushing to `main`, `violence-main`, or `upstream`; force-pushing; rewriting history; opening PRs to BitChord.
- A root-level Gradle task; building or testing Wear OS or desktop without being asked.
- Committing any secret, keystore, `local.properties`, cookie or token.
- Re-adding telemetry, presence pings, analytics, crash reporting, the author's updater, or donation links.
- Disabling or deleting a KEEP service, a login flow, or Jam wiring to "clean up".
- Using a paid service, a trial, or anything needing a credit card.
- Publishing the APKs, web build links, or code publicly. Tagging `v*.*.*` (it would trigger the upstream desktop `release.yml`).
- Changing desktop branding or `upgradeUuid`; renaming the internal `com.music.bitchord` package; deleting `/native/analyzer`.
- Removing `NOTICE`, `LICENSE`, or attribution to Kushagra Singh.

## Known open issues (details in PROJECT_STATE.md s.9)
TV manifest still has old `bitchord` scheme/host; `shared JamInvite.ORIGIN` still the author's domain; the live web path is `/Violence` (matches `vite.config.ts`/`web.yml`), but `JamInviteLink.ORIGIN`, the app manifest `pathPattern` and `invite.ts` still say `/violence` (not changed yet); backend still carries upstream branding; Jam default server is still the author's.

## Key docs (`fork-docs/`)
PROJECT_STATE (verified snapshot), PHASES, AGENT_RULES, ALLOWED_EXTERNAL_SERVICES (KEEP/DISABLE/REPLACE-LATER), DECOUPLING_LOG, REBRAND_LOG, ARCHITECTURE, JAM_DESIGN, BACKEND_AUDIT, WEB_TARGET_AUDIT, TV_TARGET_AUDIT, MODULE_PRUNING_PLAN (do not execute), FREE_HOSTING_PLAN, CI_DEPLOY, EXTERNAL_DEPENDENCIES, REBRAND_CHECKLIST, OPEN_QUESTIONS, REPO_STATE. Also `NOTICE`, `README.md`, `web/README.md`, `backend/README.md`.
