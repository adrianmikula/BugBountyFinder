# Bug Bounty Finder

Automated bug bounty and PR bounty hunting system built with Spring Boot 3.x.

## Features

- **Bounty Polling**: Automatically polls Algora and Polar.sh for new bounties
- **Repository Management**: Clones and manages GitHub repositories
- **LLM Integration**: Uses Ollama for local LLM inference (cost-effective)
- **Virtual Threads**: High concurrency with Project Loom
- **TDD Approach**: Comprehensive unit tests for all core components

## Tech Stack

- **Spring Boot 3.2+** with Java 21
- **Spring AI** for LLM orchestration
- **PostgreSQL** for state management
- **Redis** for caching and queues
- **JGit** for Git operations
- **Ollama** for local LLM inference
- **Resilience4j** for circuit breakers and rate limiting

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

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- Ollama (for local LLM)

### Setup

1. **Install Ollama and pull a model:**
   ```bash
   curl -fsSL https://ollama.ai/install.sh | sh
   ollama pull llama3.2:3b
   ```

2. **Start PostgreSQL and Redis:**
   ```bash
   docker-compose up -d postgres redis
   ```

3. **Configure application.yml** with your database credentials

4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Running Tests

```bash
./mvnw test
```

## Development Status

### ✅ Completed
- Project structure and configuration
- Domain models (Bounty, Repository) with tests
- BountyPollingService with tests
- RepositoryService with tests
- JPA entities and repositories

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

