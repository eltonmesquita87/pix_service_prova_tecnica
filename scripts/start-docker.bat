@echo off
REM ============================================================================
REM Script de Automacao do Docker Compose - Pix Service
REM ============================================================================
REM Sobe, para e acompanha o ambiente Docker em dois modos:
REM
REM   local        docker-compose.local.yml
REM                PostgreSQL + PgAdmin (a aplicacao roda via mvn spring-boot:run)
REM
REM   distributed  docker-compose.distributed.yml
REM                PostgreSQL + N replicas do Pix Service + NGINX (load balancer)
REM
REM Uso:
REM   start-docker.bat ^<local^|distributed^> [start^|stop^|restart^|status^|logs] [opcoes]
REM
REM Opcoes (modo distributed):
REM   --replicas N    Numero de replicas do pix-service (padrao: 2)
REM   --admin         Inclui o PgAdmin (profile admin)
REM   --monitoring    Inclui Prometheus + Grafana (profile monitoring)
REM   --no-build      Nao reconstroi a imagem da aplicacao
REM
REM Exemplos:
REM   start-docker.bat local
REM   start-docker.bat distributed --replicas 3 --admin
REM   start-docker.bat distributed logs
REM   start-docker.bat distributed stop
REM ============================================================================

setlocal enabledelayedexpansion

REM ----------------------------------------------------------------------------
REM Argumentos
REM ----------------------------------------------------------------------------
set "MODE=%~1"
if "%MODE%"=="" goto usage
if /i "%MODE%"=="-h" goto usage
if /i "%MODE%"=="--help" goto usage
if /i "%MODE%"=="help" goto usage

if /i "%MODE%"=="local" set "COMPOSE_FILE=docker-compose.local.yml" & goto mode_ok
if /i "%MODE%"=="distributed" set "COMPOSE_FILE=docker-compose.distributed.yml" & goto mode_ok
echo [ERRO] Modo invalido: '%MODE%' (use local ou distributed)
exit /b 1
:mode_ok
shift /1

set "ACTION=start"
set "REPLICAS=2"
set "ADMIN="
set "MONITORING="
set "BUILD=--build"

set "FIRST=%~1"
if not defined FIRST goto parse_args
if "%FIRST:~0,1%"=="-" goto parse_args
set "ACTION=%FIRST%"
shift /1

:parse_args
if "%~1"=="" goto args_done
if /i "%~1"=="--replicas" goto opt_replicas
if /i "%~1"=="--admin" goto opt_admin
if /i "%~1"=="--monitoring" goto opt_monitoring
if /i "%~1"=="--no-build" goto opt_no_build
if /i "%~1"=="-h" goto usage
if /i "%~1"=="--help" goto usage
echo [ERRO] Opcao desconhecida: %~1
exit /b 1

:opt_replicas
if "%~2"=="" (
    echo [ERRO] --replicas exige um valor
    exit /b 1
)
set "REPLICAS=%~2"
shift /1
shift /1
goto parse_args

:opt_admin
set "ADMIN=1"
shift /1
goto parse_args

:opt_monitoring
set "MONITORING=1"
shift /1
goto parse_args

:opt_no_build
set "BUILD="
shift /1
goto parse_args

:args_done

if /i "%ACTION%"=="start" goto action_ok
if /i "%ACTION%"=="stop" goto action_ok
if /i "%ACTION%"=="restart" goto action_ok
if /i "%ACTION%"=="status" goto action_ok
if /i "%ACTION%"=="logs" goto action_ok
echo [ERRO] Acao invalida: '%ACTION%' (use start, stop, restart, status ou logs)
exit /b 1
:action_ok

echo %REPLICAS%| findstr /r "^[1-9][0-9]*$" >nul
if errorlevel 1 (
    echo [ERRO] --replicas deve ser um inteiro ^>= 1 ^(recebido: '%REPLICAS%'^)
    exit /b 1
)

REM ----------------------------------------------------------------------------
REM Pre-requisitos
REM ----------------------------------------------------------------------------
where docker >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Docker nao encontrado no PATH
    exit /b 1
)

