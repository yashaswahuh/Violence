# TO-CHANGE: everything that can still say BitChord

A list of the places the app, web player, extension, backend and tooling still use a BitChord
name, address, key or token, and could be switched to Violence. Written from a search of the
repository (case-insensitive `bitchord`, plus `kushagrasingh`, `onrender.com`, API-key names and
app ids) on the `violence-nk` branch.

Jam is already on Violence's own servers, so the Jam server address is not listed as a to-do.
The old `bitchord://party` link is still accepted on purpose, so links people shared keep working.

**How to read the risk column**

- **Safe**: only text or a name nobody depends on. Change it any time.
- **Data**: it names a file or storage key that already holds users' data. Changing it needs a
  migration (read the old name, copy to the new one), or people lose that data.
- **Protocol**: another program reads it, so both sides must change together.
- **Legal**: attribution the GPL-3.0 licence expects. Do not remove it. Keep the credit to the
  original author.

---

## 1. External services, keys and tokens

| What | Where | Today | Notes | Risk |
|---|---|---|---|---|
| Last.fm API key and secret | `LASTFM_API_KEY` in `local.properties` or the environment; `LASTFM_SECRET` in CI (`app/build.gradle.kts`, `.github/workflows/*.yml`, desktop `DesktopScrobbling.kt`) | Empty unless you set them | Register your own Last.fm API account for Violence and put the key in the repo secrets. Without it scrobbling shows "not configured" | Safe |
| Desktop scrobbling env names | `BITCHORD_LASTFM_API_KEY`, `BITCHORD_LASTFM_API_SECRET`, `BITCHORD_LASTFM_SESSION_KEY`, `BITCHORD_LISTENBRAINZ_TOKEN` (`DesktopScrobbling.kt`) | BitChord-prefixed names | The code already also reads `violence.lastfm.key` and `violence.lastfm.secret`. Add `VIOLENCE_*` env names and keep the old ones as a fallback | Safe |
| Desktop YouTube cookie env | `BITCHORD_YTMUSIC_COOKIE` (`DesktopYouTubeAuth.kt`) | BitChord-prefixed | Rename to `VIOLENCE_YTMUSIC_COOKIE`, keep the old as a fallback | Safe |
| In-app update address | `UPDATE_INFO_URL` in `local.properties` (`app/build.gradle.kts`) | Empty, so the updater is off | Host `update.json` (see `fork-docs/RELEASE_GUIDE.md`) and set this. Nothing points at BitChord | Safe |
| Lyrics User-Agent | `LYRICS_AGENT` in `LyricsHttp.kt`; `AGENT` in `LrcLib.kt` | `Violence (https://github.com/bitchord)` | Points at a GitHub name that is not yours. Use `https://github.com/yashaswahuh/Violence`. LRCLIB and others ask for a contact URL | Safe |
| Original project links | `SettingsSheet.kt`, `TvDialogs.kt`, `web/app/src/shell/Sidebar.tsx`, `desktopApp/packaging/windows/installer.iss`, `README.md`, `NOTICE` | `github.com/kushagrasinghx/BitChord` | Credit links to the original author. Keep these. The Windows installer's `AppPublisherURL`, `AppSupportURL` and `AppUpdatesURL` should point at your repo instead, since they are where Violence users get help and updates | Legal / Safe |
| Old Jam site address in tests | `JamInviteLinkTest.kt`, `JamInviteTest.kt` (`bitchord.kushagrasingh.in`) | Old BitChord address | Only test data for "is this link rejected". Update the examples to your own domain | Safe |
| Jam probe default server | `backend/tools/jam_probe.py` line 23 (`bitchord-67v8.onrender.com`) | BitChord's Render service | A dev tool. Point the default at the Violence Jam server | Safe |
| Jam server health name | `"service":"bitchord-listen-together"` (backend response, checked in `JamInviteLinkTest.kt`) | BitChord name | The app checks this value to recognise a Jam server. Rename on the server and in the app's check in the same release | Protocol |
| Jam WebSocket sub-protocol | `bitchord.v1`, `bitchord.token.<token>` (`backend/README.md`, backend Go code, app client) | BitChord name | Both sides must change at once, and old app versions would stop connecting | Protocol |
| Extension site origins | `BITCHORD_SITE_ORIGINS`, `BITCHORD_STORE_BUILD` (`web/extension/site.config.ts`, `wxt.config.ts`, `.github/workflows/web.yml`) | BitChord-prefixed build variables | Rename to `VIOLENCE_*` and update the workflow at the same time | Safe |
| Backend test variables | `BITCHORD_TEST_INT`, `BITCHORD_TEST_CSV`, `BITCHORD_TEST_BOOL` (`backend/config/config_test.go`) | BitChord-prefixed | Test-only names | Safe |
| Backend platform comment | `backend/presence/presence.go` ("platforms BitChord ships on") | Comment | Text only | Safe |

