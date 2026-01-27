#!/bin/bash

# Stop execution in case of an error
set -e

# Define paths
CURRENT_DIR=$(pwd)

# Navigate one directory up
PARENT_DIR=$(dirname "$CURRENT_DIR")
PROJECT_ROOT_DOCKER_FILE="$PARENT_DIR/dockerfiles"
PROJECT_ROOT="$PARENT_DIR/.."
IMAGE_NAME="lcapi-service-app"
IMAGE_NAME_RUN="lcapi-service-app-running"

# Step 1: Build the Docker image
echo "Building Docker image..."
echo "Image name: $IMAGE_NAME"
echo "Project root: $PROJECT_ROOT"
echo "Project root docker file: $PROJECT_ROOT_DOCKER_FILE"

docker build -t "$IMAGE_NAME" -f "$PROJECT_ROOT_DOCKER_FILE/Dockerfile_local" "$PROJECT_ROOT"

echo "Build and run completed successfully."
