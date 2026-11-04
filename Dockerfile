# syntax=docker/dockerfile:1

# Etapa de build: compila e empacota a aplicação com Maven e Java 21.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Cache de dependências: copia apenas o pom antes do código.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa de runtime: imagem enxuta, sem ferramentas de build, com usuário sem privilégios.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

COPY --from=build /workspace/target/territorio-limpo-api-*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

# O health check usa o endpoint público do Actuator; a porta acompanha a variável PORT.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O - "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
