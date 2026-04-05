#!/bin/bash
# ═══════════════════════════════════════════════════════════
# QS-DPDP Enterprise — Database Backup Script
# Creates timestamped backups with configurable retention
# Usage: ./backup.sh [install_dir] [retention_days]
# ═══════════════════════════════════════════════════════════

INSTALL_DIR="${1:-/opt/qsdpdp}"
RETENTION_DAYS="${2:-30}"
BACKUP_DIR="$INSTALL_DIR/backups"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_NAME="qsdpdp-backup-$TIMESTAMP"

echo "═══════════════════════════════════════════════════════"
echo "  QS-DPDP Enterprise — Database Backup"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "═══════════════════════════════════════════════════════"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# ── 1. Stop writes (optional: put app in maintenance mode) ──
echo ""
echo "▸ Creating backup: $BACKUP_NAME"

# ── 2. Copy database files ──
BACKUP_PATH="$BACKUP_DIR/$BACKUP_NAME"
mkdir -p "$BACKUP_PATH"

# Copy SQLite database from Docker volume
docker cp qsdpdp-enterprise:/app/data/. "$BACKUP_PATH/data/" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "  ✓ Database files copied"
else
    echo "  ⚠ Could not copy from container — trying volume directly"
    VOLUME_PATH=$(docker volume inspect qsdpdp-data --format '{{.Mountpoint}}' 2>/dev/null)
    if [ -n "$VOLUME_PATH" ] && [ -d "$VOLUME_PATH" ]; then
        cp -r "$VOLUME_PATH/." "$BACKUP_PATH/data/"
        echo "  ✓ Database files copied from volume"
    else
        echo "  ✗ Failed to locate database files"
    fi
fi

# ── 3. Copy configuration ──
if [ -d "$INSTALL_DIR/config" ]; then
    cp -r "$INSTALL_DIR/config" "$BACKUP_PATH/config/"
    echo "  ✓ Configuration files copied"
fi

# ── 4. Copy .env ──
if [ -f "$INSTALL_DIR/.env" ]; then
    cp "$INSTALL_DIR/.env" "$BACKUP_PATH/.env"
    echo "  ✓ Environment file copied"
fi

# ── 5. Compress ──
echo ""
echo "▸ Compressing backup..."
cd "$BACKUP_DIR"
tar -czf "${BACKUP_NAME}.tar.gz" "$BACKUP_NAME"
rm -rf "$BACKUP_NAME"
BACKUP_SIZE=$(du -sh "${BACKUP_NAME}.tar.gz" | cut -f1)
echo "  ✓ Archive created: ${BACKUP_NAME}.tar.gz ($BACKUP_SIZE)"

# ── 6. Cleanup old backups ──
echo ""
echo "▸ Cleaning up backups older than $RETENTION_DAYS days..."
DELETED=$(find "$BACKUP_DIR" -name "qsdpdp-backup-*.tar.gz" -mtime +$RETENTION_DAYS -delete -print | wc -l)
echo "  ✓ Removed $DELETED old backup(s)"

# ── 7. Summary ──
TOTAL_BACKUPS=$(ls "$BACKUP_DIR"/qsdpdp-backup-*.tar.gz 2>/dev/null | wc -l)
TOTAL_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  ✅ Backup Complete"
echo "  Archive:    $BACKUP_DIR/${BACKUP_NAME}.tar.gz"
echo "  Total:      $TOTAL_BACKUPS backup(s), $TOTAL_SIZE total"
echo "  Retention:  $RETENTION_DAYS days"
echo "═══════════════════════════════════════════════════════"
