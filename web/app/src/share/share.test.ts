import { describe, expect, it } from 'vitest';
import { cardToUri, cardToWebUrl, type ProfileCard } from '@/profile/card';
import { extractIncoming, incomingKey, normalizePartyCode, parseIncoming, parsePartyLink } from './incoming';
import {
  fitSong,
  isSpotifyId,
  isYouTubeId,
  parseSongLink,
  sanitizeHint,
  searchSong,
  SONG_CUSTOM_PREFIX,
  SONG_WEB_PREFIX,
  songKey,
  songToUri,
  songToWebUrl,
  youtubeSong,
  type SongError,
  type SongRef,
} from './songLinks';

// SongLinksTest.kt, IncomingLinksTest.kt: the same vectors in TypeScript.

const YT = 'dQw4w9WgXcQ';
const SP = '4uLU6hMCjMI75M1A2tKUQC';
const cp = (...codes: number[]) => String.fromCodePoint(...codes);
const CARD: ProfileCard = { id: '123e4567-e89b-42d3-a456-426614174000', displayName: 'Alex', avatarId: 'guitar', color: '0a84ff' };

const uri = (source = 'yt', id = YT, query = 'v=1&t=Song&a=Artist', prefix = SONG_CUSTOM_PREFIX) => `${prefix}${source}/${id}?${query}`;
const ok = (text: string): SongRef => {
  const r = parseSongLink(text);
  if (!r.ok) throw new Error(`expected a song, got ${r.error} for ${text}`);
  return r.ref;
};
const err = (text: string | null | undefined): SongError | 'ok' => {
  const r = parseSongLink(text);
  return r.ok ? 'ok' : r.error;
};
const bytes = (s: string) => new TextEncoder().encode(s).length;

describe('song links: building', () => {
  it('has the documented shape and round-trips', () => {
    const ref = youtubeSong(YT, 'Never Gonna Give You Up', 'Rick Astley')!;
    expect(songToUri(ref)).toBe(`violence://song/yt/${YT}?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley`);
    expect(songToWebUrl(ref)).toBe(`${SONG_WEB_PREFIX}yt/${YT}?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley`);
    expect(ok(songToUri(ref))).toEqual(ref);
    expect(ok(songToWebUrl(ref))).toEqual(ref);
  });

  it('hints are optional for YouTube, required for search', () => {
    expect(songToUri(youtubeSong(YT)!)).toBe(`violence://song/yt/${YT}?v=1`);
    expect(searchSong(null)).toBeNull();
    expect(searchSong('   ')).toBeNull();
    const ref = searchSong('Blinding Lights', 'The Weeknd')!;
    expect(songToUri(ref)).toBe('violence://song/q/search?v=1&t=Blinding%20Lights&a=The%20Weeknd');
    expect(ok(songToUri(ref))).toEqual(ref);
  });

  it('refuses a bad video id', () => {
    for (const bad of ['short', 'dQw4w9WgXc!', `${YT}x`]) expect(youtubeSong(bad)).toBeNull();
  });

  it('always fits 200 bytes, on character boundaries', () => {
    const long = youtubeSong(YT, 't'.repeat(300), 'a'.repeat(300))!;
    expect(bytes(songToUri(long))).toBeLessThanOrEqual(200);
    expect(ok(songToUri(long)).id).toBe(YT);
    const cjk = youtubeSong(YT, cp(0x5c71).repeat(100), cp(0x1f600).repeat(60))!;
    const link = songToUri(cjk);
    expect(bytes(link)).toBeLessThanOrEqual(200);
    expect([...(ok(link).title ?? '')].every((c) => c === cp(0x5c71))).toBe(true);
    expect(fitSong(cjk).title!.length).toBeGreaterThan(0);
  });

  it('cleans control and invisible characters when building', () => {
    const ref = youtubeSong(YT, `Hel${String.fromCharCode(7)}lo${String.fromCharCode(0x200b)} ${String.fromCharCode(0x202e)}World`, 'A\nB')!;
    expect(ref.title).toBe('Hello World');
    expect(ref.artist).toBe('A B');
    expect(youtubeSong(YT, String.fromCharCode(0, 1), 'ok')!.title).toBeNull();
    expect(sanitizeHint('  a   b  ', 10)).toBe('a b');
  });

  it('round-trips unicode and awkward punctuation', () => {
    for (const title of ['Zoë', 'Σόφια', '山田太郎', 'محمد', `DJ ${cp(0x1f3a7)}`, "Don't Stop & Go = #1 + 2"]) {
      expect(ok(songToUri(youtubeSong(YT, title, 'x')!)).title).toBe(title);
    }
  });
});

