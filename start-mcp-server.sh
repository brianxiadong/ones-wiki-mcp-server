#!/bin/bash

# ONES Wiki MCP Server Startup Script

# Default configuration
DEFAULT_HOST="your-ones-host.com"
DEFAULT_EMAIL="your-email@example.com"
DEFAULT_PASSWORD="your-password"

# Get configuration from environment variables or use defaults
ONES_HOST=${ONES_HOST:-$DEFAULT_HOST}
ONES_EMAIL=${ONES_EMAIL:-$DEFAULT_EMAIL}
ONES_PASSWORD=${ONES_PASSWORD:-$DEFAULT_PASSWORD}

# JAR file path
JAR_FILE="target/ones-wiki-mcp-server-0.0.1-SNAPSHOT.jar"

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run 'mvn clean package' first to build the project"
    exit 1
fi

# Print configuration (without password)
echo "Starting ONES Wiki MCP Server..."
echo "Host: $ONES_HOST"
echo "Email: $ONES_EMAIL"
echo "Password: [HIDDEN]"
echo "JAR: $JAR_FILE"
echo "=========================="

# Start the server
java -jar "$JAR_FILE" \
    --ones.host="$ONES_HOST" \
    --ones.email="$ONES_EMAIL" \
    --ones.password="$ONES_PASSWORD" \
    --spring.main.web-application-type=none \
    --spring.main.banner-mode=off \
    --logging.pattern.console=""

echo "Server stopped." 