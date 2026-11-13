$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $projectRoot 'artifacts\mon-research'
$revision = 'a112735f8800694a6adc8d14c4d9d3f4a4833caf'
$baseUrl = "https://raw.githubusercontent.com/Barnista/MonDictDB/$revision"
New-Item -ItemType Directory -Force $destination | Out-Null
Invoke-WebRequest "$baseUrl/LICENSE" -OutFile (Join-Path $destination 'MonDictDB.LICENSE')
$revision | Set-Content -Encoding utf8 (Join-Path $destination 'commit.txt')
$files = @(1..3 | ForEach-Object { "word_$_.js" }) + @(1..24 | ForEach-Object { "definition_$_.js" })
foreach ($file in $files) {
    Invoke-WebRequest "$baseUrl/docs/assets/database/$file" -OutFile (Join-Path $destination $file)
}
& node (Join-Path $PSScriptRoot 'build-mon-lexicon.cjs')
if ($LASTEXITCODE) { throw 'Dictionary extraction failed.' }
