# Quick Start Guide

Get up and running with Bug Bounty Finder in minutes.

## Option 1: Using Mise (Recommended)

If you have [mise](https://mise.jdx.dev/) installed:

```bash
# 1. Install tools and setup
mise install

# 2. Run setup
mise run setup

# 3. Run tests
mise run test

# 4. Start application
mise run run
```

See `MISE_SETUP.md` for mise installation and setup.

## Option 2: Manual Setup

### Windows

```powershell
# 1. Run setup script
.\scripts\setup.ps1

# 2. Run tests
.\gradlew.bat test

# 3. Start application
.\gradlew.bat bootRun
```

### Linux/Mac

```bash
# 1. Run setup script
./scripts/setup.sh

# 2. Run tests
./gradlew test

# 3. Start application
./gradlew bootRun
```

## Prerequisites

- **Java 21+** (or use mise to install)
- **Docker & Docker Compose** (for PostgreSQL and Redis)
- **Gradle 8.5+** (or use Gradle wrapper)
- **Ollama** (optional, for LLM features)

## What Gets Set Up

1. ✅ Docker services (PostgreSQL, Redis)
2. ✅ Environment configuration (`.env` file)
3. ✅ Gradle wrapper
4. ✅ Project directories
5. ✅ Ollama model (if Ollama is installed)

## Next Steps

- Read `COMMANDS.md` for all available commands
- Read `docs/testing/COMPONENT_TESTS.md` for testing details
- Check `README.md` for project overview

## Troubleshooting

### Java Version Issues

If you have Java 17 but need Java 21:
- Use mise: `mise install` (installs Java 21)
- Or install Java 21 manually

### Docker Not Running

```bash
# Check Docker status
docker ps

# Start Docker Desktop (Windows/Mac)
# Or start Docker service (Linux)
```

### Port Conflicts

If ports 5432 or 6379 are in use:
- Stop conflicting services
- Or modify `docker-compose.yml` to use different ports

### Gradle Wrapper Missing

```bash
# Initialize wrapper
gradle wrapper --gradle-version 8.5

# Or use setup script which does this automatically
```

## Getting Help

- **Commands**: See `COMMANDS.md`
- **Testing**: See `docs/testing/COMPONENT_TESTS.md`
- **Mise Setup**: See `MISE_SETUP.md`
- **Architecture**: See `docs/plan/TECHNICAL_IMPLEMENTATION_PLAN.md`

