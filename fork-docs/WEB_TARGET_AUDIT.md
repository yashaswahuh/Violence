# Web Target Technical Audit & Specification

**Audit Date:** 2026-10-29  
**Source Directory:** `/web`  
**Reference Specification:** `docs/web-app-build-prompt.md` and `web/README.md`  
**Package Manager:** `pnpm` (configured via `pnpm-workspace.yaml` and `package.json`)  
**Node Version Requirement:** Node 22+ (`web/README.md:16`, `.github/workflows/web.yml:38`)

---

## 1. Web Target Architecture & Components

The web target is structured as a pnpm monorepo containing three interconnected packages:

```
web/
├── package.json               # Root scripts: typecheck, build, build:site
├── pnpm-workspace.yaml        # Packages: app, extension, shared
├── app/                       # The Web Player SPA (Vite + React 19)
├── extension/                 # Companion Browser Extension (WXT Manifest V3)
└── shared/                    # Bridge protocol contracts and shared TypeScript types
```

### 1.1 `web/app` (The Web Player SPA)
- **Framework & Build:** React 19, Vite 6, TypeScript 5.7+ (strict mode).
- **UI & Design:** Custom replication of Apple Music's web player (`music.apple.com`) using Tailwind CSS v4, custom CSS variables (`src/styles/tokens.css`), and Google Fonts DM Sans.
- **Routing:** React Router (data router) with lazy loading (`React.lazy` + `Suspense`) and layout skeletons for every view.
- **State Management:**
  - Zustand: Player transport state (`usePlayerStore`), queue state (`useQueueStore`), and user settings.
  - TanStack Query v5: API data fetching, deduplication, caching, and background revalidation.
- **Audio Playback:**
  - Shaka Player (compiled build) for YouTube DASH manifests and SABR streams (`src/playback/youtube/ShakaPlayerAdapter.ts`).
  - Standard HTML5 `<audio>` element for progressive HTTP streams.
  - Unified Web Audio API graph (`AudioContext`) for volume control, equalizer, and crossfade.

### 1.2 `web/extension` (The Companion Browser Extension)
- **Framework:** Manifest V3 built with WXT (Web Extension Tools), compiling to Chromium and Firefox targets.
- **Purpose:** Solves the fundamental browser security limits that prevent web pages from talking to YouTube Music:
  1. **Network Bridge:** Injects required headers (`Origin: https://music.apple.com`, `Referer`, `X-Origin`) and strips Origin/Referer on `c=VISIONOS` requests via `declarativeNetRequest` rules (`rules.ts`).
  2. **Identity & Sign-in:** Manages credentialed YouTube session cookies safely in the background service worker without exposing raw credentials to the DOM.
  3. **Spotify & Discord Bridges:** Routes canvas GraphQL queries and Discord presence frames without CORS rejection.
- **Page ↔ Extension Communication:**
  - **Chromium (Chrome, Edge, Brave):** Uses `externally_connectable` with a pinned extension ID (`pkddffeebcmhhanjcigilehaeonkkfih` via `extension/dev-key/`). The web app calls `chrome.runtime.sendMessage(EXTENSION_ID, ...)`.
  - **Firefox:** Firefox does not support `externally_connectable` for web pages. Uses a content script running on the site origin that relays between `window.postMessage` and `runtime.sendMessage`.
  - Both transports are abstracted behind `web/shared/src/bridge.ts`.

---

## 2. Audio Streaming & Proxy Architecture

### How the Web Version Obtains and Plays Audio

The browser streams audio through an in-browser resolution pipeline:

1. **Player Request:** `web/app/src/playback/youtube/resolver.ts` calls YouTube's InnerTube player API using YouTube.js, walking client profiles: `VISIONOS` ➔ `YTMUSIC (WEB_REMIX)` ➔ `WEB` ➔ `MWEB`.
2. **VISIONOS Direct Stream Resolution:** `VISIONOS` client responses return direct audio URLs (Opus itag 251, ~130–160 kbps).
3. **In-Browser DASH Manifest Generation:** The web app dynamically generates an in-memory DASH XML manifest (`data:application/dash+xml;base64,...`) pointing directly to googlevideo.com byte ranges.
4. **Shaka Player Execution:** Shaka Player ingests the dynamic manifest and fetches byte ranges directly from Google's CDN.

