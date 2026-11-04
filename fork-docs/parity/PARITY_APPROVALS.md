# Parity Stage Approvals (DW Stages)

**Project:** Violence ("Desktop and Web Parity", DW-0 to DW-6)  
**Rule:** Each stage may not start until the previous stage is ticked and approved by the owner.

---

- [x] **DW-0: Audit, Parity Matrix, Gap Plan & Architecture Specifications (2026-11-04)**
  - Documents written: `PARITY_AUDIT.md`, `PARITY_DECISIONS.md`, `PARITY_APPROVALS.md`.
  - Compile health checked: `:desktopUi:compileKotlinJvm` (PASS), `:desktopUi:jvmTest` (PASS, 42 tests), `:desktopApp:compileKotlin` (FAIL - documented), `:shared:jvmTest` (PASS, 234 tests), `web typecheck` (PASS), `web test` (PASS, 239 tests), `web build:site` (PASS).
  - No application code, build files, or resources modified.

- [x] **DW-1: Desktop Compile Health & Unfreeze Housekeeping (2026-11-04)**
  - Fixed compilation error in `DesktopApp.kt:4223:33` (added `EntityType.OTHER` support and aligned `recordBrowseSearch`).
  - Ran `:desktopApp:compileKotlin`: PASS (0 errors).
  - Ran `:desktopApp:test`: PASS (58 test suites, 349 tests, 0 failures, 100% pass rate).
  - Verified `.\gradlew.bat :desktopApp:createDistributable`: PASS (`Violence.exe` generated at `desktopApp/build/compose/binaries/main/app/Violence/Violence.exe`).
  - On-device test: launched desktop app locally with `.\gradlew.bat :desktopApp:run` (verified running live, successfully loading session, home, new releases, and explore feeds).
  - Unfreeze housekeeping documentation updated in `fork-docs/BUILD_COMMANDS.md` and `fork-docs/FROZEN_TARGETS.md`.

- [x] **DW-2: Desktop and Web Logic Parity (2026-11-04)**
  - Modernized Desktop Jam: `JAM_BACKEND_URL` support, 85s wake-up notice flow (`serverWaking`), 15 members ceiling, party invite/code link extraction (`PartyLinks`/`JamInvite`), stalled-track isolation (`onTrackUnplayable`, `setStalledTrack`).
  - Added in-app profile creation, 24 emoji avatar picker, 10 color swatches, and local friends storage (`DesktopProfileManager`, `DesktopProfileDialog`, `DesktopFriendsDialog`, `DesktopAddFriendDialog`, `FriendCard`) to Desktop.
  - Added song, playlist, profile, and Jam sharing dialogs with QR code rendering (`DesktopShareDialogs`, `DesktopQrCode`) to Desktop.
  - Implemented desktop incoming link handling (`DesktopIncomingLinkHub`, `DesktopReceiveDialog`), protocol registration (`DesktopWindowsProtocol` registering `violence://` scheme), and CLI argument launch support in `Main.kt`.
  - Verified Spotify playlist link import integration on Desktop (`DesktopIntegrationsUi.kt`).
  - Verified Web profile and sharing dialog confirmations (239 tests pass, `web typecheck` clean).
  - Verification: all 357 tests in `:desktopApp:test` PASS, `:desktopUi:jvmTest` PASS, Web tests & typecheck PASS.

