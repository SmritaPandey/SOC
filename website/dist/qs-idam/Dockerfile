FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="QualityShield Technologies <support@qsgrc.com>"
LABEL product="QShield IDAM Server"
LABEL version="1.0.0"
LABEL description="Quantum-Safe Identity & Access Management Server"

# Security: Run as non-root
RUN addgroup -S qshield && adduser -S qshield -G qshield

WORKDIR /app

# Copy application JAR
COPY QShield-IDAM-Server-1.0.0.jar app.jar

# Create directories for data and logs
RUN mkdir -p /home/qshield/.qsidam/logs \
    && mkdir -p /home/qshield/.qsidam/data \
    && mkdir -p /home/qshield/.qsidam/certs \
    && chown -R qshield:qshield /home/qshield /app

USER qshield

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8090/actuator/health || exit 1

# Default port
EXPOSE 8090

# JVM tuning for containers
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
