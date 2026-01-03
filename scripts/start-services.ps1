# PowerShell script to start Docker services

Write-Host "Starting PostgreSQL and Redis..." -ForegroundColor Blue
docker compose up -d
Write-Host "Services started!" -ForegroundColor Green
Write-Host "PostgreSQL: localhost:5432" -ForegroundColor Cyan
Write-Host "Redis: localhost:6379" -ForegroundColor Cyan

