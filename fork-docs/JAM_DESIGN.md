# Jam (Listen Together) Feature Design Specification

**Audit Date:** 2026-10-29  
**Feature Concept:** Spotify-style shared listening room on 100% free infrastructure.  
**Capacity:** 4–6 concurrent devices for personal listening parties.  
**Constraint:** Audio streaming is 100% decentralized (on-device); only lightweight JSON synchronization signals pass through the server.

---

## 1. Architectural Overview & State Machine

The Jam feature enables up to 5–6 devices to share a synchronized playback timeline and collaborative queue. 

```
                                  ┌───────────────────────────┐
                                  │   Free Backend / Broker   │
                                  │ (Render Go or Firebase RT)│
                                  └─────────────┬─────────────┘
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 │ Small Sync Frames            │ Small Sync Frames            │ Small Sync Frames
                 │ (State, Queue, Pings)        │ (State, Queue, Pings)        │ (State, Queue, Pings)
                 ▼                              ▼                              ▼
        ┌─────────────────┐            ┌─────────────────┐            ┌─────────────────┐
        │   Device 1      │            │   Device 2      │            │   Device 3      │
        │   (Host Phone)  │            │   (Guest Phone) │            │   (Guest TV)    │
        └────────┬────────┘            └────────┬────────┘            └────────┬────────┘
                 │                              │                              │
                 ▼                              ▼                              ▼
        ┌─────────────────┐            ┌─────────────────┐            ┌─────────────────┐
        │ Direct Stream   │            │ Direct Stream   │            │ Direct Stream   │
        │ From Google CDN │            │ From Google CDN │            │ From Google CDN │
        └─────────────────┘            └─────────────────┘            └─────────────────┘
```

### Key Principles
1. **Independent Audio Streaming:** The server never touches media bytes. Each device independently resolves and buffers audio streams from YouTube/Googlevideo CDN (or configured source).
2. **Anchor-and-Offset Synchronization:** Devices never synchronize by broadcasting "play now". Instead, the server maintains an **anchor timestamp** (the server time when a specific track position was valid). Each device measures its clock offset from the server and calculates where its local playhead should be at any millisecond.
3. **Monotonic Sequences:** Every state and queue mutation advances an integer sequence counter (`seq`, `queueSeq`). Obsolete out-of-order packets are dropped instantly.

---

## 2. Room Lifecycle, Identity & Role Model

### 2.1 Room Creation & Join by Code
- **Room Code Generation:** 6-character alphanumeric string generated from an unambiguous alphabet (e.g. `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`, excluding `0`, `O`, `1`, `I`, `L`).
- **Charitable Homoglyph Normalization:** Client and server normalize user input:
  - Strips spaces and hyphens.
  - Converts lowercase to uppercase.
  - Translates `0` ➔ `O`, `1` ➔ `I`, `8` ➔ `B`.
- **Creation Flow:**
  1. Host client issues `POST /api/parties` with identity (`userId`, `deviceId`, `displayName`, `avatarUrl`).
  2. Server initializes party room with `code`, sets caller as `isHost = true`, and returns a cryptographic unguessable `token`.
- **Join Flow:**
  1. Guest client submits `POST /api/parties/{code}/join`.
  2. If room exists and active member count < `JAM_MAX_MEMBERS` (default 5–6), server mints a unique guest `token` and returns initial room snapshot.
  3. Server broadcasts updated `members` frame to all connected sockets in the room.

### 2.2 Host & Guest Permissions
- **Host Role:** The creator is designated the room host.
- **Host Privileges:**
  - `setHostOnlyControl(enabled: bool)`: Locks down all playback and queue modifications to the host.
  - `setMaxMembers(limit: int)`: Dynamically changes room capacity.
  - `kick(memberId: string)`: Ejects a guest from the room.
- **Guest Permissions:**
  - By default (`hostOnlyControl = false`), all members have equal rights to play, pause, seek, and add/remove tracks.
  - When `hostOnlyControl = true`, guests can only listen; any control frame sent by a guest is rejected with `403 host_only`.
- **Host Succession:** If the host departs (`handleLeaveParty` or disconnect timeout), the host role automatically passes to the next oldest connected member (`joinedAtMs`). The party never collapses due to host exit.

---

## 3. Shared Queue Design

To prevent mobile bandwidth bloat, the shared queue is completely decoupled from periodic state heartbeats:

