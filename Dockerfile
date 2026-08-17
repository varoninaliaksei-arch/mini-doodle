# syntax=docker/dockerfile:1

# ---- Build stage --------------------------------------------------------
# Gradle 8.14 itself must run on a JDK it supports launching on (21); the
# Java 25 toolchain declared in the build files is auto-provisioned by the
# foojay resolver for actually compiling/testing the project sources.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew \
    && ./gradlew --no-daemon :infrastructure:bootJar -x test

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S mini-doodle \
    && adduser -S mini-doodle -G mini-doodle

WORKDIR /app
COPY --from=build /workspace/infrastructure/build/libs/*.jar app.jar
RUN chown mini-doodle:mini-doodle app.jar

USER mini-doodle
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
