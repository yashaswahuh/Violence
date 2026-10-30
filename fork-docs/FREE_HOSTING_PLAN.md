# Free Hosting Architecture & Infrastructure Plan

**Audit Date:** 2026-10-29  
**Core Constraint:** 100% Free forever. No paid tiers, no expiring trials, no credit card requirements.  
**Audience Capacity:** Private personal group (4–6 people).  
**Chosen Primary Hosting:** **GitHub Pages (`https://yashaswahuh.is-a.dev/violence`) + Google Firebase Spark Plan (RTDB)**.

---

## 1. Candidate Infrastructure Evaluation

We evaluated six free-tier hosting platforms against the requirements of our private fork:

| Platform | Capabilities Provided | Verified Free Tier Limits | Credit Card Required? | Suitability for This Project |
|---|---|---|---|---|
| **GitHub Pages** | Static web hosting from repository branch or Actions artifact. | 100 GB/month bandwidth, 1 GB site size limit, 10 builds/hour. | **NO** | **SELECTED for Static Web Hosting.** Mapped to `https://yashaswahuh.is-a.dev/violence`. |
| **Cloudflare Pages** | Static web hosting, edge CDN, custom domains, Git CI/CD. | Unlimited bandwidth, unlimited requests, 500 builds/month. | **NO** | Available as secondary static fallback. |
| **GitHub Releases** | Binary asset hosting, versioned distribution, global CDN. | 2 GB max per file, unlimited total release assets, GitHub API integration. | **NO** | **SELECTED for APK Update Distribution.** Native fit for `AppUpdateChecker.kt`. |
| **Firebase Spark Plan (Google)** | Realtime Database (RTDB), Cloud Storage, Hosting. | 100 simultaneous connections, 1 GB DB storage, 10 GB/month download egress. | **NO** (Genuinely zero CC on signup). | **SELECTED for Jam Sync.** Serverless, zero spin-down, instant sync, native Android & Web SDKs. |
| **Render (Free Tier)** | Docker / Go Web Service, public URL, automated TLS. | 512 MB RAM, 0.1 CPU, 750 instance hours/month. | **NO** | **RETAINED as Fallback Jam Backend.** Existing Go codebase in `/backend` preserved. |
| **Cloudflare Workers + Durable Objects** | Edge compute + stateful WebSocket coordination. | Workers free has 100k requests/day. Durable Objects historically requires $5/mo Paid plan. | **YES / RISKY** (Durable Objects requires account billing profile / card). | **REJECTED.** Violates the hard no-credit-card constraint. |

---

## 2. Infrastructure Plan by Backend Responsibility

### 2.1 Static Web Hosting: GitHub Pages
- **Host Domain:** `https://yashaswahuh.is-a.dev/violence`
- **Why:** The repository owner already has custom domain `yashaswahuh.is-a.dev` routed to GitHub Pages. Because this repository is named `Violence`, GitHub Pages automatically serves it at `https://yashaswahuh.is-a.dev/violence` out of the box with zero external DNS or Cloudflare subpath routing complexity.
- **Workflow Configuration:**
  - Build command: `corepack pnpm --dir web build:site`
  - Output directory: `web/app/dist`
  - GitHub Actions workflow `.github/workflows/web.yml` builds the static site and publishes artifacts directly to GitHub Pages (`actions/deploy-pages@v4`).
- **Browser Extension Integration:**
  - The companion browser extension will whitelist `https://yashaswahuh.is-a.dev/violence` in its allowed origins (`BITCHORD_SITE_ORIGINS`).

---

### 2.2 APK Update Distribution: GitHub Releases
- **Primary Mechanism:** GitHub Releases API.
- **Implementation:** The Android phone app (`AppUpdateChecker.kt`) and TV app query:
  ```
  GET https://api.github.com/repos/yashaswahuh/Violence/releases/latest
  ```
  It parses `tag_name`, compares version codes, downloads the matching APK asset, and invokes the Android package installer (`FileProvider`).
- **Cost:** 100% free, 0 credit card, unlimited downloads.

---

### 2.3 Presence & Live Stats Tracking: Disabled
- **Decision:** As confirmed by the repository owner, open-app presence telemetry (`Presence.kt`) is **disabled** in this private fork.
- **Benefits:** Eliminates recurring 5-minute background network calls, saves device battery, and removes unnecessary network chatter for a 4–6 person personal player.

---

### 2.4 Jam / Shared Listening Room Architecture

#### Primary Engine: Google Firebase Realtime Database (Spark Plan)
- **Status:** **SELECTED as primary Jam engine.**
- **Why Firebase RTDB:**
  - 100% free forever, no credit card required.
  - **Zero Spin-Down:** Unlike Render free containers, Firebase RTDB is a managed cloud database that never sleeps.
  - **Instant Connection:** No 30–50 second cold starts when friends join a room after hours of inactivity.
  - **Native Real-Time Sync:** Android (`com.google.firebase:firebase-database-ktx`) and Web (`@firebase/database`) SDKs handle automatic connection management, offline persistence, and automatic reconnection.
  - **Built-in Server Timestamp:** `ServerValue.TIMESTAMP` ensures perfect NTP clock synchronization across all participant devices without custom ping/pong loops.
  - **Generous Free Capacity:** 100 simultaneous connections (our group needs ~6 connections) and 10 GB/month download egress (sync messages are a few KB).

#### Fallback Engine: Retained Standalone Go Backend (`/backend`)
- **Status:** **RETAINED IN REPO as a backup.**
- **Role:** If Firebase Realtime Database ever encounters an issue or service restriction, the standalone Go server in `/backend` can be deployed immediately to Render Free tier (or any free container host) with zero code modifications.
