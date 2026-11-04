<#
.SYNOPSIS
  Builds ONLY the active targets (Android phone, Android TV, web). Never touches the dormant
  modules (wearos, desktopApp, desktopUi) and never runs a root-level Gradle task.

.PARAMETER Target
  all (default) | phone | tv | web

.EXAMPLE
  .\scripts\build-active.ps1
  .\scripts\build-active.ps1 -Target phone
#>
param(
    [ValidateSet('all', 'phone', 'tv', 'web')]
    [string]$Target = 'all'
)

# Native tools (java, gradle, pnpm) write normal output to stderr; PowerShell 5.1 would turn that into errors under 'Stop'.
# Success or failure is decided by $LASTEXITCODE instead.
$ErrorActionPreference = 'Continue'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

. "$PSScriptRoot\_env.ps1"
Assert-OnBranch

$failed = @()

function Invoke-Step([string]$Name, [scriptblock]$Body) {
    Write-Host "=== $Name ===" -ForegroundColor Cyan
    & $Body
    if ($LASTEXITCODE -ne 0) { $script:failed += $Name; Write-Host "FAILED: $Name (exit $LASTEXITCODE)" -ForegroundColor Red }
    else { Write-Host "OK: $Name" -ForegroundColor Green }
}

# '-Pviolence.bundleWear=false' keeps :app from building the Wear OS APK (dormant target).
$noWear = '-Pviolence.bundleWear=false'

if ($Target -in 'all', 'phone') {
    Use-AndroidToolchain
    Invoke-Step 'Android phone debug (:app:assembleDevDebug)' { & .\gradlew.bat ':app:assembleDevDebug' $noWear --console=plain }
}
if ($Target -in 'all', 'tv') {
    Use-AndroidToolchain
    Invoke-Step 'Android TV debug (:tv:assembleDevDebug)' { & .\gradlew.bat ':tv:assembleDevDebug' $noWear --console=plain }
}
if ($Target -in 'all', 'web') {
    Use-WebToolchain
    Invoke-Step 'Web typecheck' { & pnpm --dir web typecheck }
    Invoke-Step 'Web site build (pnpm --dir web build:site)' { & pnpm --dir web build:site }
}

Write-Host ''
if ($failed.Count -gt 0) { Write-Host ("FAILED steps: " + ($failed -join '; ')) -ForegroundColor Red; exit 1 }
Write-Host "All requested active-target builds passed ($Target). Wear OS and desktop: dormant, not checked." -ForegroundColor Green
