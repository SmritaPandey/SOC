#!/bin/bash
# ═══════════════════════════════════════════════════════════
# QS-DPDP Enterprise — Health Check Script
# Monitors application health, license status, and resources
# Usage: ./health-check.sh [port]
# ═══════════════════════════════════════════════════════════

PORT="${1:-8443}"
BASE_URL="http://localhost:$PORT"
EXIT_CODE=0

echo "═══════════════════════════════════════════════════════"
echo "  QS-DPDP Enterprise Health Check"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "═══════════════════════════════════════════════════════"

# ── 1. Application Health ──
echo ""
echo "▸ Application Health"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/health" 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "  ✓ API: UP (HTTP $HTTP_CODE)"
else
    echo "  ✗ API: DOWN (HTTP $HTTP_CODE)"
    EXIT_CODE=1
fi

# ── 2. License Status ──
echo ""
echo "▸ License Status"
LICENSE_RESPONSE=$(curl -s "$BASE_URL/api/licensing/status" 2>/dev/null)
if echo "$LICENSE_RESPONSE" | grep -q '"status"'; then
    LICENSE_TYPE=$(echo "$LICENSE_RESPONSE" | grep -o '"type":"[^"]*"' | cut -d'"' -f4)
    LICENSE_STATUS=$(echo "$LICENSE_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    REMAINING=$(echo "$LICENSE_RESPONSE" | grep -o '"remainingDays":[0-9]*' | cut -d':' -f2)
    echo "  Type:      $LICENSE_TYPE"
    echo "  Status:    $LICENSE_STATUS"
    echo "  Remaining: ${REMAINING:-?} days"
    
    if [ -n "$REMAINING" ] && [ "$REMAINING" -lt 30 ]; then
        echo "  ⚠ WARNING: License expires in less than 30 days!"
    fi
else
    echo "  ✗ Could not retrieve license status"
    EXIT_CODE=1
fi

# ── 3. Docker Container Status ──
echo ""
echo "▸ Container Status"
if docker ps --filter "name=qsdpdp-enterprise" --format '{{.Status}}' 2>/dev/null | grep -q "Up"; then
    CONTAINER_STATUS=$(docker ps --filter "name=qsdpdp-enterprise" --format '{{.Status}}')
    echo "  ✓ Container: $CONTAINER_STATUS"
else
    echo "  ✗ Container is not running"
    EXIT_CODE=1
fi

# ── 4. Disk Space ──
echo ""
echo "▸ Disk Space"
DISK_USAGE=$(df -h / | tail -1 | awk '{print $5}' | sed 's/%//')
echo "  Root disk usage: ${DISK_USAGE}%"
if [ "$DISK_USAGE" -gt 90 ]; then
    echo "  ⚠ WARNING: Disk usage above 90%!"
    EXIT_CODE=1
fi

# ── 5. Memory ──
echo ""
echo "▸ Memory"
if command -v free &> /dev/null; then
    MEM_TOTAL=$(free -m | awk '/^Mem:/{print $2}')
    MEM_USED=$(free -m | awk '/^Mem:/{print $3}')
    MEM_PERCENT=$((MEM_USED * 100 / MEM_TOTAL))
    echo "  Used: ${MEM_USED}MB / ${MEM_TOTAL}MB (${MEM_PERCENT}%)"
    if [ "$MEM_PERCENT" -gt 90 ]; then
        echo "  ⚠ WARNING: Memory usage above 90%!"
    fi
fi

# ── 6. Docker Logs (last errors) ──
echo ""
echo "▸ Recent Errors (last 5)"
ERRORS=$(docker logs qsdpdp-enterprise --since 1h 2>&1 | grep -i "ERROR" | tail -5)
if [ -n "$ERRORS" ]; then
    echo "$ERRORS" | while read -r line; do echo "  ⚠ $line"; done
else
    echo "  ✓ No errors in last hour"
fi

echo ""
echo "═══════════════════════════════════════════════════════"
if [ $EXIT_CODE -eq 0 ]; then
    echo "  ✅ All checks PASSED"
else
    echo "  ❌ Some checks FAILED — investigate above warnings"
fi
echo "═══════════════════════════════════════════════════════"

exit $EXIT_CODE
