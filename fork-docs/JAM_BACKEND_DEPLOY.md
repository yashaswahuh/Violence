# Jam backend: deployment guide (Render Free)

Why this host and what it costs you: `JAM_BACKEND_FEASIBILITY.md`. Short version: **Render Free web service, no credit card, WebSockets supported, sleeps after 15 idle minutes, about 1 minute to wake, rooms are lost on sleep or restart.** Everything below is free. If Render ever asks for a card for a *free* web service, stop and use the fallback (section 9).

Never put real URLs or secrets in the repository. The server holds no secrets; the only value you create is its public URL, and it goes into `local.properties` (gitignored) and a GitHub repo *variable*.

## 1. What is automated vs what only you can do
| Automated (already in the repo) | Done by hand (only you can) |
|---|---|
| Backend code, `backend/render.yaml` values, `backend/Dockerfile`, tests | Create the Render account |
| Android/TV/web clients read the URL from `JAM_BACKEND_URL` / `VITE_PARTY_SERVER_URL` | Connect GitHub and create the web service |
| CI passes the GitHub variable `JAM_BACKEND_URL` to the Android, TV and web builds | Type the environment variables in the dashboard |
| Cold-start "waking up" UI and automatic reconnect | Copy the final URL into `local.properties` and the GitHub variable |
| `scripts/jam-test.mjs` test | Create the free pinger (optional) |

## 2. Create the Render service (by hand, about 10 minutes)
1. Go to https://render.com and sign up **with GitHub**. Do not enter a card. (Render docs mention a payment method only for overage billing.)
2. Dashboard -> **New** -> **Web Service** -> "Build and deploy from a Git repository". Allow Render's GitHub app to read **only** `yashaswahuh/Violence`. (Private repositories: UNVERIFIED on the free plan; if Render cannot see the repo, make the *connection* work via the GitHub app settings, do not make the repository public.)
3. Fill the form with exactly these values:
   - **Name:** `violence-jam` (the URL becomes `https://violence-jam.onrender.com`; if the name is taken Render adds a suffix, use whatever it shows).
   - **Region:** any (Singapore is in the blueprint).
   - **Branch:** `violence-nk`.
   - **Root Directory:** `backend`.
   - **Runtime:** `Go`. **Build Command:** `go build -o server .` **Start Command:** `./server`
   - **Instance Type:** **Free**.
   - **Advanced -> Health Check Path:** `/healthz`. **Auto-Deploy:** *No* (you deploy by hand; see section 6).
4. **Environment variables** (Advanced -> Add Environment Variable). Values are in `backend/render.yaml`:
   | Key | Value |
   |---|---|
   | `JAM_MAX_MEMBERS` | `15` |
   | `JAM_ALLOWED_ORIGINS` | `https://yashaswahuh.is-a.dev,http://localhost,http://localhost:5173` |
   | `JAM_TRUST_PROXY` | `true` |
   | `JAM_CREATE_RATE_PER_MINUTE` | `6` |
   | `JAM_PUBLIC_ORIGIN` | your service URL once you know it (optional; makes invite links use it) |
   Do **not** set `JAM_ENABLE_STATS`.
