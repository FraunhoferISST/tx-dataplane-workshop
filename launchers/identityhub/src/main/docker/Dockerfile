# -buster is required to have apt available
FROM eclipse-temurin:24.0.2_12-jre-alpine

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""
ARG JAR
ARG OTEL_AGENT

RUN apk --no-cache add curl

WORKDIR /app

COPY ${OTEL_AGENT} opentelemetry-javaagent.jar
COPY ${JAR} identityhub.jar

HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl --fail http://localhost:8080/api/check/health

ENV ENV_JVM_ARGS=$JVM_ARGS
CMD ["sh", "-c", "exec java -javaagent:opentelemetry-javaagent.jar -Djava.security.egd=file:/dev/urandom -jar identityhub.jar --log-level=debug"]
