@echo off
echo Executing compile_lcApi_service.bat...
call compile_lcApi_service.bat

echo Executing run_lcApi_service_image.bat...
call run_lcApi_service_image.bat

echo Executing run_lcApi_full.bat...
call run_lcApi_full.bat

echo Finish run lcApi Docker.
pause