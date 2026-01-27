#!/bin/bash

# Stop execution in case of an error
set -e

# Define the directory where docker-compose.yml is located
COMPOSE_DIR="../compose"
COMPOSE_FILE="lcapi_docker_compose.yml"

# Navigate to the directory
pushd "$COMPOSE_DIR"

# Start app with all services involved
echo "Starting all services......"
docker-compose -f "$COMPOSE_FILE" --project-name liquidcars up -d

if [ $? -ne 0 ]; then
    echo "Docker Compose failed to start all services."
    popd
    exit 1
fi

echo "liquidcars is running."

# Optionally, you can add commands to check the status or logs here

# Return to the original directory
popd
