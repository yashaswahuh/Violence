# Rebrand & Asset Migration Checklist

**Audit Date:** 2026-10-29  
**Upstream Name:** BitChord  
**Fork Brand Name:** **Violence**  
**Package Namespace:** `com.yashaswahuh.violencemusic`  
**Deep Link Scheme:** `violence://`  
**Web Player Host:** `https://yashaswahuh.is-a.dev/violence`  
**Legal Framework:** GNU General Public License v3.0 (GPL-3.0).

---

## 1. Legal & License Compliance Framework (GPL-3.0)

When forking and rebranding a GPL-3.0 project, specific legal obligations must be respected:

1. **GPL-3.0 Remains in Place:** The fork is a derivative work and must continue to be licensed under the GNU General Public License v3.0. The `LICENSE` file in the root directory must be retained.
2. **Preserve Original Copyright Notices:** Do NOT delete original copyright notices in source files (e.g. `/* Copyright (C) 2026 Kushagra Singh */` or headers mentioning `Orchard`, `Metrolist`, `NewPipe`, `Kira`). You may add your own copyright notice alongside them for new or modified files.
3. **Do Not Impersonate Upstream:** The original name "BitChord", the official logos, and the original author's name/handles must **not** be used as the brand of the fork. Rebranding protects the original author from confusion or unauthorized association.
4. **No Commercial Warranty:** All disclaimer clauses regarding YouTube/Google fair use, non-affiliation, and "AS IS" warranties must be preserved.

---

## 2. Asset Catalog (Images, Icons & Splash Assets)

All upstream visual assets must be replaced with new fork-specific graphics.

| Asset File Path | Format / Size | Upstream Usage | Replacement Action |
|---|---|---|---|
| `/Banner.png` | PNG (1419 KB, 100% width) | Root GitHub README banner | Replace with new Violence banner or remove. |
| `/Logo.png` | PNG (145 KB, 200px width) | Root GitHub README logo icon | Replace with new Violence logo icon. |
| `/upi_support.jpg` | JPEG (36 KB) | UPI payment QR code in README | **Delete** from repository completely. |
| `app/src/main/res/mipmap-hdpi/ic_launcher*.png` | PNG | Android phone launcher icon | Generate new adaptive app icon set for Violence. |
| `app/src/main/res/mipmap-mdpi/ic_launcher*.png` | PNG | Android phone launcher icon | Generate new adaptive app icon set for Violence. |
| `app/src/main/res/mipmap-xhdpi/ic_launcher*.png` | PNG | Android phone launcher icon | Generate new adaptive app icon set for Violence. |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher*.png` | PNG | Android phone launcher icon | Generate new adaptive app icon set for Violence. |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher*.png`| PNG | Android phone launcher icon | Generate new adaptive app icon set for Violence. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Vector Drawable | Android 8+ Adaptive icon foreground | Replace with new Violence vector glyph. |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Vector Drawable | Android 8+ Adaptive icon background | Replace with new background color/shape. |
| `app/src/main/res/drawable/ic_notification_logo.xml` | Vector Drawable | Small status bar media notification logo | Replace with new silhouette glyph. |
| `tv/src/main/res/drawable/tv_banner.png` | PNG (320×180 px) | Android TV Leanback home launcher banner | Replace with new TV banner featuring "Violence TV". |
| `tv/src/main/res/drawable-*/tv_banner.png` | PNG (hdpi, mdpi, xhdpi) | Android TV scaled banners | Replace with new TV banners across densities. |
| `web/app/public/favicon.svg` | SVG Vector | Web browser tab icon & extension icon source | Replace with new Violence vector logo. |
| `web/app/public/apple-touch-icon.png` | PNG | Web iOS home screen icon | Replace with new Violence icon. |
| `web/extension/public/icon/icon-16.png` | PNG (16×16 px) | Browser extension toolbar icon | Re-render from new `favicon.svg`. |
| `web/extension/public/icon/icon-32.png` | PNG (32×32 px) | Browser extension toolbar icon | Re-render from new `favicon.svg`. |
| `web/extension/public/icon/icon-48.png` | PNG (48×48 px) | Browser extension toolbar icon | Re-render from new `favicon.svg`. |
| `web/extension/public/icon/icon-128.png` | PNG (128×128 px) | Browser extension management icon | Re-render from new `favicon.svg`. |

---

## 3. Package Identifiers, Namespaces & Schemes

| Component / Target | Current Upstream ID | Target Fork Replacement |
|---|---|---|
| **Android Phone Prod ID** | `com.music.violence` | `com.yashaswahuh.violencemusic` (in `app/build.gradle.kts:65`) |
| **Android Phone Dev ID** | `com.dev.bitchord` | `com.yashaswahuh.violencemusic.dev` (in `app/build.gradle.kts:118`) |
| **Android TV Prod ID** | `com.music.violence.tv` | `com.yashaswahuh.violencemusic.tv` (in `tv/build.gradle.kts:80`) |
| **Android TV Dev ID** | `com.dev.bitchord.tv` | `com.yashaswahuh.violencemusic.tv.dev` (in `tv/build.gradle.kts:150`) |
| **Application Namespace** | `namespace = "com.music.violence"` | `namespace = "com.yashaswahuh.violencemusic"` (in `app`, `tv`) |
| **Shared Namespace** | `namespace = "com.music.violence.shared"` | `namespace = "com.yashaswahuh.violencemusic.shared"` (in `shared`) |
| **SharedUi Namespace** | `com.music.violence.sharedui` | `com.yashaswahuh.violencemusic.sharedui` (in `sharedUi`) |
| **Custom URI Scheme** | `bitchord://` | `violence://` (in `AndroidManifest.xml`, `backend/main.go`, `invite.ts`) |
| **Intent Filter Host** | `bitchord.kushagrasingh.in` | `yashaswahuh.is-a.dev` |
| **Web Extension ID** | `extension@bitchord.kushagrasingh.in` | `extension@yashaswahuh.is-a.dev` (in `web/extension/wxt.config.ts:61`) |
| **File Provider Authority** | `com.music.bitchord.provider` | `com.yashaswahuh.violencemusic.provider` (in `app/src/main/AndroidManifest.xml`) |
| **Go Backend Module** | `github.com/KabirSinghBhatia/BitChord/backend` | `github.com/yashaswahuh/Violence/backend` (in `backend/go.mod`) |

