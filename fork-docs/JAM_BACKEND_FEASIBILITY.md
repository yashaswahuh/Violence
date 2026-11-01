# Jam backend feasibility (Phase 4)

Written 2026-11-01, **before any Phase 4 code change**. Verdict at the end: **GO** (free hosting is feasible without a credit card).

## 1. What the client's Jam feature uses (verified in code)
Client code read: `app/.../data/listentogether/ListenTogether.kt`, `JamInviteLink.kt`, `playback/PartySync.kt`, `shared/.../listentogether/*` (Android + TV share these), `web/app/src/party/*`. Desktop has its own client (`DesktopListenTogether.kt`), dormant, not read.

**HTTP**
| Call | Used by | Backend |
|---|---|---|
| `GET /healthz` (body must contain `"ok":true`) | Android health probe (`probeHealthWithLatency`), TV (same code), web | implemented (`handleHealthz`) |
| `POST /api/parties` (create) | Android, web | implemented |
| `POST /api/parties/{code}/join` | Android, web | implemented |
| `GET /api/parties/{code}/preview` | Android (QR/deep-link preview), web | implemented |
| `POST /api/parties/{code}/leave` (Bearer token) | Android, web | implemented |
| `GET /api/parties/{code}` (Bearer token) | web | implemented |
| `GET /api/time` | clock sampling REST fallback | implemented |
| `GET /invite/{code}` (HTML landing for shared links) | opened in a browser | implemented (still BitChord text, see Phase 4 changes) |
**WebSocket** `GET /ws/parties/{code}`. Android/TV send `Authorization: Bearer <token>`; browsers offer subprotocols `bitchord.v1, bitchord.token.<token>` (the `bitchord.v1` string is part of the wire protocol: do not rename). Browsers must come from an allowed Origin; native apps send no Origin.
**Client -> server frames:** `ping`, `syncQueue`, `report`, `control` with actions `play pause seek setTrack setQueue queueAdd queueRemove queueClear queueMove next previous kick setMaxMembers setAutoplay setHostOnlyControl`. **Server -> client:** `welcome state queue members pong error bye activity`. **Gap check:** every call and frame above exists in `backend/main.go` and `backend/protocol/protocol.go` (`sync` also exists server-side; the clients do not send it). **No gaps.**
**Changes in the v1.9 upstream sync:** `backend/protocol|party|hub` unchanged; only presence (browser platform) and `deploy/Caddyfile` changed. Jam client files changed only in UI. So the backend in `/backend` matches the v1.9 clients.
**Not Jam, and not needed:** `POST /api/presence`, `GET /api/stats/live`, `GET /api/stats/live/badge.svg` (open-app counter, DISABLE per `ALLOWED_EXTERNAL_SERVICES.md`).

## 2. What the backend needs to run
- **Language:** Go. `backend/go.mod` says `go 1.27.0`. Only dependency: `github.com/gorilla/websocket v1.5.3`. No cgo (static binary builds with `CGO_ENABLED=0`).
- **Databases / caches:** none. **Persistent disk:** none. **Background jobs:** in-process goroutines only (5 s state heartbeat ticker, presence sweeper, party expiry).
- **Port:** `PORT` env (default 8000), binds `0.0.0.0`.
- **Memory:** ~12-18 MB idle, ~35-50 MB at 250 sockets (`BACKEND_AUDIT.md`). For about 15 users in a few rooms: negligible CPU and a few MB.
- **Environment variables** (all optional, `backend/config/config.go`): `PORT`, `JAM_MAX_MEMBERS` (default 5), `JAM_STATE_HEARTBEAT_MS`, `JAM_PLAY_LEAD_MS`, `JAM_DISCONNECT_GRACE_MS` (45 s), `JAM_EMPTY_PARTY_TTL_MS` (2 min), `JAM_PARTY_MAX_AGE_MS` (12 h), `JAM_CONTROL_RATE_PER_SECOND`, `JAM_FRAME_RATE_PER_SECOND`, `JAM_MAX_UPCOMING_QUEUE`, `JAM_MAX_QUEUE_LENGTH`, `JAM_MAX_PARTIES` (50), `JAM_CREATE_RATE_PER_MINUTE` (2 per IP), `JAM_RATE_LIMIT_MAX_ENTRIES`, `JAM_REQUEST_MAX_BYTES`, `JAM_WEBSOCKET_MAX_BYTES`, `JAM_CONNECTION_IDLE_MS` (15 min), `JAM_ALLOWED_ORIGINS` (default empty = no browser origin allowed), `JAM_TRUST_PROXY`, `JAM_PUBLIC_ORIGIN`, and the presence settings. **No secrets or API keys anywhere.**
- **Sizing note:** the default `JAM_MAX_MEMBERS=5` would block a sixth core listener. Core group is 4-6 and the protocol allows up to 10 (`JoinRequest.Validate`), so the deployment sets `JAM_MAX_MEMBERS=10`.
- **Where rooms live:** **in memory only** (`party.PartyStore`, `hub.Hub`, `ipRateLimiter`). **Lost on restart, redeploy, crash, or host sleep:** all rooms, member slots, tokens and queues vanish; clients get a failed reconnect and must create a new room. A room with zero connected members is deleted after 2 minutes anyway, and any room after 12 hours. On Render Free the process also restarts "at any time" (docs), so Jam must be treated as "start a party when you want to listen", not durable.

