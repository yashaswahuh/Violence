# Backend Technical Audit & Specification

**Audit Date:** 2026-10-29  
**Source Directory:** `/backend`  
**Go Module:** `github.com/KabirSinghBhatia/BitChord/backend` (`backend/go.mod`)  
**Language & Runtime:** Go 1.27 (`go 1.27.0` in `go.mod`, compatible with Go 1.22+)  
**Primary Dependency:** `github.com/gorilla/websocket v1.5.3`

---

## 1. Overview, Execution & Deployment

The `backend` directory contains an independent, lightweight Go service that performs two functions:
1. **Listen Together (Jam):** An in-memory room hub synchronizing playback state, queue modifications, and timeline anchors across devices via WebSockets and REST.
2. **Presence & Live Stats:** An open-app heartbeat counter tracking active installations and rendering an SVG status badge for markdown presentation.

### Execution
- **Local Run:** `go run .` (runs on `0.0.0.0:8000` by default).
- **Test Suite:** `go test -v ./...` (comprehensive unit tests in `main_test.go`, `party/party_test.go`, `presence/presence_test.go`, `config/config_test.go`).
- **Binary Build:** `CGO_ENABLED=0 go build -ldflags="-s -w" -o server .` (produces a single statically linked binary ~12 MB).

### Upstream Deployment Setup
- **Production Host:** Oracle Cloud Always Free `VM.Standard.E2.1.Micro` instance (Ubuntu 24.04 in `ap-mumbai-1`) serving `https://api.bitchord.kushagrasingh.in`.
- **System Service:** `deploy/bitchord-jam.service` running as systemd unit on `:8000`.
- **Reverse Proxy:** Caddy (`deploy/Caddyfile`) handling automated Let's Encrypt TLS termination and WebSocket proxying.
- **Continuous Deployment:** `.github/workflows/deploy-backend.yml` SSHs into the Oracle VM via `ORACLE_DEPLOY_KEY` and runs `/usr/local/sbin/bitchord-deploy`.
- **Alternative Blueprint:** `backend/render.yaml` defining a Render Web Service (`runtime: go`).

---

## 2. Complete Endpoint Catalog

All REST responses return `application/json` with camelCase fields to match Kotlin Multiplatform models directly.

### 2.1 Service & Health Endpoints

#### `GET /`
- **Handler:** `handleRoot` (`main.go:247`)
- **Purpose:** Service identification and diagnostic counts.
- **Auth:** None.
- **Request:** None.
- **Response (`200 OK`):**
  ```json
  {
    "service": "bitchord-listen-together",
    "members": 3,
    "parties": 1,
    "serverMs": 1757630001234
  }
  ```
- **Calling Code:** Browser navigation, health monitors.

#### `GET /healthz`
- **Handler:** `handleHealthz` (`main.go:260`)
- **Purpose:** Container orchestration and deployment health probe.
- **Auth:** None.
- **Response (`200 OK`):**
  ```json
  {
    "ok": true,
    "serverMs": 1757630001234
  }
  ```
- **Calling Code:** Render health checks, `.github/workflows/deploy-backend.yml:43`.

#### `GET /api/time`
- **Handler:** `handleTime` (`main.go:267`)
- **Purpose:** Obtains server monotonic millisecond timestamp before WebSocket establishment for initial NTP-like clock offset estimation.
- **Auth:** None.
- **Response (`200 OK`):**
  ```json
  {
    "serverMs": 1757630001234
  }
  ```
- **Calling Code:** `ServerClock.kt:sampleRest()`, `ListenTogether.kt`.

---

### 2.2 Listen Together (Jam) REST Endpoints

#### `POST /api/parties`
- **Handler:** `handleCreateParty` (`main.go:344`)
- **Purpose:** Creates a new party room and registers caller as host.
- **Auth:** None (IP rate limited to `JAM_CREATE_RATE_PER_MINUTE`, default 2/min).
- **Request Body (`protocol.JoinRequest`):**
  ```json
  {
    "userId": "google_1029384756",
    "deviceId": "android_98a7c6b5d4e3f2a1",
    "displayName": "Alice",
    "avatarUrl": "https://lh3.googleusercontent.com/...",
    "maxMembers": 5,
    "autoplayEnabled": true
  }
  ```
