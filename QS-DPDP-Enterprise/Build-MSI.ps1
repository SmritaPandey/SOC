# QS-DPDP Enterprise - MSI Installer Build Script
# Supports: WiX Toolset 3.11+ (wizard experience) OR jpackage (fallback)

$ErrorActionPreference = "Stop"

# Paths
$ProjectDir = "D:\N_DPDP\QS-DPDP-Enterprise"
$TargetDir = "$ProjectDir\target"
$InstallerDir = "$ProjectDir\installer"
$JarFile = "$TargetDir\qs-dpdp-enterprise-1.0.0.jar"
$WxsFile = "$InstallerDir\Product.wxs"

# App Info
$AppName = "QS-DPDP Enterprise"
$AppVersion = "1.0.0"
$Vendor = "QualitySync Technologies"
$Description = "Enterprise Compliance Operating System for DPDP Act 2023"
$Copyright = "Copyright (c) 2024 QualitySync Technologies"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║          Building QS-DPDP Enterprise Installer             ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

# ═══════════════════════════════════════════════════════════
# Step 1: Check prerequisites
# ═══════════════════════════════════════════════════════════
Write-Host "`n[1/6] Checking prerequisites..."

if (-not (Test-Path $JarFile)) {
    Write-Host "  JAR not found. Building with Maven..." -ForegroundColor Yellow
    Push-Location $ProjectDir
    & mvn package -DskipTests -q
    Pop-Location
    if (-not (Test-Path $JarFile)) {
        Write-Host "  ERROR: Build failed. JAR not found at $JarFile" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  ✓ JAR file: $JarFile" -ForegroundColor Green

# Icon check
$IconFile = "$InstallerDir\app-icon.ico"
if (Test-Path $IconFile) {
    Write-Host "  ✓ Custom icon: $IconFile" -ForegroundColor Green
}
else {
    Write-Host "  ⚠ No custom icon found (using default)" -ForegroundColor Yellow
}

# ═══════════════════════════════════════════════════════════
# Step 2: Detect build method
# ═══════════════════════════════════════════════════════════
Write-Host "`n[2/6] Detecting build method..."

$wixCandle = Get-Command candle.exe -ErrorAction SilentlyContinue
$wixLight = Get-Command light.exe -ErrorAction SilentlyContinue
$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
$useWix = $false

if ($wixCandle -and $wixLight -and (Test-Path $WxsFile)) {
    $useWix = $true
    Write-Host "  ✓ WiX Toolset detected — Building FULL WIZARD installer" -ForegroundColor Green
    Write-Host "    Features: Install / Repair / Uninstall / Directory Chooser" -ForegroundColor DarkGray
}
elseif ($jpackage) {
    Write-Host "  ✓ jpackage detected — Building basic MSI installer" -ForegroundColor Green
    Write-Host "  ⚠ Install WiX Toolset for full wizard experience" -ForegroundColor Yellow
}
else {
    Write-Host "  ERROR: Neither WiX nor jpackage found." -ForegroundColor Red
    Write-Host "  Install JDK 17+ (for jpackage) or WiX 3.11+ (for wizard installer)" -ForegroundColor Red
    exit 1
}

# ═══════════════════════════════════════════════════════════
# Step 3: Prepare installer directory
# ═══════════════════════════════════════════════════════════
Write-Host "`n[3/6] Preparing installer directory..."
New-Item -ItemType Directory -Force -Path $InstallerDir | Out-Null
Write-Host "  ✓ Installer dir: $InstallerDir" -ForegroundColor Green

# ═══════════════════════════════════════════════════════════
# Step 4: Copy launch script
# ═══════════════════════════════════════════════════════════
Write-Host "`n[4/6] Preparing launch resources..."
$launchBat = "$InstallerDir\launch.bat"
if (Test-Path $launchBat) {
    Write-Host "  ✓ Launch script: $launchBat" -ForegroundColor Green
}

# ═══════════════════════════════════════════════════════════
# Step 5: Build MSI
# ═══════════════════════════════════════════════════════════
Write-Host "`n[5/6] Building MSI installer..."

if ($useWix) {
    # ─── WIX BUILD (Full Wizard) ─────────────────────────
    Write-Host "  Building with WiX Toolset..." -ForegroundColor Cyan

    $WixDefines = "-dTargetDir=$TargetDir -dInstallerDir=$InstallerDir"
    $ObjFile = "$InstallerDir\Product.wixobj"
    $MsiOutput = "$InstallerDir\QS-DPDP-Enterprise-$AppVersion.msi"

    # Compile
    Write-Host "  Running: candle.exe $WxsFile"
    & candle.exe $WxsFile -out $ObjFile $WixDefines.Split(" ") -ext WixUIExtension
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ERROR: WiX candle failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✓ Compiled WiX source" -ForegroundColor Green

    # Link
    Write-Host "  Running: light.exe $ObjFile"
    & light.exe $ObjFile -out $MsiOutput -ext WixUIExtension -cultures:en-us
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ERROR: WiX light failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✓ Linked MSI package" -ForegroundColor Green

}
else {
    # ─── JPACKAGE BUILD (Basic MSI) ─────────────────────
    Write-Host "  Building with jpackage..." -ForegroundColor Cyan

    $JpackageArgs = @(
        "--type", "msi",
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", $Description,
        "--copyright", $Copyright,
        "--input", $TargetDir,
        "--main-jar", "qs-dpdp-enterprise-1.0.0.jar",
        "--main-class", "com.qsdpdp.ui.QSDPDPLauncher",
        "--dest", $InstallerDir,
        "--win-menu",
        "--win-menu-group", "QualitySync",
        "--win-shortcut",
        "--win-shortcut-prompt",
        "--win-dir-chooser",
        "--java-options", "-Xms256m",
        "--java-options", "-Xmx1024m"
    )

    if (Test-Path $IconFile) {
        $JpackageArgs += @("--icon", $IconFile)
    }

    & jpackage @JpackageArgs
    Write-Host "  ✓ MSI package created" -ForegroundColor Green
}

# ═══════════════════════════════════════════════════════════
# Step 6: Verify output
# ═══════════════════════════════════════════════════════════
Write-Host "`n[6/6] Verifying output..."
$MsiFile = Get-ChildItem -Path $InstallerDir -Filter "*.msi" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($MsiFile) {
    $SizeMB = [math]::Round($MsiFile.Length / 1MB, 1)
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║                    BUILD SUCCESSFUL!                       ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "  MSI Installer : $($MsiFile.FullName)"
    Write-Host "  Size          : $SizeMB MB"
    Write-Host "  Build Method  : $(if ($useWix) { 'WiX (Full Wizard)' } else { 'jpackage (Basic)' })"
    Write-Host ""
    Write-Host "  To install:" -ForegroundColor Cyan
    Write-Host "    Double-click the MSI file  OR  run:"
    Write-Host "    msiexec /i `"$($MsiFile.FullName)`""
    Write-Host ""
    Write-Host "  After installation:" -ForegroundColor Cyan
    Write-Host "    • Launch from Start Menu → QualitySync → QS-DPDP Enterprise"
    Write-Host "    • Or right-click MSI → Repair / Uninstall"
    Write-Host ""
}
else {
    Write-Host "  WARNING: MSI file not found in output directory" -ForegroundColor Yellow
    Get-ChildItem -Path $InstallerDir
}
