# Upstream sync playbook (BitChord -> Violence)

Upstream: `https://github.com/kushagrasinghx/BitChord.git` (remote `upstream`, push `DISABLED`). Work only on `violence-nk`.

## How a sync is done
1. `git branch --show-current` must print `violence-nk`; tree clean; tag a backup `pre-upstream-<ver>-<date>` and push it.
2. `git fetch upstream --tags`; find the official tag on `kushagrasinghx/BitChord`.
3. `git merge-base HEAD <tag>` must print a commit. Since the sync of v1.9 it does (see below). If it ever prints nothing, STOP.
4. `git merge --no-ff --no-commit <tag>`, resolve with the rules in `UPSTREAM_SYNC_LOG.md`, build with explicit module tasks, then commit and push.
5. Update `UPSTREAM_SYNC_LOG.md` and the "Upstream syncs" table in `PHASES.md`.

## History bridge (one-time, done 2026-10-31)
This repo was created from a copy of BitChord's source (root commits `f27b7dc`, `a3944a7`), so it shared no history with upstream. A single `git merge -s ours --allow-unrelated-histories edc1e6f` recorded upstream commit `edc1e6f` (the tree the fork was copied from; only `.gitattributes`, `LICENSE`, `gradlew`, `appimage.sh` differed) as an ancestor without changing any file. Future syncs have a normal merge base. Do not repeat the bridge.

