#!/usr/bin/env node
// Serves web/app/dist under /Violence/ the way GitHub Pages does, so the deployed behaviour can be
// checked on this machine: files are served as found, a path with no file gets 404.html with a 404
// status (that page then redirects into the app), and everything is case sensitive.
//
//   .\scripts\build-active.ps1 -Target web        (build with VITE_BASE_URL=/Violence/ first)
//   node scripts/serve-pages-local.mjs [port]      then open http://localhost:4173/Violence/
//   node scripts/serve-pages-local.mjs --check     starts it, requests the key URLs, prints PASS/FAIL, exits
//
// Needs Node 22+, nothing to install.

import { createServer } from 'node:http';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = '/Violence';
const DIST = resolve(fileURLToPath(new URL('../web/app/dist/', import.meta.url)));
const argPort = process.argv.slice(2).find((a) => /^\d+$/.test(a));
const PORT = Number(argPort || 4173);
const CHECK = process.argv.includes('--check');

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png',
  '.wasm': 'application/wasm', '.onnx': 'application/octet-stream', '.zip': 'application/zip', '.txt': 'text/plain; charset=utf-8',
  '.woff2': 'font/woff2', '.ico': 'image/x-icon',
};

function send(res, status, body, type = 'text/plain; charset=utf-8') {
  res.writeHead(status, { 'Content-Type': type });
  res.end(body);
}

const server = createServer((req, res) => {
  let pathname;
  try {
    pathname = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  } catch {
    return send(res, 400, 'bad request');
  }
  // Outside the site folder: Pages has nothing here (case sensitive: /violence/ is NOT the site).
  if (pathname !== BASE && !pathname.startsWith(BASE + '/')) return send(res, 404, 'Not found (outside /Violence/)');
  if (pathname === BASE) {
    res.writeHead(301, { Location: BASE + '/' });
    return res.end();
  }
  let rel = normalize(pathname.slice(BASE.length)).replace(/^[\\/]+/, '');
  let file = join(DIST, rel);
  if (!file.startsWith(DIST + sep) && file !== DIST) return send(res, 400, 'bad path');
  if (existsSync(file) && statSync(file).isDirectory()) file = join(file, 'index.html');
  if (existsSync(file) && statSync(file).isFile()) return send(res, 200, readFileSync(file), TYPES[extname(file).toLowerCase()] || 'application/octet-stream');
  // Pages' behaviour for a missing file: the site's 404.html, with status 404.
  const notFound = join(DIST, '404.html');
  return send(res, 404, existsSync(notFound) ? readFileSync(notFound) : 'Not found', 'text/html; charset=utf-8');
});

if (!existsSync(join(DIST, 'index.html'))) {
  console.error(`No build found in ${DIST}. Build the web target first (scripts/build-active.ps1 -Target web).`);
  process.exit(2);
}

server.listen(PORT, async () => {
  console.log(`Serving ${DIST} at http://localhost:${PORT}${BASE}/  (GitHub Pages behaviour, case sensitive)`);
  if (!CHECK) return;
  const origin = `http://localhost:${PORT}`;
  const results = [];
  const check = (name, ok, detail = '') => { results.push(ok); console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? `  (${detail})` : ''}`); };
  const get = async (path) => { const r = await fetch(origin + path); return { status: r.status, type: r.headers.get('content-type') || '', text: await r.text() }; };

  const home = await get(`${BASE}/`);
  check('home page /Violence/ is 200 HTML', home.status === 200 && home.type.includes('html'));
  const assets = [...home.text.matchAll(/(?:src|href)="([^"]+)"/g)].map((m) => m[1]).filter((u) => u.startsWith(`${BASE}/`));
  check('index.html references assets under /Violence/', assets.length > 0, `${assets.length} urls`);
  check('index.html has no root-absolute asset URLs', !/(?:src|href)="\/(?:assets|favicon|manifest)/.test(home.text));
  let bad = 0;
  for (const url of assets) { const r = await fetch(origin + url); if (r.status !== 200) { bad++; console.log(`      asset ${url} -> ${r.status}`); } }
  check('every asset index.html references resolves', bad === 0, bad ? `${bad} missing` : `${assets.length} ok`);
  const manifest = await get(`${BASE}/manifest.json`);
  const parsed = manifest.status === 200 ? JSON.parse(manifest.text) : null;
  check('PWA manifest resolves, relative start_url and scope (so /Violence/)', !!parsed && parsed.start_url === './' && parsed.scope === './');
  const robots = await get(`${BASE}/robots.txt`);
  check('robots.txt disallows everything', robots.status === 200 && /Disallow:\s*\/\s*$/m.test(robots.text));
  check('index.html is noindex', /name="robots" content="noindex/.test(home.text));
  for (const model of ['beat_this_final0_q8.onnx', 'vocals_umxhq_int8.onnx']) {
    const r = await fetch(`${origin}${BASE}/models/${model}`, { method: 'HEAD' });
    check(`model ${model} is served from /Violence/models/`, r.status === 200);
  }
  const root = await get('/models/beat_this_final0_q8.onnx');
  check('a root-absolute /models/... would 404 here (why the code must use the base path)', root.status === 404);
  for (const route of ['/party/AB12CD', '/invite/AB12CD', '/settings/listen-together', '/song/abc123', '/profile/me']) {
    const r = await get(`${BASE}${route}`);
    check(`deep link ${route} gets 404.html (which redirects into the app)`, r.status === 404 && r.text.includes('violence_redirect'));
  }
  check('wrong-case /violence/ is not the site', (await get('/violence/')).status === 404);
  server.close();
  process.exit(results.every(Boolean) ? 0 : 1);
});
