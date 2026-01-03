#!/bin/bash

# Initialize Gradle wrapper if it doesn't exist

if [ ! -f "gradlew" ]; then
    echo "Initializing Gradle wrapper..."
    
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.5
        echo "✓ Gradle wrapper initialized"
    else
        echo "Gradle is not installed. Please install Gradle or use the setup script."
        echo "You can download Gradle from: https://gradle.org/install/"
        exit 1
    fi
else
    echo "Gradle wrapper already exists"
fi

