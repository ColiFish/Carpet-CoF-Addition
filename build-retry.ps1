# =============================================================================
#  build-retry.ps1
#
#  WHY: the dependency host maven.fabricmc.net is reachable but extremely slow
#  and stall-prone from this network. Gradle has no retry of its own, so a
#  single stall fails the whole build. However Gradle DOES cache every artifact
#  it managed to download, so simply re-running makes progress each time until
#  everything is cached - after that builds take seconds.
#
#  USAGE (Windows PowerShell 5.1 blocks script files by default):
#
#      powershell -ExecutionPolicy Bypass -File .\build-retry.ps1
#      powershell -ExecutionPolicy Bypass -File .\build-retry.ps1 -MaxAttempts 50
#
#  Requires JDK 21 (NOT 24).
#
#  NOTE: keep this file ASCII-only. Windows PowerShell 5.1 reads BOM-less files
#  as ANSI/GBK, which corrupts non-ASCII text and breaks parsing.
# =============================================================================
param(
    [int]$MaxAttempts = 30,
    [string]$JavaHome = "C:\Program Files\Java\jdk-21"
)

$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
Set-Location $root

if (-not (Test-Path $JavaHome)) {
    Write-Host "JDK 21 not found at: $JavaHome" -ForegroundColor Red
    Write-Host "Pass a different path with -JavaHome" -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = $JavaHome
# This network does not do IPv6 well; force IPv4 for every JVM Gradle spawns.
$env:JAVA_TOOL_OPTIONS = "-Djava.net.preferIPv4Stack=true"

# Step 0: make sure the Fabric-only artifacts are pre-fetched into local-maven/
# (Gradle's HTTP client receives zero bytes from maven.fabricmc.net here, while
#  curl can download from it, so we mirror those few artifacts with curl first)
if (Test-Path (Join-Path $root "mirror-fabric-deps.ps1")) {
    Write-Host "Pre-fetching Fabric-only dependencies into local-maven/ ..." -ForegroundColor Cyan
    & (Join-Path $root "mirror-fabric-deps.ps1")
}

$gradle = Join-Path $root "gradlew.bat"
if (-not (Test-Path $gradle)) { $gradle = "gradle" }

for ($i = 1; $i -le $MaxAttempts; $i++) {
    Write-Host ""
    Write-Host "===== attempt $i / $MaxAttempts   $(Get-Date -Format 'HH:mm:ss') =====" -ForegroundColor Cyan

    & $gradle compileJava --console=plain 2>&1 | Select-Object -Last 12

    if ($LASTEXITCODE -eq 0) {
        Write-Host "compileJava OK, packaging ..." -ForegroundColor Green
        & $gradle build --console=plain 2>&1 | Select-Object -Last 25
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "BUILD SUCCESSFUL - jar is in build\libs\" -ForegroundColor Green
            exit 0
        }
        Write-Host "packaging failed, retrying ..." -ForegroundColor Yellow
    }
    else {
        Write-Host "attempt failed (most likely a stalled download), retrying ..." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Still not done after $MaxAttempts attempts." -ForegroundColor Red
Write-Host "Cached downloads are kept, so just raise -MaxAttempts and run again," -ForegroundColor Red
Write-Host "or use a proxy. Read the LAST error: if it is a compile error rather" -ForegroundColor Red
Write-Host "than a download failure, then the code needs a fix." -ForegroundColor Red
exit 1
