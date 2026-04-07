# QS-SIEM — Operational Manual
## Security Information & Event Management v1.0.0

---

## 1. Product Overview

**QS-SIEM** is an enterprise-grade Security Information and Event Management system that provides real-time log collection, threat correlation, behavioral analytics (UEBA), and AI-powered threat analysis. It maps all detections to the MITRE ATT&CK framework and maintains an immutable, hash-chained audit trail.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Log Ingestion | Multi-format: Syslog, CEF, JSON, LEEF, XML | NIST SI-4 |
| Correlation Engine | 5 built-in rules with MITRE ATT&CK mapping | NIST IR-4 |
| UEBA | Automated behavioral anomaly detection (5-min cycle) | NIST AC-2(12) |
| AI Threat Analysis | TF-IDF RAG + LLM-powered threat narratives | IEEE 2807 |
| Audit Trail | Hash-chained immutable records | NIST AU-10 |

---

## 2. System Requirements

### Minimum Requirements
| Component | Specification |
|-----------|--------------|
| **OS** | Windows 10/11, Ubuntu 22.04+, RHEL 9+, macOS 14+ |
| **CPU** | 4 cores (8 recommended) |
| **RAM** | 4 GB (8 GB recommended) |
| **Disk** | 20 GB (100 GB for production log storage) |
| **Java** | JDK 21 or later |
| **Database** | H2 (development) / PostgreSQL 16 (production) |
| **Network** | Port 9001 (configurable) |

---

## 3. Installation

### 3.1 Standalone Installation (Windows)

```powershell
# Option A: Using deployment script
cd D:\SOC\qs-siem
.\deploy.ps1

# Option B: Manual
cd D:\SOC
mvn install -pl qs-common -DskipTests -q
mvn package -pl qs-siem -DskipTests -q
cd qs-siem
java -jar target/qs-siem-1.0.0.jar
```

### 3.2 Standalone Installation (Linux/Mac)

```bash
cd /opt/qshield/qs-siem
chmod +x deploy.sh
./deploy.sh
```

### 3.3 Docker Deployment

```bash
cd qs-siem
docker compose up -d
```

### 3.4 Verify Installation

```bash
# Health Check
curl http://localhost:9001/api/v1/siem/health

# Expected Response:
# {"product":"QS-SIEM","version":"1.0.0","status":"UP","timestamp":"..."}
```

---

## 4. Configuration

### 4.1 Application Configuration (application.yml)

```yaml
server:
  port: 9001                    # Change as needed

spring:
  datasource:
    url: jdbc:h2:file:./data/qs-siem    # H2 for development
    # url: jdbc:postgresql://localhost:5432/qshield_csoc  # PostgreSQL for production

qshield:
  jwt:
    secret: ${JWT_SECRET}       # Set via environment variable
  ai:
    enabled: false              # Set to true if Ollama LLM is available
```

### 4.2 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | Auto-generated | JWT signing key (min 256-bit) |
| `DB_HOST` | localhost | PostgreSQL host (prod mode) |
| `DB_USERNAME` | qshield | Database username |
| `DB_PASSWORD` | — | Database password |
| `SPRING_PROFILES_ACTIVE` | default | Set to `prod` for production |

---

## 5. Operations Guide

### 5.1 Dashboard Access

Open browser: **http://localhost:9001**

The dashboard displays:
- **Events (24h)**: Total security events in the last 24 hours
- **Critical Alerts**: Number of CRITICAL severity alerts
- **Active Alerts**: Alerts in NEW status
- **Event Timeline**: 24-hour event volume chart
- **Severity Distribution**: Donut chart of event severities

### 5.2 Ingesting Security Events

#### Via API (Structured Event)
```bash
curl -X POST http://localhost:9001/api/v1/siem/events \
  -H "Content-Type: application/json" \
  -d '{
    "sourceIp": "192.168.1.100",
    "destinationIp": "10.0.0.5",
    "severity": "HIGH",
    "category": "AUTH",
    "action": "DENY",
    "normalizedMessage": "Failed SSH login attempt",
    "rawLog": "sshd[1234]: Failed password for admin from 192.168.1.100"
  }'
```

#### Via API (Raw Log)
```bash
curl -X POST "http://localhost:9001/api/v1/siem/events/raw?format=SYSLOG" \
  -H "Content-Type: text/plain" \
  -d "Mar 15 14:30:22 firewall01 kernel: Denied TCP 192.168.1.50:4532 -> 10.0.0.1:443"
```

