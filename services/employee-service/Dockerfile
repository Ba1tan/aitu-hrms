# Build context must be services/ (parent of employee-service).
# CI:         context: services,  file: services/employee-service/Dockerfile
# Compose:    context: ./services, dockerfile: employee-service/Dockerfile
#
# hrms-common is pulled from GitHub Packages — requires GITHUB_ACTOR and
# GITHUB_TOKEN (with read:packages scope) passed as build args.

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

RUN apk add --no-cache maven

# Maven settings.xml for GitHub Packages auth (builder layer only — never in runtime image)
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">' \
    '  <servers>' \
    '    <server>' \
    '      <id>github</id>' \
    "      <username>${GITHUB_ACTOR}</username>" \
    "      <password>${GITHUB_TOKEN}</password>" \
    '    </server>' \
    '  </servers>' \
    '</settings>' > /root/.m2/settings.xml

# Resolve dependencies (cached layer — only re-runs if pom.xml changes)
COPY employee-service/pom.xml        employee-service/pom.xml
RUN cd employee-service && mvn dependency:go-offline -q

# Build employee-service
COPY employee-service/src            employee-service/src
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