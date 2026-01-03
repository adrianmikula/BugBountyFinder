#!/bin/bash

# Bash setup script for Linux/Mac
# Simple setup: installs mise, then uses mise for everything

set -e

echo "🚀 Setting up Bug Bounty Finder..."
echo ""

# Install mise if not present
if ! command -v mise &> /dev/null; then
    echo "Installing mise-en-place..."
    curl https://mise.run | sh
    echo "✓ mise installed. Please restart your terminal and run this script again."
    exit 0
fi

echo "✓ mise found"

# Install tools via mise
echo "Installing tools (Java 21, Gradle 8.5)..."
mise install
echo "✓ Tools installed"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "✗ Docker is not installed. Please install Docker."
    exit 1
fi
echo "✓ Docker found"

# Start Docker services
echo "Starting Docker services..."
mise run start-services
sleep 3

# Create .env file if needed
if [ ! -f .env ]; then
    echo "Creating .env file..."
    cat > .env << EOF
# Database Configuration
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Ollama Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:3b

# Repository Clone Path
REPO_CLONE_PATH=./repos
EOF
    echo "✓ .env file created"
fi

# Create directories
mkdir -p repos logs

# Build project
echo "Building project..."
mise run build

echo ""
echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "  mise run test    # Run tests"
echo "  mise run run     # Run application"
echo "  mise tasks       # View all commands"
echo ""
