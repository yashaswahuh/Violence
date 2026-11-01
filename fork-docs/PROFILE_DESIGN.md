# In-app profile and friends: design (Phase 8)

Written 2026-11-01. Companion to `PROFILE_AUDIT.md` (what exists today). Status of the implementation is in `PHASES.md` and at the end of this file.

## 1. Goals and non-goals
- A **Violence identity** (name + avatar + colour) that is local to the device, independent of every login, and shown to others in Jam.
- A **local friends list** and a **safe, tiny profile-card format** that Phase 9 will move over NFC, QR and links. This phase only ships the format, its parser, and a "paste a profile link" box.
- **Non-goals:** no server-side profiles, no accounts, no sync between a user's devices, no friend requests, no presence, nothing mandatory. Phase 9 does NFC / QR / link sharing and the landing page.
- The YT Music, Spotify, Discord and Last.fm logins are untouched. They remain for playback and library access only.

## 2. Data model (shared code: `shared/.../data/profile/`)

### InAppProfile
| Field | Rule |
|---|---|
| `id` | UUID v4, lower-case canonical form (`8-4-4-4-12`), generated **once** on the device (`java.util.UUID.randomUUID()`), opaque, unrelated to any account or device id. Never changes while the profile exists. Deleting the profile and creating a new one makes a **new** id. |
| `displayName` | 1 to 24 Unicode **code points** after normalisation (trim, runs of spaces collapsed to one). No control characters (category Cc), no invisible/format characters except the zero-width joiner (needed by emoji sequences), no line/paragraph separators, no lone surrogates, no private-use characters. Emoji and non-Latin scripts are allowed. |
| `avatarId` | one of the bundled preset ids (section 3). |
| `color` | accent colour as **6 lower-case hex digits without `#`** (for example `0a84ff`). Any 6-hex value is accepted (presets are a suggestion, not a whitelist). |
| `createdAt` | epoch milliseconds, local only. |

**Optional local photo (phone only, never shared):** stored as a downscaled JPEG in the app's private files directory (`files/profile/photo.jpg`, at most 256 px). It is shown only on this device (Settings row, profile screen). It is not part of the model, is never serialised into a card, never sent to the Jam server, and is deleted with the profile. TV and web do not offer a photo (no picker).

