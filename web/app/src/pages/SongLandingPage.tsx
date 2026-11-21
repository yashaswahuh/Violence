import { Check, Copy, ExternalLink } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { appLink, tryAppOnce } from '@/share/openApp';
import { parseSongLink, SONG_WEB_PREFIX, songToUri, songToWebUrl } from '@/share/songLinks';
import { settingsUi } from './settingsStyles';

const card = 'flex flex-col items-center gap-2.5 rounded-[10px] bg-quinary px-4 py-7 text-center';
const primary = 'inline-flex items-center gap-2 rounded-pill bg-key-bg px-6 py-2.5 type-title-3-emphasized text-white';
const secondary = 'inline-flex items-center gap-2 rounded-pill bg-quaternary px-5 py-2 type-body-emphasized';

/** Where a song opens in the browser: YouTube Music, by id for a YouTube song and by its words for the rest. */
function browserUrl(ref: { source: string; id: string; title: string | null; artist: string | null }): string {
  if (ref.source === 'yt') return `https://music.youtube.com/watch?v=${encodeURIComponent(ref.id)}`;
  const words = [ref.title, ref.artist].filter(Boolean).join(' ');
  return `https://music.youtube.com/search?q=${encodeURIComponent(words)}`;
}

/**
 * The page a shared song link lands on: `/song/<source>/<id>?v=1&t=...&a=...` under the base path.
 *
 * A small card (title and artist as plain text) with two ways in: the Violence app, and the browser
 * (YouTube Music). On Android the page also tries the app once by itself, with the browser as the
 * fallback: if Violence is installed it opens there, and if not the browser goes to the song. That is
 * done with an Android intent address, so the browser decides whether the app exists. It tries once per
 * tab, so coming back from the app does not send the person round again; the buttons are always there.
 * The link is re-read with the same strict parser the apps use, so a hand-edited link shows the "not a
 * song link" message. No server is called. The site is `noindex` (index.html).
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

  // On Android, try the app once, falling back to the browser: see the note on this page.
  const autoOpen = parsed.ok ? appLink(songToUri(parsed.ref), browserUrl(parsed.ref)) : null;
  useEffect(() => {
    if (autoOpen) tryAppOnce(location.pathname + location.search, autoOpen);
  }, [autoOpen, location.pathname, location.search]);

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
  const browser = browserUrl(ref);
  const open = appLink(songToUri(ref), browser);
  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Shared song" />
      <div className={settingsUi.body}>
        <div className={card}>
          <p className={settingsUi.footer}>Someone shared a song with you</p>
          <p className="type-title-1-emphasized">{ref.title ?? 'A song'}</p>
          {ref.artist && <p className="type-body text-secondary">{ref.artist}</p>}
          <a className={primary} href={open}>
            <ExternalLink size={18} />
            Open in Violence app
          </a>
          <a className={secondary} href={browser} target="_blank" rel="noreferrer">
            <ExternalLink size={18} />
            Open in browser
          </a>
          <button className={secondary} onClick={() => void navigator.clipboard.writeText(link).then(() => setCopied(true))}>
            {copied ? <Check size={18} /> : <Copy size={18} />}
            {copied ? 'Copied' : 'Copy link'}
          </button>
        </div>
        <p className={settingsUi.footer}>
          On Android the song opens in the Violence app if you have it, and in YouTube Music in your browser if you do not. Nothing
          plays until you choose to.
        </p>
      </div>
    </div>
  );
}
