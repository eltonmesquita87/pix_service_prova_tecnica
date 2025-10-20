-- ============================================================================
-- Migration V2: Otimizações de Banco de Dados (Sem Particionamento)
-- ============================================================================
-- Objetivo: Melhorar performance e escalabilidade sem comprometer constraints
-- Estratégia: Autovacuum agressivo, índices otimizados, fillfactor
-- Data: 2025-10-18
-- ============================================================================
-- Nota: Particionamento foi considerado mas descartado devido a limitação
-- técnica do PostgreSQL que exige que UNIQUE constraints incluam a coluna
-- de particionamento, o que quebraria a idempotência do sistema.
-- Detalhes: Ver PARTITIONING_STATUS.md
-- ============================================================================

-- ============================================================================
-- PARTE 1: REMOVER ÍNDICE DUPLICADO
-- ============================================================================
-- idx_idempotency_keys_scope_key duplica uk_scope_key (UNIQUE já cria índice)

DROP INDEX IF EXISTS idx_idempotency_keys_scope_key;

COMMENT ON CONSTRAINT uk_scope_key ON idempotency_keys IS
'UNIQUE constraint (scope, key_value) - índice criado automaticamente, não duplicar';

-- ============================================================================
-- PARTE 2: CONFIGURAR AUTOVACUUM AGRESSIVO
-- ============================================================================
-- pix_transfers: 9.46% bloat detectado (updates de status PENDING → CONFIRMED/REJECTED)
-- wallets: 5.30% bloat detectado (updates frequentes de saldo)

ALTER TABLE pix_transfers SET (
    autovacuum_vacuum_scale_factor = 0.05,      -- Vacuum quando 5% dead tuples (padrão: 20%)
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_cost_limit = 2000,        -- Mais agressivo (padrão: 200)
    autovacuum_vacuum_cost_delay = 10           -- Menor delay (padrão: 20ms)
);

ALTER TABLE wallets SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.05,
    fillfactor = 80,                            -- Reserva 20% para HOT updates
    autovacuum_vacuum_cost_limit = 2000
);

COMMENT ON TABLE pix_transfers IS
'Transferências Pix - Autovacuum agressivo configurado (bloat detectado: 9.46%)';

COMMENT ON TABLE wallets IS
'Carteiras - Autovacuum agressivo + fillfactor 80 (bloat detectado: 5.30%)';

-- ============================================================================
-- PARTE 3: ÍNDICES BRIN PARA QUERIES TEMPORAIS
-- ============================================================================
-- Índices BRIN (Block Range INdex) são ideais para colunas com correlação
-- temporal/sequencial. Usam muito menos espaço (1% do tamanho de B-tree)
-- e são eficientes para queries de range em tabelas grandes ordenadas por data.

-- ledger_entries: Append-only, ordenado por created_at
CREATE INDEX IF NOT EXISTS idx_ledger_entries_created_at_brin
    ON ledger_entries USING BRIN (created_at)
    WITH (pages_per_range = 128);

-- pix_transfers: Ordenado por created_at
CREATE INDEX IF NOT EXISTS idx_pix_transfers_created_at_brin
    ON pix_transfers USING BRIN (created_at)
    WITH (pages_per_range = 128);

-- webhook_events: Ordenado por processed_at
CREATE INDEX IF NOT EXISTS idx_webhook_events_processed_at_brin
    ON webhook_events USING BRIN (processed_at)
    WITH (pages_per_range = 128);

COMMENT ON INDEX idx_ledger_entries_created_at_brin IS
'BRIN index para queries temporais eficientes (ex: saldo histórico)';

COMMENT ON INDEX idx_pix_transfers_created_at_brin IS
'BRIN index para queries de transferências por período';

COMMENT ON INDEX idx_webhook_events_processed_at_brin IS
'BRIN index para queries de webhooks por período';

-- ============================================================================
-- PARTE 4: OTIMIZAR ESTATÍSTICAS PARA PLANNER
-- ============================================================================
-- Aumentar estatísticas em colunas críticas melhora planos de execução

ALTER TABLE ledger_entries ALTER COLUMN wallet_id SET STATISTICS 1000;
ALTER TABLE ledger_entries ALTER COLUMN created_at SET STATISTICS 1000;

ALTER TABLE pix_transfers ALTER COLUMN from_wallet_id SET STATISTICS 1000;
ALTER TABLE pix_transfers ALTER COLUMN status SET STATISTICS 500;

ALTER TABLE wallets ALTER COLUMN user_id SET STATISTICS 500;

-- ============================================================================
-- PARTE 5: CONSTRAINT PARA PREVENÇÃO DE BLOAT EXTREMO
-- ============================================================================
-- Adicionar checks para evitar atualizações desnecessárias

-- Evitar update de saldo se valor não mudou (reduz bloat)
CREATE OR REPLACE FUNCTION prevent_noop_wallet_update()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.balance = NEW.balance AND OLD.version = NEW.version THEN
        RETURN NULL;  -- Cancela update se nada mudou
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_prevent_noop_wallet_update
    BEFORE UPDATE ON wallets
    FOR EACH ROW
    EXECUTE FUNCTION prevent_noop_wallet_update();