- **Response (`201 Created`):**
  ```json
  {
    "code": "AB12CD",
    "token": "4f9a8b7c6d5e...",
    "you": {
      "memberId": "m_1a2b3c",
      "userId": "google_1029384756",
      "deviceId": "android_98a7c6b5d4e3f2a1",
      "displayName": "Alice",
      "avatarUrl": "https://lh3.googleusercontent.com/...",
      "isHost": true,
      "joinedAtMs": 1757630001000,
      "connected": false
    },
    "party": {
      "code": "AB12CD",
      "createdAtMs": 1757630001000,
      "maxMembers": 5,
      "hostOnlyControl": false,
      "members": [ ... ],
      "playback": {
        "seq": 1,
        "track": null,
        "positionMs": 0,
        "anchorMs": 1757630001000,
        "isPlaying": false,
        "updatedBy": "m_1a2b3c",
        "autoplayEnabled": true,
        "queueSeq": 1,
        "queueIndex": 0,
        "queueLength": 0
      },
      "queue": {
        "seq": 1,
        "index": 0,
        "items": []
      }
    }
  }
  ```
- **Calling Code:** `ListenTogether.createParty()` in `app/src/main/java/.../ListenTogether.kt:384`, `web/app/src/party/listenTogether.ts:182`.

#### `POST /api/parties/{code}/join`
- **Handler:** `handleJoinParty` (`main.go:401`)
- **Purpose:** Joins an existing party using a 6-character room code.
- **Auth:** None (code normalized via `codes.Normalise()` resolving lowercase, spaces, and `0/O`, `1/I/L` homoglyphs).
- **Request Body:** Same `protocol.JoinRequest` structure as create party.
- **Response (`200 OK`):** Same JSON envelope as `POST /api/parties` (returns newly minted bearer `token` for this device).
- **Error Responses:** `404 no_such_party`, `409 party_full` (capacity reached), `422 validation_error`.
- **Calling Code:** `ListenTogether.joinParty(code)` in `app/.../ListenTogether.kt:412`, `web/app/src/party/listenTogether.ts:208`.

#### `GET /api/parties/{code}`
- **Handler:** `handleGetParty` (`main.go:507`)
- **Purpose:** Fetches full party snapshot (membership, playback state, and queue).
- **Auth:** `Authorization: Bearer <token>` required.
- **Response (`200 OK`):** Full `partyWire` JSON object.
- **Calling Code:** Client state resynchronization on reconnect.

#### `GET /api/parties/{code}/preview`
- **Handler:** `handlePreviewParty` (`main.go:459`)
- **Purpose:** Public inspection of party members and host before joining (no device slot consumed).
- **Auth:** None.
- **Response (`200 OK`):**
  ```json
  {
    "code": "AB12CD",
    "hostName": "Alice",
    "memberCount": 2,
    "maxMembers": 5,
    "isFull": false,
    "members": [
      {
        "displayName": "Alice",
        "avatarUrl": "https://...",
        "isHost": true
      },
      {
        "displayName": "Bob",
        "avatarUrl": "https://...",
        "isHost": false
      }
    ]
  }
  ```
- **Calling Code:** Deep link landing dialogs, QR code scanner previews (`PartyPreviewDialog.kt`).

#### `POST /api/parties/{code}/leave`
- **Handler:** `handleLeaveParty` (`main.go:542`)
- **Purpose:** Explicit slot surrender by a member.
- **Auth:** `Authorization: Bearer <token>` required.
- **Response (`200 OK`):** `{"ok": true}`.
- **Side Effect:** Dispatches WebSocket `bye` frame (`reason: "left"`) to leaver and updated `members` frame to remaining party. Passes host to next senior member if host departs.
- **Calling Code:** `ListenTogether.leaveParty()` in `app/.../ListenTogether.kt:495`.

