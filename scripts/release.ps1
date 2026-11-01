<#
.SYNOPSIS
  Builds the signed phone and TV release APKs, writes the update file the apps read, and prints
  the exact steps to publish them. Android phone + TV only (never the dormant modules, never a
  root-level Gradle task).

.DESCRIPTION
  Needs keystore.properties (gitignored) pointing at your release keystore, which lives OUTSIDE the
  repository. See keystore.properties.example and fork-docs/RELEASE_GUIDE.md. This script never
  creates a keystore, never stores or prints a password and never commits or pushes anything: the
  only file it can change in the repository is the version in build.gradle.kts (with -Bump or
  -VersionName), and it tells you to commit that yourself.

  Output (release-out/<version>/, gitignored):
    violence-phone-<version>.apk
    violence-tv-<version>.apk
    update.json                  <- what the apps' update check reads

.PARAMETER Bump
  Add 1 to the versionCode and to the number after "-violence." in the versionName
  (1.9.0-violence.1 -> 1.9.0-violence.2) before building.

.PARAMETER VersionName
  Set the versionName explicitly (for example 1.10.0-violence.1 after pulling a new upstream
  version) and add 1 to the versionCode. Cannot be combined with -Bump.

.PARAMETER Changelog
  Lines shown in the update dialog ("what is new"). Default: a one-line generic note.

.PARAMETER UpdateFolderUrl
  Where the update files will be on the web, WITHOUT the file name, for example
  https://yashaswahuh.is-a.dev/Violence/updates . Only used to print the final addresses; the
  APK addresses inside update.json are relative to update.json itself.

.PARAMETER SkipBuild
  Reuse the APKs from the last Gradle build (only for re-writing update.json).

.EXAMPLE
  .\scripts\release.ps1 -Bump -Changelog 'Fixed the queue', 'New Jam invite screen'
#>
param(
    [switch]$Bump,
    [string]$VersionName,
    [string[]]$Changelog = @('Bug fixes and improvements.'),
    [string]$UpdateFolderUrl,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Continue'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
. "$PSScriptRoot\_env.ps1"
Assert-OnBranch

function Stop-Release([string]$Message) { Write-Host "STOP: $Message" -ForegroundColor Red; exit 1 }

if ($Bump -and $VersionName) { Stop-Release 'Use either -Bump or -VersionName, not both.' }

# ---- 1. The keystore: required, outside the repository, never created here -----------------------------------
$propsFile = Join-Path $root 'keystore.properties'
$keytoolHint = 'Create it once (PowerShell, keytool asks for the passwords): ' +
    'New-Item -ItemType Directory -Force "$env:USERPROFILE\violence-keys"; ' +
    'keytool -genkeypair -v -keystore "$env:USERPROFILE\violence-keys\violence-release.jks" -alias violence -keyalg RSA -keysize 2048 -validity 10000' +
    "`nThen copy keystore.properties.example to keystore.properties and fill it in. Back the keystore up (fork-docs/RELEASE_GUIDE.md)."
if (-not (Test-Path $propsFile)) { Stop-Release "keystore.properties not found in the repository root.`n$keytoolHint" }
$props = @{}
foreach ($line in Get-Content $propsFile) {
    if ($line -match '^\s*([A-Za-z.]+)\s*=\s*(.*)$') { $props[$Matches[1]] = $Matches[2].Trim() }
}
foreach ($k in 'storeFile', 'storePassword', 'keyAlias', 'keyPassword') {
    if (-not $props[$k]) { Stop-Release "keystore.properties has no value for $k." }
}
$storePath = if ([IO.Path]::IsPathRooted($props['storeFile'])) { $props['storeFile'] } else { Join-Path $root $props['storeFile'] }
if (-not (Test-Path $storePath)) { Stop-Release "The keystore file does not exist: $storePath`n$keytoolHint" }
$storeFull = [IO.Path]::GetFullPath($storePath)
if ($storeFull.StartsWith([IO.Path]::GetFullPath($root), [StringComparison]::OrdinalIgnoreCase)) {
    Write-Host 'WARNING: the keystore is inside the repository folder. Move it outside (it is gitignored, but a folder copy or zip would still carry it).' -ForegroundColor Yellow
}
Write-Host "Keystore: $storeFull (alias $($props['keyAlias']))"

# ---- 2. Version: one place, root build.gradle.kts ---------------------------------------------------------------
$gradleFile = Join-Path $root 'build.gradle.kts'
$gradleRaw = [IO.File]::ReadAllText($gradleFile)
if ($gradleRaw -notmatch 'extra\["bitchordVersionCode"\]\s*=\s*(\d+)') { Stop-Release 'bitchordVersionCode not found in build.gradle.kts.' }
$code = [int]$Matches[1]
if ($gradleRaw -notmatch 'extra\["bitchordVersionName"\]\s*=\s*"([^"]+)"') { Stop-Release 'bitchordVersionName not found in build.gradle.kts.' }
$name = $Matches[1]
$versionChanged = $false
if ($Bump -or $VersionName) {
    $newCode = $code + 1
    if ($VersionName) { $newName = $VersionName }
    elseif ($name -match '^(.*-violence\.)(\d+)$') { $newName = $Matches[1] + ([int]$Matches[2] + 1) }
    else { Stop-Release "The version '$name' does not end in -violence.N. Use -VersionName to set it." }
    if ($newName -notmatch '^\d+\.\d+\.\d+(-violence\.\d+)?$') { Stop-Release "Version name '$newName' should look like 1.9.0-violence.2 (upstream version, then -violence.N)." }
    $gradleRaw = $gradleRaw -replace 'extra\["bitchordVersionCode"\]\s*=\s*\d+', "extra[`"bitchordVersionCode`"] = $newCode"
    $gradleRaw = $gradleRaw -replace 'extra\["bitchordVersionName"\]\s*=\s*"[^"]+"', "extra[`"bitchordVersionName`"] = `"$newName`""
    [IO.File]::WriteAllText($gradleFile, $gradleRaw, (New-Object Text.UTF8Encoding($false)))
    Write-Host "Version: $name ($code) -> $newName ($newCode)  [build.gradle.kts changed: commit it]" -ForegroundColor Green
    $code = $newCode; $name = $newName; $versionChanged = $true
} else {
    Write-Host "Version: $name (versionCode $code)"
}

