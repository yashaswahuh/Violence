import { Check, Copy, ExternalLink } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { parseSongLink, SONG_WEB_PREFIX, songToUri, songToWebUrl } from '@/share/songLinks';
import { settingsUi } from './settingsStyles';

const card = 'flex flex-col items-center gap-2.5 rounded-[10px] bg-quinary px-4 py-7 text-center';
const primary = 'inline-flex items-center gap-2 rounded-pill bg-key-bg px-6 py-2.5 type-title-3-emphasized text-white';
const secondary = 'inline-flex items-center gap-2 rounded-pill bg-quaternary px-5 py-2 type-body-emphasized';

/**
 * The page a shared song link lands on: `/song/<source>/<id>?v=1&t=...&a=...` under the base path.
 *
 * A small card (title and artist as plain text), a button that opens the Android app with the
 * `violence://` link, and a copy button. Nothing happens by itself: the app link is a button, not
 * a redirect, so there is no loop between the browser and the app. The link is re-read with the
 * same strict parser the apps use, so a hand-edited link shows the "not a song link" message.
 * No server is called. The site is `noindex` (index.html).
 */
export default function SongLandingPage() {
  const params = useParams();
  const location = useLocation();
  const [copied, setCopied] = useState(false);
  const parsed = parseSongLink(`${SONG_WEB_PREFIX}${params.source ?? ''}/${params.id ?? ''}${location.search}`);

  useEffect(() => {
    if (!copied) return;
    const t = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(t);
  }, [copied]);

  if (!parsed.ok) {
    return (
      <div className={settingsUi.centered}>
        <PageHeader title="Shared song" />
        <div className={settingsUi.body}>
          <p className={settingsUi.footer}>That doesn’t look like a Violence song link.</p>
        </div>
      </div>
    );
  }

  const { ref } = parsed;
  const link = songToWebUrl(ref);
  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Shared song" />
      <div className={settingsUi.body}>
        <div className={card}>
          <p className={settingsUi.footer}>Someone shared a song with you</p>
          <p className="type-title-1-emphasized">{ref.title ?? 'A song'}</p>
          {ref.artist && <p className="type-body text-secondary">{ref.artist}</p>}
          <a className={primary} href={songToUri(ref)}>
            <ExternalLink size={18} />
            Open in Violence app
          </a>
          <button className={secondary} onClick={() => void navigator.clipboard.writeText(link).then(() => setCopied(true))}>
            {copied ? <Check size={18} /> : <Copy size={18} />}
            {copied ? 'Copied' : 'Copy link'}
          </button>
        </div>
        <p className={settingsUi.footer}>
          The button opens the song in the Violence Android app. Nothing plays until you choose to. In the browser, playing music needs
          the Violence extension.
        </p>
      </div>
    </div>
  );
}