---

### 2.3 Presence & Live Stats Endpoints

#### `POST /api/presence`
- **Handler:** `handlePresence` (`main.go:275`)
- **Purpose:** Open-app telemetry heartbeat from active applications.
- **Auth:** None. **Explicitly rejects requests with an `Origin` header** (`403 origin_not_allowed`) to prevent malicious web pages from spoofing app counts. Rate limited to 20 pings/min per IP.
- **Request Body:**
  ```json
  {
    "id": "c7a8b9d0-1234-5678-90ab-cdef12345678",
    "platform": "android",
    "open": true
  }
  ```
  *(Platform must be `"android"` or `"pc"`; `id` must be a valid 36-char canonical UUID; `open` is boolean).*
- **Response (`200 OK`):**
  ```json
  {
    "intervalSec": 300
  }
  ```
  *(Dynamic backoff: client uses returned interval for subsequent pings).*
- **Calling Code:** `Presence.kt:send()` in `shared/src/jvmSharedMain/.../Presence.kt:87`.

#### `GET /api/stats/live`
- **Handler:** `handleLiveStats` (`main.go:318`)
- **Purpose:** Public endpoint returning open app counts.
- **Auth:** None (`Access-Control-Allow-Origin: *`, `Cache-Control: public, max-age=15`).
- **Response (`200 OK`):**
  ```json
  {
    "online": 42,
    "android": 35,
    "pc": 7,
    "updatedAt": 1757630001234
  }
  ```
- **Calling Code:** Web status widgets, external dashboards.

#### `GET /api/stats/live/badge.svg`
- **Handler:** `handleLiveBadge` (`main.go:331`)
- **Purpose:** Renders dynamic SVG "LISTENING NOW: X" badge for GitHub README.
- **Auth:** None (`Cache-Control: no-cache, no-store, must-revalidate, max-age=0`).
- **Response (`200 OK`):** `image/svg+xml;charset=utf-8` containing dynamically generated vector badge matching shields.io `for-the-badge` geometry.
- **Calling Code:** `README.md:17`.

#### `GET /invite/{code}`
- **Handler:** `handleInviteLanding` (`main.go:805`)
- **Purpose:** HTML landing page for room sharing links.
- **Auth:** None.
- **Response (`200 OK` / `404 Not Found`):** Rendered HTML template (`inviteTemplate`) containing deep links (`bitchord://party/{code}?server={origin}`), Android Intent URIs (`intent://party/{code}...`), current playing song title/artist, active member count, and direct link to APK download.
- **Calling Code:** Mobile web browser when user taps shared invite link.

---

### 2.4 WebSocket Endpoint: `/ws/parties/{code}`

- **Handler:** `handleWebSocket` (`main.go:858`)
- **Authentication Protocol:**
  - **Native Apps (`app`, `desktopApp`):** Send `Authorization: Bearer <token>` HTTP header during WebSocket handshake.
  - **Web Browser Client (`web/app`):** Browsers cannot set custom headers during WebSocket handshake. Instead, sends token via standard subprotocol header:
    `Sec-WebSocket-Protocol: bitchord.v1, bitchord.token.<token>`
    Server parses token prefix, upgrades connection, and negotiates subprotocol response to `bitchord.v1`.
  - **Origin Validation:** If `Origin` header is present, checked against `JAM_ALLOWED_ORIGINS`. Native apps send no Origin and pass unconditionally.

#### Protocol Frames (Client ➔ Server)