# Is the update check switched on in this build? (UPDATE_INFO_URL, same lookup as the Gradle files.)
$updateUrl = $env:UPDATE_INFO_URL
$localProps = Join-Path $root 'local.properties'
if (-not $updateUrl -and (Test-Path $localProps)) {
    $m = Select-String -Path $localProps -Pattern '^\s*UPDATE_INFO_URL\s*=\s*(.+)$' | Select-Object -First 1
    if ($m) { $updateUrl = $m.Matches[0].Groups[1].Value.Trim() }
}
if (-not $updateUrl) {
    Write-Host 'WARNING: UPDATE_INFO_URL is not set (local.properties or environment). These APKs will NOT check for updates. See RELEASE_GUIDE.md.' -ForegroundColor Yellow
} elseif (-not $UpdateFolderUrl) {
    $UpdateFolderUrl = $updateUrl -replace '/[^/]*$', ''
}

# ---- 3. Build both release APKs (minified, signed with the keystore through keystore.properties) ------------------
$outDir = Join-Path $root "release-out\$name"
if (-not $SkipBuild) {
    Use-AndroidToolchain
    & .\gradlew.bat ':app:assembleProdRelease' ':tv:assembleProdRelease' '-Pbitchord.bundleWear=false' --console=plain
    if ($LASTEXITCODE -ne 0) { Stop-Release "Gradle failed (exit $LASTEXITCODE). Nothing was published." }
}

function Find-Apk([string]$Module) {
    $dir = Join-Path $root "$Module\build\outputs\apk\prod\release"
    $apk = Get-ChildItem $dir -Filter '*universal*.apk' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apk) { Stop-Release "No universal release APK in $dir." }
    if ($apk.Name -match 'unsigned') { Stop-Release "$($apk.Name) is unsigned: keystore.properties did not take effect." }
    return $apk
}
$phoneSrc = Find-Apk 'app'
$tvSrc = Find-Apk 'tv'

# ---- 4. Check the signature and show the certificate fingerprint ------------------------------------------------
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$apksigner = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending |
    ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } | Where-Object { Test-Path $_ } | Select-Object -First 1
