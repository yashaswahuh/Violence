# Sharing songs, profiles and Jam invites: NFC, QR and links (Phase 9 design)

Written 2026-11-02, **before any Phase 9 code**. Companion to `PROFILE_DESIGN.md` (the profile card), `JAM_DESIGN.md` (party invites), `APP_LINKS.md` (verified web links). Nothing here has been run on a real phone: see section 15 for what is UNVERIFIED.

## 1. Goal and scope
Tap two Android phones together to hand over **a song, a profile card, or a Jam invite**, with **QR codes and links** for phones without NFC, for the TV and for the web. No server, no account, no analytics. Every incoming link is untrusted and **never acts on its own**: the user always confirms.

| Platform | Send | Receive |
|---|---|---|
| Phone | NFC tap (HCE tag emulation), QR, copy link, system share sheet | NFC reader mode screen, NFC system dispatch (app closed), QR scan (camera or a picture), pasted link, deep link / App Link |
| TV | QR and link text (no NFC) | deep links (`adb`, links opened on the TV) with the same confirmation |
| Web | copy link, QR | landing pages `/song/...`, `/profile`, `/party/...` ("Open in Violence app"), paste a profile link (Phase 8) |

**Differences from the brief:** the brief's example web paths are lower-case `/violence/...`; the real folder is `/Violence/` (capital V, case-sensitive Pages and Android path matching, see `APP_LINKS.md`). Spotify tracks in this app are always matched to a YouTube video id (there is no separate Spotify playback id), so the song source ids are `yt` (the real playable id), `q` (search by title and artist, the fallback) and `sp` (accepted on input only; see section 2.1).

## 2. Payload formats (all small, versioned, strict)

### 2.1 Song
```
violence://song/<source>/<id>?v=1&t=<title>&a=<artist>
https://yashaswahuh.is-a.dev/Violence/song/<source>/<id>?v=1&t=<title>&a=<artist>
```
| Part | Rule |
|---|---|
| `<source>` | `yt`: YouTube video id, exactly **11** characters from `A-Za-z0-9_-`. `q`: id is the literal `search`; **`t` required** (the receiver searches "title artist" and offers the first song result). `sp`: Spotify track id, exactly **22** base62 characters `A-Za-z0-9`; **`t` required**, because the app has no way to play a Spotify id: it only searches by title and artist. The builder emits `yt` (a Spotify-library song already has a matched video id) and `q` (when there is none); it never emits `sp`. |
| `v` | exactly `1` (missing is an error, anything else is `unsupported version`) |
| `t`, `a` | optional display hints (`t` required for `q` and `sp`). Percent-encoded UTF-8. After decoding: not empty, at most **120** (`t`) / **80** (`a`) code points, no control characters, no invisible/format characters (the same character rules as profile names, `ProfileRules`), whitespace collapsed. **Parsing rejects a hint that is too long; it never truncates.** The builder truncates, at a code-point boundary, so the `violence://` form is **at most 200 bytes** (artist is cut first, then title). |
| Anything else | other query keys, duplicate keys, a missing `v`, a fragment-only trick, more than 512 characters in total: rejected. `+` reads as a space, `%XX` must be valid UTF-8. |

The hints are **display text only**. They are shown as plain text in the confirmation and used for a `q`/`sp` search; a `yt` song is always resolved from its id, so the title in a link can never change what plays.

Size: the fixed part of a `yt` link is 34 bytes (`violence://song/yt/<11>?v=1`), leaving about 160 bytes for hints. Latin text: about 50 characters of title plus artist after `&t=`/`&a=`; CJK is 9 encoded bytes per character, so about 17.

### 2.2 Profile
The Phase 8 card, unchanged: `violence://profile?v=1&id=<uuid>&n=<name>&a=<avatarId>&c=<hex6>` and `https://yashaswahuh.is-a.dev/Violence/profile?...` (`PROFILE_DESIGN.md` section 4; `ProfileCardCodec`). Latin names fit 200 bytes; a 24-character CJK name does not (about 310 bytes). For NFC the hard limit is 250 bytes (section 3.4); a longer card is shared by QR or link only and the NFC row says so.

### 2.3 Jam invite
Unchanged: `violence://party/<CODE>[?server=<jam server>]` and `https://yashaswahuh.is-a.dev/Violence/party/<CODE>` (also the older `/invite/<CODE>`, and the configured `JAM_INVITE_BASE_URL`). `<CODE>` is the real 6-character code from `0123456789ABCDEFGHJKMNPQRSTUVWXYZ` (no I, L, O). A custom Jam server (`?server=`) is carried only when set.

