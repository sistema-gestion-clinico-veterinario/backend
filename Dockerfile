# ── Etapa 1: Compilación ──────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Descargar dependencias primero (cache de capas)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar fuente, ejecutar pruebas y compilar
COPY src ./src
RUN mvn verify -B --no-transfer-progress

# ── Etapa 2: Imagen final (solo JRE) ──────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ejecutar con una cuenta sin privilegios y dejar escritura solo donde corresponde.
RUN addgroup -S systemvet && adduser -S systemvet -G systemvet \
    && mkdir -p /app/uploads \
    && chown -R systemvet:systemvet /app

COPY --from=build --chown=systemvet:systemvet /app/target/*.jar app.jar

USER systemvet

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
