# QShield SOC Platform v2.0.0 — Quick Start Guide

## Unified SIEM · SOAR · DLP · EDR · XDR Platform

---

## System Requirements

| Component | Minimum | Recommended |
|---|---|---|
| OS | Windows 10+ / Linux (Ubuntu 20.04+) / macOS 12+ | Windows Server 2019+ / RHEL 8+ |
| Java | OpenJDK 17+ | OpenJDK 21 |
| RAM | 4 GB | 16 GB |
| Disk | 2 GB | 50 GB (for log storage) |
| Database | SQLite (embedded) | PostgreSQL 14+ |
| Search | — | Elasticsearch 8+ |
| Cache | — | Redis 7+ |

## Quick Start (Standalone JAR)

### 1. Download
Download `QShield-SOC-Platform-2.0.0.jar` from the downloads page.

### 2. Run
```bash
java -jar QShield-SOC-Platform-2.0.0.jar
```

### 3. Access
Open your browser and navigate to:
- **Dashboard**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

### Default Credentials
- **Email**: admin@qshield.local
- **Password**: Admin@123

## Windows Installer (MSI)

### 1. Download
Download `QShield-SOC-Setup-2.0.0.msi`

### 2. Install
Double-click the MSI installer and follow the wizard:
1. Accept the license agreement
2. Choose installation directory (default: `C:\Program Files\QShield\SOC`)
3. Select components: SIEM, SOAR, DLP, EDR, XDR
4. Configure database connection
5. Set admin credentials
6. Click Install

### 3. Start Service
The QShield SOC service starts automatically. Access the dashboard at http://localhost:8080

## Standalone EXE

### 1. Download
Download `QShield-SOC-Platform-2.0.0.exe`

### 2. Run
Double-click or run from command line:
```cmd
QShield-SOC-Platform-2.0.0.exe
```
No Java installation required — JRE is bundled.

## Docker Deployment

```bash
docker pull qshield/soc-platform:2.0.0
docker run -d -p 8080:8080 --name qshield-soc qshield/soc-platform:2.0.0
```

Or use Docker Compose with full infrastructure:
```yaml
# docker-compose.yml included in installation
docker-compose up -d
```
Services: PostgreSQL, Redis, Elasticsearch, Ollama (AI), QShield SOC

## Included Modules

| Module | Description |
|---|---|
| **QS-SIEM** | Log aggregation, correlation rules, UEBA, real-time dashboards |
| **QS-SOAR** | Automated playbooks, incident workflows, threat intel feeds |
| **QS-DLP** | NLP PII detection, content inspection, policy engine |
| **QS-EDR** | Endpoint telemetry, behavioral analysis, containment |
| **QS-XDR** | Cross-layer correlation, unified investigation |
| **RAG AI** | TF-IDF + vector search for contextual threat analysis |
| **NLP** | Apache OpenNLP for log analysis and entity extraction |
| **PQC Crypto** | CRYSTALS-Kyber, Dilithium, SPHINCS+ (Bouncy Castle) |

## Configuration

Environment variables or `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/qshield_soc
spring.datasource.username=qshield
spring.datasource.password=your_password

# Elasticsearch (SIEM)
spring.elasticsearch.uris=http://localhost:9200

# Redis (Cache)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# AI Engine (Ollama)
ollama.base-url=http://localhost:11434

# Security
app.jwt.secret=your-256-bit-secret
app.encryption.masterkey=your-aes-256-key
```

## Support
- **Documentation**: https://docs.qshield.io/soc
- **Email**: support@qsgrc.com
- **Enterprise**: enterprise@qsgrc.com

© 2026 QualityShield Technologies Pvt. Ltd.
