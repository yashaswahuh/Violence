import { describe, expect, it } from 'vitest';
import { ServerClock } from './clock';
import { appLink, cleanCode, extractCode, inviteLink, parseInvite } from './invite';
import { EMPTY_PLAYBACK, type PartyMember, type PartyQueue, type PartyTrack } from './models';
import { autoplaySupplierId, detectSingleMove, partyQueueIndexOf, partyUpcomingAfter } from './queueReading';

// ServerClockTest.kt, case for case.
describe('ServerClock', () => {
  let now = 1_000;
  const clock = () => new ServerClock(() => now);

  it('knows nothing until a round trip completes', () => {
    const c = clock();
    expect(c.synced).toBe(false);
    expect(c.serverNowMs()).toBeNull();
  });

  it('takes the offset at the midpoint of the round trip', () => {
    const c = clock();
    c.record(100, 5_150, 200);
    expect(c.synced).toBe(true);
    expect(c.offsetMs).toBe(5_000);
    expect(c.roundTripMs).toBe(100);
  });

  it('believes the shortest round trip, not the most recent', () => {
    const c = clock();
    c.record(0, 5_010, 20);
    c.record(100, 5_400, 700);
    expect(c.offsetMs).toBe(5_000);
    expect(c.roundTripMs).toBe(20);
  });

  it('drops a stale best sample rather than pinning the session', () => {
    const c = clock();
    c.record(0, 5_010, 20);
    c.record(300_000, 306_200, 300_400);
    expect(c.offsetMs).toBe(6_000);
    expect(c.roundTripMs).toBe(400);
  });

  it('reads the server as this clock plus the offset', () => {
    const c = clock();
    c.record(100, 5_150, 200);
    now = 900;
    expect(c.serverNowMs()).toBe(5_900);
  });

  it('forgets everything on reset', () => {
    const c = clock();
    c.record(100, 5_150, 200);
    c.reset();
    expect(c.synced).toBe(false);
    expect(c.roundTripMs).toBe(0);
  });
});

const t = (videoId: string, fromAutoplay = false): PartyTrack => ({
  videoId,
  title: videoId,
  artist: '',
  thumbnailUrl: null,
  durationMs: null,
  fromAutoplay,
});

describe('party queue reading', () => {
  // The queue keeps history, so the playing track can be in it twice (the duplicate-track fix).
  const queue: PartyQueue = { seq: 3, index: 3, items: [t('a'), t('b'), t('c'), t('a'), t('d')] };

  it('trusts the state frame’s index when it names the track', () => {
    expect(partyQueueIndexOf(queue, { ...EMPTY_PLAYBACK, queueSeq: 3, queueIndex: 3 }, 'a')).toBe(3);
  });

  it('takes the nearest copy, looking forward first, never the first from the top', () => {
    expect(partyQueueIndexOf(queue, { ...EMPTY_PLAYBACK, queueSeq: 3, queueIndex: 2 }, 'a')).toBe(3);
    expect(partyQueueIndexOf({ ...queue, index: 4 }, { ...EMPTY_PLAYBACK, queueSeq: 9, queueIndex: 0 }, 'a')).toBe(3);
    expect(partyQueueIndexOf(queue, EMPTY_PLAYBACK, 'zz')).toBe(-1);
  });

  it('reads what comes after the playing copy', () => {
    expect(partyUpcomingAfter(queue, { ...EMPTY_PLAYBACK, queueSeq: 3, queueIndex: 3 }, 'a').map((x) => x.videoId)).toEqual(['d']);
  });

  it('detects a single move, and only that', () => {
    expect(detectSingleMove(['a', 'b', 'c', 'd'], ['a', 'c', 'd', 'b'])).toEqual({ fromIndex: 1, toIndex: 3, videoId: 'b' });
    expect(detectSingleMove(['a', 'b', 'c', 'd'], ['b', 'a', 'd', 'c'])).toBeNull();
    expect(detectSingleMove(['a', 'b'], ['a', 'b'])).toBeNull();
  });
});

describe('AutoPlay supplier', () => {
  const m = (memberId: string, isHost: boolean, connected: boolean): PartyMember => ({
    memberId,
    userId: '',
    displayName: memberId,
    avatarUrl: null,
    isHost,
    connected,
    joinedAtMs: 0,
    lastSeenMs: 0,
  });

  it('is the host while connected, else the lowest connected member', () => {
    expect(autoplaySupplierId([m('z', true, true), m('a', false, true)], false)).toBe('z');
    expect(autoplaySupplierId([m('z', true, false), m('c', false, true), m('b', false, true)], false)).toBe('b');
  });

  it('has no stand-in for a host who locked the party', () => {
    expect(autoplaySupplierId([m('z', true, false), m('a', false, true)], true)).toBeNull();
  });
});

