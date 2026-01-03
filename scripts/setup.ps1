# PowerShell setup script for Windows
# Simple setup: installs mise, then uses mise for everything

Write-Host "Setting up Bug Bounty Finder..." -ForegroundColor Blue
Write-Host ""

# Install mise if not present
if (-not (Get-Command mise -ErrorAction SilentlyContinue)) {
    Write-Host "Installing mise-en-place..." -ForegroundColor Blue
    winget install jdx.mise
    Write-Host "mise installed. Please restart your terminal and run this script again." -ForegroundColor Green
    exit 0
}

Write-Host "mise found" -ForegroundColor Green

# Install tools via mise
Write-Host "Installing tools (Java 21, Gradle 8.5)..." -ForegroundColor Blue
mise install
Write-Host "Tools installed" -ForegroundColor Green

# Check Docker
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker is not installed. Please install Docker Desktop." -ForegroundColor Red
    exit 1
}
Write-Host "Docker found" -ForegroundColor Green

# Start Docker services
Write-Host "Starting Docker services..." -ForegroundColor Blue
mise run start-services
Start-Sleep -Seconds 3

# Create .env file if needed
if (-not (Test-Path .env)) {
    Write-Host "Creating .env file..." -ForegroundColor Blue
    $content = "# Database Configuration`nDB_USERNAME=postgres`nDB_PASSWORD=postgres`n`n# Redis Configuration`nREDIS_HOST=localhost`nREDIS_PORT=6379`n`n# Ollama Configuration`nOLLAMA_BASE_URL=http://localhost:11434`nOLLAMA_MODEL=llama3.2:3b`n`n# Repository Clone Path`nREPO_CLONE_PATH=./repos"
    Set-Content -Path .env -Value $content -Encoding utf8
    Write-Host ".env file created" -ForegroundColor Green
}

# Create directories
New-Item -ItemType Directory -Force -Path repos | Out-Null
New-Item -ItemType Directory -Force -Path logs | Out-Null

# Build project
Write-Host "Building project..." -ForegroundColor Blue
mise run build

Write-Host ""
Write-Host "Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  mise run test    # Run tests" -ForegroundColor White
Write-Host "  mise run run     # Run application" -ForegroundColor White
Write-Host "  mise tasks       # View all commands" -ForegroundColor White
Write-Host ""
