FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="NeurQAI Technologies <security@neurqai.com>"
LABEL product="QS-SIEM"
LABEL version="1.0.0"
WORKDIR /app
COPY target/qs-siem-1.0.0.jar app.jar
EXPOSE 9001
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:9001/api/v1/siem/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]