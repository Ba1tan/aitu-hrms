# Build context must be services/ (parent of notification-service and hrms-common).
# CI:         context: services,  file: services/notification-service/Dockerfile
# Compose:    context: ./services, dockerfile: notification-service/Dockerfile

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

COPY hrms-common/pom.xml hrms-common/pom.xml
COPY hrms-common/src     hrms-common/src
RUN cd hrms-common && mvn install -DskipTests -q

COPY notification-service/pom.xml notification-service/pom.xml
RUN cd notification-service && mvn dependency:go-offline -q

COPY notification-service/src notification-service/src
RUN cd notification-service && mvn package -DskipTests -q


FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S hrms && adduser -S hrms -G hrms

COPY --from=builder /build/notification-service/target/*.jar app.jar

USER hrms

EXPOSE 8088

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8088/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]