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
COPY transaction-api-contract/build.gradle transaction-api-contract/build.gradle
COPY transaction-api-contract/gradle.properties transaction-api-contract/gradle.properties
COPY transaction-api-contract/settings.gradle transaction-api-contract/settings.gradle
COPY src src
COPY transaction-api-contract/src transaction-api-contract/src

RUN gradle build --no-daemon -x test --debug \
    && gradle publish

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ARG VERSION=1.0.0

COPY --from=builder /app/build/libs/transaction-service-${VERSION}.jar ./app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]