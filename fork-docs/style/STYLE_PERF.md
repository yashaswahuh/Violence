# Violence Style: performance review (S6)

**What was and was not measured.** No profiler, emulator or adb exists on the build PC, so there are no frame-time or memory numbers from me. Everything below is read from the code. The owner reviewed S2 to S5 on a phone and reported smooth behaviour; the frame-stats procedure (`adb shell dumpsys gfxinfo <package> framestats`) is in the stage reviews and in `STYLE_FINAL_QA.md`.

## Cost by setting
| Setting | Cost (reasoned) |
|---|---|
| Master OFF | None. No provider, no clock, no extra composable state; one `StateFlow<Boolean>` is collected. Startup does one cheap check (below) |
| Master ON, background Off | Typography, accent engine and chrome only; no clock, no background layer |
| Background Aurora / Corner / Haze, motion Off | One static frame drawn once into one `GraphicsLayer`; no invalidation afterwards |
| Motion Subtle / Normal | One clock at 30 fps redrawing one node on its own layer: at most 3 radial-gradient rects (one cached brush) and 1 linear-gradient rect per frame, no allocation per frame. With Liquid Glass on, the glass surfaces re-evaluate their blur on each tick (the main unknown) |
| Glow intensity | Only alpha and radius scale; no extra draw calls. High also drifts 1.6x |
| Accent source Song | One palette read per song (cached), debounced 400 ms; an 800 ms colour animation recomposes the few readers of the accent (headings, the first card, the capsule) |
| Reduce transparency | Cheaper: the glow becomes one static gradient, glass tint near-solid |
| LITE mode (API below 31, low RAM, battery saver, thermal, Reduce dynamic blur) | Static background, no clock |

## Limits and layers
- **Glass layers per screen:** unchanged from before the skin. The nav bar, mini player and Search circle are the app's existing glass surfaces (up to 3 at rest, up to 6 mid-fold); the top pill and back button add the app's existing 2. The skin passes a tint and adds a border; it adds **no** blurred layer. Dialogs and sheets use the app's existing single still-based panel.
- **Background:** one layer for the whole app; the glass recording draws the same layer again as its floor (one extra blit, not a second render).
- **Throttle:** the real tick interval is averaged over 30 ticks; if it exceeds 1.4x the intended one the rate falls 30, 20, 10 fps, then static; it never rises again in that session.
- **Lists:** no per-item clock or animation was added; the first Top Picks card computes one `drawWithCache` rim; the front-card index is one `derivedStateOf` that changes only when the row settles on another card; Recents rows only change a corner size and a colour.

## Memory
- Fonts: Inter (876 KB variable) loads when the skin is on; Bodoni Moda (162 KB + 176 KB), Cormorant Garamond (1.2 MB + 0.7 MB) and Anton (171 KB) load lazily when selected. The APK grows by about 3.3 MB (fonts bundled as resources).
- One extra `GraphicsLayer` the size of the screen (about one screen-sized bitmap's worth of GPU memory) while a background is shown.

## Startup
With the skin ON, `MainActivity.onCreate` reads the settings file (already loaded by the app), parses 9 values, and performs one **synchronous** `SharedPreferences.commit()` (the safe-mode counter must survive a crash). That is a small disk write on the main thread, typically a few milliseconds; a second small write follows after the first frame. With the skin OFF it still reads the counter once and writes nothing unless the counter was non-zero. Not measured.

## Battery (reasoning)
The only continuous work is the 30 fps clock. It stops when the app is not resumed, when the full player covers the page, under reduce motion, in LITE mode, and when motion is Off. A static background and the colour animations are one-off. The honest upper bound: Normal motion with Liquid Glass on keeps the GPU busy at 30 fps while the app is open and idle; the owner should compare battery with motion Off.

## Fixed in S6
- Toggling the style no longer rebuilds the app's UI subtree (movable content), which also removes a one-time recomposition of the whole app on every toggle.

## Not done
- No idle-stop after 30 s without touch (needs a global touch hook); the clock only stops for the reasons above.
- No scroll-linked parallax (skipped on purpose in S2).
