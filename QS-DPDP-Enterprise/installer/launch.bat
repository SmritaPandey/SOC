@echo off
REM QS-DPDP Enterprise Launcher
REM Starts the application with optimal JVM settings

title QS-DPDP Enterprise
echo ══════════════════════════════════════════════
echo    QS-DPDP Enterprise v1.0.0
echo    Enterprise Compliance Operating System
echo    DPDP Act 2023 Compliant
echo ══════════════════════════════════════════════
echo.

SET JAVA_OPTS=-Xms256m -Xmx1024m -Dqsdpdp.features.auto-seed-data=false

REM Check for bundled JRE first, then system Java
IF EXIST "%~dp0runtime\bin\java.exe" (
    SET JAVA_CMD=%~dp0runtime\bin\java.exe
) ELSE (
    SET JAVA_CMD=java
)

echo Starting QS-DPDP Enterprise...
echo Web Dashboard: http://localhost:8080
echo.

"%JAVA_CMD%" %JAVA_OPTS% -jar "%~dp0qs-dpdp-enterprise-1.0.0.jar" --web-only

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to start QS-DPDP Enterprise.
    echo Please ensure Java 17+ is installed.
    pause
)
