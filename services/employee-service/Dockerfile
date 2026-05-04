# Build context must be services/ (parent of employee-service and hrms-common).
# CI:         context: services,  file: services/employee-service/Dockerfile
# Compose:    context: ./services, dockerfile: employee-service/Dockerfile
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
COPY employee-service/pom.xml employee-service/pom.xml
RUN cd employee-service && mvn dependency:go-offline -q

# Build service
COPY employee-service/src employee-service/src
RUN cd employee-service && mvn package -DskipTests -q


FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms

COPY --from=builder /build/employee-service/target/*.jar app.jar

# Document storage mount point (see app.storage.base-path)
RUN mkdir -p /data/hrms/uploads && chown -R hrms:hrms /data/hrms

USER hrms

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8082/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]