#### Supported Log Formats
| Format | Description |
|--------|-------------|
| SYSLOG | RFC 5424 Syslog messages |
| CEF | Common Event Format (ArcSight) |
| JSON | Structured JSON events |
| LEEF | Log Event Extended Format (IBM QRadar) |
| XML | Generic XML-formatted logs |

### 5.3 Viewing Events

```bash
# Get recent events (paginated)
curl "http://localhost:9001/api/v1/siem/events?page=0&size=50"

# Filter by severity
curl "http://localhost:9001/api/v1/siem/events?severity=CRITICAL"
```

### 5.4 Managing Alerts

```bash
# List all alerts
curl "http://localhost:9001/api/v1/siem/alerts?page=0&size=20"

# Update alert status
curl -X PUT "http://localhost:9001/api/v1/siem/alerts/1/status?status=INVESTIGATING&assignedTo=analyst01"
```

#### Alert Lifecycle
```
NEW → INVESTIGATING → RESOLVED
                   → ESCALATED → RESOLVED
                   → FALSE_POSITIVE
```

### 5.5 Correlation Rules

QS-SIEM includes 5 built-in correlation rules:

| Rule ID | Name | Trigger | Window | Severity | MITRE |
|---------|------|---------|--------|----------|-------|
| CR-001 | Brute Force Attack | ≥5 AUTH+DENY events | 5 min | CRITICAL | T1110 |
| CR-002 | Port Scan | ≥20 NETWORK events | 2 min | HIGH | T1046 |
| CR-003 | Malware Outbreak | ≥3 MALWARE events | 10 min | CRITICAL | T1204 |
| CR-004 | Data Exfiltration | ≥50 NETWORK events | 15 min | HIGH | T1048 |
| CR-005 | Privilege Escalation | ≥3 AUTH+ALLOW events | 5 min | CRITICAL | T1068 |

### 5.6 UEBA (User Entity Behavior Analytics)

UEBA runs automatically every 5 minutes:
- Builds behavioral baselines per IP address
- Detects anomalies using statistical scoring
- Generates CRITICAL alerts when anomaly score > 0.9
- Generates HIGH alerts when anomaly score > 0.7

---

## 6. API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/siem/health` | Service health check |
| GET | `/api/v1/siem/dashboard` | Dashboard statistics |
| GET | `/api/v1/siem/events` | List events (paginated) |
| POST | `/api/v1/siem/events` | Ingest structured event |
| POST | `/api/v1/siem/events/raw` | Ingest raw log |
| GET | `/api/v1/siem/alerts` | List alerts (paginated) |
| PUT | `/api/v1/siem/alerts/{id}/status` | Update alert status |

---

## 7. Integrations

### 7.1 Syslog Forwarding
Configure your network devices (firewalls, routers, servers) to forward syslog to:
```
UDP/TCP: <siem-host>:514 → POST to /api/v1/siem/events/raw
```

### 7.2 SOAR Integration
QS-SIEM alerts can be forwarded to QS-SOAR for automated response:
```
SIEM Alert → SOAR Incident → Playbook Execution
```

### 7.3 Splunk/QRadar Migration
QS-SIEM accepts CEF and LEEF formats natively, enabling direct migration from existing SIEM platforms.

---

## 8. Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 9001 already in use | Change `server.port` in application.yml |
| H2 database locked | Stop other QS-SIEM instances; delete `data/` directory |
| High memory usage | Increase JVM heap: `-Xmx1g` |
| Correlation rules not firing | Verify events have matching `category` and `action` fields |
| AI analysis shows "Offline" | Enable Ollama LLM or set `qshield.ai.enabled=false` |

---

## 9. Security Hardening

For production deployment:
1. **Enable HTTPS**: Configure TLS certificates in application.yml
2. **Set JWT Secret**: Use a 256-bit random key via `JWT_SECRET` env variable
3. **Use PostgreSQL**: Switch from H2 to PostgreSQL 16
4. **Enable Rate Limiting**: Already enabled (100 req/min API, 10 req/min auth)
5. **Network Segmentation**: Restrict port 9001 to SOC analyst networks only
6. **Audit Review**: Regularly verify audit trail integrity via hash-chain validation

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
