# Bug Bounty Finder - Architecture Documentation

## Overview

Bug Bounty Finder is an automated bug bounty and PR bounty hunting system built with Spring Boot 3.x. The system automatically discovers, evaluates, and processes bounties from multiple platforms while monitoring for security vulnerabilities through CVE tracking.

## Architectural Patterns

### 1. **Reactive Programming**
- Uses **Project Reactor** (Flux/Mono) for asynchronous, non-blocking operations
- Enables high concurrency with minimal resource overhead
- Applied to: API polling, webhook processing, CVE monitoring

### 2. **Domain-Driven Design (DDD)**
- Clear separation between domain models and persistence entities
- Domain objects (`Bounty`, `CVE`, `Repository`) are independent of JPA
- Mappers handle conversion between domain and entity layers

### 3. **Service Layer Pattern**
- Business logic encapsulated in service classes
- Services orchestrate interactions between repositories, external APIs, and infrastructure
- Clear boundaries: API clients, domain services, infrastructure services

### 4. **Circuit Breaker & Rate Limiting**
- **Resilience4j** provides fault tolerance patterns
- Circuit breakers protect against cascading failures from external APIs
- Rate limiters ensure compliance with API constraints (Algora, Polar, NVD)

### 5. **Event-Driven Architecture**
- Webhook-based real-time notifications (GitHub push events, CVE notifications)
- Polling-based discovery (scheduled bounty polling, CVE monitoring)
- Queue-based processing (Redis priority queue for bounty triage)

### 6. **Virtual Threads (Project Loom)**
- Java 21 virtual threads for high-concurrency I/O operations
- Enables efficient handling of thousands of concurrent operations
- Particularly effective for repository cloning and file operations

## System Components

### Core Domains

1. **Bounty Domain** (`com.bugbounty.bounty`)
   - Manages bug bounty discovery, filtering, and triage
   - Integrates with Algora and Polar.sh platforms
   - LLM-powered filtering for bounty evaluation

2. **CVE Domain** (`com.bugbounty.cve`)
   - Monitors NVD for new security vulnerabilities
   - Triggers repository scans when relevant CVEs are detected
   - Language-aware vulnerability matching

3. **Repository Domain** (`com.bugbounty.repository`)
   - Manages GitHub repository cloning and updates
   - Provides Git operations abstraction (JGit)
   - Supports file reading and directory traversal

4. **Webhook Domain** (`com.bugbounty.webhook`)
   - Handles incoming GitHub webhook events
   - Validates webhook signatures for security
   - Processes push events to trigger repository updates

## Technology Stack

- **Framework**: Spring Boot 3.2+ with Java 21
- **Database**: PostgreSQL (persistent state) + Redis (queues/cache)
- **LLM**: Ollama (local inference) via Spring AI
- **Git Operations**: JGit
- **Reactive**: Spring WebFlux + Project Reactor
- **Resilience**: Resilience4j (circuit breakers, rate limiters)
- **Schema Management**: Liquibase
- **Monitoring**: Spring Boot Actuator

## Key Integrations

- **Algora API**: Bounty discovery platform
- **Polar.sh API**: PR bounty platform
- **NVD API**: CVE vulnerability database
- **GitHub API**: Repository metadata and webhooks
- **Ollama**: Local LLM inference for intelligent filtering

