$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$fontDir = Join-Path $root "app/src/main/res/font"
$licenseDir = Join-Path $root "app/src/main/assets/licenses"
foreach ($family in @("roboto", "opensans", "lato", "montserrat", "poppins")) {
    $items = Invoke-RestMethod "https://api.github.com/repos/google/fonts/contents/ofl/$family"
    $fonts = $items | Where-Object { $_.name -match '\.ttf$' -and $_.name -notmatch 'Italic' -and ($_.name -match '\[' -or $_.name -match '-(Regular|Bold)\.ttf$') }
    foreach ($font in $fonts) {
        $suffix = if ($font.name -match 'Bold') { "bold" } else { "regular" }
        Invoke-WebRequest $font.download_url -OutFile (Join-Path $fontDir "$($family)_$suffix.ttf")
    }
    $license = $items | Where-Object { $_.name -eq "OFL.txt" }
    if (!$license -or !$fonts) { throw "Missing font/license: $family" }
    Invoke-WebRequest $license.download_url -OutFile (Join-Path $licenseDir "$($family)_ofl.txt")
    Write-Output "$family imported with OFL license"
}