## 2. App identity (Android)

| What | Where | Today | Notes | Risk |
|---|---|---|---|---|
| Phone app id | `app/build.gradle.kts` | `com.yashaswahuh.violencemusic` (and `.dev`) | Already Violence's. Nothing to do | none |
| Dev install check | `app/build.gradle.kts` and `tv/build.gradle.kts` (`verifyDevInstall`) | Compiles `com.dev.bitchord` | A leftover package name in a helper task. Should be the `.dev` Violence app id | Safe |
| Watch app protocol note | `wearos/protocol/.../WearProtocol.kt` | Mentions `com.dev.bitchord` | Comment. Also check the wear module's own `applicationId` matches the phone's, since the watch pairs by it | Protocol |
| Certificate name | `WatchInstaller.kt` (`CN=BitChord`) | BitChord name on the generated signing certificate | Only a label inside a certificate the app makes for installing the watch app. Changing it makes a new certificate, so the watch app may need re-installing | Protocol |

## 3. Storage names (changing any of these needs a migration)

These are file names on the phone. They work fine, nobody sees them, and renaming them without
copying the old data across wipes people's settings, playlists and logins. If you want them
renamed, do it in one release with a one-time copy from the old name to the new one.

| Name | File | Holds |
|---|---|---|
| `bitchord_settings` | `AppSettings.kt`, `StyleSettings.kt`, `SearchHistory.kt`, `Downloads.kt`, `TvSettings.kt` | All settings, Violence Style, search history |
| `bitchord_auth`, `bitchord_auth_plain` | `AuthStore.kt` | Signed-in accounts |
| `bitchord_sources`, `bitchord_sources_plain` | `SourceRegistry.kt` | Playback sources |
| `bitchord_local_playlists` | `LocalPlaylistStore.kt` | Playlists kept on the phone |
| `bitchord_offline_playlists` | `OfflinePlaylists.kt` | Offline playlists |
| `bitchord_playlist_sync` | `PlaylistSync.kt` | Playlist sync state |
| `bitchord_listen_together` | `ListenTogether.kt` | Jam identity and last server |
| `bitchord_party_queue_stash` | `PartyPersonalQueueStash.kt` | A queue saved while in a Jam |
| `bitchord_bounded_context` | `BoundedContextPersistence.kt` | The saved queue |
| `bitchord_last_played` | `LastPlayed.kt` | Last played song |
| `bitchord_original_versions` | `OriginalVersion.kt` | Chosen song versions |
| `bitchord_widget` | `MediaWidgetSnapshot.kt` | Home-screen widget state |
| `bitchord_auth.xml` and others | `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml` | Which files Android backs up or excludes. These must keep matching the names above |
| Desktop prefs node `com.music.bitchord.desktop*` | `DesktopPersistence.kt`, `DesktopPresence.kt`, `DesktopListeningStats.kt` | Desktop settings, presence, stats |
| Backup tag `APP_TAG = "bitchord"` | `Backup.kt` | Written inside backup files so restore recognises them. The restore code must accept both the old and new tag |
| Web storage keys `bitchord:*` | `web/app/src/**` (`bitchord:profile`, `:friends`, `:player`, `:lyrics`, `:history`, `:discord`, `:spotify`, `:apple`, `:violence-style`, `:presence-id`, `:video-quality`, `:install-banner-dismissed`, `:debug:clients`) | Browser storage for the web player | Same rule: copy old keys to new ones on first load |

