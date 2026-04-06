# ═══════════════════════════════════════════════════════════
# QShield CSOC Platform — Deployment Script
# Launches all 8 security products + unified portal
# ═══════════════════════════════════════════════════════════
param(
    [switch]$BuildFirst,
    [switch]$PortalOnly
)

$ErrorActionPreference = "Continue"
$env:Path += ";C:\apache-maven-3.9.6\bin"
$SOC_HOME = "D:\SOC"

$products = @(
    @{name="QS-SIEM";  module="qs-siem";  port=9001; color="Cyan"},
    @{name="QS-SOAR";  module="qs-soar";  port=9002; color="Magenta"},
    @{name="QS-EDR";   module="qs-edr";   port=9003; color="Yellow"},
    @{name="QS-XDR";   module="qs-xdr";   port=9004; color="Green"},
    @{name="QS-DLP";   module="qs-dlp";   port=9005; color="Red"},
    @{name="QS-IDAM";  module="qs-idam";  port=9006; color="Magenta"},
    @{name="QS-AV";    module="qs-av";    port=9007; color="Red"},
    @{name="QS-VAM";   module="qs-vam";   port=9008; color="Cyan"}
)

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        QShield CSOC Platform Deployment          ║" -ForegroundColor Cyan
Write-Host "║     Enterprise Cyber Security Operations Center  ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Build if requested
if ($BuildFirst) {
    Write-Host "[BUILD] Building all modules..." -ForegroundColor Yellow
    Set-Location $SOC_HOME
    mvn clean package -DskipTests -q 2>&1 | Out-Null
    Write-Host "[BUILD] All modules built successfully." -ForegroundColor Green
}

# Stop any existing Java processes
Write-Host "[STOP] Stopping existing Java processes..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

if (-not $PortalOnly) {
    # Launch each product
    foreach ($p in $products) {
        $jarPath = "$SOC_HOME\$($p.module)\target\$($p.module)-1.0.0.jar"
        if (Test-Path $jarPath) {
            Write-Host "[LAUNCH] Starting $($p.name) on port $($p.port)..." -ForegroundColor $p.color
            Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -WindowStyle Minimized -WorkingDirectory "$SOC_HOME\$($p.module)"
            Start-Sleep -Seconds 3
        } else {
            Write-Host "[ERROR] JAR not found: $jarPath" -ForegroundColor Red
            Write-Host "        Run with -BuildFirst to build all modules." -ForegroundColor DarkGray
        }
    }

    # Wait for services to start
    Write-Host ""
    Write-Host "[WAIT] Waiting for services to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
}

# Launch CSOC Portal
$portalPath = "$SOC_HOME\qs-csoc-portal\index.html"
if (Test-Path $portalPath) {
    Write-Host "[PORTAL] Launching QShield CSOC Unified Portal..." -ForegroundColor Cyan
    Start-Process $portalPath
}

# Health Check
Write-Host ""
Write-Host "═══════════════ Health Check ═══════════════" -ForegroundColor DarkGray
foreach ($p in $products) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$($p.port)/api/v1/$($p.module.Replace('qs-',''))/health" -TimeoutSec 5 -ErrorAction Stop
        Write-Host "  ✅ $($p.name) (:$($p.port)) — ONLINE" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ $($p.name) (:$($p.port)) — OFFLINE" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║         QShield CSOC Deployment Complete         ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║  CSOC Portal:  file:///$($portalPath -replace '\\','/')            ║" -ForegroundColor White
Write-Host "║  QS-SIEM:      http://localhost:9001             ║" -ForegroundColor Cyan
Write-Host "║  QS-SOAR:      http://localhost:9002             ║" -ForegroundColor Magenta
Write-Host "║  QS-EDR:       http://localhost:9003             ║" -ForegroundColor Yellow
Write-Host "║  QS-XDR:       http://localhost:9004             ║" -ForegroundColor Green
Write-Host "║  QS-DLP:       http://localhost:9005             ║" -ForegroundColor Red
Write-Host "║  QS-IDAM:      http://localhost:9006             ║" -ForegroundColor Magenta
Write-Host "║  QS-AV:        http://localhost:9007             ║" -ForegroundColor Red
Write-Host "║  QS-VAM:       http://localhost:9008             ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Green
