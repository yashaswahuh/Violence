import { Check, Copy, ExternalLink, UserPlus } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { cardToUri, cardToWebUrl, parseCard, WEB_BASE } from '@/profile/card';
import { ProfileAvatar } from '@/profile/ProfileAvatar';
import { useProfile } from '@/profile/store';
import { settingsUi } from './settingsStyles';

const card = 'flex flex-col items-center gap-2.5 rounded-[10px] bg-quinary px-4 py-7 text-center';
const primary = 'inline-flex items-center gap-2 rounded-pill bg-key-bg px-6 py-2.5 type-title-3-emphasized text-white';
const secondary = 'inline-flex items-center gap-2 rounded-pill bg-quaternary px-5 py-2 type-body-emphasized';

/**
 * The page a shared profile link lands on: `/profile?v=1&id=...&n=...&a=...&c=...`.
 *
 * Shows the name and picture (plain text, from a strictly validated card), a button that opens the
 * Android app, a copy button, and "Add as a friend" which asks first and then saves the friend in
 * this browser only. No redirect, no server, no photo. The site is `noindex` (index.html).
 */
export default function ProfileLandingPage() {
  const location = useLocation();
  const addFriend = useProfile((s) => s.addFriend);
  const [copied, setCopied] = useState(false);
  const [asking, setAsking] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const parsed = parseCard(`${WEB_BASE}${location.search}`);

  useEffect(() => {
    if (!copied) return;
    const t = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(t);
  }, [copied]);

  if (!parsed.ok) {
    return (
      <div className={settingsUi.centered}>
        <PageHeader title="Shared profile" />
        <div className={settingsUi.body}>
          <p className={settingsUi.footer}>That doesn’t look like a Violence profile link.</p>
        </div>
      </div>
    );
  }

  const person = parsed.card;
  const confirmAdd = () => {
    const result = addFriend(person);
    setNotice(
      result.kind === 'added'
        ? `${person.displayName} added.`
        : result.kind === 'updated'
          ? `${person.displayName} is already a friend. Details updated.`
          : result.kind === 'is-you'
            ? 'That is your own profile.'
            : 'Your friends list is full.',
    );
    setAsking(false);
  };

  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Shared profile" />
      <div className={settingsUi.body}>
        <div className={card}>
          <ProfileAvatar avatarId={person.avatarId} color={person.color} name={person.displayName} size={72} />
          <p className="type-title-1-emphasized">{person.displayName}</p>
          <p className={settingsUi.footer}>shared their Violence profile</p>
          <a className={primary} href={cardToUri(person)}>
            <ExternalLink size={18} />
            Open in Violence app
          </a>
          {asking ? (
            <div className="flex flex-col items-center gap-2" role="dialog" aria-label={`Add ${person.displayName} as a friend?`}>
              <p className="type-body-emphasized">Add {person.displayName} as a friend?</p>
              <div className="flex gap-2">
                <button className={secondary} onClick={confirmAdd}>
                  Add friend
                </button>
                <button className={secondary} onClick={() => setAsking(false)}>
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <button className={secondary} onClick={() => setAsking(true)}>
              <UserPlus size={18} />
              Add as a friend
            </button>
          )}
          {notice && (
            <p className="type-callout text-secondary" role="status">
              {notice}
            </p>
          )}
          <button className={secondary} onClick={() => void navigator.clipboard.writeText(cardToWebUrl(person)).then(() => setCopied(true))}>
            {copied ? <Check size={18} /> : <Copy size={18} />}
            {copied ? 'Copied' : 'Copy link'}
          </button>
        </div>
        <p className={settingsUi.footer}>Friends are saved only in this browser.</p>
      </div>
    </div>
  );
}
