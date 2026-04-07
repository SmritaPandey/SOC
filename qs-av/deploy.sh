#!/bin/bash
# QS-AV — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-AV — Antivirus & Endpoint Protection"
echo "  Port: 9007"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-av -DskipTests -q
cd qs-av
nohup java -jar target/qs-av-1.0.0.jar &
echo "QS-AV starting on http://localhost:9007"