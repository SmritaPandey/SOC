#!/bin/bash
# QS-DLP — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-DLP — Data Loss Prevention"
echo "  Port: 9005"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-dlp -DskipTests -q
cd qs-dlp
nohup java -jar target/qs-dlp-1.0.0.jar &
echo "QS-DLP starting on http://localhost:9005"