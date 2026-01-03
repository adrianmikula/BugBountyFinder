# Helper script for Gradle build
param([switch]$WithTests)

if (Test-Path gradlew.bat) {
    if ($WithTests) {
        .\gradlew.bat build
    } else {
        .\gradlew.bat build -x test
    }
} else {
    # Use mise exec to run gradle (mise exec sets up the environment)
    if ($WithTests) {
        mise exec -- gradle build
    } else {
        mise exec -- gradle build -x test
    }
}