COMMENT ON FUNCTION prevent_noop_wallet_update IS
'Previne updates desnecessários em wallets (reduz bloat)';

-- ============================================================================
-- PARTE 6: CONFIGURAR TOAST COMPRESSION
-- ============================================================================
-- Metadata em ledger_entries pode ser grande (JSON)
-- Usar compressão LZ4 (mais rápida que pglz padrão)

ALTER TABLE ledger_entries ALTER COLUMN metadata SET STORAGE EXTENDED;
ALTER TABLE ledger_entries ALTER COLUMN metadata SET COMPRESSION lz4;

ALTER TABLE idempotency_keys ALTER COLUMN response SET STORAGE EXTENDED;
ALTER TABLE idempotency_keys ALTER COLUMN response SET COMPRESSION lz4;

-- ============================================================================
-- PARTE 7: VIEWS MATERIALIZADAS PARA AGREGAÇÕES
-- ============================================================================
-- View materializada para estatísticas de saldo (atualizada periodicamente)

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_stats AS
SELECT
    w.id AS wallet_id,
    w.user_id,
    w.balance,
    COUNT(le.id) AS transaction_count,
    MIN(le.created_at) AS first_transaction_at,
    MAX(le.created_at) AS last_transaction_at
FROM wallets w
LEFT JOIN ledger_entries le ON le.wallet_id = w.id
GROUP BY w.id, w.user_id, w.balance;

CREATE UNIQUE INDEX idx_mv_wallet_stats_wallet_id
    ON mv_wallet_stats(wallet_id);

COMMENT ON MATERIALIZED VIEW mv_wallet_stats IS
'Estatísticas agregadas de carteiras - Atualizar periodicamente com REFRESH MATERIALIZED VIEW';

-- ============================================================================
-- PARTE 8: FUNÇÃO DE MANUTENÇÃO PERIÓDICA
-- ============================================================================

CREATE OR REPLACE FUNCTION run_maintenance() RETURNS TEXT AS $$
DECLARE
    result_message TEXT := '';
BEGIN
    result_message := '========================================' || E'\n';
    result_message := result_message || 'MANUTENÇÃO DE BANCO - ' || TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS') || E'\n';
    result_message := result_message || '========================================' || E'\n\n';

    -- Refresh materialized view
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_wallet_stats;
    result_message := result_message || '✓ Materialized view mv_wallet_stats atualizada' || E'\n';

    -- REINDEX BRIN (rápido)
    REINDEX INDEX CONCURRENTLY idx_ledger_entries_created_at_brin;
    REINDEX INDEX CONCURRENTLY idx_pix_transfers_created_at_brin;
    REINDEX INDEX CONCURRENTLY idx_webhook_events_processed_at_brin;
    result_message := result_message || '✓ Índices BRIN reindexados' || E'\n';

    -- Analyze
    ANALYZE ledger_entries, pix_transfers, webhook_events, wallets;
    result_message := result_message || '✓ Estatísticas atualizadas (ANALYZE)' || E'\n';

    result_message := result_message || E'\n========================================' || E'\n';
    result_message := result_message || 'MANUTENÇÃO CONCLUÍDA' || E'\n';
    result_message := result_message || '========================================' || E'\n';

    RETURN result_message;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION run_maintenance IS
'Função de manutenção periódica (executar diariamente via cron ou pg_cron)';

-- ============================================================================
-- PARTE 9: VALIDAÇÃO PÓS-MIGRAÇÃO
-- ============================================================================

DO $$
DECLARE
    optimization_count INTEGER;
BEGIN
    -- Verificar índice duplicado foi removido
    SELECT COUNT(*) INTO optimization_count
    FROM pg_indexes
    WHERE indexname = 'idx_idempotency_keys_scope_key';

    IF optimization_count > 0 THEN
        RAISE WARNING 'Índice duplicado ainda existe!';
    ELSE
        RAISE NOTICE '✓ Índice duplicado removido';
    END IF;

    -- Verificar índices BRIN criados
    SELECT COUNT(*) INTO optimization_count
    FROM pg_indexes
    WHERE indexname LIKE '%_brin';

    RAISE NOTICE '✓ % índices BRIN criados', optimization_count;

    -- Verificar configurações de autovacuum
    SELECT COUNT(*) INTO optimization_count
    FROM pg_class
    WHERE relname IN ('pix_transfers', 'wallets')
      AND reloptions IS NOT NULL;

    RAISE NOTICE '✓ % tabelas com autovacuum configurado', optimization_count;
END $$;

-- ============================================================================
-- MIGRATION CONCLUÍDA
-- ============================================================================
-- Próximos passos:
-- 1. Monitorar bloat com query de pg_stat_user_tables
-- 2. Executar run_maintenance() diariamente (agendar com pg_cron)
-- 3. Revisar políticas de arquivamento quando tabelas > 1M registros
-- 4. Reavaliar particionamento quando PostgreSQL 16+ disponível
-- ============================================================================
