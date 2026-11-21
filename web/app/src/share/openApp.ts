/**
 * Opening a shared link in the Violence app when there is one, and in the browser when there is not.
 *
 * On Android, Chrome and most browsers read an `intent://` address: the app that handles the `violence://`
 * scheme opens if one is installed, and if none is, the browser goes to `S.browser_fallback_url` instead.
 * That is the whole of "if the app is there open it, if not open the browser", and it is done by the
 * browser, so nothing here has to guess whether the app exists.
 */

/** Whether this is an Android device, the only place the app exists. */
export function isAndroid(userAgent: string = typeof navigator === 'undefined' ? '' : navigator.userAgent): boolean {
  return /Android/i.test(userAgent);
}

/**
 * `violence://song/yt/ID?v=1` as an Android intent address that opens the app, or [fallbackUrl] when there is
 * none. A link that is not a `violence://` one comes back as it was.
 */
export function androidIntentUrl(customUri: string, fallbackUrl: string): string {
  const prefix = 'violence://';
  if (!customUri.startsWith(prefix)) return customUri;
  const rest = customUri.slice(prefix.length);
  return `intent://${rest}#Intent;scheme=violence;S.browser_fallback_url=${encodeURIComponent(fallbackUrl)};end`;
}

/** The address a button should point at: the smart intent address on Android, the plain app link elsewhere. */
export function appLink(customUri: string, browserUrl: string, android: boolean = isAndroid()): string {
  return android ? androidIntentUrl(customUri, browserUrl) : customUri;
}

/**
 * Tries the app once for this link in this tab. Returns true when it tried, so the page can leave itself alone
 * the next time (back from the app, a reload) instead of sending the person round again.
 */
export function tryAppOnce(key: string, target: string, android: boolean = isAndroid()): boolean {
  if (!android || typeof window === 'undefined') return false;
  const flag = `violence.openApp.${key}`;
  try {
    if (window.sessionStorage.getItem(flag) === '1') return false;
    window.sessionStorage.setItem(flag, '1');
  } catch {
    // Storage is blocked: do not redirect at all, a button is still there.
    return false;
  }
  window.location.replace(target);
  return true;
}
