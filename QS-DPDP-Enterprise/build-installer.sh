#!/bin/bash
# ═══════════════════════════════════════════════════════════
# QS-DPDP Enterprise v3.0 — Linux DEB/RPM Installer Builder
# Uses JDK 21 jpackage for native Linux packages
# ═══════════════════════════════════════════════════════════

set -e

APP_NAME="qs-dpdp-enterprise"
APP_VERSION="3.0.0"
VENDOR="QS-DPDP Security"
MAIN_JAR="qs-dpdp-enterprise-3.0.0.jar"

echo "╔═══════════════════════════════════════════════════════╗"
echo "║  QS-DPDP Enterprise v3.0 — Linux Installer Builder   ║"
echo "╚═══════════════════════════════════════════════════════╝"

# Build the application
echo "[1/3] Building application..."
mvn clean package -DskipTests -B

# Build DEB package (Ubuntu/Debian)
echo "[2/3] Building DEB package..."
jpackage \
  --type deb \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "$VENDOR" \
  --description "Quantum-Safe DPDP Compliance Platform" \
  --input target \
  --main-jar "$MAIN_JAR" \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --dest dist \
  --linux-menu-group "Security" \
  --linux-shortcut \
  --java-options "-Xmx512m -XX:+UseG1GC -Dspring.profiles.active=production"

# Build RPM package (RHEL/CentOS)
echo "[3/3] Building RPM package..."
jpackage \
  --type rpm \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "$VENDOR" \
  --description "Quantum-Safe DPDP Compliance Platform" \
  --input target \
  --main-jar "$MAIN_JAR" \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --dest dist \
  --java-options "-Xmx512m -XX:+UseG1GC -Dspring.profiles.active=production"

# Generate checksums
sha256sum dist/*.deb dist/*.rpm > dist/checksums.txt 2>/dev/null

echo ""
echo "✅ Build complete! Packages in: dist/"
ls -lh dist/
