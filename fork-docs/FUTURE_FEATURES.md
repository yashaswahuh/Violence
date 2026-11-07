# Future Feature Backlog & Ideas

Documented backlog of features approved for future implementation cycles.

---

## 1. Fullscreen Karaoke & Sing Mode (Desktop & Android TV)

- **Approved Date:** 2026-11-04
- **Target Platforms:** Desktop (`:desktopApp`), Android TV (`:tv`)
- **Status:** Deferred / Backlogged (do not implement in current phase)

### Concept & Design
On widescreen displays (TV and Desktop monitors), standard single-column lyrics leave substantial unused horizontal real estate. Fullscreen Karaoke Mode adapts the layout for communal or living room singing.

### Planned Capabilities
1. **Multi-Column Vocal Separation:**
   - Detect dual-singer, duet, or backing vocal markers in LRC/TTML metadata.
   - Present primary lead vocals on the left and backing vocals/harmonies on the right (similar to Apple Music Sing).
2. **Dynamic Focus & Smooth Word Animation:**
   - Enlarged, fluidly scrolling typography using Violence Style display serifs or crisp modern sans-serifs.
   - Syllable/word-level fill animation with blooming glow effects on the active vocal line.
3. **Immersive Living Room Aesthetic:**
   - Fullscreen mode hides standard navigation bars and controls, fading in playback controls on mouse move / D-pad click.
   - Dynamic album-art-tinted glass backing or motion artwork.
4. **TV Remote / Gamepad Integration:**
   - Quick jump between verses using D-pad Left/Right or media skip buttons.
   - Dedicated toggle button in TV Now Playing screen.



---

## 2. Bit-Perfect WASAPI Exclusive Mode (Desktop USB DACs)

- **Date Logged:** 2026-11-05
- **Target Platforms:** Desktop (`:desktopApp`)
- **Status:** Deferred to Future Cycle

### Concept & Design
On Windows, shared-mode audio passes all streams through the Windows Audio Engine mixer, resampling everything to the OS default rate and applying system limiters. Audiophiles using dedicated external USB DACs (FiiO, Topping, Schiit, iFi, Apple USB-C Dongle) benefit from bit-perfect bitstream delivery.

### Planned Capabilities
1. Pass `AUDCLNT_SHAREMODE_EXCLUSIVE` to WASAPI initialization in `DesktopWindowsAudio.kt`.
2. Add toggle in Desktop Audio Settings: `"WASAPI Exclusive Mode (Bit-Perfect)"`.
3. Dynamically adjust hardware endpoint sample rate to match track container (44.1 kHz, 48 kHz, 96 kHz, 192 kHz).
4. Graceful fallback to shared mode when another application holds exclusive audio locks.

---

## 3. Local Music Folder Auto-Watcher (Desktop Java NIO)

- **Date Logged:** 2026-11-05
- **Target Platforms:** Desktop (`:desktopApp`)
- **Status:** Deferred to Future Cycle

### Concept & Design
Automatically update the desktop local music library when files are copied, modified, or deleted without requiring manual rescanning or app restarts.

### Planned Capabilities
1. Implement `DesktopLocalMusicWatcher.kt` utilizing `java.nio.file.WatchService`.
2. Recursively monitor configured music folders for `ENTRY_CREATE`, `ENTRY_MODIFY`, and `ENTRY_DELETE`.
3. 500ms sliding-window debouncing to prevent thrashing during large album imports.
4. Instant local library index updates with zero UI blocking.

---

## 4. Global Media Key Shortcuts & Tray Mouse-Wheel Volume

- **Date Logged:** 2026-11-05
- **Target Platforms:** Desktop (`:desktopApp`)
- **Status:** Deferred to Future Cycle

### Concept & Design
Provide convenient playback and volume controls for compact mechanical keyboard users (60%, 65%, 75% layouts without media keys) and fast volume adjustment without opening the main window.

### Planned Capabilities
1. Configurable system-wide global hotkeys via Win32 `RegisterHotKey`:
   - `Ctrl + Alt + Space`: Play / Pause
   - `Ctrl + Alt + Right / Left`: Next / Prev Track
   - `Ctrl + Alt + Up / Down`: Volume ±5%
2. Mouse-wheel volume adjustments on the system tray icon and PiP Mini-Player with a 1.2s micro HUD volume pill overlay.

---

## 5. Local M3U/M3U8 Playlist Two-Way Import & Export

- **Date Logged:** 2026-11-05
- **Target Platforms:** Shared Core (`:shared`), Desktop (`:desktopApp`), Android (`:app`)
- **Status:** Deferred to Future Cycle

### Concept & Design
Allow seamless interoperability with third-party local music managers (Foobar2000, MusicBee, iTunes, VLC) and DAP hardware.

### Planned Capabilities
1. Standard UTF-8 M3U parser supporting `#EXTM3U`, `#EXTINF`, and relative/absolute track paths.
2. Drag-and-drop `.m3u` / `.m3u8` file import onto Desktop window to create or queue playlists.
3. `"Export as M3U8 File"` action in playlist context menu with relative path generation for portable USB drive playback.