1. **State Frames Carry Sequence Only:** State frames carry `queueSeq`, `queueIndex`, and `queueLength` (~80 bytes), never the full list of tracks.
2. **Initial Snapshot:** Full queue is delivered once during room join (`welcome` frame).
3. **Delta Mutations:** Client gestures dispatch lightweight delta actions:
   - `queueAdd`: Adds tracks to end of queue or as "play next".
   - `queueRemove`: Removes specific track by `videoId`.
   - `queueClear`: Clears upcoming items while preserving active track.
   - `queueMove`: Reorders tracks (`fromIndex` ➔ `toIndex`).
4. **Race-Condition Protection on Moves:** `queueMove` carries the target `videoId` in addition to indices. When concurrent gestures occur (e.g. two people moving songs simultaneously on high-latency mobile data), the server validates that `queue[fromIndex].videoId == targetVideoId` under the room lock. If indices shifted due to another member's action, the server finds the true current index of `videoId` before applying the move.
5. **Queue Limits:** Upcoming queue is clamped at 25 items (`JAM_MAX_UPCOMING_QUEUE`). Duplicate Autoplay recommendations are automatically de-duplicated.

---

## 4. Message Types & Payload Specifications

### 4.1 Client ➔ Server Frames (over WebSocket)

```typescript
// 1. NTP Clock Measurement
interface PingFrame {
  type: "ping";
  clientMs: number;
}

// 2. State & Queue Resync
interface SyncFrame { type: "sync"; }
interface SyncQueueFrame { type: "syncQueue"; }

// 3. Telemetry Playhead Report (Logged only; never mutates room)
interface ReportFrame {
  type: "report";
  positionMs: number;
  isPlaying: boolean;
}

// 4. Playback Controls
interface ControlPlayFrame {
  type: "control";
  action: "play";
  positionMs?: number;
}

interface ControlPauseFrame {
  type: "control";
  action: "pause";
  positionMs?: number;
}

interface ControlSeekFrame {
  type: "control";
  action: "seek";
  positionMs: number; // Required
}

interface ControlSetTrackFrame {
  type: "control";
  action: "setTrack";
  track: {
    videoId: string;
    title: string;
    artist: string;
    thumbnailUrl: string;
    durationMs: number;
    fromAutoplay: boolean;
  };
  positionMs?: number;
  isPlaying?: boolean;
}

// 5. Queue Controls
interface ControlQueueAddFrame {
  type: "control";
  action: "queueAdd";
  tracks: TrackWire[];
  playNext: boolean;
}

interface ControlQueueMoveFrame {
  type: "control";
  action: "queueMove";
  fromIndex: number;
  toIndex: number;
  videoId?: string;
}

interface ControlQueueRemoveFrame {
  type: "control";
  action: "queueRemove";
  videoId: string;
}

// 6. Host-Only Administration
interface ControlHostOnlyFrame {
  type: "control";
  action: "setHostOnlyControl";
  enabled: boolean;
}

interface ControlKickFrame {
  type: "control";
  action: "kick";
  memberId: string;
}
```

### 4.2 Server ➔ Client Frames (over WebSocket)

```typescript
// 1. Initial Handshake
interface WelcomeFrame {
  type: "welcome";
  you: MemberWire;
  party: PartySnapshotWire;
  serverMs: number;
}

// 2. Clock Response
interface PongFrame {
  type: "pong";
  clientMs: number;
  serverMs: number;
}

// 3. Playback State Heartbeat (Broadcast every 5s & on mutations)
interface StateFrame {
  type: "state";
  playback: {
    seq: number;
    track: TrackWire | null;
    positionMs: number;
    anchorMs: number;
    isPlaying: boolean;
    updatedBy: string;
    startedBy: string;
    startedByName: string;
    autoplayEnabled: boolean;
    queueSeq: number;
    queueIndex: number;
    queueLength: number;
  };
  serverMs: number;
}

// 4. Queue Frame (Broadcast ONLY when queue list changes)
interface QueueFrame {
  type: "queue";
  queue: {
    seq: number;
    index: number;
    items: TrackWire[];
  };
  serverMs: number;
}

// 5. Membership Frame
interface MembersFrame {
  type: "members";
  members: MemberWire[];
  maxMembers: number;
  hostOnlyControl: boolean;
  serverMs: number;
}

// 6. User Activity Notification
interface ActivityFrame {
  type: "activity";
  memberId: string;
  displayName: string;
  action: string;
  serverMs: number;
}

// 7. Error & Departure
interface ErrorFrame {
  type: "error";
  error: "rate_limited" | "host_only" | "queue_full" | "invalid_action";
  message: string;
}

interface ByeFrame {
  type: "bye";
  reason: "left" | "kicked";
}
```

---

## 5. Clock Synchronization & Drift Correction Strategy

