# PROJECT_STATE: Violence (verified snapshot)

**Verified:** 2026-10-31 on branch `violence-nk` (created from `main` @ `8ba30bf`). Everything below was checked in the repo unless marked **UNCLEAR**. Nothing was fixed in this session.

## 1. What this is
Private, GPL-3.0 modified fork of BitChord (kushagrasinghx/BitChord), rebranded "Violence". Used by 4-6 people (up to 10-15 with friends/family). Never published. Everything must be 100% free (no paid tiers, expiring trials, or credit cards). Origin: `https://github.com/yashaswahuh/Violence.git`.

## 2. Git state at session start
- Remotes: `origin` (private). `upstream` added this session = `https://github.com/kushagrasinghx/BitChord.git`, push URL `DISABLED`. Newest upstream tags: v1.9, v1.8.1, v1.8, v1.7, v1.6 (v1.9 is newer than the fork base, which is based on 1.8.1).
- Branches: `main` (8ba30bf, 2026-10-31), `violence-main` (a333b34, 2026-10-30), `contributors`. `main` is 3 ahead / 0 behind `violence-main`, so `main` contains it. `main` was the base. `violence-nk` created and pushed with upstream tracking.
- Working tree was clean. History is only 24 commits.
- All 9 phase commit hashes cited in PHASES.md exist and are reachable from `main`.

## 3. Module map
| Module | Type | Feeds | Status |
|---|---|---|---|
| `:shared` | KMP lib (android + jvm) | all | ACTIVE |
| `:sharedUi` | CMP lib | app, tv, desktop | ACTIVE |
| `:app` | Android phone (flavors `dev`/`prod`) | phone | ACTIVE |
| `:tv` | Android TV; compiles `:app` source dirs, overrides same-path files | TV | ACTIVE |
| `web/` (pnpm: `app`, `extension`, `shared`) | React 19 + Vite SPA, WXT MV3 extension | web | ACTIVE |
| `backend/` | Go server (gorilla/websocket), in-memory, no DB | Jam | ACTIVE (not yet deployed) |
| `native/` | C++ analyzer used by `:app`/`:tv` CMake | phone, TV | ACTIVE (never delete) |
| `:wearos` | Wear OS, still BitChord branded (`com.music.bitchord`, "BitChord") | watch | DORMANT |
| `:desktopApp`, `:desktopUi` | Compose Desktop, branded Violence | desktop | DORMANT |

