import { glyphOf } from './catalog';

/** `rrggbb` as a CSS colour; anything unreadable is the app's red. */
function css(hex: string | null | undefined): string {
  return hex && /^[0-9a-f]{6}$/i.test(hex) ? `#${hex}` : '#fa2d48';
}

interface ProfileAvatarProps {
  avatarId?: string | null;
  color?: string | null;
  name: string;
  /** The picture that came with a Google profile or an older Jam client. */
  url?: string | null;
  size?: number;
}

/**
 * A person's picture (ProfileAvatar.kt): a preset on a circle in the profile colour, otherwise
 * the picture that came with their account, otherwise the first letter of their name. An avatar id
 * this build does not know falls through, so a member list never breaks on a newer app's preset.
 */
export function ProfileAvatar({ avatarId, color, name, url, size = 24 }: ProfileAvatarProps) {
  const glyph = glyphOf(avatarId);
  const box = { width: size, height: size };
  if (glyph) {
    const base = css(color);
    return (
      <span
        className="inline-grid flex-none place-items-center rounded-full leading-none select-none"
        style={{ ...box, fontSize: size * 0.52, background: `linear-gradient(135deg, color-mix(in srgb, ${base} 82%, white), color-mix(in srgb, ${base} 78%, black))` }}
        aria-hidden="true"
      >
        {glyph}
      </span>
    );
  }
  if (url) {
    return <img className="flex-none rounded-full object-cover" style={box} src={url} alt="" referrerPolicy="no-referrer" />;
  }
  return (
    <span
      className="inline-grid flex-none place-items-center rounded-full bg-quaternary type-callout-emphasized"
      style={box}
      aria-hidden="true"
    >
      {name.slice(0, 1).toUpperCase() || '?'}
    </span>
  );
}
