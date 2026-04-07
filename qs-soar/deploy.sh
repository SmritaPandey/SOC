#!/bin/bash
# QS-SOAR — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-SOAR — Security Orchestration, Automation & Response"
echo "  Port: 9002"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-soar -DskipTests -q
cd qs-soar
nohup java -jar target/qs-soar-1.0.0.jar &
echo "QS-SOAR starting on http://localhost:9002"