## 3. Login data in Jam messages: none
Searched the Jam client, shared and web party code for cookies, SAPISID, `sp_dc`, OAuth and Spotify: nothing is sent. The only credential is the **random bearer token the Jam server itself mints** per member. Playback itself never goes through the Jam server (each device resolves audio on its own). **Nothing to remove.**

## 4. Member identity fields (relevant to the later in-app profiles phase)
`JoinRequest` (create and join) sends: `userId`, `deviceId`, `displayName`, `avatarUrl` (optional), `maxMembers` (create only), `autoplayEnabled` (optional). The server echoes members as `memberId, userId, displayName, avatarUrl, isHost, connected, joinedAtMs, lastSeenMs` plus `startedBy`/`startedByName` in state.
Where they come from on the client:
- **`userId`:** Android `sha256(accountId + ":" + profileId).take(32)` from the signed-in **YT Music / Google session** (`AuthStore.activeSession`); web `sha256(authUser + ":" + accountId).slice(0,32)`. Hashed, so the server never holds a real Google id.
- **`deviceId`:** random UUID kept in app prefs (Android `KEY_DEVICE`) / `localStorage` (`bitchord:deviceId`) on web.
- **`displayName`:** the Jam nickname if set (`nickname()`), else the YT Music profile/channel name, else the Google account name, else the part of the email before `@`.
- **`avatarUrl`:** the active YT Music profile avatar if it is an http(s) URL.
- **Important for a later phase:** `ListenTogether.identity()` returns null (so create/join is refused) when nobody is signed in to YT Music. Today, Jam requires a YT Music sign-in on every device. An in-app profile system would be the place to change that.

## 5. Custom server setting already exists (reuse it)
`ListenTogether` already persists a user-set server (`KEY_SERVER` = `server_url`, `customServerUrl`, editor `PartyServerEditor.kt`, strings "Use your own server" etc.), probes `/healthz` and falls back to the build default; the web has `partySettings().customServer`. Phase 4 reuses these: it only changes **what the built-in default is** (`JAM_BACKEND_URL`, empty by default) and does not add a new mechanism.
Defects found: (a) the built-in default is still the original author's `bitchord-listen-together.onrender.com` (Android/TV, in `app`/`tv` `build.gradle.kts`) and `api.bitchord.kushagrasingh.in` (web, via `web.yml`); (b) an empty value was already handled on Android (blank parses as valid-empty, `hasServer` is false and the buttons are disabled) but a *malformed* non-empty value crashed the app at startup (`error("Invalid BuildConfig.LISTEN_TOGETHER_SERVER")`), and with no server there was no explanation on screen; (c) the default health probe waits 30 s but there is no "waking up" state or retry; (d) the web build refuses to run without `VITE_PARTY_SERVER_URL`, and an empty web address would send requests to the site itself. TV shares the Android Jam code but has **no Jam screen**; desktop (dormant) has its own client whose default comes only from `LISTEN_TOGETHER_SERVER` in `local.properties` (no author URL).

