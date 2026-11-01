import { Check, Copy, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { QrImage } from './QrImage';

/**
 * Share one thing from the web player: a QR code for a phone's camera and the link to copy.
 * Nothing is sent anywhere; the link holds only the shared item itself.
 */
export function ShareDialog({ title, subtitle, link, onClose }: { title: string; subtitle?: string | null; link: string; onClose: () => void }) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  useEffect(() => {
    if (!copied) return;
    const t = setTimeout(() => setCopied(false), 1500);
    return () => clearTimeout(t);
  }, [copied]);

  return (
    <div className="fixed inset-0 z-(--z-contextual-menus) grid place-items-center bg-black/50 p-4" onPointerDown={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Share ${title}`}
        className="relative flex w-full max-w-[340px] flex-col items-center gap-3 rounded-[14px] bg-thick-material p-5 shadow-[0_0_0_0.5px_var(--vibrantDivider),0_10px_40px_var(--glassMaterialShadowColor)] [backdrop-filter:saturate(2.2)_blur(32px)]"
        onPointerDown={(event) => event.stopPropagation()}
      >
        <button className="absolute top-2 right-2 grid size-7 place-items-center rounded-full text-secondary hover:bg-quinary" onClick={onClose} aria-label="Close">
          <X size={16} />
        </button>
        <div className="max-w-full text-center">
          <p className="truncate type-title-3-emphasized">{title}</p>
          {subtitle && <p className="truncate type-callout text-secondary">{subtitle}</p>}
        </div>
        <QrImage content={link} />
        <p className="type-callout text-secondary text-center">Point a phone camera at this code. The link opens in Violence.</p>
        <button
          className="inline-flex items-center gap-2 rounded-pill bg-quaternary px-5 py-2 type-body-emphasized"
          onClick={() => void navigator.clipboard.writeText(link).then(() => setCopied(true))}
        >
          {copied ? <Check size={16} /> : <Copy size={16} />}
          {copied ? 'Copied' : 'Copy link'}
        </button>
      </div>
    </div>
  );
}
