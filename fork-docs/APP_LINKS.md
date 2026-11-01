# Android App Links (invite links that open the app directly)

Written in Phase 7 (2026-11-02). **Status:** the app side is done and built; the **website side is yours to publish** (below), and none of it has been tested on a phone yet (it cannot be tested from the build machine).

## What it gives you
A Jam invite link such as `https://yashaswahuh.is-a.dev/Violence/party/AB12CD`, tapped in a chat app, opens **Violence directly** and joins the party, with no browser in between. This is "Android App Links" (verified web links).

Until the website side is published, **nothing breaks**: the link opens the web landing page (`/Violence/party/AB12CD`, built in Phase 5), which has an **Open in Violence app** button (it uses the `violence://party/AB12CD` link) and a "Join in this browser" button. On Android 11 and older a not-yet-verified link may instead show an "Open with" chooser that lists Violence. The old custom-scheme links `violence://party/<CODE>` are unchanged and always work.

## What is in the app
Both manifests (`app/src/main/AndroidManifest.xml`, `tv/src/main/AndroidManifest.xml`) carry one filter with `android:autoVerify="true"`:

| Part | Value |
|---|---|
| scheme | `https` |
| host | `yashaswahuh.is-a.dev` |
| path prefixes | `/Violence/party/`, `/Violence/invite/`, `/Violence/song/`, `/Violence/profile` (the last two added in Phase 9) |

**The folder is `/Violence/` with a capital V.** The Phase 7 brief wrote `/violence/party/`; GitHub Pages serves the project under the repository's exact name and Android path prefixes are case sensitive, so the prefix has to match what the link really contains. The link builder (`JamInviteLink`) produces `.../Violence/party/<CODE>`, and the reader accepts any base path.

It is built to grow: later features (a shared song, a profile) are one more line each in **both** manifests, for example `<data android:pathPrefix="/Violence/song/" />`, plus the page for it on the website. **Phase 9 (sharing) added `/Violence/song/` and `/Violence/profile`** (a shared song and the https form of the profile card, `SHARE_DESIGN.md`) to both manifests, and the web pages for them exist (`/song/<source>/<id>`, `/profile`). `/Violence/profile` has no trailing slash so it matches `/Violence/profile?v=1&...`. **Nothing for you to do in `assetlinks.json`**: it covers the whole domain. These prefixes verify the same way as the party ones; until the file is published the links open the web landing page, whose **Open in Violence app** button opens the app. **No change to assetlinks.json is needed** for new prefixes (it covers the whole domain).

Verification covers the **release** apps only: `com.yashaswahuh.violencemusic` (phone) and `com.yashaswahuh.violencemusic.tv` (TV), signed with your release key. The `.dev` flavors and debug/CI builds are signed differently and are simply not verified (their links fall back to the landing page).

## What YOU publish: assetlinks.json
Android checks `https://yashaswahuh.is-a.dev/.well-known/assetlinks.json` when the app is installed. It must be at the **root of the domain**.

**Where that is.** The Violence site is a *project* Pages site, served under `/Violence/`, so it cannot place a file at the domain root. The root belongs to your **user Pages site**: the repository named `yashaswahuh.github.io`, *if* `yashaswahuh.is-a.dev` is its custom domain. **UNCLEAR / owner to verify:** open `https://yashaswahuh.is-a.dev/` and Settings > Pages of `yashaswahuh.github.io`. If the domain is not set up that way, the root cannot be controlled from the Violence repository and App Links cannot be verified; the landing-page fallback stays.

In that user-site repository add two things at the top level:
1. An empty file named `.nojekyll` (otherwise GitHub's Jekyll step silently drops folders that start with a dot, including `.well-known`).
2. `.well-known/assetlinks.json` with this content (replace the fingerprint placeholder, see next section):

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.yashaswahuh.violencemusic",
      "sha256_cert_fingerprints": ["AA:BB:...:FF (64 hex digits, 32 pairs, from your release key)"]
    }
  },
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.yashaswahuh.violencemusic.tv",
      "sha256_cert_fingerprints": ["AA:BB:...:FF (the same value)"]
    }
  }
]
```
Both apps use the **same** release key, so the same fingerprint appears twice. (The TV entry is optional: a TV rarely opens web links. It does no harm.)

Requirements Android is strict about: served over HTTPS, status 200 (no redirect), valid JSON, content type `application/json` (GitHub Pages serves `.json` correctly).

## Getting the SHA-256 fingerprint of the release key
Any one of these (PowerShell). Use the **release** keystore, never a debug one.
```powershell
# 1. From the keystore (asks for its password; nothing is stored):
keytool -list -v -keystore "$env:USERPROFILE\violence-keys\violence-release.jks" -alias violence | Select-String 'SHA256'

# 2. From a built APK (no password needed): scripts\release.ps1 also prints it as its last "certificate" line
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<version>\apksigner.bat" verify --print-certs .\release-out\<version>\violence-phone-<version>.apk
```
assetlinks.json wants the fingerprint as upper-case hex pairs separated by colons (`AA:BB:CC:...`). `keytool` prints it that way; `apksigner` prints plain lowercase hex, so add the colons and upper-case it, or copy the line `scripts\release.ps1` prints, which is already in that form.

## Check that it worked
1. Open `https://yashaswahuh.is-a.dev/.well-known/assetlinks.json` in a browser: it must show the JSON, not a 404 page.
2. Google's checker (public, free): open
   `https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://yashaswahuh.is-a.dev&relation=delegate_permission/common.handle_all_urls`
   and see your two statements.
3. On a phone with USB debugging, **after installing the release APK** (verification runs at install time, and again after `--re-verify`):
   ```powershell
   adb shell pm verify-app-links --re-verify com.yashaswahuh.violencemusic
   adb shell pm get-app-links com.yashaswahuh.violencemusic
   ```
   `yashaswahuh.is-a.dev: verified` is the goal (Android 12 and newer; older versions verify on their own and show nothing in this command).
4. Tap a real invite link in a chat app: Violence opens on the Join screen with the code filled in.

If it says `none`/`1024` (failed): the file is missing or in the wrong place, the fingerprint differs from the key that signed the installed APK (a debug or CI build!), or the file redirects. Fix the file and run `--re-verify` again.

Users can also turn it on by hand: Settings > Apps > Violence > Open by default > Add link > tick `yashaswahuh.is-a.dev`.

## Notes
- The fingerprint belongs to the **key**, not to a version. It stays valid for every update, as long as updates are signed with the same keystore (the same rule that lets updates install).
- If you ever change the signing key, you cannot update existing installs anyway (see `RELEASE_GUIDE.md`); update the fingerprint in assetlinks.json at the same time.
- Verification only proves the domain is yours. It does not make the invite link private: anyone with the link can open the landing page.
