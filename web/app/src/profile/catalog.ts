/**
 * The bundled avatars, the same ids and glyphs as AvatarCatalog.kt (shared/.../data/profile).
 * Append only: an id is part of the profile-card format and of the Jam wire format.
 */

export interface Avatar {
  id: string;
  glyph: string;
}

const g = (...codePoints: number[]) => String.fromCodePoint(...codePoints);

export const AVATARS: readonly Avatar[] = [
  { id: 'headphones', glyph: g(0x1f3a7) },
  { id: 'note', glyph: g(0x1f3b5) },
  { id: 'vinyl', glyph: g(0x1f4bf) },
  { id: 'guitar', glyph: g(0x1f3b8) },
  { id: 'mic', glyph: g(0x1f3a4) },
  { id: 'piano', glyph: g(0x1f3b9) },
  { id: 'drum', glyph: g(0x1f941) },
  { id: 'trumpet', glyph: g(0x1f3ba) },
  { id: 'violin', glyph: g(0x1f3bb) },
  { id: 'radio', glyph: g(0x1f4fb) },
  { id: 'bolt', glyph: g(0x26a1) },
  { id: 'flame', glyph: g(0x1f525) },
  { id: 'star', glyph: g(0x2b50) },
  { id: 'moon', glyph: g(0x1f319) },
  { id: 'heart', glyph: g(0x1f49c) },
  { id: 'rocket', glyph: g(0x1f680) },
  { id: 'ghost', glyph: g(0x1f47b) },
  { id: 'alien', glyph: g(0x1f47d) },
  { id: 'robot', glyph: g(0x1f916) },
  { id: 'cat', glyph: g(0x1f431) },
  { id: 'fox', glyph: g(0x1f98a) },
  { id: 'panda', glyph: g(0x1f43c) },
  { id: 'wolf', glyph: g(0x1f43a) },
  { id: 'skull', glyph: g(0x1f480) },
];

export const DEFAULT_AVATAR_ID = 'headphones';
export const DEFAULT_COLOR = 'fa2d48';

/** Colour swatches offered by the editor. Any six hex digits is also accepted. */
export const COLOR_PRESETS: readonly string[] = [
  'fa2d48', 'ff9500', 'ffcc00', '34c759', '30b0c7',
  '0a84ff', '5e5ce6', 'bf5af2', 'ff375f', '8e8e93',
];

export function isKnownAvatar(id: string | null | undefined): boolean {
  return id != null && AVATARS.some((a) => a.id === id);
}

export function glyphOf(id: string | null | undefined): string | null {
  return AVATARS.find((a) => a.id === id)?.glyph ?? null;
}
