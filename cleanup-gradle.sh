#!/bin/bash

# Gradle Cleanup Script to fix hanging issues

echo "Stopping all Gradle daemons..."
./gradlew --stop

echo "Killing any remaining Java/Gradle processes..."
pkill -f gradle 2>/dev/null || true
pkill -f java 2>/dev/null || true

echo "Cleaning build directory..."
rm -rf build/
rm -rf .gradle/

echo "Clearing Gradle cache..."
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/

echo "Setting proper permissions..."
chmod +x gradlew

echo "Cleanup complete!"
