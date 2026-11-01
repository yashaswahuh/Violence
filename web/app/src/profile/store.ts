import { create } from 'zustand';
import { DEFAULT_AVATAR_ID, DEFAULT_COLOR, isKnownAvatar } from './catalog';
import type { ProfileCard } from './card';
import {
  addFriend as addFriendTo,
  removeFriend as removeFriendFrom,
  renameFriend as renameFriendIn,
  sanitizeFriends,
  sanitizeProfile,
  type AddResult,
  type FriendCard,
  type InAppProfile,
} from './friends';
import { checkName, normalizeColor, type NameProblem } from './rules';

/*
 * The local Violence profile and friends list, in the browser's own storage, under the same
 * `bitchord:` key prefix as the web app's other keys. Nothing here is sent anywhere, nothing is
 * logged, and a damaged value reads as "no profile" rather than an error.
 */

export const PROFILE_KEY = 'bitchord:profile';
export const FRIENDS_KEY = 'bitchord:friends';
export const SUGGESTED_KEY = 'bitchord:profileSuggested';

function read(key: string): unknown {
  try {
    const raw = localStorage.getItem(key);
    return raw === null ? null : JSON.parse(raw);
  } catch {
    return null;
  }
}

function write(key: string, value: unknown | null) {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage full or blocked: the in-memory copy still works for this tab.
  }
}

export type SaveResult = { ok: true; profile: InAppProfile } | { ok: false; problem: NameProblem };

interface ProfileState {
  profile: InAppProfile | null;
  friends: FriendCard[];
  save: (name: string, avatarId: string, color: string) => SaveResult;
  deleteProfile: () => void;
  addFriend: (card: ProfileCard) => AddResult;
  renameFriend: (id: string, nickname: string) => boolean;
  removeFriend: (id: string) => void;
  clearFriends: () => void;
  shouldSuggest: () => boolean;
  markSuggestionHandled: () => void;
}

export const useProfile = create<ProfileState>()((set, get) => ({
  profile: sanitizeProfile(read(PROFILE_KEY)),
  friends: sanitizeFriends(read(FRIENDS_KEY)),

  save: (name, avatarId, color) => {
    const checked = checkName(name);
    if (!checked.ok) return { ok: false, problem: checked.problem };
    const current = get().profile;
    const next: InAppProfile = {
      id: current?.id ?? crypto.randomUUID(),
      displayName: checked.name,
      avatarId: isKnownAvatar(avatarId) ? avatarId : DEFAULT_AVATAR_ID,
      color: normalizeColor(color) ?? DEFAULT_COLOR,
      createdAt: current?.createdAt ?? Date.now(),
    };
    write(PROFILE_KEY, next);
    set({ profile: next });
    return { ok: true, profile: next };
  },

  deleteProfile: () => {
    write(PROFILE_KEY, null);
    set({ profile: null });
  },

  addFriend: (card) => {
    const result = addFriendTo(get().friends, card, get().profile?.id ?? null, Date.now());
    if (result.kind === 'added' || result.kind === 'updated') {
      write(FRIENDS_KEY, result.list.length ? result.list : null);
      set({ friends: result.list });
    }
    return result;
  },

  renameFriend: (id, nickname) => {
    const next = renameFriendIn(get().friends, id, nickname);
    if (next === null) return false;
    write(FRIENDS_KEY, next.length ? next : null);
    set({ friends: next });
    return true;
  },

  removeFriend: (id) => {
    const next = removeFriendFrom(get().friends, id);
    write(FRIENDS_KEY, next.length ? next : null);
    set({ friends: next });
  },

  clearFriends: () => {
    write(FRIENDS_KEY, null);
    set({ friends: [] });
  },

  shouldSuggest: () => get().profile === null && localStorage.getItem(SUGGESTED_KEY) !== '1',
  markSuggestionHandled: () => {
    try {
      localStorage.setItem(SUGGESTED_KEY, '1');
    } catch {
      // Not stored: the suggestion may show again next visit, which is harmless.
    }
  },
}));

/** The profile right now, for code outside React (the Jam identity). */
export const currentProfile = (): InAppProfile | null => useProfile.getState().profile;
