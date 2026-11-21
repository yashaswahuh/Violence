import { describe, expect, it } from 'vitest';
import { androidIntentUrl, appLink, isAndroid } from './openApp';

describe('opening a shared link in the app or the browser', () => {
  it('turns a violence:// link into an intent that falls back to the browser', () => {
    const url = androidIntentUrl('violence://song/yt/dQw4w9WgXcQ?v=1&t=Song', 'https://music.youtube.com/watch?v=dQw4w9WgXcQ');
    expect(url).toBe(
      'intent://song/yt/dQw4w9WgXcQ?v=1&t=Song#Intent;scheme=violence;S.browser_fallback_url=' +
        encodeURIComponent('https://music.youtube.com/watch?v=dQw4w9WgXcQ') +
        ';end',
    );
    expect(url.startsWith('intent://')).toBe(true);
  });

  it('keeps the fallback address whole, whatever characters it has', () => {
    const fallback = 'https://music.youtube.com/search?q=a b&c=d;e#f';
    const url = androidIntentUrl('violence://song/q/search?v=1&t=A', fallback);
    // ; and # inside the fallback would end the intent early if they were not encoded.
    const encoded = (url.split('S.browser_fallback_url=')[1] ?? '').replace(/;end$/, '');
    expect(decodeURIComponent(encoded)).toBe(fallback);
    expect(encoded).not.toMatch(/[;#]/);
  });

  it('leaves a link that is not a violence:// one alone', () => {
    expect(androidIntentUrl('https://example.com/x', 'https://music.youtube.com/')).toBe('https://example.com/x');
  });

  it('gives Android the intent and everyone else the plain app link', () => {
    const app = 'violence://song/yt/dQw4w9WgXcQ?v=1';
    const browser = 'https://music.youtube.com/watch?v=dQw4w9WgXcQ';
    expect(appLink(app, browser, true).startsWith('intent://')).toBe(true);
    expect(appLink(app, browser, false)).toBe(app);
  });

  it('knows an Android browser from a desktop one', () => {
    expect(isAndroid('Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36')).toBe(true);
    expect(isAndroid('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36')).toBe(false);
    expect(isAndroid('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148')).toBe(false);
  });
});
