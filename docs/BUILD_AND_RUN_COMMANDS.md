# Violence: Complete Build & Run Command Reference

This document compiles every single command to build the mobile and TV apps, compile and run the desktop app, and develop, build, test, and serve the web application.

---

## Prerequisites & Environment Setup

### Toolchain Requirements
- **OS**: Windows (PowerShell recommended), macOS, or Linux.
- **Java**:
  - **JDK 17**: Required for building Android targets (`:app`, `:tv`).
  - **JDK 21**: Required by Gradle for desktop target configuration and execution (`:desktopApp`, `:desktopUi`).
- **Android SDK**: API Platform 36 & 37.0, NDK (version 27), CMake 3.22.1.
- **Node.js & Package Manager**: Node.js 22+, `pnpm` (version 12.x recommended via `corepack`).
- **Desktop Windows Packaging** (Optional, for MSI/EXE installers): [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases) must be in your system `PATH`.

---

## 1. Quick Helper Scripts (Windows PowerShell)

The repository provides automated scripts in `.\scripts\` that configure your session environment and run isolated tasks safely without touching frozen targets:

```powershell
# Build all active targets (Phone + TV + Web)
.\scripts\build-active.ps1

# Build individual targets
.\scripts\build-active.ps1 -Target phone
.\scripts\build-active.ps1 -Target tv
.\scripts\build-active.ps1 -Target web

# Run tests across active targets
.\scripts\test-active.ps1
.\scripts\test-active.ps1 -Target web
```

---

## 2. Android Phone & Android TV App Builds

> **Important Rule**: **Never run root-level Gradle tasks** (e.g. `.\gradlew.bat assemble` or `build`). Always specify the exact project task path. In PowerShell, ensure the `-P` parameter is quoted as `'-Pviolence.bundleWear=false'` to avoid PowerShell splitting on the dot.

### Android Phone (`:app`)

#### Assemble Debug APKs
```powershell
.\gradlew.bat :app:assembleDevDebug '-Pviolence.bundleWear=false'
```
*Output directory:* `app/build/outputs/apk/dev/debug/` (produces universal, arm64-v8a, armeabi-v7a, and x86_64 APKs).

#### Run Phone Unit Tests
```powershell
.\gradlew.bat :app:testDevDebugUnitTest '-Pviolence.bundleWear=false'
```

#### Assemble Release APKs (Requires Keystore)
```powershell
.\gradlew.bat :app:assembleDevRelease '-Pviolence.bundleWear=false'
.\gradlew.bat :app:assembleProdRelease
```

#### Clean Phone Build Cache Only
```powershell
.\gradlew.bat :app:clean
```

---

### Android TV (`:tv`)

#### Assemble TV Debug APK
```powershell
.\gradlew.bat :tv:assembleDevDebug '-Pviolence.bundleWear=false'
```
*Output directory:* `tv/build/outputs/apk/dev/debug/`

#### Assemble TV Release APK (Requires Keystore)
```powershell
.\gradlew.bat :tv:assembleDevRelease '-Pviolence.bundleWear=false'
```

#### Clean TV Build Cache Only
```powershell
.\gradlew.bat :tv:clean
```

---

### Shared Modules (`:shared`)

#### Run Shared JVM Unit Tests
```powershell
.\gradlew.bat :shared:jvmTest '-Pviolence.bundleWear=false'
```

---

## 3. Desktop Application (`:desktopApp` & `:desktopUi`)

Compose Multiplatform JVM application targeting Windows, macOS, and Linux. Requires **JDK 21**.

### Run the Desktop App Directly in Development Mode
```powershell
# Windows
.\gradlew.bat :desktopApp:run

# macOS / Linux
./gradlew :desktopApp:run
```

### Compile & Run Desktop Tests
```powershell
# Desktop UI module compilation & tests
.\gradlew.bat :desktopUi:compileKotlinJvm
.\gradlew.bat :desktopUi:jvmTest

# Desktop App module compilation & tests
.\gradlew.bat :desktopApp:compileKotlin
.\gradlew.bat :desktopApp:test
```

### Build Standalone Executables & Installers

#### Create Unpacked Standalone Binary (Fastest)
```powershell
# Windows: Output at desktopApp\build\compose\binaries\main\app\Violence\Violence.exe
.\gradlew.bat :desktopApp:createDistributable

# macOS: Output at desktopApp/build/compose/binaries/main/app/Violence.app
./gradlew :desktopApp:createDistributable

