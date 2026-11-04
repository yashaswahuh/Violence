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

function applyDomState(state: StyleState) {
  if (typeof document === 'undefined') return;
  if (state.enabled) {
    document.documentElement.setAttribute('data-violence-style', 'true');
    document.documentElement.setAttribute('data-vs-font', state.headingFont.toLowerCase().replace('_', '-'));
  } else {
    document.documentElement.removeAttribute('data-violence-style');
    document.documentElement.removeAttribute('data-vs-font');
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