describe('song links: parsing', () => {
  it('reads both forms, any case of the prefix, whitespace and a fragment', () => {
    const expected: SongRef = { source: 'yt', id: YT, title: 'Song', artist: 'Artist' };
    expect(ok(uri())).toEqual(expected);
    expect(ok(uri('yt', YT, 'v=1&t=Song&a=Artist', SONG_WEB_PREFIX))).toEqual(expected);
    expect(ok(uri('yt', YT, 'v=1&t=Song&a=Artist', SONG_WEB_PREFIX.toUpperCase()))).toEqual(expected);
    expect(ok(`  \n${uri()} \t`)).toEqual(expected);
    expect(ok(`${uri()}#frag`)).toEqual(expected);
  });

  it('checks ids per source', () => {
    for (const bad of ['', 'abc', YT.slice(0, -1), `${YT}A`, 'dQw4w9WgXc!', 'dQw4w9 WgXc']) expect(err(uri('yt', bad))).toBe('bad-id');
    expect(ok(uri('yt', 'a_-B9zZ0xY1')).id).toBe('a_-B9zZ0xY1');
    expect(ok(uri('sp', SP)).source).toBe('sp');
    expect(err(uri('sp', SP, 'v=1'))).toBe('missing-field');
    expect(err(uri('sp', SP.slice(0, -1)))).toBe('bad-id');
    expect(err(uri('sp', `${SP.slice(0, -1)}-`))).toBe('bad-id');
    expect(err(uri('q', 'other', 'v=1&t=T'))).toBe('bad-id');
    expect(err(uri('q', 'search', 'v=1'))).toBe('missing-field');
    expect(ok(uri('q', 'search', 'v=1&t=T'))).toEqual({ source: 'q', id: 'search', title: 'T', artist: null });
    expect(isYouTubeId(YT)).toBe(true);
    expect(isSpotifyId(SP)).toBe(true);
  });

  it('refuses wrong shapes, sources, versions and fields', () => {
    expect(err(uri('spotify', SP))).toBe('bad-source');
    expect(err(uri('YT', YT))).toBe('bad-source');
    for (const bad of [`violence://song/yt/${YT}/extra?v=1`, 'violence://song/yt?v=1', `violence://song/yt/${YT}/?v=1`, `violence://songs/yt/${YT}?v=1`, `http://yashaswahuh.is-a.dev/Violence/song/yt/${YT}?v=1`, `https://evil.example/Violence/song/yt/${YT}?v=1`, '', null]) {
      expect(err(bad)).toBe('not-a-song-link');
    }
    expect(err(uri('yt', YT, 'v=2&t=T'))).toBe('wrong-version');
    expect(err(uri('yt', YT, 'v=&t=T'))).toBe('wrong-version');
    expect(err(uri('yt', YT, 't=T&a=A'))).toBe('missing-field');
    expect(err(`violence://song/yt/${YT}`)).toBe('missing-field');
    expect(err(uri('yt', YT, 'v=1&t=A&t=B'))).toBe('duplicate-field');
    expect(err(uri('yt', YT, 'v=1&v=1'))).toBe('duplicate-field');
    expect(err(uri('yt', YT, 'v=1&t=A&x=1'))).toBe('unknown-field');
    expect(err(uri('yt', YT, 'v=1&url=http%3A%2F%2Fevil'))).toBe('unknown-field');
  });

  it('limits hints exactly and never truncates', () => {
    expect(ok(uri('yt', YT, `v=1&t=${'a'.repeat(120)}`)).title).toBe('a'.repeat(120));
    expect(err(uri('yt', YT, `v=1&t=${'a'.repeat(121)}`))).toBe('bad-title');
    expect(ok(uri('yt', YT, `v=1&a=${'a'.repeat(80)}`)).artist).toBe('a'.repeat(80));
    expect(err(uri('yt', YT, `v=1&a=${'a'.repeat(81)}`))).toBe('bad-artist');
    expect(err(uri('yt', YT, `v=1&t=${'a'.repeat(600)}`))).toBe('too-long');
    expect(err(uri('yt', YT, 'v=1&t='))).toBe('bad-title');
    expect(err(uri('yt', YT, 'v=1&t=%20%20'))).toBe('bad-title');
    expect(err(uri('yt', YT, 'v=1&a=+++'))).toBe('bad-artist');
  });

  it('rejects malicious hints and treats markup as plain text', () => {
    for (const e of ['%00', '%07', '%0A', '%0D', '%1B', '%7F', '%C2%85']) expect(err(uri('yt', YT, `v=1&t=Hi${e}there`))).toBe('bad-title');
    for (const e of ['%E2%80%AE', '%E2%80%8B', '%E2%81%A6', '%EF%BB%BF', '%E2%80%A8', '%F3%A0%80%81', '%EE%80%80']) expect(err(uri('yt', YT, `v=1&a=Hi${e}there`))).toBe('bad-artist');
    for (const e of ['%ZZ', '%C3', '%ED%A0%80']) expect(err(uri('yt', YT, `v=1&t=${e}`))).toBe('bad-encoding');
    for (const text of ['<script>alert(1)</script>', 'violence://party/AB12CD', 'https://evil.example']) {
      expect(ok(songToUri(youtubeSong(YT, text, 'x')!)).title).toBe(text);
    }
  });

  it('the title never changes what plays', () => {
    const a = ok(uri('yt', YT, 'v=1&t=First'));
    const b = ok(uri('yt', YT, 'v=1&t=Totally%20Different'));
    expect(a.id).toBe(b.id);
    expect(songKey(a)).toBe(songKey(b));
    expect(songKey({ source: 'q', id: 'search', title: 'Hello', artist: 'Adele' })).not.toBe(songKey({ source: 'q', id: 'search', title: 'Hello', artist: 'Other' }));
  });
});