## Files we customize: watch on every sync
Keep OURS, re-apply on top of upstream if overwritten:
- **Identity:** `app/build.gradle.kts` and `tv/build.gradle.kts` (`applicationId`, dev flavor IDs, `app_name`, `UPDATE_APK_PREFIX`, version name `X.Y.Z-violence.N`), both `AndroidManifest.xml` (labels, `violence` scheme, hosts, FileProvider, activity-aliases), `strings.xml` in all `app` locales and `sharedUi/.../composeResources` (every "BitChord" in text must read "Violence"), `README.md` (fork section, no badges, no donation), `NOTICE`, `MAINTAINERS.md`, `LICENSE` text.
- **Icons:** `app/src/main/res/mipmap-*/ic_*` and `drawable/ic_launcher_*`, `ic_notification_logo`, `tv/.../tv_banner.png`, web favicon/logos, `web/extension/public/icon/*`, root `Logo.png`/`Banner.png`. Upstream's own icons (Neon, Monochrome) arrive as new aliases; they are BitChord artwork (see sync log).
- **Disabled author services:** `shared/.../presence/Presence.kt` (`ENDPOINT = ""`), `AndroidPresence.kt`, `AppUpdateChecker.kt` (`LATEST_RELEASE_URL`), `web/app/src/playback/livePresence.ts` (`PRESENCE_ENABLED = false`), Discord promo button defaults, `SettingsSheet.kt` colophon (fork attribution block, no author links), `web/app/src/config.ts` (`androidAppUrl` empty).
- **Web:** `web/app/vite.config.ts` (`base` = `/Violence/`, guard needing `VITE_PARTY_SERVER_URL`), `web/app/src/config.ts`, `.github/workflows/web.yml` (Pages deploy, `VITE_PARTY_SERVER_URL` from repo variable `JAM_SERVER_URL`, default = the current Jam server), `web/extension/site.config.ts`, `wxt.config.ts`.
- **Jam (Phase 4):** `JAM_BACKEND_URL` handling and empty default in `app/build.gradle.kts`/`tv/build.gradle.kts`; `ListenTogether.kt` (`defaultServer` tolerant of empty/invalid, `wakeServerIfAsleep`, `serverWaking`, friendlier `no_server` text) and `ListenTogetherSheets.kt`/`ListenTogetherScreen.kt` (waking and not-set-up notices); the 2 strings `listen_together_server_waking` and `listen_together_not_configured` in all 20 `strings.xml`; web `party/listenTogether.ts` (`serverWaking`, `requireServer`, `wakeServerIfAsleep`), `pages/ListenTogetherPage.tsx`, `vite.config.ts` (warn, not fail, without a party server); `JamInviteLink.kt`, `shared/.../JamInvite.kt`.
- **Jam, Phase 5:** `JamInviteLink.kt` (rewritten: base-configurable links, `extractCode`, any-host https), shared `JamInvite.kt` (owner's site, `/party/`), `PartySync.kt` (`onTrackUnplayable`, `unplayableVideoId`) and its call in `PlaybackService.recoverFrom` (`givingUp` branch), `ListenTogether.kt` (`stalledTrack`, `MAX_PARTY_MEMBERS`, default party size 8), `ListenTogetherScreen.kt`/`ListenTogetherSheets.kt` (share/copy code, paste-friendly join, stalled notice, 15 cap), `app`/`tv` `AndroidManifest.xml` invite filters (TV is `violence`/owner host), `TvActivity.onCreate` consume, `tv/.../dialogs/TvListenTogetherDialog.kt` and its rows in `TvSettingsScreen`/`TvApp`, `JAM_INVITE_BASE_URL` in both gradle files; web `party/invite.ts`, `party/partySync.ts` (stalled, blocked audio, visibility), `party/listenTogether.ts` (`resyncNow`, new state fields), `pages/PartyLandingPage.tsx`, `pageRegistry.tsx` (`/party/:code`), `shell/AppShell.tsx` banners, `state/player.ts` (`onPlayBlocked`).
- **Web hosting, Phase 6:** `web/app/public/404.html` (generic deep-link redirect, keeps `/party/`), `web/app/src/playback/automix/analysis/models.ts` (model URLs under `import.meta.env.BASE_URL`: upstream used a root-absolute `/models/...`, a 404 on Pages), `web/extension/site.config.ts` origins, `.github/workflows/web.yml` and `android.yml` (rewritten), `scripts/serve-pages-local.mjs`.
- **Release and updates, Phase 7:** `AppUpdateChecker.kt` (rewritten: update.json, not GitHub releases), `TvUpdateDialog.kt`, `MainViewModel` (the single `check(getApplication())` call), `app/build.gradle.kts` and `tv/build.gradle.kts` (`UPDATE_INFO_URL`, `UPDATE_APK_KEY`; TV reads the root version and no longer falls back to debug signing), both manifests' single `autoVerify` invite filter (prefixes, not path patterns), `TvListenTogetherDialog.kt` (buttons never disabled), `keystore.properties.example`, `scripts/release.ps1`, `web.yml` (update step + release trigger). Upstream releases through GitHub: do not take their `UPDATE_APK_PREFIX` / GitHub API code back.
- **Backend (Phase 4):** Go module path is now `github.com/yashaswahuh/Violence/backend` (all imports); `config.go` (`StatsEnabled`/`JAM_ENABLE_STATS` default off, default `JAM_ALLOWED_ORIGINS`, `MaxPartyMembers` = 15 used by `protocol` and `party`), `party/party.go` (`Remove` hands the host to the oldest connected member), `main.go` (presence/stats routes only registered when `StatsEnabled`, drift log line uses the member id, invite page says Violence and has no author link), `render.yaml`, new `Dockerfile` and `.dockerignore`. **`backend/deploy/` (author's Oracle/Caddy kit) was deleted**: upstream edits to it will show as modify/delete conflicts; keep it deleted.
- **Workflows:** `.github/workflows/android.yml`, `web.yml`, `tv.yml`, `check.yml`, `release.yml` (upstream deleted `release.yml`; we keep it because desktop is kept), `desktop-check.yml` (ours). Phase 4: `android.yml`/`check.yml`/`tv.yml` write `JAM_BACKEND_URL` from the repo variable into `local.properties`; `web.yml` maps it to `VITE_PARTY_SERVER_URL`.
- **Desktop (dormant, must survive):** all of `desktopApp/` and `desktopUi/`, `DESKTOP.md`, `docs/DESKTOP_RELEASES.md`, `settings.gradle.kts` includes. Upstream DELETED the desktop app in v1.9; we keep it. Never let a merge delete it. `desktopApp/build.gradle.kts`: `packageName="Violence"`, `bundleID`, `dockName`, `menuGroup`, and `upgradeUuid = "ac94b47f-e765-4578-91a1-a5497a4c267c"`.
- **Docs and rules:** `CLAUDE.md`, everything in `fork-docs/`, `.gitignore` additions, `.claude/` is removed upstream (we follow).
- **Deliberately deleted by us (stay deleted):** `upi_support.jpg`, `.github/FUNDING.yml`, `.github/workflows/deploy-backend.yml`, `.github/workflows/close-issue.yml`.

## After every merge, re-check
`grep -r "BitChord"` in added user-visible text; new hosts in added lines; new presence/analytics/update/donation code; `Presence.kt` and `AppUpdateChecker.kt` still blank; exactly one `android:enabled="true"` launcher alias; Jam protocol files (`backend/protocol|party|hub`, `PartyModels.kt`, `ListenTogether.kt`, `web/app/src/party/*`) changed or not; build phone, TV, web with explicit tasks.

## Violence Style touch points (stage S1, 2026-11-03)
New code lives in `app/src/main/java/com/music/bitchord/ui/violencestyle/` and `app/src/test/.../violencestyle/`, strings in `app/src/main/res/values/strings_violence_style.xml`, fonts `app/src/main/res/font/vs_*.ttf`. Upstream files edited (re-apply after a sync if overwritten):
- `app/src/main/java/com/music/bitchord/MainActivity.kt`: 4 import lines; `ViolenceStyleBoot.start(this)` and `val vsEnabled` before/in `setContent`; `darkTheme` is `ViolenceStyleSettings.forceDark(vsEnabled) ?: when (theme) {...}`; `BitChordTheme(...)` replaced by `ViolenceStyledTheme(...)`; one `SideEffect { ViolenceStyleSong.report(...) }` after `rememberPlayerState`.
- `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt`: one `row(...) { ViolenceStyleSettingsBlock() }` at the top of the `ADVANCED` group.
- `NOTICE`: the font licence section. `.gitignore`: `/fork-docs/style-reference/`.
Later stages add more touch points here (see `fork-docs/style/STYLE_MAPPING.md`).

### S2 additions (2026-11-03)
- `app/src/main/java/com/music/bitchord/MainActivity.kt`: 5 more import lines; in `setContent`, `violenceBackdrop = LocalViolenceBackground.current` and `violenceBackdrop?.drawInto(this)` inside `paintBackdrop`; `ViolenceBackground()` before `BitChordApp(...)` in the root box; in `BitChordApp` the page root `Box` uses `.violenceBackground(LocalViolenceStyle.current, MaterialTheme.colorScheme.background)` instead of `.background(MaterialTheme.colorScheme.background)`; the SideEffect passes `showNowPlaying`.
- `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/SearchScreen.kt` (shared with desktop): the `TopResultCard` background is wrapped in `if (LocalViolenceSeeThrough.current)`, plus one import. New shared file `sharedUi/.../ui/violencestyle/StyleHooks.kt`.

### S3 additions (2026-11-03)
All edits are early branches on `LocalViolenceShared.current` (null when the style is off) or swapped expressions; re-apply after a sync if overwritten.
- `sharedUi/.../ui/screens/HomeScreen.kt` (shared with desktop): 6 imports; `SectionHeader` gains a trailing `hero: Boolean = false` parameter and a styled early return; `RecentSectionHeader` early return; `CompactTrackRow` (`artShape`, subtitle colour, `RowMoreButton` min size); `HeroCard` (shape, rim, title style); `import ...layout.defaultMinSize`.
- `sharedUi/.../ui/screens/TopPicksShelf.kt` (shared): 5 imports; `SectionHeader(..., hero = true)`; `TopPickCard` gains `first: Boolean = false` (call site passes `index == 0`), shape, `.violenceCardRim`, the scrim box; `CardCaption` styled early return; the skeleton's shape.
- `app/.../ui/components/FrostedTopBar.kt`: 2 imports; in `artworkPageSurface`, `liquidGlass(shape, surfaceTint = violenceGlassTint())` and `.violenceGlassEdge(shape, GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR)` instead of the plain `.border(...)`.
- New: `sharedUi/.../ui/violencestyle/StyledComponents.kt`, `app/.../ui/violencestyle/StyleChrome.kt`; `StyleHost.kt` provides `LocalViolenceShared`.

### S4 additions (2026-11-03)
Each is a swapped expression or an appended modifier; re-apply after a sync if overwritten.
- `app/.../ui/components/GlassNavBar.kt`: 4 imports; `glassSurface` passes `surfaceTint = violenceGlassTint()` and appends `.violenceGlassRim(pillShape)`; `indicatorColor = violenceIndicatorColor() ?: glassIndicatorColor()`.
- `app/.../ui/components/floatingtabbar/FloatingTabBar.kt` (vendored): 2 imports; `.violenceCapsuleGlow()` appended to the two selection-pill modifier chains; `.violenceTabSemantics(isSelected)` after the expanded tab's `clickable`.
- `app/.../ui/components/FloatingBottomBar.kt`: imports; `liquidGlass(shape = pillShape, surfaceTint = violenceGlassTint())`; the plain `.border(...)` replaced by `.violenceGlassEdge(...)`; the selected indicator's fill `violenceIndicatorColor() ?: <old>` plus `.violenceCapsuleGlow()`; `.violenceTabSemantics(selected)` on `BottomBarItem`.
- `app/.../ui/components/MiniPlayer.kt`: `.border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)` replaced by `.violenceGlassEdge(shape, 0.5.dp, Color.White.copy(alpha = 0.10f))`.
- `app/.../ui/components/IosSwitch.kt`: `violenceSwitchTrack()` first in the track colour `when`.
- `sharedUi/.../ui/components/PillSegmentedControl.kt` (shared with desktop): `pillColor` branch on `LocalViolenceShared`.
- `sharedUi/.../ui/player/ThinSlider.kt` (shared): `activeColor` default reads `LocalViolenceShared`.

### S5 additions (2026-11-03)
- `app/.../ui/components/AppGlass.kt`: 4 imports; in `rememberPanelGlass`, `val tint = violencePanelTint() ?: panelTint(dark)` and the tint alpha target uses `violencePanelSolid()`; `GlassSurface` appends `.violenceGlassRim(shape)` on both branches.
- `app/.../ui/components/GlassPanels.kt`: 4 imports; the dialog's button colour is `violenceButtonColor() ?: MaterialTheme.colorScheme.primary`.
- `app/.../ui/screens/DetailScreen.kt`: 1 import; `.violenceDetailGlow()` after `.background(pageColor)` on the detail page root.
- `sharedUi/.../ui/player/PlayerControls.kt` (shared with desktop): `TransportGlyph` gains `accentRing: Boolean = false` (the play call passes `true`), reads `LocalViolenceShared`, and draws the ring with `drawBehind`; one import.
- `app/src/main/res/values/strings_violence_style.xml`: the `vs_detail_glow_subtitle` text.

## Violence Style touch points: final list, conflict rules, re-check checklist (S6)

**Files edited (see the S1 to S5 additions above for the exact lines):** `MainActivity.kt`, `SettingsSheet.kt`, `DetailScreen.kt`, `FrostedTopBar.kt`, `GlassNavBar.kt`, `FloatingBottomBar.kt`, `floatingtabbar/FloatingTabBar.kt`, `MiniPlayer.kt`, `IosSwitch.kt`, `AppGlass.kt`, `GlassPanels.kt` (phone); `HomeScreen.kt`, `TopPicksShelf.kt`, `SearchScreen.kt`, `Common.kt`, `PillSegmentedControl.kt`, `ThinSlider.kt`, `PlayerControls.kt` (shared). New files live only under `.../ui/violencestyle/`.

**Resolving a conflict.** Keep the upstream change and re-apply the skin's line on top: every edit is one early branch or one swapped expression (grep the file for `violence` or `Violence`). Never resolve by dropping the guard: the else branch must stay the upstream code. If upstream renames a function the skin wraps (`SectionHeader`, `CompactTrackRow`, `TopPickCard`, `artworkPageSurface`, `GlassSurface`, `paintBackdrop`), move the call to the new name. If upstream replaces the vendored `floatingtabbar/`, re-add the three lines (two `.violenceCapsuleGlow()`, one `.violenceTabSemantics(isSelected)`). `app/.../ui/violencestyle/**` and the font and string files are ours and never conflict.

**Re-verify after every sync (10 minutes):**
1. `grep -rn "violence\|Violence" ` the files above: every touch point still present; `ViolenceStyledTheme` still replaces `BitChordTheme` in `setContent`.
2. Build phone, TV and web with the scripts; run the phone tests (`StyleOffParityTest` and friends must pass).
3. Style OFF: Home, nav bar, a dialog look as upstream's (no rim, no serif, no glow).
4. Style ON: Home headings, first hero card rim, nav capsule with glow line, Search rows without black bars, a dialog, the Settings block.
5. Toggle ON and OFF while music plays: playback continues.
6. Check `FROZEN_TARGETS.md` for new shared edits, and that the dormant modules and `settings.gradle.kts` includes survived.

### Follow-up additions (2026-11-03)
- `sharedUi/.../ui/player/NowPlayingScreen.kt` (shared with desktop): two imports; `FullArtworkBlurBackdrop(modifier = backdropModifier.violencePlayerGlow())` and the title's `style` reads `LocalViolenceShared.current?.displayCaps?.copy(...)` with the original style as the fallback.
- `app/.../ui/screens/SettingsSheet.kt`: `SettingsGroup` card `.background(violenceGroupFill() ?: <surfaceVariant>)` and `.violenceGlassRim(GroupShape)`.

### Follow-up: account menu and Replay cards (2026-11-03)
- `app/.../ui/components/AccountProfileSelector.kt`: style on: lighter wash plus accent tint, rim, opens on the left (82 percent width), lighter scrim; style off: the original code path.
- `sharedUi/.../ui/replay/ReplayCard.kt` (`ReplayCreditCard`): style on: mesh colours from the app accent (`violenceReplayColors`) instead of the cover; off: the cover palette as before.
- `AccountProfileSelector.kt`: also reads `ViolenceStyleSettings.accountLiquidGlass` and, when on with the style, draws via `accountLiquidGlass()` (private, same `drawBackdrop` pipeline as `IosMenu.kt` `menuGlass`).
