# Restoration log

**Phase 3 (2026-11-01).** Goal: find anything that earlier cleanup (Phase 1 decoupling, Phase 2 rebrand, the v1.9 upstream sync) disabled, removed, or emptied that playback, search, stream resolution, lyrics, metadata, thumbnails, the YT Music and Spotify logins/import, downloads, Last.fm, the web extension, or Jam depend on.

Method: read `DECOUPLING_LOG.md`; diffed every code/config file changed between the fork's import commit (`a3944a7`) and the pre-sync `main` (`7ecabf0`) outside docs, strings and images (about 90 files); read the diffs of the risky ones (PlaybackService, DownloadStore, AppSettings, web Spotify/YouTube/extension files, TV search); checked the v1.9 merge for lines removed from non-desktop files.

## A. Phase 1 and Phase 2: nothing wrongly disabled
| Change | Depends on it? | Verdict |
|---|---|---|
| `Presence.kt` `ENDPOINT = ""`, `AndroidPresence.install()` no-op | Open-app counter only. Jam, playback, login do not use presence. | Correct (DISABLE). |
| `AppUpdateChecker.kt` `LATEST_RELEASE_URL = ""` | Update prompt only. | Correct (DISABLE); re-enabled against our own releases in Phase 8. |
| Discord promo button 2 default `false` (`AppSettings.kt`, `DiscordRPC.kt`, extension) | The promo button only; Discord Rich Presence itself still works. | Correct. |
| `SettingsSheet.kt`, `DesktopApp.kt` author links removed | Footer links only. | Correct. |
| `web/app/src/config.ts` `androidAppUrl` default `''` | Hides the "get the Android app" banner; nothing else. | Correct. |
| Donation/FUNDING/UPI files, `deploy-backend.yml`, `close-issue.yml` | None. | Correct. |
| Phase 2 renames (app/TV label, application ID, `violence://`, FileProvider authority, `violence_playback` channel, `Music/Violence` folder, user-agent / `submission_client` strings, web extension text) | Identity only. **Side effects to know, not bugs:** a new notification channel id (old channel settings are not carried over), downloads go to `Music/Violence` (older `Music/BitChord` files are not shown), a different app id installs next to BitChord. | Correct; no behavior lost. |
| Jam client/server | Untouched by Phase 1; Phase 2 only changed the invite scheme/origin text. | Working as originally configured. |
Verified in code on 2026-11-01: YT Music login (`auth/YtMusicLoginScreen.kt`, `AuthStore`, `Innertube.kt` SAPISIDHASH), Spotify login (`auth/SpotifyLoginScreen.kt`, `SpotifyToken.kt`), stream resolution, lyrics providers, Last.fm and ListenBrainz clients are unchanged apart from branding strings, and the phone, TV and web builds plus 1209 + 122 Android and 159 web tests pass.

## B. Regressions found and restored in Phase 3 (caused by the v1.9 upstream merge, not by Phase 1/2)
Upstream v1.9 deleted the desktop app. Git accepted the deletion of lines we had not edited, which silently removed desktop wiring (the sync log wrongly said "includes kept"). Restored to the exact pre-merge state:
1. `settings.gradle.kts`: `include(":desktopUi")` and `include(":desktopApp")` (the file is again identical to the pre-merge version). Without them `:desktopApp` was not a Gradle project and could not be revived.
2. `.github/workflows/check.yml`: the three desktop jobs (Linux, Windows, macOS) had been dropped. Restored from history as a **manual-only** workflow, `.github/workflows/desktop-check.yml`, so they no longer run automatically (Phase 3 requirement) but are not lost.
3. `.gitignore`: the `/desktopApp/bin/` line.
Consequence recorded in `FROZEN_TARGETS.md` and `BUILD_COMMANDS.md`: with desktop included, Gradle needs a **JDK 21 toolchain** for any build on a machine; JDK 21 was installed on the owner's PC for that.

## C. Left as is on purpose
- Web presence ping added by upstream v1.9 (`livePresence.ts`) stays DISABLED (new telemetry, not a dependency).
- Web build requires `VITE_PARTY_SERVER_URL` (upstream change); `web.yml` and the scripts set it to the pre-sync Jam server so web Jam still works.
