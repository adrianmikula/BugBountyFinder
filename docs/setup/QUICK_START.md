# Quick Start Guide

Get up and running with Bug Bounty Finder in 5 minutes!

## Prerequisites

Ensure you have:
- ✅ **Java 21+** (or use mise to install automatically)
- ✅ **Docker & Docker Compose** (for PostgreSQL and Redis)
- ✅ **Gradle 8.5+** (or use Gradle wrapper - installed automatically)
- ✅ **Ollama** (optional, for LLM features - can be installed later)

## Option 1: Automated Setup (Recommended)

The setup script will install mise if needed, then use mise for everything:

**Windows (PowerShell):**
```powershell
.\scripts\setup.ps1
```

**Linux/Mac:**
```bash
./scripts/setup.sh
```

The script will:
1. ✅ Install mise-en-place if not present
2. ✅ Use mise to install tools (Java 21, Gradle 8.5)
3. ✅ Start Docker services (PostgreSQL, Redis)
4. ✅ Create configuration files and directories
5. ✅ Build the project

After setup, use mise commands for daily development:
```bash
mise run test            # Run tests
mise run run             # Run backend application
mise run frontend-dev    # Start frontend development server
mise run frontend-build  # Build frontend for production
mise tasks               # View all available commands
```

See [Mise Setup Guide](MISE_SETUP.md) for detailed mise information.

## Option 2: Manual Setup

### Step 1: Clone and Build

```bash
# Clone the repository (if not already done)
git clone <your-repo-url>
cd BugBountyFinder

# Initialize Gradle wrapper (if needed)
gradle wrapper --gradle-version 8.5

# Build the project
./gradlew build
```

### Step 2: Start Services

```bash
# Start PostgreSQL and Redis
docker compose up -d

# Verify services are running
docker compose ps
```

### Step 3: Configure API Keys

```bash
# Copy example environment file
cp .env.example .env

# Generate GitHub webhook secret
openssl rand -hex 32
# (Copy the output)

# Edit .env file and add:
GITHUB_WEBHOOK_SECRET=<paste-generated-secret>
```

**Minimum required**: Just the webhook secret. For better performance, also add:
- `GITHUB_API_TOKEN` - Get from https://github.com/settings/tokens (recommended for higher rate limits)
- `NVD_API_KEY` - Get from https://nvd.nist.gov/developers/request-an-api-key (recommended for better CVE monitoring)

See [API Keys Setup Guide](API_KEYS_SETUP.md) for detailed instructions.

### Step 4: Install Ollama Model

```bash
# Install and start Ollama (if not already done)
# Linux/Mac:
curl -fsSL https://ollama.ai/install.sh | sh

# Windows: Download from https://ollama.ai

# Pull the recommended model
ollama pull deepseek-coder:6.7b
```

**Note**: Ollama is optional but required for LLM-based bounty filtering and code analysis features.

### Step 5: Run the Application

**Using mise (if installed):**
```bash
mise run run
```

**Using Gradle directly:**
```bash
# Windows
.\gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Step 6: Verify It's Working

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Check repositories (should be empty initially)
curl http://localhost:8080/api/repositories
```

### Step 7: Add a Repository

```bash
# Add VS Code repository (high activity, good for testing)
curl -X POST http://localhost:8080/api/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/microsoft/vscode",
    "language": "TypeScript",
    "defaultBranch": "main"
  }'

# Verify it was added
curl http://localhost:8080/api/repositories
```

For more repository suggestions, see [Recommended Repositories](../research/recommended-repositories.md).

## What Gets Set Up

1. ✅ Docker services (PostgreSQL, Redis)
2. ✅ Environment configuration (`.env` file)
3. ✅ Gradle wrapper
4. ✅ Project directories (`logs/`, `repos/`)
5. ✅ Ollama model (if Ollama is installed)

## Optional: Enhance AI Workflow with MCP Servers

To significantly improve your AI coding experience and save tokens:

1. **Install Node.js** (if not already installed)
2. **Install priority MCP servers**:
   ```bash
   npm install -g @code-index/mcp-server
   npm install -g @metorial/mcp-index
   ```
