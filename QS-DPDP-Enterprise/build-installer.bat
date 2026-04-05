@echo off
REM ═══════════════════════════════════════════════════════════
REM QS-DPDP Enterprise v3.0 — Windows MSI/EXE Installer Builder
REM Uses JDK 21 jpackage to create native Windows installer
REM Similar to CrowdStrike Falcon Sensor / Palo Alto Cortex agent 
REM ═══════════════════════════════════════════════════════════

echo.
echo ╔═══════════════════════════════════════════════════════╗
echo ║  QS-DPDP Enterprise v3.0 — Installer Builder        ║
echo ║  Building native Windows MSI installer               ║
echo ╚═══════════════════════════════════════════════════════╝
echo.

SET APP_NAME=QS-DPDP-Enterprise
SET APP_VERSION=3.0.0
SET VENDOR=QS-DPDP Security
SET MAIN_JAR=qs-dpdp-enterprise-3.0.0.jar
SET ICON_PATH=src\main\resources\static\favicon.ico

REM Step 1: Build the application
echo [1/4] Building application...
call mvn clean package -DskipTests -B
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven build failed
    exit /b 1
)

REM Step 2: Build Management Console Installer (MSI)
echo [2/4] Building Management Console installer (MSI)...
jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%VENDOR%" ^
  --description "Quantum-Safe DPDP Compliance Platform — Management Console" ^
  --input target ^
  --main-jar %MAIN_JAR% ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --dest dist ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-menu-group "QS-DPDP Enterprise" ^
  --win-per-user-install ^
  --java-options "-Xmx512m" ^
  --java-options "-Xms256m" ^
  --java-options "-XX:+UseG1GC" ^
  --java-options "-Dspring.profiles.active=production" ^
  --win-upgrade-uuid "a1b2c3d4-5e6f-7890-abcd-ef1234567890" ^
  --license-file LICENSE ^
  --resource-dir src/main/resources

if %ERRORLEVEL% neq 0 (
    echo WARNING: MSI build failed, trying EXE...
    
    REM Fallback: Build EXE installer
    jpackage ^
      --type exe ^
      --name "%APP_NAME%" ^
      --app-version "%APP_VERSION%" ^
      --vendor "%VENDOR%" ^
      --description "Quantum-Safe DPDP Compliance Platform" ^
      --input target ^
      --main-jar %MAIN_JAR% ^
      --main-class org.springframework.boot.loader.launch.JarLauncher ^
      --dest dist ^
      --win-menu ^
      --win-shortcut ^
      --java-options "-Xmx512m -XX:+UseG1GC"
)

REM Step 3: Build Endpoint Agent Installer (lightweight)
echo [3/4] Building Endpoint Sensor Agent installer...
jpackage ^
  --type msi ^
  --name "QS-DPDP-Agent" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%VENDOR%" ^
  --description "QS-DPDP Endpoint Sensor Agent" ^
  --input target ^
  --main-jar %MAIN_JAR% ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --dest dist ^
  --java-options "-Xmx128m" ^
  --java-options "-Dspring.profiles.active=agent" ^
  --java-options "-Dqsdpdp.mode=agent" ^
  --win-upgrade-uuid "b2c3d4e5-6f78-9012-bcde-f12345678901"

REM Step 4: Generate checksums
echo [4/4] Generating checksums...
certutil -hashfile dist\%APP_NAME%-%APP_VERSION%.msi SHA256 > dist\checksums.txt 2>nul
certutil -hashfile dist\QS-DPDP-Agent-%APP_VERSION%.msi SHA256 >> dist\checksums.txt 2>nul

echo.
echo ╔═══════════════════════════════════════════════════════╗
echo ║  BUILD COMPLETE                                      ║
echo ║                                                      ║
echo ║  Installers in: dist\                                ║
echo ║  - QS-DPDP-Enterprise-3.0.0.msi (Management Console)║
echo ║  - QS-DPDP-Agent-3.0.0.msi (Endpoint Agent)         ║
echo ╚═══════════════════════════════════════════════════════╝
echo.
