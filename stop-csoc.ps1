# ═══════════════════════════════════════════════════════════
# QShield CSOC Platform — Stop All Services
# ═══════════════════════════════════════════════════════════

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Yellow
Write-Host "║       Stopping QShield CSOC Services...          ║" -ForegroundColor Yellow
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Yellow

$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force
    Write-Host "  ✅ Stopped $($javaProcesses.Count) Java process(es)." -ForegroundColor Green
} else {
    Write-Host "  ℹ️  No Java processes running." -ForegroundColor Cyan
}

Write-Host ""
Write-Host "All CSOC services stopped." -ForegroundColor Green
