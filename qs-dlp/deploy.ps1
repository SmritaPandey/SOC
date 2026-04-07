# QS-DLP — Individual Deployment Script
$ErrorActionPreference = "Continue"
$env:Path += ";C:\apache-maven-3.9.6\bin"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  QS-DLP — Data Loss Prevention" -ForegroundColor Cyan
Write-Host "  Port: 9005" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Build
Write-Host "[1/3] Building QS-DLP..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot\.."
mvn install -pl qs-common -DskipTests -q 2>&1 | Out-Null
mvn package -pl qs-dlp -DskipTests -q 2>&1 | Out-Null
Write-Host "      Build complete." -ForegroundColor Green

# Launch
Write-Host "[2/3] Starting QS-DLP on port 9005..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot"
Start-Process -FilePath "java" -ArgumentList "-jar", "target/qs-dlp-1.0.0.jar" -WindowStyle Minimized
Start-Sleep -Seconds 10

# Health Check
Write-Host "[3/3] Health Check..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "http://localhost:9005/api/v1/dlp/health" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "      QS-DLP is ONLINE at http://localhost:9005" -ForegroundColor Green
} catch {
    Write-Host "      QS-DLP is still starting... check http://localhost:9005 in a moment." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "Dashboard:  http://localhost:9005" -ForegroundColor White
Write-Host "API Base:   http://localhost:9005/api/v1/dlp" -ForegroundColor White
Write-Host "H2 Console: http://localhost:9005/h2-console" -ForegroundColor DarkGray