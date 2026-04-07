# QS-IDAM — Operational Manual
## Identity & Access Management v1.0.0

---

## 1. Product Overview

**QS-IDAM** provides centralized identity governance, authentication, and access control for all QShield CSOC modules. It includes JWT-based stateless authentication, MFA support, account lockout protection, role-based access control (RBAC), and user risk scoring.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Authentication | JWT access/refresh tokens, SHA-256 password hashing | NIST IA-2 |
| MFA | TOTP-based multi-factor authentication | NIST IA-2(1) |
| Account Lockout | Auto-lock after 5 failed attempts (30-min cooldown) | NIST AC-7 |
| RBAC | ADMIN, ANALYST, OPERATOR, VIEWER roles | NIST AC-3 |
| User Risk Scoring | Behavioral risk assessment per user | NIST AC-2(12) |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **Port** | 9006 |
| **CPU** | 2 cores |
| **RAM** | 2 GB |
| **Java** | JDK 21+ |

## 3. Installation
```powershell
cd D:\SOC\qs-idam && .\deploy.ps1
# Docker: docker compose up -d
# Verify: curl http://localhost:9006/api/v1/idam/health
```

## 4. Default Admin Credentials
```
Username: admin
Password: ChangeMe@FirstLogin!
```
> ⚠️ **CRITICAL**: Change the default password immediately after first login.

---

## 5. Operations Guide

### 5.1 Authentication (Login)
```bash
curl -X POST http://localhost:9006/api/v1/idam/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "ChangeMe@FirstLogin!"}'
```
**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "role": "ADMIN",
  "username": "admin"
}
```

### 5.2 Creating Users
```bash
curl -X POST http://localhost:9006/api/v1/idam/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "analyst01",
    "email": "analyst01@company.com",
    "passwordHash": "SecurePassword@123",
    "role": "ANALYST",
    "fullName": "Priya Sharma",
    "department": "SOC Operations"
  }'
```

### 5.3 Roles & Permissions
| Role | Dashboard | Events | Alerts | Config | Users |
|------|-----------|--------|--------|--------|-------|
| **ADMIN** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **ANALYST** | ✅ | ✅ | ✅ | ❌ | ❌ |
| **OPERATOR** | ✅ | ✅ | Read | ❌ | ❌ |
| **VIEWER** | ✅ | Read | Read | ❌ | ❌ |

### 5.4 Account Security Features
- **Lockout**: Account locked after 5 failed login attempts
- **Cooldown**: 30-minute lockout duration
- **Failed Attempts**: Tracked per user with audit logging
- **Password Hashing**: SHA-256 with per-user salt

### 5.5 Using JWT Tokens
```bash
# Use access token for API requests to any QShield product
curl -H "Authorization: Bearer <accessToken>" \
  http://localhost:9001/api/v1/siem/events
```

---

## 6. API Reference
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/idam/health` | Health check |
| GET | `/api/v1/idam/dashboard` | Dashboard stats |
| POST | `/api/v1/idam/auth/login` | Authenticate user |
| GET | `/api/v1/idam/users` | List all users |
| POST | `/api/v1/idam/users` | Create user |

## 7. Comparable Products
| Feature | QS-IDAM | Okta | CyberArk | ForgeRock |
|---------|---------|------|----------|-----------|
| JWT Auth | ✅ | ✅ | ✅ | ✅ |
| MFA | ✅ | ✅ | ✅ | ✅ |
| RBAC | ✅ | ✅ | ✅ | ✅ |
| Account Lockout | ✅ | ✅ | ✅ | ✅ |
| Risk Scoring | ✅ | ✅ | ✅ | ❌ |
| On-Premises | ✅ | ❌ | ✅ | ✅ |
| Open Source | ✅ | ❌ | ❌ | ❌ |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
