FROM gradle:8.14-jdk21-jammy AS builder
WORKDIR /app

ARG ARTIFACTORY_URL
ARG ARTIFACTORY_USER
ARG ARTIFACTORY_PASSWORD

ENV ARTIFACTORY_URL=${ARTIFACTORY_URL}
ENV ARTIFACTORY_USER=${ARTIFACTORY_USER}
ENV ARTIFACTORY_PASSWORD=${ARTIFACTORY_PASSWORD}

COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
COPY src ./src
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]