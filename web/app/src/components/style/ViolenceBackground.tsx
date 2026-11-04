import { useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import { useArtworkColor } from '@/lib/artworkColor';
import { usePlayer } from '@/state/player';
import { useViolenceStyle } from '@/state/violenceStyle';
import { AccentEngine, AccentMath, type AccentTokens } from '@/styles/violenceStyle';

const reducedMotionQuery = typeof matchMedia === 'function' ? matchMedia('(prefers-reduced-motion: reduce)') : null;
function useReducedMotion(): boolean {
  return useSyncExternalStore(
    (onChange) => {
      reducedMotionQuery?.addEventListener('change', onChange);
      return () => reducedMotionQuery?.removeEventListener('change', onChange);
    },
    () => reducedMotionQuery?.matches ?? false,
  );
}

function parseRgb(colorStr: string | null | undefined): string | null {
  if (!colorStr) return null;
  const match = colorStr.match(/\d+/g);
  if (!match || match.length < 3) return null;
  const [r, g, b] = [parseInt(match[0]!, 10), parseInt(match[1]!, 10), parseInt(match[2]!, 10)];
  const toHex = (c: number) => c.toString(16).padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

/**
 * Web Violence Style live background (DW-4).
 *
 * Implements LITE Style on Web (PARITY_DECISIONS Option A) using CSS radial gradients
 * for Aurora, Corner Glow, and Haze, and hardware-accelerated CSS transforms for drift.
 * Zero JavaScript animation loop, ultra-low memory, and zero battery drain.
 */
export function ViolenceBackground() {
  const style = useViolenceStyle((s) => s.style);
  const artworkUrl = usePlayer((s) => s.nowPlaying?.artworkUrl ?? null);

  // Debounce artworkUrl (400ms) matching Android's SONG_DEBOUNCE_MS
  const [stableUrl, setStableUrl] = useState<string | null>(artworkUrl);
  useEffect(() => {
    if (artworkUrl === stableUrl) return;
    const timer = setTimeout(() => {
      setStableUrl(artworkUrl);
    }, 400);
    return () => clearTimeout(timer);
  }, [artworkUrl, stableUrl]);

  const fromSong = style.accentSource === 'FROM_SONG';
  const artworkColor = useArtworkColor(fromSong ? stableUrl : null);
  const prefersStill = useReducedMotion();

  // Vibrant accent extracted from artwork (fallback to parsed background if missing)
  const songHex = artworkColor?.accentHex ?? parseRgb(artworkColor?.background);

  // Hold previous song accent to avoid flashing or falling to gray during track skips
  const [heldAccentHex, setHeldAccentHex] = useState<string | null>(null);

  useEffect(() => {
    if (!songHex) return;
    setHeldAccentHex((prev) => {
      if (!prev) return songHex;
      const rgbPrev = AccentMath.hexToRgb(prev);
      const rgbNext = AccentMath.hexToRgb(songHex);
      if (AccentMath.isNegligibleChange(rgbPrev, rgbNext)) return prev;
      return songHex;
    });
  }, [songHex]);

  const resolvedColor = useMemo(
    () => AccentEngine.resolve(style.accentSource, heldAccentHex, style.customColor, heldAccentHex),
    [style.accentSource, heldAccentHex, style.customColor],
  );

  const tokens: AccentTokens = useMemo(
    () => AccentEngine.tokensFrom(resolvedColor),
    [resolvedColor],
  );

  // Apply CSS custom properties to :root while Violence Style is enabled
  useEffect(() => {
    if (!style.enabled) return;
    const root = document.documentElement;
    root.style.setProperty('--vs-accent', tokens.accent);
    root.style.setProperty('--vs-accent-soft', tokens.accentSoft);
    root.style.setProperty('--vs-glow', tokens.glowColor);
    root.style.setProperty('--vs-text-accent', tokens.textAccent);
    root.style.setProperty('--vs-on-accent', tokens.onAccent);
    root.style.setProperty(
      '--vs-panel-tint',
      `color-mix(in srgb, #121214 92%, ${tokens.accent} 8%)`,
    );

    return () => {
      root.style.removeProperty('--vs-accent');
      root.style.removeProperty('--vs-accent-soft');
      root.style.removeProperty('--vs-glow');
      root.style.removeProperty('--vs-text-accent');
      root.style.removeProperty('--vs-on-accent');
      root.style.removeProperty('--vs-panel-tint');
    };
  }, [style.enabled, tokens]);

  if (!style.enabled || style.background === 'OFF') {
    return null;
  }

  const allowMotion = style.motion !== 'OFF' && !prefersStill;
  const isFlat = style.reduceTransparency;

  return (
    <div
      className="violence-background pointer-events-none fixed inset-0 -z-10 overflow-hidden bg-[#05050a]"
      data-style={style.background.toLowerCase()}
      data-motion={allowMotion ? style.motion.toLowerCase() : 'off'}
      data-flat={isFlat ? 'true' : undefined}
      data-intensity={style.intensity.toLowerCase()}
      aria-hidden="true"
    >
      {/* Base Canvas */}
      <div className="absolute inset-0 bg-[#05050a]" />

      {isFlat ? (
        /* Reduced transparency: subtle top-down accent gradient */
        <div
          className="absolute inset-x-0 top-0 h-[60%] opacity-20"
          style={{
            background: `linear-gradient(to bottom, ${tokens.glowColor}, transparent)`,
          }}
        />
      ) : (
        <>
          {/* Radial Gradient Blobs Container */}
          <div
            className={`violence-blobs absolute inset-0 size-full ${
              allowMotion ? 'animate-violence-drift' : ''
            }`}
            style={{
              opacity: style.intensity === 'LOW' ? 0.45 : style.intensity === 'HIGH' ? 0.95 : 0.72,
            }}
          >
            {style.background === 'AURORA' && (
              <>
                <div
                  className="violence-blob blob-1 absolute rounded-full blur-[80px]"
                  style={{
                    top: '8%',
                    left: '8%',
                    width: '75vw',
                    height: '75vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                  }}
                />
                <div
                  className="violence-blob blob-2 absolute rounded-full blur-[90px]"
                  style={{
                    top: '46%',
                    right: '0%',
                    width: '65vw',
                    height: '65vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                    opacity: 0.8,
                  }}
                />
                <div
                  className="violence-blob blob-3 absolute rounded-full blur-[70px]"
                  style={{
                    bottom: '10%',
                    left: '10%',
                    width: '50vw',
                    height: '50vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                    opacity: 0.35,
                  }}
                />
              </>
            )}

            {style.background === 'CORNER_GLOW' && (
              <>
                <div
                  className="violence-blob blob-corner-1 absolute rounded-full blur-[80px]"
                  style={{
                    top: '-10%',
                    left: '-10%',
                    width: '85vw',
                    height: '85vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                  }}
                />
                <div
                  className="violence-blob blob-corner-2 absolute rounded-full blur-[95px]"
                  style={{
                    top: '12%',
                    left: '25%',
                    width: '95vw',
                    height: '95vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                    opacity: 0.45,
                  }}
                />
              </>
            )}

            {style.background === 'HAZE' && (
              <>
                <div
                  className="violence-blob blob-haze-1 absolute rounded-full blur-[100px]"
                  style={{
                    top: '10%',
                    left: '20%',
                    width: '70vw',
                    height: '70vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                    opacity: 0.9,
                  }}
                />
                <div
                  className="violence-blob blob-haze-2 absolute rounded-full blur-[110px]"
                  style={{
                    top: '45%',
                    left: '25%',
                    width: '80vw',
                    height: '80vw',
                    background: `radial-gradient(circle, ${tokens.glowColor} 0%, transparent 70%)`,
                    opacity: 0.6,
                  }}
                />
              </>
            )}
          </div>

          {/* Vignette bottom fade */}
          <div
            className="absolute inset-x-0 bottom-0 h-[45%] pointer-events-none"
            style={{
              background: 'linear-gradient(to bottom, transparent 0%, #05050a 90%)',
            }}
          />
        </>
      )}
    </div>
  );
}

