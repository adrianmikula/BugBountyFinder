# Bug Bounty Finder

Automated bug bounty and PR bounty hunting system built with Spring Boot 3.x.

## Features

- **Bounty Polling**: Automatically polls Algora and Polar.sh for new bounties
- **GitHub Webhooks**: Real-time notifications when commits are pushed to repositories
- **Repository Management**: Clones and manages GitHub repositories
- **LLM Integration**: Uses Ollama for local LLM inference (cost-effective)
- **Virtual Threads**: High concurrency with Project Loom
- **TDD Approach**: Comprehensive unit tests for all core components

## Tech Stack

- **Spring Boot 3.2+** with Java 21
- **Gradle** for build automation
- **Spring AI** for LLM orchestration
- **PostgreSQL** for state management
- **Redis** for caching and queues
- **JGit** for Git operations
- **Ollama** for local LLM inference
- **Resilience4j** for circuit breakers and rate limiting
- **Docker Compose** for local development

## Project Structure

```
src/
├── main/
│   ├── java/com/bugbounty/
│   │   ├── bounty/          # Bounty domain and services
│   │   ├── repository/      # Repository management
│   │   └── ...
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/bugbounty/  # Unit tests (TDD)
```

## Documentation

### Architecture

For detailed architecture documentation, see the [Architecture Documentation](docs/architecture/README.md) which covers:

- **Architectural Patterns**: Reactive programming, DDD, circuit breakers, event-driven architecture
- **Data Flows**: Bounty discovery, webhook processing, CVE monitoring pipelines
- **System Integrations**: External APIs, LLM integration, database patterns
- **Component Architecture**: Domain layers, services, and component interactions

### Coding Standards

This project follows industry-standard best practices. See the [Coding Standards](docs/standards/README.md) for:

- **Core Principles**: TDD, DRY, KISS, SOLID
- **Testing Standards**: Unit tests, component tests, coverage requirements
- **Code Quality**: Code review checklist, design patterns, best practices
- **Java 21 Features**: Modern language features and usage guidelines

## Getting Started

### Prerequisites

- Java 21+
- Gradle 8.0+ (or use included wrapper)
- Docker & Docker Compose
- Ollama (for local LLM)

### Quick Setup

**Linux/Mac:**
```bash
./scripts/setup.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\setup.ps1
```

The setup script will:
- Install mise-en-place if not present
- Use mise to install tools (Java 21, Gradle 8.5)
- Start PostgreSQL and Redis via Docker
- Create configuration files and directories
- Build the project

After setup, use mise commands for daily development:
```bash
mise run test            # Run tests
mise run run             # Run backend application
mise run frontend-dev    # Start frontend development server
mise run frontend-build  # Build frontend for production
mise tasks               # View all commands
```

### Manual Setup

1. **Start Docker services:**
   ```bash
   docker compose up -d
   ```

2. **Install Ollama and pull the recommended model:**
   ```bash
   # Linux/Mac
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Windows: Download from https://ollama.ai
   
   # Pull DeepSeek Coder 6.7B (recommended for code review and bug fixing)
   ollama pull deepseek-coder:6.7b
   
   # Alternative: For faster inference with lower accuracy, use:
   # ollama pull deepseek-coder:1.3b
   ```

3. **Initialize Gradle wrapper:**
   ```bash
   gradle wrapper
   ```

4. **Build the project:**
   ```bash
   ./gradlew build
   ```

5. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```

### Code Coverage

Code coverage reports are automatically generated after each test run using JaCoCo.

**Using Mise (Recommended):**
```bash
mise run test          # Run tests (automatically generates coverage)
mise run coverage      # Generate coverage report manually
mise run coverage-open # Open coverage report in browser
mise run coverage-clean # Clean coverage reports
```

**Using Gradle directly:**
```bash
# After running tests, coverage reports are available at:
# HTML: build/reports/jacoco/test/html/index.html
# XML:  build/reports/jacoco/test/jacocoTestReport.xml

# Generate coverage report manually:
./gradlew jacocoTestReport

# Reports are also saved with timestamps for historical tracking:
# build/reports/jacoco-html-YYYY-MM-DD_HH-mm-ss/
```

**Coverage configuration:**
- Excludes: Config classes, entities, DTOs, and application main class
- Formats: HTML (interactive) and XML (for CI/CD integration)
- Historical tracking: Timestamped reports saved for every test run

### Managing Services

**Start services:**
- Linux/Mac: `./scripts/start-services.sh`
- Windows: `.\scripts\start-services.ps1`

**Stop services:**
- Linux/Mac: `./scripts/stop-services.sh`
- Windows: `.\scripts\stop-services.ps1`

## Running Tests

### Unit Tests
```bash
./gradlew test
```

### Component Tests (Integration Tests)
Component tests use Spring Boot Test with TestContainers to test major features in a real containerized environment:

```bash
# Run all tests including component tests
./gradlew test

# Run only component tests
./gradlew test --tests "com.bugbounty.component.*"

# Run specific component test
./gradlew test --tests "com.bugbounty.component.BountyPollingComponentTest"
```

**Component Test Coverage:**
- `BountyPollingComponentTest` - End-to-end polling, filtering, and queueing
- `TriageQueueComponentTest` - Redis queue operations with priority ordering
- `BountyFilteringComponentTest` - LLM-based filtering with mocked ChatClient
- `RepositoryServiceComponentTest` - Git operations and repository management
- `ApiClientComponentTest` - HTTP client integration with MockWebServer

**Note:** Component tests require Docker to be running for TestContainers.

## Command Reference

### Using Mise (Recommended)

If you have [mise](https://mise.jdx.dev/) installed:

```bash
mise install          # Install tools and setup
mise tasks            # View all available commands
mise run setup        # Run setup
mise run test         # Run tests (generates coverage automatically)
mise run coverage      # Generate coverage report
mise run coverage-open # Open coverage report in browser
mise run run          # Run application
```

See `MISE_SETUP.md` for detailed mise setup instructions.

### Direct Commands

See `COMMANDS.md` for a complete catalog of all available commands, scripts, and workflows.

## Building

```bash
# Build without tests
./gradlew build -x test

# Build with tests
./gradlew build

# Create executable JAR
./gradlew bootJar
```

## Development Status

### ✅ Completed
- Project structure and configuration
- Domain models (Bounty, Repository) with tests
- BountyPollingService with tests
- RepositoryService with tests
- JPA entities and repositories
- API client implementations (Algora, Polar)
- Triage queue service with Redis
- LLM-based bounty filtering service
- **Component tests with Spring Boot Test + TestContainers**

### 🚧 In Progress
- API client implementations (Algora, Polar)
- IssueTriageService with LLM integration

### 📋 Planned
- PR generation and submission
- Scheduled polling
- Notification system
- Monitoring and metrics

## License

MIT


