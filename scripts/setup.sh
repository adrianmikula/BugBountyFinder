#!/bin/bash

set -e

echo "🚀 Setting up Bug Bounty Finder development environment..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Java 21 is installed
echo -e "${BLUE}Checking Java version...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}Java is not installed. Please install Java 21 or later.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${YELLOW}Java 21 or later is required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION found${NC}"

# Check if Docker is installed
echo -e "${BLUE}Checking Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}Docker is not installed. Please install Docker to run PostgreSQL and Redis.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker found${NC}"

# Check if Docker Compose is available
if ! docker compose version &> /dev/null && ! docker-compose version &> /dev/null; then
    echo -e "${YELLOW}Docker Compose is not available.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker Compose found${NC}"

# Start Docker services
echo -e "${BLUE}Starting Docker services (PostgreSQL and Redis)...${NC}"
if command -v docker compose &> /dev/null; then
    docker compose up -d
else
    docker-compose up -d
fi

# Wait for services to be healthy
echo -e "${BLUE}Waiting for services to be ready...${NC}"
sleep 5

# Check PostgreSQL
echo -e "${BLUE}Checking PostgreSQL connection...${NC}"
until docker exec bugbounty-postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo "Waiting for PostgreSQL..."
    sleep 2
done
echo -e "${GREEN}✓ PostgreSQL is ready${NC}"

# Check Redis
echo -e "${BLUE}Checking Redis connection...${NC}"
until docker exec bugbounty-redis redis-cli ping > /dev/null 2>&1; do
    echo "Waiting for Redis..."
    sleep 2
done
echo -e "${GREEN}✓ Redis is ready${NC}"

# Check if Ollama is installed
echo -e "${BLUE}Checking Ollama...${NC}"
if ! command -v ollama &> /dev/null; then
    echo -e "${YELLOW}Ollama is not installed. Installing...${NC}"
    curl -fsSL https://ollama.ai/install.sh | sh
    echo -e "${GREEN}✓ Ollama installed${NC}"
else
    echo -e "${GREEN}✓ Ollama found${NC}"
fi

# Pull Ollama model if not present
echo -e "${BLUE}Checking Ollama model (llama3.2:3b)...${NC}"
if ! ollama list | grep -q "llama3.2:3b"; then
    echo -e "${BLUE}Pulling llama3.2:3b model (this may take a while)...${NC}"
    ollama pull llama3.2:3b
    echo -e "${GREEN}✓ Model downloaded${NC}"
else
    echo -e "${GREEN}✓ Model already available${NC}"
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo -e "${BLUE}Creating .env file...${NC}"
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
    echo -e "${GREEN}✓ .env file created${NC}"
else
    echo -e "${GREEN}✓ .env file already exists${NC}"
fi

# Create directories
echo -e "${BLUE}Creating necessary directories...${NC}"
mkdir -p repos
mkdir -p logs
echo -e "${GREEN}✓ Directories created${NC}"

# Initialize Gradle wrapper if needed
if [ ! -f "gradlew" ]; then
    echo -e "${BLUE}Initializing Gradle wrapper...${NC}"
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.5
        echo -e "${GREEN}✓ Gradle wrapper initialized${NC}"
    else
        echo -e "${YELLOW}Gradle is not installed. Please install Gradle first.${NC}"
        echo -e "${YELLOW}Download from: https://gradle.org/install/${NC}"
        exit 1
    fi
fi

# Build the project
echo -e "${BLUE}Building the project with Gradle...${NC}"
./gradlew build -x test
echo -e "${GREEN}✓ Build completed${NC}"

echo ""
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Review and update .env file if needed"
echo "2. Run tests: ./gradlew test"
echo "3. Start the application: ./gradlew bootRun"
echo ""
echo "Services running:"
echo "  - PostgreSQL: localhost:5432"
echo "  - Redis: localhost:6379"
echo "  - Ollama: http://localhost:11434"
echo ""