## 4. Files written to the phone or into music files

| What | Where | Today | Notes | Risk |
|---|---|---|---|---|
| Downloads folder | `Music/BitChord` in `LocalMediaRepository.kt`, `AppSettings.kt` doc, `SettingsSheet.kt` (the export subtitle) | Old folder name | The Storage tools (`Open downloads folder`, breakdown, lyrics repair) already use `Music/Violence`, so the two now disagree. Pick one and make everything use it, with a one-time move of the old folder | Data |
| Lyrics tag in audio files | `BITCHORD_LYRICS` (`LrcWriter.kt`, `EmbeddedLyrics.kt`, desktop `DesktopEmbeddedLyrics.kt`) | Tag name written into users' songs | Songs already tagged keep the old name. Read both, write the new one | Data |
| MP4 tag namespace | `Mp4Tagger.kt` (`MEAN = "com.music.bitchord"`) | Old namespace | Same as above: read both, write the new one | Data |
| Backup file name | `Backup.kt` | Now `violence-backup-DATE` | Done. Restore should still accept `bitchord-backup-*` | none |

## 5. Web player, extension and browser pieces

| What | Where | Notes | Risk |
|---|---|---|---|
| Package names `@bitchord/shared`, `@bitchord/app`, `@bitchord/extension`, `bitchord-web` | `web/package.json`, `web/shared/package.json`, `web/extension/package.json`, every `import ... from '@bitchord/shared/bridge'`, `pnpm-lock.yaml` | Workspace names only. Renaming means touching every import and regenerating the lockfile | Safe |
| Bridge channel | `violence-bridge` is already set; check the extension and web app agree after any rename | | Protocol |
| Extension old-zip cleanup | `web/extension/scripts/publish-to-site.mjs` matches `(violence\|bitchord)-extension-*.zip` | Fine to keep while old zips may exist | Safe |
| Fonts and CSS names | `'BitChord SF Pro Display'` in `web/app/src/styles/global.css` and `LyricsPanel.tsx`; `--bitchord-lyric-size` in `LyricsPanel.tsx` | Internal font and variable names | Safe |
| Presence text | `livePresence.ts`, `protocol.ts` ("Tells the BitChord server", "Replaces BitChord after Listening to") | Wording, and one message that shows in Discord | Safe |
| Comments and docs | Many files in `web/docs`, `docs`, `fork-docs`, `web/app/src/playback/automix/NOTICE.md` | Prose only. The automix `NOTICE.md` is a licence notice, so keep its attribution | Safe / Legal |
| Native code comments | `native/analyzer/*`, `native/loopmix/*`, `app/src/main/cpp/jni/*` | Mostly licence headers crediting Kushagra Singh. Keep them | Legal |
| Source file headers | `shared/.../playback/smart/*.kt` ("Copyright (C) 2026 Kushagra Singh (BitChord adaptation)") | Required credit for adapted code. Keep them | Legal |

## 6. Still to do for a clean rebrand (summary)

1. Point `LYRICS_AGENT` / `AGENT` at the Violence repo.
2. Fix the `Music/BitChord` vs `Music/Violence` mismatch (one folder, with a move).
3. Replace `com.dev.bitchord` in the two `verifyDevInstall` tasks.
4. Register your own Last.fm API key and set `UPDATE_INFO_URL`.
5. Point the Windows installer URLs at your repo.
6. Rename the `BITCHORD_*` build and env variables to `VIOLENCE_*` (keep the old as a fallback).
7. Decide whether to migrate the storage names in section 3. Not needed for users to see anything.
8. Rename the Jam service name and sub-protocol together with the server, if you want them gone.

Keep: every credit to Kushagra Singh and BitChord (README, NOTICE, the About page, licence headers).
The GPL-3.0 expects it, and the app's Credits page already lists it.
