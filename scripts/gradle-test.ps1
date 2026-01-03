# Helper script for Gradle test
param([switch]$UnitOnly, [switch]$ComponentOnly)

if (Test-Path gradlew.bat) {
    if ($UnitOnly) {
        .\gradlew.bat test --tests "*Test" --exclude-tests "*ComponentTest"
    } elseif ($ComponentOnly) {
        .\gradlew.bat test --tests "com.bugbounty.component.*"
    } else {
        .\gradlew.bat test
    }
} else {
    if ($UnitOnly) {
        gradle test --tests "*Test" --exclude-tests "*ComponentTest"
    } elseif ($ComponentOnly) {
        gradle test --tests "com.bugbounty.component.*"
    } else {
        gradle test
    }
}