### 2.4 Which form goes where
| Where | Content |
|---|---|
| NFC tag (emulated) | the `violence://` URI, 250 bytes at most |
| QR code | the **https landing URL** (works with a plain camera app on any phone and opens the app when App Links are verified); the `violence://` URI when no web base is configured (Jam only, as today) |
| Copy link / share sheet | the https landing URL |
| Web "Open in Violence app" button | the `violence://` URI |

## 3. NFC, sending (Android phone, HCE)

### 3.1 How it works
While the **Tap to share** screen is in the foreground and the phone is unlocked, the app acts as an **NFC Forum Type 4 Tag** (NDEF Tag Application) through a `HostApduService`. The other phone sees an ordinary NDEF tag holding **one URI record**. Android phone to Android phone only; iPhone and Android Beam are out of scope (Beam was removed in Android 14 per the platform notes; **UNVERIFIED** here).

Verified against the official docs (`developer.android.com/develop/connectivity/nfc/hce`, fetched 2026-11-02):
- service manifest: `android:exported="true"`, `android:permission="android.permission.BIND_NFC_SERVICE"`, intent action `android.nfc.cardemulation.action.HOST_APDU_SERVICE`, meta-data `android.nfc.cardemulation.host_apdu_service` pointing to an XML file; the app needs the `NFC` permission.
- `host-apdu-service` needs `android:description`; `android:requireDeviceUnlock` is optional; `aid-group` needs `android:description` and `android:category` (`other` for non-payment apps); `aid-filter android:name` is the AID in even-length hex.
- Android picks the service from the reader's first `SELECT AID`; no partial AID match; **a foreground activity can call `CardEmulation.setPreferredService(Activity, ComponentName)`**; `processCommandApdu` runs on the **main thread**, one logical channel; the page recommends exchanges of about 1 KB within about 300 ms.
- Screen/lock: Android 9 and lower: no HCE with the screen off; `requireDeviceUnlock="true"` asks for unlock when a reader selects the AID. Android 10+: with **Secure NFC** on, HCE is unavailable with the screen off or locked whatever the attribute says; with it off the attribute behaves as on 9.

### 3.2 Identifiers and files
| Item | Value | Source |
|---|---|---|
| AID | `D2760000850101` (7 bytes, the NFC Forum "NDEF Tag Application") | NFC Forum Type 4 Tag spec as commonly used. **UNVERIFIED** (spec text is not freely fetchable here); the AID/APDU sequence below is what every Type 4 reader, including Android's, performs |
| Capability Container file id | `E103` | same |
| NDEF file id | `E104` (declared inside the CC, so readers follow the CC rather than assume it) | same |
| Registered category | `other` | HCE docs |

**Capability container (15 bytes, the file `E103`):**
```
00 0F        CCLEN = 15
20           mapping version 2.0
00 FF        MLe: most bytes a reader may ask for in one READ BINARY
00 FF        MLc: (we never accept writes)
04 06        NDEF File Control TLV (tag 04, length 06)
E1 04          NDEF file id
01 01          maximum NDEF file size = 257 (NLEN 2 + record 255)
00             read access: granted without security
FF             write access: denied (read-only tag)
```
**NDEF file (`E104`):** `NLEN` (2 bytes, big endian = message length) followed by the NDEF message. The message is one **short record**: `D1` (MB, ME, SR, TNF = well-known), `01` (type length), `<payload length 1 byte>`, `55` (`U`), payload = `00` (no URI abbreviation) + the UTF-8 URI. So a 150-byte URI gives message 155 bytes, file 157 bytes. The URI is at most 250 bytes.

### 3.3 APDU handling (what `Type4TagEmulator` implements; all in `app/.../nfc/`, pure Kotlin, byte-level tested)
All commands are short APDUs with CLA `00`. State per connection: `appSelected`, `selectedFile` (none, CC or NDEF).

| Command | APDU | Response |
|---|---|---|
| Select application | `00 A4 04 00 07 D2760000850101 [00]` | `90 00` and `appSelected` when sharing; **`6A82`** (not found) when no share is active or the AID differs |
| Select CC file | `00 A4 00 0C 02 E1 03` (P2 `00` also accepted) | `90 00`; `6985` if no application is selected |
| Select NDEF file | `00 A4 00 0C 02 E1 04` | `90 00`; `6985` if no application is selected |
| Select any other file id | same shape, other id | `6A82` |
| Read binary | `00 B0 <off hi> <off lo> <Le>` (`Le` 00 means 256) | the bytes of the selected file from `offset`, at most `min(Le, MLe=255, remaining)`, then `90 00`. Offset equal to the file length: just `90 00`. Offset beyond the file, or P1 bit 7 set (short file id): `6B00`. No file selected: `6985` |
| Update binary (`D6`/`D7`) | any | `6982` (security status not satisfied: read-only) |
| Any other INS | | `6D00` |
| CLA other than `00` | | `6E00` |
| Shorter than 4 bytes, or `Lc` that does not match the data | | `6700` |

