#!/bin/bash
# ═══════════════════════════════════════════════════════════
# QS-DPDP Enterprise — Linux Installation Script
# Installs and starts QS-DPDP Enterprise on Linux
# ═══════════════════════════════════════════════════════════

set -e

INSTALL_DIR="${1:-/opt/qsdpdp}"
PORT="${2:-8443}"
IMAGE_ARCHIVE="${3:-}"

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  QS-DPDP Enterprise — On-Prem Installation           ${NC}"
echo -e "${CYAN}  Quantum-Safe DPDP Act 2023 Compliance Platform      ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""

# ── 1. Check Prerequisites ──
echo -e "${YELLOW}[1/7] Checking prerequisites...${NC}"

if command -v docker &> /dev/null; then
    echo -e "  ${GREEN}✓ Docker: $(docker --version)${NC}"
else
    echo -e "  ${RED}✗ Docker is not installed${NC}"
    echo "  Install: curl -fsSL https://get.docker.com | sh"
    exit 1
fi

if docker compose version &> /dev/null; then
    echo -e "  ${GREEN}✓ Docker Compose: $(docker compose version --short)${NC}"
else
    echo -e "  ${RED}✗ Docker Compose not available${NC}"
    exit 1
fi

# ── 2. Create Directory Structure ──
echo -e "${YELLOW}[2/7] Creating directory structure...${NC}"

for dir in "$INSTALL_DIR" "$INSTALL_DIR/config" "$INSTALL_DIR/data" "$INSTALL_DIR/logs" "$INSTALL_DIR/backups"; do
    mkdir -p "$dir"
    echo -e "  ${GREEN}✓ $dir${NC}"
done

# ── 3. Load Docker Image ──
echo -e "${YELLOW}[3/7] Loading Docker image...${NC}"

if [ -n "$IMAGE_ARCHIVE" ] && [ -f "$IMAGE_ARCHIVE" ]; then
    echo "  Loading from archive: $IMAGE_ARCHIVE"
    docker load -i "$IMAGE_ARCHIVE"
    echo -e "  ${GREEN}✓ Docker image loaded${NC}"
else
    echo "  ○ No image archive specified — expecting image in local registry"
fi

# ── 4. Copy Configuration ──
echo -e "${YELLOW}[4/7] Setting up configuration...${NC}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ ! -f "$INSTALL_DIR/docker-compose.yml" ]; then
    cp "$SCRIPT_DIR/../docker-compose.prod.yml" "$INSTALL_DIR/docker-compose.yml" 2>/dev/null || true
    echo -e "  ${GREEN}✓ docker-compose.yml${NC}"
fi

if [ ! -f "$INSTALL_DIR/.env" ]; then
    if [ -f "$SCRIPT_DIR/../.env.template" ]; then
        cp "$SCRIPT_DIR/../.env.template" "$INSTALL_DIR/.env"
        sed -i "s/QSDPDP_PORT=8443/QSDPDP_PORT=$PORT/" "$INSTALL_DIR/.env"
        echo -e "  ${GREEN}✓ .env created (port: $PORT)${NC}"
    fi
fi

if [ ! -f "$INSTALL_DIR/config/application-production.yml" ]; then
    cp "$SCRIPT_DIR/../config/application-production.yml" "$INSTALL_DIR/config/" 2>/dev/null || true
    echo -e "  ${GREEN}✓ application-production.yml${NC}"
fi

# ── 5. Check License ──
echo -e "${YELLOW}[5/7] Checking license...${NC}"

if [ -f "$INSTALL_DIR/config/license.key" ]; then
    echo -e "  ${GREEN}✓ License file found${NC}"
else
    echo -e "  ${YELLOW}⚠ No license file found at: $INSTALL_DIR/config/license.key${NC}"
    echo "    Application will start in DEMO mode (14 days)"
    echo "    To activate:"
    echo "    1. Start the application"
    echo "    2. GET http://localhost:$PORT/api/licensing/fingerprint"
    echo "    3. Send fingerprint to NeurQ AI Labs"
    echo "    4. Place license.key in $INSTALL_DIR/config/"
fi

# ── 6. Start Services ──
echo -e "${YELLOW}[6/7] Starting QS-DPDP Enterprise...${NC}"

cd "$INSTALL_DIR"
docker compose up -d
echo -e "  ${GREEN}✓ Services started${NC}"

# ── 7. Verify ──
echo -e "${YELLOW}[7/7] Verifying deployment...${NC}"

echo "  Waiting for application to start (30 seconds)..."
sleep 30

if curl -sf "http://localhost:$PORT/api/v1/health" > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓ Health check passed!${NC}"
else
    echo -e "  ${YELLOW}⚠ Application may still be starting. Check: docker logs qsdpdp-enterprise${NC}"
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ QS-DPDP Enterprise Installation Complete${NC}"
echo -e "  Dashboard: http://localhost:$PORT"
echo -e "  REST API:  http://localhost:$PORT/api/dashboard"
echo -e "  Swagger:   http://localhost:$PORT/swagger-ui.html"
echo -e "  Logs:      $INSTALL_DIR/logs/"
echo -e "${CYAN}═══════════════════════════════════════════════════════${NC}"
echo ""
