# Rules for Future AI Agents & Contributors

**Applies to:** All future coding phases, sessions, and automated tasks in this repository.
**Location:** `/fork-docs/AGENT_RULES.md` (updated in Phase 3, 2026-11-01; `/CLAUDE.md` is the short version, this file wins if they differ in detail)

Every AI agent starting a new phase in this project **MUST** read and adhere strictly to these core engineering directives:

---

## 1. The 100% Free-Forever Constraint (Non-Negotiable)
- Every host, service, API, database, tool, and third-party library used must be **100% free forever**.
- **No paid tiers, no free trials that expire, and no services that require a credit card on signup.**
- If any proposed feature or infrastructure would require payment or a credit card, you must **stop**, state so explicitly, and provide a 100% free alternative (such as Render free tier with keepalive, Cloudflare Pages, GitHub Pages, GitHub Releases, or Firebase Spark Plan).

---

## 2. Private Fork Constraint (Never Public)
- This is a private fork created strictly for personal use by a small group (core 4-6 people, up to about 10-15 with friends and family).
- **Never publish APKs, modified code, release notes, or website instances publicly.**
- Do not add trackers, commercial analytics, ads, telemetry beacons, or monetization code.
- Active targets: **Android Phone (`:app`)**, **Android TV (`:tv`)**, **Web (`/web`)**. Wear OS was unfrozen on 2026-11-05 and desktop on 2026-11-04 (section 7).
- Never add caps, allowlists, extra login steps or restrictions that hinder the core users.

---

## 3. GPL-3.0 License Obligations
- The upstream project is licensed under **GPL-3.0**.
- The `LICENSE` file must stay in the root of the repository.
- **Never delete existing copyright notices or author attributions from file headers** (e.g. notices citing Kushagra Singh, Metrolist, NewPipe, Kira, Orchard).
- Do not use the original author's name, handle, or the trademark "BitChord" as the primary public identity of the fork.

---

## 4. Keystore & Secret Protection
- **NEVER COMMIT KEYSTORES, PASSWORDS, OR SECRETS.**
- The release keystore (`*.jks`, `*.keystore`) and `keystore.properties` are strictly gitignored. Never add them with `git add -f` or remove them from `.gitignore`.
- API keys (such as `LASTFM_API_KEY`, `LASTFM_SECRET`, or personal tokens) must stay in `local.properties` or environment variables; never hardcode secrets in source files.
- **Login cookies, tokens, and session data for YT Music and Spotify (and Discord) must never be logged, committed, or sent to any server other than the service they belong to.**

---

## 5. Git Remote & Branch Discipline
- **BRANCH RULE:** all work, commits, and pushes go to the branch **violence-nk**, never `main` and never `violence-main`. Run `git branch --show-current` before starting and before every push.
- **Never force-push, never rewrite history, never push to the upstream remote.**
- Remote `origin` = `https://github.com/yashaswahuh/Violence.git`. Remote `upstream` = `https://github.com/kushagrasinghx/BitChord.git`, push URL `DISABLED`, **READ-ONLY**. Never open Pull Requests from this repository to upstream.
- Upstream syncs follow `UPSTREAM_SYNC.md`.

---

## 6. Development Discipline & Build Verification
- **Make small, atomic commits** with clear messages.
- **Build verification uses explicit module paths only** (see `BUILD_COMMANDS.md`, or run `scripts/build-active.ps1` and `scripts/test-active.ps1`):
  - Phone: `.\gradlew.bat :app:assembleDevDebug '-Pviolence.bundleWear=false'`
  - TV: `.\gradlew.bat :tv:assembleDevDebug '-Pviolence.bundleWear=false'` (TV is ACTIVE; the old "never build TV" directive is cancelled)
  - Web: `corepack pnpm --dir web build:site`
  - Backend: `go test -v ./...` inside `backend/`
- **Never declare a phase or task complete without successful build/test output for the active targets.**
- **Preserve unrelated documentation & comments** that explain subtle bug fixes (such as D8/R8 phi allocation notes or NewPipeExtractor `Utils` exclusion rules).
- Update `PHASES.md` and the relevant `fork-docs/` files when a phase finishes. Never guess; mark UNCLEAR.

---

## 7. DORMANT TARGETS (Wear OS and desktop)
**Status 2026-11-05: the owner unfroze Wear OS (`:wearos`); desktop was unfrozen on 2026-11-04. The rules below apply again if either is re-frozen. Wear OS builds use an explicit task only: `:wearos:assembleDevDebug`.**
Dormant modules: `:wearos` (`wearos/`), `:desktopApp` (`desktopApp/`), `:desktopUi` (`desktopUi/`). Details and the unfreeze checklist: `FROZEN_TARGETS.md`.
- **Never edit, rebrand, refactor, build, test, or lint the dormant modules unless the owner explicitly says "unfreeze".**
- **Never run root-level Gradle tasks without explicit module paths** (`build`, `assemble`, `check`, `test`, `lint`, `clean`, bare `gradlew`): they include the dormant modules. Use `BUILD_COMMANDS.md` or the scripts in `/scripts`.
- Do not include dormant targets in verification or reports beyond one line: "Wear OS and desktop: dormant, not checked."
- **Never remove code, dependencies, or resources that only dormant targets use** (including `wearos/protocol`, which `:app` compiles, `CMP_DESKTOP` in `sharedUi`, `/native/analyzer`, and the desktop includes in `settings.gradle.kts`).
- Reading dormant code is allowed only when a task truly requires it.
- If a change to shared code (`:shared`, `:sharedUi`, `/native`, expect/actual) could affect a dormant target: make the change if the active targets need it, do **NOT** touch the dormant target, and append one line to "Possible impact from later changes" in `FROZEN_TARGETS.md`.
- Keep desktop's Violence branding and its Windows installer `upgradeUuid` untouched, always. Check after every merge or upstream sync that the dormant modules and their Gradle includes survived (upstream deleted desktop in v1.9).

---

## 8. KEEP rule
Never remove or break playback, search, stream resolution, lyrics, metadata, artwork, downloads, Last.fm/ListenBrainz, the YT Music and Spotify logins/import, web extension support, or Jam. **Services marked KEEP in `ALLOWED_EXTERNAL_SERVICES.md` are untouchable**, even if the original author hosts them. Only DISABLE items (live stats, presence pings, analytics, crash reporting, telemetry, the author's update checker, donation and promo links) are switched off, and new ones that appear in upstream syncs must be disabled the same way.