# Linux: Output at desktopApp/build/compose/binaries/main/app/Violence
./gradlew :desktopApp:createDistributable
```

#### Windows Installers & Portable Zip
Requires [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases) on `PATH` for `.exe` and `.msi`.
```powershell
# Windows Executable Installer (.exe)
.\gradlew.bat :desktopApp:packageExe

# Windows MSI Installer (.msi)
.\gradlew.bat :desktopApp:packageMsi

# Windows Portable Zip
.\gradlew.bat :desktopApp:windowsPortable
```

#### macOS Packages
Must be run on macOS:
```bash
# macOS Disk Image (.dmg)
./gradlew :desktopApp:packageDmg

# macOS Installer (.pkg)
./gradlew :desktopApp:packagePkg
```
*Tip: If macOS Gatekeeper blocks local unsigned binaries, run: `xattr -cr /Applications/Violence.app`*

#### Linux Packages
Must be run on Linux (requires `rpm`, `fakeroot`, `binutils`, and `file`):
```bash
# Debian / Ubuntu (.deb)
bash ./gradlew :desktopApp:packageDeb

# Fedora / RHEL (.rpm)
bash ./gradlew :desktopApp:packageRpm

# Linux AppImage
bash ./gradlew :desktopApp:createDistributable
desktopApp/packaging/appimage.sh desktopApp/build/compose/binaries/main/app/Violence dist/Violence-linux-x86_64.AppImage
```

---

## 4. Web Application & Browser Extension (`/web`)

The web client is built with Vite, TypeScript, and pnpm. It pairs with the companion browser extension in `web/extension`.

### Preparation & Dependencies
```powershell
# Ensure corepack enables pnpm (if pnpm is not in PATH)
corepack enable pnpm

# Install web workspace dependencies from project root
corepack pnpm --dir web install
```

---

### Running the Web Version in Development

#### 1. Run the Web App Frontend Dev Server
Runs the web app locally with hot reloading (default URL: `http://localhost:5173/`):
```powershell
corepack pnpm --dir web dev
```
*(Alternative from inside the `web` folder: `cd web; pnpm dev`)*

#### 2. Run the Browser Extension in Dev Mode (Watch Mode)
```powershell
# Chrome / Edge / Chromium
corepack pnpm --dir web dev:ext

# Firefox
corepack pnpm --dir web dev:ext:firefox
```

---

### Building the Web Version for Production

#### Typecheck & Tests
```powershell
# Run TypeScript typechecks across web workspace
corepack pnpm --dir web typecheck

# Run web test suite
corepack pnpm --dir web test
```

#### Full Site & Extension Build
Packages the browser extension into `.zip` files and builds the Vite production app:
```powershell
corepack pnpm --dir web build:site
```
*Output directory:* `web/app/dist`

#### Production Build for Custom Base URL (e.g., GitHub Pages)
When deploying to a subfolder such as GitHub Pages (`/Violence/`):
```powershell
# PowerShell
$env:VITE_BASE_URL='/Violence/'
$env:BITCHORD_STORE_BUILD='1'
corepack pnpm --dir web build:site
```

```bash
# Bash
VITE_BASE_URL='/Violence/' BITCHORD_STORE_BUILD='1' corepack pnpm --dir web build:site
```

---

### Serving & Testing the Web Build Locally

#### Emulate GitHub Pages Locally
A script is provided to serve the built `web/app/dist` under `/Violence/` with matching routing and 404 redirection behavior:
```powershell
# 1. Automated HTTP routes and assets verification check
node scripts/serve-pages-local.mjs --check

# 2. Local interactive server (serves at http://localhost:4173/Violence/)
node scripts/serve-pages-local.mjs
```

---

## 5. Summary Cheat Sheet

| Task | Command |
|---|---|
| **Build Phone APK** | `.\gradlew.bat :app:assembleDevDebug '-Pviolence.bundleWear=false'` |
| **Build TV APK** | `.\gradlew.bat :tv:assembleDevDebug '-Pviolence.bundleWear=false'` |
| **Run Desktop App** | `.\gradlew.bat :desktopApp:run` |
| **Build Desktop .exe (standalone)** | `.\gradlew.bat :desktopApp:createDistributable` |
| **Build Desktop Installer** | `.\gradlew.bat :desktopApp:packageMsi` (or `packageExe`) |
| **Run Web Dev Server** | `corepack pnpm --dir web dev` |
| **Build Web Production Site** | `corepack pnpm --dir web build:site` |
| **Serve Web Production Locally** | `node scripts/serve-pages-local.mjs` |

