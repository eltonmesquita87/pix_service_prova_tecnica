#!/bin/bash

# ============================================================================
# Script de Orquestração de Testes de Performance - Pix Service
# ============================================================================
# Este script automatiza todo o fluxo de execução dos testes:
# 1. Iniciar Docker Compose (PostgreSQL)
# 2. Aguardar PostgreSQL ficar pronto
# 3. Iniciar aplicação Spring Boot
# 4. Limpar banco de dados
# 5. Executar testes de performance (10+ minutos)
# 6. Analisar resultados finais
# ============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo ""
echo "============================================================================"
echo "  TESTE DE PERFORMANCE - PIX SERVICE"
echo "============================================================================"
echo ""

# ============================================================================
# PASSO 1: Iniciar Docker Compose (PostgreSQL)
# ============================================================================
echo -e "${BLUE}[1/6] Iniciando Docker Compose (PostgreSQL)...${NC}"
echo "----------------------------------------------------------------------------"

cd "$PROJECT_DIR"

docker-compose down > /dev/null 2>&1 || true
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERRO] Falha ao iniciar Docker Compose${NC}"
    exit 1
fi

echo -e "${GREEN}[OK] Docker Compose iniciado com sucesso${NC}"
echo ""

# ============================================================================
# PASSO 2: Aguardar PostgreSQL ficar pronto
# ============================================================================
echo -e "${BLUE}[2/6] Aguardando PostgreSQL ficar pronto...${NC}"
echo "----------------------------------------------------------------------------"

MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    RETRY_COUNT=$((RETRY_COUNT + 1))

    if docker exec elton_pix-postgres-1 pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "${GREEN}[OK] PostgreSQL está pronto [tentativa $RETRY_COUNT/$MAX_RETRIES]${NC}"
        echo ""
        break
    fi

    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo -e "${RED}[ERRO] PostgreSQL não ficou pronto após $MAX_RETRIES tentativas${NC}"
        exit 1
    fi

    echo "Aguardando PostgreSQL... [tentativa $RETRY_COUNT/$MAX_RETRIES]"
    sleep 2
done

# ============================================================================
# PASSO 3: Iniciar aplicação Spring Boot
# ============================================================================
echo -e "${BLUE}[3/6] Iniciando aplicação Spring Boot...${NC}"
echo "----------------------------------------------------------------------------"

# Mata processos anteriores na porta 8080
if lsof -ti:8080 > /dev/null 2>&1; then
    echo "Matando processo na porta 8080..."
    kill -9 $(lsof -ti:8080) > /dev/null 2>&1 || true
fi

# Cria diretório de logs se não existir
mkdir -p "$PROJECT_DIR/logs"

# Inicia aplicação em background
nohup mvn spring-boot:run > "$PROJECT_DIR/logs/app.log" 2>&1 &
APP_PID=$!

echo "Aguardando aplicação iniciar... (PID: $APP_PID)"

APP_MAX_RETRIES=60
APP_RETRY_COUNT=0

while [ $APP_RETRY_COUNT -lt $APP_MAX_RETRIES ]; do
    APP_RETRY_COUNT=$((APP_RETRY_COUNT + 1))

    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}[OK] Aplicação está pronta [tentativa $APP_RETRY_COUNT/$APP_MAX_RETRIES]${NC}"
        echo ""
        break
    fi

    # Verifica se o processo ainda está rodando
    if ! kill -0 $APP_PID > /dev/null 2>&1; then
        echo -e "${RED}[ERRO] Aplicação falhou ao iniciar${NC}"
        echo "Verifique os logs em $PROJECT_DIR/logs/app.log"
        exit 1
    fi

    if [ $APP_RETRY_COUNT -eq $APP_MAX_RETRIES ]; then
        echo -e "${RED}[ERRO] Aplicação não iniciou após $APP_MAX_RETRIES tentativas${NC}"
        echo "Verifique os logs em $PROJECT_DIR/logs/app.log"
        kill -9 $APP_PID > /dev/null 2>&1 || true
        exit 1
    fi

    echo "Aguardando aplicação... [tentativa $APP_RETRY_COUNT/$APP_MAX_RETRIES]"
    sleep 2
