FROM gradle:8.14-jdk21-jammy AS builder
WORKDIR /app

ARG ARTIFACTORY_URL
ARG ARTIFACTORY_USER
ARG ARTIFACTORY_PASSWORD

ENV ARTIFACTORY_URL=${ARTIFACTORY_URL}
ENV ARTIFACTORY_USER=${ARTIFACTORY_USER}
ENV ARTIFACTORY_PASSWORD=${ARTIFACTORY_PASSWORD}

COPY build.gradle build.gradle
COPY gradle.properties gradle.properties
COPY settings.gradle settings.gradle
COPY api-contract/build.gradle api-contract/build.gradle
COPY api-contract/gradle.properties api-contract/gradle.properties
COPY api-contract/settings.gradle api-contract/settings.gradle
COPY src src
COPY api-contract/src api-contract/src

RUN gradle build --no-daemon -x test --debug \
    && gradle publish

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ARG VERSION=1.0.0
COPY --from=builder /app/build/libs/person-service-${VERSION}.jar ./app.jar
EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
