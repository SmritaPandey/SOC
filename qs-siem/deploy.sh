#!/bin/bash
# QS-SIEM — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-SIEM — Security Information & Event Management"
echo "  Port: 9001"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-siem -DskipTests -q
cd qs-siem
nohup java -jar target/qs-siem-1.0.0.jar &
echo "QS-SIEM starting on http://localhost:9001"