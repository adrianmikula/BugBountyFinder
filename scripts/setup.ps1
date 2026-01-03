# PowerShell setup script for Windows

Write-Host "🚀 Setting up Bug Bounty Finder development environment..." -ForegroundColor Blue

# Check if Java 21 is installed
Write-Host "Checking Java version..." -ForegroundColor Blue
try {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version" | ForEach-Object { $_.Line }
    Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java is not installed. Please install Java 21 or later." -ForegroundColor Yellow
    exit 1
}

# Check if Docker is installed
Write-Host "Checking Docker..." -ForegroundColor Blue
try {
    docker --version | Out-Null
    Write-Host "✓ Docker found" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not installed. Please install Docker Desktop." -ForegroundColor Yellow
    exit 1
}

# Check if Docker Compose is available
Write-Host "Checking Docker Compose..." -ForegroundColor Blue
try {
    docker compose version | Out-Null
    Write-Host "✓ Docker Compose found" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Compose is not available." -ForegroundColor Yellow
    exit 1
}

# Start Docker services
Write-Host "Starting Docker services (PostgreSQL and Redis)..." -ForegroundColor Blue
docker compose up -d

# Wait for services
Write-Host "Waiting for services to be ready..." -ForegroundColor Blue
Start-Sleep -Seconds 5

# Check PostgreSQL
Write-Host "Checking PostgreSQL connection..." -ForegroundColor Blue
$maxRetries = 30
$retryCount = 0
do {
    try {
        docker exec bugbounty-postgres pg_isready -U postgres | Out-Null
        Write-Host "✓ PostgreSQL is ready" -ForegroundColor Green
        break
    } catch {
        $retryCount++
        if ($retryCount -ge $maxRetries) {
            Write-Host "✗ PostgreSQL failed to start" -ForegroundColor Red
            exit 1
        }
        Start-Sleep -Seconds 2
    }
} while ($retryCount -lt $maxRetries)

# Check Redis
Write-Host "Checking Redis connection..." -ForegroundColor Blue
$retryCount = 0
do {
    try {
        docker exec bugbounty-redis redis-cli ping | Out-Null
        Write-Host "✓ Redis is ready" -ForegroundColor Green
        break
    } catch {
        $retryCount++
        if ($retryCount -ge $maxRetries) {
            Write-Host "✗ Redis failed to start" -ForegroundColor Red
            exit 1
        }
        Start-Sleep -Seconds 2
    }
} while ($retryCount -lt $maxRetries)

# Check if Ollama is installed
Write-Host "Checking Ollama..." -ForegroundColor Blue
try {
    ollama --version | Out-Null
    Write-Host "✓ Ollama found" -ForegroundColor Green
} catch {
    Write-Host "Ollama is not installed. Please install from https://ollama.ai" -ForegroundColor Yellow
    Write-Host "After installation, run: ollama pull llama3.2:3b" -ForegroundColor Yellow
}

# Check Ollama model
Write-Host "Checking Ollama model (llama3.2:3b)..." -ForegroundColor Blue
try {
    $models = ollama list
    if ($models -match "llama3.2:3b") {
        Write-Host "✓ Model already available" -ForegroundColor Green
    } else {
        Write-Host "Pulling llama3.2:3b model (this may take a while)..." -ForegroundColor Blue
        ollama pull llama3.2:3b
        Write-Host "✓ Model downloaded" -ForegroundColor Green
    }
} catch {
    Write-Host "Could not check/pull Ollama model. Please ensure Ollama is running." -ForegroundColor Yellow
}

# Create .env file if it doesn't exist
if (-not (Test-Path .env)) {
    Write-Host "Creating .env file..." -ForegroundColor Blue
    @"
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
"@ | Out-File -FilePath .env -Encoding utf8
    Write-Host "✓ .env file created" -ForegroundColor Green
} else {
    Write-Host "✓ .env file already exists" -ForegroundColor Green
}

# Create directories
Write-Host "Creating necessary directories..." -ForegroundColor Blue
New-Item -ItemType Directory -Force -Path repos | Out-Null
New-Item -ItemType Directory -Force -Path logs | Out-Null
Write-Host "✓ Directories created" -ForegroundColor Green

# Initialize Gradle wrapper if needed
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Initializing Gradle wrapper..." -ForegroundColor Blue
    if (Get-Command gradle -ErrorAction SilentlyContinue) {
        gradle wrapper --gradle-version 8.5
        Write-Host "✓ Gradle wrapper initialized" -ForegroundColor Green
    } else {
        Write-Host "Gradle is not installed. Please install Gradle first." -ForegroundColor Yellow
        Write-Host "Download from: https://gradle.org/install/" -ForegroundColor Yellow
        exit 1
    }
}

# Build the project
Write-Host "Building the project with Gradle..." -ForegroundColor Blue
.\gradlew.bat build -x test
Write-Host "✓ Build completed" -ForegroundColor Green

Write-Host ""
Write-Host "✅ Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Review and update .env file if needed"
Write-Host "2. Run tests: .\gradlew.bat test"
Write-Host "3. Start the application: .\gradlew.bat bootRun"
Write-Host ""
Write-Host "Services running:"
Write-Host "  - PostgreSQL: localhost:5432"
Write-Host "  - Redis: localhost:6379"
Write-Host "  - Ollama: http://localhost:11434"
Write-Host ""

