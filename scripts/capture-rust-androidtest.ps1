param(
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $projectRoot "app\build\rust-androidtest-output.txt"
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$buildScript = Join-Path $projectRoot "scripts\build-rust-androidtest.ps1"
$command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$buildScript`" -Detailed > `"$OutputPath`" 2>&1"

# cmd.exe owns redirection, so Gradle's stdout and stderr land in one plain-text
# file without PowerShell rewriting native-process diagnostics.
& cmd.exe /d /c $command
$exitCode = $LASTEXITCODE

Write-Output "Saved complete build output to: $OutputPath"
exit $exitCode
