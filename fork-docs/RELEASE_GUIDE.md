# Releasing Violence (signing, building, publishing updates, installing)

Written in Phase 7 (2026-11-02). It has two parts: **A. for the owner** (make a release and publish it) and **B. for friends and family** (install it). Part B is written to be copied and sent to someone non-technical.

Everything here is free: Windows, JDK, the Android SDK, GitHub (private repo + Pages + Releases), no store account, no card. **Status:** the build and signing steps were run with a throwaway test key (see the Phase 7 report in `PHASES.md`); the publish step (GitHub release, Pages deploy) and installing on a real phone/TV have **not** been run.

---

# A. For the owner

## A1. One time: create your release key
Android only installs an update when it is signed with **the same key** as the installed app. The key lives in a file called a keystore. Make it once, keep it **outside this repository**.

PowerShell (one line; `keytool` ships with the JDK, and `scripts\_env.ps1` finds it, or use the full path of your JDK 17 `bin\keytool.exe`). It asks for a password (twice) and some name fields (any answer, even blanks, is fine):
```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\violence-keys"; keytool -genkeypair -v -keystore "$env:USERPROFILE\violence-keys\violence-release.jks" -alias violence -keyalg RSA -keysize 2048 -validity 10000
```
> **BACK IT UP NOW.** Copy `violence-release.jks` **and** write down the password in at least two places that are not this computer and not GitHub (a USB stick, a cloud drive that is private to you, a password manager). If you lose the file or the password, **nobody can ever update again**: every phone has to uninstall Violence (losing downloads and settings) and install a new build signed with a new key. The key cannot be recovered. Do not put it in the repository, in a chat, or in an email to a friend.

Then tell the build where it is. Copy `keystore.properties.example` to `keystore.properties` (same folder, it is gitignored) and fill it in:
```properties
storeFile=C:/Users/<you>/violence-keys/violence-release.jks
storePassword=<the password>
keyAlias=violence
keyPassword=<the same password>
```
Without this file (or if the keystore is missing) a release build comes out **unsigned** and `scripts\release.ps1` refuses to run. A repository checkout never contains the key.

## A2. One time: tell the builds where updates live
Add to `local.properties` (repository root, gitignored; one value per line). All of these are public addresses, not secrets, except the Last.fm pair:
```properties
UPDATE_INFO_URL=https://yashaswahuh.is-a.dev/Violence/updates/update.json
JAM_BACKEND_URL=<your Jam server, see JAM_BACKEND_DEPLOY.md>
JAM_INVITE_BASE_URL=https://yashaswahuh.is-a.dev/Violence
LASTFM_API_KEY=...        # optional, see LASTFM_SETUP.md
LASTFM_SECRET=...
```
`UPDATE_INFO_URL` is what switches the in-app update check on. **If it is empty the app never checks and makes no network call for it.** (If your site is not at `yashaswahuh.is-a.dev`, use the address from the Web Deploy run summary, see `CI_DEPLOY.md`.)

And on GitHub (once), in addition to the settings in `CI_DEPLOY.md`:
- **Settings > Environments > github-pages > Deployment branches and tags**: besides `violence-nk`, add the tag pattern **`v*`** (the release publishing triggers the site deploy from the tag).
- Optional repository variable `UPDATE_FOLDER` (Settings > Secrets and variables > Actions > Variables). Default `updates`. The files are public to anyone who knows the address; a hard-to-guess folder name such as `updates-7f3k9x2q` keeps them out of casual reach. If you set it, change the folder in `UPDATE_INFO_URL` to match.