## 6. Free host candidates (checked 2026-11-01 against official pages; items not stated in docs are UNVERIFIED)
| Host | Card? | Free plan | Sleep / cold start | WebSocket | Verdict |
|---|---|---|---|---|---|
| **Render (Free web service)** | **Not required**: docs mention a payment method only for overage ("exceed bandwidth or build minutes without a payment method -> free services suspended for the rest of the month") | 750 free instance hours per workspace per month (no rollover), 1 instance, 512 MB / 0.1 CPU per upstream `render.yaml`/BACKEND_AUDIT (RAM/CPU not stated on the free page: UNVERIFIED), ephemeral disk, may restart at any time | **Spins down after 15 min without inbound traffic** (HTTP requests and WebSocket messages count as traffic); spin-up "takes about one minute" | Supported ("WebSocket connections" listed for web services; a new WebSocket connection wakes a sleeping service) | **CHOSEN primary.** The backend was already tuned for it (`render.yaml`, `MaxParties` comment). Fits ~15 users: one process, a few MB, 5 s heartbeats. Private-repo deploys: UNVERIFIED (Render's GitHub app normally supports them). |
| **Northflank (Sandbox)** | "No card needed" (pricing page) | 2 services, up to $20/mo of resources, Docker builds | **"Always on, no sleeping"** | Networking docs list WebSockets as supported but another page says only HTTP/HTTP2 ports can be public: **UNVERIFIED, test before relying** | **FALLBACK #1.** Better UX if WebSockets work (no cold start). The pricing page showed a security-incident banner; the backend holds no secrets, so exposure is low (UNVERIFIED details). |
| Fly.io | **Required** ("All organizations require a credit card"; no free tier for new orgs) | none | n/a | yes | Rejected (card). |
| Koyeb | **Required** ("credit card required... $29 pre-authorization hold") | 1 free web service, 512 MB / 0.1 vCPU | unstated | unstated | Rejected (card). |
| Hugging Face Spaces (Docker) | Paid plan required to create Docker Spaces | n/a | sleeps | - | Rejected (paid; public Spaces also publish source). |
| Railway | no card, but one-time $5 credit only | trial | pauses when credit ends | yes | Rejected (not free forever). |
| Cloudflare Workers + Durable Objects | free plan exists; needs a JS rewrite | - | - | - | Rejected (would rewrite the protocol; `FREE_HOSTING_PLAN.md` already rejected). |
| Own PC / old phone + Cloudflare Tunnel (free) | no | free | always on while the machine is on | yes | Possible last resort, not chosen (needs a machine that is always on). |
| Firebase RTDB (Spark) | no | free | none | n/a (different protocol) | **Documented fallback only. Not built in this phase.** |

## 7. 15 simultaneous users on Render Free
~15 sockets across 2-4 rooms. Per socket: a 5 s `state` frame (~80 bytes), 15 s pings, 10 s reports: well under 1 KB/s per user. Create is limited to 2 per minute per IP (a home network shares one IP: fine, but raise it if friends create rooms in bursts). `JAM_MAX_PARTIES=50` is far above need. CPU 0.1 is enough for this load (BACKEND_AUDIT estimates under 1% CPU for 4-6 users). Monthly: 750 instance hours; a service kept awake all month uses about 744 h, so **only one always-awake free service fits per workspace** (see the pinger caveat in the deploy guide). Outbound bandwidth cap is not stated numerically on the page (UNVERIFIED); Jam traffic is tiny.

## 8. Verdict: GO
A free host exists that runs this backend with WebSockets and no credit card (Render Free), with a no-sleep fallback worth testing (Northflank Sandbox) and Firebase as the documented last fallback. Costs the owner accepts with Render Free: a ~1 minute cold start after 15 idle minutes, and rooms lost on any restart.
**Limits of this verification:** Go and Docker are not installed on the owner's PC, so the backend could not be compiled or tested locally; see `JAM_TEST_PLAN.md`.
