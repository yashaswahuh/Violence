import { Check, Copy, ExternalLink } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { PLAYLIST_WEB_PREFIX, parsePlaylistLink, playlistToAppUrl } from '@/share/playlistLinks';
import { settingsUi } from './settingsStyles';

const card = 'flex flex-col items-center gap-2.5 rounded-[10px] bg-quinary px-4 py-7 text-center';
const primary = 'inline-flex items-center gap-2 rounded-pill bg-key-bg px-6 py-2.5 type-title-3-emphasized text-white';
const secondary = 'inline-flex items-center gap-2 rounded-pill bg-quaternary px-5 py-2 type-body-emphasized';

/**
 * The page a shared playlist link lands on: `/playlist/yt/<id>?...` or `/playlist/songs?...` under
 * the base path. Same idea as the song page: a plain-text card, a button that opens the Android
 * app with the `violence://` link, and, for a YouTube Music playlist, a link to it. Nothing
 * happens by itself and no server is called. The link is re-read with the strict parser the apps
 * use, so a hand-edited link shows the "not a playlist link" message.
 */
export default function PlaylistLandingPage() {
  const location = useLocation();
  const [copied, setCopied] = useState(false);
  // The router's path is under the base, so the web link is rebuilt from it.
  const path = location.pathname.replace(/^.*\/playlist\//, '');
  const webUrl = `${PLAYLIST_WEB_PREFIX}${path}${location.search}`;
  const parsed = parsePlaylistLink(webUrl);

  useEffect(() => {
    if (!copied) return;
    const t = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(t);
  }, [copied]);

  if (!parsed.ok) {
    return (
      <div className={settingsUi.centered}>
        <PageHeader title="Shared playlist" />
        <div className={settingsUi.body}>
          <p className={settingsUi.footer}>That doesn’t look like a Violence playlist link.</p>
        </div>
      </div>
    );
  }

  const { ref } = parsed;
  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Shared playlist" />
      <div className={settingsUi.body}>
        <div className={card}>
          <p className={settingsUi.footer}>Someone shared a playlist with you</p>
          <p className="type-title-1-emphasized">{ref.title}</p>
          {ref.kind === 'songs' && <p className="type-body text-secondary">{ref.videoIds.length} songs</p>}
          <a className={primary} href={playlistToAppUrl(webUrl)}>
            <ExternalLink size={18} />
            Open in Violence app
          </a>
          {ref.kind === 'yt' && ref.id && (
            <a className={secondary} href={`https://music.youtube.com/playlist?list=${encodeURIComponent(ref.id)}`} target="_blank" rel="noreferrer">
              <ExternalLink size={18} />
              Open on YouTube Music
            </a>
          )}
          <button className={secondary} onClick={() => void navigator.clipboard.writeText(webUrl).then(() => setCopied(true))}>
            {copied ? <Check size={18} /> : <Copy size={18} />}
            {copied ? 'Copied' : 'Copy link'}
          </button>
        </div>
        <p className={settingsUi.footer}>
          The button opens the playlist in the Violence Android app. Nothing plays or is saved until you choose to.
        </p>
      </div>
    </div>
  );
}
