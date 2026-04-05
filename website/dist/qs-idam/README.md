# QShield IDAM v1.0.0 — Quick Start Guide

## Quantum-Safe Identity & Access Management

---

## System Requirements

| Component | Minimum | Recommended |
|---|---|---|
| OS | Windows 10+ / Linux / macOS | Windows Server 2019+ |
| Java | OpenJDK 17+ | OpenJDK 21 |
| RAM | 2 GB | 8 GB |
| Disk | 500 MB | 10 GB |
| Database | SQLite (embedded) | PostgreSQL 14+ |

## Quick Start (JAR)

### 1. Download
Download `QShield-IDAM-Server-1.0.0.jar`

### 2. Run
```bash
java -jar QShield-IDAM-Server-1.0.0.jar
```

### 3. Access
- **Admin Console**: http://localhost:8090
- **API Docs**: http://localhost:8090/swagger-ui.html
- **Health Check**: http://localhost:8090/actuator/health

### Default Admin
- **Email**: admin@qshield.local
- **Password**: Admin@123

## Windows Installer (EXE)

### 1. Download
Download `QShield-IDAM-Setup-1.0.0.exe`

### 2. Install
Run the installer wizard:
1. Accept EULA
2. Choose install directory
3. Configure database (SQLite default or PostgreSQL)
4. Set admin credentials
5. Complete installation

### 3. Access
Service starts automatically. Open http://localhost:8090

## Features Included

| Feature | Description |
|---|---|
| **SSO / OIDC** | OAuth2 + PKCE, OpenID Connect, JWKS endpoint |
| **WebAuthn / FIDO2** | Passkey authentication, Touch ID, Windows Hello |
| **PAM Vault** | Privileged credential management with rotation |
| **RBAC + ABAC** | Role-based + Attribute-based access control |
| **W3C VCs** | Verifiable Credential issuance and verification |
| **AI Risk Engine** | 5-factor adaptive authentication scoring |
| **Device Trust** | Device fingerprinting and management |
| **Multi-Tenant** | Complete tenant isolation |
| **PQC Tokens** | Quantum-safe JWT signing (Dilithium) |

## API Quick Reference

```bash
# Health check
curl http://localhost:8090/actuator/health

# Authenticate
curl -X POST http://localhost:8090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@qshield.local","password":"Admin@123"}'

# List users
curl http://localhost:8090/api/v1/users \
  -H "Authorization: Bearer <token>"

# Create user
curl -X POST http://localhost:8090/api/v1/users \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@company.com","name":"John Doe","role":"USER"}'
```

## Support
- **Documentation**: https://docs.qshield.io/idam
- **Email**: support@qsgrc.com

© 2026 QualityShield Technologies Pvt. Ltd.