Reading `Le` bytes where fewer remain returns the remainder with `90 00` rather than an error. **UNVERIFIED** against the Type 4 spec text (some tags answer `62 82`); Android's own reader accepts it.

**Content per share.** `ShareSession.start(uri)` builds the NDEF message once and stores it; the emulator reads the **current** message on every read of the NDEF file, so changing the shared item is: `stop()`, `start(newUri)`. Nothing is written to disk and no other data is reachable: the only files are the two above.

### 3.4 When it is active
- The service is declared **disabled** (`android:enabled="false"`) so the AID is not registered at all normally. `NfcShareSession.start(activity, uri)` enables the component with `PackageManager.setComponentEnabledSetting(..., DONT_KILL_APP)`, stores the message, and calls `setPreferredService`. `stop()` clears the message, calls `unsetPreferredService`, and disables the component again. The Tap to share screen starts in `onResume` and stops in `onPause`, so a screen-off, a call, switching apps or closing the sheet all stop it. If the app is killed while enabled, the service still answers `6A82` because no message is stored.
- `requireDeviceUnlock="true"`: a locked phone does not hand the tag out (on Android 10+ with Secure NFC on, HCE does not run with the screen off at all).
- Refused unless: NFC hardware present, NFC on, `FEATURE_NFC_HOST_CARD_EMULATION` present, and the URI is at most 250 bytes. Each case shows a friendly message (section 8); none crashes.
- The manifest declares `android.hardware.nfc` and `android.hardware.nfc.hce` as `uses-feature ... required="false"` so the app installs on phones without NFC. The official NFC guide shows `required="true"` and says to omit the element for optional use; `required="false"` is the standard Android mechanism and is what the brief asks for (the exact `.hce` feature name is from the platform constant `FEATURE_NFC_HOST_CARD_EMULATION`; **UNVERIFIED** in the docs fetched).

## 4. NFC, receiving

### 4.1 System dispatch (app closed or in the background)
Intent filters on `MainActivity`:
- `android.nfc.action.NDEF_DISCOVERED` (category DEFAULT) with `<data scheme="violence" host="song|profile|party">`.
- `android.intent.action.VIEW` (DEFAULT, BROWSABLE) with the same hosts (the existing `violence://party` filter gets `song` and `profile` added).
The NFC guide confirms the tag dispatch order (NDEF_DISCOVERED first) and that a URI record is matched with `scheme`/`host`/`pathPrefix`. `MainActivity` is `singleTask`, so the intent arrives in `onCreate` or `onNewIntent`; both go to `IncomingLinkHub.consume(intent)`.
**Android 17 (API 37):** the NFC guide says an activity that receives NFC intents must be protected by `android.permission.DISPATCH_NFC_MESSAGE` for apps **targeting above API 36**. This app targets 36, so it is **not added** (adding it now could block NFC delivery on current systems). To do when `targetSdk` is raised: move the NDEF filter to an `activity-alias` with that permission. UNVERIFIED on a device.

