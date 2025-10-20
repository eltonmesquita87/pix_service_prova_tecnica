# =======================================================================================
# Dockerfile Multi-Stage para Pix Service (Produção)
# =======================================================================================
# Otimizado para:
# - Tamanho reduzido (< 300 MB)
# - Startup rápido (< 30s)
# - Segurança (usuário não-root, imagem slim)
# - Performance (G1GC, heap otimizado)
# - Cache de dependências Maven
# =======================================================================================

# =======================================================================================
# STAGE 1: BUILD - Compilação da aplicação
# =======================================================================================
FROM eclipse-temurin:11-jdk-jammy AS build

# Metadata do build
LABEL stage=build
LABEL description="Build stage for Pix Service"

# Definir diretório de trabalho
WORKDIR /app

# Instalar Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copiar pom.xml (permite cache de camada)
COPY pom.xml .

# Cache de dependências Maven (camada separada para cache)
# Esta camada só é recriada se pom.xml mudar
RUN mvn dependency:go-offline -B || true

# Copiar código-fonte
COPY src src

# Build da aplicação (skip tests para imagem de produção)
# Tests devem rodar no CI/CD antes do build da imagem
RUN mvn clean package -DskipTests -B

# Extrair JAR layered (Spring Boot 2.3+)
# Permite cache mais granular de camadas da aplicação
RUN mkdir -p target/dependency && \
    (cd target/dependency; jar -xf ../*.jar)

# =======================================================================================
# STAGE 2: RUNTIME - Imagem final otimizada
# =======================================================================================
FROM eclipse-temurin:11-jre-jammy AS runtime

# Metadata da imagem
LABEL maintainer="devops-team@pixservice.com"
LABEL org.opencontainers.image.title="Pix Service"
LABEL org.opencontainers.image.description="Microserviço de Carteira Digital com Pix"
LABEL org.opencontainers.image.source="https://github.com/empresa/pix-service"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.vendor="Pix Service Team"
LABEL org.opencontainers.image.licenses="MIT"

# Instalar apenas dependências essenciais
# curl: para health checks
# ca-certificates: para HTTPS
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Criar usuário não-root para segurança
# UID 1001 é padrão para aplicações Spring
RUN groupadd -r spring -g 1001 && \
    useradd -r -u 1001 -g spring spring

# Criar diretório da aplicação e dar permissões
WORKDIR /app
RUN chown -R spring:spring /app

# Copiar dependências do build stage (camada otimizada)
COPY --from=build --chown=spring:spring /app/target/dependency/BOOT-INF/lib /app/lib
COPY --from=build --chown=spring:spring /app/target/dependency/META-INF /app/META-INF
COPY --from=build --chown=spring:spring /app/target/dependency/BOOT-INF/classes /app

# Alternar para usuário não-root
USER spring:spring

# =======================================================================================
# CONFIGURAÇÕES DE JVM PARA PRODUÇÃO
# =======================================================================================

# Variáveis de ambiente para otimização de performance
ENV JAVA_OPTS="\
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:InitialRAMPercentage=70.0 \
    -XX:MaxRAMPercentage=85.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/dumps/heapdump.hprof \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

# Variáveis de ambiente da aplicação
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
ENV MANAGEMENT_SERVER_PORT=8081

# Expor portas
EXPOSE 8080 8081

# =======================================================================================
# HEALTH CHECK
# =======================================================================================
# Verifica se a aplicação está rodando corretamente
# - interval: frequência de verificação
# - timeout: tempo máximo de espera por resposta
# - start-period: tempo de graça inicial (startup)
# - retries: número de falhas consecutivas antes de marcar como unhealthy

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=40s \
            --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# =======================================================================================
# ENTRYPOINT E CMD
# =======================================================================================

# Usar exec form para permitir graceful shutdown
# Spring Boot usa layered JARs para startup mais rápido
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp /app:/app/lib/* com.elton.pixservice.PixServiceApplication"]

# =======================================================================================
# BUILD INSTRUCTIONS
# =======================================================================================
# Build:
#   docker build -t pix-service:latest .
#   docker build -t pix-service:1.0.0 --build-arg VERSION=1.0.0 .
#
# Run:
#   docker run -d \
#     --name pix-service \
#     -p 8080:8080 \
#     -p 8081:8081 \
#     -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pixdb \
#     -e SPRING_DATASOURCE_USERNAME=pixuser \
#     -e SPRING_DATASOURCE_PASSWORD=pixpass \
#     --memory=512m \
#     --cpus=1.0 \
#     pix-service:latest
#
# Logs:
#   docker logs -f pix-service
#
# Health:
#   curl http://localhost:8081/actuator/health
#
# Metrics:
#   curl http://localhost:8081/actuator/prometheus
# =======================================================================================
