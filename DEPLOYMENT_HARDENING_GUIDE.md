# QShield ONE — Production Deployment Hardening Guide

> Version 1.0.0 | QualityShield Technologies Pvt. Ltd.  
> Standards: NIST SP 800-53, ISO 27001, CIS Benchmarks, OWASP ASVS L2

---

## 1. Pre-Deployment Checklist

### 1.1 Certificate Generation

```bash
# Generate TLS certificate (PKCS12 keystore)
# SOC Platform
keytool -genkeypair -alias qshield-soc \
  -keyalg RSA -keysize 4096 \
  -validity 365 -storetype PKCS12 \
  -keystore qshield-soc.p12 \
  -dname "CN=soc.qsgrc.com, OU=Security Operations, O=QualityShield Technologies, L=New Delhi, ST=Delhi, C=IN"

# IDAM Server
keytool -genkeypair -alias qshield-idam \
  -keyalg RSA -keysize 4096 \
  -validity 365 -storetype PKCS12 \
  -keystore qshield-idam.p12 \
  -dname "CN=idam.qsgrc.com, OU=Identity Management, O=QualityShield Technologies, L=New Delhi, ST=Delhi, C=IN"
```

### 1.2 JWT Secret Generation

```bash
# Generate 256-bit Base64 secret
openssl rand -base64 32
# Example output: dGhpc0lzQVNlY3JldEtleUZvclFTaGllbGQ=

# IMPORTANT: Use DIFFERENT secrets for SOC and IDAM!
```

### 1.3 Database Setup

```sql
-- PostgreSQL (recommended for production)
CREATE DATABASE qshield_soc OWNER qshield_admin;
CREATE DATABASE qshield_idam OWNER qshield_admin;

-- SECURITY: Restrict privileges
REVOKE ALL ON DATABASE qshield_soc FROM PUBLIC;
GRANT CONNECT ON DATABASE qshield_soc TO qshield_soc_app;
GRANT USAGE ON SCHEMA public TO qshield_soc_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO qshield_soc_app;
-- No DROP privileges for app user!
```

---

## 2. Environment Variables (Required)

```bash
# ── TLS ──
export SSL_KEYSTORE_PASSWORD="your-keystore-password"

# ── JWT (different for each service!) ──
export JWT_SECRET="$(openssl rand -base64 32)"

# ── Database ──
export DB_HOST="postgres.internal"
export DB_PORT="5432"
export DB_NAME="qshield_soc"       # or qshield_idam
export DB_USERNAME="qshield_soc_app"
export DB_PASSWORD="strong-db-password"

# ── Redis (for session/cache) ──
export REDIS_HOST="redis.internal"
export REDIS_PORT="6379"
export REDIS_PASSWORD="redis-secret"

# ── AI Engine (optional) ──
export OLLAMA_HOST="http://ollama.internal:11434"
```

---

## 3. Deployment Commands

### 3.1 SOC Platform

```powershell
# Production launch with all security controls active
java -Xms512m -Xmx2048m `
  -Dserver.port=8443 `
  -Dspring.profiles.active=prod `
  -jar QShield-SOC-Platform-2.0.0.jar

# With systemd (Linux)
# [Unit]
# Description=QShield SOC Platform
# After=postgresql.service redis.service
#
# [Service]
# User=qshield
# ExecStart=/usr/bin/java -Xms512m -Xmx2048m -Dspring.profiles.active=prod -jar /opt/qshield/soc/QShield-SOC-Platform-2.0.0.jar
# Restart=always
# RestartSec=10
# SuccessExitStatus=143
# Environment=JWT_SECRET=...
# Environment=SSL_KEYSTORE_PASSWORD=...
```

### 3.2 IDAM Server

```powershell
java -Xms256m -Xmx1024m `
  -Dserver.port=8444 `
  -Dspring.profiles.active=prod `
  -jar QShield-IDAM-Server-1.0.0.jar
```

### 3.3 QShield AV (Native)

```powershell
# Install as Windows Service
sc.exe create QShieldAV binPath= "D:\QSAV\engine\build\qshield_daemon.exe" start= auto
sc.exe description QShieldAV "QShield Antivirus Real-Time Protection"
sc.exe start QShieldAV
```

---

## 4. Network Hardening

### 4.1 Firewall Rules

| Service | Port | Protocol | Access |
|---|---|---|---|
| SOC Platform | 8443 | HTTPS | Internal network + VPN only |
| IDAM Server | 8444 | HTTPS | Internal network + VPN only |
| PostgreSQL | 5432 | TCP | SOC/IDAM servers only |
| Redis | 6379 | TCP | SOC server only |
| Elasticsearch | 9200 | TCP | SOC server only |
| Ollama AI | 11434 | TCP | SOC server only |

### 4.2 Reverse Proxy (nginx)

