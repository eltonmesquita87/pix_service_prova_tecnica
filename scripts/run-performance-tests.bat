@echo off
REM ============================================================================
REM Script de Orquestracao de Testes de Performance - Pix Service
REM ============================================================================
REM Este script automatiza todo o fluxo de execucao dos testes:
REM 1. Iniciar Docker Compose (PostgreSQL)
REM 2. Aguardar PostgreSQL ficar pronto
REM 3. Iniciar aplicacao Spring Boot
REM 4. Limpar banco de dados
REM 5. Executar testes de performance (10+ minutos)
REM 6. Analisar resultados finais
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ============================================================================
echo   TESTE DE PERFORMANCE - PIX SERVICE
echo ============================================================================
echo.

REM ============================================================================
REM PASSO 1: Iniciar Docker Compose (PostgreSQL)
REM ============================================================================
echo [1/6] Iniciando Docker Compose (PostgreSQL)...
echo ----------------------------------------------------------------------------

cd /d "%~dp0.."

docker-compose down >nul 2>&1
docker-compose up -d

if %ERRORLEVEL% neq 0 (
    echo [ERRO] Falha ao iniciar Docker Compose
    exit /b 1
)

echo [OK] Docker Compose iniciado com sucesso
echo.

REM ============================================================================
REM PASSO 2: Aguardar PostgreSQL ficar pronto
REM ============================================================================
echo [2/6] Aguardando PostgreSQL ficar pronto...
echo ----------------------------------------------------------------------------

set MAX_RETRIES=30
set RETRY_COUNT=0

:wait_postgres
set /a RETRY_COUNT+=1

docker exec elton_pix-postgres-1 pg_isready -U postgres >nul 2>&1

if %ERRORLEVEL% equ 0 (
    echo [OK] PostgreSQL esta pronto [tentativa %RETRY_COUNT%/%MAX_RETRIES%]
    echo.
    goto postgres_ready
)

if %RETRY_COUNT% geq %MAX_RETRIES% (
    echo [ERRO] PostgreSQL nao ficou pronto apos %MAX_RETRIES% tentativas
    exit /b 1
)

echo Aguardando PostgreSQL... [tentativa %RETRY_COUNT%/%MAX_RETRIES%]
timeout /t 2 /nobreak >nul
goto wait_postgres

:postgres_ready

REM ============================================================================
REM PASSO 3: Iniciar aplicacao Spring Boot
REM ============================================================================
echo [3/6] Iniciando aplicacao Spring Boot...
echo ----------------------------------------------------------------------------

REM Mata processos anteriores na porta 8080
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Matando processo na porta 8080: %%a
    taskkill /F /PID %%a >nul 2>&1
)

REM Inicia aplicacao em background
start /b cmd /c "mvn spring-boot:run > logs\app.log 2>&1"

echo Aguardando aplicacao iniciar...

set APP_MAX_RETRIES=60
set APP_RETRY_COUNT=0

:wait_app
set /a APP_RETRY_COUNT+=1

curl -s http://localhost:8080/actuator/health >nul 2>&1

if %ERRORLEVEL% equ 0 (
    echo [OK] Aplicacao esta pronta [tentativa %APP_RETRY_COUNT%/%APP_MAX_RETRIES%]
    echo.
    goto app_ready
)

if %APP_RETRY_COUNT% geq %APP_MAX_RETRIES% (
    echo [ERRO] Aplicacao nao iniciou apos %APP_MAX_RETRIES% tentativas
    echo Verifique os logs em logs\app.log
    exit /b 1
)

echo Aguardando aplicacao... [tentativa %APP_RETRY_COUNT%/%APP_MAX_RETRIES%]
timeout /t 2 /nobreak >nul
goto wait_app

:app_ready

REM ============================================================================
REM PASSO 4: Limpar banco de dados
REM ============================================================================
echo [4/6] Limpando banco de dados...
echo ----------------------------------------------------------------------------

call "%~dp0truncate-database.bat"

if %ERRORLEVEL% neq 0 (
    echo [ERRO] Falha ao limpar banco de dados
    exit /b 1
)

echo [OK] Banco de dados limpo com sucesso
echo.

REM ============================================================================
REM PASSO 5: Executar testes de performance (10+ minutos)
REM ============================================================================
echo [5/6] Executando testes de performance (10+ minutos)...
echo ----------------------------------------------------------------------------
echo.
echo Inicio: %date% %time%
echo.

cd performance-tests

call mvn gatling:test

if %ERRORLEVEL% neq 0 (
    echo [ERRO] Falha ao executar testes de performance
    exit /b 1
)

echo.
echo Fim: %date% %time%
echo.

cd ..

echo [OK] Testes de performance concluidos
echo.

REM ============================================================================
REM PASSO 6: Analisar resultados finais
REM ============================================================================
echo [6/6] Analisando resultados finais...
echo ----------------------------------------------------------------------------

REM Encontra o relatorio mais recente
for /f "delims=" %%i in ('dir /b /ad /o-d "performance-tests\target\gatling\pixservicesimulation-*" 2^>nul') do (
    set LATEST_REPORT=%%i
    goto found_report
)

:found_report

if "%LATEST_REPORT%"=="" (
    echo [AVISO] Nenhum relatorio Gatling encontrado
    goto end
)

set REPORT_PATH=performance-tests\target\gatling\%LATEST_REPORT%\index.html

echo.
echo ============================================================================
echo   RESULTADOS DO TESTE DE PERFORMANCE
echo ============================================================================
echo.
echo Relatorio HTML: %REPORT_PATH%
echo.

REM Extrai metricas basicas do simulation.log
if exist "%REPORT_PATH%\..\simulation.log" (
    echo Processando metricas...
    echo.

    REM Conta requests OK e KO
    for /f %%a in ('findstr /c:"OK" "%REPORT_PATH%\..\simulation.log" ^| find /c /v ""') do set OK_COUNT=%%a
    for /f %%a in ('findstr /c:"KO" "%REPORT_PATH%\..\simulation.log" ^| find /c /v ""') do set KO_COUNT=%%a

    set /a TOTAL_COUNT=!OK_COUNT! + !KO_COUNT!

    if !TOTAL_COUNT! gtr 0 (
        set /a SUCCESS_RATE=!OK_COUNT! * 100 / !TOTAL_COUNT!

        echo Total de Requests: !TOTAL_COUNT!
        echo Requests OK: !OK_COUNT!
        echo Requests KO: !KO_COUNT!
        echo Taxa de Sucesso: !SUCCESS_RATE!%%
        echo.
    )
)

echo Para visualizar o relatorio completo, abra:
echo file:///%CD:\=/%/%REPORT_PATH%
echo.

REM Abre o relatorio no navegador padrao
start "" "%REPORT_PATH%"

:end

echo ============================================================================
echo   TESTE CONCLUIDO COM SUCESSO
echo ============================================================================
echo.
echo Proximos passos:
echo   1. Analise o relatorio HTML do Gatling
echo   2. Verifique as metricas de performance
echo   3. Identifique gargalos e oportunidades de otimizacao
echo.

endlocal
