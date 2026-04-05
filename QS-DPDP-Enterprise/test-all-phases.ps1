# QS-DPDP Enterprise - All 13 Phases API Verification Script
$base = "http://localhost:8080"
$pass = 0
$fail = 0
$results = @()

function Test-API {
    param([string]$Phase, [string]$Name, [string]$Url, [string]$Method = "GET", [string]$Body = "")
    try {
        if ($Method -eq "POST") {
            $r = Invoke-WebRequest -Uri $Url -Method POST -ContentType "application/json" -Body $Body -UseBasicParsing -TimeoutSec 10
        } else {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
        }
        $status = $r.StatusCode
        $len = $r.Content.Length
        $snippet = if ($len -gt 120) { $r.Content.Substring(0, 120) + "..." } else { $r.Content }
        $script:pass++
        return "$Phase | $Name | $status OK | ${len}B | $snippet"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if (-not $code) { $code = "ERR" }
        $script:fail++
        return "$Phase | $Name | $code FAIL | $($_.Exception.Message)"
    }
}

# Wait for server
Write-Host "Waiting for server..."
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "$base/api/dashboard" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (-not $ready) { Write-Host "SERVER NOT READY AFTER 60s"; exit 1 }
Write-Host "Server is ready!`n"

# ========= PHASE 1: SYNC =========
$results += Test-API "P1" "Sync Status" "$base/api/sync/status"

# ========= PHASE 2: CONNECTORS =========
$results += Test-API "P2" "Connectors Overview" "$base/api/connectors/overview"
$results += Test-API "P2" "Connectors All" "$base/api/connectors"
$results += Test-API "P2" "Connectors BFSI" "$base/api/connectors/sector/BFSI"
$results += Test-API "P2" "Connectors Enabled" "$base/api/connectors/enabled"

# ========= PHASE 3: PII =========
$results += Test-API "P3" "PII Rules" "$base/api/pii-discovery/rules"

# ========= PHASE 4: CONSENT VALIDATION =========
$results += Test-API "P4" "Validation Stats" "$base/api/consent-validation/statistics"
$results += Test-API "P4" "Violations" "$base/api/consent-validation/violations?status=OPEN&limit=5"
$results += Test-API "P4" "Validate Access" "$base/api/consent-validation/validate" "POST" '{"principalId":"test-001","dataCategory":"AADHAAR","purpose":"KYC","accessedBy":"system","volume":1}'

# ========= PHASE 5: RBI =========
$results += Test-API "P5" "RBI Domains" "$base/api/rbi-compliance/domains"
$results += Test-API "P5" "RBI Score" "$base/api/rbi-compliance/score"

# ========= PHASE 6: GRC =========
$results += Test-API "P6" "GRC Overview" "$base/api/grc/overview"
$results += Test-API "P6" "GRC Templates" "$base/api/grc/templates"
$results += Test-API "P6" "GRC Controls" "$base/api/grc/controls"
$results += Test-API "P6" "GRC Risks" "$base/api/grc/risks"
$results += Test-API "P6" "GRC Policies" "$base/api/grc/policies"

# ========= PHASE 7: WEB (check JS file exists) =========
$results += Test-API "P7" "Data Principal JS" "$base/js/data-principal.js?v=20260326"
$results += Test-API "P7" "Sync Client JS" "$base/js/sync-client.js?v=20260326"

# ========= PHASE 8: MOBILE =========
$results += Test-API "P8" "Mobile Dashboard" "$base/api/mobile/dashboard"
$results += Test-API "P8" "Mobile Register" "$base/api/mobile/register" "POST" '{"deviceId":"test-device","platform":"ANDROID","pushToken":"test-token-123"}'

# ========= PHASE 9: VOICE =========
$results += Test-API "P9" "Voice Languages" "$base/api/voice-consent/languages"
$results += Test-API "P9" "Voice Prompt" "$base/api/voice-consent/prompt?language=hi&purpose=data+collection"

# ========= PHASE 10: INCIDENTS =========
$results += Test-API "P10" "Incident Languages" "$base/api/incidents/languages"
$results += Test-API "P10" "Detect Incident" "$base/api/incidents/detect" "POST" '{"type":"DATA_BREACH","severity":"HIGH","description":"Test breach","affectedSystem":"CRM","estimatedAffected":100}'

# ========= PHASE 11: AI RISK =========
$results += Test-API "P11" "Org Risk" "$base/api/ai-risk/organization"
$results += Test-API "P11" "AI Query" "$base/api/ai-risk/query" "POST" '{"query":"How do I revoke consent?"}'

# ========= PHASE 12: LEDGER =========
$results += Test-API "P12" "Ledger Stats" "$base/api/compliance-ledger/statistics"
$results += Test-API "P12" "Ledger Validate" "$base/api/compliance-ledger/validate"
$results += Test-API "P12" "Ledger Record" "$base/api/compliance-ledger/record" "POST" '{"action":"TEST_ENTRY","category":"VERIFICATION","entityType":"SYSTEM","entityId":"test-001","actor":"admin","details":"Phase 13 verification test"}'
$results += Test-API "P12" "Ledger Entries" "$base/api/compliance-ledger/entries?limit=5"

# ========= EXISTING MODULES =========
$results += Test-API "EX" "Dashboard API" "$base/api/dashboard"
$results += Test-API "EX" "Consent Stats" "$base/api/consent/statistics"

# ========= RESULTS =========
Write-Host "`n=========================================="
Write-Host "  QS-DPDP ENTERPRISE - PHASE VERIFICATION"
Write-Host "=========================================="
Write-Host ""
foreach ($r in $results) {
    Write-Host $r
}
Write-Host "`n=========================================="
Write-Host "TOTAL: $($pass + $fail) tests | PASS: $pass | FAIL: $fail"
Write-Host "=========================================="
