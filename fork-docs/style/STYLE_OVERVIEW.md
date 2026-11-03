# Violence Style: one-page overview for contributors

**What it is.** An optional, experimental *skin* for the phone app (`:app` plus the shared `:sharedUi` components it uses). Off by default. It changes typography, adds a live glowing background, decorates headings and hero cards, tints the glass bars and controls, and colours accents from the playing song, the device or a chosen colour. It never moves, removes or renames a control, screen or gesture, and with the switch off the app is exactly the app without it.

**Where it lives.**
- `app/src/main/java/com/music/bitchord/ui/violencestyle/`: the state (`StyleModel`), the settings and safe mode (`StyleSettings`, `StyleSafeMode`), the accent maths and engine (`AccentMath`, `AccentEngine`), capability rules (`StyleCapability`), the live background and clock (`StyleBackground*`), fonts and typography (`StyleTypography`), glass helpers (`StyleGlass`, `StyleChrome`), the theme host (`StyleHost`) and the Settings UI and preview (`StyleSettingsUi`).
- `sharedUi/.../ui/violencestyle/`: `StyleHooks.kt` (`LocalViolenceSeeThrough`) and `StyledComponents.kt` (`LocalViolenceShared`, the section header, card rim and scrim). `:sharedUi` cannot see `:app`, so the app provides the style through composition locals.
- Fonts: `app/src/main/res/font/vs_*.ttf` (all SIL OFL 1.1; licences in `NOTICE` and `fork-docs/style/licenses/`).
- Tests: `app/src/test/.../ui/violencestyle/` (pure logic: colours, contrast guard, geometry, capability, safe mode, OFF parity of modifiers).
- Docs: `fork-docs/style/` (`STYLE_SPEC`, `STYLE_PLAN`, `STYLE_COVERAGE`, the stage reviews, `STYLE_A11Y_AUDIT`, `STYLE_PERF`, `STYLE_FINAL_QA`, `FIDELITY_REVIEW`).

**How the OFF path is guaranteed.**
1. Every composition local defaults to "off" (`LocalViolenceStyle` = DISABLED, `LocalViolenceShared` = null, `LocalViolenceBackground` = null, `LocalViolenceSeeThrough` = false), so anything outside the provider renders the original.
2. `ViolenceStyledTheme` is exactly `BitChordTheme { content }` while off.
3. Every call site in an upstream file is an early branch or a swapped expression whose else path is the original code; modifier helpers return their receiver (or the original `background`/`border`) when off. Unit tests assert this for the modifiers.
4. Settings default to off and are stored under `violence_style.*` keys in the existing settings file.

**How to add a styled element.** Put the logic in a pure function with unit tests; read `LocalViolenceStyle` (app) or `LocalViolenceShared` (shared UI) at the call site and fall through to the original code when it is off; never read settings directly in a component; keep drawing to cached brushes and strokes (no blur, no per-item clocks); keep contrast numbers in tests (see `StyleS3Test`, `StyleS4Test`).

**Upstream sync.** The files the skin edits and how to resolve conflicts are in `fork-docs/UPSTREAM_SYNC.md` ("Violence Style touch points").

**Report a problem.** Open an issue (or tell the maintainer) with the device, Android version, and the settings (background, motion, intensity, accent source, heading font, reduce transparency). Turning the switch off, or two failed starts in a row (safe mode), always returns the app to its normal look.
