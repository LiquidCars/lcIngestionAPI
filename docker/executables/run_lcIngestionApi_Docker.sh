#!/bin/bash

echo "Executing compile_lcApi_service.sh..."
./compile_lcApi_service.sh

echo "Executing run_lcApi_service_image.sh..."
./run_lcApi_service_image.sh

echo "Executing run_lcApi_full.sh..."
./run_lcApi_full.sh

echo "Finish run lcApi Docker."
read -p "Press enter to continue"