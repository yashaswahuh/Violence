# Shared helpers for scripts/*.ps1 (dot-sourced). Sets up the toolchain for THIS PowerShell session only;
# nothing is installed or changed globally. Contains no secrets.

function Assert-OnBranch {
    # Branch rule: work happens on violence-nk.
    $branch = (git branch --show-current).Trim()
    if ($branch -ne 'violence-nk') {
        Write-Host "You are on '$branch', not violence-nk. Switch with: git switch violence-nk" -ForegroundColor Yellow
        Write-Host 'Continuing, because building does not change the repository, but commits and pushes must go to violence-nk.' -ForegroundColor Yellow
    }
}

function Use-AndroidToolchain {
    # Android Gradle Plugin 8.10 needs JDK 17.
    if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe") -or ((& cmd /c ""`"$env:JAVA_HOME\bin\java.exe`" -version 2>&1"" | Out-String) -notmatch 'version "17\.')) {
        $candidates = @()
        $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue
        $candidates += Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue
        $candidates += Get-ChildItem 'C:\Program Files\Android\Android Studio\jbr' -Directory -ErrorAction SilentlyContinue
        $jdk = $candidates | Select-Object -First 1
        if (-not $jdk) { throw 'JDK 17 not found. Install it (winget install EclipseAdoptium.Temurin.17.JDK) or set JAVA_HOME to a JDK 17.' }
        $env:JAVA_HOME = $jdk.FullName
    }
    if (-not $env:ANDROID_HOME -and -not (Test-Path 'local.properties')) {
        $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
        if (-not (Test-Path $sdk)) { throw 'Android SDK not found. Set ANDROID_HOME or create local.properties with sdk.dir.' }
        $env:ANDROID_HOME = $sdk
    }
    # :desktopApp is part of the Gradle build and declares a JDK 21 toolchain, so Gradle needs one on disk for ANY build.
    $has21 = @(Get-ChildItem 'C:\Program Files\Eclipse Adoptium','C:\Program Files\Java',(Join-Path $env:USERPROFILE '.jdks') -Directory -Filter '*21*' -ErrorAction SilentlyContinue).Count -gt 0
    if (-not $has21) { Write-Host 'WARNING: no JDK 21 found. Gradle will fail at configuration (desktop toolchain). Install Temurin 21 (see fork-docs/BUILD_COMMANDS.md).' -ForegroundColor Yellow }
    Write-Host "JAVA_HOME=$env:JAVA_HOME"
}

function Use-WebToolchain {
    # The web scripts call plain `pnpm`; if it is not on PATH, expose the corepack one for this session.
    if (-not (Get-Command pnpm -ErrorAction SilentlyContinue)) {
        $shim = Join-Path $env:TEMP 'pnpm-shim'
        New-Item -ItemType Directory -Force $shim | Out-Null
        corepack enable --install-directory $shim pnpm | Out-Null
        $env:PATH = "$shim;$env:PATH"
    }
    # The Jam server for the web build comes from the environment (VITE_PARTY_SERVER_URL), or JAM_BACKEND_URL
    # if that is set; empty is fine, the site then says Jam is not set up.
    if (-not $env:VITE_PARTY_SERVER_URL -and $env:JAM_BACKEND_URL) { $env:VITE_PARTY_SERVER_URL = $env:JAM_BACKEND_URL }
    if (-not $env:VITE_BASE_URL) { $env:VITE_BASE_URL = '/Violence/' }
}
