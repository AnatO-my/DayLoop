$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { 'C:\Users\Ot\AppData\Local\Android\Sdk' }
$platform = Join-Path $sdk 'platforms\android-36\android.jar'
$buildToolsRoot = Join-Path $sdk 'build-tools'
$buildTools = Get-ChildItem -LiteralPath $buildToolsRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $buildTools) { throw 'No Android build-tools directory found.' }
$aapt2 = Join-Path $buildTools.FullName 'aapt2.exe'
$d8 = Join-Path $buildTools.FullName 'd8.bat'
$zipalign = Join-Path $buildTools.FullName 'zipalign.exe'
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
$javac = (Get-Command javac.exe -ErrorAction Stop).Source

$keytoolCommand = Get-Command keytool.exe -ErrorAction SilentlyContinue
if ($keytoolCommand) {
  $keytool = $keytoolCommand.Source
} else {
  $keytoolCandidates = @(
    'C:\Program Files\Java\jdk-24\bin\keytool.exe',
    'C:\Program Files\Android\Android Studio1\jbr\bin\keytool.exe',
    'C:\Program Files\Avid\Application Manager\jre\bin\keytool.exe'
  )
  $keytool = $keytoolCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
  if (-not $keytool) { throw 'keytool.exe not found on PATH or known local JDK locations.' }
}

$jarCommand = Get-Command jar.exe -ErrorAction SilentlyContinue
if ($jarCommand) {
  $jar = $jarCommand.Source
} else {
  $jarCandidates = @(
    'C:\Program Files\Java\jdk-24\bin\jar.exe',
    'C:\Program Files\Android\Android Studio1\jbr\bin\jar.exe',
    'C:\Program Files\Avid\Application Manager\jre\bin\jar.exe'
  )
  $jar = $jarCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
  if (-not $jar) { throw 'jar.exe not found on PATH or known local JDK locations.' }
}

foreach ($required in @($platform, $aapt2, $d8, $zipalign, $apksigner, $javac, $keytool, $jar)) {
  if (-not (Test-Path -LiteralPath $required)) { throw "Missing required tool: $required" }
}

function Run-Checked {
  param(
    [Parameter(Mandatory=$true)][string]$Exe,
    [Parameter(Mandatory=$true)][string[]]$Arguments
  )
  & $Exe @Arguments
  if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE): $Exe $($Arguments -join ' ')" }
}

$build = Join-Path $root 'build'
$resolvedRoot = [System.IO.Path]::GetFullPath($root)
$resolvedBuild = [System.IO.Path]::GetFullPath($build)
if (-not $resolvedBuild.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to clean build directory outside spike root: $resolvedBuild"
}
if (Test-Path -LiteralPath $build) { Remove-Item -LiteralPath $build -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $build 'classes') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $build 'dex') | Out-Null

$unsignedApk = Join-Path $build 'unsigned.apk'
$alignedApk = Join-Path $build 'aligned.apk'
$signedApk = Join-Path $build 'exact-alarm-spike.apk'

Run-Checked $aapt2 @('link', '--manifest', (Join-Path $root 'AndroidManifest.xml'), '-I', $platform, '--min-sdk-version', '26', '--target-sdk-version', '36', '-o', $unsignedApk)
$javaFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
Run-Checked $javac (@('-source', '8', '-target', '8', '-bootclasspath', $platform, '-d', (Join-Path $build 'classes')) + $javaFiles)
$classFiles = Get-ChildItem -LiteralPath (Join-Path $build 'classes') -Recurse -Filter '*.class' | Select-Object -ExpandProperty FullName
Run-Checked $d8 (@('--min-api', '26', '--output', (Join-Path $build 'dex')) + $classFiles)
Run-Checked $jar @('uf', $unsignedApk, '-C', (Join-Path $build 'dex'), 'classes.dex')
Run-Checked $zipalign @('-f', '-p', '4', $unsignedApk, $alignedApk)
$keystore = Join-Path $build 'debug.keystore'
Run-Checked $keytool @('-genkeypair', '-v', '-keystore', $keystore, '-storepass', 'android', '-alias', 'androiddebugkey', '-keypass', 'android', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-dname', 'CN=Android Debug,O=Android,C=US')
Run-Checked $apksigner @('sign', '--ks', $keystore, '--ks-key-alias', 'androiddebugkey', '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', $signedApk, $alignedApk)
Run-Checked $apksigner @('verify', '--verbose', $signedApk)
Write-Host "Built $signedApk"

