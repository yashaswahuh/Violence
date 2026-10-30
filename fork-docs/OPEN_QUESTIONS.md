# Resolved Architectural & Implementation Decisions

**Audit Date:** 2026-10-29  
**Decision Sign-off Date:** 2026-10-29  
**Status:** **ALL QUESTIONS RESOLVED BY REPOSITORY OWNER. READY FOR PHASE 1.**

---

## 1. Core Project Decisions

### Decision 1: Upstream Tracking Remote
- **Resolution:** **YES — Add Read-Only Upstream Remote.**
- **Action for Phase 1:**
  - Add upstream tracking remote:
    ```bash
    git remote add upstream https://github.com/kushagrasinghx/BitChord.git
    git remote set-url --push upstream DISABLE
    ```
  - Fetch upstream and fast-forward local branch with `origin/main`.

---

### Decision 2: Rebranding Identity & Namespaces
- **Resolution:**
  - **App Display Name:** **"Violence"**
  - **Package ID / Application ID:** `com.yashaswahuh.violencemusic`
    - Phone Prod ID: `com.yashaswahuh.violencemusic`
    - Phone Dev ID: `com.yashaswahuh.violencemusic.dev`
    - Android TV Prod ID: `com.yashaswahuh.violencemusic.tv`
    - Android TV Dev ID: `com.yashaswahuh.violencemusic.tv.dev`
  - **Custom URI Scheme:** `violence://` (e.g. `violence://party/<CODE>`)
  - **FileProvider Authority:** `com.yashaswahuh.violencemusic.provider`

---

### Decision 3: Jam Backend Architecture
- **Resolution:** **STRATEGY 2 (Firebase Realtime Database - Spark Plan), with Go Backend Retained as Fallback.**
- **Details:**
  - Primary Jam implementation will use Google Firebase Realtime Database (Spark Plan, 100% free forever, no credit card required, zero cold starts, automatic offline persistence, native Android & Web real-time sync).
  - The existing Go backend code in `/backend` will be preserved as a fallback container service.

---

### Decision 4: Open-App Presence Telemetry
- **Resolution:** **RECOMMENDED APPROACH — DISABLE PRESENCE.**
- **Details:**
  - In a private personal fork for 4–6 individuals, public live listener badges are unnecessary.
  - Disable `Presence.kt` telemetry pings to save mobile battery and eliminate background network polling.

---

### Decision 5: Static Web Hosting Target
- **Resolution:** **GITHUB PAGES on `https://yashaswahuh.is-a.dev/violence`.**
- **Details:**
  - The user's root GitHub Pages domain is configured to `https://yashaswahuh.is-a.dev`.
  - The web player will be hosted at `https://yashaswahuh.is-a.dev/violence`.
  - The repository's `.github/workflows/web.yml` will be updated to deploy the static build (`pnpm build:site`) directly to GitHub Pages (`gh-pages` branch or GitHub Pages environment).
  - The companion browser extension will whitelist `https://yashaswahuh.is-a.dev/violence` in its allowed origins.

---

## 2. Decision 6: Last.fm Developer Credentials
- **Resolution:** **YES — KEEP LAST.FM INTEGRATION ENABLED.**
- **Details:**
  - Last.fm developer accounts and API keys are 100% free forever (no credit card).
  - The app will support Last.fm scrobbling when `LASTFM_API_KEY` and `LASTFM_SECRET` are provided in `local.properties`.

---

## 3. Technical Ambiguities Status

1. **Web Attestation for Age-Restricted Tracks:**
   - Evaluated as non-blocking. The web player will focus on anonymous/VISIONOS direct streaming (covering the vast majority of tracks). Signed-in playback on Android and Android TV works through native WebView BotGuard.
2. **Cloudflare Durable Objects:**
   - Eliminated from consideration in favor of Firebase RTDB and GitHub Pages.
