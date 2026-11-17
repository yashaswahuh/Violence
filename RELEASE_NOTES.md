# Violence v0.9.5 (Pre-release)

Violence is an unofficial, privately maintained, modified fork of BitChord by Kushagra Singh, licensed under GPL-3.0. Features an Apple Music-inspired UI with live synchronized lyrics, party listening, native system controls, and multiplatform support.

---

### What's New in v0.9.5

#### 🖥️ Desktop (Windows, macOS, Linux)
- **Apple Music-Style Mini Player**: Compact floating miniplayer with real-time synchronized animated lyrics, playback controls, and artwork visualization.
- **Liquid Frosted Glass UI**: Modern translucent, acrylic blurred styling across surfaces with reactive glowing accents based on artwork palette.
- **Windows System Media Transport Controls (SMTC)**: Native Windows integration with proper application identification, media buttons, live timeline seeking, and high-resolution album artwork.
- **Full Context Menus & Playlist Management**: Complete queue interaction (Play Next, Add to Queue, Remove, Reorder), track actions, and full context menu options.
- **Installer & Portable Distributions**:
  - Windows: Modern custom dark-themed Inno Setup installer (`.exe`), WiX MSI (`.msi`), portable archive (`.zip`), and standalone binary.
  - Linux: Standalone AppImage (`.AppImage`), Debian package (`.deb`), and Red Hat package (`.rpm`).
  - macOS: Apple Disk Image (`.dmg`), installer package (`.pkg`), and zipped application bundle (`.zip`).

#### 🌐 Web Player & Extension
- **Listen Together Fast-Path**: Direct endpoint dispatch eliminates artificial loading delays when joining or creating parties.
- **Cloud Server Wake Resilience**: Automated wake-up detection for free-tier cloud Jam servers with backoff polling and strict timeout protections.
- **Direct Code & Invite Link Joining**: Seamless 6-character code input or invite link pasting with server query preservation (`?server=...`).
- **Enforced Canonical HTTPS Origin**: Automatic protocol upgrade to prevent CORS header omissions and WebSocket handshake failures on production domains.
- **Enhanced Error Handling**: Informative feedback when a party has ended or cannot be found, with one-click return to Listen Together settings.

#### 📱 Android & Android TV
- **Production Release APKs**: Optimized release binaries for both phone and TV form factors across all major ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`) and universal APKs.
- **Dual V1 + V2 Signing**: Explicit APK signature schemes enabled for seamless sideloading across all Android versions without package parsing issues.
- **In-App Over-The-Air (OTA) Updates**: Background update checks via `update.json` with cryptographic SHA-256 verification and automatic CPU architecture targeting.
- **Listen Together Jam**: Synchronized playback engine with client-side server clock drift compensation.
- **Last.fm Integration**: Background track scrobbling and playback tracking.

---

### Package Downloads

| Platform | File | Type |
|---|---|---|
| **Android Phone** | `violence-v0.9.5-android-universal.apk` | Universal APK (All Devices) |
| **Android Phone** | `violence-v0.9.5-android-arm64-v8a.apk` | 64-bit ARM |
| **Android Phone** | `violence-v0.9.5-android-armeabi-v7a.apk` | 32-bit ARM |
| **Android Phone** | `violence-v0.9.5-android-x86_64.apk` | 64-bit x86 |
| **Android TV** | `violence-v0.9.5-tv-universal.apk` | Universal TV APK |
| **Android TV** | `violence-v0.9.5-tv-arm64-v8a.apk` | 64-bit ARM TV |
| **Android TV** | `violence-v0.9.5-tv-armeabi-v7a.apk` | 32-bit ARM TV |
| **Android TV** | `violence-v0.9.5-tv-x86_64.apk` | 64-bit x86 TV |
| **Windows** | `violence-v0.9.5-windows-x64-setup.exe` | Setup Installer (Recommended) |
| **Windows** | `violence-v0.9.5-windows-x64-installer.msi` | Windows Installer |
| **Windows** | `violence-v0.9.5-windows-x64-portable.zip` | Portable ZIP |
| **Windows** | `violence-v0.9.5-windows-x64.exe` | Standalone Executable |
| **macOS** | `violence-v0.9.5-macos.dmg` | Disk Image (Drag & Drop) |
| **macOS** | `violence-v0.9.5-macos.pkg` | Installer Package |
| **macOS** | `violence-v0.9.5-macos.zip` | Application Bundle Archive |
| **Linux** | `violence-v0.9.5-linux-x86_64.AppImage` | AppImage |
| **Linux** | `violence-v0.9.5-linux-amd64.deb` | Debian / Ubuntu Package |
| **Linux** | `violence-v0.9.5-linux-x86_64.rpm` | Fedora / RHEL Package |
| **Updates Metadata** | `update.json` | OTA Update Manifest |
