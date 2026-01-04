# MCP Servers Quick Reference

Quick reference for MCP servers that enhance the Bug Bounty Finder development workflow.

## Priority Setup (Highest Impact)

### 1. Code Index MCP 🔍
**What**: Semantic codebase search  
**Impact**: 60-80% token savings on code queries  
**Setup**: `npm install -g @code-index/mcp-server`

### 2. Memory MCP 🧠
**What**: Long-term context storage  
**Impact**: 20-40% token savings, maintains project knowledge  
**Setup**: `npm install -g @metorial/mcp-index`

### 3. Spring Actuator MCP 📊
**What**: Real-time logs, health, metrics  
**Impact**: 70-90% token savings on log queries  
**Setup**: Custom server (see full guide)

## Build Tools

### Maven Tools MCP 🛠️
**What**: Gradle/Maven dependency analysis  
**Impact**: Instant dependency insights  
**Setup**: `npm install -g @maven-tools/mcp-server`

### Spring Initializr MCP 🚀
**What**: Generate Spring Boot projects  
**Impact**: Faster project/module creation  
**Setup**: `npm install -g @antigravity/spring-initializr-mcp`

## Quick Config

Add to Cursor MCP settings:

```json
{
  "mcpServers": {
    "code-index": {
      "command": "npx",
      "args": ["-y", "@code-index/mcp-server"],
      "env": { "CODE_INDEX_PATH": "./src" }
    },
    "memory": {
      "command": "npx",
      "args": ["-y", "@metorial/mcp-index"],
      "env": { "MEMORY_STORAGE_PATH": "./.mcp-memory" }
    }
  }
}
```

## Full Documentation

See [MCP_SERVERS_SETUP.md](./MCP_SERVERS_SETUP.md) for complete setup instructions.

