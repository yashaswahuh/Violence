# Jam audit (Phase 5)

Written 2026-11-02 **before any Phase 5 code change**. Scope: the existing Jam flow on Android phone, Android TV and web against the self-hosted Go backend. Method: read the real code (`ListenTogether.kt`, `PartySync.kt`, `JamInviteLink.kt`, the Jam screens and sheets, `MainActivity` deep-link handling, TV activity/manifest, web `party/*` and `pages/ListenTogetherPage.tsx`, backend `party/party.go` and `main.go`). Nothing here was observed on a real device (UNCLEAR where it says so).

## 1. What already works (do not rewrite)
- **Sync engine is carefully built** (`PartySync.kt`, web `partySync.ts` is a port): outbound only through the user-intent door, inbound writes under it so nothing echoes; scheduled resume (server anchors play 350 ms ahead and `msUntilStart` waits it out); drift corrected only after **2 consecutive readings over 1.2 s**, then a **6 s cooldown**; a control is matched exactly once (`alignedSeq`), queue-only changes never cause a seek; position trusted only after the clock is measured; focus loss detaches the device without publishing; late joiners load the track and seek to `partyPositionMs()`.
- **Traffic is small:** 5 s `state` heartbeat, ping every 15 s, a `report` every 10 s (logged only), `syncQueue` only when `queueSeq` disagrees. No position spam.
- **Reconnect loop** on phone and web: jittered backoff 1 s doubling to 30 s, clock reset and re-measured, queue resynced by `queueSeq`, membership held 45 s by the server; terminal errors (`bad_token`, `no_such_party`, `bye`) leave the party instead of spinning.
- **Roles:** host-only control toggle, kick (with a 24 h local shut-out), capacity setting. **Backend cleanup is sound:** `PartyStore.Sweep` drops expired members and parties, the heartbeat ticker calls `hub.DropParty` for rooms that no longer exist, `PartyMaxAge` 12 h, `EmptyPartyTTL` 2 min. No leak found by reading.
- **Flow coverage (code read):** create, join by code (preview then confirm), join by link (`JamInviteLink.consume` from `onCreate` and `onNewIntent`), member list, shared queue (set/add/remove/move/clear), play/pause/seek/next/previous/setTrack, leave, kick, host handover, rejoin after a dropped socket. Android process restart deliberately **releases** the old slot on `init` (closing the app ends the phone's membership; the web resumes within the grace period).

## 2. Findings
Severity: **HIGH** breaks sync or the main path, **MED** wrong or fragile, **LOW** polish. Fix status is filled in at the end of the phase (`PHASES.md`).

### J1 (HIGH, sync) A track that cannot play on one device pulls that device, and then everyone, out of sync
- **Phone.** `PlaybackService.recoverFrom` gives up on a failed track and calls `skipPastUnplayable`, which calls `exoPlayer.seekToNextMediaItem()` on the *inner* player (not through the session wrapper, so no "user intent"). Then `PartySync.reconcile` sees `currentMediaItem != party track` and `load()`s the failed track again: fail, skip, reload, **a loop on that device**. And the next local pause/seek/skip runs `publish()`, which compares the local track (N+1) against the party (N) and sends **`setTrack(N+1)` to the whole party**. One region-blocked or unresolvable song on one phone can therefore move four other people.
- **Web.** On a load error `usePlayer` sits in `status: 'error'` on the party's track. `reconcile` finds the entry matches, `playWhenReady()` is false, so it calls `partyPlayer.play()` every 700 ms tick, and `play()` on an errored player re-loads the track: **a retry storm** against YouTube from that tab until the party moves on.
- **TV.** Same Android code; TV has no Jam UI today (J5), so not reachable yet.
- **Fix:** an "unplayable for this device" state keyed by the track id. Inside a party the service does not skip; `PartySync` marks the track stalled, publishes nothing, does not reload it, shows a small notice ("Can't play this song here. You'll rejoin on the next one."), and clears the state when the party's track changes (late joins and reconnects included). Web: same state in `partySync.ts`, no retry loop. No protocol change.

### J2 (HIGH, joining) The join-by-code field destroys a pasted link
- **Phone** (`ListenTogetherScreen.kt` `onCodeChange`): keeps only letters/digits then `take(6)`. Pasting `https://…/invite/ABC123` becomes `HTTPSY`. **Web** (`ListenTogetherPage.tsx`): `maxLength={7}` cuts a pasted link to 7 characters, so a link cannot even be pasted.
- **Fix:** one extractor per platform that accepts a bare code, `violence://party/CODE`, `https://…/party/CODE` or `…/invite/CODE` (optional `?server=`), and a whole share message containing either; used by the join field on phone, web and TV. Tests for the extractor.

### J3 (HIGH, invites) The shared link goes to a page that does not handle it
- The link is `JamInviteLink.ORIGIN + "/invite/CODE"` with `ORIGIN = https://yashaswahuh.is-a.dev/violence` (web: `INVITE_ORIGIN`, same). The live site is **`/Violence/`** (GitHub Pages is case sensitive), so the link 404s; and the base is hard-coded, not configurable. The share action only offers the link; the invite sheet has the code as a small tappable text and a QR of the *web* link.
- **Fix:** `JAM_INVITE_BASE_URL` (Android/TV BuildConfig from `local.properties`/env; web `VITE_INVITE_BASE_URL`), **empty = share only the code**. Link shape `<base>/party/<CODE>` (plus `?server=` when the party is on a custom server). Share sheet offers **Share link** (when a base is set), **Share code** and a one-tap **Copy code**; the QR encodes `violence://party/CODE` (opens the app when scanned) when there is no base. New web route `/party/:code` landing page (see J4).

### J4 (HIGH, invites) The web app has no landing page for a party link
- Only `/invite/:code` exists (the join card inside the Listen Together page; it joins in the browser). Nothing offers "Open in the Violence app".
- **Fix:** route `/party/:code` (under the web base path) with the code, **Open in Violence app** (`violence://party/CODE`, one tap, no auto-redirect loop), **Join in this browser**, **Copy code**, `noindex` kept. `/invite/:code` keeps working. Web build and deploy workflows are not touched (a later phase).

### J5 (HIGH for TV, MED overall) Android TV cannot take part in a Jam at all
- `tv/` has no Listen Together screen. `TvActivity` calls `JamInviteLink.consume(intent)` but **nothing on TV reads `JamInviteLink.pending`**, so an invite is silently dropped. The TV manifest still declares the **old author host `bitchord.kushagrasingh.in` and scheme `bitchord`** (REBRAND_LOG claimed it was rebranded; PROJECT_STATE D1), so `violence://party/CODE` does not even reach `TvActivity`.
- **Fix:** manifest to `violence`/owner host/new path patterns; a TV Jam page (D-pad, large layout, 6-character code entry grid, start/join/leave, member list, copy/share code as text on screen) reachable from TV settings and opened automatically by an incoming invite. Not testable here (no TV or emulator): UNCLEAR until run on a TV.

### J6 (MED, backend) Host handover is arbitrary
- `Party.Remove` hands the host role to **the first survivor in Go map order**: random, and it can be a member who is already disconnected. `JAM_DESIGN.md` says "next oldest connected member".
- **Fix:** oldest connected member (lowest `JoinedAtMs` among connected; else oldest overall). Small Go change plus test; not compilable on the owner's PC (no Go), so reviewed by hand.

### J7 (MED, scale) A room cannot hold 15
- `JoinRequest.Validate` caps `maxMembers` at **10**; the phone sheet `coerceIn(2, 10)`; `JAM_MAX_MEMBERS` default 5 (render.yaml 10). The owner sizes for about 15 people.
- **Fix:** protocol max **15** (backend validate and clamp, `render.yaml` 15, phone and web steppers to 15). Tests updated. Backend memory for 15 sockets per room is negligible (see feasibility doc). The in-party member list is a plain column inside a scrolling page and is fine for 15 (checked by reading; not measured).

### J8 (MED, web) Hidden tabs drift and blocked autoplay is not handled
- No `visibilitychange` handling: browsers throttle timers in a hidden tab (and sleep them hard after minutes), so a backgrounded web player drifts and a dropped socket is noticed late. `play()` rejected by the autoplay policy (`NotAllowedError`) is not surfaced: the tab would just be silent.
- **Fix:** on becoming visible, reconcile now and re-open a dead socket; if `play()` is blocked, show a "tap to start audio" notice and retry on the next gesture.

### J9 (LOW) Smaller things
- QR content depends on a web origin that did not work (see J3).
- Shared `JamInvite.ORIGIN` (dead code, only its test uses it) still names the original author's domain (D2): switched to the owner's site and the new `/party/` shape.
- Phone: the not-in-party buttons were already disabled without a server and now explain why (Phase 4). Friendly errors for a sleeping server and an empty `JAM_BACKEND_URL` exist (Phase 4); the missing-room error is the server's message ("No party with that code.") shown in the sheet.
- Web: the Listen Together lede says "up to five devices": updated with J7.

### Not found / UNCLEAR
- No crash or sync bug found in create, leave, kick, rejoin, queue operations or reconnect by reading (they have careful guards and comments); **none of it was run on devices**. Notification/headset controls go through the same session wrapper, so they sync (code read, not tested). Cast and crossfade interplay with a party: UNCLEAR.
- The backend could not be compiled or run (no Go/Docker on the PC). The Jam test script has only been run against a mock until the owner deploys.

## 3. Differences from `PHASES.md` Phase 5 and this prompt
`PHASES.md` Phase 5 described only stalled-track isolation (`playbackStalled`) and the invite-domain update. This phase follows the prompt and adds: invite configuration and web landing page, paste-a-link join, TV Jam screen and manifest, 15-member rooms, deterministic host handover, web visibility/autoplay handling, tests and a two-device checklist. The protocol is **unchanged** except the `maxMembers` ceiling (10 to 15, client and Go backend together).

## 4. Fix status at the end of Phase 5 (2026-11-02)
"Fixed in code" means written, built and unit-tested where a test exists; **nothing was run on a phone, TV, browser or the deployed backend** (see `JAM_TEST_PLAN.md` sections 8-11 for the owner's checks).
| # | Finding | Status |
|---|---|---|
| J1 | Unplayable track loops and drags the party | **Fixed in code, phone (and TV, same service) and web.** `PartySync.onTrackUnplayable` + `PlaybackService` hook; web `stalledVideoId`. Not run on devices. |
| J2 | Join field destroys a pasted link | **Fixed in code**: `JamInviteLink.extractCode` (phone), `extractCode` (web, TV keypad needs no paste). Unit-tested (9 phone tests, 5 web tests). |
| J3 | Invite link goes to a page that does not handle it | **Fixed in code**: `JAM_INVITE_BASE_URL` / `VITE_INVITE_BASE_URL`, empty = share only the code; share link / share code / copy code; QR holds `violence://party/<CODE>` when there is no base. |
| J4 | No web landing page | **Fixed in code**: `/party/:code` (`PartyLandingPage.tsx`): code, Open in Violence app, Join in this browser, Copy code; no auto-redirect; site-wide `noindex` already in `index.html`. Web build/deploy workflows untouched. |
| J5 | TV cannot take part; TV manifest still on the author's host and `bitchord` scheme; cold-start link dropped | **Fixed in code**: manifest to `violence`/owner host/`/Violence/party` etc., `JamInviteLink.consume` in `TvActivity.onCreate`, new `TvListenTogetherDialog` (key grid, start/join/leave, two-column members) opened from Settings or an incoming invite. **Not run on a TV or emulator (UNCLEAR).** |
| J6 | Host handover arbitrary | **Fixed in Go** (oldest connected member) with 2 tests. **Go not run** (no Go on the PC). |
| J7 | A room cannot hold 15 | **Fixed in code**: backend ceiling `config.MaxPartyMembers = 15` (validate, create, set size), `render.yaml` 15, phone/web steppers to 15, default party size 8. Go test added, not run. |
| J8 | Web hidden tab drift, blocked autoplay | **Fixed in code**: `visibilitychange` and `online` call `resyncNow()` + `reconcile()`; blocked `play()` raises a "Tap to start" banner. Not run in browsers. |
| J9 | Smaller items | Shared `JamInvite` points at the owner's site and reads `/party/` and `/invite/` (D2 resolved); QR fixed; web copy "up to 15 devices". |

### Left as is, on purpose or for later
- **The drift/reconcile engine was not refactored** and has no unit test (it mixes the player, the clock and coroutines; a pure-function extraction would risk the sync behaviour that already works). It remains covered only by reading and by the two-device checklist.
- **TV invites carrying `?server=` for a custom Jam server are ignored** (the TV dialog uses the TV's own server setting).
- **Process restart on the phone still ends the membership** (by design, see section 1).
- The `/invite/<CODE>` page served by the Jam backend itself still works, but links no longer point at it.
