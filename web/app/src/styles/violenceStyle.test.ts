import { describe, expect, it } from 'vitest';
import {
  AccentEngine,
  AccentMath,
  ACCENT_PRESETS,
  DEFAULT_STYLE_STATE,
  INTENSITY_PEAKS,
  type RGB,
} from './violenceStyle';

describe('ViolenceStyle Core Math and Tokens', () => {
  it('default style state has style disabled and matches defaults', () => {
    expect(DEFAULT_STYLE_STATE.enabled).toBe(false);
    expect(DEFAULT_STYLE_STATE.background).toBe('AURORA');
    expect(DEFAULT_STYLE_STATE.motion).toBe('NORMAL');
    expect(DEFAULT_STYLE_STATE.intensity).toBe('MEDIUM');
    expect(DEFAULT_STYLE_STATE.accentSource).toBe('FROM_SONG');
    expect(DEFAULT_STYLE_STATE.customColor).toBe('#3F7BFF');
    expect(DEFAULT_STYLE_STATE.headingFont).toBe('BODONI_MODA');
  });

  it('intensity peaks match specification', () => {
    expect(INTENSITY_PEAKS.LOW).toBe(0.38);
    expect(INTENSITY_PEAKS.MEDIUM).toBe(0.62);
    expect(INTENSITY_PEAKS.HIGH).toBe(0.85);
  });

  it('calculates relative luminance and contrast ratios correctly', () => {
    const white: RGB = [255, 255, 255];
    const black: RGB = [0, 0, 0];
    expect(AccentMath.relativeLuminance(white)).toBeCloseTo(1.0, 2);
    expect(AccentMath.relativeLuminance(black)).toBeCloseTo(0.0, 2);
    expect(AccentMath.contrastRatio(white, black)).toBeCloseTo(21.0, 1);
    expect(AccentMath.contrastRatio(white, white)).toBeCloseTo(1.0, 2);
  });

  it('identifies neutral colors correctly', () => {
    expect(AccentMath.isNeutral([128, 128, 128])).toBe(true);
    expect(AccentMath.isNeutral([232, 232, 240])).toBe(true);
    expect(AccentMath.isNeutral([255, 0, 0])).toBe(false);
    expect(AccentMath.isNeutral([63, 123, 255])).toBe(false);
  });

  it('clamps chromatic colors within saturation and lightness bounds', () => {
    const pureBlue: RGB = [0, 0, 255];
    const clamped = AccentMath.clampAccent(pureBlue);
    const hsl = AccentMath.toHsl(clamped);
    expect(hsl.s).toBeGreaterThanOrEqual(0.54);
    expect(hsl.s).toBeLessThanOrEqual(0.91);
    expect(hsl.l).toBeGreaterThanOrEqual(0.44);
    expect(hsl.l).toBeLessThanOrEqual(0.66);
  });

  it('lifts text colors to reach WCAG AA contrast (4.5:1)', () => {
    for (const preset of ACCENT_PRESETS) {
      const rgb = AccentMath.hexToRgb(preset.hex);
      const clamped = AccentMath.clampAccent(rgb);
      const lifted = AccentMath.liftToContrast(clamped);
      const ratio = AccentMath.contrastRatio(lifted, AccentMath.DARK_BACKGROUND);
      expect(ratio).toBeGreaterThanOrEqual(AccentMath.SMALL_TEXT_CONTRAST);
    }
  });

  it('derives tokens for all presets without error', () => {
    for (const preset of ACCENT_PRESETS) {
      const tokens = AccentEngine.tokensFrom(preset.hex);
      expect(tokens.accent).toMatch(/^#[0-9a-f]{6}$/i);
      expect(tokens.glowColor).toMatch(/^#[0-9a-f]{6}$/i);
      expect(tokens.textAccent).toMatch(/^#[0-9a-f]{6}$/i);
      expect(tokens.onAccent).toMatch(/^#(ffffff|000000)$/);
      expect(typeof tokens.neutral).toBe('boolean');
    }
  });

  it('resolves accent sources correctly', () => {
    // FROM_SONG with vivid song
    expect(AccentEngine.resolve('FROM_SONG', '#FF0000', '#3F7BFF')).toBe('#FF0000');

    // FROM_SONG with dull/gray song -> neutral white preset
    expect(AccentEngine.resolve('FROM_SONG', '#3C3C3C', '#3F7BFF')).toBe('#E8E8F0');

    // FROM_SONG fallback to previous song
    expect(AccentEngine.resolve('FROM_SONG', null, '#3F7BFF', '#2FBF71')).toBe('#2FBF71');

    // CUSTOM source
    expect(AccentEngine.resolve('CUSTOM', null, '#8B5CF6')).toBe('#8B5CF6');
  });

  it('detects negligible color changes for smooth transitions', () => {
    const red1 = AccentMath.hexToRgb('#FF375F');
    const red2 = AccentMath.hexToRgb('#FE355D');
    expect(AccentMath.isNegligibleChange(red1, red2)).toBe(true);

    const blue = AccentMath.hexToRgb('#3F7BFF');
    expect(AccentMath.isNegligibleChange(red1, blue)).toBe(false);
  });

  it('updates DOM attributes and theme elements when enabled', async () => {
    const attrs = new Map<string, string>();
    const fakeDoc = {
      documentElement: {
        setAttribute: (k: string, v: string) => attrs.set(k, v),
        removeAttribute: (k: string) => attrs.delete(k),
        getAttribute: (k: string) => attrs.get(k) ?? null,
      },
      querySelectorAll: () => [],
      querySelector: () => null,
    };
    (globalThis as unknown as { document: typeof fakeDoc }).document = fakeDoc;

    try {
      const { useViolenceStyle } = await import('@/state/violenceStyle');
      const store = useViolenceStyle.getState();

      // Initially disabled -> no attribute
      expect(fakeDoc.documentElement.getAttribute('data-violence-style')).toBeNull();

      // Enable
      store.setEnabled(true);
      expect(fakeDoc.documentElement.getAttribute('data-violence-style')).toBe('true');
      expect(fakeDoc.documentElement.getAttribute('data-vs-font')).toBe('bodoni-moda');

      // Change font
      store.setHeadingFont('CORMORANT_GARAMOND');
      expect(fakeDoc.documentElement.getAttribute('data-vs-font')).toBe('cormorant-garamond');

      // Disable -> attributes cleaned up
      store.setEnabled(false);
      expect(fakeDoc.documentElement.getAttribute('data-violence-style')).toBeNull();
      expect(fakeDoc.documentElement.getAttribute('data-vs-font')).toBeNull();
    } finally {
      delete (globalThis as unknown as { document?: typeof fakeDoc }).document;
    }
  });
});
