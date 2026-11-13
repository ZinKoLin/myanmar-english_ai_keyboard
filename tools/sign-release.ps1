param(
    [Parameter(Mandatory)][string]$Keystore,
    [Parameter(Mandatory)][string]$Alias,
    [string]$Version = '1.0.0',
    [switch]$SamePassword
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$buildTools = Join-Path $sdk 'build-tools\35.0.0'
$javaBin = 'C:\Program Files\Android\Android Studio\jbr\bin'
$unsignedApk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release-unsigned.apk'
$unsignedBundle = Join-Path $projectRoot 'app\build\outputs\bundle\release\app-release.aab'
$releaseDir = Join-Path $projectRoot 'dist'
foreach ($file in @($Keystore, $unsignedApk, $unsignedBundle)) {
    if (!(Test-Path -LiteralPath $file)) { throw "Required file missing: $file" }
}
New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
$signedApk = Join-Path $releaseDir "Brain-Keyboard-$Version-release.apk"
$signedBundle = Join-Path $releaseDir "Brain-Keyboard-$Version-release.aab"
$storeSecret = Read-Host 'Keystore password' -AsSecureString
$keySecret = if ($SamePassword) { $storeSecret } else { Read-Host 'Key password' -AsSecureString }
try {
    # Child tools receive transient environment variables, never command-line passwords.
    $env:BRAIN_SIGN_STORE_PASSWORD = [Net.NetworkCredential]::new('', $storeSecret).Password
    $env:BRAIN_SIGN_KEY_PASSWORD = [Net.NetworkCredential]::new('', $keySecret).Password
    & (Join-Path $javaBin 'keytool.exe') -list -keystore $Keystore -alias $Alias -storepass:env BRAIN_SIGN_STORE_PASSWORD
    if ($LASTEXITCODE) { throw 'Keystore or alias verification failed.' }
    & (Join-Path $buildTools 'zipalign.exe') -c -P 16 4 $unsignedApk
    if ($LASTEXITCODE) { throw 'Unsigned APK is not aligned.' }
    & (Join-Path $buildTools 'apksigner.bat') sign --ks $Keystore --ks-key-alias $Alias --ks-pass env:BRAIN_SIGN_STORE_PASSWORD --key-pass env:BRAIN_SIGN_KEY_PASSWORD --v4-signing-enabled false --out $signedApk $unsignedApk
    if ($LASTEXITCODE) { throw 'APK signing failed.' }
    & (Join-Path $buildTools 'apksigner.bat') verify --verbose --print-certs $signedApk
    if ($LASTEXITCODE) { throw 'APK signature verification failed.' }
    & (Join-Path $buildTools 'zipalign.exe') -c -P 16 4 $signedApk
    if ($LASTEXITCODE) { throw 'Signed APK is not aligned.' }
    & (Join-Path $javaBin 'jarsigner.exe') -keystore $Keystore -storepass:env BRAIN_SIGN_STORE_PASSWORD -keypass:env BRAIN_SIGN_KEY_PASSWORD -signedjar $signedBundle $unsignedBundle $Alias
    if ($LASTEXITCODE) { throw 'Bundle signing failed.' }
    & (Join-Path $javaBin 'jarsigner.exe') -verify $signedBundle
    if ($LASTEXITCODE) { throw 'Bundle signature verification failed.' }
    $hashes = Get-FileHash -Algorithm SHA256 -LiteralPath $signedApk, $signedBundle
    $hashes | ForEach-Object { "$($_.Hash.ToLowerInvariant())  $([IO.Path]::GetFileName($_.Path))" } |
        Set-Content -Encoding utf8 (Join-Path $releaseDir 'SHA256SUMS.txt')
    Get-Item -LiteralPath $signedApk, $signedBundle | Select-Object Name, Length
} finally {
    Remove-Item Env:\BRAIN_SIGN_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:\BRAIN_SIGN_KEY_PASSWORD -ErrorAction SilentlyContinue
    if ($keySecret -ne $storeSecret) { $keySecret.Dispose() }
    $storeSecret.Dispose()
}
