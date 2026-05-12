# ── Etapa 1: Compilación ──────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Descargar dependencias primero (cache de capas)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -B

# ── Etapa 2: Imagen final (solo JRE) ──────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Directorio para archivos subidos (Render Disk o temporal)
RUN mkdir -p uploads

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
