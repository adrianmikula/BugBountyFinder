# Helper script for Gradle run
if (Test-Path gradlew.bat) {
    .\gradlew.bat bootRun
} else {
    gradle bootRun
}