| Frame Type (`type`) | Payload Properties | Purpose | Enforced Rules |
|---|---|---|---|
| `ping` | `clientMs: number` | NTP clock measurement | Server replies with `pong` retaining `clientMs` + server stamp. |
| `sync` | *(none)* | Requests current playback state | Server replies with immediate `state` frame. |
| `syncQueue` | *(none)* | Requests full queue synchronization | Sent when client detects its local `queueSeq != state.queueSeq`. |
| `report` | `positionMs: number, isPlaying: bool` | Telemetry playhead report | Logged only; server never modifies room playhead from reports. |
| `control:play` | `action: "play", positionMs?: number` | Resumes playback | Anchored `JAM_PLAY_LEAD_MS` into future. Subject to `hostOnlyControl`. |
| `control:pause` | `action: "pause", positionMs?: number` | Pauses playback | Halts playhead progression. Subject to `hostOnlyControl`. |
| `control:seek` | `action: "seek", positionMs: number` | Jumps to track position | Required `positionMs`. Anchored forward. Subject to `hostOnlyControl`. |
| `control:setTrack` | `action: "setTrack", track: Track, positionMs?: number, isPlaying?: bool` | Changes current song | Updates `startedBy`, resets position to 0, bumps `seq`. |
| `control:setQueue` | `action: "setQueue", queue: Track[], queueIndex: number` | Overwrites entire room queue | Clamped to `JAM_MAX_QUEUE_LENGTH` (default 26 tracks). |
| `control:queueAdd` | `action: "queueAdd", tracks: Track[], playNext: bool` | Appends or inserts tracks | Drops duplicate Autoplay items; capped at 25 upcoming tracks. |
| `control:queueRemove`| `action: "queueRemove", videoId: string` | Removes track by ID | Adjusts active index if removed track precedes current. |
| `control:queueClear` | `action: "queueClear"` | Clears upcoming queue | Preserves active playing track. |
| `control:queueMove` | `action: "queueMove", fromIndex: int, toIndex: int, videoId?: string` | Reorders track in queue | Validates `videoId` under room lock to prevent stale-index drag race. |
| `control:next` | `action: "next"` | Advances to next track | Increments `queueIndex`, bumps `seq`. |
| `control:previous` | `action: "previous"` | Returns to prior track | Decrements `queueIndex`, bumps `seq`. |
| `control:setHostOnlyControl`| `action: "setHostOnlyControl", enabled: bool` | Locks controls to host | **Host only** (`403 host_only` error if attempted by guest). |
| `control:setMaxMembers`| `action: "setMaxMembers", maxMembers: int` | Changes room size | **Host only** (clamped between 2 and `JAM_MAX_MEMBERS`). |
| `control:kick` | `action: "kick", memberId: string` | Ejects guest member | **Host only**. Drops target socket, sends `bye` (`reason: "kicked"`). |

#### Protocol Frames (Server ➔ Client)

| Frame Type (`type`) | Payload Properties | Purpose |
|---|---|---|
| `welcome` | `you: Member, party: PartyWire, serverMs: number` | Initial state handshake immediately following connection upgrade. |
| `pong` | `clientMs: number, serverMs: number` | Clock synchronization response. |
| `state` | `playback: PlaybackStateWire, serverMs: number` | Periodic heartbeat (every 5s) and control broadcast. Carries monotonic `seq`. |
| `queue` | `queue: QueueWire, serverMs: number` | Broadcast only when queue membership or order changes. |
| `members` | `members: Member[], maxMembers: int, hostOnlyControl: bool, serverMs: number` | Broadcast on member join, departure, disconnect, or role change. |
| `activity` | `memberId: string, displayName: string, action: string, serverMs: number` | UI notification of user gestures ("Alice paused the music"). |
| `error` | `error: string, message: string` | Rejection notice (`rate_limited`, `host_only`, `queue_full`). |
| `bye` | `reason: "left" \| "kicked"` | Notifies client that its room membership has ended. |

---

## 3. Storage, State & Secret Requirements

