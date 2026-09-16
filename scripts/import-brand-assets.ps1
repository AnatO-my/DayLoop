param([string]$Java = 'java')
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$cache = Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1'
$dependencies = @(
    'com.android.tools/sdk-common/31.13.2',
    'com.android.tools/common/31.13.2',
    'com.google.guava/guava',
    'com.google.guava/failureaccess',
    'org.jetbrains.kotlin/kotlin-stdlib',
    'org.jetbrains/annotations'
)
$jars = foreach ($dependency in $dependencies) {
    Get-ChildItem (Join-Path $cache $dependency) -Recurse -Filter '*.jar' |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Select-Object -ExpandProperty FullName
}
if (-not ($jars | Where-Object { $_ -match 'sdk-common-31.13.2.jar$' })) {
    throw 'Android sdk-common 31.13.2 must already be cached. This script does not download dependencies.'
}
& $Java --class-path ($jars -join [IO.Path]::PathSeparator) (Join-Path $PSScriptRoot 'ImportBrandAssets.java') $repo
if ($LASTEXITCODE -ne 0) { throw 'Asset import stopped; review converter diagnostics before continuing.' }
