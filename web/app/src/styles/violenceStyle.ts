/**
 * Violence Style (Experimental) — Core Color Math, Tokens and Style State
 *
 * Direct TypeScript port of AccentMath.kt, AccentEngine.kt, and StyleModel.kt
 * from :shared to guarantee mathematical parity between Desktop and Web.
 */

export type StyleBackground = 'AURORA' | 'CORNER_GLOW' | 'HAZE' | 'OFF';
export type StyleMotion = 'OFF' | 'SUBTLE' | 'NORMAL';
export type StyleIntensity = 'LOW' | 'MEDIUM' | 'HIGH';
export type StyleAccentSource = 'FROM_SONG' | 'FROM_DEVICE' | 'CUSTOM';
export type StyleHeadingFont = 'BODONI_MODA' | 'CORMORANT_GARAMOND' | 'ANTON';

export interface StyleState {
  enabled: boolean;
  background: StyleBackground;
  motion: StyleMotion;
  intensity: StyleIntensity;
  accentSource: StyleAccentSource;
  customColor: string; // hex (e.g. #3F7BFF)
  headingFont: StyleHeadingFont;
  reduceTransparency: boolean;
  detailGlow: boolean;
}

export const DEFAULT_STYLE_STATE: StyleState = {
  enabled: false,
  background: 'AURORA',
  motion: 'NORMAL',
  intensity: 'MEDIUM',
  accentSource: 'FROM_SONG',
  customColor: '#3F7BFF',
  headingFont: 'BODONI_MODA',
  reduceTransparency: false,
  detailGlow: true,
};

export const ACCENT_PRESETS = [
  { id: 'BLUE', name: 'Blue', hex: '#3F7BFF' },
  { id: 'RED', name: 'Red', hex: '#E5384F' },
  { id: 'PINK', name: 'Pink', hex: '#E04A9B' },
  { id: 'GREEN', name: 'Green', hex: '#2FBF71' },
  { id: 'ORANGE', name: 'Orange', hex: '#F28C28' },
  { id: 'YELLOW', name: 'Yellow', hex: '#F2C230' },
  { id: 'CYAN', name: 'Cyan', hex: '#26C6DA' },
  { id: 'PURPLE', name: 'Purple', hex: '#8B5CF6' },
  { id: 'WHITE', name: 'White', hex: '#E8E8F0' },
] as const;

export const INTENSITY_PEAKS: Record<StyleIntensity, number> = {
  LOW: 0.38,
  MEDIUM: 0.62,
  HIGH: 0.85,
};

export type RGB = [number, number, number];

export interface HSL {
  h: number;
  s: number;
  l: number;
}

export interface AccentTokens {
  accent: string;
  accentSoft: string;
  glowColor: string;
  onAccent: '#ffffff' | '#000000';
  textAccent: string;
  neutral: boolean;
}

const DARK_BACKGROUND: RGB = [0, 0, 0];
const WHITE: RGB = [255, 255, 255];
const BLACK: RGB = [0, 0, 0];

const MIN_SATURATION = 0.55;
const MAX_SATURATION = 0.90;
const MIN_LIGHTNESS = 0.45;
const MAX_LIGHTNESS = 0.65;
const GLOW_LIGHTNESS_FACTOR = 0.8;
const NEUTRAL_SATURATION = 0.08;
const NEUTRAL_CHROMA = 0.08;
const SMALL_TEXT_CONTRAST = 4.5;

function hexToRgb(hex: string): RGB {
  const clean = hex.replace('#', '').trim();
  if (clean.length === 3) {
    const c0 = clean.charAt(0);
    const c1 = clean.charAt(1);
    const c2 = clean.charAt(2);
    const r = parseInt(c0 + c0, 16);
    const g = parseInt(c1 + c1, 16);
    const b = parseInt(c2 + c2, 16);
    return [r, g, b];
  }
  const num = parseInt(clean.slice(0, 6), 16);
  if (Number.isNaN(num)) return [63, 123, 255];
  return [(num >> 16) & 0xff, (num >> 8) & 0xff, num & 0xff];
}

