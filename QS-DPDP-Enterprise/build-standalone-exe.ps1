# QS-DPDP Enterprise - Standalone EXE Builder
# Creates a self-contained .exe with bundled JRE (no Java required)

param(
    [switch]$SkipMaven,
    [string]$OutputDir = "$PSScriptRoot\target\standalone"
)

$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
$TargetDir = "$ProjectDir\target"
$JarFile = "$TargetDir\qs-dpdp-enterprise-2.0.0.jar"
$IconFile = "$ProjectDir\installer-assets\app.ico"
$AppName = "QS-DPDP-Enterprise"
$AppVersion = "2.0.0"
$MainClass = "org.springframework.boot.loader.launch.JarLauncher"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  QS-DPDP Enterprise - Standalone EXE Builder" -ForegroundColor Cyan
Write-Host "  Creates self-contained .exe with bundled JRE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Verify Prerequisites
Write-Host "[1/4] Checking prerequisites..." -ForegroundColor Yellow

$javaVersion = & java --version 2>&1 | Select-Object -First 1
if (-not $javaVersion) {
    Write-Host "ERROR: Java not found." -ForegroundColor Red
    exit 1
}
Write-Host "  OK Java: $javaVersion" -ForegroundColor Green

$jpackagePath = (Get-Command jpackage -ErrorAction SilentlyContinue).Source
if (-not $jpackagePath) {
    Write-Host "ERROR: jpackage not found." -ForegroundColor Red
    exit 1
}
Write-Host "  OK jpackage: $jpackagePath" -ForegroundColor Green

$mvnPath = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvnPath -and -not $SkipMaven) {
    if (Test-Path "$ProjectDir\mvnw.cmd") {
        $mvnPath = "$ProjectDir\mvnw.cmd"
    }
    else {
        Write-Host "ERROR: Maven not found. Use -SkipMaven if JAR already built." -ForegroundColor Red
        exit 1
    }
}
if ($mvnPath) {
    Write-Host "  OK Maven: $mvnPath" -ForegroundColor Green
}

# Step 2: Build Fat JAR
if (-not $SkipMaven) {
    Write-Host ""
    Write-Host "[2/4] Building fat JAR with Maven..." -ForegroundColor Yellow
    Push-Location $ProjectDir
    try {
        & mvn clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERROR: Maven build failed." -ForegroundColor Red
            exit 1
        }
    }
    finally {
        Pop-Location
    }
    Write-Host "  OK Fat JAR built: $JarFile" -ForegroundColor Green
}
else {
    Write-Host ""
    Write-Host "[2/4] Skipping Maven build (using existing JAR)..." -ForegroundColor Yellow
}

if (-not (Test-Path $JarFile)) {
    Write-Host "ERROR: JAR not found at $JarFile" -ForegroundColor Red
    exit 1
}

$jarSize = [math]::Round((Get-Item $JarFile).Length / 1MB, 1)
Write-Host "  OK JAR size: $jarSize MB" -ForegroundColor Green

# Step 3: Create Standalone EXE with jpackage
Write-Host ""
Write-Host "[3/4] Creating standalone EXE with bundled JRE..." -ForegroundColor Yellow

if (Test-Path $OutputDir) {
    Remove-Item -Recurse -Force $OutputDir
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$jpackageArgs = @(
    "--type", "app-image",
    "--name", $AppName,
    "--input", $TargetDir,
    "--main-jar", "qs-dpdp-enterprise-2.0.0.jar",
    "--main-class", $MainClass,
    "--dest", $OutputDir,
    "--app-version", $AppVersion,
    "--vendor", "QualityShield Technologies Pvt. Ltd.",
    "--description", "QS-DPDP Enterprise - DPDP Act 2023 Compliance Platform",
    "--java-options", "-Xms256m",
    "--java-options", "-Xmx1024m",
    "--java-options", "-Dserver.port=8080",
    "--java-options", "-Dspring.profiles.active=sqlite",
    "--java-options", "-Dqsdpdp.features.auto-seed-data=true"
)

if (Test-Path $IconFile) {
    $jpackageArgs += @("--icon", $IconFile)
    Write-Host "  OK Using icon: $IconFile" -ForegroundColor Green
}

Write-Host "  Running jpackage..." -ForegroundColor DarkGray
& jpackage @jpackageArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: jpackage failed." -ForegroundColor Red
    exit 1
}

# Step 4: Verify Output
Write-Host ""
Write-Host "[4/4] Verifying output..." -ForegroundColor Yellow

$exePath = Join-Path $OutputDir "$AppName\$AppName.exe"
if (Test-Path $exePath) {
    $folderSize = [math]::Round(((Get-ChildItem -Recurse (Join-Path $OutputDir $AppName) | Measure-Object -Property Length -Sum).Sum) / 1MB, 1)

    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "  BUILD SUCCESSFUL" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "  EXE Path : $exePath" -ForegroundColor Green
    Write-Host "  Total Size: $folderSize MB" -ForegroundColor Green
    Write-Host "" -ForegroundColor Green
    Write-Host "  USAGE:" -ForegroundColor Green
    Write-Host "  1. Double-click QS-DPDP-Enterprise.exe" -ForegroundColor Green
    Write-Host "  2. Open http://localhost:8080 in your browser" -ForegroundColor Green
    Write-Host "  3. Web app (port 3000) connects automatically" -ForegroundColor Green
    Write-Host "  4. Mobile app (port 5000) connects automatically" -ForegroundColor Green
    Write-Host "  NO Java installation required!" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
}
else {
    Write-Host "ERROR: EXE not found at expected path: $exePath" -ForegroundColor Red
    Get-ChildItem -Recurse $OutputDir | Select-Object FullName | Format-Table
    exit 1
}
