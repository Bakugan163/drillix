# Multi-stage build: Maven + JDK are used only while compiling the application.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

# Small runtime image with Java 21 only.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/data \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/target/drillix-0.0.1-SNAPSHOT.jar /app/app.jar
# Seed a new named Docker volume with the database shipped in this project.
# H2 lock/trace files are intentionally not copied; H2 creates them at runtime.
COPY --chown=app:app data/mydb.mv.db /app/data/mydb.mv.db

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