`settings.gradle.kts` (rootProject name still `BitChord`) includes `:shared :sharedUi :desktopUi :desktopApp` always, and `:app :tv :wearos` only if an Android SDK is found (`local.properties sdk.dir`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`). Without an SDK the Gradle build is desktop-only.

Build stack: Gradle 8.11.1 wrapper, AGP 8.10.1, Kotlin 2.3.20, R8 8.13.23 classpath override in root build, Compose MP 1.10.3, Media3 1.11.0, compileSdk 37, targetSdk 36, minSdk 26, JDK 17 required for Android (AGP enforces it). Version 1.8.1, code 28 (phone) / 26 (TV, hardcoded in `tv/build.gradle.kts`), beta suffix `beta1`. Internal Kotlin package and namespace stay `com.music.bitchord` (JNI symbols depend on it).

## 4. Identifiers
- Phone: `com.yashaswahuh.violencemusic` (dev flavor `.dev`). TV: `com.yashaswahuh.violencemusic.tv` (dev `.tv.dev`). Desktop bundleID `com.yashaswahuh.violencemusic.desktop`. Deep link scheme `violence://` (`violence://party/<CODE>?server=...`). FileProvider `${applicationId}.provider`.
- Desktop packaging (`desktopApp/build.gradle.kts`): `packageName = "Violence"`, `dockName = "Violence"`, Windows `menuGroup = "Violence"`, `upgradeUuid = "ac94b47f-e765-4578-91a1-a5497a4c267c"`. Preserve all of these.

## 5. How the code works (verified in source)
- **Search/playback:** `YtMusicRepository` (InnerTube `WEB_REMIX`, direct client to YouTube, no API key) -> `SourceResolver`/`SourceRegistry` (addons, JioSaavn) -> `StreamResolver`: InnerTubeX client walk (VISIONOS, WEB_REMIX, WEB, MWEB) with BotGuard PO token via WebView and Rhino cipher solving; NewPipeExtractor fallback; HTTP probe before ExoPlayer. `PlaybackService` (Media3 session service), `CrossfadeController` (two ExoPlayers, 0-12 s, Automix with native analyzer + ONNX), `DownloadService`, `LyricsRepository` (13 providers), `LyricsTranslation`. No audio goes through any server.
- **YT Music login:** `auth/YtMusicLoginScreen.kt` opens Google sign-in in a WebView, then `CookieManager.getCookie("https://music.youtube.com")`. The cookie jar is stored in `AuthStore` (`EncryptedSharedPreferences`, AES256-GCM, falls back to plain prefs if the Keystore is unusable; excluded from backups). Requests are signed in `Innertube.kt` with `SAPISIDHASH` plus `X-Goog-AuthUser`, and multi-account/brand profiles are supported.
- **Spotify login/import:** `auth/SpotifyLoginScreen.kt` WebView login, reads the `sp_dc` cookie from `open.spotify.com`. `data/canvas/SpotifyToken.kt` injects `sp_dc` into an offscreen WebView to harvest bearer and client tokens. `SpotifyLibrary` imports Liked Songs and playlists, `SpotifyMatch` maps them to YT Music (ISRC then fuzzy), `SpotifyCanvas` fetches canvas videos. Where the `sp_dc` value is persisted: **UNCLEAR** (not traced to a specific store; check `AppSettings`/`AccountSessions`).
- **Discord RPC:** WebView login, user token in `AuthStore`, direct gateway WebSocket.
- **Scrobbling:** `data/scrobbling/` (`ScrobbleManager`, `LastFM` for Last.fm/Libre.fm, `ListenBrainzManager`). Last.fm needs `LASTFM_API_KEY`/`LASTFM_SECRET` from `local.properties` or env (BuildConfig).
- **Jam (Listen Together):** client `app/.../data/listentogether/ListenTogether.kt`, `JamInviteLink.kt`, `playback/PartySync.kt`, shared models and `ServerClock` in `shared/.../listentogether/`, web `web/app/src/party/`. Server URL = `BuildConfig.LISTEN_TOGETHER_SERVER` from `local.properties`/env `LISTEN_TOGETHER_SERVER`, else the hardcoded default `https://bitchord-listen-together.onrender.com` (`app/build.gradle.kts:53`, `tv/build.gradle.kts:52`); user can override in the Jam screen. Web default `DEFAULT_SERVER = 'https://api.bitchord.kushagrasingh.in'` (`web/app/src/party/listenTogether.ts:38`). Protocol: REST `POST /api/parties`, `POST /api/parties/{code}/join`, `GET /api/parties/{code}` (+`/preview`), `POST .../leave`, `GET /api/time`, `/healthz`, `/` and `/invite/{code}` landing page; WebSocket `/ws/parties/{code}` with a per-device bearer token (native: `Authorization` header; browser: subprotocol `bitchord.v1, bitchord.token.<hex>`). Frames: client `ping, sync, syncQueue, report, control{play,pause,seek,setTrack,setQueue,queueAdd,queueRemove,queueClear,queueMove,next,previous,setHostOnlyControl,setMaxMembers,kick}`; server `welcome, pong, state, queue, members, activity, error, bye`. Anchor-and-offset timeline, `seq`/`queueSeq` counters, 350 ms play lead, 45 s reconnect grace, 5 s state heartbeat, 5 members default, rooms in memory only (no DB, no secrets, no outbound calls). Config via `JAM_*` env vars (`backend/config`). Deploy files in `backend/deploy` (Oracle/Caddy/systemd, upstream's) and `backend/render.yaml`.
- **Web + extension:** SPA resolves YouTube in the browser (YouTube.js, Shaka DASH). The WXT extension (`web/extension`) injects headers via declarativeNetRequest and brokers cookies, Spotify and Discord, because browsers block direct YT Music requests. No server proxy exists. Allowed site origins in `web/extension/site.config.ts` = `https://yashaswahuh.is-a.dev` (+ localhost:5173 in dev). `build:site` packages the extension zips into `web/app/public/downloads/`. Vite `base` defaults to `/Violence/` (capital V).
- **TV:** `TvActivity`, Compose-for-TV UI, `TvAuthServer` (local HTTP :8080 + QR so a phone can post YT cookies to the TV), refresh-rate controller, USB scanner. Overrides `wear/WatchInstaller.kt` and `wear/WearBridge.kt` with stubs.
- **Icon picker:** `AppIcon.kt` toggles 14+ `activity-alias` entries (`.LauncherDefault`, `.LauncherGlass*`, `.LauncherSamsungGlass*`, `.LauncherPixelGlass*`, ...) via `PackageManager.setComponentEnabledSetting`.
- **Update checker:** `AppUpdateChecker.kt`, (rewritten in Phase 7, see section 18): reads `update.json` from `UPDATE_INFO_URL`; `UPDATE_APK_KEY` = `phone` / `tv`. The GitHub-releases lookup is gone.
- **Wear OS:** bundled into the phone APK by `BundleWearApk` unless `-Pbitchord.bundleWear=false`.

## 6. Phase and task status
| Item | Docs say | Verified |
|---|---|---|
| Phase 0 audit docs | done | 17 docs in `/fork-docs` exist |
| Phase 1 hygiene + decoupling | done 2026-10-30 | DONE (code verified: Presence `ENDPOINT=""`, updater `""`, donation files deleted) |
| Phase 2 rebrand | done 2026-10-30 | MOSTLY DONE, see discrepancies D1-D4 |
| Early CI (web.yml, android.yml) | done | exist, see section 8 |
| Desktop rebrand (called "Phase 3" in REBRAND_LOG s.6) | done | DONE (packaging, upgradeUuid verified) |
| Alternate app icons | done | commit `3a8d446`; 14 aliases present; artwork content not visually checked (**UNCLEAR**) |
| Fork attribution | done | README section, NOTICE, TV About dialog, phone SettingsSheet, web Sidebar footer all present |
| Upstream sync to v1.9 | not run | NOT RUN (fork at 1.8.1) |
| Phases 3-9 (as numbered in PHASES.md) | not run | NOT RUN |

## 7. Decoupling check (DECOUPLING_LOG vs code)
Verified disabled: presence pings, author update checker, donation links/files, author settings links, Discord promo button default, `FUNDING.yml`, `deploy-backend.yml`, `close-issue.yml`, README stats badge. No analytics or crash SDK exists upstream. **Nothing playback-, search-, lyrics-, login- or Jam-related was wrongly disabled.** Jam is intentionally untouched (still pointing at the author's server).

## 8. Workflows
- `web.yml` (violence-main, main; web/**): pnpm `build:site`, deploy to GitHub Pages with `VITE_BASE_URL` = Pages base path or `/Violence`. Summary line says `https://yashaswahuh.is-a.dev/Violence/`.
- `android.yml` (violence-main, main): `:app:assembleDevDebug :tv:assembleDevDebug -Pbitchord.bundleWear=false`, uploads APK artifacts (30 days).
- `tv.yml` (main, PR): `:tv:assembleDevDebug`. Upstream, not updated.
- `check.yml` (main, PR): desktop tests and `createDistributable` on 3 OSes (touches dormant desktop). Upstream.
- `release.yml` (tags `v*.*.*`): desktop packaging release (DEB/RPM/MSI/DMG...). Upstream; would publish desktop builds if a tag is pushed.
- `contributors-image.yml`, `update-contributors.yml`: upstream contributor widgets, scheduled (**UNCLEAR** whether they still run or are harmless in the private repo).
- Not present: `/scripts`, `fork-docs/ALLOWED_EXTERNAL_SERVICES.md` exists, but `fork-docs/BUILD_COMMANDS.md` and `fork-docs/FROZEN_TARGETS.md` do NOT exist. `.github/scripts/contributors_svg.py` exists.

## 9. Discrepancies between docs and reality
- **D1 (bug):** `tv/src/main/AndroidManifest.xml:246-257` still has host `bitchord.kushagrasingh.in` and scheme `bitchord`. REBRAND_LOG claims `:tv` was rebranded to `violence://`. TV invite links will not open the TV app. Comments there also still say BitChord.
- **D2 (bug):** `shared/.../listentogether/JamInvite.kt:12` `ORIGIN` is still `https://bitchord.kushagrasingh.in`, while `JamInviteLink.kt` uses `https://yashaswahuh.is-a.dev/violence`. The two disagree on the invite format; `JamInvite` is also tested in `JamInviteTest`.
- **D3:** Web path casing: `vite.config.ts` base and `web.yml` use `/Violence/`; docs, `JamInviteLink.ORIGIN`, the manifest `pathPattern` (`/violence/invite/...`) and `web/app/src/party/invite.ts` use `/violence/`. GitHub Pages is case sensitive. **Owner confirmed: the live URL is `https://yashaswahuh.is-a.dev/Violence` and `yashaswahuh.is-a.dev` is the Pages custom domain.** So `vite.config.ts` and `web.yml` are right, while `JamInviteLink.ORIGIN`, the manifest `pathPattern` `/violence/invite/...` and `invite.ts` use the wrong casing (not changed; owner said to change nothing else).
- **D4:** `backend/` still carries upstream branding: Go module `github.com/KabirSinghBhatia/BitChord/backend`, `backend/README.md`, `deploy/*` (Oracle domains, author's repo clone URL, `bitchord.in` origins), `render.yaml` service name `bitchord-listen-together`, and the invite HTML page's "Download" link to the author's releases (`main.go:752`). The healthz `service` is `violence-listen-together` but the Jam client test still expects `bitchord-listen-together` (whether the client checks this: **UNCLEAR**).
- **D5:** `AGENT_RULES.md` says Wear OS and Desktop are "pruned" and tells agents to "commit and push directly to main" and never build TV. Reality: they are present (dormant), the branch rule is now `violence-nk`, and TV is ACTIVE. PHASES.md Phase 3 still plans deleting Wear/Desktop, which contradicts the "dormant, kept intact" decision. Do not run Phase 3 as written. Phase 3 in PHASES.md (prune) vs "Phase 3" in REBRAND_LOG (desktop rebrand) have clashing numbers.
- **D6:** PHASES.md Phase 1 claims TV was built and passed, while AGENT_RULES forbids TV builds. TV build status: **UNCLEAR**.
- **D7:** `TV_TARGET_AUDIT.md` still lists old application IDs (stale). PHASES.md Phase 4 says Render or Firebase; the owner decision is self-hosted Go backend with Firebase as documented fallback only (FREE_HOSTING_PLAN and OPEN_QUESTIONS still say Firebase is primary). Phase 6 says Cloudflare Pages; decision is GitHub Pages.
- **D8:** AGENT_RULES.md line 1 has a stray image embed. `OPEN_QUESTIONS.md` uses `DISABLE` for the push URL; actual value used is `DISABLED`.
- **D9:** `.claude/launch.json` is tracked (harmless, **UNCLEAR** if intended). `.gitignore` ignores `AGENTS.md`, so `CLAUDE.md` is fine.

## 10. Author-hosted references that remain
| Reference | Where | Impact |
|---|---|---|
| `bitchord-listen-together.onrender.com` | `app` and `tv` build.gradle.kts default | Jam works only through the author's server until re-pointed (Phase 4). |
| `api.bitchord.kushagrasingh.in` | `web/.../listenTogether.ts:38` | Same, web. |
| `bitchord.kushagrasingh.in` | TV manifest, `shared JamInvite.kt`, `JamInviteLink` legacy accept, tests, backend README/deploy | Invite links (D1/D2). |
| `github.com/kushagrasinghx/BitChord` links | Settings/TV About/web footer/sidebar (attribution, intended), `DESKTOP.md`, `docs/DESKTOP_RELEASES.md`, `MAINTAINERS.md`, `CODE_OF_CONDUCT.md` (author's email `kushagrasinghx@gmail.com`), `backend/deploy` | Attribution is intended; the rest is cosmetic or stale. |
| Embedded public Google key `AIzaSy...` in `PoTokenWebView.kt` (and desktop equivalent), Musixmatch fallback secret | upstream code | Not the owner's secrets; leave them. |
Unaffected (not author-hosted): YouTube/Google, Spotify, all lyrics providers, JioSaavn, Last.fm, ListenBrainz, Discord, Cast, WebDAV. The only author-hosted *functional* dependency is Jam.

## 11. Secrets scan
No keystore, `local.properties`, `google-services.json`, `.env`, service-account or key file is tracked (only `web/app/.env.example`, empty values; `keystore.properties.example`). Pattern grep of tracked content found no tokens. `.gitignore` covers keystores, `.env*`, service accounts, `local.properties`, `*.apk`. Git history was not scanned for old secrets (**UNCLEAR**).

## 12. Baseline build (once, 2026-10-31)
Command: `.\gradlew.bat :app:assembleDevDebug` in the clone with `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`.
**Result: FAIL (environment, not code).** This machine has only JDK 16 and JDK 26. With JDK 26 Gradle 8.11.1 aborts ("What went wrong: 26.0.2.1", it cannot parse the version). With JDK 16 AGP fails: "Android Gradle plugin requires Java 17 to run." The Android SDK folder has only `platforms/android-34`, with no `ndk`, no `cmake` (the project needs compileSdk 37, CMake 3.22.1 and an NDK). `gradlew projects/tasks` could not run, so task paths below come from build files and workflows. No pnpm or Go on PATH (corepack exists). Needed before any build works: JDK 17, SDK platform 37, NDK, CMake 3.22.1, pnpm via corepack. Not fixed, as instructed.

Task paths (from build files and CI): phone `:app:assembleDevDebug`; TV `:tv:assembleDevDebug`; web `corepack pnpm --dir web build:site`; phone tests `:app:testDevDebugUnitTest`; shared tests `:shared:jvmTest`; web tests `pnpm --dir web test`; backend `go test -v ./...` in `backend/`. Add `-Pbitchord.bundleWear=false` to phone/TV builds to skip Wear.

## 13. Known risks
1. Root `build`/`assemble`/`check`/`test` would pull in `:wearos`, `:desktopApp`, `:desktopUi`. Never run them.
2. Jam depends on the author's servers until Phase 4; they can disappear at any time.
3. The upstream clients and web player depend on unofficial YouTube/Spotify behavior and can break without notice; upstream v1.9 may contain fixes (sync not done).
4. `check.yml`/`release.yml` still target desktop; pushing a `v*.*.*` tag would trigger a desktop release.
5. GitHub Pages sites are publicly reachable by URL even for private repos.
6. D1-D3 may silently break invite/deep-link flows and the web URL.
7. Debug APKs from CI are signed with ephemeral keys, so updates over an existing install need an uninstall first.
8. The owner's Windows machine cannot currently build Android (see 12).

## 14. Open questions for the owner
1. ~~Live web URL~~ Answered: `https://yashaswahuh.is-a.dev/Violence`, custom domain confirmed.
2. Install JDK 17, SDK 37, NDK and CMake (or use Android Studio's JBR) so baseline builds can run?
3. Fix D1/D2 (TV manifest, `JamInvite.ORIGIN`) as a small Phase 2 follow-up?
4. Phase 3 (pruning) conflicts with "dormant, kept intact". Should PHASES.md be rewritten to drop it? Should the desktop-only workflows (`check.yml`, `release.yml`) be disabled?
5. Jam: confirm self-hosted Go on a free host (which one?) and whether to rename the Go module/`render.yaml`/service strings.
6. Where is the Spotify `sp_dc` persisted, and is that acceptable? (to be traced)
7. Do the contributor-image workflows need to stay?
8. When should the upstream v1.9 sync happen relative to Phase 4?

## 15. Update after Phase 4 (2026-11-01): Jam backend
- **Jam now uses the owner's own copy of `/backend`** (host: Render Free; fallback: Northflank Sandbox; last fallback: Firebase, not built). No Jam default points at the original author any more: Android/TV default is `JAM_BACKEND_URL` (empty), web default is `VITE_PARTY_SERVER_URL` from the repo variable `JAM_BACKEND_URL` (empty). **Not deployed yet by the agent; the owner does the manual steps in `JAM_BACKEND_DEPLOY.md`.** Corrects section 5 ("Where the Jam backend URL is configured today").
- Empty address is a supported state: the phone shows "Jam is not set up in this build yet"; the web shows a note. Wake-up ("Waking up the Jam server...", 85 s retry) applies to the built-in server only.
- Backend: stats/presence routes off by default (`JAM_ENABLE_STATS`), default allowed origins = owner's site + localhost, Go module renamed, author's `deploy/` kit deleted, `Dockerfile` added, display name no longer logged. Never compiled locally (no Go/Docker).
- Still open (unchanged): TV manifest scheme/host, `shared JamInvite.ORIGIN`, `/violence` vs `/Violence` in invite code (D1-D3 above).

## 16. Update after Phase 5 (2026-11-02): Jam reliability and invites
- **Invites:** the link base is `JAM_INVITE_BASE_URL` (empty = share only the code); links are `<base>/party/<CODE>`; the web has a landing page at `/party/:code`; `violence://party/<CODE>` is handled by phone and TV (cold start and running). **D1 (TV manifest) and D2 (shared `JamInvite.ORIGIN`) are fixed**; D3 (`/violence` vs `/Violence`) is resolved by using the configured base and matching any base path.
- **Sync:** a track that cannot play on one device no longer loops or moves the party (phone, TV, web). Host handover picks the oldest connected member. Rooms hold up to 15; default party size is 8.
- **TV now has Listen Together** (Settings > Listen Together). Web has resync after a hidden tab and a blocked-audio banner.
- **Not verified on devices or the real backend**: see `JAM_AUDIT.md` section 4 and `JAM_TEST_PLAN.md` sections 8-11.

## 17. Update after Phase 6 (2026-11-02): web hosting and cloud builds
- **Web:** `web.yml` builds for the `/Violence/` folder and deploys to GitHub Pages on pushes to `violence-nk`; the 404 fallback now keeps `/party/<CODE>` (the landing page) and handles every route; Automix models load from the base path. Checked locally with a Pages-like server and a real headless browser. **Not yet run on GitHub.**
- **Android:** `android.yml` builds the phone and TV debug APKs on pushes to `violence-nk` and uploads them (30 days). Needs JDK 21 as well as 17 because the dormant desktop module is part of the Gradle build.
- **Extension:** trusts exactly `https://yashaswahuh.is-a.dev` (plus localhost:5173 in dev builds). **Last.fm:** Android/TV only; keys from `local.properties` or `LASTFM_API_KEY`/`LASTFM_SECRET`; the web has none.
- **Owner to do:** Pages source = GitHub Actions; add `violence-nk` to the `github-pages` environment's allowed branches; optional repo variables `JAM_BACKEND_URL`, `JAM_INVITE_BASE_URL` and secrets `LASTFM_API_KEY`, `LASTFM_SECRET`; confirm the custom domain (UNCLEAR).

## 18. Update after Phase 7 (2026-11-02): TV review, signing, updates, App Links
- **Versions:** one place, root `build.gradle.kts` (`bitchordVersionCode`, `bitchordVersionName`, `bitchordBetaSuffix`). The TV now reads it too (it had its own `29` / copy of the name). Format `<upstream>-violence.N`, versionCode +1 per release. Currently `1.9.0-violence.1` / `35`, not yet released.
- **Signing:** `keystore.properties` (gitignored, points at a keystore outside the repo). Phone and TV release both come out unsigned without it (the TV used to silently fall back to the debug key; fixed). `scripts/release.ps1` builds `violence-phone-<v>.apk` and `violence-tv-<v>.apk` + `update.json` into `release-out/<v>/` (gitignored).
- **Updates:** `AppUpdateChecker` reads `update.json` (`versionCode`, `versionName`, `changelog`, `apks.<phone|tv>-<cpu>.{url,sha256}` (per-CPU APKs, the app picks its own)) from `UPDATE_INFO_URL` at most once per 24 h, compares versionCode, verifies SHA-256, installs through the system installer (FileProvider `<applicationId>.provider`, `REQUEST_INSTALL_PACKAGES`). Files are served by GitHub Pages: `web.yml` copies the newest published release's `update.json` + the per-CPU APKs it lists into the site folder `updates/` (also on every later deploy). Tests: `AppUpdateCheckerTest`.
- **App Links:** one `autoVerify` filter (both manifests) for `https://yashaswahuh.is-a.dev` with prefixes `/Violence/party/` and `/Violence/invite/`; owner must publish `.well-known/assetlinks.json` at the domain root (`APP_LINKS.md`).
- **TV:** read through (no device): focus-dropping disabled buttons in the Jam keypad fixed, update dialog rebranded. `TV_TEST_PLAN.md` is the manual test list. Docs: `RELEASE_GUIDE.md`.
- **Not done / unverified:** nothing here has run on a phone, TV, or on GitHub (release workflow trigger, Pages file-size limit, assetlinks verification).
