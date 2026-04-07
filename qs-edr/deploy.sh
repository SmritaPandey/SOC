#!/bin/bash
# QS-EDR — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-EDR — Endpoint Detection & Response"
echo "  Port: 9003"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-edr -DskipTests -q
cd qs-edr
nohup java -jar target/qs-edr-1.0.0.jar &
echo "QS-EDR starting on http://localhost:9003"