set "COMPOSE=docker compose"
docker compose version >nul 2>&1
if errorlevel 1 (
    set "COMPOSE=docker-compose"
    docker-compose version >nul 2>&1
    if errorlevel 1 (
        echo [ERRO] Docker Compose nao encontrado ^(nem 'docker compose' nem 'docker-compose'^)
        exit /b 1
    )
)

docker info >nul 2>&1
if errorlevel 1 (
    echo [ERRO] O daemon do Docker nao esta rodando
    exit /b 1
)

cd /d "%~dp0.."
if not exist "%COMPOSE_FILE%" (
    echo [ERRO] Arquivo %COMPOSE_FILE% nao encontrado em %CD%
    exit /b 1
)

REM Profiles: ao iniciar, apenas os solicitados; nas demais acoes, todos
REM (para que stop/status/logs enxerguem os servicos opcionais ja em execucao).
set "ALL_PROFILES=--profile admin --profile monitoring"
set "PROFILES="
if /i "%ACTION%"=="start" goto profiles_start
if /i "%ACTION%"=="restart" goto profiles_start
set "PROFILES=%ALL_PROFILES%"
goto profiles_done
:profiles_start
if /i "%MODE%"=="distributed" (
    if defined ADMIN set "PROFILES=!PROFILES! --profile admin"
    if defined MONITORING set "PROFILES=!PROFILES! --profile monitoring"
)
:profiles_done

if /i "%ACTION%"=="start" goto do_start
if /i "%ACTION%"=="stop" goto do_stop
if /i "%ACTION%"=="restart" goto do_restart
if /i "%ACTION%"=="status" goto do_status
if /i "%ACTION%"=="logs" goto do_logs
exit /b 1

REM ----------------------------------------------------------------------------
REM Acoes
REM ----------------------------------------------------------------------------
:do_status
%COMPOSE% -f %COMPOSE_FILE% %PROFILES% ps
exit /b %ERRORLEVEL%

:do_logs
%COMPOSE% -f %COMPOSE_FILE% %PROFILES% logs -f --tail 100
exit /b %ERRORLEVEL%

:do_restart
call :stop_stack
if errorlevel 1 exit /b 1
goto do_start

:do_stop
call :stop_stack
exit /b %ERRORLEVEL%

:stop_stack
echo Parando containers (%MODE%)...
%COMPOSE% -f %COMPOSE_FILE% %ALL_PROFILES% down --remove-orphans
if errorlevel 1 (
    echo [ERRO] Falha ao parar containers
    exit /b 1
)
echo [OK] Containers parados (volumes preservados)
exit /b 0

:do_start
echo.
echo ============================================================================
echo   PIX SERVICE - DOCKER COMPOSE (%MODE%)
echo ============================================================================
echo.

REM Os dois modos usam o mesmo container_name para o PostgreSQL e as mesmas
REM portas; se o distributed estiver no ar, ele precisa sair antes do local.
if /i "%MODE%"=="local" (
    set "DIST_RUNNING="
    for /f %%i in ('%COMPOSE% -f docker-compose.distributed.yml ps -q nginx pix-service 2^>nul') do set "DIST_RUNNING=1"
    if defined DIST_RUNNING (
        echo [AVISO] Ambiente distributed em execucao - parando antes de subir o local
        %COMPOSE% -f docker-compose.distributed.yml %ALL_PROFILES% down --remove-orphans
    )
)

if /i "%MODE%"=="distributed" if defined MONITORING if not exist "monitoring\prometheus.yml" (
    echo [ERRO] monitoring\prometheus.yml nao existe - o profile monitoring nao pode subir
    exit /b 1
)

if /i "%MODE%"=="local" (
    echo [1/2] Subindo PostgreSQL e PgAdmin...
    %COMPOSE% -f %COMPOSE_FILE% up -d --remove-orphans
) else (
    echo [1/2] Subindo ambiente distribuido com %REPLICAS% replica^(s^)...
    %COMPOSE% -f %COMPOSE_FILE% %PROFILES% up -d %BUILD% --remove-orphans --scale pix-service=%REPLICAS%
)
if errorlevel 1 (
    echo [ERRO] Falha ao subir o Docker Compose
    exit /b 1
)
echo.

