# Last.fm setup (scrobbling)

Violence on Android and Android TV can scrobble what you play to Last.fm. It is optional and free. **The web player has no Last.fm integration** (verified: no web code reads a Last.fm key), so nothing is needed for the web build.

## How it fits together
- One **API account** (free) identifies the *app* to Last.fm. It gives an **API key** and a **shared secret**.
- Each **listener signs in with their own Last.fm account** inside the app (Settings > Scrobbling). Scrobbles go to *that person's* profile, signed with the shared secret.
- The app reads the key and secret at **build time** from `LASTFM_API_KEY` and `LASTFM_SECRET`. If they are missing the app still builds and runs; scrobbling then reports "Last.fm API credentials are not configured for this build" (`app/.../scrobbling/LastFM.kt`).

## 1. Create the API account (free, once)
1. Make a free Last.fm account if you do not have one (https://www.last.fm/join) and sign in.
2. Open https://www.last.fm/api/account/create.
3. Fill in the form: an application name (for example `Violence`) and a short description. Leave any callback URL empty unless the form requires one. (UNVERIFIED: the form could not be viewed without signing in, so the exact fields were not checked; it asks for no payment as far as the Last.fm API terms say.)
4. Copy the **API key** and the **Shared secret** from the page it shows. Treat the secret like a password.

## 2. Put the keys where the build reads them
- **Your own builds:** add two lines to `local.properties` in the repository root (this file is gitignored; never commit it):
  ```properties
  LASTFM_API_KEY=your_api_key
  LASTFM_SECRET=your_shared_secret
  ```
  then `.\scripts\build-active.ps1 -Target phone` (or `tv`).
- **Cloud builds (GitHub Actions):** repository **Settings > Secrets and variables > Actions > Secrets > New repository secret**, two secrets named exactly `LASTFM_API_KEY` and `LASTFM_SECRET`. `android.yml` passes them to the build through the environment; if they are not set the APKs are built without Last.fm.
- **Environment variables** `LASTFM_API_KEY` / `LASTFM_SECRET` work too (for any machine that builds).

## 3. Each person connects their own account
In the app: Settings > Scrobbling > Last.fm > sign in with their own Last.fm username and password. Friends who have no Last.fm account can skip it or use ListenBrainz (token only, no API account needed).

## Honest notes
- **The keys end up inside the APK** (BuildConfig). Anyone who has an APK can extract them. Only hand APKs to people you trust (private-repo artifacts need repository access anyway), and if a key ever leaks, create a new API account and rebuild; it costs nothing.
- **Limits.** From the Last.fm API Terms of Service (https://www.last.fm/api/tos, read 2026-11-02): use is free for **non-commercial** purposes; Last.fm "sets and enforces limits on use of the API" at its discretion and states **no numeric rate limit** in the terms; there is a "Reasonable Usage Cap" of 100 MB on stored data; responses must be cached as their HTTP headers say; Last.fm must be credited. Older Last.fm documentation mentions about 5 requests per second per IP: UNVERIFIED, not stated in the terms.
- **15 users scrobbling to their own accounts** is tiny: each listener causes about one "now playing" update and one scrobble per song (roughly two requests every 3 to 4 minutes per active listener). Fifteen people listening at the same moment is on the order of 0.1 to 0.2 requests per second in total, far below any limit mentioned anywhere. This is an estimate, not a measured figure. Because every request is signed with the same app key, Last.fm sees one small app, not 15.
- **Terms:** a private group using the app for personal listening is non-commercial. Last.fm's terms say use outside the EEA is allowed only where users agreed to it when they registered (clause 2.3); that is between each person and Last.fm.
