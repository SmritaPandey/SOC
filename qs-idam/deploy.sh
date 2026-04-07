#!/bin/bash
# QS-IDAM — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-IDAM — Identity & Access Management"
echo "  Port: 9006"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-idam -DskipTests -q
cd qs-idam
nohup java -jar target/qs-idam-1.0.0.jar &
echo "QS-IDAM starting on http://localhost:9006"