# Sharing: manual test plan (two phones, a TV, a browser)

Written 2026-11-02 for Phase 9. **Nothing in this plan has been run on a real phone.** The code was built and unit-tested on a PC (see the bottom); NFC and the camera cannot be tested without hardware, so this is the checklist for you. Design and the byte-level protocol: `SHARE_DESIGN.md`.

## 0. What you need
- **Two Android phones with NFC** (call them **A**, the sender, and **B**, the receiver), both with the same Violence build installed (the release APK from `RELEASE_GUIDE.md`, or a debug build). NFC switched on in both (Settings > Connected devices > Connection preferences > NFC; the wording varies).
- For the fallback tests: a phone **without NFC** (or just turn NFC off on one), a second screen to show a QR code, optionally a TV with the TV build, and a browser on `https://yashaswahuh.is-a.dev/Violence/`.
- A Jam server for the Jam tests (`JAM_BACKEND_DEPLOY.md`), and for song tests a network.
- Optional: a free tag-reader app (for example "NFC Tools") on B, to see what A is really sending, independent of Violence.

## 1. THE FIRST TEST (do this one first, five minutes)
Profile card, A to B, in the receiving app.
1. **A:** Settings > Profile. Create a profile if there is none (name, picture, Save). Open it again, tap **Share my profile**, tap **Tap to share**. A full screen says "Tap to share" with a pulsing NFC symbol. Leave it open, **screen on and unlocked**.
2. **B:** Settings > **Receive**. It says "Tap phones". Do nothing else.
3. Put the **backs of the two phones together** (the NFC antenna is usually behind the camera area or the top third of the back; slide slowly if nothing happens). Hold for about two seconds.
4. **Expected on B:** a dialog "**Add <A's name> as a friend?**" with A's picture. Tap **Add friend**: Settings > Friends now lists A.
5. **If nothing happens,** try, in this order: (a) swap roles; (b) move the phones slowly, front-to-back, until one vibrates or beeps; (c) take cases off; (d) on B, close Receive and reopen it; (e) on both phones, turn NFC off and on; (f) try the QR code route in section 5 to see if everything except NFC works. **Write down both phone models and Android versions and what happened**: that is the information that decides whether NFC receive needs a second approach for those phones (see `SHARE_DESIGN.md` section 15).

If step 4 works you have verified, on real hardware: host card emulation on A, the Type 4 tag APDU conversation, reader mode on B, the incoming-link handler and the friend confirmation.

## 2. Tap to share: songs, profiles, Jam invites, both directions
Repeat each row with A as sender, then B as sender. "Receive" = the Receive screen open on the receiver unless the row says otherwise.

