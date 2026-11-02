# Frozen (dormant) targets

Status since Phase 3 (2026-11-01): **Wear OS and desktop are DORMANT.** Kept intact and revivable. Not edited, built, tested, or linted in routine work. The earlier plan to delete them (`MODULE_PRUNING_PLAN.md`, old Phase 3) is cancelled.

## (a) The dormant modules
| Module | Gradle path / dir | What it is | Status |
|---|---|---|---|
| Wear OS | `:wearos` (`wearos/`, source `wearos/src`, plus `wearos/protocol`) | Watch remote app: now playing, controls, local speaker playback; Data Layer to the phone. `applicationId` `com.music.bitchord` (dev `com.dev.bitchord`). | Dormant. Still **BitChord branded** (39 lines mention BitChord, incl. `app_name`). |
| Desktop app | `:desktopApp` (`desktopApp/`: `src`, `native` (SMTC, window helpers, CMake), `packaging`) | Compose Desktop launcher: tray, SMTC, JavaFX WebView BotGuard, jpackage (MSI/EXE/DEB/RPM/DMG/AppImage). | Dormant. **Violence branded by the owner and working** (as of the rebrand). |
| Desktop UI | `:desktopUi` (`desktopUi/`) | Desktop window layout, sidebar, wide now-playing; branched from `:sharedUi`. | Dormant. |
Also dormant by association: `DESKTOP.md`, `docs/DESKTOP_RELEASES.md`, `.github/workflows/desktop-check.yml` (manual), `.github/workflows/release.yml` (manual), the `bitchord.bundleWear` Gradle property path in `app/build.gradle.kts`, and the TV stubs `tv/.../wear/WatchInstaller.kt`, `WearBridge.kt` (they exist only because Wear code exists in `:app`).

## (b) Dependencies on shared code (what a change could affect)
- **`:wearos` -> shared code:** none by Gradle (`wearos/build.gradle.kts` has no `project(...)` deps). **Reverse link:** `:app` compiles `wearos/protocol` (`WearProtocol.kt`) as a source dir (`app/build.gradle.kts:101`), and `:app` hosts `WearBridge.kt`/`WatchInstaller.kt` + `play-services-wearable`, `libadb-android`. So **`wearos/protocol` is live code for the phone app**: never delete or edit it as "dormant junk". The phone also bundles the watch APK unless `'-Pbitchord.bundleWear=false'`.
- **`:desktopUi`** -> `api(project(":shared"))`.
- **`:desktopApp`** -> `:shared`, `:desktopUi`, JavaFX 21, JavaCPP FFmpeg, ONNX Runtime; compiles `native/analyzer/*.cpp` through `desktopApp/native/CMakeLists.txt` (same sources as Android: **never delete or break `/native/analyzer`**).
- **`:shared`**: `jvmSharedMain` is shared by Android and desktop (`androidMain`/`jvmMain` both depend on it); `commonMain` by both. `PresencePlatform.Pc` and other desktop-only branches live there. Changing `:shared` APIs can break `:desktopApp`/`:desktopUi` without any warning in the active builds.
- **`:sharedUi`**: has a `jvm` target pinned to `org.jetbrains.compose.ui:ui-desktop:$CMP_DESKTOP` (`sharedUi/build.gradle.kts` lines 25, 89) purely for the desktop graph; `jvmSharedMain` shared with Android. Keep `CMP_DESKTOP`.
- **Root `build.gradle.kts` / `settings.gradle.kts`:** `settings.gradle.kts` includes `:desktopUi` and `:desktopApp` always and `:wearos` with the Android SDK. **Do not remove these lines.** Note: because `:desktopApp` is a Gradle project, **every Gradle build on a machine needs a JDK 21 toolchain installed** (desktop declares it), even if only phone is being built. Install Temurin 21 (`winget install EclipseAdoptium.Temurin.21.JDK`) alongside JDK 17.