3. **Configure Cursor** - See [MCP Servers Setup Guide](MCP_SERVERS_SETUP.md)

**Benefits**:
- 60-80% token savings on code queries (Code Index)
- 20-40% token savings through context reuse (Memory)
- 70-90% token savings on log queries (Spring Actuator)

## Next Steps

1. **Configure GitHub Webhooks**: See [GitHub Webhook Setup](GITHUB_WEBHOOK_SETUP.md)
2. **Add More Repositories**: See [Recommended Repositories](../research/recommended-repositories.md)
3. **Set Up MCP Servers**: See [MCP Servers Setup](MCP_SERVERS_SETUP.md) for enhanced AI workflow
4. **Read Architecture Docs**: See [Architecture Documentation](../architecture/README.md)
5. **Review Testing Guide**: See [Component Tests](../testing/COMPONENT_TESTS.md)

## Troubleshooting

### Java Version Issues

If you have Java 17 but need Java 21:
- **Using mise** (recommended): `mise install` (installs Java 21 automatically)
- **Manual**: Install Java 21 from https://adoptium.net/

### Docker Not Running

```bash
# Check Docker status
docker ps

# Start Docker Desktop (Windows/Mac)
# Or start Docker service (Linux)
sudo systemctl start docker
```

### Port Conflicts

If ports 5432 (PostgreSQL) or 6379 (Redis) are in use:
- Stop conflicting services
- Or modify `docker-compose.yml` to use different ports

### Gradle Wrapper Missing

```bash
# Initialize wrapper
gradle wrapper --gradle-version 8.5

# Or use setup script which does this automatically
```

### Application Won't Start

- **Check if services are running**: `docker compose ps`
- **Check if Ollama is running**: `curl http://localhost:11434/api/tags`
- **Check logs**: Look for errors in console output or `./logs/application.log`
- **Verify database connection**: Ensure PostgreSQL is accessible on port 5432
- **Verify Redis connection**: Ensure Redis is accessible on port 6379

### API Rate Limits

- **GitHub**: Add `GITHUB_API_TOKEN` to `.env` for higher rate limits (5,000/hour vs 60/hour)
- **NVD**: Add `NVD_API_KEY` to `.env` for better CVE monitoring (50 requests/30s vs 5 requests/30s)

### Ollama Connection Failed

- **Ensure Ollama is running**: `ollama serve`
- **Check `OLLAMA_BASE_URL`** in `.env` (default: `http://localhost:11434`)
- **Verify model is installed**: `ollama list` (should show `deepseek-coder:6.7b`)
- **Pull model if missing**: `ollama pull deepseek-coder:6.7b`

### Database Connection Issues

- **Check PostgreSQL is running**: `docker compose ps`
- **Verify credentials**: Check `.env` file for `DB_USERNAME` and `DB_PASSWORD`
- **Check logs**: Look for connection errors in application logs
- **Reset database** (if needed): `docker compose down -v && docker compose up -d`

## Full Documentation

- **[API Keys Setup](API_KEYS_SETUP.md)** - Detailed API key configuration
- **[GitHub Webhook Setup](GITHUB_WEBHOOK_SETUP.md)** - Configure webhooks for real-time notifications
- **[MCP Servers Setup](MCP_SERVERS_SETUP.md)** - Enhance AI workflow with MCP servers
- **[Mise Setup](MISE_SETUP.md)** - Detailed mise-en-place configuration
- **[Recommended Repositories](../research/recommended-repositories.md)** - Good repositories for testing
- **[Architecture Documentation](../architecture/README.md)** - System architecture and design
- **[Component Tests](../testing/COMPONENT_TESTS.md)** - Testing guide and examples
- **[Coding Standards](../standards/README.md)** - Project coding standards and best practices

## Getting Help

- **Setup Issues**: Check this guide and [Mise Setup](MISE_SETUP.md)
- **API Configuration**: See [API Keys Setup](API_KEYS_SETUP.md)
- **Testing**: See [Component Tests](../testing/COMPONENT_TESTS.md)
- **Architecture**: See [Architecture Documentation](../architecture/README.md)
- **Project Overview**: Check [README.md](../../README.md)

---

**Ready to start?** Run the setup script and you'll be up and running in minutes!
