param([string]$Layer = 'base', [int]$Row = 0)
$ErrorActionPreference = 'Stop'
$folder = Join-Path $PSScriptRoot "../popup-audit"
New-Item -ItemType Directory -Force -Path $folder | Out-Null
$ys = @(1710,1840,1980,2120)
$xs = if ($Row -eq 3) { @(165,270,380,485,595,700,810,915) } else { @(55,165,270,380,485,595,700,810,915,1025) }
foreach ($index in 0..($xs.Count-1)) {
    $x=$xs[$index]; $y=$ys[$Row]
    if ($Layer -eq 'shift') { adb shell input tap 55 2120 }
    $file = "$Layer-$Row-$index.png"
    adb shell "input swipe $x $y $x $y 1400 & sleep 0.8; screencap -p /sdcard/popup-audit.png; wait"
    adb pull /sdcard/popup-audit.png (Join-Path $folder $file) 2>$null | Out-Null
    adb shell input keyevent 62
}
adb shell uiautomator dump /sdcard/popup-values.xml | Out-Null
[xml]$tree = (adb shell cat /sdcard/popup-values.xml)
$node = $tree.SelectSingleNode('//node[@class="android.widget.EditText"]')
if ($node) {
    $value=$node.GetAttribute('text')
    Write-Output $value
    Write-Output (($value.ToCharArray() | ForEach-Object { 'U+{0:X4}' -f [int]$_ }) -join ' ')
}
Add-Type -AssemblyName System.Drawing
$canvas = [System.Drawing.Bitmap]::new(1080, [int]([Math]::Ceiling($xs.Count/2)*440))
$g = [System.Drawing.Graphics]::FromImage($canvas)
try {
    foreach ($index in 0..($xs.Count-1)) {
        $img = [System.Drawing.Image]::FromFile((Join-Path $folder "$Layer-$Row-$index.png"))
        try {
            $dest=[System.Drawing.Rectangle]::new([int](($index%2)*540),[int]([Math]::Floor($index/2)*440),540,420)
            $g.DrawImage($img,$dest,0,1450,1080,840,[System.Drawing.GraphicsUnit]::Pixel)
            $g.DrawString("$Layer row $Row key $index",[System.Drawing.SystemFonts]::DefaultFont,[System.Drawing.Brushes]::Black, $dest.X, ($dest.Y+420))
        } finally { $img.Dispose() }
    }
    $canvas.Save((Join-Path $folder "$Layer-$Row-sheet.png"))
} finally { $g.Dispose(); $canvas.Dispose() }
