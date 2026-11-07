<#
.SYNOPSIS
  Draws the Quote share cards to PNG files on this PC, so a design can be looked at without
  a phone. Output: app\build\card-previews\ . These are JVM renders with a made-up cover, not
  device screenshots.
#>
$ErrorActionPreference = 'Continue'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
. "$PSScriptRoot\_env.ps1"
Use-AndroidToolchain
$env:VIOLENCE_PREVIEWS = '1'
& .\gradlew.bat ':app:testDevDebugUnitTest' '--tests' 'com.music.violence.ui.preview.CardPreviewTest' '-Pviolence.bundleWear=false' --console=plain
Write-Host "PNGs: $root\app\build\card-previews"
