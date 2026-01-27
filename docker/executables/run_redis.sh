#!/bin/bash

# Stop execution in case of an error
set -e

# Define the directory where docker-compose.yml is located
COMPOSE_DIR="../compose"
COMPOSE_FILE="redis_docker_compose.yml"

# Navigate to the directory
pushd "$COMPOSE_DIR"

# Start Keycloak
echo "Starting Redis..."
docker-compose -f "$COMPOSE_FILE" --project-name redis up -d

if [ $? -ne 0 ]; then
    echo "Docker Compose failed to start Redis."
    popd
    exit 1
fi

echo "Redis is running."

# Optionally, you can add commands to check the status or logs here

# Return to the original directory
popd
