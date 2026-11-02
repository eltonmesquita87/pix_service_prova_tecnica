#!/bin/bash

# ============================================================================
# Script de Automação do Docker Compose - Pix Service
# ============================================================================
# Sobe, para e acompanha o ambiente Docker em dois modos:
#
#   local        docker-compose.local.yml
#                PostgreSQL + PgAdmin (a aplicação roda via mvn spring-boot:run)
#
#   distributed  docker-compose.distributed.yml
#                PostgreSQL + N réplicas do Pix Service + NGINX (load balancer)
#
# Uso:
#   ./start-docker.sh <local|distributed> [start|stop|restart|status|logs] [opções]
#
# Opções (modo distributed):
#   --replicas N    Número de réplicas do pix-service (padrão: 2)
#   --admin         Inclui o PgAdmin (profile admin)
#   --monitoring    Inclui Prometheus + Grafana (profile monitoring)
#   --no-build      Não reconstrói a imagem da aplicação
#
# Exemplos:
#   ./start-docker.sh local
#   ./start-docker.sh distributed --replicas 3 --admin
#   ./start-docker.sh distributed logs
#   ./start-docker.sh distributed stop
# ============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

info()  { echo -e "${BLUE}$*${NC}"; }
ok()    { echo -e "${GREEN}[OK] $*${NC}"; }
warn()  { echo -e "${YELLOW}[AVISO] $*${NC}"; }
fail()  { echo -e "${RED}[ERRO] $*${NC}" >&2; exit 1; }

usage() {
    sed -n '4,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

# ----------------------------------------------------------------------------
# Argumentos
# ----------------------------------------------------------------------------
MODE="${1:-}"
case "$MODE" in
    local)       COMPOSE_FILE="docker-compose.local.yml" ;;
    distributed) COMPOSE_FILE="docker-compose.distributed.yml" ;;
    ""|-h|--help|help) usage; exit 0 ;;
    *) usage; echo ""; fail "Modo inválido: '$MODE' (use local ou distributed)" ;;
esac
shift

ACTION="start"
if [ $# -gt 0 ] && [[ "$1" != -* ]]; then
    ACTION="$1"
    shift
fi

REPLICAS=2
ADMIN=false
MONITORING=false
BUILD=true

while [ $# -gt 0 ]; do
    case "$1" in
        --replicas)
            [ $# -ge 2 ] || fail "--replicas exige um valor"
            REPLICAS="$2"; shift 2 ;;
        --admin)      ADMIN=true; shift ;;
        --monitoring) MONITORING=true; shift ;;
        --no-build)   BUILD=false; shift ;;
        -h|--help)    usage; exit 0 ;;
        *) fail "Opção desconhecida: $1" ;;
    esac
done

case "$ACTION" in
    start|stop|restart|status|logs) ;;
    *) fail "Ação inválida: '$ACTION' (use start, stop, restart, status ou logs)" ;;
esac

[[ "$REPLICAS" =~ ^[1-9][0-9]*$ ]] || fail "--replicas deve ser um inteiro >= 1 (recebido: '$REPLICAS')"

# ----------------------------------------------------------------------------
# Pré-requisitos
# ----------------------------------------------------------------------------
command -v docker > /dev/null 2>&1 || fail "Docker não encontrado no PATH"

if docker compose version > /dev/null 2>&1; then
    COMPOSE=(docker compose)
elif command -v docker-compose > /dev/null 2>&1; then
    COMPOSE=(docker-compose)
else
    fail "Docker Compose não encontrado (nem 'docker compose' nem 'docker-compose')"
fi

docker info > /dev/null 2>&1 || fail "O daemon do Docker não está rodando"

cd "$PROJECT_DIR"
[ -f "$COMPOSE_FILE" ] || fail "Arquivo $COMPOSE_FILE não encontrado em $PROJECT_DIR"

# Profiles: ao iniciar, apenas os solicitados; nas demais ações, todos
# (para que stop/status/logs enxerguem os serviços opcionais já em execução).
PROFILE_ARGS=()
if [ "$ACTION" = "start" ] || [ "$ACTION" = "restart" ]; then
    if [ "$MODE" = "distributed" ]; then
        $ADMIN      && PROFILE_ARGS+=(--profile admin)
        $MONITORING && PROFILE_ARGS+=(--profile monitoring)
    fi
else
    PROFILE_ARGS=(--profile admin --profile monitoring)
fi

compose() {
    "${COMPOSE[@]}" -f "$COMPOSE_FILE" ${PROFILE_ARGS[@]+"${PROFILE_ARGS[@]}"} "$@"
}

