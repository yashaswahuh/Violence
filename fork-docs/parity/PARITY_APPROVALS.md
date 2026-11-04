# Parity Stage Approvals (DW Stages)

**Project:** Violence ("Desktop and Web Parity", DW-0 to DW-6)  
**Rule:** Each stage may not start until the previous stage is ticked and approved by the owner.

---

- [x] **DW-0: Audit, Parity Matrix, Gap Plan & Architecture Specifications (2026-11-04)**
  - Documents written: `PARITY_AUDIT.md`, `PARITY_DECISIONS.md`, `PARITY_APPROVALS.md`.
  - Compile health checked: `:desktopUi:compileKotlinJvm` (PASS), `:desktopUi:jvmTest` (PASS, 42 tests), `:desktopApp:compileKotlin` (FAIL - documented), `:shared:jvmTest` (PASS, 234 tests), `web typecheck` (PASS), `web test` (PASS, 239 tests), `web build:site` (PASS).
  - No application code, build files, or resources modified.

- [ ] **DW-1: Desktop Compile Health & Unfreeze Housekeeping**
  - Fix single compilation error in `DesktopApp.kt:4223:33` (non-exhaustive `when`).
  - Run `:desktopApp:test` (57 test suites) and verify 100% pass rate.
  - Verify `.\gradlew.bat :desktopApp:createDistributable` generates runnable desktop binary.
  - On-device test: launch desktop app locally with `.\gradlew.bat :desktopApp:run`.

- [ ] **DW-2: Desktop and Web Logic Parity**
  - Modernize Desktop Jam (`JAM_BACKEND_URL`, 85s wake-up, 15 members, link extraction, stalled-track isolation).
  - Add in-app profile creation, avatar picker, and local friends storage to Desktop.
  - Add song, playlist, and Jam sharing dialogs with QR code rendering to Desktop.
  - Verify Web profile and sharing dialog confirmations.
  - Review on Desktop and Web: create profile, join Jam party between Phone and Desktop.

- [ ] **DW-3: Violence Style Foundation (Desktop & Web)**
  - Move pure `AccentMath`, `AccentEngine`, `StyleModel` into `:shared`.
  - Add style toggle and settings schema to Desktop and Web (default OFF).
  - Configure SIL OFL fonts (Bodoni Moda, Cormorant Garamond, Inter) on Desktop and Web.
  - Verify 100% OFF-parity: visual output is identical to upstream when toggle is OFF.

- [ ] **DW-4: Live Background, Glass Chrome & Styled Components**
  - Implement radial gradient background (Corner Glow, Aurora, Haze) on Desktop and Web.
  - Implement accent-tinted glass surfaces and rims (LITE mode first).
  - Verify visual appearance and frame rates on Desktop and Web browser.

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
