@echo off
echo Executing compile_lcIngestionApi_service.bat...
call compile_lcIngestionApi_service.bat

echo Executing run_lcIngestionApi_service_image.bat...
call run_lcIngestionApi_service_image.bat

echo Executing run_lcIngestionApi_full.bat...
call run_lcIngestionApi_full.bat

echo Finish run lcIngestionApi Docker.
pause