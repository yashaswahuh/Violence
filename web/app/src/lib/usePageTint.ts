import { useEffect } from 'react';
import type { ArtworkColor } from './artworkColor';
import { useViolenceStyle } from '@/state/violenceStyle';

/**
 * Tints the whole scrolling page with the artwork's colour while a detail
 * page is open in classic theme, as Apple's album and playlist pages do (report §3,
 * `has-theme-override`).
 *
 * In Violence Style, the background is always the luminous canvas with live glow
 * set by the playing song's accent. Detail pages must never apply their own tint
 * to the background or override Violence Style tokens.
 */
export function usePageTint(color: ArtworkColor | null) {
  const violenceStyleEnabled = useViolenceStyle((s) => s.style.enabled);

  useEffect(() => {
    const page = document.getElementById('scrollable-page');
    if (!page) return;

    if (violenceStyleEnabled || !color) {
      page.style.removeProperty('--page-tint');
      delete page.dataset.tint;
      return;
    }

    page.style.setProperty('--page-tint', color.background);
    page.dataset.tint = color.dark ? 'dark' : 'light';
    return () => {
      page.style.removeProperty('--page-tint');
      delete page.dataset.tint;
    };
  }, [color, violenceStyleEnabled]);
}
