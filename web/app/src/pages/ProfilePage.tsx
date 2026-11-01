import { useState } from 'react';
import { useNavigate } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { AVATARS, COLOR_PRESETS, DEFAULT_AVATAR_ID, DEFAULT_COLOR } from '@/profile/catalog';
import { codePointLength, checkName, NAME_MAX_CODE_POINTS } from '@/profile/rules';
import { ProfileAvatar } from '@/profile/ProfileAvatar';
import { useProfile } from '@/profile/store';
import { settingsUi } from './settingsStyles';

const MAX_TYPED = 60;

function problemText(problem: string | undefined): string {
  switch (problem) {
    case 'too-long':
      return `Names can have up to ${NAME_MAX_CODE_POINTS} characters.`;
    case 'control-character':
    case 'invisible-character':
      return 'That name has characters that cannot be used.';
    default:
      return '';
  }
}

/**
 * The Violence profile (ProfileScreen on the phone): a name, a picture and a colour that other
 * people see in Listen Together. Separate from every sign-in, optional, and kept in this browser.
 */
export default function ProfilePage() {
  const navigate = useNavigate();
  const saved = useProfile((s) => s.profile);
  const save = useProfile((s) => s.save);
  const deleteProfile = useProfile((s) => s.deleteProfile);

  const [name, setName] = useState(saved?.displayName ?? '');
  const [avatarId, setAvatarId] = useState(saved?.avatarId ?? DEFAULT_AVATAR_ID);
  const [color, setColor] = useState(saved?.color ?? DEFAULT_COLOR);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const checked = checkName(name);
  const problem = !checked.ok && name !== '' ? problemText(checked.problem) : '';
  const changed =
    !saved || (checked.ok && checked.name !== saved.displayName) || avatarId !== saved.avatarId || color !== saved.color;

  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Profile" />
      <div className={settingsUi.body}>
        <div className="flex justify-center pb-2">
          <ProfileAvatar avatarId={avatarId} color={color} name={name} size={96} />
        </div>

        <h2 className={settingsUi.sectionHeader}>Name</h2>
        <section className={settingsUi.group}>
          <div className="px-4 py-3">
            <input
              className="w-full rounded-[8px] border-0 bg-quaternary px-2.5 py-[7px] type-body text-primary"
              value={name}
              maxLength={MAX_TYPED}
              placeholder="Your name"
              aria-label="Name"
              onChange={(e) => setName(e.target.value)}
            />
            <div className="mt-1.5 flex justify-between type-callout">
              <span className="text-[#ff453a]" role="alert">
                {problem}
              </span>
              <span className="text-secondary">
                {codePointLength(name)}/{NAME_MAX_CODE_POINTS}
              </span>
            </div>
          </div>
        </section>

        <h2 className={settingsUi.sectionHeader}>Picture</h2>
        <section className={settingsUi.group}>
          <div className="flex flex-wrap gap-2.5 px-4 py-3" role="radiogroup" aria-label="Picture">
            {AVATARS.map((a) => (
              <button
                key={a.id}
                role="radio"
                aria-checked={a.id === avatarId}
                aria-label={a.id}
                className={`rounded-full p-0.5 ${a.id === avatarId ? 'outline-2 outline-key' : ''}`}
                onClick={() => setAvatarId(a.id)}
              >
                <ProfileAvatar avatarId={a.id} color={color} name="" size={38} />
              </button>
            ))}
          </div>
        </section>

        <h2 className={settingsUi.sectionHeader}>Colour</h2>
        <section className={settingsUi.group}>
          <div className="flex flex-wrap gap-3 px-4 py-3.5" role="radiogroup" aria-label="Colour">
            {COLOR_PRESETS.map((hex) => (
              <button
                key={hex}
                role="radio"
                aria-checked={hex === color}
                aria-label={hex}
                className={`size-7 rounded-full ${hex === color ? 'outline-2 outline-offset-2 outline-primary' : ''}`}
                style={{ backgroundColor: `#${hex}` }}
                onClick={() => setColor(hex)}
              />
            ))}
          </div>
        </section>

        <section className={`${settingsUi.group} mt-6`}>
          <button
            className={`${settingsUi.integrationPlain} ${settingsUi.linkRow} justify-center text-key disabled:opacity-40`}
            disabled={!checked.ok || !changed}
            onClick={() => {
              if (save(name, avatarId, color).ok) navigate('/settings');
            }}
          >
            <span className="col-span-2 text-center">Save profile</span>
          </button>
        </section>
        <p className={settingsUi.footer}>
          People in your Listen Together see your name, picture and colour. Your email and Google account are never shared. This profile is
          separate from your YouTube Music and Spotify sign-ins, and it is kept only in this browser.
        </p>

        {saved && (
          <section className={`${settingsUi.group} mt-6`}>
            {confirmDelete ? (
              <div className="px-4 py-3">
                <p className="type-body">Delete your profile? In Listen Together you will appear with your YouTube Music name again. Your friends list stays.</p>
                <div className="mt-3 flex gap-3">
                  <button
                    className={settingsUi.inlineButton}
                    onClick={() => {
                      deleteProfile();
                      navigate('/settings');
                    }}
                  >
                    Delete profile
                  </button>
                  <button className={settingsUi.inlineButton} onClick={() => setConfirmDelete(false)}>
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <button className={`${settingsUi.integrationPlain} ${settingsUi.linkRow} justify-center text-[#ff453a]`} onClick={() => setConfirmDelete(true)}>
                <span className="col-span-2 text-center">Delete profile</span>
              </button>
            )}
          </section>
        )}
      </div>
    </div>
  );
}