| # | Sender does | Receiver | Expected |
|---|---|---|---|
| 2.1 | Play a song, three dots > **Share** > **Tap to share** | Receive open | A card "**Shared with you**" with the song's **cover art, title, artist, album and length** (it fills in a second after it opens). **Play now** starts that song, **Add to queue** adds it (toast), **Add to playlist** opens the playlist picker. Cancel does nothing. Nothing plays before a choice. |
| 2.2 | Settings > Profile > **Share my profile** > Tap to share | Receive open | "Add <name> as a friend?" (section 1). Tapping again adds nobody twice (the friend's details are only refreshed). |
| 2.3 | Start a Jam, open the invite sheet, **Tap to share** | Receive open | The Listen Together screen opens with the code ready and the party preview; **Join** asks before joining. |
| 2.4 | any of the above | **app closed** (swipe it away, screen on and unlocked) | The phone opens Violence by itself and shows the same confirmation. If the phone shows "no app found" or does nothing, that is the system tag dispatch not reading an emulated tag (UNVERIFIED, see design section 4.1); the in-app Receive route (2.1 to 2.3) is the supported path. |
| 2.5 | any of the above | **app open on another screen** (not Receive) | Same as 2.4: the confirmation appears over whatever is open, or nothing happens if the phone's tag dispatch does not read it. |
| 2.6 | sender's **screen locked** | Receive open | Nothing is received (the sender asks to be unlocked, or does nothing). Unlock and tap again: it works. **Nothing is ever received from a locked sender.** |
| 2.7 | leave the Tap to share screen (Back, Home, lock) | Receive open | Tapping the phones now does nothing: the sender stopped being a tag when the screen closed. |
| 2.8 | **NFC off on the sender** | | The Share sheet shows "**NFC is off**" and **Open NFC settings** (opens the system NFC page). |
| 2.9 | | **NFC off on the receiver** | Receive shows "NFC is off. Turn it on to receive by tapping." and **Open NFC settings**. |
| 2.10 | | a phone **without NFC** | No NFC option anywhere (Share sheet: QR and links only; Receive: only Scan and Paste). |
| 2.11 | tap the same phones together again right away | Receive open | The same confirmation is not stacked twice; after you dismiss it, tapping again offers it again at once; acting on it and tapping again within ten seconds is ignored. |
| 2.12 | A shares, B is mid-conversation with a **different link** within seconds | | B shows the newest one; at most five different links per ten seconds are taken (a flood is ignored). |

## 3. QR codes
| # | Steps | Expected |
|---|---|---|
| 3.1 | A: song > Share > **Show QR code** | A QR code appears; **Copy link** shows "Link copied"; **Share Violence link** opens the system share sheet with the https link. |
| 3.2 | B: Receive > **Scan a QR code** | Android asks for the **camera** permission now (and not earlier, check: opening Receive alone never asks). Allow. The camera shows; point it at A's code: B shows the same confirmation as a tap. |
| 3.3 | B: deny the camera permission | A message explains, and **Choose a picture** and **Paste a link** still work. |
| 3.4 | A: take a screenshot of the QR code; B: Scan > **Choose a picture** > pick the screenshot | Same confirmation. A picture with no Violence QR code says "No Violence QR code found in that picture." |
| 3.5 | Scan a QR code of something else (a website) | "That is not a Violence link." The scanner keeps looking; nothing opens. |
| 3.6 | Scan the QR code with the phone's **own camera app** (B, outside Violence) | It offers to open the https link: with App Links verified (`APP_LINKS.md`) it opens Violence directly, otherwise the web landing page, whose **Open in Violence app** button opens the app. |
| 3.7 | A: Jam invite sheet | The QR code there still works as before (and now also **Tap to share**). |

## 4. Links
| # | Steps | Expected |
|---|---|---|
| 4.1 | B: Receive > paste a copied link (song, profile, Jam) > **Open link** | Same confirmations. A whole chat message that contains the link also works. |
| 4.2 | Paste garbage, an `http://` link, a link with a wrong version, a song link with a 10-character video id | A friendly message each time ("That is not a Violence link.", "...newer version...", "...damaged or not valid"); nothing else happens. |
| 4.3 | From a computer: `adb shell am start -a android.intent.action.VIEW -d "violence://song/yt/dQw4w9WgXcQ?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley" com.yashaswahuh.violencemusic` | The song dialog appears (app cold or running). Nothing plays until you choose. |
| 4.4 | Same with `violence://profile?v=1&id=123e4567-e89b-42d3-a456-426614174000&n=Alex&a=guitar&c=0a84ff` and `violence://party/AB12CD` | The friend dialog; the Listen Together screen with the code ready. |
| 4.5 | `adb shell am start -a android.intent.action.VIEW -d "https://yashaswahuh.is-a.dev/Violence/song/yt/dQw4w9WgXcQ?v=1"` | With App Links verified the app opens; otherwise the browser opens the landing page. |

## 5. TV
The TV has no NFC and no camera: it shows codes and links, and confirms incoming links.
| # | Steps | Expected |
|---|---|---|
| 5.1 | Play a song > hold OK / the menu > **Share** | A QR code and the link in words, **Done** focused. A phone camera on the code opens the song page or the app. |
| 5.2 | Settings > Profile > **Share Profile**; Settings > Listen Together (in a party) > **Show QR Code** | The same dialog for the profile card and the Jam invite. |
| 5.3 | `adb shell am start -a android.intent.action.VIEW -d "violence://song/yt/dQw4w9WgXcQ?v=1&t=Test" com.yashaswahuh.violencemusic.tv` | "Open This Song?" with **Play Now** focused. **Play Now** and **Add to Queue** work with the remote only. Same for a profile link ("Add ... as a Friend?"). |

## 6. Web
| # | Steps | Expected |
|---|---|---|
| 6.1 | `https://yashaswahuh.is-a.dev/Violence/song/yt/dQw4w9WgXcQ?v=1&t=Test&a=Artist` | A card with the title, artist, **Open in Violence app** and **Copy link**. No automatic redirect, no loop. |
| 6.2 | `.../Violence/profile?v=1&id=...&n=Alex&a=guitar&c=0a84ff` | The picture and name, **Open in Violence app**, **Add as a friend** (asks first), **Copy link**. |
| 6.3 | A hand-edited link (wrong id, missing `v`) | "That doesn't look like a Violence song/profile link." |
| 6.4 | Web player: a track's three dots > **Share…**; Settings > Profile > **Share my profile** | A dialog with a QR code and **Copy link**. Escape closes it. |
| 6.5 | Friends page: paste a song link / a Jam link / a profile link | Song and Jam open their pages; a profile asks "Add ... as a friend?". |
| 6.6 | View source of the page | Still `<meta name="robots" content="noindex, nofollow">`. |

## 7. Privacy spot checks
- A shared song/profile/invite link contains only that item (no email, account id, token, device id, photo). Read the link (Copy link) and check.
- Nothing is logged: `adb logcat` while sharing and receiving shows no names, ids or links from Violence (it logs nothing in this code).
- Scanning, tapping and pasting do **not** make a network request by themselves. When a **song** link arrives, the card looks the song up (the normal YouTube Music lookup) to show its cover and details; with the phone offline the card shows the title from the link and "Could not find that song." and only Cancel works.

## 8. Report template
```
Sender phone / Android version:
Receiver phone / Android version:
Test number and what happened:
Did the receiver's NFC reader screen or the system open the app?
Anything shown on the sender?
```

## 9. What was run on the build PC (2026-11-02)
See `PHASES.md`, Phase 9, for the exact numbers. In short: shared, app (NFC byte-level, QR decode, hub) and web unit tests; phone, TV and web builds; the local Pages check for the new link shapes. **Not run:** anything with a real NFC chip or camera, App Links verification, the Jam server with the new links.

## 10. Part 1 fixes: what to check (2026-11-02)
| # | Steps | Expected |
|---|---|---|
| 10.1 | Settings main page | Only **one** profile row (your picture and name, or "Create your profile") sits above Audio Quality. Searching Settings for "friends", "nfc", "qr" or "receive" still finds it. |
| 10.2 | Settings > Profile | Under the title: **Friends** (with a count) and **Receive**. A clear gap between the privacy paragraph and **Share my profile**. |
| 10.3 | Profile > **Choose a photo** > pick any picture | The photo is accepted and shows next to the row (no "could not be read"). **Remove photo** works. The photo never appears on another phone. |
| 10.4 | Share sheet > **Share Violence link** > pick any app | The text is `violence://...` (not https). **Copy link** still gives the https link. |
| 10.5 | Tap to share (any item): watch the sender | Two phones touching, looping. When the receiver has read it: a green check and "Sent", then back to waiting after about two seconds. Check it is readable in **both light and dark theme** (the phone line art should follow the theme's text colour, the ripple is red). |
| 10.6 | Receiver, Receive screen | The same two-phones animation while waiting; "Received" with a check when a tap was accepted. |
| 10.7 | **Jam invite** > Tap to share, receiver on Receive | The invite arrives (Listen Together opens with the code ready). **If it still fails, note the message the receiver shows**: it now says which of "not sharing", "moved apart", "could not read" it was, and that tells us what to fix next. Also say what the sender's screen showed (the pulsing phones, or "Tap to share is not available right now"). |
| 10.8 | Receiver, tap while the sender is NOT on Tap to share | "The other phone is not sharing right now. Ask them to open Tap to share, then try again." (once, not repeatedly). |

## 11. Sharing ideas, part 2 (2026-11-02)

| # | Do this | Expect |
|---|---|---|
| 11.1 | Profile > turn on **Join friends Jams by tap**; friends A and B have added each other. A: Jam invite > Tap to share; B taps | B joins at once (a short "Joining the Jam from A" message), no preview. |
| 11.2 | Same, switch left OFF (default) | The preview appears as before. |
| 11.3 | Same, but B has NOT added A | The preview appears. |
| 11.4 | Share a song by tap/QR/link, both people have a profile | The received card shows "From <name>" with their avatar. |
| 11.5 | Receive a song while inside a Jam | **Add to Jam queue** shows; the song lands in the queue. Outside a Jam the button is absent. |
| 11.6 | Long-press the mini-player | The share sheet for the playing song opens (with a small buzz). A normal tap still expands the player. |
| 11.7 | Tap to share a song | A short tick on "Sent" (sender) and "Received" (receiver). |
| 11.8 | Share sheet > **Copy app link**, paste in the Receive page | A `violence://` link; the paste is accepted. **Copy link** still copies the https link. |

## 12. Part 2 bug fixes (2026-11-02)

| # | Do this | Expect |
|---|---|---|
| 12.1 | Profile page | The avatar has clear air below the Friends/Receive group. The auto-join switch is now near the bottom. |
| 12.2 | Open Replay stories on a tall phone | The backdrop fills the screen edge to edge; text and the share button stay clear of the status and gesture bars. |
| 12.3 | Any page with the top bar | A Receive (NFC) icon left of the Downloads/account buttons opens Receive. Web: an inbox icon in the sidebar footer and mobile bar opens the paste page. |
| 12.4 | Playlist menu (YouTube Music playlist) > Share | The share sheet (Tap to share / QR / copy). Tap to another phone: "Playlist shared with you" > Open playlist. |
| 12.5 | A phone-only playlist > Share, tap | The receiver sees "N songs" > Save as playlist; it appears in the library. |
| 12.6 | Liquid Glass on: scroll Home, fold/unfold the bar | Looks as before; compare smoothness against the previous build. |
| 12.7 | Liquid Glass on: open a dialog, the share sheet, Settings | Panels carry the glass tint and light edge; text stays readable. Off: exactly as before. |
| 12.8 | Glass on: long-press the now-playing part of the bar | The share sheet opens (the glass bar previously ignored the long-press). |

