# External Dependencies & Infrastructure Audit

**Audit Date:** 2026-10-29  
**Scope:** Complete repository scan across all modules, build configurations, manifests, workflows, and source files.

---

## 1. Original Author's Infrastructure & Domain References

The following table lists every hardcoded or configured reference to the original author's servers, domains, endpoints, repositories, and personal links. In our private fork, all references must be replaced or eliminated.

| # | File Path & Line | Original Reference | Description / Purpose | Replacement Required for Private Fork |
|---|---|---|---|---|
| 1 | `shared/src/jvmSharedMain/kotlin/com/music/bitchord/data/presence/Presence.kt:118` | `https://api.bitchord.kushagrasingh.in/api/presence` | Open-app presence telemetry endpoint called every 5 minutes. | Replace with self-hosted backend presence URL (or disable if presence tracking is not desired). |
| 2 | `web/app/src/party/listenTogether.ts:38` | `https://api.bitchord.kushagrasingh.in` | Default Listen Together party server URL for web player. | Replace with self-hosted backend URL or configure via environment variable. |
| 3 | `app/src/main/AndroidManifest.xml:246` | `android:host="bitchord.kushagrasingh.in"` | Deep link intent filter for party invite URLs (`https://bitchord.kushagrasingh.in/invite/...`). | Replace with fork's custom domain, GitHub Pages domain, or Cloudflare Pages hostname. |
| 4 | `tv/src/main/AndroidManifest.xml:246` | `android:host="bitchord.kushagrasingh.in"` | TV deep link intent filter for party invite URLs. | Replace with fork's custom domain / host. |
| 5 | `app/src/main/java/com/music/bitchord/data/listentogether/JamInviteLink.kt:14,15,23,27` | `https://bitchord.kushagrasingh.in` | Host constant and formatting logic for outgoing share links. | Replace `ORIGIN` and `HOST` constants with fork's invite host. |
| 6 | `shared/src/commonMain/kotlin/com/music/bitchord/data/listentogether/JamInvite.kt:12` | `https://bitchord.kushagrasingh.in` | KMP shared model origin for invite validation. | Replace `ORIGIN` constant with fork's domain. |
| 7 | `web/app/src/party/invite.ts:7` | `https://bitchord.kushagrasingh.in` | Web invite link generation origin (`INVITE_ORIGIN`). | Replace with fork's web hosting origin. |
| 8 | `web/extension/wxt.config.ts:61` | `extension@bitchord.kushagrasingh.in` | Browser extension Gecko ID in Manifest V3. | Replace with fork-specific extension ID. |
| 9 | `app/build.gradle.kts:53` | `https://bitchord-listen-together.onrender.com` | Default fallback build config for `LISTEN_TOGETHER_SERVER`. | Change default to self-hosted backend URL or require `local.properties`. |
| 10 | `tv/build.gradle.kts:52` | `https://bitchord-listen-together.onrender.com` | Default fallback build config for `LISTEN_TOGETHER_SERVER` on TV. | Change default to self-hosted backend URL. |
| 11 | `app/src/main/java/com/music/bitchord/data/AppUpdateChecker.kt:46` | `https://api.github.com/repos/kushagrasinghx/BitChord/releases/latest` | In-app update checker polling author's GitHub releases. | Point to user's private/public GitHub releases repository (`api.github.com/repos/yashaswahuh/...`). |
| 12 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopUpdateChecker.kt:36` | `https://api.github.com/repos/kushagrasinghx/BitChord/releases/latest` | Desktop in-app update checker. | (Module scheduled for pruning, but update URL if retained). |
| 13 | `web/app/src/config.ts:10` | `https://github.com/kushagrasinghx/BitChord/releases/latest` | Link to Android APK download displayed on web player. | Replace with fork's release URL. |
| 14 | `web/app/.env.example:7` | `https://github.com/kushagrasinghx/BitChord/releases/latest` | Example environment variable for Android app URL. | Replace with fork's release URL. |
| 15 | `app/src/main/java/com/music/bitchord/data/discord/DiscordRPC.kt:98` | `https://github.com/kushagrasinghx/BitChord` | Button URL in Discord Rich Presence ("Visit BitChord"). | Replace with fork's repository URL or personal site. |
| 16 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopDiscordRpc.kt:364` | `https://github.com/kushagrasinghx/BitChord` | Desktop Discord presence button URL. | Replace with fork's repository URL. |
| 17 | `web/extension/lib/discord.ts:33` | `https://github.com/kushagrasinghx/BitChord` | Extension Discord rich presence button URL. | Replace with fork's repository URL. |
| 18 | `app/src/main/java/com/music/bitchord/BitChordApplication.kt:151` | `https://github.com/kushagrasinghx/BitChord` | HTTP User-Agent header emitted by application. | Rebrand User-Agent string. |
| 19 | `app/src/main/java/com/music/bitchord/data/scrobbling/LastFM.kt:89` | `https://github.com/kushagrasinghx/BitChord` | User-Agent sent to Last.fm API. | Rebrand User-Agent string. |
| 20 | `app/src/main/java/com/music/bitchord/ui/screens/SettingsSheet.kt:1569-1581` | Links to `kushagrasinghx/BitChord`, `kushagrasinghx`, `discord.gg/pDdKfrdHY6`, `bitchord.kushagrasingh.in` | Settings screen colophon / footer links. | Remove author personal links; point to fork repo or remove entirely. |
| 21 | `desktopApp/src/main/kotlin/com/music/bitchord/desktop/DesktopApp.kt:6649-6653` | Links to `kushagrasinghx/BitChord`, `kushagrasinghx` | Desktop settings footer links. | Replace or remove. |
| 22 | `tv/src/main/java/com/music/bitchord/ui/tv/dialogs/TvDialogs.kt:1216` | `title = "Kushagra Singh", value = "Creator"` | TV "About" dialog row. | Update attribution while keeping GPL notices intact. |
| 23 | `backend/main.go:21-27` | `github.com/KabirSinghBhatia/BitChord/backend/...` | Go internal package imports in backend. | Replace module path with fork module path in `go.mod` and imports. |
| 24 | `backend/main.go:752` | `https://github.com/kushagrasinghx/BitChord/releases` | Fallback APK download link in web invite HTML template. | Replace with fork's release download URL. |
| 25 | `.github/workflows/deploy-backend.yml:31-43` | `ORACLE_DEPLOY_KEY`, `ORACLE_HOST`, `api.bitchord.kushagrasingh.in` | Upstream CD workflow deploying to author's Oracle VM. | Replace with self-hosted deployment action or remove. |
| 26 | `README.md:17` | `https://api.bitchord.kushagrasingh.in/api/stats/live/badge.svg` | Live stats badge in README. | Point to self-hosted badge or remove badge. |
| 27 | `README.md:127-131` | Ko-fi, PayPal (`paypal.me/kuxhagrasingh`), UPI (`upi_support.jpg`) | Donation and financial support links for original author. | Remove completely from fork documentation. |
| 28 | `.github/FUNDING.yml:1` | `ko_fi: kushagrasinghx` | GitHub funding configuration. | Delete or clear. |
| 29 | `CODE_OF_CONDUCT.md:14` | `kushagrasinghx@gmail.com` | Reporting email address. | Replace with fork owner's contact. |
| 30 | `app/src/test/java/.../JamInviteLinkTest.kt` & `shared/.../JamInviteTest.kt` | Unit tests hardcoded to `bitchord.kushagrasingh.in` | Unit test validation assertions. | Update test assertions to match fork's configured domain. |

