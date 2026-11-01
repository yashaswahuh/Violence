#!/usr/bin/env node
// Jam backend smoke/load test: opens N WebSocket clients against a local or deployed Jam server
// and exercises create room, join by code, play, pause, seek, set track, queue add, leave, rejoin.
//
//   node scripts/jam-test.mjs --url http://localhost:8000 --clients 2
//   node scripts/jam-test.mjs --url https://<your-service>.onrender.com --clients 15
//
// Needs Node 22+ (built-in fetch and WebSocket), nothing to install. Sends only made-up test
// identities and fake track ids; no login data. Exit code 0 = every check passed.
//
// Options: --url <base> (required), --clients <n> (default 2), --room-size <n> (default 5, max 15),
//          --wake-seconds <n> (wait this long for a sleeping host, default 90), --out <file.json>,
//          --profiles (Phase 8: every even-numbered member also sends the in-app profile fields
//          profileId/avatarId/avatarColor, and the run checks they come back in the member list and
//          that the pre-join preview shows avatar and colour but never the profile id; the odd
//          members stay "old clients" that send none, so one run covers both)

const args = Object.fromEntries(
  process.argv.slice(2).reduce((pairs, arg, i, all) => {
    if (arg.startsWith('--')) pairs.push([arg.slice(2), all[i + 1] && !all[i + 1].startsWith('--') ? all[i + 1] : 'true']);
    return pairs;
  }, []),
);

const BASE = String(args.url || '').replace(/\/+$/, '');
const CLIENTS = Math.max(2, Number(args.clients || 2));
const ROOM_SIZE = Math.min(15, Math.max(2, Number(args['room-size'] || 5))); // 15 = the backend's config.MaxPartyMembers
const WAKE_SECONDS = Number(args['wake-seconds'] || 90);
const PROFILES = args.profiles === 'true';
if (!BASE || !/^https?:\/\//.test(BASE)) {
  console.error('Usage: node scripts/jam-test.mjs --url http://localhost:8000 --clients 2|15');
  process.exit(2);
}
if (typeof WebSocket === 'undefined') {
  console.error('This Node has no built-in WebSocket. Use Node 22 or newer.');
  process.exit(2);
}
const WS_BASE = BASE.replace(/^http/, 'ws');

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const now = () => Date.now();

function check(name, ok, detail = '') {
  results.push({ name, ok: !!ok, detail });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  (' + detail + ')' : ''}`);
  return !!ok;
}

async function api(method, path, body, token) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try { json = await res.json(); } catch { /* empty body */ }
  return { status: res.status, json };
}

function identity(n, room) {
  // Made-up, stable per run. userId/deviceId look like the app's (hashed id, UUID).
  const tag = `${room}-${n}`;
  const base = { userId: `test-user-${tag}`.padEnd(32, '0').slice(0, 32), deviceId: `test-device-${tag}`, displayName: `Tester ${tag}` };
  if (PROFILES && n % 2 === 0) {
    const roomNo = Number(String(room).replace(/\D/g, '')) || 0;
    return { ...base, profileId: `1234abcd-0000-4000-8000-${String(roomNo * 100 + n).padStart(12, '0')}`, avatarId: 'guitar', avatarColor: '0A84FF' };
  }
  return base;
}

/** One connected member: collects frames, lets a test wait for a matching one. */
class Member {
  constructor(label, membership) {
    this.label = label;
    this.token = membership.token;
    this.code = membership.code;
    this.frames = [];
    this.waiters = [];
    this.closed = false;
  }
  open() {
    return new Promise((resolve, reject) => {
      // Browsers cannot set headers on a WebSocket, so the server also accepts the token as a subprotocol.
      const ws = new WebSocket(`${WS_BASE}/ws/parties/${this.code}`, ['bitchord.v1', `bitchord.token.${this.token}`]);
      this.ws = ws;
      const timer = setTimeout(() => reject(new Error(`${this.label}: websocket open timeout`)), 20000);
      ws.onopen = () => { clearTimeout(timer); resolve(); };
      ws.onerror = () => { clearTimeout(timer); reject(new Error(`${this.label}: websocket error`)); };
      ws.onclose = () => { this.closed = true; };
      ws.onmessage = (event) => {
        let frame;
        try { frame = JSON.parse(String(event.data)); } catch { return; }
        frame._at = now();
        this.frames.push(frame);
        this.waiters = this.waiters.filter((w) => !w.try(frame));
      };
    });
  }
  send(frame) { this.ws.send(JSON.stringify(frame)); }
  /** Resolves with the first frame (already received or future) matching predicate, or null on timeout. */
  waitFor(predicate, ms = 8000, since = 0) {
    const have = this.frames.find((f) => f._at >= since && predicate(f));
    if (have) return Promise.resolve(have);
    return new Promise((resolve) => {
      const timer = setTimeout(() => resolve(null), ms);
      this.waiters.push({ try: (f) => { if (f._at >= since && predicate(f)) { clearTimeout(timer); resolve(f); return true; } return false; } });
    });
  }
  close() { try { this.ws.close(); } catch { /* already closed */ } }
}