echo [2/2] Aguardando servicos ficarem saudaveis...
call :wait_healthy pix-postgres 90
if errorlevel 1 (
    %COMPOSE% -f %COMPOSE_FILE% logs --tail 30 postgres
    echo [ERRO] PostgreSQL nao ficou saudavel
    exit /b 1
)
echo [OK] PostgreSQL pronto

if /i "%MODE%"=="distributed" (
    call :wait_healthy pix-nginx 60
    if errorlevel 1 (
        echo [ERRO] NGINX nao ficou saudavel
        exit /b 1
    )
    echo [OK] NGINX pronto

    call :wait_http http://localhost:8090/actuator/health 240
    if errorlevel 1 (
        %COMPOSE% -f %COMPOSE_FILE% %PROFILES% logs --tail 50 pix-service
        echo [ERRO] Aplicacao nao respondeu em http://localhost:8090/actuator/health
        exit /b 1
    )
    echo [OK] Aplicacao respondendo via load balancer
)

echo.
%COMPOSE% -f %COMPOSE_FILE% %PROFILES% ps
echo.
echo Endpoints:
if /i "%MODE%"=="local" (
    echo   PostgreSQL   localhost:5432  ^(pixdb / pixuser / pixpass^)
    echo   PgAdmin      http://localhost:5050
    echo.
    echo Suba a aplicacao com: mvn spring-boot:run
) else (
    echo   Aplicacao    http://localhost:8090
    echo   Health       http://localhost:8090/actuator/health
    echo   Metricas     http://localhost:8091/actuator/prometheus
    if defined ADMIN echo   PgAdmin      http://localhost:5050
    if defined MONITORING echo   Prometheus   http://localhost:9090
    if defined MONITORING echo   Grafana      http://localhost:3000
)
echo.
echo Logs:  %~nx0 %MODE% logs
echo Parar: %~nx0 %MODE% stop
exit /b 0

REM ----------------------------------------------------------------------------
REM Helpers de espera
REM   :wait_healthy ^<container^> ^<timeout_s^>
REM   :wait_http    ^<url^>       ^<timeout_s^>
REM ----------------------------------------------------------------------------
:wait_healthy
set /a WAIT_ELAPSED=0
:wait_healthy_loop
set "WAIT_STATUS=missing"
for /f "delims=" %%s in ('docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" %~1 2^>nul') do set "WAIT_STATUS=%%s"
if "!WAIT_STATUS!"=="healthy" exit /b 0
if !WAIT_ELAPSED! geq %~2 exit /b 1
echo Aguardando %~1... [!WAIT_STATUS!] ^(!WAIT_ELAPSED!s/%~2s^)
ping -n 3 127.0.0.1 >nul
set /a WAIT_ELAPSED+=2
goto wait_healthy_loop

:wait_http
set /a WAIT_ELAPSED=0
:wait_http_loop
curl -fs -o nul "%~1" >nul 2>&1
if not errorlevel 1 exit /b 0
if !WAIT_ELAPSED! geq %~2 exit /b 1
echo Aguardando %~1... ^(!WAIT_ELAPSED!s/%~2s^)
ping -n 4 127.0.0.1 >nul
set /a WAIT_ELAPSED+=3
goto wait_http_loop

:usage
echo Uso: %~nx0 ^<local^|distributed^> [start^|stop^|restart^|status^|logs] [opcoes]
echo.
echo Modos:
echo   local        PostgreSQL + PgAdmin (docker-compose.local.yml)
echo   distributed  PostgreSQL + N replicas + NGINX (docker-compose.distributed.yml)
echo.
echo Opcoes (modo distributed):
echo   --replicas N    Numero de replicas do pix-service (padrao: 2)
echo   --admin         Inclui o PgAdmin (profile admin)
echo   --monitoring    Inclui Prometheus + Grafana (profile monitoring)
echo   --no-build      Nao reconstroi a imagem da aplicacao
echo.
echo Exemplos:
echo   %~nx0 local
echo   %~nx0 distributed --replicas 3 --admin
echo   %~nx0 distributed logs
echo   %~nx0 distributed stop
exit /b 0