### 4.2 In-app Receive screen (reader mode)
`Receive` (Settings, Friends) calls `NfcAdapter.enableReaderMode(activity, callback, NFC_A | NFC_B | SKIP_NDEF_CHECK | NO_PLATFORM_SOUNDS, presenceCheckDelay 250 ms)` while it is resumed and `disableReaderMode` when paused. This is the more reliable path across devices (it does not depend on the receiving system's tag dispatch reading an emulated tag). `onTagDiscovered` gets an `IsoDep` tag and `Type4TagReader` runs, over `IsoDep.transceive`:
1. `SELECT AID` `00 A4 04 00 07 D2760000850101 00` expect `...90 00`
2. `SELECT` `E103`, then `READ BINARY` 15 bytes: check `CCLEN >= 15`, mapping version major 2, read `MLe` and the NDEF file id and size from the TLV
3. `SELECT` the NDEF file id from the CC, `READ BINARY` 2 bytes (`NLEN`); refuse `NLEN` 0 or above **512**
4. `READ BINARY` the message in chunks of `min(MLe, 255)`
5. Parse the NDEF message: **exactly one** record, TNF well-known, type `U`; URI prefix codes `00` (none) and `04` (`https://`) accepted, others refused
The reader has a 3 s transceive timeout, and never reads more than `NLEN`. The resulting string goes to `IncomingLinkHub.offer`. API details (`enableReaderMode`, `IsoDep`) are from the platform API reference as known to the author: the official reference page could not be read in full here, so **UNVERIFIED** in this session.

## 5. The incoming-link handler (shared by NFC, QR, paste, deep links, App Links)
`IncomingLinks.parse(text)` (shared, pure) returns `Song`, `Profile`, `Party` or `Invalid(reason)`:
- accepted forms: `violence://song|profile|party`, the legacy `bitchord://party`, and `https://yashaswahuh.is-a.dev/Violence/{song/<src>/<id>, profile, party/<CODE>, invite/<CODE>}` (and, for a Jam code only, the configured `JAM_INVITE_BASE_URL` host passed in by the app). Scheme and host are case-insensitive; **http is never accepted**.
- every id and field is validated as in section 2 (11-char YouTube id, 22-char base62 Spotify id, real party-code alphabet, the Phase 8 card rules). Free text (a pasted chat message) is searched for the first token that parses.
- a result is only ever a **candidate**. The app layer (`IncomingLinkHub`) then:
  - **gates** repeated reads: the same link seen again within **10 s** is ignored (an NFC reader sees the tag every presence check while the phones touch); at most **5 different links per 10 s** are accepted, the rest ignored; and an identical link already waiting for a decision is not queued twice;
  - **confirms**: song -> "Play now / Add to queue / Add to playlist"; profile -> "Add <name> as a friend?" (Phase 8 dialog logic, so the same card twice only updates the friend, never duplicates); Jam invite -> the existing join flow that previews the party and asks before joining;
  - **explains** anything invalid with a friendly message and does nothing else. The text of the bad link is never shown in a message or written to a log.
- Resolving a song after "Play now" / "Add" / "Add to playlist" is the only network use: the app's normal YouTube Music lookup for the video id (or a normal search for `q`/`sp`). Declining makes no request.

## 6. QR codes
- **Generating:** ZXing `core` (already in the project for the Jam invite) via the existing styled `QrCode` composable; content = the https landing URL (section 2.4). Error correction M, as it is today.
- **Scanning:** CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`; Apache-2.0) feeds frames to ZXing's `MultiFormatReader` restricted to QR (Apache-2.0). The **CAMERA permission is requested only when the scan screen opens**, and the manifest marks camera `required="false"`. Because not everyone grants a camera, the scan screen also offers **Choose a picture** (system photo picker, no permission) and decodes a screenshot of a QR code with the same ZXing reader. Scan results go to `IncomingLinkHub.offer` and the scanner closes at the first valid link.
- No ML Kit / Play Services (not open source, outside the "free and GPL-compatible libraries" rule).

## 7. Entry points
| Where | What |
|---|---|
| Song menu (three dots, also from Now Playing) | the existing **Share** row now opens the Share sheet: Tap to share (NFC phones), Show QR code, Copy link, Share Violence link, Share YouTube Music link (the previous behaviour) |
| Profile screen | **Share my profile** (only once a profile exists) |
| Listen Together invite | the invite sheet keeps code/link/QR and gains **Tap to share** |
| Settings and Friends | **Receive** (NFC reader, Scan QR code, Paste link) |
| TV | song actions: **Share** (QR + link); profile dialog: **Share My Profile** (QR + link); Jam dialog in a party: QR + link; incoming deep links get the same confirmations |
| Web | song/track menu and profile page: Copy link + QR; Friends page: paste a profile link (Phase 8) |
There is no separate "home menu" in the app: Settings is where the existing Profile and Friends rows live, and Receive sits beside them.
On a phone without NFC the NFC option is hidden; QR and link stay.

## 8. Failure behaviour (never a crash)
| Situation | Behaviour |
|---|---|
| no NFC hardware | NFC rows hidden; QR/link only |
| NFC turned off | row stays with "NFC is off" and **Open NFC settings** (`Settings.ACTION_NFC_SETTINGS`) |
| HCE unsupported | "This phone cannot send by tap. Use the QR code." |
| screen locked / screen off | Tap to share stops (the sheet pauses); a locked phone is not served (`requireDeviceUnlock`) |
| link too long for NFC | NFC row explains; QR and link still work |
| unreadable / foreign tag, wrong NDEF, bad link | "That is not a Violence link." |
| no camera, permission denied | explanation plus **Choose a picture** and **Paste link** |

## 9. Web landing routes
`/song/:source/:id` and `/profile` (and the existing `/party/:code`) under the base path. Each shows a small card (title/artist or name/avatar, plain text only), a primary **Open in Violence app** button (the `violence://` URI), **Copy link**, no automatic redirect (no loop), no server call. The 404 fallback already routes unknown deep links to the SPA; the new routes are registered in the page registry so they render instead of the not-found page. `noindex` stays (robots/meta from Phase 6). The web has no scanner or NFC.

## 10. App Links
Both manifests' existing `autoVerify` filter gets two more `pathPrefix` lines: `/Violence/song/` and `/Violence/profile` (the existing `/Violence/party/` and `/Violence/invite/` stay). No change to `assetlinks.json` is needed (it covers the whole domain).

## 11. Privacy
- What leaves the device: only the shared item's own link (song id + display hints, the Phase 8 card fields, or a party code), and only to the other phone/camera/app the user chose. No cookie, token, account id, email, device id or photo is ever in a payload.
- Nothing is logged: the share/receive code has no logger calls, and invalid input is reported by a category, never echoed.
- No network call is made by sharing, scanning, reading a tag or pasting. After the user **confirms** a song, the normal track resolution runs (YouTube Music lookup by id, or a search for `q`/`sp`).
- HCE exposes exactly two read-only files (the capability container and the one NDEF message) and only while the Tap to share screen is shown.

## 12. Code layout
- **Shared** (`shared/.../data/share/`, pure, common code): `ShareText`, `SongLinks`, `IncomingLinks` (+ `PartyLinks`), `IncomingLinkGate`. Tests in `commonTest`.
- **App** (`app/.../nfc/`): `NdefUri`, `Type4Tag` (emulator and reader), `ShareHostApduService`, `NfcShareSession`, `NfcReceiver`; (`app/.../share/`): `IncomingLinkHub`, `ShareItem`, Compose `ShareSheet`, `TapToShareDialog`, `ReceiveScreen`, `QrScanner`, `IncomingLinkDialogs`. Tests in `app/src/test`.
- **TV:** `TvShareDialogs` (reuses `QrCode` and the hub); **Web:** `web/app/src/share/` (TypeScript mirror of the parsers with the same vectors, the landing pages, the QR component).

## 13. New dependencies (all free, GPL-3.0 compatible)
| Library | Used for | License |
|---|---|---|
| `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` 1.6.2 | camera preview and frame analysis for scanning (phone only) | Apache-2.0 |
| `com.google.zxing:core` 3.5.3 | already present (QR encode); now also QR decode | Apache-2.0 |
| `uqr` (npm, web) | QR image generation on the web | MIT |
Apache-2.0 and MIT are compatible with GPL-3.0. Nothing from Google Play Services.

## 14. Tests
- Shared: song builder/parser (valid, oversized, wrong version, bad ids per source, injection in title/artist, control and bidi characters, unicode, duplicate and unknown fields, `+`/`%` handling), incoming handler decisions for every form and for hostile input, the gate (duplicates, rate limit).
- App (JVM): NDEF record builder/parser; byte-level Type 4 emulator tests (CC bytes, NLEN bytes, status words, offsets/lengths, chunk boundaries at 255/256, files of 0..257 bytes) and an **emulator-to-reader round trip** that runs the real reader against the real emulator.
- Web: the same song/handler vectors in TypeScript.
- Manual: `SHARE_TEST_PLAN.md` (two phones).

## 15. UNVERIFIED (needs two real phones, or the NFC Forum spec)
- That a real second phone's reader mode and its system tag dispatch read this emulated tag at all, per model. HCE emulating a bare NDEF tag is a widely used pattern, but not every phone/ROM exposes the right NFC routing for the NDEF AID.
- The AID and file ids against the NFC Forum Type 4 Tag text; that `Le` greater than the remainder is accepted; the `READ BINARY` `6B00` choice.
- `enableReaderMode`/`IsoDep` API shapes (known from the reference; the page could not be read in full here); URI prefix codes `00` and `04` (NFC Forum URI RTD, from memory; the Android guide's example comment is ambiguous).
- `required="false"` for the `.nfc.hce` feature, Secure NFC behaviour on each phone, the Android 17 `DISPATCH_NFC_MESSAGE` requirement.
- Camera scanning speed/quality across phones; App Links verification for the new prefixes (needs `assetlinks.json`, `APP_LINKS.md`).

## 16. Implementation status
See `PHASES.md` (Phase 9) for the date, commits and what was verified.
