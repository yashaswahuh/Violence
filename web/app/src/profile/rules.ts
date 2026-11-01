/**
 * What the in-app profile may contain: ProfileRules.kt, rule for rule. The same checks run on a
 * typed name, a name inside a friend's card and a name read back from storage.
 */

export const NAME_MAX_CODE_POINTS = 24;
/** The Jam server rejects display names longer than this many UTF-8 bytes. */
export const JAM_NAME_MAX_BYTES = 80;

export type NameProblem = 'empty' | 'too-long' | 'control-character' | 'invisible-character';
export type NameResult = { ok: true; name: string } | { ok: false; problem: NameProblem };

/** Characters that render as nothing but are not caught by their Unicode category. */
const INVISIBLE_LOOKALIKES = new Set([0x034f, 0x115f, 0x1160, 0x17b4, 0x17b5, 0x2800, 0x3164, 0xffa0]);

/** More stacked combining marks than this in a row is "zalgo" text, not a name. */
const MAX_COMBINING_RUN = 4;

const CONTROL = /^\p{Cc}$/u;
const INVISIBLE = /^[\p{Cf}\p{Zl}\p{Zp}\p{Co}\p{Cs}]$/u;
const SPACE = /^\p{Zs}$/u;
const MARK = /^[\p{Mn}\p{Me}]$/u;

type Kind = 'control' | 'invisible' | 'space' | 'mark' | 'other';

function classify(codePoint: number, char: string): Kind {
  if (INVISIBLE_LOOKALIKES.has(codePoint)) return 'invisible';
  if (codePoint >= 0x10000) {
    if (codePoint >= 0xe0000 && codePoint <= 0xe007f) return 'invisible'; // tag characters
    if (codePoint >= 0xf0000) return 'invisible'; // planes 15 and 16: private use
    if ((codePoint & 0xffff) >= 0xfffe) return 'invisible'; // non-characters
    if (codePoint >= 0xe0100 && codePoint <= 0xe01ef) return 'mark';
    return 'other';
  }
  if ((codePoint >= 0xfdd0 && codePoint <= 0xfdef) || codePoint === 0xfffe || codePoint === 0xffff) return 'invisible';
  if (codePoint === 0x200d) return 'other'; // zero-width joiner, needed by emoji sequences
  if (CONTROL.test(char)) return 'control';
  if (INVISIBLE.test(char)) return 'invisible';
  if (SPACE.test(char)) return 'space';
  if (MARK.test(char)) return codePoint === 0xfe0f || codePoint === 0xfe0e ? 'other' : 'mark';
  return 'other';
}

/** Trims, collapses spaces and checks [raw]. Too long is rejected, never truncated. */
export function checkName(raw: string): NameResult {
  const kept: string[] = [];
  let combiningRun = 0;
  let pendingSpace = false;
  for (const char of raw) {
    const codePoint = char.codePointAt(0)!;
    const kind = classify(codePoint, char);
    if (kind === 'control') return { ok: false, problem: 'control-character' };
    if (kind === 'invisible') return { ok: false, problem: 'invisible-character' };
    if (kind === 'space') {
      pendingSpace = kept.length > 0;
      combiningRun = 0;
      continue;
    }
    if (kind === 'mark') {
      combiningRun++;
      if (combiningRun > MAX_COMBINING_RUN) return { ok: false, problem: 'invisible-character' };
    } else {
      combiningRun = 0;
    }
    if (pendingSpace) {
      kept.push(' ');
      pendingSpace = false;
    }
    kept.push(char);
  }
  if (kept.length === 0) return { ok: false, problem: 'empty' };
  if (kept.length > NAME_MAX_CODE_POINTS) return { ok: false, problem: 'too-long' };
  return { ok: true, name: kept.join('') };
}

export function normalizeName(raw: string): string | null {
  const result = checkName(raw);
  return result.ok ? result.name : null;
}

/** The canonical lower-case dashed UUID for [raw] (dashed or 32 plain hex digits), or null. */
export function normalizeId(raw: string): string | null {
  const text = raw.trim().toLowerCase();
  let hex: string;
  if (text.length === 36) {
    if (text[8] !== '-' || text[13] !== '-' || text[18] !== '-' || text[23] !== '-') return null;
    hex = text.replace(/-/g, '');
  } else if (text.length === 32) {
    hex = text;
  } else {
    return null;
  }
  if (!/^[0-9a-f]{32}$/.test(hex)) return null;
  if (/^0+$/.test(hex)) return null;
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/** Six hex digits (no `#`), lower-cased, or null. */
export function normalizeColor(raw: string): string | null {
  const text = raw.trim().toLowerCase();
  return /^[0-9a-f]{6}$/.test(text) ? text : null;
}

/** [name] cut so its UTF-8 form is at most [maxBytes] bytes, never inside a character. */
export function truncateUtf8(name: string, maxBytes: number = JAM_NAME_MAX_BYTES): string {
  const encoder = new TextEncoder();
  if (encoder.encode(name).length <= maxBytes) return name;
  let out = '';
  let bytes = 0;
  for (const char of name) {
    const size = encoder.encode(char).length;
    if (bytes + size > maxBytes) break;
    bytes += size;
    out += char;
  }
  return out.trimEnd();
}

export function codePointLength(text: string): number {
  let n = 0;
  for (const _ of text) n++;
  return n;
}
