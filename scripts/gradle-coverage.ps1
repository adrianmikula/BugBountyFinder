# Generate code coverage report and open in browser

$ErrorActionPreference = "Continue"

# Run tests first (even if they fail, we still want coverage data)
Write-Host "Running tests to generate coverage data..." -ForegroundColor Cyan
if (Test-Path gradlew.bat) {
    .\gradlew.bat test --continue 2>&1 | Out-Null
} else {
    gradle test --continue 2>&1 | Out-Null
}

# Generate coverage report (this will also run jacocoCoverageSummary task)
Write-Host "Generating coverage report..." -ForegroundColor Cyan
if (Test-Path gradlew.bat) {
    .\gradlew.bat jacocoTestReport jacocoCoverageSummary
} else {
    gradle jacocoTestReport jacocoCoverageSummary
}

$reportPath = "build\reports\jacoco\test\html\index.html"
$xmlPath = "build\reports\jacoco\test\jacocoTestReport.xml"

if (Test-Path $reportPath) {
    Write-Host ""
    Write-Host "=== Code Coverage Report ===" -ForegroundColor Green
    Write-Host "HTML Report: $((Resolve-Path $reportPath).Path)" -ForegroundColor Cyan
    Write-Host "XML Report: $((Resolve-Path $xmlPath).Path)" -ForegroundColor Cyan
    
    # Parse XML and show summary
    if (Test-Path $xmlPath) {
        try {
            $xml = [xml](Get-Content $xmlPath)
            $counter = $xml.report.counter | Where-Object { $_.type -eq "INSTRUCTION" }
            if ($counter) {
                $missed = [int]$counter.missed
                $covered = [int]$counter.covered
                $total = $missed + $covered
                $percentage = if ($total -gt 0) { [math]::Round(($covered / $total) * 100, 2) } else { 0 }
                
                Write-Host ""
                Write-Host "Coverage Summary:" -ForegroundColor Yellow
                
                $color = if ($percentage -ge 80) { "Green" } elseif ($percentage -ge 60) { "Yellow" } else { "Red" }
                Write-Host "  Instructions: $covered/$total ($percentage%)" -ForegroundColor $color
                Write-Host "  Covered: $covered" -ForegroundColor Green
                Write-Host "  Missed: $missed" -ForegroundColor Red
            }
        } catch {
            Write-Host "  Could not parse coverage summary" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "Opening in browser..." -ForegroundColor Yellow
    Start-Process $reportPath
} else {
    Write-Host "Coverage report not found. Run tests first." -ForegroundColor Red
    exit 1
}

