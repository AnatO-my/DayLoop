$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$res = Join-Path $root 'app/src/main/res'
$android = 'http://schemas.android.com/apk/res/android'
[xml]$manifest = Get-Content (Join-Path $root 'app/src/main/AndroidManifest.xml') -Raw
[xml]$strings = Get-Content (Join-Path $res 'values/strings.xml') -Raw
$labels = @()
foreach ($kind in @('today', 'goal', 'focus')) {
    [xml]$provider = Get-Content (Join-Path $res "xml/${kind}_widget_info.xml") -Raw
    $node = $provider.DocumentElement
    if ($node.GetAttribute('previewImage', $android) -ne "@drawable/dayloop_${kind}_picker") { throw "$kind preview image is not wired" }
    if ($node.GetAttribute('previewLayout', $android) -ne "@layout/${kind}_widget_preview_layout") { throw "$kind preview layout is not wired" }
    [xml]$layout = Get-Content (Join-Path $res "layout/${kind}_widget_preview_layout.xml") -Raw
    $image = $layout.SelectSingleNode('//ImageView')
    if ($null -eq $image -or $image.GetAttribute('src', $android) -ne "@drawable/dayloop_${kind}_picker") { throw "$kind layout image mismatch" }
    $receiver = $manifest.manifest.application.receiver | Where-Object { $_.GetAttribute('name', $android) -ieq ".widget.${kind}WidgetReceiver" }
    $labelRef = $receiver.GetAttribute('label', $android)
    if ($labelRef -ne "@string/widget_${kind}_label") { throw "$kind receiver label mismatch" }
    $labels += ($strings.resources.string | Where-Object name -eq "widget_${kind}_label").InnerText
    foreach ($dark in @($false, $true)) {
        $qualifier = if ($dark) { 'drawable-night-xxxhdpi' } else { 'drawable-xxxhdpi' }
        $file = Join-Path $res "$qualifier/dayloop_${kind}_picker.png"
        $bmp = [Drawing.Bitmap]::new($file)
        try {
            $width = if ($kind -eq 'focus') { 800 } else { 680 }
            $height = if ($kind -eq 'today') { 720 } else { 880 }
            if ($bmp.Width -ne $width -or $bmp.Height -ne $height) { throw "$kind has incorrect density/ratio" }
            if ($bmp.GetPixel(0, 0).A -ne 0) { throw "$kind corners must be transparent" }
            $background = if ($dark) { '#191A18' } else { '#F4F8F9' }
            if ($bmp.GetPixel(($width / 2), 8).ToArgb() -ne [Drawing.ColorTranslator]::FromHtml($background).ToArgb()) { throw "$kind background mismatch" }
            if ($bmp.GetPixel(($width - 120), ($height - 160)).ToArgb() -ne [Drawing.ColorTranslator]::FromHtml('#DDF36B').ToArgb()) { throw "$kind action color mismatch" }
        } finally { $bmp.Dispose() }
    }
}
if (($labels | Select-Object -Unique).Count -ne 3) { throw 'Widget labels must be distinct' }
Write-Output 'PASS: three unique labels; provider/layout references; six preview dimensions, transparent corners, theme backgrounds and action colors.'
