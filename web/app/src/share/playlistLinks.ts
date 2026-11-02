/**
 * Playlist links: PlaylistLinks.kt, rule for rule (read side only; the web never builds them).
 *
 *   violence://playlist/yt/<playlistId>?v=1&t=<title>
 *   violence://playlist/songs?v=1&t=<title>&s=<id>,<id>,...
 *   https://yashaswahuh.is-a.dev/Violence/playlist/... (the same paths)
 *
 * Everything that arrives is untrusted: parsing never throws, never repairs and never echoes the
 * input. The title is a label only. An optional "sent by" card (f, fn, fa, fc) is accepted and
 * ignored here; the app shows it.
 */

import { percentDecode } from '@/profile/card';
import { checkText } from '@/profile/rules';
import { isYouTubeId } from './songLinks';

export const PLAYLIST_VERSION = '1';
export const PLAYLIST_CUSTOM_PREFIX = 'violence://playlist/';
export const PLAYLIST_WEB_PREFIX = 'https://yashaswahuh.is-a.dev/Violence/playlist/';
export const PLAYLIST_MAX_INPUT_CHARS = 512;
export const PLAYLIST_TITLE_MAX = 60;
export const PLAYLIST_MAX_SONGS = 30;
const SENDER_KEYS = ['f', 'fn', 'fa', 'fc'];

export interface PlaylistRef {
  kind: 'yt' | 'songs';
  id: string | null;
  title: string;
  videoIds: string[];
}

export type PlaylistResult = { ok: true; ref: PlaylistRef } | { ok: false; wrongVersion: boolean };

const rejected = (wrongVersion = false): PlaylistResult => ({ ok: false, wrongVersion });

export const isPlaylistId = (id: string): boolean => /^[A-Za-z0-9_-]{10,64}$/.test(id);

export function parsePlaylistLink(input: string): PlaylistResult {
  const text = input.trim();
  if (text.length > PLAYLIST_MAX_INPUT_CHARS) return rejected();
  const noFragment = text.split('#')[0] ?? '';
  const lower = noFragment.toLowerCase();
  let prefixLength: number;
  if (lower.startsWith(PLAYLIST_CUSTOM_PREFIX)) prefixLength = PLAYLIST_CUSTOM_PREFIX.length;
  else if (lower.startsWith(PLAYLIST_WEB_PREFIX.toLowerCase())) prefixLength = PLAYLIST_WEB_PREFIX.length;
  else return rejected();

  const rest = noFragment.slice(prefixLength);
  const q = rest.indexOf('?');
  if (q < 0) return rejected();
  const segments = rest.slice(0, q).split('/');
  let kind: 'yt' | 'songs';
  let id: string | null = null;
  const second = segments[1] ?? '';
  if (segments.length === 2 && segments[0] === 'yt' && isPlaylistId(second)) {
    kind = 'yt';
    id = second;
  } else if (segments.length === 1 && segments[0] === 'songs') {
    kind = 'songs';
  } else {
    return rejected();
  }

  const fields = new Map<string, string>();
  for (const pair of rest.slice(q + 1).split('&')) {
    if (pair === '') continue;
    const eq = pair.indexOf('=');
    const key = eq < 0 ? pair : pair.slice(0, eq);
    const value = eq < 0 ? '' : pair.slice(eq + 1);
    if (key !== 'v' && key !== 't' && key !== 's' && !SENDER_KEYS.includes(key)) return rejected();
    if (fields.has(key)) return rejected();
    fields.set(key, value);
  }
  const version = fields.get('v');
  if (version === undefined) return rejected();
  if (version !== PLAYLIST_VERSION) return rejected(true);

  const rawTitle = fields.get('t');
  if (rawTitle === undefined) return rejected();
  const decoded = percentDecode(rawTitle);
  if (decoded === null) return rejected();
  const checked = checkText(decoded, PLAYLIST_TITLE_MAX);
  if (!checked.ok) return rejected();

  let videoIds: string[] = [];
  if (kind === 'songs') {
    const list = fields.get('s');
    if (list === undefined) return rejected();
    videoIds = list.split(',');
    if (videoIds.length === 0 || videoIds.length > PLAYLIST_MAX_SONGS || !videoIds.every(isYouTubeId)) return rejected();
  } else if (fields.has('s')) {
    return rejected();
  }
  return { ok: true, ref: { kind, id, title: checked.name, videoIds } };
}

/** The `violence://` form of a web playlist link (same path and query). */
export function playlistToAppUrl(webUrl: string): string {
  return PLAYLIST_CUSTOM_PREFIX + webUrl.slice(PLAYLIST_WEB_PREFIX.length);
}
