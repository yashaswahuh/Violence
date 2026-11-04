import React from 'react';
import { useViolenceStyle } from '@/state/violenceStyle';

interface Props {
  title: string;
  subtitle?: string;
  onShowAll?: () => void;
  showAllLabel?: string;
  hero?: boolean;
}

function splitTitle(title: string): { caps: string; italic: string | null } {
  const text = title.trim();
  const at = text.indexOf(' for ');
  if (at > 0 && at + 5 < text.length) {
    return {
      caps: text.substring(0, at).toUpperCase(),
      italic: text.substring(at + 1),
    };
  }
  return { caps: text.toUpperCase(), italic: null };
}

export function ViolenceSectionHeader({
  title,
  subtitle,
  onShowAll,
  showAllLabel = 'Show all',
  hero = false,
}: Props) {
  const enabled = useViolenceStyle((s) => s.style.enabled);
  if (!enabled) return null;

  const { caps, italic } = splitTitle(title);

  return (
    <div className="violence-section-header flex flex-col gap-1 py-3 px-1">
      <div className="flex items-center gap-3">
        <h2 className="flex flex-wrap items-baseline gap-2 tracking-wide font-display text-white">
          <span className="text-xl md:text-2xl font-bold uppercase tracking-wider">{caps}</span>
          {italic && <span className="text-xl md:text-2xl italic font-serif opacity-90">{italic}</span>}
        </h2>

        {/* Decorative Hairline */}
        <div className="relative flex-1 h-px bg-white/20 min-w-6">
          {hero && (
            <div
              className="absolute right-0 top-1/2 -translate-y-1/2 size-2 rounded-full"
              style={{
                backgroundColor: 'var(--vs-accent, #3f7bff)',
                boxShadow: '0 0 8px 2px var(--vs-accent, #3f7bff)',
              }}
            />
          )}
        </div>

        {onShowAll && (
          <button
            type="button"
            onClick={onShowAll}
            className="text-xs font-semibold hover:underline"
            style={{ color: 'var(--vs-text-accent, #3f7bff)' }}
          >
            {showAllLabel}
          </button>
        )}
      </div>

      {subtitle && <p className="text-sm text-white/60 truncate">{subtitle}</p>}
    </div>
  );
}

