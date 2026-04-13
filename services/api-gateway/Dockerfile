# Build context must be services/ (parent of api-gateway) so hrms-common
# source is available. Both CI and docker compose use context: ./services.
#
# CI:         context: services,  file: services/api-gateway/Dockerfile
# Compose:    context: ./services, dockerfile: api-gateway/Dockerfile

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

# 1. Install hrms-common into the local Maven repo inside this layer
COPY hrms-common/pom.xml         hrms-common/pom.xml
COPY hrms-common/src             hrms-common/src
RUN cd hrms-common && mvn install -DskipTests -q

# 2. Resolve api-gateway dependencies (cached layer — only re-runs if pom.xml changes)
COPY api-gateway/pom.xml         api-gateway/pom.xml
RUN cd api-gateway && mvn dependency:go-offline -q

# 3. Build api-gateway
COPY api-gateway/src             api-gateway/src
RUN cd api-gateway && mvn package -DskipTests -q


FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms

COPY --from=builder /build/api-gateway/target/*.jar app.jar

USER hrms

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]