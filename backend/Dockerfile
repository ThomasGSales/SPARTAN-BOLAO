# =====================================================================
#  Backend Spring Boot (Kotlin) — build multi-stage
#  PLACEHOLDER: funcional assim que o projeto Gradle existir (Fase 2).
# =====================================================================

# ---- Stage 1: build ----
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
