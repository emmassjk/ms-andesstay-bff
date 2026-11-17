# ═══════════════════════════════════════════════════════════════════════════════
# Dockerfile — ms-andesstay-bff
# Multi-stage build: Maven (compilación) → JRE 17 ligero (ejecución)
# ═══════════════════════════════════════════════════════════════════════════════

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copiar pom.xml para aprovechar la cache de Docker en la descarga de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copiar código fuente y compilar (skipTests para agilizar la construcción de la imagen)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Instalar curl para healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Crear usuario y grupo no-root por seguridad
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copiar el fat JAR generado desde la etapa de compilación
COPY --from=build /app/target/ms-andesstay-bff-*.jar app.jar

# Asegurar permisos correctos
RUN chown -R appuser:appuser /app
USER appuser

# Puerto por defecto del microservicio BFF
EXPOSE 8080

# Opciones de memoria JVM por defecto
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Healthcheck integrado contra Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