```nginx
# /etc/nginx/conf.d/qshield-soc.conf
server {
    listen 443 ssl http2;
    server_name soc.qsgrc.com;

    ssl_certificate /etc/letsencrypt/live/soc.qsgrc.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/soc.qsgrc.com/privkey.pem;
    ssl_protocols TLSv1.3;
    ssl_ciphers 'TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256';

    # Security headers (defense-in-depth, supplements Spring Security headers)
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;

    # Rate limiting at nginx level (additional layer)
    limit_req_zone $binary_remote_addr zone=api:10m rate=50r/s;
    limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/m;

    location /api/v1/auth/ {
        limit_req zone=auth burst=3 nodelay;
        proxy_pass https://127.0.0.1:8443;
    }

    location /api/ {
        limit_req zone=api burst=20;
        proxy_pass https://127.0.0.1:8443;
    }

    location / {
        proxy_pass https://127.0.0.1:8443;
    }
}
```

---

## 5. Security Controls Verification

### 5.1 Post-Deployment Checks

```bash
# 1. Verify TLS 1.3
openssl s_client -connect soc.qsgrc.com:8443 -tls1_3

# 2. Verify security headers
curl -I https://soc.qsgrc.com:8443/api/v1/health
# Expected headers:
#   X-Frame-Options: DENY
#   X-Content-Type-Options: nosniff
#   Content-Security-Policy: default-src 'self' ...
#   Referrer-Policy: strict-origin-when-cross-origin
#   Permissions-Policy: camera=(), microphone=(), ...

# 3. Verify API authentication
curl https://soc.qsgrc.com:8443/api/v1/dashboard
# Expected: 401 Unauthorized (not 200!)

# 4. Verify rate limiting
for i in $(seq 1 12); do
  curl -s -o /dev/null -w "%{http_code}\n" https://idam.qsgrc.com:8444/api/v1/auth/login
done
# Expected: 429 Too Many Requests after 10 attempts

# 5. Verify H2 console disabled
curl https://idam.qsgrc.com:8444/h2-console/
# Expected: 403 Forbidden or 404 Not Found

# 6. Verify Swagger disabled
curl https://soc.qsgrc.com:8443/swagger-ui/index.html
# Expected: 404 Not Found
```

### 5.2 Run Compliance Tests

```bash
# SOC Platform — 466 tests (VAPT, NIST, STQC, IEEE, DSCI)
cd D:\SOC\QS-DPDP-Enterprise
mvn test -Dtest="VAPTTest,VAPTPhase2Test,STQCCertificationTest,NISTComplianceTest,IEEEComplianceTest,DSCIComplianceTest"

# IDAM Server — 39 tests (VAPT, NIST, STQC)
cd E:\QS-IDAM
.\mvnw.cmd test -pl qs-idam-server "-Dtest=VAPTSecurityTest,NISTSTQCComplianceTest"

# QShield AV — NIST KAT
cd D:\QSAV\engine\build
.\test_nist_kat.exe
```

---

## 6. Monitoring & Alerting

### 6.1 Log Monitoring

| Log File | Monitor For |
|---|---|
| `~/.qsdpdp-enterprise/logs/qshield-soc.log` | `WARN`, `ERROR`, failed logins |
| `~/.qsidam/logs/qshield-idam.log` | `Rate limit exceeded`, auth failures |
| System Event Log | QShieldAV service status |

### 6.2 Health Endpoints

| Service | Health URL | Expected |
|---|---|---|
| SOC | `GET /actuator/health` | `{"status":"UP"}` |
| IDAM | `GET /actuator/health` | `{"status":"UP"}` |

---

## 7. Backup & Recovery

```bash
# Daily PostgreSQL backup
pg_dump -h $DB_HOST -U qshield_admin qshield_soc | gzip > /backup/soc_$(date +%Y%m%d).sql.gz
pg_dump -h $DB_HOST -U qshield_admin qshield_idam | gzip > /backup/idam_$(date +%Y%m%d).sql.gz

# Audit log backup (immutable — never delete!)
cp -r ~/.qsdpdp-enterprise/data/audit_log* /backup/audit/

# Key material backup (HSM recommended for production)
# Store PKCS12 keystores in a secure vault (not on same server)
```

---

## 8. Compliance Documentation

When preparing for STQC/ISO certification, provide auditors with:

1. **Test Reports**: `mvn surefire-report:report` (HTML test reports)
2. **Crypto Capabilities**: `GET /api/v1/platform/crypto-status` (lists all algorithms)
3. **Key Inventory**: `GET /api/v1/platform/key-inventory` (current key status)
4. **Audit Trail**: `GET /api/v1/audit/integrity-report` (hash chain verification)
5. **This Hardening Guide**: Evidence of deployment security controls
