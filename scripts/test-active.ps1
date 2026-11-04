<#
.SYNOPSIS
  Runs the unit tests of ONLY the active targets (Android phone + shared code, web).
  Never touches the dormant modules (wearos, desktopApp, desktopUi) and never runs a
  root-level Gradle task. Android TV has no unit-test task of its own (UNCLEAR if that changes);
  its sources are the phone's, so the phone tests cover them.

.PARAMETER Target
  all (default) | phone | tv | web
  'tv' only builds nothing: it prints a note, because :tv has no unit tests.

.EXAMPLE
  .\scripts\test-active.ps1
  .\scripts\test-active.ps1 -Target web
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

$noWear = '-Pviolence.bundleWear=false'

if ($Target -in 'all', 'phone') {
    Use-AndroidToolchain
    Invoke-Step 'Phone unit tests (:app:testDevDebugUnitTest)' { & .\gradlew.bat ':app:testDevDebugUnitTest' $noWear --console=plain }
    Invoke-Step 'Shared unit tests (:shared:jvmTest)' { & .\gradlew.bat ':shared:jvmTest' $noWear --console=plain }
}
if ($Target -eq 'tv') {
    Write-Host 'TV: no unit-test task of its own; its sources are the phone app, covered by -Target phone.' -ForegroundColor Yellow
}
if ($Target -in 'all', 'web') {
    Use-WebToolchain
    Invoke-Step 'Web unit tests (pnpm --dir web test)' { & pnpm --dir web test }
}

Write-Host ''
if ($failed.Count -gt 0) { Write-Host ("FAILED steps: " + ($failed -join '; ')) -ForegroundColor Red; exit 1 }
Write-Host "All requested active-target tests passed ($Target). Wear OS and desktop: dormant, not checked." -ForegroundColor Green
