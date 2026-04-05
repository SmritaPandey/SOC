# ═══════════════════════════════════════════════════════════
# QS-DPDP Enterprise — Windows Installation Script
# Installs and starts QS-DPDP Enterprise on a Windows server
# ═══════════════════════════════════════════════════════════

param(
    [string]$InstallDir = "C:\QS-DPDP",
    [string]$ImageArchive = "",
    [int]$Port = 8443
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  QS-DPDP Enterprise — On-Prem Installation           " -ForegroundColor Cyan
Write-Host "  Quantum-Safe DPDP Act 2023 Compliance Platform      " -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# ── 1. Check Prerequisites ──
Write-Host "[1/7] Checking prerequisites..." -ForegroundColor Yellow

# Check Docker
try {
    $dockerVersion = docker --version 2>&1
    Write-Host "  ✓ Docker: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Docker is not installed or not in PATH" -ForegroundColor Red
    Write-Host "  Please install Docker Desktop from https://docs.docker.com/desktop/install/windows-install/" -ForegroundColor Red
    exit 1
}

# Check Docker Compose
try {
    $composeVersion = docker compose version 2>&1
    Write-Host "  ✓ Docker Compose: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Docker Compose not available" -ForegroundColor Red
    exit 1
}

# ── 2. Create Directory Structure ──
Write-Host "[2/7] Creating directory structure..." -ForegroundColor Yellow

$dirs = @("$InstallDir", "$InstallDir\config", "$InstallDir\data", "$InstallDir\logs", "$InstallDir\backups")
foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "  ✓ Created: $dir" -ForegroundColor Green
    } else {
        Write-Host "  ○ Exists:  $dir" -ForegroundColor DarkGray
    }
}

# ── 3. Load Docker Image ──
Write-Host "[3/7] Loading Docker image..." -ForegroundColor Yellow

if ($ImageArchive -and (Test-Path $ImageArchive)) {
    Write-Host "  Loading from archive: $ImageArchive" -ForegroundColor DarkGray
    docker load -i $ImageArchive
    Write-Host "  ✓ Docker image loaded" -ForegroundColor Green
} else {
    Write-Host "  ○ No image archive specified — expecting image in local registry" -ForegroundColor DarkGray
    Write-Host "    To load: docker load -i qsdpdp-enterprise-3.0.0.tar.gz" -ForegroundColor DarkGray
}

# ── 4. Copy Configuration Files ──
Write-Host "[4/7] Setting up configuration..." -ForegroundColor Yellow

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Copy docker-compose if not present
$composeTarget = "$InstallDir\docker-compose.yml"
if (!(Test-Path $composeTarget)) {
    $composeSrc = Join-Path $scriptDir "..\docker-compose.prod.yml"
    if (Test-Path $composeSrc) {
        Copy-Item $composeSrc $composeTarget
        Write-Host "  ✓ docker-compose.yml copied" -ForegroundColor Green
    }
}

# Copy .env template
$envTarget = "$InstallDir\.env"
if (!(Test-Path $envTarget)) {
    $envSrc = Join-Path $scriptDir "..\.env.template"
    if (Test-Path $envSrc) {
        Copy-Item $envSrc $envTarget
        # Set the port
        (Get-Content $envTarget) -replace "QSDPDP_PORT=8443", "QSDPDP_PORT=$Port" | Set-Content $envTarget
        Write-Host "  ✓ .env created (port: $Port)" -ForegroundColor Green
    }
}

# Copy production config
$configTarget = "$InstallDir\config\application-production.yml"
if (!(Test-Path $configTarget)) {
    $configSrc = Join-Path $scriptDir "..\config\application-production.yml"
    if (Test-Path $configSrc) {
        Copy-Item $configSrc $configTarget
        Write-Host "  ✓ application-production.yml copied" -ForegroundColor Green
    }
}

# ── 5. Check License ──
Write-Host "[5/7] Checking license..." -ForegroundColor Yellow

$licenseFile = "$InstallDir\config\license.key"
if (Test-Path $licenseFile) {
    Write-Host "  ✓ License file found: $licenseFile" -ForegroundColor Green
} else {
    Write-Host "  ⚠ No license file found at: $licenseFile" -ForegroundColor Yellow
    Write-Host "    The application will start in DEMO mode (14 days)" -ForegroundColor Yellow
    Write-Host "    To activate:" -ForegroundColor Yellow
    Write-Host "    1. Start the application" -ForegroundColor DarkGray
    Write-Host "    2. GET http://localhost:$Port/api/licensing/fingerprint" -ForegroundColor DarkGray
    Write-Host "    3. Send fingerprint to NeurQ AI Labs for license generation" -ForegroundColor DarkGray
    Write-Host "    4. Place license.key in $InstallDir\config\" -ForegroundColor DarkGray
    Write-Host "    5. POST the file content to /api/licensing/activate-file" -ForegroundColor DarkGray
}

# ── 6. Start Services ──
Write-Host "[6/7] Starting QS-DPDP Enterprise..." -ForegroundColor Yellow

Push-Location $InstallDir
try {
    docker compose up -d
    Write-Host "  ✓ Services started" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Failed to start: $_" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

# ── 7. Verify ──
Write-Host "[7/7] Verifying deployment..." -ForegroundColor Yellow

Write-Host "  Waiting for application to start (30 seconds)..." -ForegroundColor DarkGray
Start-Sleep -Seconds 30

try {
    $response = Invoke-WebRequest -Uri "http://localhost:$Port/api/v1/health" -UseBasicParsing -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✓ Health check passed!" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Health check returned: $($response.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠ Application may still be starting. Check:" -ForegroundColor Yellow
    Write-Host "    docker logs qsdpdp-enterprise" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  ✅ QS-DPDP Enterprise Installation Complete          " -ForegroundColor Green
Write-Host "                                                       " -ForegroundColor Cyan
Write-Host "  Dashboard: http://localhost:$Port                    " -ForegroundColor White
Write-Host "  REST API:  http://localhost:$Port/api/dashboard      " -ForegroundColor White
Write-Host "  Swagger:   http://localhost:$Port/swagger-ui.html    " -ForegroundColor White
Write-Host "  Logs:      $InstallDir\logs\                         " -ForegroundColor White
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
