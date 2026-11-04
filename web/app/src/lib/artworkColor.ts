import { useEffect, useState } from 'react';
import { artworkAtPixels } from './artwork';

export interface ArtworkColor {
  /** The background colour behind the page ("joe colour", report §3). */
  background: string;
  /** Whether text on it should be light (a dark colour) or dark. */
  dark: boolean;
  /** The vibrant accent colour extracted from the artwork (hex #RRGGBB). */
  accentHex?: string;
}

const cache = new Map<string, ArtworkColor>();

function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
  const rf = r / 255;
  const gf = g / 255;
  const bf = b / 255;
  const max = Math.max(rf, gf, bf);
  const min = Math.min(rf, gf, bf);
  const d = max - min;
  const l = (max + min) / 2;
  if (d === 0) return [0, 0, l];
  const s = d / (1 - Math.abs(2 * l - 1));
  let h = 0;
  if (max === rf) {
    h = 60 * (((gf - bf) / d) % 6);
  } else if (max === gf) {
    h = 60 * ((bf - rf) / d + 2);
  } else {
    h = 60 * ((rf - gf) / d + 4);
  }
  if (h < 0) h += 360;
  return [h, s, l];
}

const toHex = (c: number) => c.toString(16).padStart(2, '0');

/**
 * The artwork's colours, drawn from a copy on canvas.
 * Extracts both the classic background tint and the vibrant accent swatch
 * matching AndroidX Palette behaviour.
 */
async function measure(url: string): Promise<ArtworkColor> {
  const image = new Image();
  image.crossOrigin = 'anonymous';
  image.referrerPolicy = 'no-referrer';
  image.decoding = 'async';
  // 60px: a size YouTube's pages request, so the CDN has it cached (lib/artwork.ts).
  image.src = artworkAtPixels(url, 60);
  await image.decode();

  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 32;
  const context = canvas.getContext('2d', { willReadFrequently: true })!;
  context.drawImage(image, 0, 0, 32, 32);
  const { data } = context.getImageData(0, 0, 32, 32);

  let bgR = 0;
  let bgG = 0;
  let bgB = 0;
  const buckets = new Map<number, { r: number; g: number; b: number; count: number }>();

  for (let i = 0; i < data.length; i += 4) {
    const a = data[i + 3]!;
    if (a < 128) continue;
    const r = data[i]!;
    const g = data[i + 1]!;
    const b = data[i + 2]!;

    bgR += r;
    bgG += g;
    bgB += b;

    // 5-bit channel quantization (32x32x32 = 32768 colors)
    const key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
    const existing = buckets.get(key);
    if (existing) {
      existing.r += r;
      existing.g += g;
      existing.count++;
    } else {
      buckets.set(key, { r, g, b, count: 1 });
    }
  }

  const n = (data.length / 4) || 1;
  const avgR = bgR / n;
  const avgG = bgG / n;
  const avgB = bgB / n;

  // Apple darkens the colour behind its dark layout; keep text legible.
  const luminance = (0.2126 * avgR + 0.7152 * avgG + 0.0722 * avgB) / 255;
  const dark = luminance < 0.6;
  const k = dark ? Math.min(1, 0.55 / Math.max(luminance, 0.05)) * 0.6 : 1;
  const scale = (v: number) => Math.round(dark ? Math.min(v * k, 90) : v);
  const background = `rgb(${scale(avgR)}, ${scale(avgG)}, ${scale(avgB)})`;

  // Swatch candidate extraction
  const swatches: Array<{ r: number; g: number; b: number; count: number }> = [];
  for (const b of buckets.values()) {
    swatches.push({
      r: Math.round(b.r / b.count),
      g: Math.round(b.g / b.count),
      b: Math.round(b.b / b.count),
      count: b.count,
    });
  }

  // Filter out near-black, near-white, and skin tones (AndroidX Palette default filter)
  const candidates = swatches.filter((s) => {
    const [h, sat, lit] = rgbToHsl(s.r, s.g, s.b);
    const isBlack = lit <= 0.08;
    const isWhite = lit >= 0.95;
    const nearRedILine = h >= 10 && h <= 37 && sat <= 0.82;
    return !isBlack && !isWhite && !nearRedILine;
  });

  const pool = candidates.length > 0 ? candidates : swatches;

  // Score vibrant: saturation * sqrt(population)
  let bestSwatch = pool[0];
  let bestScore = -1;
  for (const s of pool) {
    const [, sat] = rgbToHsl(s.r, s.g, s.b);
    const score = sat * Math.sqrt(s.count);
    if (score > bestScore) {
      bestScore = score;
      bestSwatch = s;
    }
  }

  const accentHex = bestSwatch
    ? `#${toHex(bestSwatch.r)}${toHex(bestSwatch.g)}${toHex(bestSwatch.b)}`
    : undefined;

  return { background, dark, accentHex };
}

export function useArtworkColor(url: string | null | undefined): ArtworkColor | null {
  const [state, setState] = useState<{ url: string | null | undefined; color: ArtworkColor | null }>({
    url,
    color: url ? cache.get(url) ?? null : null,
  });

  // Always evaluate color against the current url. If url changed and is not yet in state,
  // return the cached color if ready, or null while loading. Never return another url's color!
  const color = state.url === url ? state.color : (url ? cache.get(url) ?? null : null);

  useEffect(() => {
    if (!url) {
      setState({ url: null, color: null });
      return;
    }
    const known = cache.get(url);
    if (known) {
      setState({ url, color: known });
      return;
    }
    let live = true;
    measure(url)
      .then((measured) => {
        cache.set(url, measured);
        if (live) setState({ url, color: measured });
      })
      .catch(() => {
        // An unreadable or failed image clears the color rather than keeping a stale one
        if (live) setState({ url, color: null });
      });
    return () => {
      live = false;
    };
  }, [url]);

  return color;
}
