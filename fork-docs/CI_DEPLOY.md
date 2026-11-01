# CI and deployment (one page)

Updated in Phase 6 (2026-11-02). Two workflows matter for sharing the app: **Web Deploy** (`web.yml`) puts the web player on GitHub Pages, and **Android CI** (`android.yml`) builds downloadable phone and TV debug APKs. Both run on a push to **`violence-nk`**; neither needs a secret to work.

## What each workflow does
| Workflow | Runs when | What it does | Where the result is |
|---|---|---|---|
| **Web Deploy** `web.yml` | push to `violence-nk` touching `web/**` or the workflow file; or "Run workflow" | typecheck + tests, then `pnpm build:site` (builds the site for the `/Violence/` folder and packages the browser extension zips it offers), sanity-checks the output (404.html, robots.txt, noindex, asset paths), uploads it and deploys with the official Pages actions | the run's **Deploy to GitHub Pages** job summary and the repo's **Deployments** panel show the URL |
| **Web Deploy, on a release** | a GitHub release is published (Phase 7) | the same workflow also copies that release's `update.json` + the per-CPU APKs it lists into the site's `updates/` folder (the app update files; `RELEASE_GUIDE.md`). A later web-only deploy keeps them (it downloads the newest release again) | `<site>/updates/update.json` |
| **Android CI** `android.yml` | push to `violence-nk` touching `app/ tv/ shared/ sharedUi/ native/ gradle/` or the root Gradle files or the workflow file (docs-only and web-only pushes do not trigger it); or "Run workflow" | JDK 21 + 17, Android SDK, then `:app:assembleDevDebug :tv:assembleDevDebug` (explicit module tasks, no Wear OS) and renames the APKs | **Artifacts** at the bottom of the run page: `violence-phone-debug-<sha>` and `violence-tv-debug-<sha>`, kept 30 days |
| other, already in the repo | | `check.yml` (phone tests + backend Go checks), `tv.yml` (TV build), `desktop-check.yml` and `release.yml` (dormant desktop, manual only), two upstream contributor-widget workflows | |

## One-time settings (by hand, on github.com)
1. **Settings > Pages > Build and deployment > Source = GitHub Actions** (not "Deploy from a branch").
2. **Settings > Actions > General**: Actions allowed; workflow permissions can stay at the default read (the workflows ask for what they need).
3. **Settings > Environments > github-pages > Deployment branches and tags**: this environment is created by the first Pages run and **defaults to the default branch only**. Add `violence-nk` (or "No restriction"), otherwise the deploy job is rejected ("Branch ... is not allowed to deploy to github-pages").
3b. For release-triggered deploys (Phase 7): in the same `github-pages` environment also allow the **tag pattern `v*`** (releases are tagged `v<version>`). Without it a release publishes fine but its Web Deploy run is rejected; any push to `violence-nk` under `web/` then deploys it just the same.
4. **Settings > Collaborators**: invite the people who should download APKs (artifacts of a private repo need repository access).
5. **Optional values** (Settings > Secrets and variables > Actions). All are optional; **every build works with all of them missing**:
   - **Variables:** `JAM_BACKEND_URL` (the Jam server, see `JAM_BACKEND_DEPLOY.md`), `JAM_INVITE_BASE_URL` (where shared party links point, for example `https://yashaswahuh.is-a.dev/Violence`). Used by both workflows.
   - **Secrets:** `LASTFM_API_KEY`, `LASTFM_SECRET` (Android/TV only, see `LASTFM_SETUP.md`). The web build uses no secret.

## How to run them
- **Push:** any matching push to `violence-nk` starts the right workflow. The first push that contains these files runs them.
- **"Run workflow" button:** GitHub shows it only for workflow files that exist on the **default branch** (`main`). Until these workflows are merged there, use the push trigger on `violence-nk` (for example a small edit under `web/` or to the workflow file) and watch the **Actions** tab.
- **The first run to do:** Web Deploy, because it also creates the `github-pages` environment (step 3 above). Success = both jobs green, the summary shows a URL, and opening `<URL>` shows the player; opening `<URL>party/ABC123` shows the invite page; reloading any page does not give a GitHub 404.
- **Android CI success** = one green job and two artifacts. Download, unzip the artifact, install the `.apk`. Debug builds from different runs are signed with different keys, so **uninstall the older build before installing a newer one** (release builds, signed with the owner's key, come from `scripts/release.ps1`: `RELEASE_GUIDE.md`).

## If a run fails
| Symptom | Check |
|---|---|
| Deploy job: "branch not allowed" / environment protection | step 3 above |
| Deploy job: Pages not enabled / 404 on `configure-pages` | step 1 above |
| Build step: "wrong base path" / "no 404.html" | the sanity check names what is wrong; `web/app/public/404.html`, `VITE_BASE_URL` |
| `pnpm install --frozen-lockfile` fails | `web/pnpm-lock.yaml` is out of date; run `pnpm install` in `web/` and commit the lockfile |
| Android: "Cannot find a Java installation ... languageVersion=21" | the JDK 21 step or `-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64` (see below) |
| Android: SDK platform / NDK / CMake errors | the **Install SDK packages** step (`platforms;android-37.0`, `platforms;android-36`, `cmake;3.22.1`, `ndk;27.0.12077973`) |
| Android: "no universal APK" | the build log above the step; the output folders are `app/build/outputs/apk/dev/debug` and `tv/...` |

## Honest notes
- **(a) Pages is public.** A GitHub Pages site is reachable by anyone who has the URL, even when the repository is private (private Pages needs GitHub Enterprise). The page has `noindex` and `robots.txt` disallows everything, which keeps it out of search engines but is not access control. Share the link only with people you trust. A free way to truly lock it down is **Cloudflare Pages with Cloudflare Access**; that is **documented here but NOT implemented**.
- **(b) The address.** The site is served at `https://yashaswahuh.is-a.dev/Violence/` **only if your user Pages site (`yashaswahuh.github.io`) already uses `yashaswahuh.is-a.dev` as its custom domain**; otherwise it is at `https://<github-username>.github.io/Violence/`. **UNCLEAR: owner to verify** in Settings > Pages. **The folder is `/Violence/` with a capital V**: GitHub Pages names it after the repository and is case sensitive, so `/violence/` is a different (non-existent) address. (The Phase 6 brief wrote `/violence/`; the workflow takes the folder from `actions/configure-pages`, so it always matches the repository name.) The browser extension only trusts the exact origin `https://yashaswahuh.is-a.dev`; on the `github.io` address it refuses to work.
- **(c) The web version needs the browser extension** (browsers block direct YouTube requests). See `WEB_USER_GUIDE.md` for friends and family.
- **Not verified on GitHub.** The first real run happens on GitHub. Checked locally instead: both workflow files parse as YAML, every action is pinned to a major version, the site builds with the `/Violence/` base and with every optional value missing, and a GitHub-Pages-like local server (`node scripts/serve-pages-local.mjs --check`) plus a real headless browser resolve the assets, the 404 redirect and `/party/<CODE>`. **UNVERIFIED until the first run:** `android-actions/setup-android@v3` plus the explicit `sdkmanager` install on the runner, and that Gradle finds JDK 21 through `JAVA_HOME_21_X64` (the dormant desktop module declares a Java 21 toolchain, so Gradle needs one even for a phone build).
- **Secrets.** The Android workflow passes the Last.fm values through the step's environment, never in a script or a file. Anything baked into an APK or a web page can be extracted by whoever has it.
