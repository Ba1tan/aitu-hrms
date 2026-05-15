# Build context must be services/ (parent of reporting-service and hrms-common).
# CI:         context: services,  file: services/reporting-service/Dockerfile
# Compose:    context: ./services, dockerfile: reporting-service/Dockerfile

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

COPY hrms-common/pom.xml hrms-common/pom.xml
COPY hrms-common/src     hrms-common/src
RUN cd hrms-common && mvn install -DskipTests -q

COPY reporting-service/pom.xml reporting-service/pom.xml
RUN cd reporting-service && mvn dependency:go-offline -q

COPY reporting-service/src reporting-service/src
RUN cd reporting-service && mvn package -DskipTests -q


FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms

COPY --from=builder /build/reporting-service/target/*.jar app.jar

USER hrms

EXPOSE 8087

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8087/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]