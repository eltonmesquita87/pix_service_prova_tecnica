#!/bin/bash
# Script para limpar o banco de dados do Pix Service
# Uso: ./truncate-database.sh

echo "========================================"
echo "Limpando banco de dados do Pix Service"
echo "========================================"
echo

echo "Verificando container PostgreSQL..."
if ! docker ps --filter "name=pix-postgres" --format "{{.Names}}" | grep -q "pix-postgres"; then
    echo "ERRO: Container PostgreSQL não está rodando!"
    echo "Execute: docker-compose up -d"
    exit 1
fi

echo "Container encontrado: pix-postgres"
echo

echo "Executando TRUNCATE em todas as tabelas..."
docker exec -i pix-postgres psql -U pixuser -d pixdb -c "TRUNCATE TABLE webhook_events, idempotency_keys, pix_transfers, ledger_entries, pix_keys, wallets RESTART IDENTITY CASCADE;"

if [ $? -eq 0 ]; then
    echo
    echo "========================================"
    echo "SUCESSO! Banco de dados limpo."
    echo "========================================"
    echo
    echo "Verificando contagem de registros:"
    docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT 'wallets' as table_name, COUNT(*) as count FROM wallets UNION ALL SELECT 'pix_keys', COUNT(*) FROM pix_keys UNION ALL SELECT 'ledger_entries', COUNT(*) FROM ledger_entries UNION ALL SELECT 'pix_transfers', COUNT(*) FROM pix_transfers UNION ALL SELECT 'idempotency_keys', COUNT(*) FROM idempotency_keys UNION ALL SELECT 'webhook_events', COUNT(*) FROM webhook_events;"
else
    echo
    echo "========================================"
    echo "ERRO ao limpar banco de dados!"
    echo "========================================"
    exit 1
fi
