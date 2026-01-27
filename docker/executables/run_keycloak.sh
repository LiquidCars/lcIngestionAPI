#!/bin/bash

# Stop execution in case of an error
set -e

# Define the directory where docker-compose.yml is located
COMPOSE_DIR="../compose"
COMPOSE_FILE="keycloak_compose.yml"

# Navigate to the directory
pushd "$COMPOSE_DIR"

# Start Keycloak
echo "Starting Keycloak..."
docker-compose -f "$COMPOSE_FILE" --project-name keycloak up -d

if [ $? -ne 0 ]; then
    echo "Docker Compose failed to start Keycloak."
    popd
    exit 1
fi

echo "Keycloak is running."

# Optionally, you can add commands to check the status or logs here

# Return to the original directory
popd
