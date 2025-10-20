@echo off
REM Script para limpar o banco de dados do Pix Service
REM Uso: truncate-database.bat

echo ========================================
echo Limpando banco de dados do Pix Service
echo ========================================
echo.

echo Verificando container PostgreSQL...
docker ps --filter "name=pix-postgres" --format "{{.Names}}" > nul 2>&1
if %errorlevel% neq 0 (
    echo ERRO: Container PostgreSQL nao esta rodando!
    echo Execute: docker-compose up -d
    exit /b 1
)

echo Container encontrado: pix-postgres
echo.

echo Executando TRUNCATE em todas as tabelas...
docker exec -i pix-postgres psql -U pixuser -d pixdb -c "TRUNCATE TABLE webhook_events, idempotency_keys, pix_transfers, ledger_entries, pix_keys, wallets RESTART IDENTITY CASCADE;"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo SUCESSO! Banco de dados limpo.
    echo ========================================
    echo.
    echo Verificando contagem de registros:
    docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT 'wallets' as table_name, COUNT(*) as count FROM wallets UNION ALL SELECT 'pix_keys', COUNT(*) FROM pix_keys UNION ALL SELECT 'ledger_entries', COUNT(*) FROM ledger_entries UNION ALL SELECT 'pix_transfers', COUNT(*) FROM pix_transfers UNION ALL SELECT 'idempotency_keys', COUNT(*) FROM idempotency_keys UNION ALL SELECT 'webhook_events', COUNT(*) FROM webhook_events;"
) else (
    echo.
    echo ========================================
    echo ERRO ao limpar banco de dados!
    echo ========================================
    exit /b 1
)