5. **Create Web Service.** Wait for "Live". The first build downloads Go dependencies.
6. **If the build fails** with "go.mod requires go >= 1.27.0": switch **Runtime** to **Docker** (Settings -> Runtime), set **Dockerfile Path** `./Dockerfile` and keep Root Directory `backend`; redeploy. The Dockerfile pulls exactly the Go version `go.mod` names. (Neither path could be tested from the owner's PC: Go and Docker are not installed there, so the first Render build is the first compile. See `JAM_TEST_PLAN.md`.)

## 3. Check it works
```powershell
curl.exe https://<your-service>.onrender.com/healthz
```
Expect `{"ok":true,"serverMs":...}` (the very first call can take about a minute while the service starts). Then run the full test from the repo root, 2 clients and then 15:
```powershell
node scripts/jam-test.mjs --url https://<your-service>.onrender.com --clients 2
node scripts/jam-test.mjs --url https://<your-service>.onrender.com --clients 15 --out jam-test-15.json
```
All checks must pass; paste the summary lines into `JAM_TEST_PLAN.md` ("Results"). Do not commit `jam-test-15.json` if it contains your URL.

## 4. Point the apps at it
- **Android phone and TV (local builds):** add to `local.properties` in the repo root (gitignored; never commit it):
  ```properties
  JAM_BACKEND_URL=https://<your-service>.onrender.com
  ```
  then `.\scripts\build-active.ps1 -Target phone` (or `tv`). It becomes the built-in default; anything typed under "Use your own server" in the app still wins. The old name `LISTEN_TOGETHER_SERVER` is still read if `JAM_BACKEND_URL` is absent.
- **Web (GitHub Pages):** GitHub -> repo **Settings** -> **Secrets and variables** -> **Actions** -> **Variables** -> **New repository variable**: name `JAM_BACKEND_URL`, value `https://<your-service>.onrender.com`. Then Actions -> "Web Deploy" -> Run workflow. (`web.yml` maps it to `VITE_PARTY_SERVER_URL`.) Local web build: `$env:VITE_PARTY_SERVER_URL='https://...'` then `.\scripts\build-active.ps1 -Target web`.
- **CI-built Android/TV APKs:** the same GitHub variable `JAM_BACKEND_URL` is written into `local.properties` by `android.yml`, `check.yml` and `tv.yml`.
- **Invite links (optional, Phase 5):** `JAM_INVITE_BASE_URL` is where shared links point. Set it to your web player, for example `https://yashaswahuh.is-a.dev/Violence` (capital V: Pages is case sensitive), in `local.properties` (`JAM_INVITE_BASE_URL=...`) and as a GitHub repo variable of the same name (Android/TV CI reads it). Links then look like `https://yashaswahuh.is-a.dev/Violence/party/<CODE>` and open the web landing page. **Leave it empty to share only the code** (the invite sheet then offers *Share code* and *Copy code*, and the QR code opens the app via `violence://party/<CODE>`). For the web build the variable is `VITE_INVITE_BASE_URL`; since Phase 6 `web.yml` passes the same GitHub repo variable `JAM_INVITE_BASE_URL` to it (and `JAM_BACKEND_URL` to `VITE_PARTY_SERVER_URL`), so one pair of repo variables configures the phone, TV and web builds.
- **Everyone's app:** install the new build. Old builds (and a build with no URL) show "Jam is not set up in this build yet" instead of failing; the user can also type the address under **Use your own server**. **Everyone in a party must use the same server.**

## 5. How the apps behave with a sleeping server
Render stops an idle free service after 15 minutes without traffic (HTTP requests and WebSocket messages count as traffic, so a running party keeps it awake). The first request afterwards takes about a minute.
- **Starting, joining or looking up a party:** the app first asks `/healthz`. If there is no answer it shows **"Waking up the Jam server... this can take up to a minute."** and retries with a growing pause (2 s up to 10 s) for **85 seconds**, then either carries on or says "The Jam server didn't wake up. Give it a minute and try again."
- **Dropped connection during a party:** the phone and web reconnect on their own with a jittered backoff (1 s doubling to 30 s) and resync the clock and queue. The server holds a member's slot for 45 s. If the *server* restarted, the room is gone (memory only): create a new one.
- Custom servers typed in by a user do not get the 85 s wait; they fail fast.

## 6. Redeploy and roll back
- **Redeploy** (after backend changes land on `violence-nk`): Render dashboard -> your service -> **Manual Deploy** -> **Deploy latest commit**. Rooms in progress end. Do it when nobody is listening.
- **Roll back:** service -> **Events/Deploys** -> pick the last good deploy -> **Rollback** (UNVERIFIED label wording; Render keeps previous deploys). Or point `JAM_BACKEND_URL` somewhere else and rebuild.
- **Turn Jam off:** clear `JAM_BACKEND_URL` (local.properties and the GitHub variable) and rebuild; the app says Jam is not set up. Or suspend the service in the dashboard.
- **Backend env change only:** edit in the dashboard (Environment) -> save; Render restarts the service.

## 7. Optional: free uptime pinger (keeps it from sleeping)
Without a pinger the first person each session waits about a minute (they see the "waking up" message). With one, it stays awake.
- **cron-job.org** (free, no card): create a job, URL `https://<your-service>.onrender.com/healthz`, **every 10 minutes** (must be under 15), method GET. You can limit it to hours you actually listen, e.g. 16:00-24:00, to save instance hours.
- **UptimeRobot** (free plan; the free interval, 5 minutes, is UNVERIFIED): monitor type HTTP(s), the same URL.
- **Honest caveats:** Render gives **750 free instance hours per month per workspace**, with no rollover. A service awake 24/7 uses about 744 hours, so a 24/7 pinger fits **only if this is the only free web service in your Render workspace**; a second one would run you out and Render would suspend free services until next month. A time-limited pinger (as above) is safer. Whether Render's terms allow keep-alive pings is UNVERIFIED; Render "might restart a Free web service at any time" regardless, which also ends rooms.

## 8. Operating notes
- Logs (Render dashboard) contain party codes, member ids and timings, **no display names, no avatars, no tokens** (the drift log line was changed to the member id). Do not add more logging of member data.
- Create rate is 6 per minute per IP; rooms are limited to 10 members (the app creates rooms of 5 by default; the host can change it in the party).
- Browser access is limited to `https://yashaswahuh.is-a.dev` and localhost by `JAM_ALLOWED_ORIGINS`. If you serve the site from another address (for example the `github.io` fallback URL), add it to that variable.

## 9. Fallbacks
1. **Northflank Sandbox** (free, no card, always on, so no cold start; WebSocket on a public port is UNVERIFIED, test it first): create account -> project -> **Add service -> Combined service** -> repository `yashaswahuh/Violence`, branch `violence-nk`, build context `backend`, Dockerfile `backend/Dockerfile`, port `8000` HTTP public. Add the same environment variables as in 2.4 (`JAM_TRUST_PROXY` = `true`). Run `jam-test.mjs` against its URL before using it.
2. **Own PC + free Cloudflare Tunnel** if a machine is always on (needs Go or Docker there).
3. **Firebase Realtime Database (Spark, free, no card)**: the documented last resort. It means a different protocol and a client rewrite; it is **not built** and will be done only if the owner asks.
