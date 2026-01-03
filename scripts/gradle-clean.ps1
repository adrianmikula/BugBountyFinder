# Helper script for Gradle clean
if (Test-Path gradlew.bat) {
    .\gradlew.bat clean
} else {
    gradle clean
}

