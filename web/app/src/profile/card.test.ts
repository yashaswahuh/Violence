import { describe, expect, it } from 'vitest';
import { AVATARS, COLOR_PRESETS } from './catalog';
import { cardToUri, cardToWebUrl, CUSTOM_PREFIX, fitsNfcTarget, parseCard, percentDecode, percentEncode, WEB_BASE, type CardError, type ProfileCard } from './card';
import { addFriend, jamProfileFields, MAX_FRIENDS, renameFriend, removeFriend, sanitizeFriends, sanitizeProfile, shownName, sortedFriends, type FriendCard, type InAppProfile } from './friends';
import { checkName, normalizeColor, normalizeId, normalizeName, truncateUtf8 } from './rules';

// ProfileCardCodecTest.kt, ProfileRulesAndFriendsTest.kt and JamProfileIdentityTest.kt, case for case.

const ID = '123e4567-e89b-42d3-a456-426614174000';
const card = (over: Partial<ProfileCard> = {}): ProfileCard => ({ id: ID, displayName: 'Alex', avatarId: 'guitar', color: '0a84ff', ...over });
const FACE = String.fromCodePoint(0x1f600);
const CJK = String.fromCodePoint(0x5c71);
const FAMILY = String.fromCodePoint(0x1f469, 0x200d, 0x1f469, 0x200d, 0x1f467);

function uri(parts: Partial<Record<'v' | 'id' | 'n' | 'a' | 'c', string | null>> = {}, base = CUSTOM_PREFIX, extra = ''): string {
  const all = { v: '1', id: ID, n: 'Alex', a: 'guitar', c: '0a84ff', ...parts };
  const query = (Object.entries(all) as [string, string | null][])
    .filter(([, value]) => value !== null)
    .map(([key, value]) => `${key}=${value}`)
    .join('&');
  return `${base}?${query}${extra}`;
}

const error = (text: string | null | undefined): CardError | 'ok' => {
  const result = parseCard(text);
  return result.ok ? 'ok' : result.error;
};
const ok = (text: string): ProfileCard => {
  const result = parseCard(text);
  if (!result.ok) throw new Error(`expected a card, got ${result.error} for ${text}`);
  return result.card;
};
const nameProblem = (text: string) => {
  const result = parseCard(text);
  return result.ok ? null : result.nameProblem;
};

describe('profile card: valid', () => {
  it('parses the custom scheme and the web form', () => {
    expect(ok(uri())).toEqual(card());
    expect(ok(uri({}, WEB_BASE))).toEqual(card());
    expect(ok(uri({}, `${WEB_BASE}/`))).toEqual(card());
    expect(ok(uri({}, WEB_BASE.toUpperCase()))).toEqual(card());
  });

  it('ignores field order, surrounding whitespace and a fragment', () => {
    expect(ok(`  \n${CUSTOM_PREFIX}?c=0a84ff&a=guitar&n=Alex&id=${ID}&v=1 \t`)).toEqual(card());
    expect(ok(uri({}, CUSTOM_PREFIX, '#frag'))).toEqual(card());
  });

  it('normalises the uuid and the colour', () => {
    const parsed = ok(uri({ id: '123E4567E89B42D3A456426614174000', c: '0A84FF' }));
    expect(parsed.id).toBe(ID);
    expect(parsed.color).toBe('0a84ff');
  });

  it('reads plus and %20 as a space', () => {
    expect(ok(uri({ n: 'Mary+Ann' })).displayName).toBe('Mary Ann');
    expect(ok(uri({ n: 'Mary%20Ann' })).displayName).toBe('Mary Ann');
  });

  it('round-trips emoji and non-Latin names', () => {
    for (const name of [`DJ ${FACE}`, CJK.repeat(4), 'محمد', 'Zoë', 'Σόφια', FAMILY]) {
      expect(ok(cardToUri(card({ displayName: name }))).displayName).toBe(name);
      expect(ok(cardToWebUrl(card({ displayName: name }))).displayName).toBe(name);
    }
  });

  it('tolerates raw non-ASCII in the link', () => {
    expect(ok(uri({ n: 'Zoë' })).displayName).toBe('Zoë');
  });

  it('accepts every preset avatar', () => {
    for (const avatar of AVATARS) expect(ok(uri({ a: avatar.id })).avatarId).toBe(avatar.id);
  });

  it('emits a fixed shape', () => {
    expect(cardToUri(card())).toBe(`violence://profile?v=1&id=${ID}&n=Alex&a=guitar&c=0a84ff`);
    expect(cardToWebUrl(card())).toBe(`${WEB_BASE}?v=1&id=${ID}&n=Alex&a=guitar&c=0a84ff`);
  });

  it('escapes the characters that need it', () => {
    const link = cardToUri(card({ displayName: 'A&B=C #1+2' }));
    expect(link).toContain('n=A%26B%3DC%20%231%2B2');
    expect(ok(link).displayName).toBe('A&B=C #1+2');
  });
});