## A3. Cut a release (every time)
In PowerShell, in the repository folder:
```powershell
git switch violence-nk
.\scripts\release.ps1 -Bump -Changelog 'What changed, line one', 'line two'
```
What it does: checks the keystore, adds 1 to the version (below), builds the **phone** and **TV** release APKs (minified with R8, signed with your key; Wear OS and desktop are not built; **expect 30 to 60 minutes**, R8 is slow), checks every signature, and writes into `release-out\<version>\`:
- `violence-phone-<version>.apk` and `violence-tv-<version>.apk`: the **universal** files that work on any device (about 183 MB and 170 MB). These are what you hand to people.
- `violence-phone-<version>-arm64-v8a.apk`, `-armeabi-v7a.apk`, `-x86_64.apk`, and the same three for the TV (65 to 90 MB each): what the **in-app update** downloads, so each device fetches only about 80 MB. The app picks the file for its own CPU.
- `update.json` (what the apps read: versionCode, versionName, your changelog, and each per-CPU APK's name and SHA-256)

It then prints the publish steps for you. Nothing is committed, pushed or uploaded by the script. In short:
1. Commit the version bump and push: `git add build.gradle.kts; git commit -m "Release <version>"; git branch --show-current; git push origin violence-nk` (the middle command must print `violence-nk`).
2. GitHub > the repository > **Releases > Draft a new release**. Tag `v<version>` (create it on `violence-nk`), title `Violence <version>`. Drag in **all** the files from `release-out\<version>\` (8 APKs and `update.json`; the universal ones stay on the release page, only the per-CPU ones and `update.json` go to the website). Leave **"Set as a pre-release" unticked**. **Publish release.**
3. Publishing starts the **Web Deploy** workflow, which copies `update.json` and the per-CPU APKs it lists into the website folder `updates/`. About two minutes later `https://yashaswahuh.is-a.dev/Violence/updates/update.json` shows the new version.
4. Check it: open that address; `versionCode` must be the new number. Phones and TVs pick it up on their next launch, at most once a day.

Same thing from a terminal, if `gh` is logged in: `release.ps1` prints the exact `gh release create ...` command.

### The very first release
Nothing is installed anywhere yet, so no bump is needed; the version in `build.gradle.kts` (currently **1.9.0-violence.1**, versionCode 35) is the first:
```powershell
git switch violence-nk
.\scripts\release.ps1 -Changelog 'First Violence release'
```
and then steps 2-4 above (the tag is `v1.9.0-violence.1`). Install it on your own phone first (B1) and try the main things before sending it to anyone. There is no older version to update *from*, so the update check can only be tested once a second release exists: make a tiny second release (`-Bump`) and open the first install.

### Versions
One place: `extra["violenceVersionCode"]` and `extra["violenceVersionName"]` in the root `build.gradle.kts` (the phone and the TV both read it).
- **versionName** is `<upstream version>-violence.<N>`, for example `1.9.0-violence.2`. `-Bump` adds 1 to `N`. After merging a new upstream BitChord version, set the new base by hand: `.\scripts\release.ps1 -VersionName 1.10.0-violence.1` (this resets `N` and still adds 1 to the versionCode).
- **versionCode** is a whole number that must go up with every release (Android refuses an update with a lower or equal number). `-Bump` and `-VersionName` add 1.
- The update check compares **versionCode only**, never the name.

## A4. How the update works for a user
- On launch (after the first-run setup), the app asks `UPDATE_INFO_URL` for `update.json`, **at most once every 24 hours**. A failed check (offline) is retried at the next launch and never shows an error.
- If `versionCode` is higher than the installed one, a dialog shows the version and your changelog. **Download** fetches the APK for this app (phone or TV), checks it against the SHA-256 from `update.json` (a mismatch is discarded), and **Install** opens Android's installer.
- The first time, Android asks to allow installs from Violence ("Install unknown apps"). Allow it, go back, tap Install again.
- Phone: the dialog also has a link to the release page. TV: the same, with a QR code to scan if there is no APK for the TV.
- Nothing about the user is sent: the check is a plain HTTPS download of one small file. There is no crash-reporting or analytics service in the app.

## A5. Limits of the free host (is it enough for 15 people?)
Update files are served from **GitHub Pages**, built from the private repository, as part of the existing website deploy.
- A Pages site may be up to **1 GB**. The updates folder holds the six per-CPU APKs of one release: measured with the first test build, 65 to 90 MB each, about 460 MB in total, so it fits but is not far off. (The two universal APKs, 183 and 170 MB, are deliberately **not** put on the site; they stay on the GitHub release page, whose files may be up to 2 GB.) Each deploy replaces the previous site, so old releases do not pile up (they stay in the Releases list). If the app grows past roughly 150 MB per CPU the site would no longer fit: then drop `x86_64` (emulators and a few Chromebooks only).
- Bandwidth: GitHub documents a soft limit of **100 GB per month** for Pages. One phone update downloads one per-CPU APK, about 80 MB. 15 people use about 1.2 GB per release, so 80 or more releases a month fit. An estimate, not a measured number.
- **Per-file size limit for an Actions-deployed Pages site: UNVERIFIED.** Git itself rejects files over 100 MB; every per-CPU file is under that, `release.ps1` warns above 95 MB.
- Anyone who knows the address can download the APK (a Pages site is public even when the repository is private). It contains your Jam server address and Last.fm API key, like any build. Use the optional hard-to-guess `UPDATE_FOLDER`.
- Why not GitHub Releases directly? In a private repo a release download needs a GitHub login, which friends do not have and the app must not ask for. Why not Cloudflare R2 or Firebase? They want a card on file or have tighter free limits; Cloudflare Pages has a 25 MiB per-file limit, smaller than the APK.

