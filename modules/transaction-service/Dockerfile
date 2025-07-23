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

RUN gradle build --no-daemon -x test --debug

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ARG VERSION=1.0.0
COPY --from=builder /app/build/libs/transaction-service-${VERSION}.jar ./app.jar

# Установка Flyway CLI
RUN apt-get update && apt-get install -y wget unzip && \
    wget -q https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/9.22.3/flyway-commandline-9.22.3-linux-x64.tar.gz && \
    tar -xzf flyway-commandline-9.22.3-linux-x64.tar.gz && \
    mv flyway-9.22.3 /opt/flyway && \
    ln -s /opt/flyway/flyway /usr/local/bin/flyway && \
    rm flyway-commandline-9.22.3-linux-x64.tar.gz

# Копирование миграций
COPY src/main/resources/db/migration /app/db/migration
COPY migrate-and-run.sh ./migrate-and-run.sh
RUN chmod +x migrate-and-run.sh
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["./migrate-and-run.sh"]