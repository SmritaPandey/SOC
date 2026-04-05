# QShield Antivirus v1.0.0 — Quick Start Guide

## AI-Powered Quantum-Safe Endpoint Protection

---

## System Requirements

| Component | Minimum | Recommended |
|---|---|---|
| OS | Windows 10+ (x64) | Windows 11 |
| RAM | 512 MB | 4 GB |
| Disk | 100 MB | 1 GB |
| CPU | x86_64 | Multi-core x86_64 |

## Quick Start

### Command-Line Scanner

#### 1. Download
Download `qshield_scan.exe`

#### 2. Scan a file or directory
```cmd
qshield_scan.exe --scan C:\Users\Downloads
qshield_scan.exe --scan C:\suspected_file.exe
qshield_scan.exe --scan C:\ --recursive --quarantine
```

#### 3. Scan options
```
Usage: qshield_scan.exe [OPTIONS]
  --scan <path>         Path to scan (file or directory)
  --recursive           Scan subdirectories
  --quarantine          Move threats to quarantine vault
  --report <file>       Save scan report to file
  --json                Output results as JSON
  --verbose             Show detailed scan progress
  --signature-db <path> Custom signature database path
  --help                Show help
```

### Real-Time Protection Daemon

#### 1. Download
Download `qshield_daemon.exe`

#### 2. Run as service
```cmd
qshield_daemon.exe --install   # Install as Windows service
qshield_daemon.exe --start     # Start the service
qshield_daemon.exe --status    # Check status
qshield_daemon.exe --stop      # Stop the service
```

#### 3. Features
- File system monitoring (real-time on-access scanning)
- Process behavior analysis
- Network connection monitoring
- Automatic quarantine of detected threats
- System tray notification icon

### Java-Based Installer

#### 1. Download
Download `QShieldAV-Setup.jar`

#### 2. Install (GUI)
```cmd
java -jar QShieldAV-Setup.jar
```
Follow the installation wizard to:
1. Accept license agreement
2. Choose installation directory
3. Select components (Scanner, Daemon, AI Engine)
4. Configure auto-start options
5. Complete installation

## AI Engine Capabilities

| Engine | Technology | Function |
|---|---|---|
| **Signature Scanner** | SHA-256 + Fuzzy Hash + YARA | Known threat detection |
| **Neural Network** | Custom 20→64→32→14 NN | Malware family classification (14 classes) |
| **Random Forest** | 50 trees, depth 10 | Secondary behavioral classification |
| **Isolation Forest** | 100 trees | Anomaly/zero-day detection |
| **RAG Engine** | TF-IDF + Cosine Similarity | Threat intelligence retrieval |
| **Heuristic Engine** | Pattern + Behavioral rules | Unknown variant detection |

## Malware Classifications

The neural network classifies files into 14 categories:
1. Clean/Benign
2. Trojan
3. Ransomware
4. Spyware
5. Adware
6. Worm
7. Rootkit
8. Keylogger
9. Backdoor
10. Cryptominer
11. Botnet
12. Fileless
13. PUP (Potentially Unwanted Program)
14. Unknown/Suspicious

## Quantum-Safe Features

| Algorithm | Standard | Use Case |
|---|---|---|
| CRYSTALS-Kyber | FIPS 203 | Secure update channels |
| CRYSTALS-Dilithium | FIPS 204 | Signature DB integrity |
| SPHINCS+ | FIPS 205 | Firmware/code signing |

## Support
- **Documentation**: https://docs.qshield.io/av
- **Email**: support@qsgrc.com

© 2026 QualityShield Technologies Pvt. Ltd.