## (c) History of changes in the dormant modules
- **Phase 1 (decoupling):** `DesktopPresence.kt` and `DesktopUpdateChecker.kt` made no-ops; `DesktopDiscordRpc.kt`/`DesktopDiscordSettings.kt` promo button off and project URL to the fork; `DesktopApp.kt` author links removed. Wear OS untouched.
- **Phase 2 (rebrand):** wear: only `ic_logo.xml` and `ic_notification.xml` drawables replaced; **applicationId, namespace, labels, strings still BitChord**. Desktop: `ic_logo.xml` drawable only.
- **Desktop rebrand (owner, commit `8ba30bf`, REBRAND_LOG s.6):** `packageName = "Violence"`, `bundleID = "com.yashaswahuh.violencemusic.desktop"`, `dockName`, `menuGroup = "Violence"`, window titles, tray/About/licence text, 51 string catalogs, download/cache/log/analysis folders, MPRIS identity, ListenBrainz client, icons (`AppIcon.ico/.png/.icns`), fresh Windows `upgradeUuid`.
- **Upstream sync v1.9 (2026-10-31):** upstream **deleted** the desktop app. We kept all desktop files byte-identical to before the merge. Note: the merge first silently dropped the `include(":desktopUi")`/`include(":desktopApp")` lines in `settings.gradle.kts`, the desktop jobs in `check.yml`, and the `/desktopApp/bin/` line in `.gitignore`; Phase 3 restored all three (see `RESTORATION_LOG.md`). Upstream also changed 37 `shared` and 70 `sharedUi` files after removing desktop, so **desktop has not been compiled against v1.9 shared code (UNCLEAR, may not compile)**. Wear OS: upstream changed nothing.
- **Phase 3:** no dormant file edited. Only CI/Gradle wiring outside them: desktop CI moved to `desktop-check.yml` (manual), `release.yml` tag trigger removed, `check.yml` Android job made explicit.

