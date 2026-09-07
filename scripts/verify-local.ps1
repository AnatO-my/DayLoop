$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

& .\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleDebug --no-daemon