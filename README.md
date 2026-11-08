<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="Violence app icon: a glowing blue V inside a glass orb" width="200" />

# Violence

### Private Music Streaming Client

<br/>

[**About This Fork**](#about-this-fork) · [**Features**](#features) · [**Disclaimer**](#disclaimer) · [**License**](#license)

</div>

## About this fork

**Violence Music** is a fork of [BitChord](https://github.com/kushagrasinghx/BitChord), created by Kushagra Singh. It builds on BitChord's open-source foundation with its own changes and features.

- **License:** The original BitChord project is licensed under the GNU General Public License v3.0 (GPL-3.0), and this fork stays licensed under the GNU General Public License v3.0 (GPL-3.0).
- **Modifications:** This project has been modified from the original starting on October 30, 2026.
- **Credit:** BitChord and its design are the work of Kushagra Singh; this fork is maintained separately and is not endorsed by YouTube, Google, Spotify, Last.fm, or Apple.

### Main Changes
Based on fork documentation and logs (`/fork-docs`):
- **Rebrand:** Renamed application identity and user-facing assets to Violence across Android, TV, Web, and companion extensions; updated package application IDs (`com.yashaswahuh.violencemusic`), URI schemes (`violence://`), and notification channels.
- **Author-Hosted Services Disabled or Replaced:** Decoupled and disabled upstream author telemetry and presence pings (`api.bitchord.kushagrasingh.in/api/presence`), the upstream update checker, and donation/promotional links. Nothing in the app calls the original author's servers.
- **Its own update check:** Settings has **Check for updates**. Once a day at most, Violence reads this repository's GitHub Releases (or an `update.json`, if the build sets `UPDATE_INFO_URL`), and an APK it offers is checked against its SHA-256 before it is handed to the system installer.
- **New CI:** GitHub Actions workflows for Android phone and TV debug builds, unit tests, GitHub Pages web deployment (with a `404.html` so deep links and refresh work), a manual desktop check, and a release workflow that builds the desktop packages.
- **Playback Sources & APIs Reference:** See [fork-docs/PLAYBACK_SOURCES.md](fork-docs/PLAYBACK_SOURCES.md) for complete technical documentation of playback endpoints, login flows, lyrics providers, and replacement guides.

### Third-Party Disclaimer
Violence is not affiliated with or endorsed by YouTube, Google, Spotify, Last.fm, or Apple. All trademarks, service marks, trade names, and logos belong to their respective owners. Users are solely responsible for complying with the terms of service of any third-party platforms they access through this software. The user interface of Violence is inspired by Apple Music.

---

<div align="center">

<img src="Banner.png" alt="Violence banner" width="100%" />

<h1><a id="features"></a>Features</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### Playback
- **Search, browse and play** anything available on YouTube Music.
- **Hi-Res lossless audio** — FLAC/ALAC from a configured module source, with YouTube Music as fallback.
- **Gapless playback with true crossfade**, adjustable 0–12s.
- **Automix [Beta]** — DJ-style transitions with beat-matching and tempo-stretching, and a cover-art blend from one song to the next.
- **Offline downloads** — save tracks with embedded metadata.
- **Local music library** integration.
- **Background playback** via a proper foreground media session.
- **Apple-like lyrics animation** — credit to [binimum](https://github.com/binimum/am-lyrics).

#### Connectivity & Accounts
- **Sign in with your Google account** for personalized content.
- **Spotify integration** — connect your account to play your playlists and Liked Songs.
- **Discord Rich Presence** — in-app login, live track/artist/album and progress.
- **Scrobbling** to Last.fm and ListenBrainz.
- **Pluggable sources** — add, edit, test and health-check module sources.
- **Jam (Listen Together)** — sync listening with up to 15 members on Android, wake-up notice, profile identities, and link sharing across Android, TV, Web, and Desktop. Joining or leaving a Jam keeps your own queue.
- **In-app profiles & friends** — 24 avatars, custom accent color, local friends list, and link sharing without requiring a Google sign-in.
- **Universal link sharing** — `violence://` scheme handler, QR codes, and clipboard link sharing for songs, playlists, profiles, and Jam.
- **Tap to share (NFC)** — hold two phones together to send a link; the receiving phone reads it as a `violence://` link.
- **Playlist import** — bring in a playlist from a Spotify or Apple Music link.

    </td>
    <td width="50%" valign="top">

#### Experience
- **Animated album canvas** — motion artwork on the now-playing screen.
- **Word-synced lyrics** — word/syllable-level highlighting from multiple sources.
- **Lyrics providers** — credit to [lrc.red](https://lrc.red), [BiniLyrics](https://github.com/binimum), [BetterLyrics](https://github.com/better-lyrics/better-lyrics), [PaxSenix](https://lyrics.paxsenix.org), [LyricsPlus](https://github.com/ibratabian17/YouLyPlus), [SimpMusic](https://github.com/maxrave-dev/SimpMusic), [Unison](https://unison.boidu.dev), [Megalobiz](https://www.megalobiz.com), [KuGou](https://www.kugou.com), [LRCLIB](https://lrclib.net), [Musixmatch](https://www.musixmatch.com) and [Genius](https://genius.com).
- **Dynamic, artwork-driven theming** — Material palette extracted from album art.
- **Frosted-glass UI** — translucent bars via Haze, Material 3 theming.
- **Replay** — your listening stats, shareable as a poster, with a listening personality (for example The Night Owl or The Sampler) worked out from how you listen. An experimental Quote design (Test 1.0) also styles shared lyrics cards and the Replay poster.
- **Alternate app icons** — a choice of launcher icons in Settings; the default is the blue V orb.

#### Controls & Tweaks
- **Per-network audio quality** — separate quality ceilings for Wi-Fi and mobile data.
- **Playback speed control** and **skip silence**.
- **Sleep timer** — fixed presets or "stop after this track".
- **System equalizer** integration.
- **AutoEQ headphone correction** — pick your headphone model from a built-in database, or load a custom curve.
- **Headphone crossfeed (bs2b)** — a binaural effect that softens hard stereo separation, with Subtle, Natural and Jan Meier presets.
- **Settings extras** — Check for updates, a Changelog and a Credits page.
- **Bit-perfect USB exclusive output** — sends lossless tracks to a USB DAC unchanged, at each track's own rate and bit depth. Thanks to [Mornye](https://github.com/mornyeapp/Mornye) for the reference.
- **Stats for nerds** — codec, bit depth, sample rate, and more on the now-playing screen.
- **Desktop system tray & shortcuts** — minimize-to-tray, a tray playback popup, a mini player (Ctrl+M), keyboard shortcuts such as Space, and media key integration (Windows SMTC, Linux MPRIS).
- **Installable Web PWA** — standalone mode, maskable icons, dynamic theme-color and accent-tinted SVG favicons.
- **Also on** Android TV (remote-first interface with synced lyrics and QR-code sign-in) and Wear OS (a watch remote for the phone app).

    </td>
  </tr>
</table>

</div>


---

<div align="center">

<h1><a id="disclaimer"></a>Disclaimer & Legal Notice</h1>

Violence is an independent, private third-party audio player and client. It is **not** associated with Google LLC, YouTube Music, Deezer, Telegram, or any of their parent companies.

* **No Media Hosting:** Violence does not host, upload, or store copyrighted music files. It operates strictly as an interface to scan local device storage or stream media directly from public, public-facing, or user-authenticated APIs.
* **Fair Use & API Usage:** This software is created solely for personal research, educational, and fair-use purposes. The user is entirely responsible for ensuring their usage aligns with their local copyright laws and YouTube Terms of Service.
* **No Ad-Blocking Guarantee:** While Violence focuses on providing a clean listening environment, it does not guarantee permanent bypasses or modifications to commercial third-party platform conditions.
* **Copyleft:** Violence is free software under the GPLv3. Any distribution must come with the Corresponding Source under the same license.

</div>

---

<div align="center">

<h1><a id="license"></a>License</h1>

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.

</div>


## Violence Style (experimental)

An optional skin for Android phone, Desktop, and Web: a display-serif heading style, a live glowing background, glass bars and buttons, and accent colours taken from the playing song, your device or a colour you pick. It is **on by default on Android phones** and **off by default on Desktop and Web**. With it off the app is exactly the app without it.

- **Turn it on or off:** Settings > "Appearance" on Android, Desktop and Web. On Android it follows your theme (System, Light or Dark) and has a dark and a light look; Desktop and Web use the dark look.
- **Settings:** background (Aurora, Corner, Haze, Off), motion (Off, Subtle, Normal), glow intensity (Low, Medium, High), accent source (Song, Device, Custom with nine presets and a picker), heading font (Bodoni Moda, Cormorant, Anton), reduce transparency, a glow on album and playlist pages, full-screen cover art on albums and playlists, and the Quote design for lyrics and Replay cards (Test 1.0). A "Mode" line shows when the app runs in Lite mode (older Android, low memory, battery saver, reduced blur).
- **If something goes wrong:** switch it off in the same place. If the app fails to start twice in a row with the skin on, it turns itself off and tells you.
- **Report a problem:** say which device and Android version you use and which settings were on.
- Fonts used by the skin (Inter, Bodoni Moda, Cormorant Garamond, Anton) are under the SIL Open Font License 1.1; see `NOTICE`. More: `fork-docs/style/STYLE_OVERVIEW.md`.
