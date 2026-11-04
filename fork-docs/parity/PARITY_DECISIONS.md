# Parity Decisions: Owner Template (DW-0)

**Date:** 2026-11-04  
**Project:** Violence ("Desktop and Web Parity", DW-0)  
**Status:** Awaiting owner decisions. Fill in the bracketed choices below before starting DW-1 / DW-2.

---

## 1. Feature Porting Choices

Select which missing or unwired features you want ported to Desktop and Web during the DW stages.

### 1.1 In-App Profiles and Friends
Phase 8 added local profiles (`InAppProfile`: name, preset avatar emoji, accent color) and local friends lists (`FriendCard`), used as member identity in Jam without needing a Google/YouTube Music sign-in.
- [ ] **Desktop:** Port in-app profile creation, avatar picker, and local friends storage (Recommended: YES)
- [ ] **Web:** Retain and polish Phase 8 profile/friends implementation (Recommended: YES)
*Decision:* [YES / NO / CUSTOM]

### 1.2 Jam (Listen Together) Modernization on Desktop
Desktop currently runs a legacy client (points to old `LISTEN_TOGETHER_SERVER`, max 5 members, no wake-up notice, no link joining, no stalled-track isolation).
- [ ] Update Desktop to read `JAM_BACKEND_URL` and support 15-member rooms (Recommended: YES)
- [ ] Add 85-second "Waking up server..." retry notice for Render Free sleeping backend (Recommended: YES)
- [ ] Add pasted-link and `violence://party/` code extraction (Recommended: YES)
- [ ] Add unplayable-track isolation so one client's error does not desync party (Recommended: YES)
*Decision:* [YES / NO / CUSTOM]

### 1.3 Sharing and Link Handling
- [ ] **Desktop Sharing Dialog:** Add Song, Playlist, and Jam sharing dialogs with QR code rendering and "Copy Link" buttons (Recommended: YES)
- [ ] **Desktop Protocol Handler (`violence://`):** Register the `violence://` protocol in the Windows registry / OS file so clicking links in Discord/browser opens the desktop app (Recommended: YES)
- [ ] **Clipboard "Receive" Screen:** A dedicated Receive action on Desktop and Web that automatically reads and parses a pasted or clipboard link (Recommended: YES)
*Decision:* [YES / NO / CUSTOM]

### 1.4 Spotify and Apple Music Import
- [ ] **Spotify Playlist Link Import on Desktop:** Wire the existing `DesktopSpotifyLibrary` into a user-facing import dialog matching the phone app (Recommended: YES)
- [ ] **Apple Music Link and File Import:**
  - *Option A:* Build full Apple Music link scraper (requires CORS proxy / extension bridge on Web, and iTunes lookup on Desktop) and M3U file import.
  - *Option B:* Defer Apple Music import to a separate dedicated project; keep Spotify import for now.
*Decision:* [Option A / Option B] (Recommended: Option B for DW stages, or Option A if prioritized)

---

## 2. Violence Style Decisions for Desktop & Web

### 2.1 Web Implementation Tier
- *Option A (Recommended):* **LITE Style on Web.** Uses CSS radial gradients for background glow and native CSS `backdrop-filter: blur(16px)` for glass panels. Fast, low memory, zero battery drain.
- *Option B:* **FULL Style on Web.** Canvas 2D or WebGL rendering with continuous 30/60 fps animation and multi-pass blurs. (Higher CPU/battery usage in browser).
*Decision:* [Option A / Option B]

### 2.2 Desktop Implementation Tier
- *Option A (Recommended):* **FULL Style with LITE Fallback.** Live radial gradient background via Skia hardware canvas + Haze / Skia glass. Automatic fallback to LITE (solid tint + accent rim) on low-spec hardware or if user enables Reduce Transparency.
- *Option B:* **LITE Style only on Desktop.** Semi-translucent surfaces with accent rim strokes, no live background animation.
*Decision:* [Option A / Option B]

### 2.3 Light Appearance Handling
Violence Style is tuned as a luminous dark skin (`#05050A` canvas). How should Desktop and Web behave when the user selects Light theme?
- *Option A (Recommended):* **Force Player to Dark Canvas.** When Violence Style is enabled, the player and background remain in dark luminous mode (like Apple Music Dark / Spotify), regardless of OS theme.
- *Option B:* **Inverted Light Glass Palette.** Create a light glass variant (`#FFFFFF` translucent background with dark saturated accent colors and black text).
- *Option C:* **Auto-disable Style in Light Mode.** If user switches to Light mode, Violence Style turns off automatically until Dark mode is active.
*Decision:* [Option A / Option B / Option C]

---

## 3. Platform Polish & Substitutes

### 3.1 Desktop System Integration
- [ ] Enable system tray icon with minimize-to-tray and playback controls menu (Recommended: YES)
- [ ] Windows SMTC media keys and macOS/Linux MPRIS integrations (Recommended: YES)
- [ ] Global / window keyboard shortcuts (Space for pause, Ctrl+F for search, etc.) (Recommended: YES)
*Decision:* [YES / NO / CUSTOM]

### 3.2 Desktop In-App Update Notice
- [ ] Check `update.json` from `UPDATE_INFO_URL` once per day and show an in-app banner with a download button when a newer desktop version is released (Recommended: YES)
*Decision:* [YES / NO / CUSTOM]

---

## 4. Priority Order of Proposed Stages

Confirm the proposed execution sequence:
1. **DW-1:** Desktop compile health & unfreeze housekeeping (Fix `DesktopApp.kt` exhaustive when, verify all tests)
2. **DW-2:** Logic features on Desktop & Web (Jam, profiles, friends, sharing dialogs, imports)
3. **DW-3:** Style foundation (Shared math, state holder, settings, tokens, fonts) with 100% OFF parity
4. **DW-4:** Live background, glass chrome, and styled components
5. **DW-5:** Platform extras (Tray, media keys, keyboard navigation, PWA, installer docs)
6. **DW-6:** Final audit, accessibility, performance benchmarks, and docs handover

*Do you approve this sequence?* [APPROVED / REVISE (specify changes)]