### FriendCard
`id` (the friend's profile id), `displayName`, `avatarId`, `color` (same rules as above), `addedAt` (epoch ms), optional `nickname` (local rename, 1 to 24 code points, same character rules; empty/absent = use their own name). The nickname is never shared.

### Storage ("whatever the app already uses")
- **Android phone and TV:** a dedicated, non-encrypted `SharedPreferences` file `violence_profile` in the app's private storage (the same mechanism as `AppSettings` and `ListenTogether`). It holds one JSON string for the profile, one for the friends list, and the "suggestion shown" flag. Nothing here is a secret, so the encrypted store (`AuthStore`) is not used. The file is **excluded from cloud backup** (new plain-prefs file is otherwise copied by Android Auto Backup; a restored profile id on a second phone would make two devices claim one identity) but kept in device-to-device transfer.
- **Web:** `localStorage`, key prefix `bitchord:` like every other key the web app already writes (`bitchord:deviceId`, `bitchord:party:settings`): `bitchord:profile`, `bitchord:friends`, `bitchord:profileSuggested`. Same JSON shapes as Android.
- Both read defensively: damaged JSON or invalid entries are dropped, never crash the app.
- Limits: at most **200 friends** (sanity bound for storage; a 15-person circle is far below it).

### Not tied to any login
Signing out of YT Music, switching Google accounts or removing Spotify does not touch the profile. The profile is **not** included in the settings backup file (`Backup.kt`; it only exports `bitchord_settings`).

## 3. Preset avatars
24 ids, each rendered as an emoji glyph on a circle filled with the profile colour (so there are no image assets to bundle and the same id looks the same on phone, TV and web; the glyph comes from the platform emoji font).

`headphones, note, vinyl, guitar, mic, piano, drum, trumpet, violin, radio, bolt, flame, star, moon, heart, rocket, ghost, alien, robot, cat, fox, panda, wolf, skull`

An id that a receiving app does not know is **invalid** when it arrives in a card (strict) and is shown as the first letter of the name when it arrives from Jam (tolerant, so a newer app's avatar never breaks an older app's member list). Presets are append-only; never rename or reuse an id.

Default for a new profile: `headphones`, colour `fa2d48`.

## 4. The profile card (shared by NFC, QR and links in Phase 9)

### Format, version 1
```
violence://profile?v=1&id=<uuid>&n=<name, percent-encoded UTF-8>&a=<avatarId>&c=<hex6>
https://yashaswahuh.is-a.dev/Violence/profile?v=1&id=<uuid>&n=<name>&a=<avatarId>&c=<hex6>
```
The https form uses `JamInvite.ORIGIN` (`https://yashaswahuh.is-a.dev/Violence`; capital V like every other web path) plus `/profile`. A trailing `/` is tolerated. Landing page for that URL and the App Links manifest line (`/Violence/profile`) arrive with Phase 9 (the format is final now).

- Fixed emit order `v,id,n,a,c`; the parser accepts any order.
- `id` is emitted dashed and lower-case; the parser also accepts 32 hex digits without dashes and normalises.
- `n` is percent-encoded with only `A-Za-z0-9-._~` left as is, so a space is `%20`, an emoji is four `%XX` bytes. On parsing, `+` is read as a space (form style), so a card that went through a browser still reads right.
- `c` has **no `#`** (a `#` would start the URL fragment).
- No photo, no email, no account id, no device id, no nickname, no timestamp travel in a card.

### Size
Card sizes in UTF-8 bytes (`violence://` form): typical ASCII name of 8 characters about 100 bytes; 24 ASCII characters about 120 bytes; 24 BMP non-Latin characters (for example CJK, 9 bytes each once encoded) about 310 bytes. **The "under about 200 bytes" NFC target holds for Latin-script names**; the codec exposes `fitsNfcTarget()` and Phase 9 should fall back to a QR code (or shorten the name for the tag) when a card does not fit. For reference: an NTAG213 tag holds only 144 bytes of user memory, NTAG215 504 and NTAG216 888 (UNVERIFIED, from the NXP datasheets as remembered), so Phase 9 should recommend NTAG215 or larger.

### Strict parsing (every incoming card is untrusted)
Checked in this order; the first failure is the error:
1. Whole input, trimmed, must be at most **512 characters** (`TooLong`), and start with `violence://profile` or the https base (otherwise `NotACard`). Scheme and host are case-insensitive; the `/Violence/profile` path match is case-insensitive too, as `JamInvite.parse` already does.
2. A `#fragment` is ignored. The query must be present.
3. `v` must be exactly `1` (`WrongVersion`; a missing `v` is `MissingField`).
4. Every key may appear **once** (`DuplicateField`) and only `v, id, n, a, c` are allowed (`UnknownField`). All five are required (`MissingField`).
5. `id`: UUID (dashed or plain 32 hex, any case), normalised to lower-case dashed (`BadId`). A card with the all-zero id is rejected.
6. `n`: valid percent-encoding with valid UTF-8 (`BadEncoding`), then the name rules in section 2 (`BadName` with the reason: empty, too long, control character, invisible character). A name longer than 24 code points is **rejected, not truncated**.
7. `a`: must be a known preset id (`BadAvatar`).
8. `c`: exactly 6 hex digits (`BadColor`); lower-cased.

The result is `ProfileCard(id, displayName, avatarId, color)`. It is only ever a *candidate*: nothing is stored until the user confirms.

### Adding a friend
"Paste a profile link" on the friends screen parses the pasted text, then asks **"Add `<name>` as a friend?"** (the name is shown as plain text). Rules:
- Your own id: refused with a message ("That's your own profile").
- An id already on the list: the stored copy is updated to the new name/avatar/colour (they may have changed them), your local nickname and `addedAt` are kept, and the message says so.
- Otherwise appended with `addedAt = now`.

## 5. Jam identity

### What changes in the wire protocol (backward compatible)
`JoinRequest` gains three **optional** fields, next to `displayName`:

| Field | Meaning | Server validation |
|---|---|---|
| `profileId` | the opaque profile UUID | lower-case dashed UUID, otherwise dropped |
| `avatarId` | preset avatar id | `[a-z0-9]{1,16}`, otherwise dropped (the server does not hard-code the preset list, so adding an avatar needs no redeploy) |
| `avatarColor` | 6 hex digits | `[0-9a-fA-F]{6}`, lower-cased, otherwise dropped |

An invalid optional field is **ignored, never an error**: a bad extra can never stop someone joining. The member object sent to everyone in the room (`members` frame, `welcome`) carries the same three fields when set. The pre-join **preview** (visible to anyone holding the code) carries only `avatarId` and `avatarColor`, never `profileId`. The server stores nothing beyond the in-memory room (it never did) and does not log any of these fields.

`PartyMember` and `JoinRequest` (shared Kotlin) and the web `Member`/`JoinRequest` types get the same optional fields. Android's JSON reader ignores unknown keys and the Go server ignores unknown request fields, so every old/new combination works (section 8).

### Which identity is sent
When a profile exists on the device:
- `displayName` = the Jam nickname if the user typed one for this party (existing feature, unchanged), otherwise the profile name; truncated at a code-point boundary to **80 UTF-8 bytes** because the current server rejects longer names (a 24-emoji name is 96 bytes).
- `avatarUrl` = **not sent** (no Google avatar once the user has chosen a Violence identity); `avatarId`, `avatarColor`, `profileId` as above.
- `userId` = first 32 hex of `sha256("violence-profile:" + profileId)`. It no longer derives from the Google account.
- `deviceId` unchanged.
- A device **with a profile can create and join Jam without being signed in to YT Music.**

When there is no profile: **exactly the previous behaviour** (YT Music name/avatar, hashed account id, sign-in required).

### What the receiving app shows
`avatarId` + `avatarColor` set: a coloured circle with the glyph. Otherwise `avatarUrl` if present, otherwise the first letter of the name (as before).

## 6. What leaves the device, and when
| When | Fields | Where to |
|---|---|---|
| Create / join / rejoin a Jam party | `displayName` (profile name or Jam nickname, at most 80 bytes), `avatarId`, `avatarColor`, `profileId` (opaque UUID), `userId` (a hash of the profile id), `deviceId` | the Jam server (self-hosted); members of the room get name, avatar id, colour, profile id; the pre-join preview gets name, avatar id, colour only |
| The user shares a card (Phase 9: NFC, QR, link) | `id`, `displayName`, `avatarId`, `color` | wherever the user sends it |
| Nothing else | | |

**Never sent anywhere:** email, Google account id, cookies, tokens, the local photo, friend list, nicknames, `createdAt`. The profile is not sent to Last.fm, ListenBrainz, Discord, Spotify, YouTube or any analytics (there is none).

**Not logged:** profile and friend code does not call any logger; card parse errors report an error *code* only, never the input text. (`ListenTogether` already redacts server addresses; this phase adds no new log lines.)

## 7. Deleting and clearing
- **Delete profile** (confirmation dialog): removes the profile JSON, the photo file and the "suggested" flag's profile state; the device falls back to the YT Music identity in Jam on the next join. A party the user is already in keeps showing the old name until they rejoin (the server only reads identity at join).
- **Clear friends list** (confirmation dialog): one action empties the list. Removing a single friend is a separate action.
- Uninstalling the app removes everything (the file is private storage; cloud backup excludes it).

## 8. Compatibility matrix (Jam)
| Client | Server | Result |
|---|---|---|
| old | old | unchanged |
| old | new | works; the server just has nothing extra to store |
| new, **no profile** | old or new | works; sends no new fields |
| new, profile | **old** | works: joins with the profile **name** (extra JSON fields are ignored by the old server); other members see an initial instead of the glyph, because an old server does not echo `avatarId`/`avatarColor` |
| new, profile | **new** | full: glyph + colour for everyone running a new client; old clients in the same room see the initial |

So **the Jam server should be redeployed to show avatars and colours** (names work without it). It does not need a data migration.

## 9. First-run suggestion
Once, after setup/onboarding, if there is no profile: a small dialog "Make your Violence profile? Pick a name and a picture so friends see you in Jam. You can skip this; everything works without it." with **Create profile** and **Not now**. Either answer sets the flag so it is never shown again (the profile can always be made from Settings). It is a plain dialog over the normal UI: playback and navigation are never blocked, Back dismisses it. On TV it is the same dialog, fully D-pad controlled, with focus on **Not now** so a stray OK press never starts anything.

## 10. Screens
- **Phone:** Settings gets a **Profile** row (avatar + name, or "Create your profile") above Account & Integrations, opening `Profile` (name field, avatar grid, colour row, optional photo, Save, Delete) and a **Friends** row opening `Friends` (list, rename, remove, "Paste a profile link", "Clear friends"). They are new `AppRoute`s like `ListenTogether`.
- **TV:** Settings gets Profile and Friends rows opening dialogs. Everything is a focusable button or grid cell (`tvClick`); name entry uses the TV's system keyboard; no photo.
- **Web:** Settings gets a Profile section and a Friends section on the same page style.
- Design language: the existing grouped rows, large rounded corners, system accent colour and SF-style type used by the surrounding screens.

## 11. Tests
- Shared (`jvmTest`/`commonTest`): codec round trips and rejection of every case in section 4 (valid, too-long, control characters, emoji and non-Latin names, bad uuid, unknown avatar, bad colour, oversize, wrong version, duplicates, unknown keys, bad percent-encoding), friends rules (add, duplicate, self, rename, remove, clear, cap), storage (load/save/corrupt data) and the Jam identity builder (name truncation, no-profile fallback).
- Web: the same vectors in TypeScript.
- Go: `JoinRequest.Validate` for the new optional fields (valid, invalid-and-dropped, absent) and member serialisation.
- Manual: `JAM_TEST_PLAN.md` section "Profile in Jam".

> **Phase 9 (2026-11-02):** the transports for this card are built: NFC tap (hard limit 250 bytes on the tag, so a long CJK card is shared by QR or link instead), QR codes and links, the `/Violence/profile` landing page and App Links prefix. See `SHARE_DESIGN.md`.

## 12. Implementation status (2026-11-01)
Implemented as designed, on phone, TV and web. What was verified and what is still UNVERIFIED is in `PHASES.md` (Phase 8). Deviations and details that the design did not spell out:
- **Strings:** the 56 new phone strings are English placeholders in all 17 other language files (the locale test only checks that the keys exist). The TV uses plain English text like the rest of the TV UI; the web too.
- **Suggestion dialog (phone):** shown once to existing users after the update too (they have never had a profile), but never during onboarding or while an update dialog is up.
- **Web suggestion:** a dismissible banner at the bottom, lowest priority after the extension, update and party banners.
- **Jam gate:** the phone, TV and web Jam screens treat "has a profile" like "signed in to YouTube Music".
- **Backup:** `violence_profile.xml` and `files/profile/` are excluded from cloud backup (`backup_rules.xml`, `data_extraction_rules.xml`); device-to-device transfer keeps them.
- **Invisible-character list:** besides Unicode categories, eight look-alikes (Hangul filler, braille blank, combining grapheme joiner and similar) are rejected; stacks of more than four combining marks are rejected. Unicode data differs slightly between the JVM and the browser for very new characters.
