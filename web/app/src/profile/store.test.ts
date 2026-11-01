// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from 'vitest';
import { FRIENDS_KEY, PROFILE_KEY, SUGGESTED_KEY, useProfile } from './store';

const FRIEND = { id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', displayName: 'Ann', avatarId: 'cat', color: 'ff9500' };

beforeEach(() => {
  localStorage.clear();
  useProfile.setState({ profile: null, friends: [] });
});

describe('profile store', () => {
  it('creates the profile once and keeps its id and creation time on edit', () => {
    const created = useProfile.getState().save('  Alex ', 'guitar', '0A84FF');
    expect(created.ok).toBe(true);
    if (!created.ok) return;
    expect(created.profile.displayName).toBe('Alex');
    expect(created.profile.color).toBe('0a84ff');
    expect(created.profile.id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    const edited = useProfile.getState().save('Alexandra', 'fox', '34c759');
    expect(edited.ok && edited.profile.id).toBe(created.profile.id);
    expect(edited.ok && edited.profile.createdAt).toBe(created.profile.createdAt);
    expect(JSON.parse(localStorage.getItem(PROFILE_KEY)!).displayName).toBe('Alexandra');
  });

  it('refuses a bad name and writes nothing', () => {
    expect(useProfile.getState().save('a\nb', 'cat', 'ff9500')).toEqual({ ok: false, problem: 'control-character' });
    expect(useProfile.getState().save('', 'cat', 'ff9500')).toEqual({ ok: false, problem: 'empty' });
    expect(useProfile.getState().profile).toBeNull();
    expect(localStorage.getItem(PROFILE_KEY)).toBeNull();
  });

  it('falls back to defaults for an unknown avatar or colour', () => {
    const result = useProfile.getState().save('Alex', 'unicorn', 'zzz');
    expect(result.ok && result.profile.avatarId).toBe('headphones');
    expect(result.ok && result.profile.color).toBe('fa2d48');
  });

  it('deletes the profile but keeps the friends, and a new profile gets a new id', () => {
    const first = useProfile.getState().save('Alex', 'cat', 'ff9500');
    useProfile.getState().addFriend(FRIEND);
    useProfile.getState().deleteProfile();
    expect(useProfile.getState().profile).toBeNull();
    expect(localStorage.getItem(PROFILE_KEY)).toBeNull();
    expect(useProfile.getState().friends).toHaveLength(1);
    const second = useProfile.getState().save('Alex', 'cat', 'ff9500');
    expect(second.ok && first.ok && second.profile.id !== first.profile.id).toBe(true);
  });

  it('adds, renames, removes and clears friends in one action', () => {
    expect(useProfile.getState().addFriend(FRIEND).kind).toBe('added');
    expect(useProfile.getState().renameFriend(FRIEND.id, 'Annie')).toBe(true);
    expect(useProfile.getState().renameFriend(FRIEND.id, 'bad\u0000')).toBe(false);
    expect(JSON.parse(localStorage.getItem(FRIENDS_KEY)!)[0].nickname).toBe('Annie');
    useProfile.getState().addFriend({ ...FRIEND, id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', displayName: 'Ben' });
    useProfile.getState().removeFriend(FRIEND.id);
    expect(useProfile.getState().friends.map((f) => f.displayName)).toEqual(['Ben']);
    useProfile.getState().clearFriends();
    expect(useProfile.getState().friends).toEqual([]);
    expect(localStorage.getItem(FRIENDS_KEY)).toBeNull();
  });

  it('refuses your own card', () => {
    const me = useProfile.getState().save('Alex', 'cat', 'ff9500');
    if (!me.ok) throw new Error('save failed');
    const result = useProfile.getState().addFriend({ id: me.profile.id, displayName: 'Alex', avatarId: 'cat', color: 'ff9500' });
    expect(result.kind).toBe('is-you');
    expect(useProfile.getState().friends).toEqual([]);
  });

  it('suggests once, never with a profile', () => {
    expect(useProfile.getState().shouldSuggest()).toBe(true);
    useProfile.getState().markSuggestionHandled();
    expect(localStorage.getItem(SUGGESTED_KEY)).toBe('1');
    expect(useProfile.getState().shouldSuggest()).toBe(false);
    localStorage.clear();
    useProfile.getState().save('Alex', 'cat', 'ff9500');
    expect(useProfile.getState().shouldSuggest()).toBe(false);
  });

  it('stores no account, photo or token data', () => {
    useProfile.getState().save('Alex', 'cat', 'ff9500');
    useProfile.getState().addFriend(FRIEND);
    const all = [PROFILE_KEY, FRIENDS_KEY].map((k) => localStorage.getItem(k) ?? '').join(' ').toLowerCase();
    for (const word of ['photo', 'email', 'cookie', 'token', 'google']) expect(all).not.toContain(word);
  });
});