describe('incoming links', () => {
  const kind = (text: string) => parseIncoming(text).kind;
  const reason = (text: string | null | undefined) => {
    const r = parseIncoming(text);
    return r.kind === 'invalid' ? r.reason : r.kind;
  };

  it('reads a song, a profile card and a party in their forms', () => {
    expect(kind(`violence://song/yt/${YT}?v=1&t=Song`)).toBe('song');
    expect(kind(`${SONG_WEB_PREFIX}yt/${YT}?v=1`)).toBe('song');
    expect(parseIncoming(cardToUri(CARD))).toEqual({ kind: 'profile', card: CARD });
    expect(parseIncoming(cardToWebUrl(CARD))).toEqual({ kind: 'profile', card: CARD });
    for (const link of ['violence://party/AB12CD', 'violence://party/ab12cd', 'bitchord://party/AB12CD', 'violence://party?code=ab12cd', 'https://yashaswahuh.is-a.dev/Violence/party/AB12CD', 'https://yashaswahuh.is-a.dev/Violence/invite/AB12CD/']) {
      expect(parseIncoming(link)).toEqual({ kind: 'party', invite: { code: 'AB12CD', serverUrl: null } });
    }
    expect(incomingKey(parseIncoming('violence://party/AB12CD'))).toBe('party:AB12CD');
    expect(incomingKey(parseIncoming(cardToUri(CARD)))).toBe(`profile:${CARD.id}`);
  });

  it('normalises party codes like the server and validates the alphabet', () => {
    expect(normalizePartyCode('ILO012')).toBe('110012');
    for (const bad of ['ABC12', 'ABC1234', 'AB!2CD', '']) expect(normalizePartyCode(bad)).toBeNull();
    expect(reason('violence://party/ABC12')).toBe('malformed');
    expect(reason('violence://party/')).toBe('malformed');
    expect(reason('violence://party/AB12CD/extra')).toBe('malformed');
  });

  it('keeps a custom Jam server only when it is plain https', () => {
    const server = (q: string) => parsePartyLink(`violence://party/AB12CD${q}`)?.serverUrl;
    expect(server('?server=https%3A%2F%2Fjam.example.com')).toBe('https://jam.example.com');
    expect(server('?server=https%3A%2F%2Fjam.example.com%3A8443%2F')).toBe('https://jam.example.com:8443');
    for (const bad of ['http%3A%2F%2Fjam.example.com', 'https%3A%2F%2Fuser%3Apass%40jam.example.com', 'https%3A%2F%2Fjam.example.com%2Fpath', 'javascript%3Aalert(1)', 'https%3A%2F%2F']) {
      expect(server(`?server=${bad}`)).toBeNull();
    }
  });

  it('extra web bases carry only party codes', () => {
    const bases = ['https://friends.example.org/jam/'];
    expect(parseIncoming('https://friends.example.org/jam/party/AB12CD', bases).kind).toBe('party');
    expect(parseIncoming('https://friends.example.org/jam/party/AB12CD').kind).toBe('invalid');
    expect(parseIncoming(`https://friends.example.org/jam/song/yt/${YT}?v=1`, bases).kind).toBe('invalid');
    expect(parseIncoming('http://friends.example.org/jam/party/AB12CD', bases).kind).toBe('invalid');
  });

  it('refuses everything else by category', () => {
    expect(reason(`violence://song/yt/${YT}?v=2`)).toBe('unsupported-version');
    expect(reason('violence://song/yt/short?v=1')).toBe('malformed');
    expect(reason('violence://profile?v=2&id=x')).toBe('unsupported-version');
    for (const text of ['hello', 'https://example.org', 'http://yashaswahuh.is-a.dev/Violence/party/AB12CD', 'javascript:alert(1)', 'file:///etc/passwd', `violence://evil@song/yt/${YT}?v=1`, `https://yashaswahuh.is-a.dev.evil.example/Violence/song/yt/${YT}?v=1`]) {
      expect(reason(text)).toBe('not-a-violence-link');
    }
    expect(reason(null)).toBe('empty');
    expect(reason('   ')).toBe('empty');
    expect(reason('x'.repeat(513))).toBe('too-long');
    expect(JSON.stringify(parseIncoming(`violence://song/yt/${YT}?v=1&t=hunter2%00`))).not.toContain('hunter2');
  });

  it('finds a link inside a pasted message and keeps the most useful reason', () => {
    expect(extractIncoming(`Check this out!\nviolence://song/yt/${YT}?v=1&t=Song, it is great.`).kind).toBe('song');
    expect(extractIncoming('(<violence://party/AB12CD>)').kind).toBe('party');
    expect(extractIncoming('https://example.org/a violence://party/AB12CD').kind).toBe('party');
    expect(extractIncoming(`see https://example.org and violence://song/yt/${YT}?v=9`)).toEqual({ kind: 'invalid', reason: 'unsupported-version' });
    expect(extractIncoming('just words')).toEqual({ kind: 'invalid', reason: 'not-a-violence-link' });
    expect(extractIncoming('  ')).toEqual({ kind: 'invalid', reason: 'empty' });
  });
});
