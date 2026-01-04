plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    jacoco
}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File

group = "com.bugbounty"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.spring.io/milestone")
    }
    maven {
        url = uri("https://repo.spring.io/snapshot")
    }
}

extra["springAiVersion"] = "0.8.0"
extra["resilience4jVersion"] = "2.1.0"
extra["jgitVersion"] = "6.8.0.202311291450-r"
extra["mockWebServerVersion"] = "4.12.0"
extra["testcontainersVersion"] = "1.19.3"
extra["awaitilityVersion"] = "4.2.0"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-core:${property("springAiVersion")}")
    implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter:${property("springAiVersion")}")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Git Operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:${property("jgitVersion")}")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.h2database:h2")
    testImplementation("com.squareup.okhttp3:mockwebserver:${property("mockWebServerVersion")}")
    testImplementation("org.awaitility:awaitility:${property("awaitilityVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // Enable JaCoCo for test execution
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.11"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

// Generate code coverage report after tests
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    // Configure class and source directories
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/entity/**",
                    "**/dto/**",
                    "**/*Application.class",
                    "**/*Config.class"
                )
            }
        }
    )
    
    sourceDirectories.setFrom(sourceSets.main.get().allSource.srcDirs)
    
    // Save report with timestamp for historical tracking
    doLast {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val reportDir = reports.html.outputLocation.get().asFile
        val timestampedDir = File(reportDir.parent, "jacoco-html-$timestamp")
        reportDir.copyRecursively(timestampedDir, overwrite = true)
        println("Code coverage report saved to: ${timestampedDir.absolutePath}")
        println("Latest report: ${reportDir.absolutePath}")
    }
}

// Task to print coverage summary from XML report
tasks.register("jacocoCoverageSummary") {
    description = "Print code coverage summary from JaCoCo XML report"
    dependsOn(tasks.jacocoTestReport)
    
    doLast {
        try {
            val xmlReport = tasks.jacocoTestReport.get().reports.xml.outputLocation.get().asFile
            val htmlReport = tasks.jacocoTestReport.get().reports.html.outputLocation.get().asFile
            
            if (!xmlReport.exists()) {
                println("Coverage XML report not found at: ${xmlReport.absolutePath}")
                return@doLast
            }
            
            val xml = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
            }.newDocumentBuilder().parse(xmlReport)
            
            val counters = xml.getElementsByTagName("counter")
            var instructionCounter: org.w3c.dom.Element? = null
            
            for (i in 0 until counters.length) {
                val counter = counters.item(i) as org.w3c.dom.Element
                if (counter.getAttribute("type") == "INSTRUCTION") {
                    instructionCounter = counter
                    break
                }
            }
            
            if (instructionCounter != null) {
                val missed = instructionCounter.getAttribute("missed").toIntOrNull() ?: 0
                val covered = instructionCounter.getAttribute("covered").toIntOrNull() ?: 0
                val total = missed + covered
                val percentage = if (total > 0) {
                    String.format("%.2f", (covered.toDouble() / total) * 100)
                } else {
                    "0.00"
                }
                
                println("\n=== Code Coverage Summary ===")
                println("Instructions: $covered/$total ($percentage%)")
                println("  Covered: $covered")
                println("  Missed: $missed")
                println("\nHTML Report: ${htmlReport.absolutePath}")
                println("XML Report: ${xmlReport.absolutePath}")
            } else {
                println("Could not find instruction counter in coverage report")
            }
        } catch (e: Exception) {
            // Silently fail - don't break the build
            println("Note: Could not generate coverage summary: ${e.message}")
        }
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${project.name}-${project.version}.jar")
}