---

## 4. Source Code & Configuration Occurrences of "BitChord"

### 4.1 Strings & UI Resources
- `app/src/main/res/values/strings.xml`:
  - `app_name`: `"BitChord"` ➔ Change to `"Violence"`
  - `bitchord_music`: `"BitChord Music"` ➔ Change to `"Violence Music"`
  - `about_bitchord`: `"About BitChord"` ➔ Change to `"About Violence"`
  - `listening_to_bitchord`: `"Listening to BitChord"` ➔ Change to `"Listening to Violence"`
- `app/src/main/res/values-*/strings.xml`:
  - Update `app_name` across all 30 localization resource folders (e.g. `values-es`, `values-de`, `values-fr`, `values-zh`, etc.) to `"Violence"`.
- `tv/build.gradle.kts:151,155`:
  - `resValue("string", "app_name", "BitChord TV")` ➔ Change to `"Violence TV"`
- `tv/src/main/java/com/music/violence/ui/tv/dialogs/TvDialogs.kt:1213`:
  - `TvDialog(title = "BitChord", ...)` ➔ Change to `"Violence"`
- `web/app/index.html`:
  - `<title>BitChord</title>` ➔ Change to `<title>Violence</title>`
- `web/app/src/components/Sidebar.tsx` & headers:
  - Text occurrences of "BitChord" ➔ Replace with `"Violence"`.
- `backend/main.go:80, 253, 606`:
  - Log strings and title `"BitChord Listen Together"` ➔ Change to `"Violence Listen Together"`.

### 4.2 Application Code & Headers
- `app/src/main/java/com/music/violence/ViolenceApplication.kt`:
  - Rename class to `ViolenceApplication.kt` (update manifest `android:name`).
  - User-Agent header string: `"BitChord/${BuildConfig.VERSION_NAME}..."` ➔ `"Violence/${BuildConfig.VERSION_NAME}..."`.
- `app/src/main/java/com/music/violence/data/scrobbling/LastFM.kt:89`:
  - `headers.append(HttpHeaders.UserAgent, "BitChord (...)")` ➔ `"Violence (https://github.com/yashaswahuh/Violence)"`.
- `app/src/main/java/com/music/violence/data/scrobbling/ListenBrainzManager.kt:33,83`:
  - `"submission_client":"BitChord"` ➔ `"submission_client":"Violence"`
- `app/src/main/java/com/music/violence/data/discord/DiscordRPC.kt:24,67`:
  - Presence activity name `"Listening to BitChord"` ➔ `"Listening to Violence"`.
- `app/src/main/java/com/music/violence/ui/screens/SettingsSheet.kt:1560-1585`:
  - Remove personal colophon links:
    - GitHub: `https://github.com/kushagrasinghx/BitChord`
    - Developer: `https://github.com/kushagrasinghx`
    - Discord: `https://discord.gg/pDdKfrdHY6`
    - Website: `https://bitchord.kushagrasingh.in/`
  - Replace with link to private fork repository (`https://github.com/yashaswahuh/Violence`).

### 4.3 Documentation & Workflows
- `README.md`:
  - Strip author badges: Trendshift, FMHY, Shields live stats badge, Release downloads.
  - Remove Support section (Ko-fi, PayPal, UPI QR code).
  - Update title to `Violence` and document private fork purpose.
- `.github/FUNDING.yml`: Delete file.
- `.github/workflows/deploy-backend.yml`: Remove upstream deployment action.
- `.github/workflows/close-issue.yml`: Remove upstream author logins (`kushagrasinghx`, `Galaxyyss`).
- `.github/workflows/update-contributors.yml` & `contributors-image.yml`: Remove or disable.

---

## 5. Step-by-Step Rebrand Execution Plan

To avoid compilation breakages, execute the rebranding in this strict order during Phase 2:

1. **Phase A: Delete Personal Links & Donation Assets**
   - Delete `upi_support.jpg`.
   - Delete `.github/FUNDING.yml`.
   - Clean `README.md` and `SettingsSheet.kt` footer.
2. **Phase B: Replace Visual Brand Assets**
   - Place new app icon vector in `ic_launcher_foreground.xml` and `ic_notification_logo.xml`.
   - Replace `tv_banner.png` in `tv/src/main/res/`.
   - Replace `favicon.svg` in `web/app/public/`.
3. **Phase C: String & Label Renaming**
   - Update `app_name` across all `values*/strings.xml`.
   - Update `web/app/index.html` title and web components.
   - Update `submission_client` and `User-Agent` strings.
4. **Phase D: Package & Namespace Migration**
   - Rename `applicationId` in `app/build.gradle.kts` to `com.yashaswahuh.violencemusic` and `tv/build.gradle.kts` to `com.yashaswahuh.violencemusic.tv`.
   - Update deep link schemes from `bitchord://` to `violence://`.
   - Update URI intent filters and host definitions to `yashaswahuh.is-a.dev`.
   - Refactor package folders from `com/music/violence` to `com/yashaswahuh/violencemusic`.
5. **Phase E: Verification**
   - Run `./gradlew assembleDevDebug` and `./gradlew :tv:assembleDevDebug` to confirm clean builds.
