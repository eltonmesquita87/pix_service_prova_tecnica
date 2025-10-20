-- ============================================================================
-- Script de Manutenção de Partições
-- ============================================================================
-- Objetivo: Criar partições futuras automaticamente (executar mensalmente)
-- Frequência: 1x por mês (primeiro dia do mês às 00:00)
-- Método: Manual via psql ou automatizado via pg_cron
-- ============================================================================

-- ============================================================================
-- FUNÇÃO: Criar Próxima Partição
-- ============================================================================
CREATE OR REPLACE FUNCTION create_next_partition(
    p_table_name TEXT,
    p_months_ahead INTEGER DEFAULT 3
) RETURNS TEXT AS $$
DECLARE
    partition_name TEXT;
    partition_start DATE;
    partition_end DATE;
    sql_command TEXT;
    partitions_created INTEGER := 0;
    result_message TEXT := '';
BEGIN
    -- Loop para criar múltiplas partições futuras
    FOR i IN 1..p_months_ahead LOOP
        -- Calcular início e fim da partição
        partition_start := DATE_TRUNC('month', CURRENT_DATE + (i || ' months')::INTERVAL)::DATE;
        partition_end := DATE_TRUNC('month', CURRENT_DATE + ((i + 1) || ' months')::INTERVAL)::DATE;

        -- Gerar nome da partição (formato: table_YYYY_MM)
        partition_name := p_table_name || '_' || TO_CHAR(partition_start, 'YYYY_MM');

        -- Verificar se partição já existe
        IF NOT EXISTS (
            SELECT 1 FROM pg_tables
            WHERE tablename = partition_name
        ) THEN
            -- Criar partição
            sql_command := FORMAT(
                'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                partition_name,
                p_table_name,
                partition_start,
                partition_end
            );

            EXECUTE sql_command;
            partitions_created := partitions_created + 1;

            result_message := result_message || FORMAT('✓ Criada partição %s [%s - %s]', partition_name, partition_start, partition_end) || E'\n';
        ELSE
            result_message := result_message || FORMAT('• Partição %s já existe', partition_name) || E'\n';
        END IF;
    END LOOP;

    result_message := FORMAT('Partições criadas para %s: %s/%s', p_table_name, partitions_created, p_months_ahead) || E'\n' || result_message;

    RETURN result_message;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNÇÃO: Manutenção Automática de Todas as Tabelas Particionadas
-- ============================================================================
CREATE OR REPLACE FUNCTION maintain_all_partitions() RETURNS TEXT AS $$
DECLARE
    result_message TEXT := '';
BEGIN
    result_message := '========================================' || E'\n';
    result_message := result_message || 'MANUTENÇÃO DE PARTIÇÕES - ' || TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS') || E'\n';
    result_message := result_message || '========================================' || E'\n\n';

    -- Criar partições futuras para ledger_entries
    result_message := result_message || '📊 LEDGER_ENTRIES:' || E'\n';
    result_message := result_message || create_next_partition('ledger_entries', 3) || E'\n';

    -- Criar partições futuras para pix_transfers
    result_message := result_message || '💸 PIX_TRANSFERS:' || E'\n';
    result_message := result_message || create_next_partition('pix_transfers', 3) || E'\n';

    -- Criar partições futuras para webhook_events
    result_message := result_message || '🔔 WEBHOOK_EVENTS:' || E'\n';
    result_message := result_message || create_next_partition('webhook_events', 3) || E'\n';

    result_message := result_message || '========================================' || E'\n';
    result_message := result_message || 'MANUTENÇÃO CONCLUÍDA' || E'\n';
    result_message := result_message || '========================================' || E'\n';

    RETURN result_message;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- EXECUTAR MANUTENÇÃO MANUAL
-- ============================================================================
-- Descomentar para executar imediatamente:
-- SELECT maintain_all_partitions();

-- ============================================================================
-- FUNÇÃO: Arquivar Partições Antigas
-- ============================================================================
CREATE OR REPLACE FUNCTION archive_old_partitions(
    p_table_name TEXT,
    p_retention_months INTEGER
) RETURNS TEXT AS $$
DECLARE
    partition_record RECORD;
    partition_max_date TIMESTAMP;
    cutoff_date DATE;
    sql_command TEXT;
    partitions_archived INTEGER := 0;
    result_message TEXT := '';
