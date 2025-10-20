#!/bin/bash

# ============================================================================
# Script de Manutenção de Partições - Linux/Mac
# ============================================================================
# Objetivo: Criar partições futuras automaticamente
# Frequência: Executar mensalmente (primeiro dia do mês)
# ============================================================================

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo ""
echo "============================================================================"
echo "  MANUTENÇÃO DE PARTIÇÕES - PIX SERVICE"
echo "============================================================================"
echo ""

cd "$PROJECT_DIR"

# Verificar se PostgreSQL está rodando
if ! docker exec pix-postgres pg_isready -U pixuser -d pixdb > /dev/null 2>&1; then
    echo -e "${RED}[ERRO] PostgreSQL não está rodando ou não está acessível${NC}"
    echo "Execute: docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}[OK] PostgreSQL está acessível${NC}"
echo ""

echo "Executando manutenção de partições..."
echo "----------------------------------------------------------------------------"

# Executar função de manutenção
docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT maintain_all_partitions();"

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERRO] Falha ao executar manutenção de partições${NC}"
    exit 1
fi

echo ""
echo "============================================================================"
echo "  VERIFICANDO SAÚDE DAS PARTIÇÕES"
echo "============================================================================"
echo ""

docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT * FROM check_partition_health();"

echo ""
echo "============================================================================"
echo "  MANUTENÇÃO CONCLUÍDA"
echo "============================================================================"
echo ""
echo "Próximos passos:"
echo "  1. Revisar partições criadas acima"
echo "  2. Agendar execução mensal deste script (cron)"
echo "  3. Monitorar crescimento das partições"
echo ""

# Exemplo de agendamento com cron:
echo "Para agendar execução automática (primeiro dia do mês às 00:00):"
echo "  crontab -e"
echo "  0 0 1 * * $SCRIPT_DIR/maintain-partitions.sh >> /var/log/partition-maintenance.log 2>&1"
echo ""
