Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$brand = Get-Content -Raw (Join-Path $PSScriptRoot 'brain-keyboard-paths.json') | ConvertFrom-Json

function ConvertTo-BrandPath([string] $data) {
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    $tokens = [regex]::Matches($data, '[MCHVLZ]|-?\d+(?:\.\d+)?') | ForEach-Object Value
    $i = 0; [single]$x = 0; [single]$y = 0; [single]$startX = 0; [single]$startY = 0
    while ($i -lt $tokens.Count) {
        $command = $tokens[$i++]
        switch ($command) {
            'M' { $x = [single]$tokens[$i++]; $y = [single]$tokens[$i++]; $startX=$x; $startY=$y; $path.StartFigure() }
            'L' { $nx=[single]$tokens[$i++]; $ny=[single]$tokens[$i++]; $path.AddLine($x,$y,$nx,$ny); $x=$nx; $y=$ny }
            'H' { $nx=[single]$tokens[$i++]; $path.AddLine($x,$y,$nx,$y); $x=$nx }
            'V' { $ny=[single]$tokens[$i++]; $path.AddLine($x,$y,$x,$ny); $y=$ny }
            'C' {
                $x1=[single]$tokens[$i++]; $y1=[single]$tokens[$i++]; $x2=[single]$tokens[$i++]; $y2=[single]$tokens[$i++]
                $nx=[single]$tokens[$i++]; $ny=[single]$tokens[$i++]; $path.AddBezier($x,$y,$x1,$y1,$x2,$y2,$nx,$ny); $x=$nx; $y=$ny
            }
            'Z' { $path.CloseFigure(); $x=$startX; $y=$startY }
            default { throw "Unsupported brand path command: $command" }
        }
    }
    return ,$path
}

function New-BrandIcon([int] $size) {
    $bitmap = [Drawing.Bitmap]::new($size,$size)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.ScaleTransform($size/108.0,$size/108.0)
    $navy = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($brand.navy))
    $mint = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($brand.mint))
    $fold = [Drawing.Pen]::new($navy.Color,2.5)
    $fold.StartCap = $fold.EndCap = [Drawing.Drawing2D.LineCap]::Round
    $edge = [Drawing.Pen]::new($mint.Color,2.5)
    try {
        $background = ConvertTo-BrandPath 'M24 0 H84 C97 0 108 11 108 24 V84 C108 97 97 108 84 108 H24 C11 108 0 97 0 84 V24 C0 11 11 0 24 0 Z'
        $g.FillPath($navy,$background); $background.Dispose()
        foreach($data in $brand.brains) { $p=ConvertTo-BrandPath $data; $g.FillPath($mint,$p); $p.Dispose() }
        foreach($data in $brand.folds) { $p=ConvertTo-BrandPath $data; $g.DrawPath($fold,$p); $p.Dispose() }
        $p=ConvertTo-BrandPath $brand.board; $g.DrawPath($edge,$p); $p.Dispose()
        foreach($data in $brand.keys) { $p=ConvertTo-BrandPath $data; $g.FillPath($mint,$p); $p.Dispose() }
    } finally { $g.Dispose(); $navy.Dispose(); $mint.Dispose(); $fold.Dispose(); $edge.Dispose() }
    return ,$bitmap
}

foreach($size in @(512,1024)) {
    $icon=New-BrandIcon $size
    $icon.Save((Join-Path $PSScriptRoot "brain-keyboard-icon-$size.png"),[Drawing.Imaging.ImageFormat]::Png)
    $icon.Dispose()
}
$logo=[Drawing.Bitmap]::new(900,220)
$g=[Drawing.Graphics]::FromImage($logo)
$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint=[Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$icon=New-BrandIcon 352
$font=[Drawing.Font]::new('Segoe UI',68,[Drawing.FontStyle]::Bold,[Drawing.GraphicsUnit]::Pixel)
$brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($brand.navy))
try {
    $g.DrawImage($icon,22,22,176,176)
    $g.DrawString('Brain Keyboard',$font,$brush,[single]220,[single]63)
    $logo.Save((Join-Path $PSScriptRoot 'brain-keyboard-logo.png'),[Drawing.Imaging.ImageFormat]::Png)
} finally { $g.Dispose(); $logo.Dispose(); $icon.Dispose(); $font.Dispose(); $brush.Dispose() }
Write-Output 'Rendered Brain Keyboard logo and 512/1024 PNG icons.'
