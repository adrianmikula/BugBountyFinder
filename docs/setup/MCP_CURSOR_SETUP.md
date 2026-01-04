# MCP Servers Setup for Cursor

This guide will help you configure Code and Memory MCP servers in Cursor to enhance your AI coding workflow.

## ✅ Prerequisites Check

We've already installed the required packages:
- ✅ `@aakarsh-sasi/memory-bank-mcp` - Memory storage
- ✅ `@modelcontextprotocol/server-filesystem` - Filesystem access for codebase

## Step-by-Step Setup

### Step 1: Open Cursor Settings

1. Open Cursor
2. Press `Ctrl+,` (or `Cmd+,` on Mac) to open Settings
3. Navigate to **Features** → **MCP** (or search for "MCP" in settings)

### Step 2: Add Memory Bank MCP Server

1. Click **"+ Add New MCP Server"** or **"Add MCP Server"**
2. Fill in the following details:

   **Name:** `memory-bank`
   
   **Command:** `npx`
   
   **Arguments:** 
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
   
   **Environment Variables (optional):**
   - You can leave this empty, or add:
     - `MEMORY_BANK_PATH`: `.memory-bank` (default)

3. Click **Save** or **Add Server**
4. Toggle the server **ON** to enable it

### Step 3: Add Filesystem MCP Server

1. Click **"+ Add New MCP Server"** again
2. Fill in the following details:

   **Name:** `filesystem`
   
   **Command:** `npx`
   
   **Arguments:**
   ```
   -y
   @modelcontextprotocol/server-filesystem
   .
   ```
   
   **Note:** The `.` at the end sets the root directory to the current project

3. Click **Save** or **Add Server**
4. Toggle the server **ON** to enable it

### Step 4: Restart Cursor

**Important:** After adding MCP servers, you must restart Cursor for them to take effect.

1. Close Cursor completely
2. Reopen Cursor
3. Open your BugBountyFinder project

### Step 5: Verify MCP Servers Are Working

1. Open Cursor's chat (press `Ctrl+L` or `Cmd+L`)
2. Ask: **"What tools do you have available?"**
3. You should see responses mentioning:
   - Memory Bank MCP tools (like `initialize_memory_bank`, `track_progress`, etc.)
   - Filesystem MCP tools (like file reading, directory listing, etc.)

## Testing the MCP Servers

### Test Memory Bank MCP

Try these prompts in Cursor chat:

1. **Initialize Memory Bank:**
   ```
   Remember that this project uses TDD (Test-Driven Development) approach
   ```

2. **Track Progress:**
   ```
   Track that we're working on MCP server integration
   ```

3. **Ask About Memory:**
   ```
   What do you remember about this project's development approach?
   ```

### Test Filesystem MCP

Try these prompts:

1. **Read a file:**
   ```
   Show me the contents of build.gradle.kts
   ```

2. **List directory:**
   ```
   What files are in the src/main/java/com/bugbounty directory?
   ```

3. **Search for code:**
   ```
   Find all Java files that contain "Repository" in the name
   ```

## Configuration Reference

If you prefer to configure manually, here's the JSON configuration:

```json
{
  "mcpServers": {
    "memory-bank": {
      "command": "npx",
      "args": [
        "-y",
        "@aakarsh-sasi/memory-bank-mcp",
        "--mode",
        "code",
        "--path",
        ".",
        "--folder",
        ".memory-bank"
      ]
    },
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "."
      ]
    }
  }
}
```

## What These Servers Do

### Memory Bank MCP 🧠

**Purpose:** Long-term memory storage for your project

**Benefits:**
- Remembers project conventions and patterns
- Tracks development decisions and progress
- Maintains context across sessions
- Reduces redundant explanations (saves 20-40% tokens)

**Available Tools:**
- `initialize_memory_bank` - Set up memory storage
- `track_progress` - Log development progress
- `log_decision` - Record important decisions
- `query_memory` - Search stored memories

### Filesystem MCP 📁

**Purpose:** Enhanced filesystem access for codebase navigation

**Benefits:**
- Fast file reading and searching
- Directory navigation
- Better code context retrieval
- Reduces token usage by providing targeted file access

**Available Tools:**
- File reading operations
- Directory listing
- File search capabilities

## Troubleshooting

### MCP Servers Not Appearing

**Problem:** After restarting, MCP servers don't show up in available tools

**Solutions:**
1. Check that servers are toggled **ON** in Settings
2. Verify Node.js is installed: `node --version`
3. Check Cursor's output/logs for errors
4. Try removing and re-adding the servers

### "Command not found" Errors

**Problem:** Cursor can't find `npx`

**Solutions:**
1. Ensure Node.js is in your PATH
2. Try using full path to npx:
   - Windows: `C:\Program Files\nodejs\npx.cmd`
   - Or use: `npm.cmd` with args: `["exec", "-y", "@aakarsh-sasi/memory-bank-mcp", ...]`

### Memory Bank Not Working

**Problem:** Memory Bank doesn't remember things

**Solutions:**
1. Check that `.memory-bank` folder exists in project root
2. Verify the `--path` argument points to project root (`.`)
3. Try initializing explicitly: "Initialize the memory bank for this project"

### Filesystem Access Issues

**Problem:** Can't read files or access directories

**Solutions:**
1. Verify the root path argument (`.`) is correct
2. Check file permissions
3. Ensure you're in the project root directory

## Advanced Configuration

### Custom Memory Bank Location

If you want to store memory in a different location:

```json
{
  "args": [
    "-y",
    "@aakarsh-sasi/memory-bank-mcp",
    "--mode",
    "code",
    "--path",
    ".",
    "--folder",
    "custom-memory-folder"
  ]
}
```

### Multiple Project Support

For multiple projects, you can configure different memory banks:

```json
{
  "memory-bank-project1": {
    "command": "npx",
    "args": ["-y", "@aakarsh-sasi/memory-bank-mcp", "--mode", "code", "--folder", ".memory-project1"]
  },
  "memory-bank-project2": {
    "command": "npx",
    "args": ["-y", "@aakarsh-sasi/memory-bank-mcp", "--mode", "code", "--folder", ".memory-project2"]
  }
}
```

## Next Steps

1. **Test the servers** using the prompts above
2. **Start using memory** - Ask Cursor to remember project conventions
3. **Explore filesystem tools** - Use for faster code navigation
4. **Check for updates** - Keep MCP servers updated: `npm update -g @aakarsh-sasi/memory-bank-mcp`

## Additional Resources

- **Memory Bank MCP GitHub**: https://github.com/aakarsh-sasi/memory-bank-mcp
- **Filesystem MCP Docs**: https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
- **Cursor MCP Documentation**: https://docs.cursor.com/cli/mcp
- **Full MCP Setup Guide**: [MCP_SERVERS_SETUP.md](./MCP_SERVERS_SETUP.md)

---

**Ready to enhance your workflow!** After setup, your AI assistant will have better memory and faster code access. 🚀