describe('profile card: size', () => {
  it('fits the NFC target for Latin names but not for 24 CJK characters', () => {
    expect(fitsNfcTarget(cardToUri(card({ displayName: 'A'.repeat(24) })))).toBe(true);
    const cjk = CJK.repeat(24);
    expect(fitsNfcTarget(cardToUri(card({ displayName: cjk })))).toBe(false);
    expect(ok(cardToUri(card({ displayName: cjk }))).displayName).toBe(cjk);
  });

  it('refuses oversized input before parsing', () => {
    expect(error(uri({ n: 'A'.repeat(600) }))).toBe('too-long');
    expect(error('x'.repeat(513))).toBe('too-long');
  });
});

describe('profile card: rejects', () => {
  it('anything that is not a card', () => {
    for (const text of [null, '', '   ', 'hello', 'violence://party/ABC123', 'https://example.org/Violence/profile?v=1', 'violence://profile', 'violence://profilex?v=1', 'https://yashaswahuh.is-a.dev/Violence/party/ABC123']) {
      expect(error(text)).toBe('not-a-card');
    }
  });

  it('wrong or missing version', () => {
    expect(error(uri({ v: '2' }))).toBe('wrong-version');
    expect(error(uri({ v: '' }))).toBe('wrong-version');
    expect(error(uri({ v: '1.0' }))).toBe('wrong-version');
    expect(error(uri({ v: null }))).toBe('missing-field');
  });

  it('missing, duplicate and unknown fields', () => {
    for (const key of ['id', 'n', 'a', 'c'] as const) expect(error(uri({ [key]: null }))).toBe('missing-field');
    expect(error(uri({}, CUSTOM_PREFIX, '&n=Other'))).toBe('duplicate-field');
    expect(error(uri({}, CUSTOM_PREFIX, '&v=1'))).toBe('duplicate-field');
    expect(error(uri({}, CUSTOM_PREFIX, '&email=a%40b.c'))).toBe('unknown-field');
    expect(error(uri({}, CUSTOM_PREFIX, '&photo=x'))).toBe('unknown-field');
  });

  it('a bad uuid', () => {
    for (const bad of ['', '123', 'not-a-uuid-at-all-not-a-uuid-at-all!', '123e4567-e89b-42d3-a456-42661417400', '123e4567-e89b-42d3-a456-4266141740000', '123e4567e89b-42d3-a456-426614174000', '123e4567-e89b-42d3-a456-42661417400g', '00000000-0000-0000-0000-000000000000', '00000000000000000000000000000000']) {
      expect(error(uri({ id: bad }))).toBe('bad-id');
    }
  });

  it('a name that is too long is rejected, not truncated', () => {
    expect(nameProblem(uri({ n: 'A'.repeat(25) }))).toBe('too-long');
    expect(ok(cardToUri(card({ displayName: FACE.repeat(24) }))).displayName).toBe(FACE.repeat(24));
    expect(nameProblem(uri({ n: percentEncode(FACE.repeat(25)) }))).toBe('too-long');
  });

  it('an empty or blank name', () => {
    for (const n of ['', '%20%20', '+++']) expect(nameProblem(uri({ n }))).toBe('empty');
  });

  it('control characters in the name', () => {
    for (const escaped of ['%00', '%01', '%07', '%08', '%09', '%0A', '%0D', '%1B', '%7F', '%C2%85', '%C2%9F']) {
      expect(error(uri({ n: `Al${escaped}ex` }))).toBe('bad-name');
      expect(nameProblem(uri({ n: `Al${escaped}ex` }))).toBe('control-character');
    }
  });

  it('invisible and directional tricks in the name', () => {
    for (const escaped of ['%E2%80%8B', '%E2%80%AE', '%E2%80%8F', '%E2%81%A6', '%EF%BB%BF', '%C2%AD', '%E2%80%A8', '%E2%80%A9', '%E3%85%A4', '%E2%A0%80', '%EE%80%80', '%F3%A0%80%81']) {
      expect(nameProblem(uri({ n: `Al${escaped}ex` }))).toBe('invisible-character');
    }
  });

  it('stacked combining marks', () => {
    const zalgo = 'a' + String.fromCharCode(0x300, 0x301, 0x302, 0x303, 0x304, 0x305);
    expect(nameProblem(uri({ n: percentEncode(zalgo) }))).toBe('invisible-character');
    expect(ok(uri({ n: percentEncode('e' + String.fromCharCode(0x301)) })).displayName).toBe('e' + String.fromCharCode(0x301));
  });

  it('bad percent-encoding', () => {
    for (const bad of ['%', 'Al%', 'Al%4', 'Al%GG', 'Al%zzex', '%C3', '%FF%FE', '%E2%80', '%ED%A0%80']) {
      expect(error(uri({ n: bad }))).toBe('bad-encoding');
    }
  });

  it('an unknown avatar', () => {
    for (const a of ['unicorn', '', 'GUITAR']) expect(error(uri({ a }))).toBe('bad-avatar');
  });

  it('a bad colour', () => {
    for (const c of ['', 'fff', '#0a84ff', '0a84f', '0a84ff0', 'gggggg', '0a84fg', 'red']) expect(error(uri({ c }))).toBe('bad-color');
  });

  it('never echoes the input', () => {
    const secret = 'hunter2-should-not-appear';
    expect(JSON.stringify(parseCard(uri({ n: `${secret}%00` })))).not.toContain(secret);
  });

  it('percent helpers agree with each other', () => {
    expect(percentDecode(percentEncode('Zoë 山 😀'))).toBe('Zoë 山 😀');
    expect(percentDecode('%')).toBeNull();
  });
});

