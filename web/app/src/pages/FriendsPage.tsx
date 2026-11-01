import { useState } from 'react';
import { useNavigate } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { MAX_INPUT_CHARS, type ProfileCard } from '@/profile/card';
import { extractIncoming, INVALID_TEXT, type InvalidReason } from '@/share/incoming';
import { songToWebUrl } from '@/share/songLinks';
import { shownName, sortedFriends, type FriendCard } from '@/profile/friends';
import { ProfileAvatar } from '@/profile/ProfileAvatar';
import { normalizeName } from '@/profile/rules';
import { useProfile } from '@/profile/store';
import { settingsUi } from './settingsStyles';

const errorText = (reason: InvalidReason): string => INVALID_TEXT[reason];

/** One friend, with an inline nickname editor. The name is somebody else's text: shown as plain text only. */
function FriendRow({ friend }: { friend: FriendCard }) {
  const rename = useProfile((s) => s.renameFriend);
  const remove = useProfile((s) => s.removeFriend);
  const [editing, setEditing] = useState(false);
  const [nickname, setNickname] = useState(friend.nickname ?? '');
  const valid = nickname.trim() === '' || normalizeName(nickname) !== null;

  return (
    <div className="px-4 py-2.5 type-title-3">
      <div className="flex items-center gap-3">
        <ProfileAvatar avatarId={friend.avatarId} color={friend.color} name={friend.displayName} size={36} />
        <div className="min-w-0 flex-1">
          <div className="truncate">{shownName(friend)}</div>
          {friend.nickname && <div className="truncate type-callout text-secondary">Profile name: {friend.displayName}</div>}
        </div>
        <button className={settingsUi.inlineButton} onClick={() => setEditing((e) => !e)}>
          {editing ? 'Close' : 'Edit'}
        </button>
      </div>
      {editing && (
        <div className="mt-2.5 flex flex-wrap items-center gap-2">
          <input
            className={settingsUi.input}
            value={nickname}
            maxLength={60}
            placeholder="Nickname (only you see it)"
            aria-label="Nickname"
            onChange={(e) => setNickname(e.target.value)}
          />
          <button
            className={settingsUi.inlineButton}
            disabled={!valid}
            onClick={() => {
              if (rename(friend.id, nickname)) setEditing(false);
            }}
          >
            Save
          </button>
          <button className={`${settingsUi.inlineButton} text-[#ff453a]`} onClick={() => remove(friend.id)}>
            Remove friend
          </button>
        </div>
      )}
    </div>
  );
}

/**
 * The friends list (FriendsScreen on the phone): add one by pasting a profile link after a
 * confirmation, give one a nickname, remove one, or clear the list. Saved only in this browser.
 */
