param(
    [switch]$Detailed
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$javaHome = "C:\Users\Ot\.jdks\jbr-21.0.11"
$java = Join-Path $javaHome "bin\java.exe"
$wrapper = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"

if (-not (Test-Path -LiteralPath $java)) {
    throw "Android Studio JDK was not found at '$java'. Update `$javaHome before running this script."
}

Push-Location $projectRoot
try {
    $arguments = @(
        "-Xmx64m",
        "-Xms64m",
        "-Dorg.gradle.appname=gradlew",
        "-Dorg.gradle.daemon=false",
        "-jar",
        $wrapper,
        "--no-daemon",
        "--console=plain",
        "-Pkotlin.compiler.execution.strategy=in-process",
        ":app:assembleDebug",
        ":app:assembleDebugAndroidTest"
    )
    if ($Detailed) {
        # Full KAPT reruns can fail with an AGP/Kotlin worker handshake despite a
        # successful compiler exit. Gradle's normal input tracking still rebuilds
        # changed Kotlin, Rust, and packaging inputs while preserving diagnostics.
        $arguments += "--info"
    }
    # Gradle writes diagnostics to stderr. Do not let PowerShell turn those
    # lines into a NativeCommandError before Gradle has emitted its full report.
    $ErrorActionPreference = "Continue"
    & $java @arguments
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) { exit $gradleExitCode }
} finally {
    Pop-Location
}
