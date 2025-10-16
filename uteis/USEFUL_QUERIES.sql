-- ================================================
-- USEFUL SQL QUERIES - Pix Service
-- ================================================
-- Database: pixdb
-- User: pixuser
-- Connect: psql -h localhost -U pixuser -d pixdb

-- ================================================
-- 1. BASIC QUERIES - Consultas Básicas
-- ================================================

-- Ver todas as carteiras com saldo
SELECT
    id,
    user_id,
    balance,
    version,
    created_at,
    updated_at
FROM wallets
ORDER BY id;

-- Ver todas as chaves Pix
SELECT
    pk.id,
    pk.wallet_id,
    w.user_id,
    pk.key_type,
    pk.key_value,
    pk.created_at
FROM pix_keys pk
JOIN wallets w ON pk.wallet_id = w.id
ORDER BY pk.created_at DESC;

-- Ver todas as transferências com status
SELECT
    pt.end_to_end_id,
    w1.user_id as from_user,
    w2.user_id as to_user,
    pt.amount,
    pt.status,
    pt.created_at,
    pt.confirmed_at,
    pt.rejected_at
FROM pix_transfers pt
JOIN wallets w1 ON pt.from_wallet_id = w1.id
JOIN wallets w2 ON pt.to_wallet_id = w2.id
ORDER BY pt.created_at DESC;

-- Ver ledger completo
SELECT
    le.id,
    le.wallet_id,
    w.user_id,
    le.amount,
    le.type,
    le.end_to_end_id,
    le.metadata,
    le.created_at
FROM ledger_entries le
JOIN wallets w ON le.wallet_id = w.id
ORDER BY le.created_at DESC;

-- ================================================
-- 2. BALANCE VALIDATION - Validação de Saldo
-- ================================================

-- Comparar saldo materializado vs ledger
SELECT
    w.id as wallet_id,
    w.user_id,
    w.balance as materialized_balance,
    COALESCE(SUM(
        CASE
            WHEN le.type IN ('DEPOSIT', 'TRANSFER_CREDIT') THEN le.amount
            WHEN le.type IN ('WITHDRAW', 'TRANSFER_DEBIT') THEN -le.amount
            ELSE 0
        END
    ), 0) as ledger_calculated_balance,
    w.balance - COALESCE(SUM(
        CASE
            WHEN le.type IN ('DEPOSIT', 'TRANSFER_CREDIT') THEN le.amount
            WHEN le.type IN ('WITHDRAW', 'TRANSFER_DEBIT') THEN -le.amount
            ELSE 0
        END
    ), 0) as difference
FROM wallets w
LEFT JOIN ledger_entries le ON w.id = le.wallet_id
GROUP BY w.id, w.user_id, w.balance
ORDER BY w.id;

-- Saldo histórico de uma carteira em momento específico
SELECT
    wallet_id,
    SUM(
        CASE
            WHEN type IN ('DEPOSIT', 'TRANSFER_CREDIT') THEN amount
            WHEN type IN ('WITHDRAW', 'TRANSFER_DEBIT') THEN -amount
            ELSE 0
        END
    ) as historical_balance
FROM ledger_entries
WHERE wallet_id = 1
  AND created_at <= '2025-10-13 12:00:00'
GROUP BY wallet_id;

-- ================================================
-- 3. TRANSFER ANALYSIS - Análise de Transferências
-- ================================================

-- Transferências pendentes
SELECT * FROM pix_transfers
WHERE status = 'PENDING'
ORDER BY created_at;

-- Transferências confirmadas nas últimas 24h
SELECT
    pt.*,
    w1.user_id as from_user,
    w2.user_id as to_user
FROM pix_transfers pt
JOIN wallets w1 ON pt.from_wallet_id = w1.id
JOIN wallets w2 ON pt.to_wallet_id = w2.id
WHERE pt.status = 'CONFIRMED'
  AND pt.confirmed_at >= NOW() - INTERVAL '24 hours'
ORDER BY pt.confirmed_at DESC;

-- Transferências rejeitadas
SELECT * FROM pix_transfers
WHERE status = 'REJECTED'
ORDER BY rejected_at DESC;

-- Estatísticas de transferências
SELECT
    status,
    COUNT(*) as count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount,
    MIN(amount) as min_amount,
    MAX(amount) as max_amount
FROM pix_transfers
GROUP BY status;

-- ================================================
-- 4. IDEMPOTENCY ANALYSIS - Análise de Idempotência
-- ================================================

-- Ver todas as chaves de idempotência
SELECT
    id,
    scope,
    key_value,
    LEFT(response, 100) as response_preview,
    created_at
FROM idempotency_keys
ORDER BY created_at DESC;

