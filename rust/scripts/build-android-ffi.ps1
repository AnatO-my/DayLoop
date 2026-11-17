param(
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,
    [string]$NdkRoot = "C:\Users\Ot\AppData\Local\Android\Sdk\ndk\29.0.14206865"
)

$ErrorActionPreference = "Stop"
$workspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$clang = Join-Path $NdkRoot "toolchains\llvm\prebuilt\windows-x86_64\bin\aarch64-linux-android26-clang.cmd"
$jniInclude = Join-Path $NdkRoot "toolchains\llvm\prebuilt\windows-x86_64\sysroot\usr\include"

if (-not (Test-Path -LiteralPath $clang)) {
    throw "Android arm64 Clang was not found at '$clang'. Set -NdkRoot to an installed NDK."
}

$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = $clang
& cargo build --manifest-path (Join-Path $workspaceRoot "rust\Cargo.toml") --target aarch64-linux-android --release --package dayloop-ffi
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$library = Join-Path $workspaceRoot "rust\target\aarch64-linux-android\release\libdayloop_ffi.so"
$abiDirectory = Join-Path $OutputDirectory "arm64-v8a"
New-Item -ItemType Directory -Force -Path $abiDirectory | Out-Null
Copy-Item -LiteralPath $library -Destination (Join-Path $abiDirectory "libdayloop_ffi.so") -Force

$bridgeSource = Join-Path $workspaceRoot "rust\jni\dayloop_ffi_jni.c"
$bridgeLibrary = Join-Path $abiDirectory "libdayloop_ffi_jni.so"
& $clang "-shared" "-fPIC" "-I$jniInclude" "-L$(Split-Path $library -Parent)" "-ldayloop_ffi" "-Wl,--no-undefined" "-o" $bridgeLibrary $bridgeSource
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
