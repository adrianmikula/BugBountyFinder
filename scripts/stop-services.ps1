# PowerShell script to stop Docker services

Write-Host "Stopping PostgreSQL and Redis..." -ForegroundColor Blue
docker compose down
Write-Host "Services stopped!" -ForegroundColor Green

