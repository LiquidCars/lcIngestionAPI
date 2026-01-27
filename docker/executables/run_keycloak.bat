@echo off
setlocal

REM Define the directory where docker-compose.yml is located
set COMPOSE_DIR=..\compose
set COMPOSE_FILE=keycloak_compose.yml

REM Navigate to the directory
pushd "%COMPOSE_DIR%"

REM Start Keycloak
echo Starting Keycloak...
docker-compose -f %COMPOSE_FILE% --project-name keycloak up -d

if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose failed to start Keycloak.
    popd
    exit /b %ERRORLEVEL%
)

echo Keycloak is running.

REM Optionally, you can add commands to check the status or logs here

REM Return to the original directory
popd

endlocal