---

## 2. Third-Party Services & APIs

The table below catalogs every third-party external service and API integrated into the application, evaluating authentication requirements, free-tier availability, and safety for personal use.

| Service / Platform | Used In / Classes | Purpose in App | Account / Key Required? | Free Tier Available? | Private Fork Verdict & Recommendations |
|---|---|---|---|---|---|
| **YouTube Music (InnerTube)** | `shared/.../StreamResolver.kt`, `YtMusicRepository.kt`, `InnerTubeXResolver.kt` | Core catalog search, album/artist browsing, stream URL extraction. | **No account required** for anonymous playback. Optional Google sign-in via cookie for user library. | **100% Free** (Direct API scraping). | **SAFE TO KEEP.** Core functionality. Requests originate directly from client IP. |
| **YouTube Video CDN (Googlevideo)** | `StreamResolver.kt`, Media3 `ExoPlayer`, Shaka Player | Audio stream delivery (Opus / AAC / DASH byte ranges). | None. URLs signed by YouTube cipher tiers. | **100% Free**. | **SAFE TO KEEP.** Streams directly to client device; no proxying. |
| **Spotify Web API & Pathfinder** | `SpotifyToken.kt`, `SpotifyLibrary.kt`, `SpotifyCanvas.kt` | Liked songs & playlist import; vertical video canvas background. | **User's own account cookie (`sp_dc`)** entered in Settings. No developer API key. | **100% Free** (Standard free Spotify account works). | **SAFE TO KEEP.** Completely optional and opt-in per user. |
| **Last.fm** | `app/.../LastFM.kt`, `AccountAndScrobblingScreen.kt` | Music scrobbling and "Now Playing" status updates. | Requires user account + API Key (`LASTFM_API_KEY`, `LASTFM_SECRET` via `local.properties`). | **100% Free** (Personal developer keys from Last.fm are free). | **SAFE TO KEEP.** Optional. Fork owner can generate free personal keys or leave blank. |
| **ListenBrainz** | `app/.../ListenBrainzManager.kt` | Open-source music scrobbling. | Requires user's personal ListenBrainz User Token. | **100% Free** (Non-profit MetaBrainz project). | **SAFE TO KEEP.** Optional, user-configured. |
| **Discord Gateway** | `DiscordRPC.kt`, `DesktopDiscordRpc.kt`, `web/extension/.../discord.ts` | Rich Presence ("Listening to ...") on user's profile. | Requires user's Discord personal token. | **100% Free**. | **SAFE TO KEEP (WITH WARNING).** Works via user gateway token. App already warns user of self-bot policy in Settings. |
| **Google Translate Web** | `LyricsTranslation.kt:68` (`translate.googleapis.com`) | On-demand dynamic lyrics translation with syllable re-timing. | **No key required** (Uses Google's public web translation endpoint `client=gtx`). | **100% Free**. | **SAFE TO KEEP.** Low traffic for 4–6 personal users; negligible rate-limit risk. |
| **Google Input Tools** | `LyricsTranslation.kt:69` (`inputtools.google.com`) | Language input transliteration fallback. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **JioSaavn** | `JioSaavnSource.kt`, `JioSaavnService.kt` | Optional regional high-bitrate music source (320kbps AAC). | **No key required** (DES decrypted stream URLs from public endpoint). | **100% Free**. | **SAFE TO KEEP.** Opt-in module source. |
| **LRCLIB** | `shared/.../lyrics/LrcLib.kt` (`lrclib.net`) | Synced and plain lyrics provider. | **No key required**. | **100% Free** (Open community API). | **SAFE TO KEEP.** |
| **BiniLyrics** | `shared/.../lyrics/BiniLyrics.kt` (`lyrics-api.binimum.org`) | Syllable-synced lyrics provider. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **lrc.red** | `shared/.../lyrics/LrcRed.kt` (`lrc.red`) | Apple-style TTML synced lyrics provider. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **BetterLyrics / Unison** | `BetterLyrics.kt`, `Unison.kt` (`lyrics-api.boidu.dev`) | Syllable-synced lyrics provider. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **PaxSenix** | `PaxSenix.kt` (`api.paxsenix.org`, `lyrics.paxsenix.org`) | Apple Music catalog and lyrics scraping proxy. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **LyricsPlus** | `LyricsPlus.kt` (Multiple community mirrors) | Multi-provider fallback lyrics mirror. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **SimpMusic Lyrics** | `SimpMusicLyrics.kt` (`api-lyrics.simpmusic.org`) | Synced lyrics API. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **Megalobiz** | `Megalobiz.kt` (`www.megalobiz.com`) | LRC lyrics search and scraper. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **KuGou** | `KuGou.kt` (`lyrics.kugou.com`) | Chinese and Asian music lyrics search. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **Musixmatch** | `Musixmatch.kt` (`apic.musixmatch.com`) | Commercial lyrics API using embedded app token. | **No user key required** (Uses embedded app client token). | **100% Free**. | **SAFE TO KEEP.** |
| **Genius** | `Genius.kt` (`genius.com`) | Plain text lyrics scraper. | **No key required**. | **100% Free**. | **SAFE TO KEEP.** |
| **Custom Addons (Pluggable Sources)** | `AddonSource.kt`, `AddonClient.kt` | External user-defined audio sources (FLAC/ALAC lossless). | Configured by user in app. | Depends on user's host. | **SAFE TO KEEP.** Completely optional. |
| **Google Cast Framework** | `CastPlayback.kt`, `play-services-cast-framework` | Casting playback to Chromecast and Google Nest devices. | Uses Google Cast developer app ID or default receiver. | **100% Free**. | **SAFE TO KEEP.** Standard Android framework API. |

---

## 3. Analytics & Telemetry Assessment

- **Third-Party Crashlytics / Analytics:** **NONE FOUND**. The codebase contains **zero** Google Firebase Crashlytics, Sentry, Mixpanel, Segment, or Datadog SDKs.
- **Internal Telemetry:**
  - `Presence.kt`: Transmits install UUID and platform to the backend. In our fork, this will route only to our self-hosted backend.
  - `PartySync.kt`: Transmits playhead position reports (`report` frame) over WebSocket for room drift monitoring.
- **Privacy Verdict:** Highly clean. No telemetry or crash data is leaked to commercial tracking platforms.
