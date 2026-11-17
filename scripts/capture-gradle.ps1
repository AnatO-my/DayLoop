param(
    [Parameter(Mandatory)]
    [string[]]$GradleArguments,
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$javaHome = "C:\Users\Ot\.jdks\jbr-21.0.11"
$java = Join-Path $javaHome "bin\java.exe"
$wrapper = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"

if (-not (Test-Path -LiteralPath $java)) {
    throw "Android Studio JDK was not found at '$java'. Update `$javaHome before running this script."
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $projectRoot "app\build\gradle-output.txt"
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$arguments = @(
    "-Xmx64m",
    "-Xms64m",
    "-Dorg.gradle.appname=gradlew",
    "-Dorg.gradle.daemon=false",
    "-jar",
    $wrapper,
    "--no-daemon",
    "--console=plain",
    "-Pkotlin.compiler.execution.strategy=in-process"
) + $GradleArguments

Push-Location $projectRoot
try {
    # Both native streams are retained in one uploadable log. Tee keeps concise
    # progress visible while the file contains the full diagnostic sequence.
    & $java @arguments 2>&1 | Tee-Object -FilePath $OutputPath
    $gradleExitCode = $LASTEXITCODE
    Write-Output "Saved complete Gradle output to: $OutputPath"
    exit $gradleExitCode
} finally {
    Pop-Location
}
