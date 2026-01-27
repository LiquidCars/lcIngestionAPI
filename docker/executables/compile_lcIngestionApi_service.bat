@echo off
setlocal

REM Get the current directory (where the script is being executed)
set CURRENT_DIR=%CD%

REM Define the relative path to gradlew.bat from the current directory
set GRADLE_REL_PATH=..\..\gradlew.bat

REM Combine current directory with relative path to form full path to gradlew.bat
set GRADLE_PATH=%CURRENT_DIR%\%GRADLE_REL_PATH%

REM Define the project root
set PROJECT_ROOT=%CURRENT_DIR%\..\..


REM Step 1: Navigate to the project root and compile the application using Gradle
pushd %PROJECT_ROOT%
echo Compiling the application with Gradle...
echo Project root: %PROJECT_ROOT%
echo Gradle path: %GRADLE_PATH%
%GRADLE_PATH% build
if %ERRORLEVEL% NEQ 0 (
    echo Gradle build failed.
    popd
    exit /b %ERRORLEVEL%
)
echo Compile the application with Gradle finished
popd

endlocal
