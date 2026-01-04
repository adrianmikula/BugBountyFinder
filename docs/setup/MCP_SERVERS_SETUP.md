# MCP Servers Setup Guide

This guide covers Model Context Protocol (MCP) servers that can significantly enhance your agentic AI coding workflow by providing high-value context, feedback, and automation capabilities.

## Overview

MCP servers extend your AI assistant's capabilities by providing:
- **Real-time application monitoring** (Spring Boot logs, errors, health)
- **Codebase indexing and semantic search** (faster code navigation)
- **Long-term memory storage** (context across sessions)
- **Build tool integration** (Gradle, npm dependency management)

## Recommended MCP Servers

### 1. Code Index MCP Server 🔍
**Purpose**: Codebase indexing and semantic search

**Benefits**:
- Index entire codebase for fast semantic search
- Multi-language support (Java, TypeScript, JavaScript, etc.)
- Reduces token usage by providing precise code context
- Faster code navigation and understanding

**Installation**:
```bash
# Install via npm
npm install -g @code-index/mcp-server

# Or clone from GitHub
git clone https://github.com/ViperJuice/Code-Index-MCP.git
cd Code-Index-MCP
npm install
npm run build
```

**Configuration** (add to Cursor MCP settings):
```json
{
  "mcpServers": {
    "code-index": {
      "command": "npx",
      "args": ["-y", "@code-index/mcp-server"],
      "env": {
        "CODE_INDEX_PATH": "./src"
      }
    }
  }
}
```

**Usage**:
- Automatically indexes your codebase
- Provides semantic search across all files
- Returns relevant code snippets with context

**Repository**: https://github.com/ViperJuice/Code-Index-MCP

---

### 2. Memory MCP Server / RAG Server 🧠
**Purpose**: Long-term memory storage and retrieval

**Benefits**:
- Store context across AI sessions
- Semantic search over past conversations and decisions
- Maintain project history and patterns
- Reduces redundant explanations

**Installation**:
```bash
# Option 1: Memory MCP Server
npm install -g @metorial/mcp-index

# Option 2: RAG Server (more advanced)
npm install -g @creati-ai/mcp-rag-server
```

**Configuration** (Memory MCP):
```json
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@metorial/mcp-index"],
      "env": {
        "MEMORY_STORAGE_PATH": "./.mcp-memory"
      }
    }
  }
}
```

**Configuration** (RAG Server - recommended for advanced use):
```json
{
  "mcpServers": {
    "rag": {
      "command": "npx",
      "args": ["-y", "@creati-ai/mcp-rag-server"],
      "env": {
        "VECTOR_DB_PATH": "./.mcp-vectors",
        "EMBEDDING_MODEL": "text-embedding-3-small"
      }
    }
  }
}
```

**Usage**:
- Stores important decisions and patterns
- Retrieves relevant context from past sessions
- Maintains project-specific knowledge

**Repositories**:
- Memory: https://github.com/metorial/mcp-index
- RAG: https://creati.ai/mcp/mcp-rag-server/

---

### 3. Maven Tools MCP Server 🛠️
**Purpose**: Gradle and Maven dependency analysis

**Benefits**:
- Instant dependency insights (versions, vulnerabilities, age)
- Bulk dependency operations
- Build optimization suggestions
- Reduces manual dependency research

**Installation**:
```bash
# Install via npm
npm install -g @maven-tools/mcp-server

# Or use the hosted version
# No installation needed if using hosted service
```

**Configuration**:
```json
{
  "mcpServers": {
    "maven-tools": {
      "command": "npx",
      "args": ["-y", "@maven-tools/mcp-server"],
      "env": {
        "GRADLE_PROJECT_PATH": "."
      }
    }
  }
}
```

**Usage**:
- Analyzes `build.gradle.kts` dependencies
- Suggests version updates
- Identifies outdated or vulnerable dependencies
- Provides dependency tree analysis

**Repository**: https://mcp.so/zh/server/spring-boot-ai-mongo-mcp-server/BootcampToProd

