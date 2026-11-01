# Resolved Architectural & Implementation Decisions

**Audit Date:** 2026-10-29  
**Decision Sign-off Date:** 2026-10-29  
**Status:** All questions resolved by the repository owner. Decision 3 (Jam backend) was reversed on 2026-11-01; see below.

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
- **Resolution (REVERSED 2026-11-01, Phase 4): the existing Go backend in `/backend`, self-hosted on a free host (Render Free web service), is the Jam server. Firebase Realtime Database is only the documented FALLBACK and is NOT implemented.**
- **Why it changed:** the client and server already speak a complete Jam protocol (rooms, queue, anchor-and-offset sync). Re-implementing it on Firebase would mean rewriting the protocol on every client. The backend is a single static binary with no database and no secrets, small enough for a free instance (`JAM_BACKEND_FEASIBILITY.md`).
- **Cost accepted:** Render Free sleeps after 15 idle minutes (about 1 minute to wake) and rooms live in memory, so they are lost on sleep or restart. The apps show a "waking up" state and reconnect on their own.
- **Original decision (kept for the record):** Firebase RTDB (Spark Plan) as the primary, Go backend as fallback. Firebase remains the last fallback if no host works; see `JAM_BACKEND_DEPLOY.md` section 9.
- Details and manual steps: `JAM_BACKEND_FEASIBILITY.md`, `JAM_BACKEND_DEPLOY.md`, `JAM_TEST_PLAN.md`.

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
