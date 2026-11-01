/**
 * Song links: SongLinks.kt, rule for rule.
 *
 *   violence://song/<source>/<id>?v=1&t=<title>&a=<artist>
 *   https://yashaswahuh.is-a.dev/Violence/song/<source>/<id>?v=1&t=<title>&a=<artist>
 *
 * `yt` is a YouTube video id (11 characters), `q` a search by words (id `search`, title required),
 * `sp` a Spotify track id (22 base62 characters, title required, searched by words). Everything
 * that arrives is untrusted: parsing never throws, never truncates and never echoes the input.
 * The title and artist are display labels only; a `yt` song is always looked up by its id.
 */

import { percentDecode, percentEncode } from '@/profile/card';
import { checkText } from '@/profile/rules';

export type SongSource = 'yt' | 'q' | 'sp';

export interface SongRef {
  source: SongSource;
  id: string;
  title: string | null;
  artist: string | null;
}

export type SongError =
  | 'not-a-song-link'
  | 'too-long'
  | 'wrong-version'
  | 'missing-field'
  | 'duplicate-field'
  | 'unknown-field'
  | 'bad-source'
  | 'bad-id'
  | 'bad-encoding'
  | 'bad-title'
  | 'bad-artist';

export type SongResult = { ok: true; ref: SongRef } | { ok: false; error: SongError };

export const SONG_VERSION = '1';
export const SONG_CUSTOM_PREFIX = 'violence://song/';
export const SONG_WEB_PREFIX = 'https://yashaswahuh.is-a.dev/Violence/song/';
export const SONG_TARGET_BYTES = 200;
export const SONG_MAX_INPUT_CHARS = 512;
export const TITLE_MAX = 120;
export const ARTIST_MAX = 80;
export const SEARCH_ID = 'search';

export const isYouTubeId = (id: string): boolean => /^[A-Za-z0-9_-]{11}$/.test(id);
export const isSpotifyId = (id: string): boolean => /^[A-Za-z0-9]{22}$/.test(id);

const SOURCES: readonly SongSource[] = ['yt', 'q', 'sp'];

/** [raw] without characters a link may not carry, spaces collapsed, at most [max] code points. */
export function sanitizeHint(raw: string, max: number): string {
  let kept = '';
  for (const char of raw) {
    const checked = checkText(char, 1);
    if (checked.ok) kept += char;
    else if (char.trim() === '') kept += ' ';
  }
  const collapsed = checkText(kept.replace(/ +/g, ' ').trim(), Number.MAX_SAFE_INTEGER);
  if (!collapsed.ok) return '';
  return truncateCodePoints(collapsed.name, max);
}

export function truncateCodePoints(text: string, max: number): string {
  const points = [...text];
  return points.length <= max ? text : points.slice(0, max).join('').trimEnd();
}

const hint = (raw: string | null | undefined, max: number): string | null => {
  const clean = raw ? sanitizeHint(raw, max) : '';
  return clean === '' ? null : clean;
};

export function youtubeSong(videoId: string, title?: string | null, artist?: string | null): SongRef | null {
  return isYouTubeId(videoId) ? { source: 'yt', id: videoId, title: hint(title, TITLE_MAX), artist: hint(artist, ARTIST_MAX) } : null;
}

export function searchSong(title?: string | null, artist?: string | null): SongRef | null {
  const t = hint(title, TITLE_MAX);
  return t === null ? null : { source: 'q', id: SEARCH_ID, title: t, artist: hint(artist, ARTIST_MAX) };
}

const bytes = (text: string) => new TextEncoder().encode(text).length;

function build(prefix: string, ref: SongRef): string {
  let out = `${prefix}${ref.source}/${ref.id}?v=${SONG_VERSION}`;
  if (ref.title !== null) out += `&t=${percentEncode(ref.title)}`;
  if (ref.artist !== null) out += `&a=${percentEncode(ref.artist)}`;
  return out;
}