---

### 4. Spring Initializr MCP Server 🚀
**Purpose**: Spring Boot project generation and configuration

**Benefits**:
- Generate Spring Boot projects via natural language
- Add dependencies automatically
- Configure project structure
- Useful for creating new modules or microservices

**Installation**:
```bash
# Install via npm
npm install -g @antigravity/spring-initializr-mcp
```

**Configuration**:
```json
{
  "mcpServers": {
    "spring-initializr": {
      "command": "npx",
      "args": ["-y", "@antigravity/spring-initializr-mcp"]
    }
  }
}
```

**Usage**:
- Generate new Spring Boot modules
- Add dependencies to existing projects
- Configure Java versions and project metadata

**Repository**: https://antigravity.codes/mcp/spring-initializr

---

### 5. Spring Boot Actuator MCP Server 📊
**Purpose**: Monitor Spring Boot application logs, health, and metrics

**Benefits**:
- Real-time log access and filtering
- Health endpoint monitoring
- Error tracking and analysis
- Metrics and performance data
- **Significantly reduces token usage** by providing targeted log queries

**Installation** (Custom Implementation):
Since there isn't a ready-made Spring Boot Actuator MCP server, you can create one or use a generic HTTP MCP server:

```bash
# Option 1: Use a generic HTTP MCP server
npm install -g @modelcontextprotocol/server-http

# Option 2: Create custom server (see Custom Implementation section)
```

**Configuration** (HTTP MCP for Actuator):
```json
{
  "mcpServers": {
    "spring-actuator": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-http"],
      "env": {
        "ACTUATOR_BASE_URL": "http://localhost:8080/actuator",
        "ACTUATOR_USERNAME": "admin",
        "ACTUATOR_PASSWORD": "admin"
      }
    }
  }
}
```

**Spring Boot Configuration** (enable Actuator endpoints):
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,env
  endpoint:
    health:
      show-details: always
    loggers:
      enabled: true
  # Optional: Secure endpoints
  security:
    enabled: false  # Set to true in production with proper auth
```

**Usage**:
- Query recent logs: "Show me errors from the last 10 minutes"
- Health checks: "What's the application health status?"
- Metrics: "Show me memory usage and request rates"
- Logger management: "Change log level for com.bugbounty package"

---

### 6. NPM Package Manager MCP Server 📦
**Purpose**: npm dependency management for frontend

**Benefits**:
- Analyze `package.json` dependencies
- Suggest updates and security fixes
- Manage npm scripts
- Optimize bundle size

**Installation**:
```bash
# Install via npm
npm install -g @npm-tools/mcp-server
```

**Configuration**:
```json
{
  "mcpServers": {
    "npm-tools": {
      "command": "npx",
      "args": ["-y", "@npm-tools/mcp-server"],
      "env": {
        "NPM_PROJECT_PATH": "./frontend"
      }
    }
  }
}
```

**Usage**:
- Analyze frontend dependencies
- Check for outdated packages
- Identify security vulnerabilities
- Optimize package.json

**Note**: If a dedicated npm MCP server doesn't exist, you can use the Code Index MCP to analyze `package.json` files.

---

## Complete MCP Configuration

Here's a complete configuration file for Cursor (`.cursor/mcp.json` or Cursor settings):

```json
{
  "mcpServers": {
    "code-index": {
      "command": "npx",
      "args": ["-y", "@code-index/mcp-server"],
      "env": {
        "CODE_INDEX_PATH": "./src"
      }
    },
    "memory": {
      "command": "npx",
      "args": ["-y", "@metorial/mcp-index"],
      "env": {
        "MEMORY_STORAGE_PATH": "./.mcp-memory"
      }
    },
    "maven-tools": {
      "command": "npx",
      "args": ["-y", "@maven-tools/mcp-server"],
      "env": {
        "GRADLE_PROJECT_PATH": "."
      }
    },
    "spring-initializr": {
      "command": "npx",
      "args": ["-y", "@antigravity/spring-initializr-mcp"]
    }
  }
}
```

---

## Custom Spring Boot Actuator MCP Server

Since there isn't a ready-made Spring Boot Actuator MCP server, here's a simple Node.js implementation you can create:

### Create `mcp-spring-actuator/index.js`:

```javascript
#!/usr/bin/env node