-- Contar tentativas duplicadas por escopo
SELECT
    scope,
    COUNT(*) as total_keys,
    COUNT(DISTINCT key_value) as unique_keys,
    COUNT(*) - COUNT(DISTINCT key_value) as duplicates
FROM idempotency_keys
GROUP BY scope;

-- Ver webhooks processados
SELECT
    we.event_id,
    we.end_to_end_id,
    we.event_type,
    we.processed_at,
    pt.status as transfer_status
FROM webhook_events we
LEFT JOIN pix_transfers pt ON we.end_to_end_id = pt.end_to_end_id
ORDER BY we.processed_at DESC;

-- Detectar webhooks duplicados (nunca deveria acontecer)
SELECT
    event_id,
    COUNT(*) as count
FROM webhook_events
GROUP BY event_id
HAVING COUNT(*) > 1;

-- ================================================
-- 5. AUDIT QUERIES - Auditoria
-- ================================================

-- Histórico completo de uma carteira
SELECT
    le.created_at,
    le.type,
    le.amount,
    le.end_to_end_id,
    le.metadata,
    SUM(
        CASE
            WHEN le2.type IN ('DEPOSIT', 'TRANSFER_CREDIT') THEN le2.amount
            WHEN le2.type IN ('WITHDRAW', 'TRANSFER_DEBIT') THEN -le2.amount
            ELSE 0
        END
    ) OVER (ORDER BY le.created_at) as running_balance
FROM ledger_entries le
JOIN ledger_entries le2 ON le2.wallet_id = le.wallet_id AND le2.created_at <= le.created_at
WHERE le.wallet_id = 1
ORDER BY le.created_at;

-- Rastrear uma transferência específica
SELECT
    'Transfer' as source,
    pt.end_to_end_id,
    pt.from_wallet_id,
    pt.to_wallet_id,
    pt.amount,
    pt.status,
    pt.created_at as timestamp
FROM pix_transfers pt
WHERE pt.end_to_end_id = 'E12345678901234567890123456789012'

UNION ALL

SELECT
    'Ledger Entry' as source,
    le.end_to_end_id,
    le.wallet_id as wallet,
    NULL as to_wallet,
    le.amount,
    le.type::text as status,
    le.created_at as timestamp
FROM ledger_entries le
WHERE le.end_to_end_id = 'E12345678901234567890123456789012'

UNION ALL

SELECT
    'Webhook Event' as source,
    we.end_to_end_id,
    NULL as from_wallet,
    NULL as to_wallet,
    NULL as amount,
    we.event_type as status,
    we.processed_at as timestamp
FROM webhook_events we
WHERE we.end_to_end_id = 'E12345678901234567890123456789012'

ORDER BY timestamp;

-- ================================================
-- 6. PERFORMANCE QUERIES - Performance
-- ================================================

-- Ver tamanho das tabelas
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Ver índices e seu uso
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Queries lentas (se pg_stat_statements estiver habilitado)
-- SELECT
--     mean_exec_time,
--     calls,
--     query
-- FROM pg_stat_statements
-- ORDER BY mean_exec_time DESC
-- LIMIT 10;

-- ================================================
-- 7. DATA INTEGRITY CHECKS - Verificação de Integridade
-- ================================================

-- Verificar carteiras com saldo negativo (nunca deveria acontecer)
SELECT * FROM wallets
WHERE balance < 0;

-- Verificar transferências órfãs (sem carteira de origem/destino)
SELECT * FROM pix_transfers pt
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE id = pt.from_wallet_id)
   OR NOT EXISTS (SELECT 1 FROM wallets WHERE id = pt.to_wallet_id);

-- Verificar chaves Pix órfãs
SELECT * FROM pix_keys pk
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE id = pk.wallet_id);

-- Verificar ledger entries órfãs
SELECT * FROM ledger_entries le
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE id = le.wallet_id);

-- Verificar transferências PENDING antigas (> 1 hora)
SELECT * FROM pix_transfers
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL '1 hour';

-- ================================================
-- 8. BUSINESS REPORTS - Relatórios de Negócio
-- ================================================

-- Top 10 carteiras por saldo
SELECT
    w.id,
    w.user_id,
    w.balance,
    COUNT(DISTINCT pk.id) as pix_keys_count
FROM wallets w
LEFT JOIN pix_keys pk ON w.id = pk.wallet_id
GROUP BY w.id, w.user_id, w.balance
ORDER BY w.balance DESC
LIMIT 10;

-- Carteiras mais ativas (por número de transações)
SELECT
    w.id,
    w.user_id,
    COUNT(le.id) as transactions_count,
    SUM(CASE WHEN le.type IN ('DEPOSIT', 'TRANSFER_CREDIT') THEN le.amount ELSE 0 END) as total_credits,
    SUM(CASE WHEN le.type IN ('WITHDRAW', 'TRANSFER_DEBIT') THEN le.amount ELSE 0 END) as total_debits
