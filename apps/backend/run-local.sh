#!/bin/bash

# Local Development Run Script for Ziboto Backend
# This script ensures MapStruct classes are properly generated before running

set -e

echo "🔨 Cleaning and compiling..."
mvn clean compile

echo "✅ Compilation complete!"
echo "🚀 Starting Spring Boot application..."
echo ""

mvn spring-boot:run
