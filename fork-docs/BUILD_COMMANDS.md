# Build commands (active targets only)

Active: Android phone (`:app`), Android TV (`:tv`), web (`/web`), plus the shared code they use (`:shared`, `:sharedUi`, `/native`, `/backend`).
Dormant: Wear OS (`:wearos`), desktop (`:desktopApp`, `:desktopUi`). See `FROZEN_TARGETS.md`.

## The rule
**Root-level Gradle tasks are forbidden for routine work**: `build`, `assemble`, `assembleDevDebug`, `check`, `test`, `testDevDebugUnitTest`, `lint`, `clean`, or a bare `.\gradlew.bat`, with no `:module:` path. Gradle runs a task name in EVERY project that has it, so they would also build `:wearos` (it has `dev`/`prod` flavors too) and `:desktopApp`/`:desktopUi`. Always write the module path.

Verified 2026-11-01: `settings.gradle.kts` includes `:shared :sharedUi :desktopUi :desktopApp` always and `:app :tv :wearos` when an Android SDK is found.

## Easiest: the scripts
```powershell
.\scripts\build-active.ps1                 # phone + TV + web
.\scripts\build-active.ps1 -Target phone   # or tv, or web
.\scripts\test-active.ps1                  # phone + shared tests + web tests
.\scripts\test-active.ps1 -Target web
```
They set JDK 17 / `ANDROID_HOME` / `pnpm` for the session only, run only the explicit task paths below, and never touch dormant modules.

## Exact tasks (PowerShell)
Needs JDK 17, Android SDK platform 37.0 (and 36), NDK, CMake 3.22.1. **Quote the `-P` flag** in PowerShell, or it is split at the dot.

| What | Command |
|---|---|
| Phone debug APK | `.\gradlew.bat :app:assembleDevDebug '-Pviolence.bundleWear=false'` |
| TV debug APK | `.\gradlew.bat :tv:assembleDevDebug '-Pviolence.bundleWear=false'` |
| Phone unit tests | `.\gradlew.bat :app:testDevDebugUnitTest '-Pviolence.bundleWear=false'` |
| Shared unit tests | `.\gradlew.bat :shared:jvmTest '-Pviolence.bundleWear=false'` |
| Clean phone / TV only | `.\gradlew.bat :app:clean :tv:clean` (never a bare `clean`) |
| Web typecheck | `corepack pnpm --dir web typecheck` (scripts call `pnpm`, so put a corepack shim on PATH) |
| Web build | `corepack pnpm --dir web build:site` |
| Web tests | `corepack pnpm --dir web test` |
| Backend tests | `cd backend; go test -v ./...`. Go is not installed on the owner's machine. In Phase 8 they were run with a **portable** Go 1.27.3 (zip from go.dev, SHA-256 checked, unpacked into a scratch folder, `GOROOT`/`GOPATH`/`GOTOOLCHAIN=local` set for the session). **Do not set `GOFLAGS=-mod=mod`**: it rewrites `backend/go.mod` (drops `// indirect`). `-race` needs a C compiler and does not work on this PC. Real-server check: `go build -o jamserver.exe .` then run it with `PORT`, `JAM_CREATE_RATE_PER_MINUTE=200` and `node scripts/jam-test.mjs --url http://localhost:<port> --clients 4 --profiles`. |

`'-Pviolence.bundleWear=false'` stops `:app` from building and bundling the Wear OS APK. Without it, `:app` builds `:wearos` too (`app/build.gradle.kts` `BundleWearApk`).
Web needs `VITE_PARTY_SERVER_URL` set for `build:site` (upstream v1.9 refuses to build without it); the scripts and `web.yml` use the same default.
Outputs: phone `app/build/outputs/apk/dev/debug/` (4 APKs: universal, arm64-v8a, armeabi-v7a, x86_64), TV `tv/build/outputs/apk/dev/debug/`, web `web/app/dist`.
Measured (cold, this PC): phone ~15 min, TV ~6.5 min, tests ~2 min, web ~1 min.

## Owner-only manual desktop commands (dormant: never run by agents)
Run by hand by the owner, only when working on desktop. **Verified 2026-11-01** with `.\gradlew.bat :desktopApp:tasks --all` (configuration only, nothing built): `run`, `runDistributable`, `createDistributable` (plus `packageMsi`, `packageExe`, `packageDeb`, `packageRpm`, `packageDmg`, `packagePkg`) exist.
```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :desktopApp:createDistributable
```
Because `:desktopApp` is part of the Gradle build, **every build on a machine needs a JDK 21 toolchain installed** (`desktopApp/build.gradle.kts` line 161), in addition to JDK 17 for Android. On the owner PC it is the portable Temurin 21.0.13 in `%USERPROFILE%\.jdks\jdk-21.0.13+9` (Gradle finds it there); `winget install EclipseAdoptium.Temurin.21.JDK` also works. Without it even `:app:assembleDevDebug` fails at configuration with "Cannot find a Java installation ... languageVersion=21". Desktop has not been compiled against the v1.9 shared code (see `FROZEN_TARGETS.md`). The manual desktop CI workflow is `.github/workflows/desktop-check.yml` (`workflow_dispatch` only).
## CI (what runs automatically)
| Workflow | Runs on | Tasks |
|---|---|---|
| `android.yml` | push `violence-nk` (path-filtered), manual | JDK 21+17, SDK, `:app:assembleDevDebug :tv:assembleDevDebug -Pviolence.bundleWear=false`, uploads the two APKs (Phase 6) |
| `tv.yml` | push (same branches), PR | `:tv:assembleDevDebug` |
| `check.yml` | push (same branches), PR | `:app:testDevDebugUnitTest`, `:app:assembleDevDebug` (explicit), plus backend Go checks |
| `web.yml` | push `violence-nk` (`web/**`), release published, manual | typecheck + tests + `pnpm build:site` with `VITE_BASE_URL`=the Pages folder, adds the newest release's update files under `updates/`, deploy to GitHub Pages (Phase 6, 7) |
| `desktop-check.yml` | **manual only** | desktop tests and packaging (dormant) |
| `release.yml` | **manual only** (tag trigger removed) | desktop packaging release (dormant) |
| `contributors-image.yml`, `update-contributors.yml` | upstream cron/push `main` | no Gradle; README contributor widgets (not changed) |

## Web: build for the deployed folder and check it like GitHub Pages
```powershell
$env:VITE_BASE_URL='/Violence/'; $env:BITCHORD_STORE_BUILD='1'
.\scripts\build-active.ps1 -Target web        # or: corepack pnpm --dir web build:site
node scripts/serve-pages-local.mjs --check      # serves web/app/dist under /Violence/ like Pages and checks routes, assets, deep links
node scripts/serve-pages-local.mjs              # then open http://localhost:4173/Violence/
```
`BITCHORD_STORE_BUILD=1` is what CI sets so the packaged extension never answers pages on localhost.

## Release APKs (signed, for sharing) and the update file
Needs `keystore.properties` and a keystore outside the repo (`RELEASE_GUIDE.md` A1). Phone and TV only.
```powershell
.\scripts\release.ps1 -Bump -Changelog 'what changed'    # build, sign, write release-out\<version>\ (phone apk, tv apk, update.json), print the publish steps
.\gradlew.bat ':app:assembleDevRelease' '-Pviolence.bundleWear=false'    # dev flavor release, only when needed
```
Never run it with a real keystore from CI; the keystore stays on the owner's machine.