FROM wallets w
LEFT JOIN ledger_entries le ON w.id = le.wallet_id
GROUP BY w.id, w.user_id
ORDER BY transactions_count DESC
LIMIT 10;

-- Volume de transferências por dia
SELECT
    DATE(created_at) as date,
    COUNT(*) as transfers_count,
    SUM(amount) as total_volume,
    AVG(amount) as avg_amount
FROM pix_transfers
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Taxa de sucesso de transferências
SELECT
    status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM pix_transfers
GROUP BY status;

-- ================================================
-- 9. CLEANUP QUERIES - Limpeza (CUIDADO!)
-- ================================================

-- ATENÇÃO: Estas queries deletam dados permanentemente!
-- Use apenas em ambiente de desenvolvimento/teste

-- Limpar todos os dados (preserva schema)
-- TRUNCATE TABLE webhook_events CASCADE;
-- TRUNCATE TABLE idempotency_keys CASCADE;
-- TRUNCATE TABLE pix_transfers CASCADE;
-- TRUNCATE TABLE ledger_entries CASCADE;
-- TRUNCATE TABLE pix_keys CASCADE;
-- TRUNCATE TABLE wallets CASCADE;

-- Resetar sequences
-- ALTER SEQUENCE wallets_id_seq RESTART WITH 1;
-- ALTER SEQUENCE pix_keys_id_seq RESTART WITH 1;
-- ALTER SEQUENCE ledger_entries_id_seq RESTART WITH 1;
-- ALTER SEQUENCE idempotency_keys_id_seq RESTART WITH 1;
-- ALTER SEQUENCE webhook_events_id_seq RESTART WITH 1;

-- Deletar carteiras específicas (cascata)
-- DELETE FROM wallets WHERE id IN (1, 2, 3);

-- ================================================
-- 10. MONITORING QUERIES - Monitoramento
-- ================================================

-- Contadores gerais
SELECT
    (SELECT COUNT(*) FROM wallets) as total_wallets,
    (SELECT COUNT(*) FROM pix_keys) as total_pix_keys,
    (SELECT COUNT(*) FROM pix_transfers) as total_transfers,
    (SELECT COUNT(*) FROM ledger_entries) as total_ledger_entries,
    (SELECT COUNT(*) FROM webhook_events) as total_webhooks,
    (SELECT SUM(balance) FROM wallets) as total_balance_in_system;

-- Últimas operações
(SELECT 'Wallet Created' as operation, user_id as detail, created_at as timestamp
 FROM wallets ORDER BY created_at DESC LIMIT 5)
UNION ALL
(SELECT 'Pix Key Registered' as operation, key_value as detail, created_at as timestamp
 FROM pix_keys ORDER BY created_at DESC LIMIT 5)
UNION ALL
(SELECT 'Transfer Created' as operation, end_to_end_id as detail, created_at as timestamp
 FROM pix_transfers ORDER BY created_at DESC LIMIT 5)
ORDER BY timestamp DESC
LIMIT 15;

-- Health check do banco
SELECT
    NOW() as current_time,
    pg_postmaster_start_time() as server_start_time,
    NOW() - pg_postmaster_start_time() as uptime,
    (SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active') as active_connections,
    current_database() as database_name,
    version() as postgres_version;

-- ================================================
-- EXAMPLES - Exemplos de Uso
-- ================================================

-- Exemplo 1: Ver histórico completo de uma carteira específica
/*
SELECT
    le.created_at,
    le.type,
    CASE
        WHEN le.type IN ('WITHDRAW', 'TRANSFER_DEBIT') THEN -le.amount
        ELSE le.amount
    END as signed_amount,
    le.metadata
FROM ledger_entries le
WHERE le.wallet_id = 1
ORDER BY le.created_at;
*/

-- Exemplo 2: Rastrear uma transferência do início ao fim
/*
-- Substituir 'E123...' pelo endToEndId real
WITH transfer_data AS (
    SELECT end_to_end_id FROM pix_transfers WHERE end_to_end_id = 'E123...'
)
SELECT * FROM pix_transfers WHERE end_to_end_id IN (SELECT end_to_end_id FROM transfer_data)
UNION ALL
SELECT * FROM ledger_entries WHERE end_to_end_id IN (SELECT end_to_end_id FROM transfer_data)
UNION ALL
SELECT * FROM webhook_events WHERE end_to_end_id IN (SELECT end_to_end_id FROM transfer_data);
*/

-- Exemplo 3: Verificar concorrência - Ver versões de carteiras
/*
SELECT
    id,
    user_id,
    balance,
    version,
    updated_at
FROM wallets
ORDER BY updated_at DESC;
*/
