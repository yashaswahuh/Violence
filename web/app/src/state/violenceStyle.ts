import { create } from 'zustand';
import {
  DEFAULT_STYLE_STATE,
  type StyleAccentSource,
  type StyleBackground,
  type StyleHeadingFont,
  type StyleIntensity,
  type StyleMotion,
  type StyleState,
} from '@/styles/violenceStyle';

const STORAGE_KEY = 'bitchord:violence-style';

interface ViolenceStyleStore {
  style: StyleState;
  setEnabled: (enabled: boolean) => void;
  setBackground: (background: StyleBackground) => void;
  setMotion: (motion: StyleMotion) => void;
  setIntensity: (intensity: StyleIntensity) => void;
  setAccentSource: (accentSource: StyleAccentSource) => void;
  setCustomColor: (customColor: string) => void;
  setHeadingFont: (headingFont: StyleHeadingFont) => void;
  setReduceTransparency: (reduceTransparency: boolean) => void;
}

function loadSavedState(): StyleState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_STYLE_STATE;
    const parsed = JSON.parse(raw);
    return {
      ...DEFAULT_STYLE_STATE,
      ...parsed,
      enabled: Boolean(parsed.enabled), // strictly enforce boolean
    };
  } catch {
    return DEFAULT_STYLE_STATE;
  }
}

function saveState(state: StyleState) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Storage quota or private browsing mode
  }
}

function updateFavicon(accentHex: string | null) {
  if (typeof document === 'undefined') return;
  const favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
  if (!favicon) return;
  if (!accentHex) {
    if (favicon.hasAttribute('data-original-href')) {
      favicon.href = favicon.getAttribute('data-original-href')!;
    }
    return;
  }
  if (!favicon.hasAttribute('data-original-href')) {
    favicon.setAttribute('data-original-href', favicon.href);
  }
  const svg = `<svg width="1024" height="1024" viewBox="0 0 1024 1024" fill="none" xmlns="http://www.w3.org/2000/svg"><rect width="1024" height="1024" rx="224" fill="#05050A" stroke="${accentHex}" stroke-width="32"/><path d="M212 260 H372 L512 590 L652 260 H812 L556 756 C532 790 492 790 468 756 Z" fill="${accentHex}"/></svg>`;
  favicon.href = `data:image/svg+xml,${encodeURIComponent(svg)}`;
}

function applyDomState(state: StyleState) {
  if (typeof document === 'undefined') return;
  if (state.enabled) {
    document.documentElement.setAttribute('data-violence-style', 'true');
    document.documentElement.setAttribute('data-vs-font', state.headingFont.toLowerCase().replace('_', '-'));
    
    // Update theme-color meta tags to dark luminous canvas
    const themeMetas = document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]');
    themeMetas.forEach((meta) => {
      meta.setAttribute('content', '#05050A');
    });

    updateFavicon(state.customColor || '#3F7BFF');
  } else {
    document.documentElement.removeAttribute('data-violence-style');
    document.documentElement.removeAttribute('data-vs-font');

    // Restore default theme-colors
    const darkMeta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"][media*="dark"]');
    if (darkMeta) darkMeta.setAttribute('content', '#1f1f1f');
    const lightMeta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"][media*="light"]');
    if (lightMeta) lightMeta.setAttribute('content', '#ffffff');
    const defaultMeta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]:not([media])');
    if (defaultMeta) defaultMeta.setAttribute('content', '#1f1f1f');

    updateFavicon(null);
  }
}

export const useViolenceStyle = create<ViolenceStyleStore>()((set, get) => {
  const initial = loadSavedState();
  applyDomState(initial);

  const update = (partial: Partial<StyleState>) => {
    const next = { ...get().style, ...partial };
    saveState(next);
    applyDomState(next);
    set({ style: next });
  };

  return {
    style: initial,
    setEnabled: (enabled) => update({ enabled }),
    setBackground: (background) => update({ background }),
    setMotion: (motion) => update({ motion }),
    setIntensity: (intensity) => update({ intensity }),
    setAccentSource: (accentSource) => update({ accentSource }),
    setCustomColor: (customColor) => update({ customColor }),
    setHeadingFont: (headingFont) => update({ headingFont }),
    setReduceTransparency: (reduceTransparency) => update({ reduceTransparency }),
  };
});
