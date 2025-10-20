-- Script para limpar todas as tabelas do banco de dados
-- Útil para resetar o ambiente após testes de performance
--
-- Uso:
-- docker exec -i pix-postgres psql -U pixuser -d pixdb < scripts/truncate-all-tables.sql
--
-- ou via psql:
-- psql -U pixuser -d pixdb -f scripts/truncate-all-tables.sql

BEGIN;

-- Truncate all tables in correct order (respecting foreign keys)
TRUNCATE TABLE
    webhook_events,
    idempotency_keys,
    pix_transfers,
    ledger_entries,
    pix_keys,
    wallets
RESTART IDENTITY CASCADE;

-- Verify tables are empty
SELECT
    'wallets' as table_name,
    COUNT(*) as count
FROM wallets
UNION ALL
SELECT 'pix_keys', COUNT(*) FROM pix_keys
UNION ALL
SELECT 'ledger_entries', COUNT(*) FROM ledger_entries
UNION ALL
SELECT 'pix_transfers', COUNT(*) FROM pix_transfers
UNION ALL
SELECT 'idempotency_keys', COUNT(*) FROM idempotency_keys
UNION ALL
SELECT 'webhook_events', COUNT(*) FROM webhook_events;

COMMIT;
