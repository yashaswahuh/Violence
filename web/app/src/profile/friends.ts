/**
 * The friends list rules (Friends.kt) and the profile shapes, as plain functions on plain data so
 * they are testable without a browser and shared by every page.
 */

import { isKnownAvatar, DEFAULT_AVATAR_ID, DEFAULT_COLOR } from './catalog';
import type { ProfileCard } from './card';
import { normalizeColor, normalizeId, normalizeName, truncateUtf8 } from './rules';

/** A sanity bound on stored data, far above any circle of friends this app is for. */
export const MAX_FRIENDS = 200;

export interface InAppProfile {
  id: string;
  displayName: string;
  avatarId: string;
  color: string;
  createdAt: number;
}

export interface FriendCard {
  id: string;
  displayName: string;
  avatarId: string;
  color: string;
  addedAt: number;
  /** A name the user gave them, local only. */
  nickname?: string | null;
}

export const shownName = (friend: FriendCard): string => (friend.nickname && friend.nickname.trim()) || friend.displayName;

export const cardOf = (profile: InAppProfile): ProfileCard => ({
  id: profile.id,
  displayName: profile.displayName,
  avatarId: profile.avatarId,
  color: profile.color,
});

export type AddResult =
  | { kind: 'added'; list: FriendCard[] }
  | { kind: 'updated'; list: FriendCard[] }
  | { kind: 'is-you' }
  | { kind: 'full' };

export function addFriend(list: FriendCard[], card: ProfileCard, selfId: string | null, now: number): AddResult {
  if (selfId !== null && card.id === selfId) return { kind: 'is-you' };
  const index = list.findIndex((f) => f.id === card.id);
  if (index >= 0) {
    const next = list.slice();
    next[index] = { ...list[index]!, displayName: card.displayName, avatarId: card.avatarId, color: card.color };
    return { kind: 'updated', list: next };
  }
  if (list.length >= MAX_FRIENDS) return { kind: 'full' };
  return { kind: 'added', list: [...list, { ...card, addedAt: now, nickname: null }] };
}

/** Sets or clears a nickname. Blank clears it; an invalid one returns null and changes nothing. */
export function renameFriend(list: FriendCard[], id: string, nickname: string): FriendCard[] | null {
  const index = list.findIndex((f) => f.id === id);
  if (index < 0) return list;
  let clean: string | null = null;
  if (nickname.trim() !== '') {
    clean = normalizeName(nickname);
    if (clean === null) return null;
  }
  const next = list.slice();
  next[index] = { ...list[index]!, nickname: clean };
  return next;
}

export const removeFriend = (list: FriendCard[], id: string): FriendCard[] => list.filter((f) => f.id !== id);

export const sortedFriends = (list: FriendCard[]): FriendCard[] =>
  list.slice().sort((a, b) => shownName(a).toLowerCase().localeCompare(shownName(b).toLowerCase()));

// ---- Storage is untrusted too ------------------------------------------------------------------

export function sanitizeProfile(raw: unknown): InAppProfile | null {
  if (typeof raw !== 'object' || raw === null) return null;
  const r = raw as Record<string, unknown>;
  const id = typeof r.id === 'string' ? normalizeId(r.id) : null;
  const name = typeof r.displayName === 'string' ? normalizeName(r.displayName) : null;
  if (!id || !name) return null;
  return {
    id,
    displayName: name,
    avatarId: typeof r.avatarId === 'string' && isKnownAvatar(r.avatarId) ? r.avatarId : DEFAULT_AVATAR_ID,
    color: (typeof r.color === 'string' && normalizeColor(r.color)) || DEFAULT_COLOR,
    createdAt: typeof r.createdAt === 'number' ? r.createdAt : 0,
  };
}

export function sanitizeFriends(raw: unknown): FriendCard[] {
  if (!Array.isArray(raw)) return [];
  const seen = new Set<string>();
  const out: FriendCard[] = [];
  for (const entry of raw) {
    const profile = sanitizeProfile({ ...(entry as object), createdAt: 0 });
    if (!profile || seen.has(profile.id)) continue;
    seen.add(profile.id);
    const nick = (entry as { nickname?: unknown }).nickname;
    out.push({
      id: profile.id,
      displayName: profile.displayName,
      avatarId: profile.avatarId,
      color: profile.color,
      addedAt: typeof (entry as { addedAt?: unknown }).addedAt === 'number' ? (entry as { addedAt: number }).addedAt : 0,
      nickname: typeof nick === 'string' ? normalizeName(nick) : null,
    });
    if (out.length >= MAX_FRIENDS) break;
  }
  return out;
}

// ---- What Jam sends -----------------------------------------------------------------------------

export interface JamProfileFields {
  /** The name members see, already cut to what the server accepts (80 UTF-8 bytes). */
  name: string;
  profileId: string;
  avatarId: string;
  avatarColor: string;
  /** Hashed into the server-side userId. Derived from the profile id only, never an account. */
  userIdSeed: string;
}

/** JamProfileIdentity.kt: a per-party nickname wins over the profile name. */
export function jamProfileFields(profile: InAppProfile, nickname = ''): JamProfileFields {
  const wanted = nickname.trim() || profile.displayName;
  return {
    name: truncateUtf8(wanted) || profile.displayName.slice(0, 1),
    profileId: profile.id,
    avatarId: profile.avatarId,
    avatarColor: profile.color,
    userIdSeed: `violence-profile:${profile.id}`,
  };
}
