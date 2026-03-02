#!/bin/bash

echo "Executing compile_lcIngestionApi_service.sh..."
./compile_lcIngestionApi_service.sh

echo "Executing run_lcIngestionApi_service_image.sh..."
./run_lcIngestionApi_service_image.sh

echo "Executing run_lcIngestionApi_full.sh..."
./run_lcIngestionApi_full.sh

echo "Finish run lcIngestionApi Docker."
read -p "Press enter to continue"