# Build context must be services/ (parent of attendance-service and hrms-common).
# CI:         context: services,  file: services/attendance-service/Dockerfile
# Compose:    context: ./services, dockerfile: attendance-service/Dockerfile
#
# hrms-common is built from local source — no GitHub Packages auth needed.
# The Dockerfile installs hrms-common into the local .m2 cache before building
# the service, so any change to hrms-common is picked up immediately without
# republishing a SNAPSHOT.

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

# Install hrms-common into the local repo (cached unless its source changes)
COPY hrms-common/pom.xml hrms-common/pom.xml
COPY hrms-common/src     hrms-common/src
RUN cd hrms-common && mvn install -DskipTests -q

# Resolve service dependencies (cached unless its pom.xml changes)
COPY attendance-service/pom.xml attendance-service/pom.xml
RUN cd attendance-service && mvn dependency:go-offline -q

# Build service
COPY attendance-service/src attendance-service/src
RUN cd attendance-service && mvn package -DskipTests -q


FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms

COPY --from=builder /build/attendance-service/target/*.jar app.jar

USER hrms

EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8083/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]