async function waitForHealthy() {
  const deadline = now() + WAKE_SECONDS * 1000;
  let first = true;
  while (now() < deadline) {
    try {
      const { status, json } = await api('GET', '/healthz');
      if (status === 200 && json && json.ok === true) {
        return check('GET /healthz answers {"ok":true}', true, first ? 'awake' : 'after waking');
      }
    } catch { /* host asleep or unreachable */ }
    if (first) console.log(`      host not answering yet, waiting up to ${WAKE_SECONDS}s for it to wake...`);
    first = false;
    await sleep(3000);
  }
  return check('GET /healthz answers {"ok":true}', false, `no answer within ${WAKE_SECONDS}s`);
}

const TRACK = (n) => ({ videoId: `TEST${String(n).padStart(7, '0')}`, title: `Test song ${n}`, artist: 'Jam test', durationMs: 200000, fromAutoplay: false });

async function runRoom(roomNo, size) {
  const label = `room${roomNo}`;
  const timings = [];
  // 1. create
  const hostId = identity(0, label);
  const created = await api('POST', '/api/parties', { ...hostId, maxMembers: Math.max(2, size) });
  if (!check(`${label}: create room`, created.status === 201 && created.json && created.json.code && created.json.token, `status ${created.status}`)) return;
  const code = created.json.code;
  const members = [new Member(`${label}/host`, created.json)];
  await members[0].open();
  check(`${label}: host gets welcome`, !!(await members[0].waitFor((f) => f.type === 'welcome')));

  // 2. preview (no slot used), join by code
  const preview = await api('GET', `/api/parties/${code}/preview`);
  check(`${label}: preview by code`, preview.status === 200 && preview.json && preview.json.code === code, `status ${preview.status}`);
  for (let i = 1; i < size; i++) {
    const joined = await api('POST', `/api/parties/${code}/join`, identity(i, label));
    if (!check(`${label}: member ${i} joins by code`, joined.status === 200 && joined.json && joined.json.token, `status ${joined.status}`)) continue;
    const m = new Member(`${label}/m${i}`, joined.json);
    await m.open();
    members.push(m);
  }
  await Promise.all(members.map((m) => m.waitFor((f) => f.type === 'welcome')));
  const everyoneSeesAll = await members[0].waitFor((f) => f.type === 'members' && f.members && f.members.length === members.length, 8000);
  check(`${label}: member list shows all ${members.length}`, !!everyoneSeesAll);

  if (PROFILES && everyoneSeesAll) {
    const list = everyoneSeesAll.members;
    const expected = Math.ceil(size / 2);
    const withProfile = list.filter((m) => m.profileId !== undefined);
    check(
      `${label}: profile fields travel with the member list`,
      withProfile.length === expected && withProfile.every((m) => /^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(m.profileId) && m.avatarId === 'guitar' && m.avatarColor === '0a84ff'),
      `${withProfile.length}/${expected} members with a profile`,
    );
    check(
      `${label}: members without a profile carry none of the fields (old clients unchanged)`,
      list.filter((m) => m.profileId === undefined).every((m) => m.avatarId === undefined && m.avatarColor === undefined),
    );
    check(
      `${label}: preview shows avatar and colour but never the profile id`,
      !!(preview.json && preview.json.members.some((m) => m.avatarId === 'guitar' && m.avatarColor === '0a84ff')) && preview.json.members.every((m) => m.profileId === undefined),
    );
  }

  const host = members[0];
  const guest = members[members.length - 1];
  const allReceive = async (predicate, ms = 8000, since = 0) => (await Promise.all(members.map((m) => m.waitFor(predicate, ms, since)))).every(Boolean);

  // 3. set track, play, pause, seek (host), then a guest controls too (everyone can by default)
  let t = now();
  host.send({ type: 'control', action: 'setTrack', track: TRACK(1), positionMs: 0, isPlaying: false });
  check(`${label}: setTrack reaches all`, await allReceive((f) => f.type === 'state' && f.playback && f.playback.track && f.playback.track.videoId === TRACK(1).videoId, 8000, t));
  t = now();
  host.send({ type: 'control', action: 'play', positionMs: 0 });
  const playOk = await allReceive((f) => f.type === 'state' && f.playback && f.playback.isPlaying === true, 8000, t);
  check(`${label}: play reaches all`, playOk);
  timings.push(...members.map((m) => (m.frames.filter((f) => f._at >= t && f.type === 'state' && f.playback && f.playback.isPlaying).sort((a, b) => a._at - b._at)[0]?._at ?? t) - t));
  t = now();
  host.send({ type: 'control', action: 'pause', positionMs: 5000 });
  check(`${label}: pause reaches all`, await allReceive((f) => f.type === 'state' && f.playback && f.playback.isPlaying === false, 8000, t));
  t = now();
  host.send({ type: 'control', action: 'seek', positionMs: 30000 });
  check(`${label}: seek reaches all`, await allReceive((f) => f.type === 'state' && f.playback && Math.abs(Number(f.playback.positionMs) - 30000) < 2000, 8000, t));
  t = now();
  guest.send({ type: 'control', action: 'play', positionMs: 30000 });
  check(`${label}: a guest can control too`, await host.waitFor((f) => f.type === 'state' && f.playback && f.playback.isPlaying === true, 8000, t) !== null);

  // 4. queue add
  t = now();
  host.send({ type: 'control', action: 'queueAdd', tracks: [TRACK(2), TRACK(3)], playNext: false });
  check(`${label}: queueAdd reaches all`, await allReceive((f) => f.type === 'queue' && f.queue && Array.isArray(f.queue.items) && f.queue.items.length >= 2, 8000, t)); // the two added tracks: the current track (setTrack) is held apart from the queue

  // 5. leave and rejoin (a guest)
  const leaverIdx = members.length - 1;
  const leaver = members[leaverIdx];
  t = now();
  const left = await api('POST', `/api/parties/${code}/leave`, undefined, leaver.token);
  check(`${label}: member leaves`, left.status === 200, `status ${left.status}`);
  check(`${label}: others see the leave`, await host.waitFor((f) => f.type === 'members' && f.members && f.members.length === members.length - 1, 8000, t) !== null);
  leaver.close();
  const rejoined = await api('POST', `/api/parties/${code}/join`, identity(leaverIdx, label));
  check(`${label}: same member rejoins`, rejoined.status === 200 && rejoined.json && rejoined.json.token, `status ${rejoined.status}`);
  if (rejoined.status === 200) {
    const back = new Member(`${label}/rejoined`, rejoined.json);
    await back.open();
    members[leaverIdx] = back;
    const welcome = await back.waitFor((f) => f.type === 'welcome');
    check(`${label}: rejoiner gets current state (track + queue)`, !!(welcome && welcome.party && welcome.party.playback && welcome.party.queue && welcome.party.queue.items.length >= 2));
  }

  // 6. everyone out
  await Promise.all(members.map((m, i) => (i === 0 ? Promise.resolve() : api('POST', `/api/parties/${code}/leave`, undefined, m.token))));
  members.forEach((m) => m.close());
  const sorted = timings.filter((x) => Number.isFinite(x)).sort((a, b) => a - b);
  if (sorted.length) console.log(`      ${label}: play fan-out latency ms  min ${sorted[0]}  median ${sorted[Math.floor(sorted.length / 2)]}  max ${sorted[sorted.length - 1]}`);
  return { room: label, members: size, latenciesMs: sorted };
}