### 5.1 NTP Offset Measurement
To calculate exact playhead position without trusting packet arrival times:
1. Client records local monotonic time $t_0$ (`SystemClock.elapsedRealtime()`).
2. Client sends `{"type": "ping", "clientMs": t_0}`.
3. Server responds with `{"type": "pong", "clientMs": t_0, "serverMs": t_s}`.
4. Client receives response at local monotonic time $t_1$.
5. Calculations:
   $$\text{Round Trip Time (RTT)} = t_1 - t_0$$
   $$\text{Clock Offset} = t_s - \frac{t_0 + t_1}{2}$$
6. **Sample Filtering:** Client retains the sample with the **smallest RTT** over a sliding window. Error is mathematically bounded by $\pm \frac{\text{RTT}}{2}$.

### 5.2 Playhead Math (`PartySync.kt` & `ServerClock.kt`)
At any given moment on the client device:
$$\text{serverNow} = \text{deviceNow} + \text{clockOffset}$$
$$\text{targetPlayhead} = \begin{cases} 
\text{positionMs} + \max(0, \text{serverNow} - \text{anchorMs}) & \text{if } \text{isPlaying} \\ 
\text{positionMs} & \text{if paused} 
\end{cases}$$

### 5.3 Scheduled Resume Lead (`JAM_PLAY_LEAD_MS`)
- When a user presses Play or Seeks, the server anchors the resume **350 ms into the future** (`anchorMs = serverNow + 350`).
- During those 350 ms, `targetPlayhead` holds at `positionMs` rather than running backwards.
- Every client receives the frame before the anchor instant arrives, allowing ExoPlayer to buffer without falling behind.

### 5.4 Audible Drift Threshold
- Seeking an audio player produces an audible click/stutter.
- `PartySync.kt` enforces a `DRIFT_LIMIT_MS` threshold (default: 1,500 ms).
- If $|\text{player.currentPosition} - \text{targetPlayhead}| < 1500\text{ ms}$, the player is left alone. Small timing jitter between hardware decoders is ignored.
- Only actual desyncs (buffering stall, incoming phone call, app coming back from deep sleep) trigger a correcting seek.

---

## 6. Edge Case Handling: Unplayable Tracks on One Device

### Problem Statement
A song in the shared queue may fail to play on one participant's device (e.g. YouTube Music region restriction, network timeout, corrupted DASH chunk, or missing codec), while playing successfully on other participants' devices.

### Antipattern to Avoid
A naive client might throw an error and publish a `pause` or `next` action to the room, interrupting the music for everyone else.

### Resilient Behavior Specification
1. **Local Failure Isolation:** When `StreamResolver.resolve(videoId)` fails or ExoPlayer throws `PlaybackException`:
   - The failing client **MUST NOT** broadcast any control frame (`pause`, `seek`, or `next`) to the server.
2. **Local Stalled State:** The client enters a local state: `PartyPlaybackStalled(videoId)`.
3. **Mute & Background Sync:** The local player pauses or mutes local audio output.
4. **Unobtrusive Notification:** The client UI displays an in-app banner or toast:
   > *"Track unavailable on your network. Listening in sync — audio will resume on next track."*
5. **Passive Timeline Tracking:** The client continues to receive room `state` frames and advances its simulated timeline in sync with the room.
6. **Automatic Recovery:** When the room finishes the song and advances to `queueIndex + 1`, the client clears the stalled state and immediately resumes audio playback on the next track.

---

## 7. Reconnection & Network Degradation Model

1. **Transient Disconnection Grace Period:**
   - On the server, a dropped TCP socket does not remove the member.
   - Membership is preserved for `JAM_DISCONNECT_GRACE_MS` (45 seconds).
2. **Client Auto-Reconnect Loop:**
   - `ListenTogether.kt` registers an Android `ConnectivityManager.NetworkCallback`.
   - When network restores, it initiates WebSocket reconnection using the stored bearer `token`.
   - Reconnection reclaims the existing member slot without incrementing room count.
3. **State Resynchronization:**
   - Upon reconnecting, the server emits a fresh `welcome` frame.
   - The client takes an immediate `ping`/`pong` sample to update `clockOffset`.
   - If `state.queueSeq != localQueue.seq`, the client dispatches `syncQueue` to fetch the updated playlist.
4. **Audio Focus Interruption Handling (`PartySync.kt`):**
   - If a member receives a phone call, Android OS revokes audio focus and ExoPlayer pauses locally.
   - `PartySync` detects `focusLost` and **detaches** from the room timeline locally without sending a `pause` frame to the server.
   - When the phone call ends, the user taps "Catch Up", and the local player jumps to the current room playhead.

---

## 8. Specific Client Classes Affected in Future Implementation

The following existing classes are the exact points of extension for Jam feature maintenance:

