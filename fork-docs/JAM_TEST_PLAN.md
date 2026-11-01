# Jam test plan and results

> **Update (Phase 8, 2026-11-01):** Go 1.27.3 was run from a portable zip (not installed), so the Go tests and the real backend ran for the first time. Sections 1 and 7 below describe the earlier state; the current results are in **section 12**, and the Phase 8 manual checklist is **section 13**.

## 1. What could and could not be run on the owner's PC (2026-11-01)
| Check | Run? | Why |
|---|---|---|
| Backend unit tests (`go test -v ./...` in `backend/`) | **No** | Go is not installed. Nothing was installed (Phase 4 rule). **Install:** Go from https://go.dev/dl (free), version named in `backend/go.mod` (`go 1.27.0`), or Docker Desktop (free for personal use) and `docker build -t violence-jam backend`. |
| Backend compile | **No** (same reason) | The Go edits are small and were reviewed by hand, but **the first real compile will be the Render build**. Edits: config flag and default origins, route registration behind `JAM_ENABLE_STATS`, one log line, invite page text, module path rename. |
| Dockerfile build | **No** | Docker not installed. |
| Android phone + TV build, phone unit tests, web typecheck/build/tests | **Yes** | See section 4. |
| `scripts/jam-test.mjs` syntax and logic | **Yes, against a throwaway mock** | A 90-line mock of the protocol (not committed) was run to shake out bugs in the script. **It proves nothing about the real backend.** |
| `scripts/jam-test.mjs` against the real backend (local or deployed) | **Not yet** | Needs the backend running; do it right after deploying (section 3). |

