#!/bin/bash

# Stop execution in case of an error
set -e

# Get the current directory (where the script is being executed)
CURRENT_DIR=$(pwd)

# Define the relative path to gradlew from the current directory
GRADLE_REL_PATH="../../gradlew"

# Combine current directory with relative path to form full path to gradlew
GRADLE_PATH="$CURRENT_DIR/$GRADLE_REL_PATH"

# Define the project root
PROJECT_ROOT="$CURRENT_DIR/../../"

# Step 1: Navigate to the project root and compile the application using Gradle
pushd "$PROJECT_ROOT"
echo "Compiling the application with Gradle..."
echo "Project root: $PROJECT_ROOT"
echo "Gradle path: $GRADLE_PATH"
"$GRADLE_PATH" build
if [ $? -ne 0 ]; then
    echo "Gradle build failed."
    popd
    exit 1
fi
echo "Compile the application with Gradle finished"
popd