### Remaining BitChord branding in dormant code (record only; do not fix)
- **Desktop:** 122 code lines mention "BitChord" (mostly identifiers: `BitChordDesktopApp`, `BitChordIcons`, `com.music.bitchord.*` package, comments), plus native strings `L"BitChordSmtcWindow"` and window title `L"BitChord"` in `desktopApp/native/smtc/smtc_abi.cpp` and `smtc_jni.cpp` (Windows SMTC helper window), error text in `desktopApp/build.gradle.kts` lines 39 and 451. Packaging metadata and UI strings are already Violence.
- **Wear OS:** `applicationId` `com.music.bitchord` / `com.dev.bitchord` (must equal the phone's id to pair, but the phone is now `com.yashaswahuh.violencemusic` / `.dev`: **pairing would not work until aligned**), `namespace` `com.music.bitchord.wear`, `app_name` "BitChord"/"BitChord Dev", APK name `BitChordWear-*`, README, `app/src/main/res/values/wear.xml` and the phone manifest comments still say BitChord.

### The desktop `upgradeUuid` (never changes)
`desktopApp/build.gradle.kts` line 590, inside `nativeDistributions { windows { ... } }`:
`upgradeUuid = "ac94b47f-e765-4578-91a1-a5497a4c267c"`
This is the Windows MSI upgrade identity. **It must never change and must never revert to BitChord's value**; changing it makes Windows treat the next installer as a different product (side by side install, no upgrade). Same for `packageName = "Violence"`, `bundleID = "com.yashaswahuh.violencemusic.desktop"`, `dockName`, `menuGroup`.

## (d) UNFREEZE CHECKLIST
Only when the owner says "unfreeze wearos" or "unfreeze desktop".
**Both**
1. Say which target is being unfrozen and move its row to "Active" in `CLAUDE.md`/`AGENT_RULES.md`/`BUILD_COMMANDS.md`.
2. Install the toolchain: JDK 21 for desktop, Android SDK/NDK for wear.
3. Read "Possible impact from later changes" below and the upstream sync log; those lines are the likely breakages.
4. Re-enable CI for that target (add `push`/`pull_request` triggers back to `desktop-check.yml`, restore the job in `check.yml`; for release re-add the tag trigger to `release.yml` only if desktop releases are wanted).
5. Build with explicit tasks, fix breakage caused by shared-code changes (do it inside the unfrozen target, not by reverting active-target work), run its tests, test by hand.
6. Keep desktop branding and `upgradeUuid` exactly as above.
**Desktop**: `.\gradlew.bat :desktopApp:test :desktopUi:jvmTest` then `:desktopApp:run` and `:desktopApp:createDistributable`; check update checker/presence are still no-ops; reconcile with upstream's deletion (it will not receive upstream fixes: port by hand); finish the "BitChord" identifier leftovers if wanted; confirm the Windows installer upgrades an existing Violence install.
**Wear OS**: align `applicationId` (prod and dev) with the phone's `com.yashaswahuh.violencemusic` / `.dev` (the pairing rule in `wearos/build.gradle.kts` header), rebrand `app_name`, strings, APK prefix and namespace decisions, update `wear.xml` and manifest comments in `:app`, check `BundleWearApk` and `WearBridge`, remove the `'-Pbitchord.bundleWear=false'` workaround from the scripts, build `:wearos:assembleDevDebug`, pair with a watch.

## (e) Possible impact from later changes
Append one line per change: `date | file | what changed | why it could affect a dormant target`.

2026-10-31 | `shared/**`, `sharedUi/**` (37 + 70 files, upstream v1.9) | merged without desktop adaptation | desktopApp/desktopUi may not compile against the new shared/sharedUi APIs; not built.
2026-11-01 | `web/app/src/playback/livePresence.ts`, `settings.gradle.kts` (restored), CI files | web presence disabled; desktop includes restored; CI re-wired | none expected for dormant code; recorded for completeness.
2026-11-01 | `backend/` (Go module path, stats flag, origins, deleted `deploy/`), `ListenTogether.kt`/Jam UI in `:app`, web party code | Phase 4 Jam repoint: new `JAM_BACKEND_URL`, empty built-in default, wake-up retry | Desktop's own Jam client (`DesktopListenTogether.kt`) was not touched: it has no built-in server (reads the old `LISTEN_TOGETHER_SERVER` from `local.properties` at desktop build time, not `JAM_BACKEND_URL`) and has no wake-up UX. On unfreeze: add `JAM_BACKEND_URL` support and port the wake-up logic if wanted.
2026-11-02 | `shared/.../JamInvite.kt` (now owner's site, `/party/`+`/invite/`), `backend/` party ceiling 10 -> 15 and host handover | Phase 5 Jam work | Desktop's own Jam client (`DesktopListenTogether.kt`) still assumes the old caps in its UI (a party size stepper up to 10, if it has one: UNCLEAR, not read) and has no stalled-track isolation, no paste-a-link extraction and no `/party/` landing support; the shared `JamInvite` object is the only Jam code desktop shares. `ListenTogether.kt`'s new public members (`stalledTrack`, `MAX_PARTY_MEMBERS`) are Android-only. On unfreeze: port `extractCode`, the 15 ceiling and the unplayable-track rule.
2026-11-02 | `build.gradle.kts` (version still read from the same keys), `tv/build.gradle.kts`, `AppUpdateChecker.kt` (phone/TV only) | Phase 7: update check moved to `update.json`; `UPDATE_*` build values; TV reads the root version | `wearos` still reads the root version (unchanged keys); `DesktopUpdateChecker` is separate and still a no-op. No dormant file edited or built.
2026-11-01 | `shared/.../listentogether/PartyModels.kt` (`JoinRequest`, `PartyMember`, `PartyPreviewMember` gain trailing optional fields), new `shared/.../data/profile/**`, `sharedUi/.../ui/player/ListenTogetherMembersSheet.kt` (uses the new `ProfileAvatar`), new `sharedUi/.../ui/profile/ProfileAvatar.kt` | Phase 8 in-app profiles | The new fields have defaults, so `DesktopListenTogether.kt` (which builds `JoinRequest`/`PartyMember` positionally or by name) should still compile; the members sheet is shared with desktop and now calls `ProfileAvatar` (new in `sharedUi`, same source set, no new dependency). Not built. Desktop has no profile UI and will not send the new fields (an old-style client, which the server and the Android/web clients handle). On unfreeze: build it, then optionally add the profile screens.
2026-11-02 | `shared/.../data/share/**` (new), `ProfileRules.checkText` (new overload; `checkName` now calls it) | Phase 9 sharing | Additive. Desktop does not use the new package. `checkName` behaviour is unchanged (same limit, same rules), so the desktop profile-free build is unaffected. Not built.

## 2026-11-02: Part 2 bug fixes (shared code touched)

- `shared/.../data/share/PlaylistLinks.kt` (new), `IncomingLinks.kt` (new `IncomingLink.Playlist`): additive. Wear OS and desktop do not read share links, so neither is affected.
- `ui/components/backdrop/*` (Shadow and Highlight nodes now cache their recording) and `LiquidGlass.kt` live in `:app` only; `:tv` compiles them from `app/src/main/java` and its debug build passes.

## 2026-11-02: Part 3 (shared code touched)

- `sharedUi/.../ui/player/PlayerFrost.kt` (new `LocalGlassDisc`, default null) and `PlayerControls.kt` (`CircleGlyph` uses it when set): the Android app supplies Liquid Glass for the player's round buttons; desktop leaves it null and is unchanged. Desktop was not built or tested.

- Part 4: no further shared-code change (the player's buttons use the existing LocalGlassDisc hook, now supplied with a snapshot-backed glass inside the phone's player window). Desktop is not built or tested.
