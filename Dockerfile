# syntax=docker/dockerfile:1
# Imagem da Matriz (DA38): o frontend compilado vai dentro do .jar do Spring Boot, que serve
# o frontend e a API na mesma origem (DA02, DA28). Um único container da aplicação.
# Construir a partir da raiz do repositório: docker build -t matriz:local .
# A configuração vem de variáveis de ambiente (DA30, .env.example): a imagem não tem credenciais.

# 1. Frontend (Vue 3, TypeScript e Vite). O build vai para backend/src/main/resources/static.
FROM node:24-alpine AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2. Backend (Java 21 e Maven Wrapper), com o frontend nos recursos estáticos.
# Os testes não correm aqui: correm na pipeline, antes de construir a imagem (DA39).
FROM eclipse-temurin:21-jdk-noble AS backend
WORKDIR /src/backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -q dependency:go-offline
COPY backend/src/main/ src/main/
COPY --from=frontend /src/backend/src/main/resources/static/ src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 sh ./mvnw -B -Dmaven.test.skip=true package \
    && cp target/matriz-*.jar /tmp/matriz.jar

# 3. Execução: só o JRE, com um utilizador sem privilégios.
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.title="Matriz" \
      org.opencontainers.image.description="Gestão de projetos e tarefas para equipas" \
      org.opencontainers.image.source="https://github.com/vapreace/ai-power-software-engineering-matrix"
RUN addgroup -S -g 10001 matriz && adduser -S -D -H -u 10001 -G matriz matriz
WORKDIR /app
COPY --from=backend /tmp/matriz.jar /app/matriz.jar
USER 10001:10001
# Memória da JVM proporcional à do container; termina em vez de continuar sem memória.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Logs em JSON, no formato ECS do Spring Boot (DA38).
ENV LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs
EXPOSE 8080
# Pronta quando aceita pedidos e chega à base de dados (readiness, DA38).
HEALTHCHECK --interval=10s --timeout=3s --start-period=120s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/matriz.jar"]