# ----------------------------------------------------------------------------
# Helpers de espera
# ----------------------------------------------------------------------------
wait_healthy() {
    local container="$1" timeout="$2" elapsed=0 status
    while [ "$elapsed" -lt "$timeout" ]; do
        status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo "missing")
        [ "$status" = "healthy" ] && return 0
        echo "Aguardando $container... [$status] (${elapsed}s/${timeout}s)"
        sleep 2
        elapsed=$((elapsed + 2))
    done
    return 1
}

wait_http() {
    local url="$1" timeout="$2" elapsed=0
    while [ "$elapsed" -lt "$timeout" ]; do
        curl -fs -o /dev/null "$url" 2>/dev/null && return 0
        echo "Aguardando $url... (${elapsed}s/${timeout}s)"
        sleep 3
        elapsed=$((elapsed + 3))
    done
    return 1
}

# ----------------------------------------------------------------------------
# Ações
# ----------------------------------------------------------------------------
stop_stack() {
    info "Parando containers ($MODE)..."
    # Todos os profiles, para derrubar também admin/monitoring se estiverem no ar
    "${COMPOSE[@]}" -f "$COMPOSE_FILE" --profile admin --profile monitoring down --remove-orphans
    ok "Containers parados (volumes preservados)"
}

start_stack() {
    echo ""
    echo "============================================================================"
    echo "  PIX SERVICE - DOCKER COMPOSE ($MODE)"
    echo "============================================================================"
    echo ""

    # Os dois modos usam o mesmo container_name para o PostgreSQL e as mesmas
    # portas; se o distributed estiver no ar, ele precisa sair antes do local.
    if [ "$MODE" = "local" ]; then
        if [ -n "$("${COMPOSE[@]}" -f docker-compose.distributed.yml ps -q nginx pix-service 2>/dev/null)" ]; then
            warn "Ambiente distributed em execução - parando antes de subir o local"
            "${COMPOSE[@]}" -f docker-compose.distributed.yml --profile admin --profile monitoring down --remove-orphans
        fi
    fi

    if [ "$MODE" = "distributed" ] && $MONITORING && [ ! -f monitoring/prometheus.yml ]; then
        fail "monitoring/prometheus.yml não existe - o profile monitoring não pode subir"
    fi

    if [ "$MODE" = "local" ]; then
        info "[1/2] Subindo PostgreSQL e PgAdmin..."
        compose up -d --remove-orphans
    else
        local build_args=()
        $BUILD && build_args+=(--build)
        info "[1/2] Subindo ambiente distribuído com $REPLICAS réplica(s)..."
        compose up -d ${build_args[@]+"${build_args[@]}"} --remove-orphans --scale "pix-service=$REPLICAS"
    fi
    echo ""

    info "[2/2] Aguardando serviços ficarem saudáveis..."
    if ! wait_healthy pix-postgres 90; then
        compose logs --tail 30 postgres || true
        fail "PostgreSQL não ficou saudável"
    fi
    ok "PostgreSQL pronto"

    if [ "$MODE" = "distributed" ]; then
        wait_healthy pix-nginx 60 || fail "NGINX não ficou saudável"
        ok "NGINX pronto"

        if command -v curl > /dev/null 2>&1; then
            if ! wait_http "http://localhost:8090/actuator/health" 240; then
                compose logs --tail 50 pix-service || true
                fail "Aplicação não respondeu em http://localhost:8090/actuator/health"
            fi
            ok "Aplicação respondendo via load balancer"
        else
            warn "curl não encontrado - não foi possível validar a aplicação via HTTP"
        fi
    fi

    echo ""
    compose ps
    echo ""
    print_endpoints
}

print_endpoints() {
    echo "Endpoints:"
    if [ "$MODE" = "local" ]; then
        echo "  PostgreSQL   localhost:5432  (pixdb / pixuser / pixpass)"
        echo "  PgAdmin      http://localhost:5050"
        echo ""
        echo "Suba a aplicação com: mvn spring-boot:run"
    else
        echo "  Aplicação    http://localhost:8090"
        echo "  Health       http://localhost:8090/actuator/health"
        echo "  Métricas     http://localhost:8091/actuator/prometheus"
        $ADMIN      && echo "  PgAdmin      http://localhost:5050"
        $MONITORING && echo "  Prometheus   http://localhost:9090" && echo "  Grafana      http://localhost:3000"
    fi
    echo ""
    echo "Logs:  $0 $MODE logs"
    echo "Parar: $0 $MODE stop"
    return 0
}

case "$ACTION" in
    start)   start_stack ;;
    stop)    stop_stack ;;
    restart) stop_stack; start_stack ;;
    status)  compose ps ;;
    logs)    compose logs -f --tail 100 ;;
esac
