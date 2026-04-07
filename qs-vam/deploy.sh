#!/bin/bash
# QS-VAM — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-VAM — Vulnerability Assessment & Management"
echo "  Port: 9008"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-vam -DskipTests -q
cd qs-vam
nohup java -jar target/qs-vam-1.0.0.jar &
echo "QS-VAM starting on http://localhost:9008"