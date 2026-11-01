import { Check, Copy, ExternalLink, Globe } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { appLink, cleanCode, sanitizeServerUrl } from '@/party/invite';
import { settingsUi } from './settingsStyles';

const card = 'flex flex-col items-center gap-2.5 rounded-[10px] bg-quinary px-4 py-7 text-center';
const codeStyle = 'type-[700_40px/48px] tracking-[0.18em]';
const primary = 'inline-flex items-center gap-2 rounded-pill bg-key-bg px-6 py-2.5 type-title-3-emphasized text-white';
const secondary = 'inline-flex items-center gap-2 rounded-pill bg-quaternary px-5 py-2 type-body-emphasized';

/**
 * The page a shared party link lands on: `/party/<CODE>` (under the site's base path).
 *
 * Offers the three things a person arriving from a chat can want: open the Android app, join right
 * here, or just copy the code. Nothing happens by itself: the app link is a button, not a redirect,
 * so there is no way to loop between the browser and the app. The site is `noindex` (index.html).
 */
export default function PartyLandingPage() {
  const params = useParams();
  const [search] = useSearchParams();
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);
  const code = params.code ? cleanCode(params.code) : null;
  const server = sanitizeServerUrl(search.get('server'));

  useEffect(() => {
    if (!copied) return;
    const t = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(t);
  }, [copied]);

  if (!code) {
    return (
      <div className={settingsUi.centered}>
        <PageHeader title="Listen Together" />
        <div className={settingsUi.body}>
          <p className={settingsUi.footer}>That doesn’t look like a party link. A party code is six letters or digits.</p>
          <button className={secondary} onClick={() => navigate('/settings/listen-together')}>
            Enter a code
          </button>
        </div>
      </div>
    );
  }

  const joinHere = () => navigate(`/invite/${code}${server ? `?server=${encodeURIComponent(server)}` : ''}`);

  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Listen Together" />
      <div className={settingsUi.body}>
        <div className={card}>
          <p className={settingsUi.footer}>You’re invited to a party</p>
          <p className={codeStyle}>{code}</p>
          <a className={primary} href={appLink(code, server)}>
            <ExternalLink size={18} />
            Open in Violence app
          </a>
          <button className={secondary} onClick={joinHere}>
            <Globe size={18} />
            Join in this browser
          </button>
          <button
            className={secondary}
            onClick={() => void navigator.clipboard.writeText(code).then(() => setCopied(true))}
          >
            {copied ? <Check size={18} /> : <Copy size={18} />}
            {copied ? 'Copied' : 'Copy code'}
          </button>
        </div>
        <p className={settingsUi.footer}>
          In the Android app, choose Listen Together, then Join, and paste this link or type the code. In the browser,
          playing music needs the Violence extension.
        </p>
      </div>
    </div>
  );
}
