# Helper script for Gradle test
param([switch]$UnitOnly, [switch]$ComponentOnly, [switch]$E2EOnly)

if (Test-Path gradlew.bat) {
    if ($UnitOnly) {
        .\gradlew.bat test --tests "*Test" --exclude-tests "*ComponentTest" --exclude-tests "*E2ETest" --exclude-tests "*e2e.*"
    } elseif ($ComponentOnly) {
        .\gradlew.bat test --tests "com.bugbounty.component.*"
    } elseif ($E2EOnly) {
        .\gradlew.bat test --tests "com.bugbounty.e2e.*"
    } else {
        .\gradlew.bat test
    }
} else {
    if ($UnitOnly) {
        gradle test --tests "*Test" --exclude-tests "*ComponentTest" --exclude-tests "*E2ETest" --exclude-tests "*e2e.*"
    } elseif ($ComponentOnly) {
        gradle test --tests "com.bugbounty.component.*"
    } elseif ($E2EOnly) {
        gradle test --tests "com.bugbounty.e2e.*"
    } else {
        gradle test
    }
}

