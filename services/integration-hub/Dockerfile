# Build context must be services/ (parent of integration-hub and hrms-common).
# CI:         context: services,  file: services/integration-hub/Dockerfile
# Compose:    context: ./services, dockerfile: integration-hub/Dockerfile

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

COPY hrms-common/pom.xml hrms-common/pom.xml
COPY hrms-common/src     hrms-common/src
RUN cd hrms-common && mvn install -DskipTests -q

COPY integration-hub/pom.xml integration-hub/pom.xml
RUN cd integration-hub && mvn dependency:go-offline -q

COPY integration-hub/src integration-hub/src
RUN cd integration-hub && mvn package -DskipTests -q


FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms

COPY --from=builder /build/integration-hub/target/*.jar app.jar

USER hrms

EXPOSE 8089

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8089/api/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]