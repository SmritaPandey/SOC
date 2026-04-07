FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="NeurQAI Technologies <security@neurqai.com>"
LABEL product="QS-VAM"
LABEL version="1.0.0"
WORKDIR /app
COPY target/qs-vam-1.0.0.jar app.jar
EXPOSE 9008
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:9008/api/v1/vam/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]