| Class Path & File | Responsibility in Jam | Required Extension / Adaptation |
|---|---|---|
| `app/src/main/java/.../data/listentogether/ListenTogether.kt` | Client state machine, WebSocket client, REST join/create | Update default server URL; add stalled-track recovery handlers. |
| `app/src/main/java/.../playback/PartySync.kt` | Media3 ExoPlayer bridge; outbound intent vs inbound reconcile | Enhance error interception so local player failures do not broadcast pause. |
| `app/src/main/java/.../data/listentogether/JamInviteLink.kt` | Invite URL parsing and deep link creation | Replace `ORIGIN` and `HOST` with fork domain. |
| `shared/src/commonMain/.../data/listentogether/JamInvite.kt` | Multiplatform invite URL parser | Update host whitelist to fork domain. |
| `shared/src/commonMain/.../data/listentogether/ServerClock.kt` | NTP clock measurement and drift calculation | Retained as-is (core math is robust). |
| `shared/src/commonMain/.../data/listentogether/PartyModels.kt` | Data transfer models (`PartyPlayback`, `TrackWire`) | Add `stalled` or `error` flags if protocol is extended. |
| `web/app/src/party/listenTogether.ts` | Web player Listen Together controller | Update default server URL; handle browser subprotocol tokens. |
| `web/app/src/party/invite.ts` | Web invite link builder | Update `INVITE_ORIGIN` to fork static web domain. |
| `app/build.gradle.kts` & `tv/build.gradle.kts` | Build configuration properties | Set `LISTEN_TOGETHER_SERVER` buildConfig field to fork backend instance. |


---

## 9. Phase 8: in-app profile identity fields (additive, backward compatible)

Added 2026-11-01. Full rationale and the profile feature itself: `PROFILE_DESIGN.md`. Verified against the real Go backend run locally (`JAM_TEST_PLAN.md` section 12).

**Request** (`POST /api/parties` and `POST /api/parties/{code}/join`): `JoinRequest` gains three optional fields next to `displayName`.

| Field | Meaning | Server rule |
|---|---|---|
| `profileId` | the opaque UUID of the sender's in-app profile | lower-case dashed UUID after trim/lower-casing, otherwise **dropped** |
| `avatarId` | preset avatar id | `[a-z0-9]{1,16}` after trim/lower-casing, otherwise **dropped**. The server does not know the preset list, so new avatars need no redeploy |
| `avatarColor` | six hex digits, no `#` | `[0-9a-f]{6}` after lower-casing, otherwise **dropped** |

An invalid optional field is ignored, never a 422: a cosmetic extra must not stop anyone joining. `displayName` is unchanged (1-80 UTF-8 bytes); a client with a profile cuts its name to 80 bytes before sending because a 24-emoji name is 96 bytes.

**Responses.** A `PartyMember` (in `welcome`, `members`, and the create/join answers) carries `profileId`, `avatarId`, `avatarColor` **only when the member has them**; members without a profile serialise exactly as before. The pre-join **preview** (`GET /api/parties/{code}/preview`, readable by anyone with the code) carries `avatarId` and `avatarColor` per member and **never `profileId`**. A rejoining device replaces all three with whatever it sends now, so deleting a profile on the device clears it on the next join.

**What the server stores.** The three values live in the in-memory `Member` for the life of the room, like the name and avatar URL already did. Nothing is written to disk, nothing is logged (the existing rule: no names, avatars or tokens in logs; verified for these fields in the local run).

**Identity on the client.** With a profile: `userId = sha256("violence-profile:" + profileId)[:32]`, `avatarUrl` is not sent, and Jam no longer needs a YT Music sign-in. Without one: unchanged.

**Compatibility.**
| Client | Server | Result |
|---|---|---|
| old | new | works; the server has nothing extra to store |
| new, no profile | old or new | works; sends no new fields |
| new, profile | old | joins with the profile name (unknown JSON fields are ignored); others see an initial instead of the glyph |
| new, profile | new | glyph and colour for everyone on a new client; old clients in the room see the initial |

**Redeploy.** Needed only to see avatars and colours; names and joining work against the existing server. Steps: `JAM_BACKEND_DEPLOY.md` section 6.

Files: `backend/protocol/protocol.go` (`JoinRequest`, `Validate`), `backend/party/party.go` (`Member`, `Profile`, `JoinWithProfile`), `backend/main.go` (`profileOf`, preview), `shared/.../PartyModels.kt`, `web/app/src/party/models.ts`. Tests: `backend/protocol/profile_test.go`, `backend/party/profile_test.go`, `backend/profile_http_test.go`, `scripts/jam-test.mjs --profiles`.