- [x] **DW-3: Violence Style Foundation (Desktop & Web) (2026-11-04)**
  - Moved pure `AccentMath`, `AccentEngine`, `StyleModel`, and `StyleBackgroundGeometry` into `:shared` (`shared/src/commonMain/kotlin/com/music/violence/ui/violencestyle/`).
  - Added unit test coverage in `:shared` (`shared/src/commonTest/kotlin/com/music/violence/style/StyleCoreTest.kt`): 245 tests pass.
  - Deployed SIL OFL fonts (`vs_bodoni_moda.ttf`, `vs_bodoni_moda_italic.ttf`, `vs_cormorant_garamond.ttf`, `vs_cormorant_garamond_italic.ttf`, `vs_inter.ttf`, `vs_anton.ttf`) to Compose Resources on Desktop and `@font-face` on Web.
  - Desktop integration: `DesktopAppearanceSettings` with `violenceStyleEnabled` (default false), `desktopTypography(enabled)` switching between system font and Violence styling, settings UI controls in Appearance settings tab, `DesktopStyleParityTest` (361 tests pass).
  - Web integration: TypeScript port of `AccentMath`, presets, token resolver, and reactive store (`useViolenceStyle`) with `localStorage` persistence, Appearance section in `SettingsPage.tsx`, unit tests (`violenceStyle.test.ts` - 8 tests pass), `pnpm test` (247 tests pass), `pnpm typecheck` (0 errors), `pnpm build:site` (clean build).
  - Strict 100% OFF-parity: default is OFF on both Desktop and Web; zero CSS, DOM changes, or font overrides occur when toggle is disabled.

- [x] **DW-4: Live Background, Glass Chrome & Styled Components (2026-11-04)**
  - Desktop: implemented `DesktopStyleBackground.kt` using Compose Skia hardware canvas, multi-stop radial gradient blobs (Aurora, Corner Glow, Haze), dark vignette, 30 fps frame throttling (`rememberBackgroundClock` with `StyleFrameThrottle`), and flat fallback under reduced transparency / LITE mode.
  - Desktop: implemented `DesktopStyledComponents.kt` with `ViolenceSectionHeader` (display serif, caps, hairline, and hero glowing dot), `violenceCardRim`, `ViolenceCardScrim`, `violencePillColor`, `violencePlayerGlow`, `violenceIndicatorColor`, and glass chrome borders.
  - Desktop: wired into `DesktopApp.kt` (dark luminous canvas `#05050A`, accent-tinted `desktopChromeGlass`, `desktopBarGlass`, `desktopWindowGlass`, sidebar item selected glowing capsule, floating navigation, and mini-player) and `HomeScreen.kt` (section headers, hero cards).
  - Desktop tests: expanded `DesktopStyleParityTest.kt` with tests for scheme switching, glass colors, geometry, text splitting, and scrim curve (366 tests pass in `:desktopApp:test`, `:desktopUi:jvmTest` PASS).
  - Web: implemented `ViolenceBackground.tsx` (LITE Style on Web via CSS radial gradient blobs for Aurora, Corner Glow, Haze, dark vignette, and hardware-accelerated drifting transforms).
  - Web: implemented `ViolenceSectionHeader.tsx` (display serif, caps, hairline, and hero glowing dot).
  - Web: added design tokens and glass utilities in `tokens.css` and `global.css` under `[data-violence-style='true']` (forced dark canvas, accent-tinted glass surfaces, `glass-pill` bloom, sidebar selected capsule indicator).
  - Web: unit tests in `violenceStyle.test.ts` (9 tests pass, 248 total tests pass), `pnpm typecheck` clean, `pnpm build:site` clean.
  - Android & Shared: `:app:testDevDebugUnitTest` (63 tasks, 0 regressions), `:shared:jvmTest` (245 tests pass).
  - Strict 100% OFF-parity: when toggle is OFF, zero rendering or behavioral differences across Desktop, Web, and Mobile.

- [ ] **DW-5: Platform Extras and Polish**
  - Desktop system tray integration, media keys (SMTC / MPRIS), and keyboard shortcuts.
  - Desktop update check against `update.json`.
  - Web PWA manifest and theme color integration.
  - Packaging verification for Windows (`.msi` with unchanged `upgradeUuid`).

- [ ] **DW-6: Final Audit, Accessibility, Performance & Docs Handover**
  - End-to-end smoke test across Phone, TV, Web, and Desktop.
  - Accessibility audit (WCAG AA contrast).
  - Update all project documents (`PHASES.md`, `PROJECT_STATE.md`, `README.md`).
  - Final project sign-off and handover.