describe('invites', () => {
  it('reads the link shapes and normalises the code', () => {
    expect(parseInvite('https://yashaswahuh.is-a.dev/violence/invite/ab12cd')).toEqual({ code: 'AB12CD', serverUrl: null });
    expect(parseInvite('https://yashaswahuh.is-a.dev/Violence/party/ab12cd')).toEqual({ code: 'AB12CD', serverUrl: null });
    expect(parseInvite('https://yashaswahuh.is-a.dev/Violence/party/ab12cd/')).toEqual({ code: 'AB12CD', serverUrl: null });
    expect(parseInvite('https://yashaswahuh.is-a.dev/party/AB12CD')).toEqual({ code: 'AB12CD', serverUrl: null });
    expect(parseInvite('violence://party/AB12CD?server=jam.example.com')).toEqual({ code: 'AB12CD', serverUrl: 'https://jam.example.com' });
    expect(parseInvite('bitchord://party/AB12CD?server=jam.example.com')).toEqual({ code: 'AB12CD', serverUrl: 'https://jam.example.com' });
    expect(parseInvite('https://example.com/nope')).toBeNull();
    expect(parseInvite('https://yashaswahuh.is-a.dev/Violence/party/ABC12')).toBeNull();
    expect(parseInvite('https://yashaswahuh.is-a.dev/Violence/party/AB12CD/extra')).toBeNull();
    expect(cleanCode(' ab-12 cd ')).toBe('AB12CD');
  });

  it('has no web link without a configured base: share only the code', () => {
    expect(inviteLink('ab12cd', null, '')).toBeNull();
    expect(inviteLink('ab12cd', null, '   ')).toBeNull();
  });

  it('builds the web link from the configured base', () => {
    expect(inviteLink('ab12cd', null, 'https://yashaswahuh.is-a.dev/Violence')).toBe('https://yashaswahuh.is-a.dev/Violence/party/AB12CD');
    expect(inviteLink('ab12cd', null, 'https://yashaswahuh.is-a.dev/Violence/')).toBe('https://yashaswahuh.is-a.dev/Violence/party/AB12CD');
    expect(inviteLink('ab12cd', null, 'site.example')).toBe('https://site.example/party/AB12CD');
  });

  it('carries a custom Jam server and reads it back', () => {
    const link = inviteLink('ab12cd', 'https://jam.example.com/', 'https://site.example/Violence');
    expect(link).toBe('https://site.example/Violence/party/AB12CD?server=https%3A%2F%2Fjam.example.com');
    expect(parseInvite(link!)).toEqual({ code: 'AB12CD', serverUrl: 'https://jam.example.com' });
  });

  it('builds the app link that opens Android', () => {
    expect(appLink('ab12cd')).toBe('violence://party/AB12CD');
    expect(appLink('ab12cd', 'https://jam.example.com')).toBe('violence://party/AB12CD?server=https%3A%2F%2Fjam.example.com');
    expect(parseInvite(appLink('ab12cd', 'https://jam.example.com'))).toEqual({ code: 'AB12CD', serverUrl: 'https://jam.example.com' });
  });
});

// The join field: a typed code, a pasted link, or a pasted share message (it used to cut a link to 7 characters).
describe('extractCode', () => {
  it('takes a bare code in any case, with spaces or hyphens', () => {
    expect(extractCode('K7M2PQ')).toBe('K7M2PQ');
    expect(extractCode('  k7m2pq ')).toBe('K7M2PQ');
    expect(extractCode('K7M 2PQ')).toBe('K7M2PQ');
    expect(extractCode('k7m-2pq')).toBe('K7M2PQ');
  });

  it('takes a pasted web or app link', () => {
    expect(extractCode('https://yashaswahuh.is-a.dev/Violence/party/K7M2PQ')).toBe('K7M2PQ');
    expect(extractCode('https://yashaswahuh.is-a.dev/Violence/party/k7m2pq?server=https%3A%2F%2Fjam.example.com')).toBe('K7M2PQ');
    expect(extractCode('https://yashaswahuh.is-a.dev/Violence/invite/K7M2PQ')).toBe('K7M2PQ');
    expect(extractCode('violence://party/K7M2PQ')).toBe('K7M2PQ');
  });

  it('takes a whole share message', () => {
    expect(extractCode('https://yashaswahuh.is-a.dev/Violence/party/K7M2PQ\n\nListen with me on Violence - party code K7M2PQ')).toBe('K7M2PQ');
    expect(extractCode('Listen with me on Violence - party code K7M2PQ')).toBe('K7M2PQ');
  });

  it('does not mistake words for a code, and does not guess between two', () => {
    expect(extractCode('please invite me to listen')).toBeNull();
    expect(extractCode('hello there')).toBeNull();
    expect(extractCode('try K7M2PQ or X9Y8Z7 maybe')).toBeNull();
  });

  it('gives nothing for empty or partial input', () => {
    expect(extractCode(null)).toBeNull();
    expect(extractCode('')).toBeNull();
    expect(extractCode('AB12')).toBeNull();
    expect(extractCode('ABC1234')).toBeNull();
  });
});