/** [ref] with the hints shortened until the `violence://` form fits [maxBytes]. */
export function fitSong(ref: SongRef, maxBytes: number = SONG_TARGET_BYTES): SongRef {
  let title = ref.title;
  let artist = ref.artist;
  const dropLast = (text: string) => [...text].slice(0, -1).join('').trimEnd();
  while (bytes(build(SONG_CUSTOM_PREFIX, { ...ref, title, artist })) > maxBytes) {
    const tLen = title ? percentEncode(title).length : 0;
    const aLen = artist ? percentEncode(artist).length : 0;
    if (tLen === 0 && aLen === 0) break;
    if (aLen >= tLen) artist = artist ? dropLast(artist) : artist;
    else title = title ? dropLast(title) : title;
  }
  return { ...ref, title: title || null, artist: artist || null };
}

export const songToUri = (ref: SongRef, maxBytes?: number): string => build(SONG_CUSTOM_PREFIX, fitSong(ref, maxBytes));
export const songToWebUrl = (ref: SongRef, maxBytes?: number): string => build(SONG_WEB_PREFIX, fitSong(ref, maxBytes));

/** The same song whatever hints came with it: used to recognise a repeated link. */
export const songKey = (ref: SongRef): string =>
  `song:${ref.source}:${ref.source === 'q' ? [ref.title, ref.artist].filter(Boolean).join(' ').trim() : ref.id}`;

const reject = (error: SongError): SongResult => ({ ok: false, error });

export function parseSongLink(input: string | null | undefined): SongResult {
  const text = (input ?? '').trim();
  if (text.length > SONG_MAX_INPUT_CHARS) return reject('too-long');
  const noFragment = text.split('#')[0]!;
  const lower = noFragment.toLowerCase();
  const prefixLength = lower.startsWith(SONG_CUSTOM_PREFIX)
    ? SONG_CUSTOM_PREFIX.length
    : lower.startsWith(SONG_WEB_PREFIX.toLowerCase())
      ? SONG_WEB_PREFIX.length
      : -1;
  if (prefixLength < 0) return reject('not-a-song-link');

  const rest = noFragment.slice(prefixLength);
  const q = rest.indexOf('?');
  const path = q < 0 ? rest : rest.slice(0, q);
  const query = q < 0 ? null : rest.slice(q + 1);
  const segments = path.split('/');
  if (segments.length !== 2) return reject('not-a-song-link');
  const [sourceText, id] = segments as [string, string];
  const source = SOURCES.find((s) => s === sourceText);
  if (!source) return reject('bad-source');
  if (source === 'yt' && !isYouTubeId(id)) return reject('bad-id');
  if (source === 'sp' && !isSpotifyId(id)) return reject('bad-id');
  if (source === 'q' && id !== SEARCH_ID) return reject('bad-id');
  if (query === null) return reject('missing-field');

  const fields = new Map<string, string>();
  for (const pair of query.split('&')) {
    if (pair === '') continue;
    const eq = pair.indexOf('=');
    const key = eq < 0 ? pair : pair.slice(0, eq);
    const value = eq < 0 ? '' : pair.slice(eq + 1);
    if (key !== 'v' && key !== 't' && key !== 'a') return reject('unknown-field');
    if (fields.has(key)) return reject('duplicate-field');
    fields.set(key, value);
  }
  const version = fields.get('v');
  if (version === undefined) return reject('missing-field');
  if (version !== SONG_VERSION) return reject('wrong-version');

  let title: string | null = null;
  let artist: string | null = null;
  if (fields.has('t')) {
    const decoded = percentDecode(fields.get('t')!);
    if (decoded === null) return reject('bad-encoding');
    const checked = checkText(decoded, TITLE_MAX);
    if (!checked.ok) return reject('bad-title');
    title = checked.name;
  }
  if (fields.has('a')) {
    const decoded = percentDecode(fields.get('a')!);
    if (decoded === null) return reject('bad-encoding');
    const checked = checkText(decoded, ARTIST_MAX);
    if (!checked.ok) return reject('bad-artist');
    artist = checked.name;
  }
  if ((source === 'q' || source === 'sp') && title === null) return reject('missing-field');
  return { ok: true, ref: { source, id, title, artist } };
}
