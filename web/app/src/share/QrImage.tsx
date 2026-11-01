import { encode } from 'uqr';

/**
 * A QR code as an SVG, drawn from the module matrix (uqr, MIT). Dark modules on white with a
 * quiet border, error correction M, so a phone camera reads it off a screen at an angle.
 */
export function QrImage({ content, size = 220 }: { content: string; size?: number }) {
  const { data } = encode(content, { ecc: 'M', border: 2 });
  const n = data.length;
  let d = '';
  for (let y = 0; y < n; y++) {
    const row = data[y]!;
    for (let x = 0; x < n; x++) if (row[x]) d += `M${x} ${y}h1v1h-1z`;
  }
  return (
    <svg viewBox={`0 0 ${n} ${n}`} width={size} height={size} role="img" aria-label="QR code" shapeRendering="crispEdges" className="rounded-[10px]">
      <rect width={n} height={n} fill="#ffffff" />
      <path d={d} fill="#000000" />
    </svg>
  );
}
