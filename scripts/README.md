# Scripts de Utilidades - Pix Service

Este diretório contém scripts utilitários para gerenciar o banco de dados e facilitar o desenvolvimento e testes.

## Scripts Disponíveis

### 1. Start Docker (Docker Compose local e distribuído)

Automatiza o ciclo de vida do ambiente Docker: sobe, aguarda os serviços ficarem saudáveis, mostra os endpoints, e também para/reinicia/mostra status e logs.

| Modo | Compose file | O que sobe |
|------|--------------|------------|
| `local` | `docker-compose.local.yml` | PostgreSQL + PgAdmin (a aplicação roda com `mvn spring-boot:run`) |
| `distributed` | `docker-compose.distributed.yml` | PostgreSQL + N réplicas do Pix Service + NGINX (load balancer) |

```bash
# Linux/Mac/Git Bash
./scripts/start-docker.sh <local|distributed> [start|stop|restart|status|logs] [opções]
```
```batch
:: Windows
scripts\start-docker.bat <local|distributed> [start|stop|restart|status|logs] [opcoes]
```

A ação padrão é `start`. Opções (modo `distributed`):

| Opção | Descrição |
|-------|-----------|
| `--replicas N` | Número de réplicas do `pix-service` (padrão: 2) |
| `--admin` | Inclui o PgAdmin (profile `admin`) |
| `--monitoring` | Inclui Prometheus + Grafana (profile `monitoring`) — exige `monitoring/prometheus.yml` |
| `--no-build` | Não reconstrói a imagem da aplicação |

Exemplos:
```bash
./scripts/start-docker.sh local
./scripts/start-docker.sh distributed --replicas 3 --admin
./scripts/start-docker.sh distributed logs
./scripts/start-docker.sh distributed stop
```

Observações:
- `stop` remove os containers mas **preserva os volumes** (dados do PostgreSQL).
- Os dois modos usam o container `pix-postgres` e a porta 5050; ao subir o `local` com o `distributed` ativo, o script derruba o `distributed` antes.
- No modo `distributed` o script só conclui quando `http://localhost:8090/actuator/health` responde via NGINX (timeout de 4 min).

### 2. Truncate Database (Limpar Banco de Dados)

Scripts para limpar todas as tabelas do banco de dados, útil após testes de performance ou para resetar o ambiente.

#### Windows (Batch)
```batch
cd scripts
truncate-database.bat
```

#### Linux/Mac (Shell)
```bash
cd scripts
chmod +x truncate-database.sh
./truncate-database.sh
```

#### SQL Direto
```bash
docker exec -i pix-postgres psql -U pixuser -d pixdb < scripts/truncate-all-tables.sql
```

ou

```bash
psql -U pixuser -d pixdb -f scripts/truncate-all-tables.sql
```

## O que os scripts fazem

Os scripts de TRUNCATE:

1. ✅ Verificam se o container PostgreSQL está rodando
2. ✅ Executam TRUNCATE em todas as tabelas na ordem correta (respeitando foreign keys)
3. ✅ Resetam os sequences (AUTO_INCREMENT) com `RESTART IDENTITY`
4. ✅ Usam CASCADE para remover todas as dependências
5. ✅ Verificam se as tabelas foram limpas (contagem = 0)

### Tabelas Limpas

- `webhook_events` - Eventos de webhook processados
- `idempotency_keys` - Chaves de idempotência
- `pix_transfers` - Transferências Pix
- `ledger_entries` - Entradas do ledger (histórico)
- `pix_keys` - Chaves Pix registradas
- `wallets` - Carteiras digitais

### Ordem de TRUNCATE

A ordem é importante devido às foreign keys:

```
webhook_events (sem FK)
    ↓
idempotency_keys (sem FK)
    ↓
pix_transfers (FK: from_wallet_id, to_wallet_id)
    ↓
ledger_entries (FK: wallet_id)
    ↓
pix_keys (FK: wallet_id)
    ↓
wallets (tabela base)
```

## Quando Usar

### ✅ Usar TRUNCATE quando:

- Após executar testes de performance
- Antes de iniciar novos testes
- Para resetar o ambiente de desenvolvimento
- Quando precisar de um banco limpo com sequences resetados

### ⚠️ NÃO usar em:

- Ambiente de produção
- Quando precisar manter dados de auditoria
- Se houver dados importantes que não podem ser perdidos

## Exemplo de Uso

```bash
# 1. Executar testes de performance
cd performance-tests
mvn gatling:test

# 2. Limpar banco de dados
cd ../scripts
./truncate-database.sh

# 3. Verificar que está limpo
docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT COUNT(*) FROM wallets;"
```

## Verificação Manual

Para verificar manualmente se as tabelas estão vazias:

```bash
docker exec -it pix-postgres psql -U pixuser -d pixdb

# No psql:
SELECT
    'wallets' as table_name, COUNT(*) as count FROM wallets
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
```

## Requisitos

- Docker instalado e rodando
- Container `pix-postgres` ativo
- Permissões de escrita no banco de dados

## Troubleshooting

### Erro: "Container PostgreSQL não está rodando"

**Solução**: Inicie o container:
```bash
docker-compose -f docker-compose.local.yml up -d
```

### Erro: "permission denied"

**Solução**: Torne o script executável (Linux/Mac):
```bash
chmod +x truncate-database.sh
```

### Erro: "FATAL: password authentication failed"

**Solução**: Verifique as credenciais no `application.yml`:
- Username: `pixuser`
- Password: `pixpass`
- Database: `pixdb`

## Backup Antes de Limpar (Opcional)

Se quiser fazer backup antes de limpar:

```bash
# Criar backup
docker exec -i pix-postgres pg_dump -U pixuser pixdb > backup_$(date +%Y%m%d_%H%M%S).sql

# Restaurar backup
docker exec -i pix-postgres psql -U pixuser -d pixdb < backup_20251016_203000.sql
```