function rgbToHex([r, g, b]: RGB): string {
  const toHex = (c: number) => Math.max(0, Math.min(255, Math.round(c))).toString(16).padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function relativeLuminance([r, g, b]: RGB): number {
  const channel = (v: number) => {
    const c = v / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

function contrastRatio(a: RGB, b: RGB): number {
  const la = relativeLuminance(a);
  const lb = relativeLuminance(b);
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}

function chroma([r, g, b]: RGB): number {
  return (Math.max(r, g, b) - Math.min(r, g, b)) / 255;
}

function toHsl([r, g, b]: RGB): HSL {
  const rf = r / 255;
  const gf = g / 255;
  const bf = b / 255;
  const max = Math.max(rf, gf, bf);
  const min = Math.min(rf, gf, bf);
  const l = (max + min) / 2;
  const d = max - min;
  if (d === 0) return { h: 0, s: 0, l };

  const s = d / (1 - Math.abs(2 * l - 1));
  let h: number;
  if (max === rf) {
    h = 60 * (((gf - bf) / d) % 6);
  } else if (max === gf) {
    h = 60 * ((bf - rf) / d + 2);
  } else {
    h = 60 * ((rf - gf) / d + 4);
  }
  return { h: h < 0 ? h + 360 : h, s: Math.max(0, Math.min(1, s)), l };
}

function fromHsl(h: number, s: number, l: number): RGB {
  const hh = ((h % 360) + 360) % 360;
  const ss = Math.max(0, Math.min(1, s));
  const ll = Math.max(0, Math.min(1, l));
  const c = (1 - Math.abs(2 * ll - 1)) * ss;
  const x = c * (1 - Math.abs(((hh / 60) % 2) - 1));
  const m = ll - c / 2;

  let r1 = 0;
  let g1 = 0;
  let b1 = 0;
  if (hh < 60) {
    r1 = c; g1 = x; b1 = 0;
  } else if (hh < 120) {
    r1 = x; g1 = c; b1 = 0;
  } else if (hh < 180) {
    r1 = 0; g1 = c; b1 = x;
  } else if (hh < 240) {
    r1 = 0; g1 = x; b1 = c;
  } else if (hh < 300) {
    r1 = x; g1 = 0; b1 = c;
  } else {
    r1 = c; g1 = 0; b1 = x;
  }

  return [
    Math.max(0, Math.min(255, Math.round((r1 + m) * 255))),
    Math.max(0, Math.min(255, Math.round((g1 + m) * 255))),
    Math.max(0, Math.min(255, Math.round((b1 + m) * 255))),
  ];
}

function isNeutral(rgb: RGB): boolean {
  return chroma(rgb) < NEUTRAL_CHROMA || toHsl(rgb).s < NEUTRAL_SATURATION;
}

function isDull(rgb: RGB): boolean {
  return chroma(rgb) < 0.08 || toHsl(rgb).s < 0.20;
}

function clampAccent(rgb: RGB): RGB {
  const hsl = toHsl(rgb);
  if (isNeutral(rgb)) {
    return fromHsl(hsl.h, Math.min(hsl.s, NEUTRAL_SATURATION), Math.max(0.80, Math.min(0.95, hsl.l)));
  }
  return fromHsl(
    hsl.h,
    Math.max(MIN_SATURATION, Math.min(MAX_SATURATION, hsl.s)),
    Math.max(MIN_LIGHTNESS, Math.min(MAX_LIGHTNESS, hsl.l)),
  );
}

function liftToContrast(rgb: RGB, background: RGB = DARK_BACKGROUND, minContrast = SMALL_TEXT_CONTRAST): RGB {
  if (contrastRatio(rgb, background) >= minContrast) return rgb;
  const hsl = toHsl(rgb);
  let l = hsl.l;
  while (l < 1) {
    l = Math.min(1, l + 0.02);
    const candidate = fromHsl(hsl.h, hsl.s, l);
    if (contrastRatio(candidate, background) >= minContrast) return candidate;
  }
  return WHITE;
}

function onColor(rgb: RGB): '#ffffff' | '#000000' {
  return contrastRatio(WHITE, rgb) >= contrastRatio(BLACK, rgb) ? '#ffffff' : '#000000';
}

function isNegligibleChange(a: RGB, b: RGB): boolean {
  const x = toHsl(a);
  const y = toHsl(b);
  let dh = Math.abs(x.h - y.h);
  if (dh > 180) dh = 360 - dh;
  return dh < 6 && Math.abs(x.l - y.l) < 0.04 && Math.abs(x.s - y.s) < 0.08;
}

function makeUsable(rgb: RGB): RGB | null {
  const hsl = toHsl(rgb);
  if (isNeutral(rgb)) {
    return hsl.l >= 0.70 ? clampAccent(rgb) : null;
  }
  if (hsl.l < 0.08 || hsl.l > 0.97) return null;
  return clampAccent(rgb);
}

export const AccentMath = {
  DARK_BACKGROUND,
  WHITE,
  BLACK,
  MIN_SATURATION,
  MAX_SATURATION,
  MIN_LIGHTNESS,
  MAX_LIGHTNESS,
  GLOW_LIGHTNESS_FACTOR,
  NEUTRAL_SATURATION,
  NEUTRAL_CHROMA,
  SMALL_TEXT_CONTRAST,
  hexToRgb,
  rgbToHex,
  relativeLuminance,
  contrastRatio,
  chroma,
  toHsl,
  fromHsl,
  isNeutral,
  isDull,
  clampAccent,
  liftToContrast,
  onColor,
  isNegligibleChange,
  makeUsable,
};

export const AccentEngine = {
  tokensFrom(sourceHex: string): AccentTokens {
    const rgb = hexToRgb(sourceHex);
    const clampedRgb = clampAccent(rgb);
    const neutral = isNeutral(rgb);
    const hsl = toHsl(clampedRgb);
    const glowRgb = fromHsl(
      hsl.h,
      Math.min(hsl.s, neutral ? NEUTRAL_SATURATION : 0.85),
      hsl.l * GLOW_LIGHTNESS_FACTOR,
    );
    const textRgb = liftToContrast(clampedRgb);

    return {
      accent: rgbToHex(clampedRgb),
      accentSoft: `rgba(${clampedRgb[0]}, ${clampedRgb[1]}, ${clampedRgb[2]}, 0.22)`,
      glowColor: rgbToHex(glowRgb),
      onAccent: onColor(clampedRgb),
      textAccent: rgbToHex(textRgb),
      neutral,
    };
  },

  resolve(
    source: StyleAccentSource,
    songHex: string | null,
    customHex: string,
    previousSongHex: string | null = null,
  ): string {
    const defaultColor = '#3F7BFF';
    switch (source) {
      case 'FROM_SONG': {
        if (songHex) {
          const rgb = hexToRgb(songHex);
          if (isDull(rgb)) return '#E8E8F0'; // White neutral
          return songHex;
        }
        if (previousSongHex) return previousSongHex;
        return customHex || defaultColor;
      }
      case 'FROM_DEVICE':
      case 'CUSTOM':
      default: {
        const rgb = hexToRgb(customHex);
        const usable = makeUsable(rgb);
        return usable ? customHex : defaultColor;
      }
    }
  },
};
