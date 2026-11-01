# Allowed External Services & API Catalog

**Audit Date:** 2026-10-30 (updated 2026-11-01 for the v1.9 sync; no other new external services appeared in v1.9)  
**Phase:** Phase 1 (Repository Hygiene & Author Decoupling)  
**Scope:** Complete inventory of every external domain, API, and service contacted at runtime.

---

## 1. Classification Methodology & Policy

Each runtime external endpoint is classified according to the following directives:
- **KEEP:** Required for catalog search, stream/URL resolution, audio playback, lyrics, metadata, album art/thumbnails, Spotify link/import, downloads, Last.fm, Libre.fm, ListenBrainz, Discord RPC, or WebDAV. Services are kept with identical runtime behavior.
- **DISABLE:** Author-hosted or third-party live listener stats, open-app presence telemetry (`Presence.kt`), analytics, crash reporting, update checking against author repositories, donation/support prompts, and promotional links. These are either cleanly removed or converted into zero-overhead no-ops.
- **REPLACE-LATER:** Party/Jam room networking and deep link invite routing. Kept untouched in Phase 1. **Phase 4 (2026-11-01):** Jam now uses the owner's self-hosted copy of `/backend` (Firebase is only the documented fallback).

---

## 2. External Services & Endpoints Inventory

| Service / Platform | Endpoint / Domain | File & Line Reference | Feature / Purpose | Classification | Hosted by Original Author? | Notes & Security Audit |
|---|---|---|---|---|---|---|
| **YouTube Music (InnerTube)** | `music.youtube.com`, `www.youtube.com` (`/youtubei/v1/*`) | `shared/.../StreamResolver.kt:71`, `YtMusicRepository.kt:35`, `InnerTubeXResolver.kt` | Search catalog, browse albums/artists, stream metadata, up-next | **KEEP** | No (Google LLC) | Direct client-to-YouTube InnerTube API queries. No API key required. |
| **YouTube Video CDN** | `*.googlevideo.com` | `StreamResolver.kt`, Media3 `ExoPlayer`, `AudioCache.kt:48`, `ChunkedDataSource.kt` | Audio byte streaming (Opus, AAC, DASH ranges) | **KEEP** | No (Google LLC) | Signed cipher streaming directly to client IP. |
| **YouTube Imagery CDN** | `i.ytimg.com`, `yt3.ggpht.com`, `yt3.googleusercontent.com` | `shared/.../Song.kt:artworkAt`, `AndroidManifest.xml:182` | Song thumbnails, playlist covers, artist avatars | **KEEP** | No (Google LLC) | Public image CDN. |
| **Google Authentication** | `accounts.google.com` | `app/.../WebSession.kt:55`, `YtMusicLoginScreen.kt:24` | Optional sign-in to access user's YouTube Music library | **KEEP** | No (Google LLC) | User authenticates via embedded WebView; cookie stored locally. |
| **Google Translate Web** | `translate.googleapis.com` | `app/.../LyricsTranslation.kt:68` | On-demand dynamic lyrics machine translation | **KEEP** | No (Google LLC) | Public `client=gtx` translation endpoint. Zero cost, no key. |
| **Google Input Tools** | `inputtools.google.com` | `app/.../LyricsTranslation.kt:69` | Dynamic language transliteration / pronunciation | **KEEP** | No (Google LLC) | Public Google transliteration endpoint. |
| **Spotify Web API & Pathfinder** | `accounts.spotify.com`, `clienttoken.spotify.com`, `api-partner.spotify.com`, `api.spotify.com`, `spclient.wg.spotify.com` | `app/.../SpotifyToken.kt:14`, `SpotifyLibrary.kt:35` | Import liked songs & user playlists from Spotify | **KEEP** | No (Spotify AB) | Opt-in per user with user's own `sp_dc` cookie. |
| **Spotify Visuals & Canvas** | `i.scdn.co`, `image-cdn-ak.spotifycdn.com`, `image-cdn-fa.spotifycdn.com`, `canvaz.scdn.co` | `app/.../SpotifyCanvas.kt:42`, `SpotifyLibrary.kt` | Spotify album art, artist headers, looping canvas video | **KEEP** | No (Spotify AB) | Direct CDN asset requests. |
| **ViviMusic Canvas Worker** | `vivimusicanvas.mkmdevilmi.workers.dev` | `app/.../SpotifyCanvas.kt:50` | Fallback proxy worker for Canvas MP4 extraction | **KEEP** | No (Community Cloudflare Worker) | Fallback when direct canvas access is restricted. |
| **JioSaavn** | `www.jiosaavn.com`, `aac.saavncdn.com` | `app/.../JioSaavnSource.kt:18`, `JioSaavnService.kt:25` | High-bitrate regional audio source (320kbps AAC) | **KEEP** | No (JioSaavn / Reliance) | Opt-in modular audio source. DES decrypted stream URLs. |
| **LRCLIB** | `lrclib.net` | `shared/.../lyrics/LrcLib.kt:24` | Line-synced and plain lyrics provider | **KEEP** | No (Community Open Source) | Non-profit community API. No key required. |
| **BiniLyrics** | `lyrics-api.binimum.org` | `shared/.../lyrics/BiniLyrics.kt:18` | Syllable-synced lyrics provider | **KEEP** | No (Community Open Source) | Open community lyrics service. |
| **lrc.red** | `lrc.red` | `shared/.../lyrics/LrcRed.kt:20` | Apple-style TTML synced lyrics provider | **KEEP** | No (Community Open Source) | Open community API. |
| **BetterLyrics / Unison** | `lyrics-api.boidu.dev`, `unison.boidu.dev`, `cdn.betterlyrics.org` | `shared/.../lyrics/BetterLyrics.kt:22`, `Unison.kt:25` | Syllable-synced lyrics provider | **KEEP** | No (Community Open Source) | High-accuracy synced lyrics. |
| **PaxSenix** | `api.paxsenix.org`, `lyrics.paxsenix.org` | `shared/.../lyrics/PaxSenix.kt:22` | Apple Music & Musixmatch lyrics proxy | **KEEP** | No (PaxSenix) | Public scraper proxy. |
| **Apple Music / iTunes** | `amp-api.music.apple.com`, `is1-ssl.mzstatic.com`, `itunes.apple.com` | `shared/.../PaxSenix.kt:35`, `Song.kt` | Catalog search and high-resolution album artwork | **KEEP** | No (Apple Inc.) | Public storefront and artwork CDN. |
| **Musixmatch** | `apic.musixmatch.com`, `www.musixmatch.com` | `shared/.../lyrics/Musixmatch.kt:27,29` | Commercial lyrics database | **KEEP** | No (Musixmatch) | Uses embedded fallback client secret (`f09016176ba43a1cfd1031fbd6b3d26c`). Flagged in Phase 1 report. |
| **SimpMusic Lyrics** | `api-lyrics.simpmusic.org` | `shared/.../lyrics/SimpMusicLyrics.kt:19` | Synced lyrics API | **KEEP** | No (SimpMusic Open Source) | Open lyrics API. |
| **Megalobiz** | `www.megalobiz.com` | `shared/.../lyrics/Megalobiz.kt:24` | Community LRC lyrics scraper | **KEEP** | No (Megalobiz) | Web scraper. |
| **KuGou** | `lyrics.kugou.com` | `shared/.../lyrics/KuGou.kt:22` | Asian & Chinese language synced lyrics | **KEEP** | No (Tencent / KuGou) | Public search API. |
| **Genius** | `genius.com`, `images.genius.com`, `assets.genius.com` | `shared/.../lyrics/Genius.kt:22` | Plain-text lyrics and annotations | **KEEP** | No (Genius Media) | Web scraper using open multi-search endpoint. |
| **Last.fm** | `ws.audioscrobbler.com`, `www.last.fm` | `app/.../scrobbling/LastFM.kt:22`, `ArtistFacts.kt:234` | Music scrobbling and artist genre tags | **KEEP** | No (Last.fm / CBS) | Free personal developer keys configured via `local.properties`. |
| **Libre.fm** | `libre.fm` | `app/.../scrobbling/LastFM.kt:23` | GNU open-source music scrobbling | **KEEP** | No (Libre.fm / Foocorp) | Open alternative scrobbling endpoint. |
| **ListenBrainz** | `api.listenbrainz.org` | `app/.../scrobbling/ListenBrainzManager.kt:19` | Non-profit music scrobbling | **KEEP** | No (MetaBrainz Foundation) | User-supplied personal token. |
| **Discord Gateway** | `gateway.discord.gg`, `discord.com/api/v9`, `cdn.discordapp.com` | `app/.../discord/DiscordRPC.kt:33`, `DiscordWebSocket.kt:12` | Rich Presence playback status on Discord profile | **KEEP** | No (Discord Inc.) | User-supplied Discord user token. |
| **Google Cast Framework** | Local LAN / Receiver | `app/.../CastPlayback.kt` | Casting audio to Chromecast / Nest devices | **KEEP** | No (Google LLC) | Standard Android Cast SDK framework. |
| **Custom Addons** | User-defined URLs | `shared/.../AddonSource.kt:15` | Pluggable audio sources (e.g. FLAC / lossless) | **KEEP** | No (User defined) | Configured by user in app. |
| **WebDAV** | User-defined URLs | `app/.../WebDavClient.kt:20` | Remote/local personal library streaming | **KEEP** | No (User defined) | Configured by user in app. |
| **Jam backend (Listen Together)** | owner's own Render service, address from `JAM_BACKEND_URL` (empty by default; Android/TV `local.properties`, web `VITE_PARTY_SERVER_URL`) | `app/build.gradle.kts`, `tv/build.gradle.kts`, `web/app/src/party/listenTogether.ts` | Party / Jam multi-device playback synchronization | **KEEP (self-hosted)** | **NO since Phase 4** | Was REPLACE-LATER and pointed at the original author's `bitchord-listen-together.onrender.com` / `api.bitchord.kushagrasingh.in`. Phase 4 removed those defaults from the code; the app now talks to the owner's copy of `/backend`. See `JAM_BACKEND_DEPLOY.md`. |
| **Jam Invite Domain** | the address in `JAM_INVITE_BASE_URL` (owner's own web player; empty by default) | `app/.../JamInviteLink.kt`, `web/app/src/party/invite.ts`, both `AndroidManifest.xml` intent filters (`yashaswahuh.is-a.dev`) | Outgoing share links (`<base>/party/<CODE>`) and the deep link filters | **KEEP (owner's domain)** | No | Phase 5: no hard-coded author domain remains in Jam invite code; the legacy author host is read nowhere. Empty base = no web link, only the code and `violence://party/<CODE>`. |
| **Open-App Presence Telemetry** | `api.bitchord.kushagrasingh.in/api/presence` | `shared/.../presence/Presence.kt:118`, `AndroidPresence.kt:22`, `DesktopPresence.kt:21` | Background heartbeat tracking active installs every 5 min | **DISABLE** | **YES (Original Author)** | Cleanly no-oped in Phase 1: `ENDPOINT` nullified, `AndroidPresence.install` made no-op. Zero network calls. |
| **Live Stats & Badge** | `api.bitchord.kushagrasingh.in/api/stats/live`, `.../badge.svg` | `README.md:17` | Public counter and vector badge of open apps | **DISABLE** | **YES (Original Author)** | Badge and link removed from README. |
| **Author In-App Update Checker** | `api.github.com/repos/kushagrasinghx/BitChord/releases/latest` | `app/.../AppUpdateChecker.kt:46`, `desktopApp/.../DesktopUpdateChecker.kt:36` | Polling author's GitHub releases for updates | **DISABLE** | **YES (Original Author)** | Cleanly no-oped: `LATEST_RELEASE_URL` set to empty string, check returns immediately. Re-enabled for fork in Phase 8. |
| **Author APK Web Promotional Link** | `github.com/kushagrasinghx/BitChord/releases/latest` | `web/app/src/config.ts:10`, `web/app/.env.example:7` | Web banner / sidebar prompt to download author's APK | **DISABLE** | **YES (Original Author)** | Default URL set to empty; UI conditionally suppresses download button when unset. |
| **Promotional Discord RPC Button** | `github.com/kushagrasinghx/BitChord` | `app/.../DiscordRPC.kt:17,98`, `AppSettings.kt` | "Visit BitChord" button 2 on Discord status | **DISABLE** | **YES (Original Author)** | Button 2 default visibility set to `false`. |
| **Settings Screen Author Links** | `github.com/kushagrasinghx/BitChord`, `github.com/kushagrasinghx`, `discord.gg/pDdKfrdHY6`, `bitchord.kushagrasingh.in` | `app/.../SettingsSheet.kt:1569-1583`, `desktopApp/.../DesktopApp.kt:6649-6659` | Footer links to author's profile, Discord, and website | **DISABLE** | **YES (Original Author)** | Removed from Settings footer. Version string retained. |
| **Donation & Crowdfunding Prompts** | `ko-fi.com/kushagrasinghx`, `paypal.me/kuxhagrasingh`, `upi_support.jpg`, `.github/FUNDING.yml` | `README.md:127-131`, `.github/FUNDING.yml`, `upi_support.jpg` | Financial solicitation links for original author | **DISABLE** | **YES (Original Author)** | Deleted `upi_support.jpg` and `.github/FUNDING.yml`; stripped `# Support` from README. |
| **Web Live Presence Ping (added by upstream v1.9)** | `<party server>/api/presence` (author's Jam server) | `web/app/src/playback/livePresence.ts` | Open-tab counter for the web player | **DISABLE** | **YES (Original Author)** | New in v1.9. Disabled in the 2026-10-31 sync (`PRESENCE_ENABLED = false`). Zero network calls. |
| **Author Backend Deployment Workflow** | `api.bitchord.kushagrasingh.in/healthz` (Oracle VM) | `.github/workflows/deploy-backend.yml` | Automated CD deploying to author's Oracle VM | **DISABLE** | **YES (Original Author)** | Deleted workflow file. |
| **Author Upstream Issue Closer** | Upstream GitHub Actions | `.github/workflows/close-issue.yml` | Auto-closes issues with `/close` for specific upstream logins | **DISABLE** | **YES (Original Author)** | Deleted workflow file. |

---

## 3. Author-Hosted Services Reliance Assessment

Of all services in the catalog, **ONLY ONE functional service is hosted by the original author**:
- **Listen Together / Jam Party Backend** (`bitchord-listen-together.onrender.com` / `api.bitchord.kushagrasingh.in`).

**Crucial Note for Repository Owner:**
No music playback, search, lyrics, metadata, or download services are hosted by the original author. Music streaming and playback queries go directly from client devices to YouTube Music, Google CDN, JioSaavn, and public community lyrics providers. Therefore, keeping playback and search working requires **zero reliance** on the author's personal infrastructure. The only feature that touched author servers was Jam/Listen Together; since Phase 4 no Jam default points at the author (the invite-link leftovers above are the exception).