export default function FriendsPage() {
  const friends = useProfile((s) => s.friends);
  const addFriend = useProfile((s) => s.addFriend);
  const clearFriends = useProfile((s) => s.clearFriends);

  const [text, setText] = useState('');
  const navigate = useNavigate();
  const [error, setError] = useState<InvalidReason | null>(null);
  const [candidate, setCandidate] = useState<ProfileCard | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [confirmClear, setConfirmClear] = useState(false);

  const check = () => {
    setNotice(null);
    // Any Violence link: a profile asks to add a friend here; a song or a Jam invite opens its page.
    const result = extractIncoming(text);
    if (result.kind === 'profile') {
      setCandidate(result.card);
      setError(null);
    } else if (result.kind === 'song') {
      navigate(`/song/${result.ref.source}/${result.ref.id}?${songToWebUrl(result.ref).split('?')[1] ?? ''}`);
    } else if (result.kind === 'party') {
      navigate(`/party/${result.invite.code}${result.invite.serverUrl ? `?server=${encodeURIComponent(result.invite.serverUrl)}` : ''}`);
    } else {
      setError(result.reason);
    }
  };

  const paste = async () => {
    try {
      setText((await navigator.clipboard.readText()).slice(0, MAX_INPUT_CHARS + 1));
      setError(null);
    } catch {
      // Clipboard access refused: typing or pasting into the box still works.
    }
  };

  const confirmAdd = (card: ProfileCard) => {
    const result = addFriend(card);
    setNotice(
      result.kind === 'added'
        ? `${card.displayName} added.`
        : result.kind === 'updated'
          ? `${card.displayName} is already a friend. Details updated.`
          : result.kind === 'is-you'
            ? 'That is your own profile.'
            : 'Your friends list is full.',
    );
    setCandidate(null);
    setText('');
  };

  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Friends" />
      <div className={settingsUi.body}>
        <h2 className={settingsUi.sectionHeader}>Add from a link</h2>
        <section className={settingsUi.group}>
          <div className="px-4 py-3">
            <div className="flex gap-2">
              <input
                className="min-w-0 flex-1 rounded-[8px] border-0 bg-quaternary px-2.5 py-[7px] type-body text-primary"
                value={text}
                placeholder="violence://profile?…"
                aria-label="Profile link"
                onChange={(e) => {
                  setText(e.target.value.slice(0, MAX_INPUT_CHARS + 1));
                  setError(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && text.trim()) check();
                }}
              />
              <button className={settingsUi.inlineButton} onClick={paste}>
                Paste
              </button>
              <button className={settingsUi.inlineButton} disabled={!text.trim()} onClick={check}>
                Check link
              </button>
            </div>
            {error && (
              <p className="mt-2 type-callout text-[#ff453a]" role="alert">
                {errorText(error)}
              </p>
            )}
            {notice && (
              <p className="mt-2 type-callout text-secondary" role="status">
                {notice}
              </p>
            )}
          </div>
        </section>

        {candidate && (
          <section className={`${settingsUi.group} mt-4`} role="dialog" aria-label={`Add ${candidate.displayName} as a friend?`}>
            <div className="flex items-center gap-3 px-4 py-3">
              <ProfileAvatar avatarId={candidate.avatarId} color={candidate.color} name={candidate.displayName} size={48} />
              <div className="min-w-0 flex-1 type-title-3-emphasized">
                <div className="truncate">Add {candidate.displayName} as a friend?</div>
              </div>
              <button className={settingsUi.inlineButton} onClick={() => confirmAdd(candidate)}>
                Add friend
              </button>
              <button className={settingsUi.inlineButton} onClick={() => setCandidate(null)}>
                Cancel
              </button>
            </div>
          </section>
        )}

        <h2 className={settingsUi.sectionHeader}>Friends</h2>
        {friends.length === 0 ? (
          <p className={settingsUi.footer}>No friends yet. Paste a profile link to add one.</p>
        ) : (
          <>
            <section className={settingsUi.group}>
              {sortedFriends(friends).map((friend) => (
                <FriendRow key={friend.id} friend={friend} />
              ))}
            </section>
            <section className={`${settingsUi.group} mt-6`}>
              {confirmClear ? (
                <div className="px-4 py-3">
                  <p className="type-body">Clear your friends list? Their own profiles are not affected.</p>
                  <div className="mt-3 flex gap-3">
                    <button
                      className={settingsUi.inlineButton}
                      onClick={() => {
                        clearFriends();
                        setConfirmClear(false);
                        setNotice('Friends list cleared.');
                      }}
                    >
                      Clear friends list
                    </button>
                    <button className={settingsUi.inlineButton} onClick={() => setConfirmClear(false)}>
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <button className={`${settingsUi.integrationPlain} ${settingsUi.linkRow} justify-center text-[#ff453a]`} onClick={() => setConfirmClear(true)}>
                  <span className="col-span-2 text-center">Clear friends list</span>
                </button>
              )}
            </section>
          </>
        )}
        <p className={settingsUi.footer}>Friends are saved only in this browser.</p>
      </div>
    </div>
  );
}