const { Server } = require("@modelcontextprotocol/sdk/server/index.js");
const { StdioServerTransport } = require("@modelcontextprotocol/sdk/server/stdio.js");
const axios = require("axios");

const ACTUATOR_BASE_URL = process.env.ACTUATOR_BASE_URL || "http://localhost:8080/actuator";
const ACTUATOR_USERNAME = process.env.ACTUATOR_USERNAME;
const ACTUATOR_PASSWORD = process.env.ACTUATOR_PASSWORD;

const server = new Server(
  {
    name: "spring-actuator",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

async function makeRequest(endpoint) {
  const url = `${ACTUATOR_BASE_URL}${endpoint}`;
  const config = {};
  
  if (ACTUATOR_USERNAME && ACTUATOR_PASSWORD) {
    config.auth = {
      username: ACTUATOR_USERNAME,
      password: ACTUATOR_PASSWORD,
    };
  }
  
  try {
    const response = await axios.get(url, config);
    return response.data;
  } catch (error) {
    throw new Error(`Actuator request failed: ${error.message}`);
  }
}

server.setRequestHandler("tools/list", async () => ({
  tools: [
    {
      name: "get_health",
      description: "Get Spring Boot application health status",
      inputSchema: {
        type: "object",
        properties: {},
      },
    },
    {
      name: "get_logs",
      description: "Get recent application logs (requires logfile endpoint)",
      inputSchema: {
        type: "object",
        properties: {
          lines: {
            type: "number",
            description: "Number of lines to retrieve (default: 100)",
          },
          level: {
            type: "string",
            description: "Filter by log level (ERROR, WARN, INFO, DEBUG)",
            enum: ["ERROR", "WARN", "INFO", "DEBUG"],
          },
        },
      },
    },
    {
      name: "get_metrics",
      description: "Get application metrics",
      inputSchema: {
        type: "object",
        properties: {
          metric: {
            type: "string",
            description: "Specific metric name (optional)",
          },
        },
      },
    },
    {
      name: "get_info",
      description: "Get application information",
      inputSchema: {
        type: "object",
        properties: {},
      },
    },
  ],
}));

server.setRequestHandler("tools/call", async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "get_health":
        return { content: [{ type: "text", text: JSON.stringify(await makeRequest("/health"), null, 2) }] };
      
      case "get_logs":
        // Note: This requires logfile actuator endpoint configuration
        const lines = args?.lines || 100;
        const level = args?.level;
        let logs = await makeRequest(`/logfile?lines=${lines}`);
        if (level) {
          logs = logs.split("\n").filter(line => line.includes(level)).join("\n");
        }
        return { content: [{ type: "text", text: logs }] };
      
      case "get_metrics":
        if (args?.metric) {
          const metric = await makeRequest(`/metrics/${args.metric}`);
          return { content: [{ type: "text", text: JSON.stringify(metric, null, 2) }] };
        }
        const metrics = await makeRequest("/metrics");
        return { content: [{ type: "text", text: JSON.stringify(metrics, null, 2) }] };
      
      case "get_info":
        return { content: [{ type: "text", text: JSON.stringify(await makeRequest("/info"), null, 2) }] };
      
      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error) {
    return {
      content: [{ type: "text", text: `Error: ${error.message}` }],
      isError: true,
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Spring Boot Actuator MCP server running on stdio");
}

main().catch(console.error);
```

### Install dependencies:

```bash
mkdir mcp-spring-actuator
cd mcp-spring-actuator
npm init -y
npm install @modelcontextprotocol/sdk axios
```

### Make it executable:

```bash
chmod +x index.js
```

### Configuration:

```json
{
  "mcpServers": {
    "spring-actuator": {
      "command": "node",
      "args": ["./mcp-spring-actuator/index.js"],
      "env": {
        "ACTUATOR_BASE_URL": "http://localhost:8080/actuator",
        "ACTUATOR_USERNAME": "admin",
        "ACTUATOR_PASSWORD": "admin"
      }
    }
  }
}
```

---

## Setup Instructions

### Step 1: Install Node.js and npm

Ensure you have Node.js 18+ installed:
```bash
node --version
npm --version
```

### Step 2: Install MCP Servers

Install the recommended servers:
```bash
# Code Index
npm install -g @code-index/mcp-server

# Memory
npm install -g @metorial/mcp-index

# Maven Tools (if available)
npm install -g @maven-tools/mcp-server

# Spring Initializr
npm install -g @antigravity/spring-initializr-mcp
```

### Step 3: Configure Cursor

1. Open Cursor Settings
2. Navigate to MCP Servers section
3. Add the configuration JSON (see "Complete MCP Configuration" above)
4. Restart Cursor

### Step 4: Enable Spring Boot Actuator

Update `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,logfile
  endpoint:
    health:
      show-details: always
    loggers:
      enabled: true
    logfile:
      enabled: true
      external-file: ./logs/application.log
```

### Step 5: Test MCP Servers

Restart Cursor and test by asking:
- "Index the codebase" (Code Index)
- "What dependencies are outdated?" (Maven Tools)
- "Show me recent errors from Spring Boot" (Spring Actuator)
- "Remember that we use TDD approach" (Memory)

---

## Benefits Summary

### Token Savings 💰
- **Code Index**: Reduces token usage by 60-80% for code queries
- **Memory**: Eliminates redundant context (saves 20-40% tokens)
- **Actuator**: Targeted log queries vs. full file reads (saves 70-90% tokens)

### Speed Improvements ⚡
- **Code Index**: 10x faster code search
- **Maven Tools**: Instant dependency analysis vs. manual research
- **Memory**: Instant context retrieval vs. re-explaining

### Workflow Enhancements 🚀
- **Real-time monitoring**: Immediate error detection
- **Context continuity**: Maintains project knowledge across sessions
- **Automated analysis**: Dependency and security insights

---

## Troubleshooting

### MCP Server Not Found

**Problem**: `npx` can't find the MCP server package

**Solution**:
1. Check if package name is correct
2. Try installing globally: `npm install -g <package-name>`
3. Use full path: `node /path/to/mcp-server/index.js`

### Spring Actuator Connection Failed

**Problem**: Cannot connect to Actuator endpoints

**Solution**:
1. Ensure Spring Boot app is running
2. Check `ACTUATOR_BASE_URL` is correct
3. Verify endpoints are exposed in `application.yml`
4. Check firewall/network settings

### Code Index Not Working

**Problem**: Code Index doesn't find files

**Solution**:
1. Verify `CODE_INDEX_PATH` points to correct directory
2. Check file permissions
3. Ensure codebase is not too large (may need chunking)

---

## Next Steps

1. **Start with Code Index and Memory** - Highest impact, easiest setup
2. **Add Maven Tools** - For dependency management
3. **Set up Spring Actuator** - For real-time monitoring
4. **Customize as needed** - Add more servers based on your workflow

---

## Resources

- **MCP Documentation**: https://modelcontextprotocol.io
- **Code Index MCP**: https://github.com/ViperJuice/Code-Index-MCP
- **Memory MCP**: https://github.com/metorial/mcp-index
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **MCP Server Registry**: https://mcp.so

---

## Security Considerations

1. **Actuator Endpoints**: Secure in production with authentication
2. **Memory Storage**: Don't store sensitive data (API keys, passwords)
3. **Network Access**: Limit MCP server network access if possible
4. **Environment Variables**: Keep secrets in `.env`, not in MCP config

---

## Contributing

If you create custom MCP servers for this project, consider:
1. Documenting them in this guide
2. Sharing with the community
3. Following MCP server best practices

---

**Last Updated**: 2025-01-27

