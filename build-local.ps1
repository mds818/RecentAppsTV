param(
    [Parameter(Mandatory=$true)][string]$JdkPath,
    [Parameter(Mandatory=$true)][string]$SdkPath,
    [ValidateSet('Debug','Release')][string]$Variant = 'Debug'
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = (Resolve-Path -LiteralPath $JdkPath).Path
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.local/gradle'
$env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.local/android'
$javaHome = (Join-Path $PSScriptRoot '.local/java-user').Replace('\','/')
$taskTemp = (Join-Path $PSScriptRoot '.local/tmp').Replace('\','/')
New-Item -ItemType Directory -Force $javaHome,$env:ANDROID_USER_HOME,$taskTemp | Out-Null
$env:JAVA_TOOL_OPTIONS = '-Duser.home="' + $javaHome + '" -Djava.io.tmpdir="' + $taskTemp + '"'
$sdk = (Resolve-Path -LiteralPath $SdkPath).Path.Replace('\','/')
"sdk.dir=$sdk" | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'local.properties')
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat "-Dorg.gradle.java.home=$($env:JAVA_HOME)" "assemble$Variant" --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)" }
} finally { Pop-Location }
