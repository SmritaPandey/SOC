# QS-DPDP Enterprise — User Guide

## Getting Started

### 1. Access the Platform
Open your browser and navigate to `http://localhost:8080`. The compliance dashboard loads immediately.

### 2. Dashboard Overview
The dashboard displays your overall compliance score with key metrics:
- Active consents, breaches, gap status
- Sector compliance coverage
- Training completion rates
- Security alerts summary

## Core Modules

### Consent Management
**Navigate to**: Dashboard → Consent Management

**Key Actions:**
- **Collect Consent** — Create a new consent record specifying data principal, purpose, and data categories
- **Withdraw Consent** — One-click withdrawal with automatic propagation to all processors (DPDP §6)
- **Modify Preferences** — Update granular per-category consent preferences
- **Guardian Consent** — Record verifiable parental consent for children (DPDP §33)
- **Audit Trail** — View the immutable hash-chained consent ledger

### Breach Management
**Navigate to**: Dashboard → Breach Manager

**Key Actions:**
- **Report Incident** — Classify severity and initiate containment
- **DPBI Notification** — Auto-generated 72-hour notification
- **CERT-In Report** — 6-hour cyber incident reporting
- **Track Resolution** — Monitor remediation progress

### Gap Analysis
**Navigate to**: Dashboard → Gap Analysis

**Key Actions:**
- **Run Assessment** — Automated compliance gap scan across all DPDP sections
- **View Results** — Risk-scored findings with remediation recommendations
- **Generate Report** — Export PDF/Markdown gap analysis report

### Training & Awareness
**Navigate to**: Dashboard → Training

**Key Actions:**
- **Browse Programs** — 10 DPDP compliance modules (foundational to advanced)
- **Take Quiz** — MCQ-based assessment with immediate scoring
- **View Certificate** — Downloadable completion certificates
- **Track Progress** — Dashboard with completion rates and scores

### Sector Compliance
**Navigate to**: Dashboard → Sectors

**Supported Sectors (18):**
BFSI, Healthcare, Insurance, Fintech, Telecom, Government, Education,
E-Commerce, Manufacturing, Energy, Transport, Media, Agriculture,
Pharma, Real Estate, Legal, Hospitality, Social Media

Each sector provides:
- Sector-specific regulatory mappings
- Compliance checklists
- Risk assessment tools
- Policy templates

### AI Chatbot
**Navigate to**: Dashboard → Chatbot (or click the AI assistant icon)

**What it can do:**
- Explain DPDP Act provisions with section citations
- Guide you through compliance tasks step-by-step
- Generate policies, SOPs, and notice templates
- Navigate to relevant modules
- Troubleshoot common issues

**Example queries:**
- "What is consent under DPDP?"
- "How do I report a data breach?"
- "Generate a data retention policy"
- "What are my obligations as SDF?"

### Cross-Border Transfers
**Navigate to**: Dashboard → Cross-Border

**Key Actions:**
- **Assess Transfer** — Check if a country is on the permitted list
- **Record Transfer** — Log cross-border data movements with full audit trail
- **View Statistics** — Transfer dashboard with country-wise analytics

### Children's Data Protection
**Navigate to**: Dashboard → Children Data

**Key Actions:**
- **Verify Age** — Gate for determining if parental consent is needed
- **Record Parental Consent** — With verifiable guardian identification
- **Check Processing** — Real-time enforcement of §33 prohibitions

## Data Principal Rights

As required by DPDP Act §11-14, the platform supports:
- **Right to Access** (§11) — Data principals can request their data
- **Right to Correction** (§12) — Submit correction requests
- **Right to Erasure** (§12) — Request data deletion
- **Right to Grievance** (§13) — Raise complaints
- **Right to Nomination** (§14) — Nominate a representative

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| Ctrl+D | Go to Dashboard |
| Ctrl+C | Open Consent Manager |
| Ctrl+B | Open Breach Manager |
| Ctrl+G | Open Gap Analysis |
| Ctrl+/ | Open Chatbot |

## Support

For issues, use the AI chatbot or check the Admin Guide for troubleshooting steps.