### Server-Side Proxy Audit: Is There a Proxy in `/backend`?

> [!IMPORTANT]
> **NO SERVER-SIDE AUDIO PROXY EXISTS IN THIS CODEBASE.**
> There is **no proxy in the `/backend` folder**, and **no audio traffic ever touches any server**.

This is an explicit, documented architectural constraint in `docs/web-app-build-prompt.md:50-57`:
> *"Compute budget (a hard constraint): The only server is an Oracle Always Free VM: 1/8 OCPU, 1 GB RAM, roughly 50 Mbps. No audio and no YouTube API traffic may ever go through our servers... All YouTube traffic leaves from the user's browser, from their own IP, through the extension. Do not build an audio proxy. A capped proxy for users without the extension is explicitly out of scope."*

The `/backend` folder contains **only** the Go synchronization server for Listen Together and presence tracking. It handles zero audio bytes and makes zero calls to YouTube or Google.

The browser extension (`web/extension`) acts as the local, on-device proxy/bridge by modifying request headers at the browser network layer.

---

## 3. Static Hosting Feasibility & Tradeoffs

### Can the Web Version Be Hosted Statically on Free Hosts (Cloudflare Pages / GitHub Pages)?

**YES, ABSOLUTELY.**  
The web player (`web/app`) is already engineered to compile into static HTML/JS/CSS assets.

### Build and Deployment Configuration
- **Build Command:** `corepack pnpm --dir web build:site`
  - This script compiles `web/extension` first, packages the browser extension zip files (`bitchord-extension-chrome.zip`, `bitchord-extension-firefox.zip`) into `web/app/public/downloads/`, and runs `vite build`.
- **Output Directory:** `web/app/dist`
- **Supported Static Hosts:**
  - **Cloudflare Pages:** 100% free, unlimited bandwidth, global edge CDN, custom domain support. (Already documented upstream in `web/README.md:67-75`).
  - **GitHub Pages:** 100% free, straightforward GitHub Actions automated deployment.

### What Works Fully Statically
- Entire Apple Music-style UI (Search, Browse, Albums, Playlists, Artist pages, Settings).
- Full player chrome, queue management, volume slider, sleep timer.
- Extension download and update-check page (`/extension`).
- Static lyrics rendering and local UI interactions.

### What Would Break (and Required Countermeasures)

| Feature | Behavior Without Companion Extension | Behavior With Extension Configured for Site Origin |
|---|---|---|
| **YouTube Music Audio Playback** | **BROKEN.** Browser blocks calls to `googlevideo.com` and `youtubei/v1` due to CORS preflights and missing headers. Player displays prompt to install extension. | **WORKS COMPLETELY.** Extension modifies headers on client side; audio streams directly from Google CDN to browser. |
| **YouTube Account Sign-in** | **DISABLED.** Web page cannot access user's Google session cookies. | **WORKS.** Extension background worker accesses session cookies securely. |
| **Spotify Liked Songs / Canvas** | **DISABLED.** Fails due to cross-origin blocks on Spotify Pathfinder GraphQL. | **WORKS.** Extension bridges requests with user's `sp_dc` cookie. |
| **Discord Rich Presence** | **DISABLED.** Web page cannot open arbitrary raw WebSockets without gateway rejection. | **WORKS.** Extension manages Discord gateway WebSocket. |
| **Listen Together (Jam)** | **DEPENDS ON BACKEND.** Jam requires a WebSocket connection to the self-hosted Go backend. It cannot run purely on static hosting. | **WORKS**, provided the backend server URL is configured in settings or environment. |

### Critical Fork Configuration for Static Hosting
When hosting the web app on a new domain (e.g. `https://myfork.pages.dev` or GitHub Pages):
1. **Extension Origin Whitelist:** In `web/extension`, the environment variable `BITCHORD_SITE_ORIGINS` must be set to include the new static domain during the extension build (`BITCHORD_SITE_ORIGINS=https://myfork.pages.dev`). Otherwise, the extension will refuse to bridge messages from the new site.
2. **Web App Configuration:** In `web/app/.env.production`, set:
   ```env
   VITE_ANDROID_APP_URL=https://github.com/yashaswahuh/Violence/releases/latest
   ```
