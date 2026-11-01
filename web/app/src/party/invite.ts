/**
 * Party invites (JamInviteLink.kt on Android). Shapes, all carrying the same six-character code:
 *  - `violence://party/<CODE>[?server=<jam server>]`: opens the app.
 *  - `<VITE_INVITE_BASE_URL>/party/<CODE>[?server=...]`: this app's landing page. Only built when a
 *    base is configured; **empty means "share only the code"**.
 *  - `.../invite/<CODE>`: the older shape (the Jam server serves one too); this app joins from it.
 * The site is served under a base path (/Violence/), so the page paths below are matched under any prefix.
 */

import { config } from '@/config';

export const CODE_LENGTH = 6;

/** The letters and digits the Jam server mints codes from (no I, L or O). */
const CODE_ALPHABET = '0123456789ABCDEFGHJKMNPQRSTUVWXYZ';

/** Letters and digits only, upper case; null unless it's six of them. */
export function cleanCode(raw: string): string | null {
  const cleaned = raw.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
  return cleaned.length === CODE_LENGTH ? cleaned : null;
}

export function sanitizeServerUrl(raw: string | null | undefined): string | null {
  const trimmed = raw?.trim().replace(/\/+$/, '');
  if (!trimmed) return null;
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    return new URL(withScheme).host ? withScheme : null;
  } catch {
    return null;
  }
}

export function parseInvite(value: string): { code: string; serverUrl: string | null } | null {
  let url: URL;
  try {
    url = new URL(value.trim());
  } catch {
    return null;
  }
  const server = sanitizeServerUrl(url.searchParams.get('server'));
  if ((url.protocol === 'violence:' || url.protocol === 'bitchord:') && url.host.toLowerCase() === 'party') {
    const code = cleanCode(url.pathname.replace(/^\/+|\/+$/g, '') || url.searchParams.get('code') || '');
    return code ? { code, serverUrl: server } : null;
  }
  const match = url.pathname.match(/^(?:\/[^/]+)*\/(?:party|invite)\/([A-Za-z0-9]{6})\/?$/i);
  if ((url.protocol === 'https:' || url.protocol === 'http:') && match) return { code: match[1]!.toUpperCase(), serverUrl: server };
  return null;
}

/**
 * The party code inside whatever somebody pasted or typed, or null: a bare code (any case, with spaces
 * or hyphens), a full invite link, or a whole share message holding either. Free text is only trusted
 * when it holds exactly one six-character token made of code letters, so words are not read as a code.
 */
export function extractCode(text: string | null | undefined): string | null {
  const input = text?.trim() ?? '';
  if (!input) return null;
  for (const token of input.split(/\s+/)) {
    if (token.includes('://')) {
      const parsed = parseInvite(token.replace(/^[<("',.]+|[>)",'.]+$/g, ''));
      if (parsed) return parsed.code;
    }
  }
  if (!input.includes('://')) {
    const squashed = cleanCode(input);
    if (squashed) return squashed;
  }
  const candidates = [
    ...new Set(
      input
        .split(/[^A-Za-z0-9]+/)
        .map((t) => t.toUpperCase())
        .filter((t) => t.length === CODE_LENGTH && [...t].every((c) => CODE_ALPHABET.includes(c))),
    ),
  ];
  return candidates.length === 1 ? candidates[0]! : null;
}

/** The configured base for shared links (VITE_INVITE_BASE_URL), or '' when none. */
export function inviteBase(raw: string = config.inviteBaseUrl): string {
  const trimmed = raw.trim().replace(/\/+$/, '');
  if (!trimmed) return '';
  return /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
}

/** The web link to share (the landing page), or null when no base is configured: share only the code. */
export function inviteLink(code: string, customServer: string | null = null, base: string = config.inviteBaseUrl): string | null {
  const origin = inviteBase(base);
  if (!origin) return null;
  const link = `${origin}/party/${code.toUpperCase()}`;
  const server = customServer?.trim().replace(/\/+$/, '');
  return server ? `${link}?server=${encodeURIComponent(server)}` : link;
}

/** `violence://party/<CODE>`: opens the Android app. Carries the Jam server when it is a custom one. */
export function appLink(code: string, customServer: string | null = null): string {
  const server = customServer?.trim().replace(/\/+$/, '');
  return `violence://party/${code.toUpperCase()}${server ? `?server=${encodeURIComponent(server)}` : ''}`;
}