$fingerprint = $null
if ($apksigner) {
    foreach ($apk in $phoneSrc, $tvSrc) {
        $out = & $apksigner verify --print-certs $apk.FullName 2>&1 | Out-String
        if ($LASTEXITCODE -ne 0) { Stop-Release "apksigner says $($apk.Name) is not validly signed:`n$out" }
        if ($out -match 'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})') { $fingerprint = $Matches[1].ToLower() }
    }
} else {
    Write-Host 'WARNING: apksigner not found in the Android SDK build-tools; the signature was not double-checked.' -ForegroundColor Yellow
}

# ---- 5. Collect the files and write update.json --------------------------------------------------------------------
New-Item -ItemType Directory -Force $outDir | Out-Null
$files = @{}
foreach ($pair in @(@('phone', $phoneSrc), @('tv', $tvSrc))) {
    $key = $pair[0]; $src = $pair[1]
    $dest = Join-Path $outDir "violence-$key-$name.apk"
    Copy-Item $src.FullName $dest -Force
    $item = Get-Item $dest
    $files[$key] = [ordered]@{
        url    = $item.Name
        sha256 = (Get-FileHash $dest -Algorithm SHA256).Hash.ToLower()
        bytes  = $item.Length
    }
    Write-Host ("{0,-7} {1}  {2:N1} MB" -f $key, $item.Name, ($item.Length / 1MB))
    if ($item.Length -gt 90MB) { Write-Host "WARNING: $($item.Name) is above 90 MB; GitHub rejects files over 100 MB. See RELEASE_GUIDE.md (dropping x86_64)." -ForegroundColor Yellow }
}
$update = [ordered]@{
    versionCode = $code
    versionName = $name
    changelog   = @($Changelog)
    apks        = [ordered]@{ phone = $files['phone']; tv = $files['tv'] }
}
$json = ConvertTo-Json -InputObject $update -Depth 6
[IO.File]::WriteAllText((Join-Path $outDir 'update.json'), $json, (New-Object Text.UTF8Encoding($false)))

# ---- 6. What to do next ----------------------------------------------------------------------------------------------
Write-Host ''
Write-Host "Built $name (versionCode $code) into: $outDir" -ForegroundColor Green
if ($fingerprint) { Write-Host "Signing certificate SHA-256 (for assetlinks.json, see APP_LINKS.md): $($fingerprint.ToUpper() -replace '(..)(?=.)', '$1:')" }
Write-Host ''
Write-Host 'PUBLISH (nothing has been committed or uploaded yet):' -ForegroundColor Cyan
$n = 1
if ($versionChanged) {
    Write-Host ("{0}. git add build.gradle.kts; git commit -m `"Release {1}`"; git branch --show-current   (must print violence-nk); git push origin violence-nk" -f $n, $name); $n++
}
Write-Host ("{0}. On GitHub: repository > Releases > Draft a new release. Tag: v{1} (create it on violence-nk). Title: Violence {1}." -f $n, $name); $n++
Write-Host ("{0}. Drag these 3 files into the release (or use the gh command below): violence-phone-{1}.apk, violence-tv-{1}.apk, update.json" -f $n); $n++
Write-Host ("{0}. Leave 'Set as a pre-release' UNticked, then Publish release. The Web Deploy workflow then copies the files to the site (about 2 minutes)." -f $n); $n++
if ($UpdateFolderUrl) { Write-Host ("{0}. Check it worked: open {1}/update.json in a browser. It should show versionCode {2}. Phones and TVs notice it within a day of the next launch." -f $n, $UpdateFolderUrl, $code); $n++ }
Write-Host ''
Write-Host 'Same release from a terminal (needs gh logged in):' -ForegroundColor DarkGray
Write-Host ("  gh release create v{0} `"{1}\violence-phone-{0}.apk`" `"{1}\violence-tv-{0}.apk`" `"{1}\update.json`" --target violence-nk --title `"Violence {0}`" --notes `"{2}`"" -f $name, $outDir, ($Changelog -join '; ')) -ForegroundColor DarkGray
Write-Host ''
Write-Host 'Never commit the keystore, keystore.properties or the APKs (all gitignored).' -ForegroundColor Yellow