### Database & File Storage
- **Database:** **NONE**. The backend uses zero SQL, zero NoSQL, and zero Redis instances.
- **File System:** **NONE**. Does not write to disk, does not create temp files, and requires no persistent volume mount.
- **In-Memory Store:**
  - `party.PartyStore`: Thread-safe map protected by `sync.RWMutex`. Rooms expire after 2 minutes of zero connected members (`JAM_EMPTY_PARTY_TTL_MS`) or hard 12-hour limit (`JAM_PARTY_MAX_AGE_MS`).
  - `presence.Tracker`: In-memory map of `[16]byte` UUID keys to `entry{lastMs, platform}`. Memory usage is ~50 bytes per active open app. Swept every 5 seconds.
  - `ipRateLimiter`: Sliding timestamp window map. Swept periodically.

### Secrets & API Keys
- **API Keys / Secrets:** **NONE**. The backend holds zero Google, YouTube, Spotify, or third-party API keys.
- **Authentication:** All room access tokens are random cryptographic hex strings minted in-memory per join session.

### Environment Variables (`backend/config/config.go`)

| Environment Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8000` | HTTP listen port. |
| `JAM_MAX_MEMBERS` | `5` | Maximum devices per room. |
| `JAM_STATE_HEARTBEAT_MS` | `5000` | Interval between unprompted state broadcasts. |
| `JAM_PLAY_LEAD_MS` | `350` | Scheduled future lead for play/seek events. |
| `JAM_DISCONNECT_GRACE_MS` | `45000` | Grace period holding room slot during socket drops. |
| `JAM_EMPTY_PARTY_TTL_MS` | `120000` | Lifetime of room after all members disconnect. |
| `JAM_PARTY_MAX_AGE_MS` | `43200000` | Hard ceiling on room lifetime (12 hours). |
| `JAM_CONTROL_RATE_PER_SECOND` | `25` | Per-member control rate ceiling. |
| `JAM_FRAME_RATE_PER_SECOND` | `30` | Per-member total frame rate ceiling. |
| `JAM_MAX_UPCOMING_QUEUE` | `25` | Max songs in upcoming shared queue. |
| `JAM_MAX_QUEUE_LENGTH` | `26` | Total queue limit (`1 + MaxUpcomingQueue`). |
| `JAM_MAX_PARTIES` | `50` | Server-wide concurrent party room ceiling. |
| `JAM_CREATE_RATE_PER_MINUTE` | `2` | New party creation rate limit per IP. |
| `JAM_RATE_LIMIT_MAX_ENTRIES` | `10000` | Max entries in rate-limit tracking table. |
| `JAM_REQUEST_MAX_BYTES` | `16384` | Max HTTP request body size (16 KB). |
| `JAM_WEBSOCKET_MAX_BYTES` | `16384` | Max WebSocket frame size (16 KB). |
| `JAM_CONNECTION_IDLE_MS` | `900000` | Idle socket disconnect timeout (15 minutes). |
| `JAM_ALLOWED_ORIGINS` | `""` | Comma-separated browser `Origin` allowlist. |
| `JAM_TRUST_PROXY` | `false` | Read `X-Forwarded-For` for client IP detection. |
| `JAM_PUBLIC_ORIGIN` | `""` | Public origin emitted in invite deep links. |
| `JAM_PRESENCE_INTERVAL_SEC`| `300` | Presence heartbeat interval (5 minutes). |
| `JAM_PRESENCE_GRACE_SEC` | `90` | Grace period before forgetting inactive install. |
| `JAM_PRESENCE_MAX_ENTRIES` | `200000` | Maximum tracked installs in memory cap. |
| `JAM_PRESENCE_PINGS_PER_MINUTE`| `20` | Max presence pings per minute per IP. |
| `JAM_PRESENCE_RATE_LIMIT_MAX_IPS`| `100000`| Max IPs tracked by presence rate limiter. |

---

## 4. Free Hosting Suitability & Bottlenecks

### Memory Footprint
- Base Go runtime: ~12–18 MB RSS.
- Under peak load (50 parties × 5 devices = 250 sockets + 10,000 tracked presence IDs): ~35–50 MB RSS.
- **Verdict:** Extremely lightweight; easily fits within free tier RAM limits (Render 512 MB, Fly.io 256 MB, VPS 512 MB).

### CPU Utilization
- Go goroutine scheduler with non-blocking WebSocket I/O.
- Ticker routines: Heartbeat (5s), Presence sweep (5s).
- **Verdict:** Under 1% CPU utilization for a small group of 4–6 users.

### File System Writes
- Zero file system writes.
- **Verdict:** Runs seamlessly in read-only container environments (Docker, distroless, serverless containers).

### Outbound Network & IP Reputation
- The backend makes **zero outbound requests** to YouTube, Google, Spotify, or any third-party service.
- **Verdict:** Datacenter IP blocking (such as YouTube blocking AWS/Render IP ranges) has **zero impact** on this backend.

### Critical Free-Tier Challenges
1. **Render Free Tier Spin-Down:**
   - Render's free Web Service spins down to zero after 15 minutes of inactivity.
   - **Impact on Jam:** Because state is held entirely in memory, sleeping the container terminates all active WebSockets and wipes all active party rooms instantly. A cold start on the next request takes 30–50 seconds.
2. **Cloudflare Workers (Serverless) State Boundary:**
   - Standard Cloudflare Workers are stateless request handlers. They cannot coordinate a persistent WebSocket room across multiple devices without **Durable Objects**.
   - Cloudflare Durable Objects previously required a paid Workers plan ($5/mo), though experimental WebSocket hibernation exists.
3. **Scale & Redundancy:**
   - Because all rooms live in memory on a single process, the backend cannot run multiple instances behind a round-robin load balancer without an external pub/sub layer (such as Redis). For personal use (4–6 people), a single instance is ideal.

---

## 5. Live "Listening Now" Stats: End-to-End Walkthrough

```
[Android / PC App]
       │
       │ POST /api/presence {"id": UUID, "platform": "android", "open": true}
       ▼
