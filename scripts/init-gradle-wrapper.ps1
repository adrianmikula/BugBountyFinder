# Initialize Gradle wrapper if it doesn't exist

if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Initializing Gradle wrapper..." -ForegroundColor Blue
    
    if (Get-Command gradle -ErrorAction SilentlyContinue) {
        gradle wrapper --gradle-version 8.5
        Write-Host "✓ Gradle wrapper initialized" -ForegroundColor Green
    } else {
        Write-Host "Gradle is not installed. Please install Gradle or use the setup script." -ForegroundColor Yellow
        Write-Host "You can download Gradle from: https://gradle.org/install/" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Gradle wrapper already exists" -ForegroundColor Green
}

