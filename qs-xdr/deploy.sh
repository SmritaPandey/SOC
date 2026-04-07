#!/bin/bash
# QS-XDR — Individual Deployment (Linux/Mac)
echo "========================================"
echo "  QS-XDR — Extended Detection & Response"
echo "  Port: 9004"
echo "========================================"
cd "$(dirname "$0")/.."
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-xdr -DskipTests -q
cd qs-xdr
nohup java -jar target/qs-xdr-1.0.0.jar &
echo "QS-XDR starting on http://localhost:9004"