(async () => {
  console.log(`Jam test against ${BASE}: ${CLIENTS} clients, rooms of up to ${ROOM_SIZE}`);
  if (!(await waitForHealthy())) return finish(1);
  const t = await api('GET', '/api/time');
  check('GET /api/time returns serverMs', t.status === 200 && typeof (t.json && t.json.serverMs) === 'number', `status ${t.status}`);
  const stats = await api('GET', '/api/stats/live');
  check('stats endpoint is off (404)', stats.status === 404, `status ${stats.status}`);

  const rooms = [];
  for (let left = CLIENTS; left > 0; left -= ROOM_SIZE) rooms.push(Math.min(ROOM_SIZE, left));
  // A lone leftover client cannot make a room of its own; fold it into the previous room.
  if (rooms.length > 1 && rooms[rooms.length - 1] < 2) { rooms[rooms.length - 2] += rooms.pop(); }
  const summaries = [];
  for (let i = 0; i < rooms.length; i++) {
    summaries.push(await runRoom(i + 1, rooms[i]));
  }
  finish(0, summaries.filter(Boolean));
})().catch((error) => { check('test run completed without an exception', false, String(error && error.message || error)); finish(1); });

function finish(code, summaries = []) {
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed${failed.length ? `, ${failed.length} FAILED` : ''}`);
  if (args.out) {
    import('node:fs').then((fs) => fs.writeFileSync(args.out, JSON.stringify({ base: BASE, clients: CLIENTS, roomSize: ROOM_SIZE, at: new Date().toISOString(), results, summaries }, null, 2)));
  }
  setTimeout(() => process.exit(failed.length ? 1 : code), 300);
}
