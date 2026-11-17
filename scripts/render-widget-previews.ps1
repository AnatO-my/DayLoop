# Reproduce compact widget layouts as density-aware PNG fallbacks for OEM pickers.
# Keep geometry/sample text aligned with widget Content functions and WidgetPreviewSamples.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent

function Fill-Round($g, $color, [single]$x, [single]$y, [single]$w, [single]$h, [single]$radius) {
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    $d = 2 * $radius
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc(($x + $w - $d), $y, $d, $d, 270, 90)
    $path.AddArc(($x + $w - $d), ($y + $h - $d), $d, $d, 0, 90)
    $path.AddArc($x, ($y + $h - $d), $d, $d, 90, 90)
    $path.CloseFigure()
    $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
    $g.FillPath($brush, $path)
    $brush.Dispose()
    $path.Dispose()
}

function Draw-Text($g, $text, $color, [single]$size, [single]$x, [single]$y, [single]$w, [single]$h,
    [bool]$bold = $false, [bool]$center = $false) {
    $style = if ($bold) { [Drawing.FontStyle]::Bold } else { [Drawing.FontStyle]::Regular }
    $font = [Drawing.Font]::new('Arial', $size, $style, [Drawing.GraphicsUnit]::Pixel)
    $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
    $format = [Drawing.StringFormat]::new([Drawing.StringFormat]::GenericTypographic)
    $format.FormatFlags = [Drawing.StringFormatFlags]::NoWrap
    if ($center) {
        $format.Alignment = [Drawing.StringAlignment]::Center
        $format.LineAlignment = [Drawing.StringAlignment]::Center
    }
    $g.DrawString($text, $font, $brush, [Drawing.RectangleF]::new($x, $y, $w, $h), $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function Draw-Action($g, $text, [single]$x, [single]$y, [single]$w) {
    Fill-Round $g '#DDF36B' $x $y $w 48 24
    Draw-Text $g $text '#171B16' 14 $x $y $w 48 $true $true
}

foreach ($dark in @($false, $true)) {
    $qualifier = if ($dark) { 'drawable-night-xxxhdpi' } else { 'drawable-xxxhdpi' }
    $dir = Join-Path $root "app/src/main/res/$qualifier"
    [IO.Directory]::CreateDirectory($dir) | Out-Null
    $background = if ($dark) { '#191A18' } else { '#F4F8F9' }
    $text = if ($dark) { '#F4F8F9' } else { '#191A18' }
    $muted = if ($dark) { '#B9C2B3' } else { '#535D50' }
    foreach ($kind in @('today', 'goal', 'focus')) {
        $w = if ($kind -eq 'focus') { 200 } else { 170 }
        $h = if ($kind -eq 'today') { 180 } else { 220 }
        $bitmap = [Drawing.Bitmap]::new(($w * 4), ($h * 4), [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [Drawing.Graphics]::FromImage($bitmap)
        try {
            $g.Clear([Drawing.Color]::Transparent)
            $g.ScaleTransform(4, 4)
            $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
            $g.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
            Fill-Round $g $background 0 0 $w $h 24
            $title = switch ($kind) { 'today' { 'Today' } 'goal' { 'Goal' } 'focus' { 'Focus' } }
            Draw-Text $g $title $text 18 16 16 ($w - 32) 24 $true
            switch ($kind) {
                'today' {
                    Draw-Text $g '3 remaining' $text 18 16 48 138 24 $true
                    Draw-Text $g '2 completed' $muted 12 16 73 138 18
                    Draw-Action $g '+' 16 116 48
                    Draw-Action $g 'Open' 72 116 82
                }
                'goal' {
                    Draw-Text $g 'Read 10 books' $text 16 16 48 138 24
                    Fill-Round $g $muted 16 80 138 4 2
                    Fill-Round $g '#DDF36B' 16 80 55.2 4 2
                    Draw-Text $g '40%' $text 16 16 88 138 24
                    Draw-Action $g 'P&Gs' 16 156 138
                }
                'focus' {
                    Draw-Text $g 'Pomodoro' $muted 13 16 40 168 18
                    Draw-Text $g '25:00' $text 28 16 65 168 36 $true
                    Draw-Text $g '45 min today' $muted 13 16 103 168 20
                    Draw-Action $g 'Start' 16 156 168
                }
            }
            $bitmap.Save((Join-Path $dir "dayloop_${kind}_picker.png"), [Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $g.Dispose()
            $bitmap.Dispose()
        }
    }
}
Write-Output 'Generated six light/dark widget picker images at 4x density.'
