# Rules for Future AI Agents & Contributors

**Applies to:** All future coding phases, sessions, and automated tasks in this repository.  
**Location:** `/fork-docs/AGENT_RULES.md`

Every AI agent starting a new phase in this project **MUST** read and adhere strictly to these core engineering directives:

---

## 1. The 100% Free-Forever Constraint (Non-Negotiable)
- Every host, service, API, database, tool, and third-party library used must be **100% free forever**.
- **No paid tiers, no free trials that expire, and no services that require a credit card on signup.**
- If any proposed feature or infrastructure would require payment or a credit card, you must **stop**, state so explicitly, and provide a 100% free alternative (such as Render free tier with keepalive, Cloudflare Pages, GitHub Pages, GitHub Releases, or Firebase Spark Plan).

---

## 2. Private Fork Constraint (Never Public)
- This is a private fork created strictly for personal use by a small group of 4–6 individuals.
- **Never publish APKs, modified code, release notes, or website instances publicly.**
- Do not add trackers, commercial analytics, ads, telemetry beacons, or monetization code.
- Only three targets are maintained: **Android Phone (`:app`)**, **Android TV (`:tv`)**, and **Web (`/web`)**. Wear OS and Desktop are pruned.

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

---

## 5. Git Remote & Upstream Discipline
- **NEVER PUSH TO UPSTREAM.**
- Remote `origin` points to the private repository (`https://github.com/yashaswahuh/Violence.git`).
- Remote `upstream` (when added) points to `https://github.com/kushagrasinghx/BitChord.git` and must be treated as **READ-ONLY**.
- Never open Pull Requests from this repository back to the upstream repository.

---

## 6. Development Discipline & Build Verification
- **Make Small, Atomic Commits:** Group related changes logically (e.g. one commit for string rebranding, one for package renaming). Write clear commit messages.
- **BUILD TARGET RESTRICTION (STANDING USER DIRECTIVE):**
  - **ONLY test/build Android Phone (`:app:assembleDevDebug`) and Web (`pnpm --dir web build:site`).**
  - **DO NOT build or test WearOS or Android TV (`:tv`) under any circumstances, now or in future phases, unless the user explicitly asks to start doing it.**
- **Build Verification Commands:**
  - Android APP: run `.\gradlew.bat :app:assembleDevDebug`.
  - Web: run `pnpm --dir web build:site` (or `corepack pnpm --dir web build:site`).
  - Backend: run `go test -v ./...` inside `backend/`.
  - **Never declare a phase or task complete without successful build/test execution output for the allowed targets.**
- **Preserve Unrelated Documentation & Comments:** Maintain comments that explain subtle bug fixes (such as D8/R8 phi allocation notes or NewPipeExtractor Utils exclusion rules).
