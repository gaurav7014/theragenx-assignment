# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17.0.11_9-jdk-jammy AS builder

WORKDIR /app


COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle

# Local dev wrapper points to file:///tmp/gradle-9.6.0-bin.zip (Zscaler workaround).
# Rewrite to the canonical URL so the Docker build can download it directly.
RUN sed -i \
    's|distributionUrl=.*|distributionUrl=https\\://services.gradle.org/distributions/gradle-9.6.0-bin.zip|' \
    gradle/wrapper/gradle-wrapper.properties \
  && sed -i '/org.gradle.java.home/d' gradle.properties \
  && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17.0.11_9-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/build/libs/pv-service-*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