## A6. Other build variants
- **Dev flavor** (installs beside the real app as `com.yashaswahuh.violencemusic.dev`): `.\gradlew.bat ':app:assembleDevRelease' '-Pviolence.bundleWear=false'` (output `app\build\outputs\apk\dev\release`). Signed with the release key when `keystore.properties` exists. Not needed for normal releases.
- **Debug builds** (`.\scripts\build-active.ps1`, and the Android CI workflow) are signed with a *debug* key; they cannot be updated by a release APK or the other way round. Uninstall one before installing the other.
- R8/minify: release builds are minified and resource-shrunk; the keep rules in `app/proguard-rules.pro` cover Ktor and the `@Serializable` classes used by Jam and Last.fm, the playback libraries, search and lyrics code, and the YouTube Music / Spotify login code. Launch a release build and try each (Jam, Last.fm, playback, search, lyrics, a download, both logins) before sending it out; this has **not** been done on a device.

---

# B. For friends and family: installing Violence

Send this part (or the sections you need). You will be sent a file ending in `.apk`.

## B1. Android phone
1. Get the file `violence-phone-<version>.apk` (the one without a CPU name in it) onto the phone (download link, messaging app, USB, or a file-sharing app).
2. Open it from the **Downloads** folder or the **Files** app.
3. Android says it cannot install from this source: tap **Settings**, switch on **Allow from this source** for the app you opened the file with (Chrome or Files), go **Back**, and tap **Install**.
4. If **Play Protect** says "unrecognised app", tap **More details > Install anyway**. It says this about every app that is not from the Play Store.
5. Tap **Open**. Sign in to YouTube Music when asked (this is your own account; it stays on your phone).

**Updating later:** the app tells you when a new version exists (a window with what changed). Tap **Download**, then **Install**. Your downloads and settings are kept. If you ever get "App not installed", you have an older test build from another source: uninstall it first, then install again.

## B2. Android TV / Google TV
The TV file is a different one: `violence-tv-<version>.apk` (again the one without a CPU name). The phone file does not work on a TV. The TV has no browser to download from, so use one of these:

**Easiest: a file-sharing app.** On the TV install the free app **Send Files to TV** (Play Store on the TV) and the same app on your phone; send the `.apk` from the phone to the TV; open it on the TV. Or install the free app **Downloader** on the TV and type the address of the `.apk` file.

**First time only:** the TV will say it cannot install from this source. Open the TV's **Settings > Apps > Security & restrictions > Unknown sources** (menu names differ: on Google TV, the prompt that appears links you straight to the switch) and allow it for the app you used (Downloader, Send Files, or a file manager), then install again.

**With a computer (adb), for the person who sets TVs up:**
1. On the TV: Settings > About > press **Build** 7 times (turns on developer options). Then Settings > Developer options > turn on **USB debugging** / **Network debugging** and note the TV's IP address (Settings > Network).
2. On the computer (Android platform-tools, free: https://developer.android.com/tools/releases/platform-tools):
   ```powershell
   adb connect <tv-ip>:5555
   adb install violence-tv-<version>.apk
   ```
   To update an already-installed TV app in place: `adb install -r violence-tv-<version>.apk`.

The app appears in the TV's apps row as **Violence**. Update later from **Settings > Software Update** inside the app, or it offers it by itself.

## B3. If something goes wrong
| What you see | What to do |
|---|---|
| "App not installed" / "conflicts with an existing package" | An older test copy is installed. Uninstall Violence (or Violence TV), then install the new file. (Your downloaded songs are inside the app and will be gone; ask first if you have many.) |
| "Parse error" / "There was a problem parsing the package" | The download is incomplete or it is the wrong file (phone file on a TV, or the opposite). Download again. |
| The install button is greyed out | Switch on **Allow from this source** for the app you opened the file with (B1 step 3). |
| Play Protect warning | More details > Install anyway (B1 step 4). |
| The update window never appears | It checks once a day when the app starts. Close the app fully and open it again tomorrow; or install the new file by hand. |
