/**
 * Reading an incoming link (IncomingLinks.kt, PartyLinks.kt): a song, a profile card or a Jam
 * invite, strictly. Used by the Friends page "paste a link" box. Plain http is never accepted and a
 * result is only ever a candidate: the page asks before it does anything.
 */

import { CUSTOM_PREFIX as PROFILE_PREFIX, parseCard, WEB_BASE as PROFILE_WEB, type ProfileCard } from '@/profile/card';
import { parseSongLink, SONG_CUSTOM_PREFIX, SONG_WEB_PREFIX, songKey, type SongRef } from './songLinks';

export const SITE_ORIGIN = 'https://yashaswahuh.is-a.dev/Violence';
const PARTY_ALPHABET = '0123456789ABCDEFGHJKMNPQRSTUVWXYZ';
const MAX_INPUT_CHARS = 512;

export type InvalidReason = 'empty' | 'too-long' | 'not-a-violence-link' | 'unsupported-version' | 'malformed';

export interface PartyInvite {
  code: string;
  serverUrl: string | null;
}

export type IncomingLink =
  | { kind: 'song'; ref: SongRef }
  | { kind: 'profile'; card: ProfileCard }
  | { kind: 'party'; invite: PartyInvite }
  | { kind: 'invalid'; reason: InvalidReason };

/** The normalised party code (case, and I/L to 1, O to 0, as the Jam server does), or null. */
export function normalizePartyCode(raw: string): string | null {
  if (raw.length !== 6) return null;
  let out = '';
  for (const c of raw.toUpperCase()) {
    const mapped = c === 'I' || c === 'L' ? '1' : c === 'O' ? '0' : c;
    if (!PARTY_ALPHABET.includes(mapped)) return null;
    out += mapped;
  }
  return out;
}

function queryParam(query: string | null, name: string): string | null {
  if (!query) return null;
  for (const pair of query.split('&')) {
    const eq = pair.indexOf('=');
    if (eq > 0 && pair.slice(0, eq).toLowerCase() === name) {
      try {
        return decodeURIComponent(pair.slice(eq + 1));
      } catch {
        return null;
      }
    }
  }
  return null;
}

function cleanServer(raw: string | null): string | null {
  if (!raw) return null;
  const trimmed = raw.trim().replace(/\/+$/, '');
  return trimmed.length > 0 && trimmed.length <= 200 && /^https:\/\/[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?(:[0-9]{1,5})?$/.test(trimmed) ? trimmed : null;
}

export function parsePartyLink(input: string, extraBases: readonly string[] = []): PartyInvite | null {
  const text = input.trim();
  if (text === '' || text.length > MAX_INPUT_CHARS) return null;
  const noFragment = text.split('#')[0]!;
  const q = noFragment.indexOf('?');
  const path = q < 0 ? noFragment : noFragment.slice(0, q);
  const query = q < 0 ? null : noFragment.slice(q + 1);
  const lower = path.toLowerCase();

  let code: string | null = null;
  if (lower.startsWith('violence://party') || lower.startsWith('bitchord://party')) {
    const rest = path.slice(path.toLowerCase().indexOf('://party') + '://party'.length).replace(/^\/+|\/+$/g, '');
    code = rest !== '' ? rest : queryParam(query, 'code');
  } else {
    const bases = [SITE_ORIGIN, ...extraBases.map((b) => b.trim().replace(/\/+$/, '')).filter((b) => /^https:\/\//i.test(b))];
    for (const base of bases) {
      for (const kind of ['party', 'invite']) {
        const prefix = `${base}/${kind}/`.toLowerCase();
        if (lower.startsWith(prefix)) {
          const candidate = path.slice(prefix.length).replace(/\/+$/, '');
          if (!candidate.includes('/')) code = candidate;
        }
      }
    }
  }
  if (code === null) return null;
  const normalized = normalizePartyCode(code);
  return normalized === null ? null : { code: normalized, serverUrl: cleanServer(queryParam(query, 'server')) };
}

const looksLikeParty = (lower: string): boolean =>
  lower.startsWith('violence://party') || lower.startsWith('bitchord://party') || lower.startsWith(`${SITE_ORIGIN.toLowerCase()}/party/`) || lower.startsWith(`${SITE_ORIGIN.toLowerCase()}/invite/`);

export function parseIncoming(input: string | null | undefined, extraBases: readonly string[] = []): IncomingLink {
  const text = (input ?? '').trim();
  if (text === '') return { kind: 'invalid', reason: 'empty' };
  if (text.length > MAX_INPUT_CHARS) return { kind: 'invalid', reason: 'too-long' };
  const lower = text.toLowerCase();

  if (lower.startsWith(SONG_CUSTOM_PREFIX) || lower.startsWith(SONG_WEB_PREFIX.toLowerCase())) {
    const r = parseSongLink(text);
    return r.ok ? { kind: 'song', ref: r.ref } : { kind: 'invalid', reason: r.error === 'wrong-version' ? 'unsupported-version' : 'malformed' };
  }
  const base = lower.split('?')[0]!.split('#')[0]!.replace(/\/+$/, '');
  if (base === PROFILE_PREFIX || base === PROFILE_WEB.toLowerCase()) {
    const r = parseCard(text);
    return r.ok ? { kind: 'profile', card: r.card } : { kind: 'invalid', reason: r.error === 'wrong-version' ? 'unsupported-version' : 'malformed' };
  }
  const party = parsePartyLink(text, extraBases);
  if (party) return { kind: 'party', invite: party };
  return { kind: 'invalid', reason: looksLikeParty(lower) ? 'malformed' : 'not-a-violence-link' };
}

/** A link inside a pasted message: the first token that parses, else the most useful reason. */
export function extractIncoming(text: string | null | undefined, extraBases: readonly string[] = []): IncomingLink {
  const input = (text ?? '').trim();
  if (input === '') return { kind: 'invalid', reason: 'empty' };
  if (input.length > MAX_INPUT_CHARS) return { kind: 'invalid', reason: 'too-long' };
  let best: IncomingLink = { kind: 'invalid', reason: 'not-a-violence-link' };
  for (const token of input.split(/\s+/)) {
    if (!token.includes('://')) continue;
    const candidate = parseIncoming(token.replace(/^[<(["']+|[>)\]"',.]+$/g, ''), extraBases);
    if (candidate.kind !== 'invalid') return candidate;
    if (candidate.reason !== 'not-a-violence-link') best = candidate;
  }
  return best;
}

export const incomingKey = (link: IncomingLink): string | null =>
  link.kind === 'song' ? songKey(link.ref) : link.kind === 'profile' ? `profile:${link.card.id}` : link.kind === 'party' ? `party:${link.invite.code}` : null;

export const INVALID_TEXT: Record<InvalidReason, string> = {
  empty: 'Nothing to open.',
  'too-long': 'That link is too long.',
  'not-a-violence-link': 'That is not a Violence link.',
  'unsupported-version': 'This link is from a newer version of Violence.',
  malformed: 'That Violence link is damaged or not valid.',
};
