# Bug Bounty Finder

Intelligent bounty triage and assistance system built with Spring Boot 3.x.

**Reality Check**: Most bounties require POCs, multiple iterations, and deep understanding. This system focuses on **brutal filtering** to find the 2-3% of truly simple bugs, then provides **AI assistance** (not full automation) to help you fix them efficiently.

## Features

- **Bounty Discovery**: Polls Algora, Polar.sh, and GitPay.me platforms to discover bounties (requires login/API keys)
- **Brutal Triage/Filtering**: Aggressive filtering to find truly simple bugs:
  - **Language Filter**: Only processes bounties in languages you know (configurable)
  - **Bounty Amount Filter**: Rejects high-value bounties (>$200) that usually indicate complexity
  - **Complexity Filter**: Brutal LLM-based filtering that rejects 95%+ of bounties
  - **Reality Check**: Only accepts trivial fixes (single file, no POC, no iterations)
- **Context Gathering**: Pre-analyzes issues and gathers relevant code snippets
- **Fix Assistance**: Generates draft fixes as starting points (requires human refinement)
- **Human-in-the-Loop**: Designed to assist developers, not replace them
- **GitHub Push Webhooks**: Real-time notifications when commits are pushed to repositories
- **Repository Management**: Clones and manages GitHub repositories via REST API
- **Repository API**: Add, list, and manage repositories through `/api/repositories` endpoints
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
- **Common Gotchas**: [Common pitfalls and problems](docs/standards/common-gotchas.md) to avoid - includes Spring Boot test configuration, reactive programming pitfalls, and more

### MCP Servers Setup

Enhance your AI coding workflow with Model Context Protocol servers. See the [MCP Servers Setup Guide](docs/setup/MCP_SERVERS_SETUP.md) for:

- **Code Indexing**: Fast semantic codebase search (60-80% token savings)
- **Memory Storage**: Long-term context across sessions (20-40% token savings)
- **Spring Boot Monitoring**: Real-time logs, health, and metrics (70-90% token savings)
- **Build Tool Integration**: Gradle and npm dependency analysis
- **Quick Reference**: [MCP Quick Reference](docs/setup/MCP_QUICK_REFERENCE.md)

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

### API Keys Configuration

Before running the application, you need to configure API keys for external services:

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Set up API keys** - See [API Keys Setup Guide](docs/setup/API_KEYS_SETUP.md) for detailed instructions:
   - **Algora**: API Key (Required - sign up and log in at algora.io to get API key)
   - **Polar.sh**: API Key (Required - sign up and log in at polar.sh to get API key)
   - **GitHub**: Personal Access Token (recommended) and Webhook Secret (required for webhooks)
   - **NVD**: API Key (optional but recommended for better rate limits)
   - **Ollama**: No API key needed (local service)

**Important**: Bounties are discovered by polling Algora and Polar.sh platforms (not by scanning GitHub). You must create accounts and get API keys from these platforms.

3. **Edit `.env` file** with your actual API keys

**Quick Start (Minimum Required):**
```bash
# Generate GitHub webhook secret
openssl rand -hex 32

# Add to .env file:
GITHUB_WEBHOOK_SECRET=your-generated-secret-here
```

For full setup instructions, see [API Keys Setup Guide](docs/setup/API_KEYS_SETUP.md).

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

## Repository Management

### Adding Repositories

You can add repositories to monitor using the REST API:

```bash
# Add a repository
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/microsoft/vscode",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'
```

### Recommended Repositories

For testing, we recommend repositories with high commit activity and active bug bounty programs. See [Recommended Repositories](docs/recommended-repositories.md) for a curated list including:

- **microsoft/vscode** - Very high activity (20-50+ commits/day), Microsoft bug bounty
- **facebook/react** - High activity, Meta bug bounty
- **vercel/next.js** - High activity, modern web framework
- **kubernetes/kubernetes** - Very high activity, CNCF security program

### API Endpoints

- `POST /api/repositories` - Add a new repository
- `GET /api/repositories` - List all repositories
- `GET /api/repositories/{id}` - Get repository by ID
- `GET /api/repositories/by-url?url={url}` - Get repository by URL
- `DELETE /api/repositories?url={url}` - Delete a repository

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
- BountyPollingService with tests (Algora, Polar, GitHub)
- **GitHub Issue Scanning** - Scans repositories for issues with dollar amounts
- **GitHub Issue Webhooks** - Real-time bounty detection when issues are created
- RepositoryService with tests
- **Repository Management API** - Add, list, and manage repositories via REST endpoints
- JPA entities and repositories
- API client implementations (Algora, Polar, GitHub)
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


