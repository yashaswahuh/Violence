/**
 * The profile-card link, version 1: ProfileCardCodec.kt, case for case.
 *
 *   violence://profile?v=1&id=<uuid>&n=<name, percent-encoded>&a=<avatarId>&c=<hex6>
 *   https://yashaswahuh.is-a.dev/Violence/profile?v=1&id=...        (same query)
 *
 * Everything that arrives here is untrusted: parse never throws and never "repairs" a value.
 * It returns a fully valid card or says why not, by code only (the input is never echoed).
 */

import { isKnownAvatar } from './catalog';
import { checkName, normalizeColor, normalizeId, type NameProblem } from './rules';

export const CARD_VERSION = '1';
export const CUSTOM_PREFIX = 'violence://profile';
export const WEB_BASE = 'https://yashaswahuh.is-a.dev/Violence/profile';
export const MAX_INPUT_CHARS = 512;
export const NFC_TARGET_BYTES = 200;

export interface ProfileCard {
  id: string;
  displayName: string;
  avatarId: string;
  color: string;
}

export type CardError =
  | 'not-a-card'
  | 'too-long'
  | 'wrong-version'
  | 'missing-field'
  | 'duplicate-field'
  | 'unknown-field'
  | 'bad-id'
  | 'bad-encoding'
  | 'bad-name'
  | 'bad-avatar'
  | 'bad-color';

export type CardResult =
  | { ok: true; card: ProfileCard }
  | { ok: false; error: CardError; nameProblem?: NameProblem };

const KEYS = new Set(['v', 'id', 'n', 'a', 'c']);

export function percentEncode(value: string): string {
  let out = '';
  for (const byte of new TextEncoder().encode(value)) {
    const c = String.fromCharCode(byte);
    out += byte < 0x80 && /[A-Za-z0-9\-._~]/.test(c) ? c : `%${byte.toString(16).toUpperCase().padStart(2, '0')}`;
  }
  return out;
}

/** `%XX` escapes (and `+` as a space) into UTF-8; null on a bad escape or invalid UTF-8. */
export function percentDecode(value: string): string | null {
  const bytes: number[] = [];
  const encoder = new TextEncoder();
  for (let i = 0; i < value.length; ) {
    const c = value[i]!;
    const code = value.charCodeAt(i);
    if (c === '%') {
      const hex = value.slice(i + 1, i + 3);
      if (!/^[0-9a-fA-F]{2}$/.test(hex)) return null;
      bytes.push(parseInt(hex, 16));
      i += 3;
    } else if (c === '+') {
      bytes.push(0x20);
      i++;
    } else if (code < 0x21 || code === 0x7f) {
      return null;
    } else if (code < 0x80) {
      bytes.push(code);
      i++;
    } else {
      // A raw non-ASCII character: take it whole (a surrogate pair is one character) as UTF-8.
      const cp = value.codePointAt(i)!;
      const ch = String.fromCodePoint(cp);
      bytes.push(...encoder.encode(ch));
      i += ch.length;
    }
  }
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes));
  } catch {
    return null;
  }
}

function query(card: ProfileCard): string {
  return `?v=${CARD_VERSION}&id=${card.id}&n=${percentEncode(card.displayName)}&a=${card.avatarId}&c=${card.color}`;
}

export const cardToUri = (card: ProfileCard): string => CUSTOM_PREFIX + query(card);
export const cardToWebUrl = (card: ProfileCard): string => WEB_BASE + query(card);
export const fitsNfcTarget = (uri: string): boolean => new TextEncoder().encode(uri).length <= NFC_TARGET_BYTES;

function queryOf(text: string): string | null {
  const noFragment = text.split('#')[0]!;
  if (!noFragment.includes('?')) return null;
  const base = noFragment.slice(0, noFragment.indexOf('?')).replace(/\/+$/, '');
  const isCard = base.toLowerCase() === CUSTOM_PREFIX || base.toLowerCase() === WEB_BASE.toLowerCase();
  return isCard ? noFragment.slice(noFragment.indexOf('?') + 1) : null;
}

export function parseCard(input: string | null | undefined): CardResult {
  const text = (input ?? '').trim();
  if (text.length > MAX_INPUT_CHARS) return { ok: false, error: 'too-long' };
  const queryText = queryOf(text);
  if (queryText === null) return { ok: false, error: 'not-a-card' };

  const fields = new Map<string, string>();
  for (const pair of queryText.split('&')) {
    if (pair === '') continue;
    const eq = pair.indexOf('=');
    const key = eq < 0 ? pair : pair.slice(0, eq);
    const value = eq < 0 ? '' : pair.slice(eq + 1);
    if (!KEYS.has(key)) return { ok: false, error: 'unknown-field' };
    if (fields.has(key)) return { ok: false, error: 'duplicate-field' };
    fields.set(key, value);
  }

  const version = fields.get('v');
  if (version === undefined) return { ok: false, error: 'missing-field' };
  if (version !== CARD_VERSION) return { ok: false, error: 'wrong-version' };
  const rawId = fields.get('id');
  const rawName = fields.get('n');
  const rawAvatar = fields.get('a');
  const rawColor = fields.get('c');
  if (rawId === undefined || rawName === undefined || rawAvatar === undefined || rawColor === undefined) {
    return { ok: false, error: 'missing-field' };
  }

  const id = normalizeId(rawId);
  if (!id) return { ok: false, error: 'bad-id' };
  const decoded = percentDecode(rawName);
  if (decoded === null) return { ok: false, error: 'bad-encoding' };
  const name = checkName(decoded);
  if (!name.ok) return { ok: false, error: 'bad-name', nameProblem: name.problem };
  if (!isKnownAvatar(rawAvatar)) return { ok: false, error: 'bad-avatar' };
  const color = normalizeColor(rawColor);
  if (!color) return { ok: false, error: 'bad-color' };
  return { ok: true, card: { id, displayName: name.name, avatarId: rawAvatar, color } };
}
