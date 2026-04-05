# QS-DPDP Enterprise Architecture

## System Overview

QS-DPDP Enterprise is a comprehensive DPDP Act 2023 compliance platform built on a Java-first, modular architecture. It combines policy management, consent lifecycle, breach response, gap analysis, SIEM/DLP, and sector-specific compliance into a single, integrated suite.

## Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│                    QS-DPDP Enterprise Platform                     │
│                     (Spring Boot 3.2 / Java 17)                    │
├───────────────────────────────────────────────────────────────────┤
│  ┌──────────────────── REST API Layer ────────────────────────┐   │
│  │  ConsentController    BreachController    DashboardController│   │
│  │  SIEMController       DLPController       GovernanceController│  │
│  │  ChatbotController    TrainingController   ChildrenDataCtrl  │   │
│  │  CrossBorderController  SectorComplianceController          │   │
│  │  EDRXDRController     NotificationController  AgentController│   │
│  └─────────────────────────────────────────────────────────────┘   │
│                            │                                       │
│  ┌──────────────────── Service Layer ─────────────────────────┐   │
│  │                                                             │   │
│  │  ┌─ Core Services ──────────────────────────────────────┐  │   │
│  │  │  ComplianceEngine     ConsentService    PolicyService │  │   │
│  │  │  BreachService        AuditService      EventBus     │  │   │
│  │  │  GapAnalysisService   PIIScannerService              │  │   │
│  │  │  ChatbotService       TrainingService   I18nService  │  │   │
│  │  │  ChildrenDataService  CrossBorderTransferService     │  │   │
│  │  │  SIEMService          DLPService        DPIAService  │  │   │
│  │  │  RightsService        NotificationService            │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  │                                                             │   │
│  │  ┌─ Sector Compliance Services (18 sectors) ────────────┐  │   │
│  │  │  BFSIComplianceService        HealthcareCompliance   │  │   │
│  │  │  InsuranceComplianceService   FintechCompliance      │  │   │
│  │  │  TelecomComplianceService     GovernmentCompliance   │  │   │
│  │  │  EducationComplianceService   EcommerceCompliance    │  │   │
│  │  │  ManufacturingCompliance      EnergyUtilityCompliance│  │   │
│  │  │  TransportLogisticsCompliance MediaDigitalCompliance │  │   │
│  │  │  AgriRuralCompliance          PharmaCompliance       │  │   │
│  │  │  RealEstateCompliance         LegalCompliance        │  │   │
│  │  │  HospitalityTravelCompliance  SocialMediaCompliance  │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                            │                                       │
│  ┌──────────────── Data Layer ────────────────────────────────┐   │
│  │  DatabaseManager (SQLite — auto-created)                    │   │
│  │  60+ auto-created tables for compliance data                │   │
│  │  Hash-chained audit ledger                                  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                    │
│  ┌─ Security Layer ──────────────────────────────────────────┐    │
│  │  Quantum-safe cryptography (Kyber/Dilithium)               │    │
│  │  RBAC + ABAC access control                                │    │
│  │  AES-256 encryption at rest, TLS 1.3 in transit            │    │
│  └────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.2 |
| Database | SQLite (embedded, auto-created) |
| Build | Maven |
| Security | Quantum-safe (Kyber/Dilithium), AES-256, TLS 1.3 |
| AI/ML | Ollama LLM (RAG chatbot), keyword-matching fallback |
| i18n | 23 languages (English + 22 Scheduled Indian Languages) |

## DPDP Act Section Mapping

| DPDP Section | Module | Service |
|---|---|---|
| §5 — Notice | Consent | ConsentService (notices) |
| §6 — Consent | Consent | ConsentService (collection, withdrawal, propagation) |
| §7 — Legitimate Uses | Consent | ConsentService (legitimate uses) |
| §8 — Data Fiduciary Obligations | Governance | ComplianceEngine, PolicyService |
| §8(6) — Breach Notification | Breach | BreachService, SIEMService |
| §9 — Cross-Border Transfer | Cross-Border | CrossBorderTransferService |
| §10 — DPIA | DPIA | DPIAService |
| §11-14 — Data Principal Rights | Rights | RightsService, ConsentService |
| §16 — Cross-Border Restrictions | Cross-Border | CrossBorderTransferService |
| §33 — Children's Data | Children | ChildrenDataService |
| SDF Rules | Governance | ComplianceEngine |

## API Endpoints Summary

| Base Path | Controller | Purpose |
|---|---|---|
| /api/v1/consent | ConsentController | Consent lifecycle CRUD |
| /api/v1/chatbot | ChatbotController | AI compliance chatbot |
| /api/v1/training | TrainingController | Training & awareness |
| /api/v1/children | ChildrenDataController | Children's data protection |
| /api/v1/crossborder | CrossBorderController | Cross-border transfers |
| /api/v1/sectors | SectorComplianceController | Unified 18-sector API |
| /api/v1/breach | BreachController | Breach management |
| /api/v1/siem | SIEMController | Security events |
| /api/v1/dlp | DLPController | Data loss prevention |
| /api/v1/dashboard | DashboardController | Compliance dashboard |
| /api/v1/governance | GovernanceController | Governance framework |
| /api/v1/xdr | EDRXDRController | EDR/XDR |
