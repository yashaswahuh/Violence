# Violence on the web: a guide for friends and family

Violence also works in a web browser on a computer. It plays music from YouTube Music, shows lyrics, and lets you listen together with friends. This guide is for you if you were sent the link by the person who runs Violence.

## What you need
- A **computer** with **Chrome, Edge or Brave** (Firefox works only in a limited way, see below).
- A free YouTube Music / Google account if you want your own library (you can skip it to just play songs).
- **On a phone, use the Violence Android app instead.** Phone browsers cannot run the add-on the web version needs.

## The address
The website is at **https://yashaswahuh.is-a.dev/Violence/** (capital V; it does not work with a small v). Only share it with people you trust: anyone who has the link can open it.

## One-time setup: install the Violence add-on
The website cannot reach YouTube by itself. A small add-on called **Violence** does that part inside your own browser, using your own connection. Nothing goes through anybody else's server.

1. Open the website. A bar at the top says **"Play YouTube Music with the Violence extension"**: click **Get the Extension** (or open the **Extension** page from the left menu).
2. Click the download for your browser. You get a `.zip` file.
3. **Unzip it** (right-click, Extract All). You get a folder called something like `violence-extension-chrome (0.4.4)`. **Keep this folder where it is**; the browser uses it directly. Do not delete it.
4. **Chrome / Edge / Brave:**
   - Type `chrome://extensions` (Edge: `edge://extensions`, Brave: `brave://extensions`) in the address bar.
   - Turn on **Developer mode** (a switch at the top right).
   - Click **Load unpacked** and choose the unzipped folder (the one that contains a file called `manifest.json`).
   - Click the puzzle-piece icon in the toolbar and **pin** Violence so you can see it.
5. Reload the website. The bar at the top should be gone.

**Firefox:** Firefox only keeps add-ons that have been officially signed, and this one is not. You can still try it, but it is removed every time Firefox closes: open `about:debugging`, click **This Firefox**, **Load Temporary Add-on**, and pick the `manifest.json` file inside the unzipped folder. Chrome, Edge or Brave is much easier.

## Using it
1. **Sign in to YouTube Music in the same browser**: open https://music.youtube.com once and sign in. The add-on uses that sign-in to show your library. Your password and cookies stay in your browser.
2. Come back to the Violence website and search or browse, then press play.
3. **Spotify** (optional): sign in to Spotify in the same browser to use your playlists.

## Listening together
- Open **Listen Together** from the menu. **Start a party** gives you a 6-character code; **Join** takes a code. You can paste a whole invite link or message into the box and it picks out the code.
- If someone sends you a link that looks like `.../Violence/party/ABC123`, open it and press **Join in this browser** (or **Open in Violence app** if you are on Android).
- If your browser says **"Tap to start the party's audio"**, press **Start audio**: browsers do not allow sound until you have clicked something.
- The first person to start a party after a quiet spell may wait up to a minute while the party server wakes up. That is normal.

## If something is wrong
| What you see | What to do |
|---|---|
| The bar says the extension is missing | Check it is installed and switched on in `chrome://extensions`, then reload the page. |
| It stopped working after the owner updated it | The site shows an **update available** notice on the Extension page: download the new zip, unzip it over/next to the old one and press the refresh arrow on the add-on in `chrome://extensions`. |
| Page is blank or "404" after a refresh | Open https://yashaswahuh.is-a.dev/Violence/ directly (capital V). |
| No sound in a party | Press **Start audio**, and check the tab is not muted. |
| A song will not play | Some songs are blocked in some countries. In a party, the app tells you and rejoins on the next song. |

## Privacy in one paragraph
The add-on only talks to YouTube, Spotify, Discord (if you turn that on) and the Violence website itself. It does not send your sign-in to anyone else. The website does not have accounts, advertising or tracking.

## For the person who runs it
How the site is built and deployed: `CI_DEPLOY.md`. The extension only works with the site at exactly `https://yashaswahuh.is-a.dev`; if the site is served from any other address (for example the `github.io` fallback), the add-on will refuse to talk to it.
