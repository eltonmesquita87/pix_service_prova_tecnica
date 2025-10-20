@echo off
REM ============================================================================
REM Script de Manutenção de Partições - Windows
REM ============================================================================
REM Objetivo: Criar partições futuras automaticamente
REM Frequência: Executar mensalmente (primeiro dia do mês)
REM ============================================================================

setlocal

echo.
echo ============================================================================
echo   MANUTENCAO DE PARTICOES - PIX SERVICE
echo ============================================================================
echo.

cd /d "%~dp0.."

REM Verificar se PostgreSQL está rodando
docker exec pix-postgres pg_isready -U pixuser -d pixdb >nul 2>&1

if %ERRORLEVEL% neq 0 (
    echo [ERRO] PostgreSQL nao esta rodando ou nao esta acessivel
    echo Execute: docker-compose up -d
    exit /b 1
)

echo [OK] PostgreSQL esta acessivel
echo.

echo Executando manutencao de particoes...
echo ----------------------------------------------------------------------------

REM Executar função de manutenção
docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT maintain_all_partitions();"

if %ERRORLEVEL% neq 0 (
    echo [ERRO] Falha ao executar manutencao de particoes
    exit /b 1
)

echo.
echo ============================================================================
echo   VERIFICANDO SAUDE DAS PARTICOES
echo ============================================================================
echo.

docker exec -i pix-postgres psql -U pixuser -d pixdb -c "SELECT * FROM check_partition_health();"

echo.
echo ============================================================================
echo   MANUTENCAO CONCLUIDA
echo ============================================================================
echo.
echo Proximos passos:
echo   1. Revisar particoes criadas acima
echo   2. Agendar execucao mensal deste script (Task Scheduler)
echo   3. Monitorar crescimento das particoes
echo.

endlocal
