# =============================================================================
#  mirror-fabric-deps.ps1
#
#  WHY THIS EXISTS
#  ---------------
#  On this kind of network (China), maven.fabricmc.net behaves very strangely:
#    * Gradle's HTTP client receives ZERO bytes from it (no timeout is ever
#      triggered, so a build just hangs for tens of minutes), BUT
#    * curl CAN download from the same URL (slow, with stalls, but resumable).
#    * All domestic mirrors (Aliyun / BMCLAPI / FastMirror / Tencent / Nova /
#      MCIMirror) do NOT carry the Fabric-only artifacts.
#
#  So: use curl to pre-download the Fabric-only artifacts into a local Maven
#  repository (local-maven/), and let build.gradle read them from there first.
#  Everything else (commons-io, gson, guava, asm, kotlin-metadata-jvm, ...) is
#  available on the Aliyun mirror and Gradle downloads it normally.
#
#  USAGE
#  -----
#      powershell -ExecutionPolicy Bypass -File .\mirror-fabric-deps.ps1
#
#  Safe to re-run: already-complete files return immediately, partial files
#  are resumed, missing ones are fetched.
#
#  NOTE: keep this file ASCII-only. Windows PowerShell 5.1 reads BOM-less files
#  as ANSI/GBK, which corrupts non-ASCII text and breaks parsing.
# =============================================================================

$ErrorActionPreference = "Continue"
$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = (Get-Location).Path }
$repo = Join-Path $projectRoot "local-maven"
$remote = "https://maven.fabricmc.net"
New-Item -ItemType Directory -Force -Path $repo | Out-Null

# group, artifact, version  (jar + pom are always attempted, module is optional)
$coords = @(
    @("net.fabricmc", "fabric-loom",                      "1.9.2"),
    @("net.fabricmc", "fabric-loader",                    "0.16.9"),
    @("net.fabricmc", "intermediary",                     "1.21.4"),
    @("net.fabricmc", "stitch",                           "0.6.2"),
    @("net.fabricmc", "tiny-remapper",                    "0.10.4"),
    @("net.fabricmc", "access-widener",                   "2.1.0"),
    @("net.fabricmc", "mapping-io",                       "0.6.1"),
    @("net.fabricmc", "lorenz-tiny",                      "4.0.2"),
    @("net.fabricmc", "fabric-loom-native",               "0.2.0"),
    @("net.fabricmc", "mercury",                          "0.4.2"),
    @("net.fabricmc", "fabric-mixin-compile-extensions",  "0.6.0"),
    @("net.fabricmc", "sponge-mixin",                     "0.17.4+mixin.0.8.7"),
    @("net.fabricmc", "sponge-mixin",                     "0.15.4+mixin.0.8.7")
)

# Extra file names to try (all optional, 404 is fine)
$extras = @(
    @("net.fabricmc", "intermediary", "1.21.4", "intermediary-1.21.4-v2.jar"),
    @("net.fabricmc", "fabric-loom-native", "0.2.0", "fabric-loom-native-0.2.0-natives-windows.jar"),
    @("net.fabricmc", "fabric-loom-native", "0.2.0", "fabric-loom-native-0.2.0-natives-linux.jar"),
    @("net.fabricmc", "fabric-loom-native", "0.2.0", "fabric-loom-native-0.2.0-natives-macos.jar")
)

function Fetch-File {
    param([string]$Url, [string]$Out, [bool]$Optional = $false)

    for ($i = 1; $i -le 25; $i++) {
        if (Test-Path $Out) {
            & curl.exe -4 -f -s -L --continue-at - --max-time 30 -o $Out $Url 2>$null
        } else {
            & curl.exe -4 -f -s -L --max-time 30 -o $Out $Url 2>$null
        }
        $code = $LASTEXITCODE
        if ($code -eq 0) { return $true }
        if ($code -eq 22) {
            if (Test-Path $Out) { Remove-Item $Out -Force -ErrorAction SilentlyContinue }
            if (-not $Optional) { Write-Host "    MISSING (404): $Url" -ForegroundColor Red }
            return $false
        }
        Start-Sleep -Milliseconds 800
    }
    Write-Host "    FAILED after 25 tries: $Url" -ForegroundColor Red
    return $false
}

function Get-Dir {
    param([string]$Group, [string]$Artifact, [string]$Version)
    $d = Join-Path $repo (($Group -replace '\.', '/') + "/$Artifact/$Version")
    New-Item -ItemType Directory -Force -Path $d | Out-Null
    return $d
}

$ok = 0; $missing = 0

foreach ($c in $coords) {
    $group = $c[0]; $artifact = $c[1]; $version = $c[2]
    $dir = Get-Dir $group $artifact $version
    $base = "$remote/" + ($group -replace '\.', '/') + "/$artifact/$version/$artifact-$version"
    Write-Host "[$artifact $version]" -ForegroundColor Cyan

    if (Fetch-File "$base.pom" (Join-Path $dir "$artifact-$version.pom")) { $ok++ } else { $missing++ }
    if (Fetch-File "$base.module" (Join-Path $dir "$artifact-$version.module") $true) { $ok++ } else { $missing++ }
    if (Fetch-File "$base.jar" (Join-Path $dir "$artifact-$version.jar")) { $ok++ } else { $missing++ }
}

Write-Host ""
Write-Host "[extra optional files]" -ForegroundColor Cyan
foreach ($e in $extras) {
    $dir = Get-Dir $e[0] $e[1] $e[2]
    $url = "$remote/" + ($e[0] -replace '\.', '/') + "/$($e[1])/$($e[2])/$($e[3])"
    if (Fetch-File $url (Join-Path $dir $e[3]) $true) { $ok++ }
}

Write-Host ""
Write-Host "DONE. fetched=$ok missing/optional-skipped=$missing" -ForegroundColor Green
Write-Host "local repo: $repo"
