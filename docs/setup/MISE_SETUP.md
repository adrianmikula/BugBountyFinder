# Mise-en-Place Setup

[mise](https://mise.jdx.dev/) (formerly rtx) is a tool version manager that helps manage development environments and project tooling.

## Installation

### Windows

```powershell
# Using winget
winget install jdx.mise

# Using Scoop
scoop bucket add mise https://github.com/jdx/mise.git
scoop install mise

# Using Chocolatey
choco install mise
```

### Linux/Mac

```bash
curl https://mise.run | sh
```

Or using Homebrew (Mac):
```bash
brew install mise
```

## Setup

After installing mise, navigate to the project directory:

```bash
cd BugBountyFinder
mise install
```

This will:
- Install Java 21
- Install Gradle 8.5
- Set up environment variables
- Make tools available in your shell

## Usage

### Activate Environment

Mise automatically activates when you enter the project directory. If not, run:

```bash
mise activate
```

### Available Tasks

View all available tasks:

```bash
mise tasks
```

Run a task:

```bash
# Setup project
mise run setup

# Start services
mise run start-services

# Run tests
mise run test

# Run application
mise run run

# Build project
mise run build
```

### Direct Tool Access

Once mise is activated, tools are available directly:

```bash
java -version    # Uses Java 21 from mise
gradle --version # Uses Gradle 8.5 from mise
```

### Environment Variables

Mise automatically sets environment variables defined in `.mise.toml`:

```bash
echo $DB_USERNAME    # postgres
echo $OLLAMA_BASE_URL # http://localhost:11434
```

## Essential Tasks

These are the high-value commands you'll use daily. For complete reference, see `COMMANDS.md`.

| Task | Description |
|------|-------------|
| `setup` | Full project setup (first time) |
| `start-services` | Start Docker services |
| `stop-services` | Stop Docker services |
| `build` | Build project (no tests) |
| `build-all` | Build project with tests |
| `test` | Run all tests |
| `test-unit` | Run unit tests only |
| `test-component` | Run component tests only |
| `run` | Run the application |
| `clean` | Clean build artifacts |
| `docker-logs` | View Docker logs (follow) |
| `docker-ps` | Check Docker services status |
| `db-connect` | Connect to PostgreSQL |
| `redis-connect` | Connect to Redis |
| `health` | Check app health |

**View all tasks:**
```bash
mise tasks
```

## Integration with IDE

### VS Code

Install the "mise" extension for VS Code to automatically activate mise when opening the project.

### IntelliJ IDEA

Mise tools are available in the terminal. Configure IntelliJ to use mise-managed tools:

1. File → Settings → Build, Execution, Deployment → Build Tools → Gradle
2. Set Gradle JVM to the mise-managed Java version
3. Use Gradle wrapper (gradlew) for builds

## Benefits

1. **Consistent Versions**: Everyone uses the same tool versions
2. **Easy Setup**: New developers just run `mise install`
3. **Task Automation**: Common commands are cataloged and easy to run
4. **Environment Management**: Environment variables are managed centrally
5. **No Global Installation**: Tools are project-specific

## Troubleshooting

### Mise not activating

```bash
# Add to your shell profile (.bashrc, .zshrc, etc.)
eval "$(mise activate bash)"  # for bash
eval "$(mise activate zsh)"   # for zsh
```

### Tools not found

```bash
# Reinstall tools
mise install

# Check tool versions
mise ls
```

### Tasks not working

```bash
# Verify mise is activated
mise which java
mise which gradle

# Check task definitions
mise tasks
```

## Alternative: Without Mise

If you prefer not to use mise, you can:

1. Install Java 21 and Gradle 8.5 manually
2. Use the scripts in `scripts/` directory
3. Reference `COMMANDS.md` for all available commands
4. Set environment variables manually or use `.env` file

All functionality is available without mise - it just makes things easier!

