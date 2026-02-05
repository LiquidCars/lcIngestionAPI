@echo off
setlocal

REM Define the directory where docker-compose.yml is located
set COMPOSE_DIR=..\compose
set COMPOSE_FILE_LIQUIDCARS=lcingestionapi_docker_compose.yml

REM Navigate to the directory
pushd "%COMPOSE_DIR%"

REM Start app with all services involved
echo Starting all services...

docker-compose -f %COMPOSE_FILE_LIQUIDCARS% --project-name lcingestionapi up -d

if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose failed to start lcingestionapi.
    popd
    exit /b %ERRORLEVEL%
)

echo lcingestionapi is running.

REM Optionally, you can add commands to check the status or logs here

REM Return to the original directory
popd

endlocal
