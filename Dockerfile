# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Support Windows checkouts without requiring an executable bit on the host.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/main/ src/main/
# Run the full Testcontainers suite on the host/CI before building the image.
# Image builds do not require access to a Docker daemon or a database.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests package \
    && cp target/spotnight-backend-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
# curl is used only for the container's Actuator healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --no-log-init --uid 10001 --gid app \
        --home-dir /nonexistent --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /workspace/app.jar ./app.jar
ENV SPRING_PROFILES_ACTIVE=prod
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl --fail --silent --show-error --max-time 3 http://localhost:8080/actuator/health > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
