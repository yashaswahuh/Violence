# Author Decoupling & Deprecation Log

**Audit & Implementation Date:** 2026-10-30  
**Phase:** Phase 1 (Repository Hygiene & Author Decoupling)

---

## 1. Overview

In accordance with Phase 1 directives, all runtime connections to the original author's telemetry, open-app tracking, live status badges, personal update checking, donation/promotional links, and upstream deployment workflows have been decoupled or cleanly disabled without degrading core music search, streaming, playback, metadata, lyrics, Spotify import, or downloads.

---

## 2. Decoupling Action Register

| # | File Path | Original Behavior | Action Taken in Phase 1 | Later Phase Handling / Replacement |
|---|---|---|---|---|
| 1 | `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/presence/Presence.kt` | Transmitted periodic telemetry ping (`POST /api/presence`) every 5 minutes to `https://api.bitchord.kushagrasingh.in/api/presence` to count active app installations. | Set `ENDPOINT = ""` and inserted immediate `if (endpoint.isBlank()) return` checks in `init`, `send`, and `closeBlocking`. Zero network calls are dispatched. | **None required (Permanently Disabled).** Private fork for 4–6 personal users does not require open-app metrics. |
| 2 | `app/src/main/java/com/music/bitchord/data/presence/AndroidPresence.kt` | Registered `ProcessLifecycleOwner` observer on application start to toggle `Presence.setOpen(true/false)`. | Turned `fun install(app: Application)` into a clean no-op, eliminating lifecycle observation overhead and telemetry trigger. | **None required (Permanently Disabled).** |
| 3 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopPresence.kt` | Registered desktop window visibility and shutdown hooks to report open-app presence. | Turned `fun install()` into a clean no-op. | **Phase 3 (Pruning).** Desktop module will be pruned in Phase 3. |
| 4 | `app/src/main/java/com/music/bitchord/data/AppUpdateChecker.kt` | Polled `https://api.github.com/repos/kushagrasinghx/BitChord/releases/latest` at launch to notify user of upstream app releases and download APKs. | Set `LATEST_RELEASE_URL = ""` and inserted early return `if (LATEST_RELEASE_URL.isBlank()) return@withContext` in `check()`. `_available` StateFlow remains null. | **Phase 8.** Re-enabled pointing to fork's own GitHub releases (`https://api.github.com/repos/yashaswahuh/Violence/releases/latest`). |
| 5 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopUpdateChecker.kt` | Polled author's GitHub releases at launch for desktop builds. | Set `LATEST_RELEASE_URL = ""` and added early return `if (LATEST_RELEASE_URL.isBlank()) return@runCatching null`. | **Phase 3 (Pruning).** |
| 6 | `web/app/src/config.ts` | Defaulted `androidAppUrl` to `https://github.com/kushagrasinghx/BitChord/releases/latest`. | Changed fallback default to empty string `''`. | **Phase 6.** Web player configuration will wire release links to fork's GitHub Pages/Releases. |
| 7 | `web/app/.env.example` | Provided author's release URL as example `VITE_ANDROID_APP_URL`. | Emptied value (`VITE_ANDROID_APP_URL=`). | **Phase 6.** |
| 8 | `web/app/src/shell/AppShell.tsx` | Rendered banner with link to download author's APK on mobile browsers. | Made the `Get the App` action conditional on non-empty `config.androidAppUrl`. | **Phase 6.** |
| 9 | `web/app/src/shell/Sidebar.tsx` | Rendered sidebar navigation link to author's APK download. | Made `Get the Android app` link conditional on non-empty `config.androidAppUrl`. | **Phase 6.** |
| 10 | `app/src/main/java/com/music/bitchord/data/discord/DiscordRPC.kt` | Shipped default promotional button 2 "Visit BitChord" pointing to `https://github.com/kushagrasinghx/BitChord`. | Updated `PROJECT_URL` to point to fork repo (`https://github.com/yashaswahuh/Violence`) and set default `button2Visible = false`. | **Phase 2.** Rebranding will rename display strings. |
| 11 | `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt` | Defaulted `discordButton2Visible` setting to `true`. | Changed default preference value to `false`. | **None.** |
| 12 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopDiscordSettings.kt` & `DesktopDiscordRpc.kt` | Enabled promotional button 2 linking to author repo by default. | Changed default `_button2Visible` to `false` and updated `PROJECT_URL` to fork repository. | **Phase 3 (Pruning).** |
| 13 | `web/app/src/extension/useDiscord.ts` & `web/extension/lib/discord.ts` | Web companion extension defaulted Discord RPC button 2 to visible with author's GitHub repo URL. | Defaulted `button2Visible` to `false` and updated `PROJECT_URL` to fork repository. | **Phase 6.** |
| 14 | `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt` | Rendered 4 links to author's GitHub, Developer profile, Discord server, and website in Settings footer. | Removed all 4 links, preserving clean `bitchord $version` text. | **Phase 2.** App name will be rebranded to "Violence". |
| 15 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt` | Rendered author GitHub, Developer, and Discord links in Desktop settings footer. | Removed promotional links; preserved `Licenses` dialog link. | **Phase 3 (Pruning).** |
| 16 | `README.md` | Displayed live listener badge (`api.bitchord.kushagrasingh.in/api/stats/live/badge.svg`), Trendshift badges, FMHY badge, and financial support section (Ko-fi, PayPal, UPI). | Stripped live badge, promotion badges, Support header, and donation links. | **Phase 2.** README will be updated with fork identity and usage documentation. |
| 17 | `upi_support.jpg` | Binary image of author's UPI QR code. | File deleted from repository. | **None (Permanently Deleted).** |
| 18 | `.github/FUNDING.yml` | Configured GitHub Sponsor / Ko-fi funding button pointing to author's account (`kushagrasinghx`). | File deleted from repository. | **None (Permanently Deleted).** |
| 19 | `.github/workflows/deploy-backend.yml` | GitHub Actions workflow deploying to author's Oracle Cloud VM and testing `api.bitchord.kushagrasingh.in/healthz`. | File deleted from repository. | **Phase 4.** Self-hosted backend deployment workflow if needed. |
| 20 | `.github/workflows/close-issue.yml` | GitHub Actions workflow auto-closing issues with `/close` command restricted to upstream logins. | File deleted from repository. | **None (Permanently Deleted).** |
| 21 | `.gitignore` | Did not explicitly ignore `google-services.json`, `.env`, or service account keys. | Hardened `.gitignore` to ignore `google-services.json`, `*/google-services.json`, `.env`, `.env.*`, `*service-account*.json`, and `*service_account*.json`. | **None.** Continuous repo hygiene. |

