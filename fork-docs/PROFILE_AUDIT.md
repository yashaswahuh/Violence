# Identity audit (Phase 8, written before any profile code)

Written 2026-11-01 on branch `violence-nk`. Everything below was read in the code; nothing was run on a device. Items I could not confirm are marked **UNCLEAR**.

**Question:** where does the app show or use "who the user is" today, what is stored where, and does an in-app profile concept already exist?

## 1. Short answer
- **Identity today = the Google / YT Music sign-in.** There is no Violence identity. A user who is not signed in to YT Music has no name, no avatar and, in Jam, no way to join.
- **One profile-like concept already exists, and it is a different thing:** the *YouTube profile / channel switcher* (`GoogleAccountSession.profiles`, `YouTubeProfile`, phone `AccountProfileSelector.kt`, TV `TvProfileSwitcher` in `TvDialogs.kt`) with an optional **4-digit PIN per YouTube profile on TV** (`TvSettings`, `TvPinThrottle`). It chooses *which YouTube Music channel's library and cookies are used for playback*. It is keyed by the Google account and channel ids.
- **Decision (see `PROFILE_DESIGN.md`): do not extend that concept, build a separate one.** Extending it would tie the new identity to a Google account, the exact thing the owner wants to avoid. The YouTube switcher and the TV PIN stay untouched (KEEP). The new `InAppProfile` lives next to them, and Jam prefers it over the YouTube identity when it exists.

## 2. Where identity is stored
| What | Where | Notes |
|---|---|---|
| YT Music cookie jar, Google sessions (`accountId`, name, email, profiles with name + avatar URL, active ids) | `AuthStore` (`auth/AuthStore.kt`): `EncryptedSharedPreferences` `bitchord_auth`, plain fallback `bitchord_auth_plain` | Excluded from Android backup / device transfer (`backup_rules.xml`, `data_extraction_rules.xml`). Cookies are secrets: never logged, never sent anywhere but Google/YouTube. |
| Selected channel (page id, datasync id, name, authuser) | same `AuthStore` | |
| Discord token | `AuthStore` (`KEY_DISCORD_TOKEN`) | Secret. |
| Discord username, display name, avatar URL (shown on the Discord settings page) | `AppSettings` (`bitchord_settings` prefs: `discordUsername`, `discordName`, avatar) | Local display only; fetched from Discord's own API with the user's token. |
| Last.fm username + session key; ListenBrainz token | `AppSettings` prefs (`lastfm_username`, ...) | Scrobbling identity, unrelated to Jam. |
| Spotify login (web login session, canvas token) | handled by `SpotifyToken.kt` and the Spotify screens (exact preference file **UNCLEAR**, not traced in this audit) | **No profile name or avatar of the Spotify user is stored or shown** (grep for a Spotify display name found nothing). The login is for library import / canvas only. |
| Jam device id (random UUID), Jam nickname, recent kicks, last party | `ListenTogether` prefs `bitchord_listen_together` (`device_id`, `party_nickname`, ...) | `device_id` identifies the install to the server so a rejoin is not counted as a new device. Not excluded from backup (UNCLEAR whether that matters: it is only a random id). |
| Web: YT account (authUser, id, name, avatar) | `ytAuthSnapshot()` in the web app (browser storage + extension), see `web/app/src/shell/AccountButton.tsx` | |
| Web: Jam nickname, custom server, device id | `localStorage` `bitchord:party:settings` (nickname, customServer), `bitchord:deviceId` | |
| TV: per-YouTube-profile PIN hash (PBKDF2) | `TvSettings` prefs | Not an identity; gates opening a YouTube profile on that TV. |

