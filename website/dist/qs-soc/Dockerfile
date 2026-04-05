# ═══════════════════════════════════════════════════════
# QS-DPDP Enterprise v3.0 — Multi-Stage Docker Build
# Stage 1: Build React frontend
# Stage 2: Build Spring Boot backend
# Stage 3: Runtime (JRE 21 + static frontend)
# ═══════════════════════════════════════════════════════

# ── Stage 1: Frontend Build ──
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --silent 2>/dev/null || npm install
COPY frontend/ .
RUN npm run build

# ── Stage 2: Backend Build ──
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true
COPY src/ src/
COPY --from=frontend-build /app/frontend/dist/ src/main/resources/static/
RUN mvn package -DskipTests -B

# ── Stage 3: Runtime ──
FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL maintainer="NeurQ AI Labs — QS-DPDP Team"
LABEL description="Quantum-Safe DPDP Compliance Platform v3.0 — Enterprise On-Prem"
LABEL vendor="NeurQ AI Labs"
LABEL version="3.0.0"

WORKDIR /app

# Create non-root user and required directories
RUN addgroup -S qsdpdp && adduser -S qsdpdp -G qsdpdp \
    && mkdir -p /app/config /app/data /app/logs \
    && chown -R qsdpdp:qsdpdp /app

# Copy compiled JAR (no source code)
COPY --from=backend-build --chown=qsdpdp:qsdpdp /app/target/qs-dpdp-enterprise-*.jar app.jar

USER qsdpdp

# Volume mount points for on-prem deployment
VOLUME ["/app/config", "/app/data", "/app/logs"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

EXPOSE 8080

ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=production
ENV SPRING_CONFIG_ADDITIONAL-LOCATION=/app/config/

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
