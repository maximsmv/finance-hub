FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV FLYWAY_VERSION=11.8.2

RUN apt-get update && apt-get install -y wget unzip jq && \
    wget -q https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/${FLYWAY_VERSION}/flyway-commandline-${FLYWAY_VERSION}-linux-x64.tar.gz && \
    tar -xzf flyway-commandline-${FLYWAY_VERSION}-linux-x64.tar.gz && \
    mv flyway-${FLYWAY_VERSION} /opt/flyway && \
    ln -s /opt/flyway/flyway /usr/local/bin/flyway && \
    rm flyway-commandline-${FLYWAY_VERSION}-linux-x64.tar.gz

COPY migrate.sh /app/migrate.sh
RUN chmod +x /app/migrate.sh

ENTRYPOINT ["/app/migrate.sh"]