## 3. Where identity is shown
| Screen | Shows | Source |
|---|---|---|
| Phone Settings, "Account & Integrations" row | Google **email** (or "Signed in / Not signed in") | `SettingsSheet.kt` (lines ~459, ~2041) from `AuthStore.activeSession.email`. Local display only. |
| Phone `AccountAndScrobblingScreen` | account + channel name, Last.fm "Signed in as", sign-out | |
| Phone `AccountProfileSelector` | YouTube profile avatar + name | |
| Phone Listen Together screen / sheets | own avatar (`ListenTogether.myAvatarUrl()` = the YouTube profile avatar) and a nickname field; member rows with `avatarUrl` | `ListenTogetherScreen.kt`, `ListenTogetherSheets.kt` |
| Now Playing "members" sheet | member names and avatars | `sharedUi/.../ListenTogetherMembersSheet.kt` (shared with dormant desktop) |
| TV top bar | YouTube profile avatar (`TvApp.kt` ~557) | |
| TV account dialog / "Who's watching?" | YouTube profiles (+ PIN prompt) | `TvDialogs.kt` |
| TV Jam dialog | member names only ("Name (host, you)"), no avatars | `TvListenTogetherDialog.kt` |
| Web sidebar `AccountButton` | YT avatar + name, account email in the menu | |
| Web Listen Together page / landing | member display name + `avatarUrl` | |
| Discord Rich Presence | the user's own Discord account, shown by Discord to Discord users | `data/discord`, `KizzyRPC.kt` |

## 4. What Jam sends today (and where it comes from)
`JoinRequest` for create and join: `userId`, `deviceId`, `displayName`, `avatarUrl?`, plus `maxMembers?` (create) and `autoplayEnabled?`. The server echoes members as `memberId, userId, displayName, avatarUrl, isHost, connected, joinedAtMs, lastSeenMs`; the party preview (anyone with the code) shows `displayName, avatarUrl, isHost` and the host name.
- **Android / TV** (`ListenTogether.identity()`): `userId = sha256(googleAccountId + ":" + youtubeProfileId).take(32)`; `displayName` = Jam nickname, else YouTube profile name, else Google account name, else the part of the email before `@`; `avatarUrl` = the YouTube profile avatar if http(s). **Returns null when nobody is signed in to YT Music, so create/join is refused ("Sign in to listen together").** No cookie, token or email is sent (checked in Phase 4, `JAM_BACKEND_FEASIBILITY.md` section 3).
- **Web** (`identity()` in `web/app/src/party/listenTogether.ts`): same idea with `sha256(authUser + ":" + channelId)`; also requires a signed-in YT account.
- **Server** (`backend/protocol/protocol.go` `JoinRequest.Validate`): `userId` 1-128, `deviceId` 1-128, `displayName` 1-80 bytes, `avatarUrl` http(s) up to 1000 chars. Unknown JSON fields are ignored (no `DisallowUnknownFields`), and the Android client (`ignoreUnknownKeys = true`) tolerates unknown fields in replies. So **new optional fields are backward compatible in both directions**.
- The server keeps members **in memory only**, for the life of the room, and does not log names or avatars (`JAM_BACKEND_DEPLOY.md`).

## 5. Scrobbling and Discord
- **Last.fm / ListenBrainz** use their own credentials (username, session key / token). They do not read any Jam or profile data and will not change.
- **Discord presence** uses the Discord token and the track being played; it does not use Jam or Google identity.
- **Neither is touched in this phase.** The in-app profile is never sent to Last.fm, ListenBrainz or Discord.

## 6. What this means for the design
1. Identity must be a **new, separate, local** thing; the Google/YouTube/Spotify/Discord logins remain for playback and library only.
2. Jam is the only consumer in this phase. It needs three small, optional wire fields next to `displayName` (a profile id, an avatar id, a colour) and must keep working for old clients/servers.
3. Jam currently *requires* a YT Music sign-in on every device. With a profile, a device can jam without signing in (a profile id gives the server the stable `userId` it needs). Without a profile, the old rule stays.
4. The YouTube profile switcher and the TV PIN are out of scope and stay as they are.
5. Auto-backup would copy a new plain preferences file to the cloud and to a new phone. Profile files are therefore excluded from cloud backup (see the design).
6. Dormant targets: `PartyModels.kt` (`JoinRequest`, `PartyMember`) and `ListenTogetherMembersSheet.kt` live in shared modules that the dormant desktop app also compiles. Changes there must be additive with defaults (logged in `FROZEN_TARGETS.md`).

## 7. Not found / UNCLEAR
- Whether the Spotify web login exposes a user name anywhere: not found in code, not displayed.
- Whether any Android system backup of `bitchord_listen_together` (the device id) is wanted: left as is.
- Wear OS and desktop have their own identity code paths (not read; dormant).