[Go Backend: handlePresence]
       │
       ├─► Validates UUID & platform ("android" | "pc")
       ├─► Checks presenceLimiter (max 20/min per IP)
       ├─► presenceTracker.Ping(id, platform, nowMs)
       │         └─ Records lastMs in seen map: map[[16]byte]entry
       │
       ▼
[startPresenceSweeper Ticker (every 5s)]
       │
       ├─► Sweep(nowMs) calculates cutoff = nowMs - (intervalSec + graceSec)
       ├─► Purges entries older than cutoff
       ├─► Sums active Android and PC installs
       └─► Atomically publishes Counts: atomic.Pointer[Counts]
       │
       ├─────────────────────────────────┬─────────────────────────────────┐
       ▼                                 ▼                                 ▼
[GET /api/stats/live]          [GET /api/stats/live/badge.svg]     [App Exit / Sleep]
JSON: {online, android, pc}     Direct SVG vector render           POST /api/presence
Cache-Control: 15s max-age      Cache-Control: no-cache            {"open": false}
Used by Web UI                  Used by README.md                  Tracker.Close(id)
```

1. **Client Registration:** `AndroidPresence.kt` registers a `ProcessLifecycleOwner` observer. When the app comes to the foreground (`onStart`), it dispatches `Presence.setOpen(true)`.
2. **Periodic Transmission:** `Presence.kt` runs a coroutine loop posting `POST /api/presence` every 300 seconds (±10% random jitter). When the app goes to the background (`onStop`), it debounces for 2 seconds and posts `{"open": false}`.
3. **Server Ingestion:** `main.go:handlePresence` decodes the UUID and platform, updates `presenceTracker`, and returns the current interval.
4. **Sweeper & Atomic Aggregation:** `startPresenceSweeper()` runs every 5 seconds, drops expired installs (exceeding 390s silence), tallies counts, and atomically swaps an immutable `Counts` pointer.
5. **Presentation:** `GET /api/stats/live/badge.svg` generates an SVG image string in-memory with precise SVG text elements for "LISTENING NOW: X", serving it with `Cache-Control: no-cache` so GitHub's Camo image proxy fetches fresh counts on each page visit.