describe('profile rules', () => {
  it('trims and collapses spaces', () => {
    expect(normalizeName('  Mary ' + String.fromCharCode(0xa0) + '  Ann  ')).toBe('Mary Ann');
  });

  it('counts code points', () => {
    expect(normalizeName(FACE.repeat(24))).toBe(FACE.repeat(24));
    expect(normalizeName(FACE.repeat(25))).toBeNull();
    expect(normalizeName('a'.repeat(24))).toBe('a'.repeat(24));
    expect(normalizeName('a'.repeat(25))).toBeNull();
  });

  it('rejects lone surrogates and control or format characters', () => {
    expect(normalizeName(`a${String.fromCharCode(0xd83d)}b`)).toBeNull();
    expect(normalizeName(`a${String.fromCharCode(0xde00)}b`)).toBeNull();
    for (const code of [0x0a, 0x09, 0x00, 0x200b, 0x202e]) expect(normalizeName(`a${String.fromCharCode(code)}b`)).toBeNull();
  });

  it('keeps zero-width-joiner sequences and variation selectors', () => {
    expect(normalizeName(FAMILY)).toBe(FAMILY);
    const heart = String.fromCharCode(0x2764, 0xfe0f);
    expect(normalizeName(heart)).toBe(heart);
  });

  it('cuts the Jam name on a character boundary', () => {
    expect(truncateUtf8('Alex')).toBe('Alex');
    const cut = truncateUtf8(FACE.repeat(24));
    expect([...cut].length).toBe(20);
    expect(new TextEncoder().encode(cut).length).toBeLessThanOrEqual(80);
    expect([...truncateUtf8(CJK.repeat(24))].length).toBe(24);
    const mixed = truncateUtf8((CJK + FACE).repeat(12));
    expect(new TextEncoder().encode(mixed).length).toBeLessThanOrEqual(80);
    expect(normalizeName(mixed)).toBe(mixed);
  });

  it('normalises ids and colours', () => {
    expect(normalizeId(' 123E4567E89B42D3A456426614174000 ')).toBe(ID);
    expect(normalizeId('nope')).toBeNull();
    expect(normalizeColor('0A84FF')).toBe('0a84ff');
    expect(normalizeColor('#0a84ff')).toBeNull();
  });

  it('has valid unique presets', () => {
    const ids = AVATARS.map((a) => a.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(ids.every((id) => /^[a-z0-9]{1,16}$/.test(id))).toBe(true);
    expect(COLOR_PRESETS.every((c) => normalizeColor(c) === c)).toBe(true);
    expect(AVATARS.every((a) => a.glyph.length > 0)).toBe(true);
  });

  it('reports why a name is bad', () => {
    expect(checkName('')).toEqual({ ok: false, problem: 'empty' });
    expect(checkName('a\nb')).toEqual({ ok: false, problem: 'control-character' });
  });
});

describe('friends', () => {
  const a: ProfileCard = { id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', displayName: 'Ann', avatarId: 'cat', color: 'ff9500' };
  const b: ProfileCard = { id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', displayName: 'Ben', avatarId: 'fox', color: '34c759' };
  const added = (list: FriendCard[], c: ProfileCard, now = 1) => {
    const r = addFriend(list, c, null, now);
    if (r.kind !== 'added') throw new Error(r.kind);
    return r.list;
  };

  it('adds a friend and refuses your own card', () => {
    expect(added([], a, 5)).toEqual([{ ...a, addedAt: 5, nickname: null }]);
    expect(addFriend([], a, a.id, 1)).toEqual({ kind: 'is-you' });
  });

  it('adding again updates the card but keeps the nickname and the date', () => {
    const renamed = renameFriend(added([], a, 5), a.id, 'Annie')!;
    const result = addFriend(renamed, { ...a, displayName: 'Ann B', avatarId: 'fox' }, null, 99);
    expect(result.kind).toBe('updated');
    const friend = (result as { list: FriendCard[] }).list[0]!;
    expect(friend).toMatchObject({ displayName: 'Ann B', avatarId: 'fox', nickname: 'Annie', addedAt: 5 });
    expect(shownName(friend)).toBe('Annie');
  });

  it('sets and clears a nickname, and refuses a bad one', () => {
    const list = added([], a);
    expect(renameFriend(list, a.id, '  Annie  ')![0]!.nickname).toBe('Annie');
    expect(renameFriend(renameFriend(list, a.id, 'Annie')!, a.id, '   ')![0]!.nickname).toBeNull();
    expect(renameFriend(list, a.id, 'a'.repeat(25))).toBeNull();
    expect(renameFriend(list, a.id, 'bad\nname')).toBeNull();
    expect(renameFriend(list, 'unknown', 'x')).toEqual(list);
  });

  it('removes and sorts', () => {
    const list = added(added([], b), a);
    expect(sortedFriends(list).map(shownName)).toEqual(['Ann', 'Ben']);
    expect(removeFriend(list, a.id).map((f) => f.id)).toEqual([b.id]);
  });

  it('has a generous cap that still lets an existing friend update', () => {
    let list: FriendCard[] = [];
    for (let i = 0; i < MAX_FRIENDS; i++) {
      list = added(list, { id: `${i.toString(16).padStart(8, '0')}-aaaa-4aaa-8aaa-aaaaaaaaaaaa`, displayName: `F${i}`, avatarId: 'cat', color: 'ff9500' });
    }
    expect(addFriend(list, a, null, 1)).toEqual({ kind: 'full' });
    expect(addFriend(list, { ...a, id: list[0]!.id }, null, 1).kind).toBe('updated');
  });
});

describe('stored data is untrusted', () => {
  it('cleans an edited profile', () => {
    const p = sanitizeProfile({ id: '123E4567E89B42D3A456426614174000', displayName: '  Al  ex ', avatarId: 'nope', color: '#zzz', createdAt: 5 })!;
    expect(p).toEqual({ id: ID, displayName: 'Al ex', avatarId: 'headphones', color: 'fa2d48', createdAt: 5 });
    expect(sanitizeProfile({ id: 'bad', displayName: 'x' })).toBeNull();
    expect(sanitizeProfile('nope')).toBeNull();
    expect(sanitizeProfile(null)).toBeNull();
  });

  it('drops bad, duplicate and unreadable friends', () => {
    const list = sanitizeFriends([
      { id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', displayName: 'Ann', avatarId: 'cat', color: 'ff9500', nickname: 'bad\u0000' },
      { id: 'not-an-id', displayName: 'Evil', avatarId: 'cat', color: 'ff9500' },
      { id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', displayName: 'Duplicate', avatarId: 'cat', color: 'ff9500' },
      { id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', displayName: 'line\nbreak', avatarId: 'cat', color: 'ff9500' },
      42,
    ]);
    expect(list.map((f) => f.displayName)).toEqual(['Ann']);
    expect(list[0]!.nickname).toBeNull();
    expect(sanitizeFriends('nope')).toEqual([]);
  });
});

describe('Jam identity from a profile', () => {
  const profile: InAppProfile = { id: ID, displayName: 'Alex', avatarId: 'guitar', color: '0a84ff', createdAt: 1 };

  it('uses the profile name, avatar and colour; a party nickname wins; blank does not', () => {
    expect(jamProfileFields(profile)).toMatchObject({ name: 'Alex', avatarId: 'guitar', avatarColor: '0a84ff', profileId: ID });
    expect(jamProfileFields(profile, '  DJ A ').name).toBe('DJ A');
    expect(jamProfileFields(profile, '   ').name).toBe('Alex');
  });

  it('derives the user id seed from the profile id only', () => {
    const seed = jamProfileFields(profile).userIdSeed;
    expect(seed).toBe(`violence-profile:${ID}`);
    expect(seed.toLowerCase()).not.toContain('google');
    expect(seed).not.toContain('@');
  });

  it('fits the server name limit even for 24 emoji, and cuts a long nickname', () => {
    const long = jamProfileFields({ ...profile, displayName: FACE.repeat(24) });
    expect(new TextEncoder().encode(long.name).length).toBeLessThanOrEqual(80);
    expect(long.name.length).toBeGreaterThan(0);
    expect(new TextEncoder().encode(jamProfileFields(profile, 'x'.repeat(200)).name).length).toBe(80);
  });
});