done

# ============================================================================
# PASSO 4: Limpar banco de dados
# ============================================================================
echo -e "${BLUE}[4/6] Limpando banco de dados...${NC}"
echo "----------------------------------------------------------------------------"

bash "$SCRIPT_DIR/truncate-database.sh"

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERRO] Falha ao limpar banco de dados${NC}"
    exit 1
fi

echo -e "${GREEN}[OK] Banco de dados limpo com sucesso${NC}"
echo ""

# ============================================================================
# PASSO 5: Executar testes de performance (10+ minutos)
# ============================================================================
echo -e "${BLUE}[5/6] Executando testes de performance (10+ minutos)...${NC}"
echo "----------------------------------------------------------------------------"
echo ""
echo "Início: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

cd "$PROJECT_DIR/performance-tests"

mvn gatling:test

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERRO] Falha ao executar testes de performance${NC}"
    exit 1
fi

echo ""
echo "Fim: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

cd "$PROJECT_DIR"

echo -e "${GREEN}[OK] Testes de performance concluídos${NC}"
echo ""

# ============================================================================
# PASSO 6: Analisar resultados finais
# ============================================================================
echo -e "${BLUE}[6/6] Analisando resultados finais...${NC}"
echo "----------------------------------------------------------------------------"

# Encontra o relatório mais recente
LATEST_REPORT=$(ls -td "$PROJECT_DIR/performance-tests/target/gatling/pixservicesimulation-"* 2>/dev/null | head -1)

if [ -z "$LATEST_REPORT" ]; then
    echo -e "${YELLOW}[AVISO] Nenhum relatório Gatling encontrado${NC}"
else
    REPORT_PATH="$LATEST_REPORT/index.html"

    echo ""
    echo "============================================================================"
    echo "  RESULTADOS DO TESTE DE PERFORMANCE"
    echo "============================================================================"
    echo ""
    echo "Relatório HTML: $REPORT_PATH"
    echo ""

    # Extrai métricas básicas do simulation.log
    SIMULATION_LOG="$LATEST_REPORT/simulation.log"

    if [ -f "$SIMULATION_LOG" ]; then
        echo "Processando métricas..."
        echo ""

        # Conta requests OK e KO
        OK_COUNT=$(grep -c "REQUEST.*OK" "$SIMULATION_LOG" || echo 0)
        KO_COUNT=$(grep -c "REQUEST.*KO" "$SIMULATION_LOG" || echo 0)
        TOTAL_COUNT=$((OK_COUNT + KO_COUNT))

        if [ $TOTAL_COUNT -gt 0 ]; then
            SUCCESS_RATE=$((OK_COUNT * 100 / TOTAL_COUNT))

            echo "Total de Requests: $TOTAL_COUNT"
            echo "Requests OK: $OK_COUNT"
            echo "Requests KO: $KO_COUNT"
            echo "Taxa de Sucesso: ${SUCCESS_RATE}%"
            echo ""
        fi
    fi

    echo "Para visualizar o relatório completo, abra:"
    echo "file://$REPORT_PATH"
    echo ""

    # Tenta abrir o relatório no navegador padrão
    if command -v xdg-open > /dev/null 2>&1; then
        xdg-open "$REPORT_PATH" > /dev/null 2>&1 &
    elif command -v open > /dev/null 2>&1; then
        open "$REPORT_PATH" > /dev/null 2>&1 &
    fi
fi

echo "============================================================================"
echo "  TESTE CONCLUÍDO COM SUCESSO"
echo "============================================================================"
echo ""
echo "Próximos passos:"
echo "  1. Analise o relatório HTML do Gatling"
echo "  2. Verifique as métricas de performance"
echo "  3. Identifique gargalos e oportunidades de otimização"
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo "Desligando aplicação..."
    if [ ! -z "$APP_PID" ]; then
        kill -9 $APP_PID > /dev/null 2>&1 || true
    fi
}

# Register cleanup on exit
trap cleanup EXIT
