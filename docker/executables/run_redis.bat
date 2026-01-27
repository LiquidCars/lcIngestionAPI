@echo off
setlocal

REM Define the directory where docker-compose.yml is located
set COMPOSE_DIR=..\compose
set COMPOSE_FILE=redis_docker_compose.yml

REM Navigate to the directory
pushd "%COMPOSE_DIR%"

REM Start Redis
echo Starting Redis...
docker-compose -f %COMPOSE_FILE% --project-name redis up -d

if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose failed to start Redis.
    popd
    exit /b %ERRORLEVEL%
)

echo Redis is running.

REM Optionally, you can add commands to check the status or logs here

REM Return to the original directory
popd

endlocal
