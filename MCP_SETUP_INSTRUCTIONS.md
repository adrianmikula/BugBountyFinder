# Quick MCP Setup Instructions

## ✅ Already Installed

The following MCP server packages are already installed globally:
- ✅ `@aakarsh-sasi/memory-bank-mcp`
- ✅ `@modelcontextprotocol/server-filesystem`

## 🚀 Quick Setup in Cursor

### 1. Open Cursor Settings
- Press `Ctrl+,` (Windows) or `Cmd+,` (Mac)
- Go to **Features** → **MCP**

### 2. Add Memory Bank MCP

Click **"+ Add New MCP Server"** and configure:

- **Name:** `memory-bank`
- **Command:** `npx`
- **Arguments:** (add each on a new line)
  ```
  -y
  @aakarsh-sasi/memory-bank-mcp
  --mode
  code
  --path
  .
  --folder
  .memory-bank
  ```

### 3. Add Filesystem MCP

Click **"+ Add New MCP Server"** again:

- **Name:** `filesystem`
- **Command:** `npx`
- **Arguments:**
  ```
  -y
  @modelcontextprotocol/server-filesystem
  .
  ```

### 4. Enable & Restart

1. Toggle both servers **ON**
2. **Restart Cursor completely**
3. Test by asking: "What tools do you have available?"

## 📖 Full Guide

See [docs/setup/MCP_CURSOR_SETUP.md](docs/setup/MCP_CURSOR_SETUP.md) for detailed instructions and troubleshooting.

## 🧪 Quick Test

After restarting, try:
- "Remember that this project uses TDD approach"
- "Show me the contents of build.gradle.kts"