BEGIN
    -- Calcular data de corte (retenção)
    cutoff_date := DATE_TRUNC('month', CURRENT_DATE - (p_retention_months || ' months')::INTERVAL)::DATE;

    result_message := FORMAT('Data de corte (retenção %s meses): %s', p_retention_months, cutoff_date) || E'\n';

    -- Listar partições candidatas a arquivamento
    FOR partition_record IN
        SELECT
            inhrelid::regclass::TEXT AS partition_name
        FROM pg_inherits
        WHERE inhparent = p_table_name::regclass
    LOOP
        -- Obter data máxima da partição (simplificado - usar coluna created_at/processed_at)
        EXECUTE FORMAT(
            'SELECT MAX(created_at) FROM %I',
            partition_record.partition_name
        ) INTO partition_max_date;

        -- Se partição está fora do período de retenção
        IF partition_max_date IS NOT NULL AND partition_max_date::DATE < cutoff_date THEN
            result_message := result_message || FORMAT(
                '⚠️  Partição %s candidata a arquivamento (última data: %s)',
                partition_record.partition_name,
                partition_max_date::DATE
            ) || E'\n';

            partitions_archived := partitions_archived + 1;

            -- OPÇÃO 1: Detach (desanexar partição sem dropar)
            -- sql_command := FORMAT('ALTER TABLE %I DETACH PARTITION %I', p_table_name, partition_record.partition_name);
            -- EXECUTE sql_command;
            -- result_message := result_message || FORMAT('  → Partição %s DETACHED', partition_record.partition_name) || E'\n';

            -- OPÇÃO 2: Export para arquivo (implementar ETL externo)
            -- result_message := result_message || FORMAT('  → Execute: pg_dump -t %s > archive/%s.sql', partition_record.partition_name, partition_record.partition_name) || E'\n';

            -- OPÇÃO 3: Drop (CUIDADO - perda de dados!)
            -- sql_command := FORMAT('DROP TABLE %I', partition_record.partition_name);
            -- EXECUTE sql_command;
            -- result_message := result_message || FORMAT('  → Partição %s DROPPED', partition_record.partition_name) || E'\n';
        END IF;
    END LOOP;

    IF partitions_archived = 0 THEN
        result_message := result_message || '✓ Nenhuma partição candidata a arquivamento' || E'\n';
    ELSE
        result_message := result_message || FORMAT('Total de partições candidatas: %s', partitions_archived) || E'\n';
        result_message := result_message || '⚠️  AÇÃO NECESSÁRIA: Revisar e executar arquivamento manual' || E'\n';
    END IF;

    RETURN result_message;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNÇÃO: Verificar Saúde das Partições
-- ============================================================================
CREATE OR REPLACE FUNCTION check_partition_health() RETURNS TABLE(
    table_name TEXT,
    partition_name TEXT,
    partition_size TEXT,
    row_count BIGINT,
    min_date TIMESTAMP,
    max_date TIMESTAMP,
    bloat_pct NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    WITH partition_info AS (
        SELECT
            inhparent::regclass::TEXT AS parent_table,
            inhrelid::regclass::TEXT AS child_partition,
            pg_size_pretty(pg_total_relation_size(inhrelid)) AS size,
            c.reltuples::BIGINT AS estimated_rows
        FROM pg_inherits
        JOIN pg_class c ON c.oid = inhrelid
        WHERE inhparent::regclass::TEXT IN ('ledger_entries', 'pix_transfers', 'webhook_events')
    ),
    partition_stats AS (
        SELECT
            'ledger_entries'::TEXT AS tbl,
            tableoid::regclass::TEXT AS part,
            COUNT(*) AS rows,
            MIN(created_at) AS min_dt,
            MAX(created_at) AS max_dt
        FROM ledger_entries
        GROUP BY tableoid

        UNION ALL

        SELECT
            'pix_transfers'::TEXT,
            tableoid::regclass::TEXT,
            COUNT(*),
            MIN(created_at),
            MAX(created_at)
        FROM pix_transfers
        GROUP BY tableoid

        UNION ALL

        SELECT
            'webhook_events'::TEXT,
            tableoid::regclass::TEXT,
            COUNT(*),
            MIN(processed_at),
            MAX(processed_at)
        FROM webhook_events
        GROUP BY tableoid
    )
    SELECT
        pi.parent_table,
        pi.child_partition,
        pi.size,
        COALESCE(ps.rows, 0),
        ps.min_dt,
        ps.max_dt,
        0::NUMERIC AS bloat_percentage -- Placeholder (cálculo de bloat requer query complexa)
    FROM partition_info pi
    LEFT JOIN partition_stats ps ON ps.part = pi.child_partition
    ORDER BY pi.parent_table, pi.child_partition;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- EXECUTAR VERIFICAÇÕES
-- ============================================================================

-- Verificar saúde das partições
-- SELECT * FROM check_partition_health();

-- Verificar partições candidatas a arquivamento (exemplo: retenção de 12 meses)
-- SELECT archive_old_partitions('ledger_entries', 12);
-- SELECT archive_old_partitions('pix_transfers', 24);
-- SELECT archive_old_partitions('webhook_events', 6);

-- ============================================================================
-- CONFIGURAR AUTOMAÇÃO COM pg_cron (OPCIONAL)
-- ============================================================================
-- Pré-requisito: Instalar extensão pg_cron
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Agendar manutenção mensal (primeiro dia do mês às 00:00)
-- SELECT cron.schedule(
--     'partition_maintenance',
--     '0 0 1 * *',
--     $$SELECT maintain_all_partitions()$$
-- );

-- Verificar jobs agendados
-- SELECT * FROM cron.job;

-- Remover job (se necessário)
-- SELECT cron.unschedule('partition_maintenance');

-- ============================================================================
-- COMENTÁRIOS FINAIS
-- ============================================================================
COMMENT ON FUNCTION create_next_partition IS 'Cria partições futuras para uma tabela específica (padrão: 3 meses à frente)';
COMMENT ON FUNCTION maintain_all_partitions IS 'Executa manutenção em todas as tabelas particionadas (criar partições futuras)';
COMMENT ON FUNCTION archive_old_partitions IS 'Identifica partições candidatas a arquivamento baseado em política de retenção';
COMMENT ON FUNCTION check_partition_health IS 'Verifica saúde das partições (tamanho, contagem, datas, bloat)';