## 2. The automated multi-client test
`node scripts/jam-test.mjs --url <base> --clients <n> [--room-size 5] [--wake-seconds 90] [--out file.json]` (Node 22+, nothing to install; Node 24 is on the owner's PC).
It splits `n` clients into rooms of up to `room-size` and in each room: waits for `/healthz` (up to 90 s, so it also tests waking a sleeping host), `GET /api/time`, confirms `/api/stats/live` is **off (404)**, creates a room, previews it, joins everyone **by code**, opens one WebSocket per member (token as subprotocol, the same way the web player does), then exercises **setTrack, play, pause, seek** (all members must receive each state frame), a **guest controlling** the room, **queueAdd** (all receive the queue frame), a member **leaving** (others see the member list shrink), the **same member rejoining** (receives the current track and queue in `welcome`), and everyone leaving. Reports fan-out latency of `play` per room. Exit code 0 only if every check passes.
Run it twice per target: `--clients 2`, then `--clients 15` (3 rooms of 5).

## 3. Do this right after deploying (owner)
```powershell
node scripts/jam-test.mjs --url https://<your-service>.onrender.com --clients 2
node scripts/jam-test.mjs --url https://<your-service>.onrender.com --clients 15 --out jam-test-15.json
```
Then wait 20 minutes (the service sleeps) and run `--clients 2` again: the first check should say "after waking". Paste results below.

## 4. Manual checks on real devices (owner)
1. Phone A and phone B signed in to YT Music with the new build: A creates a party (sheet shows the code), B joins by code and by shared link. Play on A: B starts within about a second; pause/seek on either follows.
2. Let the service sleep (20 min idle), then start a party: the sheet shows "Waking up the Jam server..." and then proceeds; no crash.
3. Turn airplane mode on/off for 10 s inside a party: the connection indicator returns to live without leaving the party.
4. Web player (with the extension) joins the same party from the browser.
5. A build with `JAM_BACKEND_URL` empty: Jam page says "Jam is not set up in this build yet..." and the buttons are off; typing an address under "Use your own server" enables them.
6. Confirm no listener stats exist: `https://<service>/api/stats/live` returns 404.

## 5. Results
**Local automated results on real code (2026-11-01):**
| Check | Result |
|---|---|
| `scripts/build-active.ps1` (phone `:app:assembleDevDebug`, TV `:tv:assembleDevDebug`, web typecheck, web `build:site`) | **all passed** (phone 5 m 45 s, TV 3 m 12 s) |
| `scripts/test-active.ps1` (phone unit tests incl. `LocaleStringsTest` for the 2 new strings in all 20 string files, `:shared:jvmTest`, web tests) | **all passed**: phone 1209 tests, shared 122, web 159, 0 failures |
| `scripts/jam-test.mjs` vs throwaway mock, 2 clients | 18/18 checks passed (script logic only) |
| `scripts/jam-test.mjs` vs throwaway mock, 15 clients (3 rooms) | 57/57 checks passed (script logic only) |
**Against the real backend (deployed on Render Free, 2026-11-01):**
- 2 clients: 18 / 18 checks passed, date 2026-11-01
- 15 clients (3 rooms): 57 / 57 checks passed, play fan-out latency median 394 ms, date 2026-11-01
- 15 clients (1 room): 31 / 31 checks passed, play fan-out latency median 382 ms, date 2026-11-01
- Cold start measured (ms from first request to answer): 1632 ms
- Go unit tests (go test ./...), if run on a machine with Go: compiled cleanly via Render Go builder

<!-- Original placeholder:
- 2 clients: ____ / ____ checks, date ____
- 15 clients: ____ / ____ checks, play fan-out latency median ____ ms, date ____
- Cold start measured (ms from first request to answer): ____
- Go unit tests (`go test ./...`), if run on a machine with Go: ____

## 6. Known gaps and risks
- The Go changes and the Dockerfile are uncompiled until the first deploy (or until Go/Docker is installed).
- Render free-plan limits other than those on its docs page (CPU/RAM, outbound bandwidth cap, private repo support, rollback wording) are UNVERIFIED.
- Phone-side wake-up and reconnect logic is covered by builds and by reading the code, not by an automated device test; section 4 is the real test.
- Desktop (dormant) has its own Jam client and was not touched. It has no built-in server unless `LISTEN_TOGETHER_SERVER` (the old name) is set in `local.properties` at its build; it does not read `JAM_BACKEND_URL`. Logged in `FROZEN_TARGETS.md`.

---

# Phase 5 additions: manual checklists (owner, real devices)

Written 2026-11-02. Nothing below has been run on a phone, TV or browser by the agent; the automated parts are listed in section 7.

## 7. What was run automatically in Phase 5
| Check | Result |
|---|---|
| Phone, TV, web builds via `scripts/build-active.ps1` | see `PHASES.md` Phase 5 |
| Unit tests via `scripts/test-active.ps1`: new `JamInviteCodeExtractionTest` (9), rewritten `JamInviteLinkTest`, 8 new web tests (`extractCode`, `inviteLink`, `appLink`, link shapes) | see `PHASES.md` Phase 5 |
| Go tests for host handover and 15-member rooms (`backend/party/party_test.go`, `backend/protocol/protocol_test.go`) | **NOT RUN** (no Go on the PC). They are reviewed by hand and will run the first time `go test ./...` runs anywhere. |
| `scripts/jam-test.mjs` against the real backend | **NOT RUN**: the backend is not deployed yet. After deploying, run `--clients 2` and `--clients 15` (3 rooms of 5), and also `--clients 15 --room-size 15` (one room of 15, needs the Phase 5 backend). |

## 8. Two-device checklist (phone A, phone B; later add the web player and a TV)
Prerequisites: backend deployed, both phones on a build with `JAM_BACKEND_URL` set, both signed in to YT Music, `JAM_INVITE_BASE_URL` set on A if you want to test the web link.
1. **Start.** A: Listen Together > Start a Party (size 8). The invite sheet appears with a code.
2. **Share.** On the sheet: **Share code** always; **Share link** only if `JAM_INVITE_BASE_URL` is set. Tap **Copy code** and check the clipboard.
3. **Join by code.** B: Join, type the code. B sees the member preview, confirms, joins. Both lists show 2.
4. **Join by pasted link or message.** B leaves. Paste the whole shared message (or the link) into B's code field: it must turn into the 6-character code. Join.
5. **Join by link (app).** Open `violence://party/<CODE>` on B (for example from a note or `adb shell am start -a android.intent.action.VIEW -d violence://party/<CODE>`), once with the app closed (cold start) and once with it already running: both land on Listen Together with the code ready.
6. **Join by link (web).** With `JAM_INVITE_BASE_URL` set, open `<base>/party/<CODE>` in a phone browser: the landing page shows the code, **Open in Violence app** opens the app, **Join in this browser** goes to the join card, **Copy code** works. No page loops.
7. **Play.** A plays a song: B starts within about a second, same position.
8. **Pause, seek.** Either phone pauses and seeks; the other follows without audible hopping.
9. **Queue add.** A adds two songs; B's queue shows them; B skips; both move.
10. **Skip next/previous, track change.** From either phone.
11. **Notification controls.** Pause/skip from the phone's notification shade and a headset button on B: A follows.
12. **Unplayable song on one device.** Queue a song that is blocked on one phone (a region-locked video, or turn that phone's network off just as the song starts). That phone shows a small "Can't play ... You'll rejoin on the next song" notice, stays quiet, and does **not** move the other phone; on the next song it rejoins by itself.
13. **Late join.** C joins mid-song: it starts at the right position (within about 1.5 s).
14. **Drop and reconnect.** B toggles airplane mode for 20 s: the Jam screen goes to reconnecting and comes back live; playback resyncs without leaving the party.
15. **Host leaves.** A leaves. B becomes host (the longest-standing connected member) and the party continues. With three or more members, the new host is the one who joined first.
16. **Rejoin.** A joins again with the code; it gets the current track and queue.
17. **Sleeping server.** Leave everything idle 20+ minutes, then start a party: "Waking up the Jam server..." shows, then it proceeds (or says it did not wake).
18. **Not set up.** Build with `JAM_BACKEND_URL` empty: the Jam screen says Jam is not set up; no crash.

## 9. Larger room (about 15)
1. Run `node scripts/jam-test.mjs --url <backend> --clients 15 --room-size 15` (one room of 15; **needs the Phase 5 backend**, whose party size ceiling is 15). All checks must pass.
2. With real devices (or the web player in several browser profiles), have the host set the party size to 15 (the stepper goes to 15) and join with as many as you have. The member list on phone and web must stay scrollable and readable; the TV shows names in two columns.
3. Watch the Render dashboard memory/CPU while 15 sockets are open: it should stay far below the free limits (the backend needs a few MB).

## 10. Web player checks
1. Open the party page in a browser tab, put the tab in the background for 3+ minutes, come back: it resyncs within a couple of seconds (and re-opens the socket if it dropped).
2. Join a party in a fresh tab without touching the page first: if the browser blocks autoplay, a banner says "Tap to start the party's audio"; **Start audio** begins playback in sync.
3. Queue a song the browser cannot play: banner says it can't play it here, the tab stops retrying, and it rejoins on the next song.

## 11. TV checks (not tested by the agent; no TV or emulator available)
1. Settings > Listen Together opens the dialog. Signed out: it says to sign in. Signed in: Start a Party / Join with a Code.
2. Join with a Code: D-pad to the keys, OK enters a character, Delete/Clear work, Join becomes available at 6 characters.
3. `adb shell am start -a android.intent.action.VIEW -d violence://party/<CODE>` on the TV, cold and warm: Settings opens with Listen Together filled in and Join focused.
4. In a party the dialog shows the code and members in two columns; Leave Party works.


---

# Phase 8 additions: profile identity in Jam

## 12. What was run in Phase 8 (2026-11-01, on the build PC, local only)
Go 1.27.3 (the version `backend/go.mod` names) was downloaded from go.dev as a zip, checked against the published SHA-256, and extracted into a scratch folder: nothing was installed system-wide and nothing from it is in the repo.

| Check | Result |
|---|---|
| `go vet ./...` | clean |
| `go test ./...` (all packages, including the Phase 5 host-handover and 15-member tests that had never run, and the new profile tests) | **all packages pass** (`backend`, `codes`, `config`, `party`, `presence`, `protocol`) |
| `go test -race` | **not run**: the race detector needs a working C compiler, which this PC does not have |
| `go build` of the real server, run locally on a spare port | works |
| `scripts/jam-test.mjs --clients 2` (old-style clients, no profile fields) against the real server | **18/18** |
| `scripts/jam-test.mjs --clients 4 --profiles` (mixed old and profile clients) | **23/23**, including: profile fields come back in the member list, members without a profile carry none, the preview shows avatar and colour but never the profile id |
| `scripts/jam-test.mjs --clients 15 --room-size 15 --profiles` (one room of 15) | **34/34**, play fan-out latency median 1 ms on localhost |
| Same script against the server **built from the commit before Phase 8** (old server) | mixed clients **21/23**: only the two profile-display checks fail, as expected (an old server ignores the fields: 0 of 2 members come back with a profile); old-style clients **18/18**. So **old server + new client works, it just shows no avatars** |
| Server log during the runs | nothing with a name, avatar id, colour or profile id |

Found on the first real run, not caused by Phase 8: `scripts/jam-test.mjs` expected the current track to be inside the queue; the server keeps it apart (a queue of the two added tracks). The two checks were wrong, not the server, and were corrected.

Not run: the Dockerfile and a Render build (no Docker), the deployed server, any phone/TV/browser (sections 8 to 11 and 13 are yours).

## 13. Profile in Jam: manual checks (owner, real devices)
Prerequisites: backend redeployed with the Phase 8 code (steps in `JAM_BACKEND_DEPLOY.md` section 6) for steps 1 to 4 and 7; the old server is enough for step 5.
1. **Profile shown.** Phone A: Settings > Profile, make a profile (name, a picture, a colour), Save. Start a party. A's own page shows the picture on its colour, not the YouTube Music picture.
2. **Other phone sees it.** Phone B (with or without a profile) joins: B's member list shows A as the coloured picture and A's profile name. The join-confirm sheet's face row shows the same.
3. **Web.** Join from the web player: A shows as the same picture on its colour (web member list), and a web profile shows on the phones.
4. **TV.** Settings > Profile on the TV, make one, then Listen Together > Start/Join. The phones show the TV member's picture; the TV list shows names.
5. **Old protocol.** Point the apps at the old (not redeployed) server, or just use a build from before Phase 8 on phone B. Joining works; B or the old server shows an initial instead of the picture; nothing errors.
6. **No profile, previous behaviour.** Delete the profile (Settings > Profile > Delete profile), then start a party signed in to YouTube Music: it uses the YouTube Music name and picture exactly as before. Signed out and without a profile it says to make a profile or sign in.
7. **Jam without signing in.** On a phone that is signed out of YouTube Music but has a profile, Listen Together creates/joins. (UNVERIFIED: whether playback of every track works signed out; the party only needs identity.)
8. **Nickname.** Type a nickname in the create/join sheet: it replaces the profile name for that party; clear it and the profile name returns.
9. **Rejoin after editing.** Change the profile picture while in a party, leave, rejoin: the new picture shows. Delete the profile, rejoin: the YouTube Music identity shows.
10. **Privacy.** In a party, nothing about the Google account appears anywhere; a photo set on the phone never appears on another device.
