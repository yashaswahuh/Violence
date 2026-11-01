# Jam test plan and results

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
**Against the real backend:** NOT RUN. Fill in after deployment:
- 2 clients: ____ / ____ checks, date ____
- 15 clients: ____ / ____ checks, play fan-out latency median ____ ms, date ____
- Cold start measured (ms from first request to answer): ____
- Go unit tests (`go test ./...`), if run on a machine with Go: ____

## 6. Known gaps and risks
- The Go changes and the Dockerfile are uncompiled until the first deploy (or until Go/Docker is installed).
- Render free-plan limits other than those on its docs page (CPU/RAM, outbound bandwidth cap, private repo support, rollback wording) are UNVERIFIED.
- Phone-side wake-up and reconnect logic is covered by builds and by reading the code, not by an automated device test; section 4 is the real test.
- Desktop (dormant) has its own Jam client and was not touched. It has no built-in server unless `LISTEN_TOGETHER_SERVER` (the old name) is set in `local.properties` at its build; it does not read `JAM_BACKEND_URL`. Logged in `FROZEN_TARGETS.md`.
