@echo off
setlocal

REM Define paths
set CURRENT_DIR=%CD%

pushd ..
set PROJECT_ROOT_DOCKER_FILE=%CD%\dockerfiles
set PROJECT_ROOT=%CD% ..\..
popd

set DOCKERFILE_PATH=%CURRENT_DIR%
set IMAGE_NAME=lcingestionapi-service-app
set IMAGE_NAME_RUN=lcingestionapirun    -service-app-running

REM Step 1: Build the Docker image
echo Building Docker image...
echo Image name: %IMAGE_NAME%
echo Project root: %PROJECT_ROOT%
echo Project root docker file: %PROJECT_ROOT_DOCKER_FILE%
docker build -t %IMAGE_NAME% -f "%PROJECT_ROOT_DOCKER_FILE%\Dockerfile_local" "%PROJECT_ROOT%"
if %ERRORLEVEL% NEQ 0 (
    echo Docker build failed.
    exit /b %ERRORLEVEL%
)

echo Build and run completed successfully.
endlocal
