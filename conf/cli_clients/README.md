# Clink Tool Configuration

This directory contains the configuration for the Zen MCP server's `clink` tool, which enables multi-agent orchestration and cross-CLI collaboration within the development workflow.

## Directory Structure

```
conf/
  cli_clients/
    ├── gemini.json    # Google Gemini CLI configuration
    ├── claude.json    # Anthropic Claude CLI configuration
    ├── codex.json     # OpenAI Codex CLI configuration
    └── qwen.json      # Alibaba Qwen CLI configuration

systemprompts/
  clink/
    ├── planner.txt              # Strategic planning role prompt
    ├── codereviewer.txt         # Code review role prompt
    ├── default.txt              # General assistance role prompt
    ├── default_planner.txt      # Default planner role prompt
    ├── codex_codereviewer.txt   # Codex-specific code reviewer role prompt
    └── qwen_codereviewer.txt    # Qwen-specific code reviewer role prompt
```

## Security Considerations

⚠️ **IMPORTANT**: The default configurations include flags that bypass safety prompts to enable autonomous operations:

- `--yolo` for Gemini (bypasses approval prompts)
- `--permission-mode acceptEdits` for Claude (auto-applies edits)
- `--dangerously-bypass-approvals-and-sandbox` for Codex (bypasses safety checks)

These configurations should only be used in trusted environments. For production or sensitive environments, consider modifying these settings to require explicit approval for changes.

## Usage

The clink tool can be used to:
- Spawn isolated AI subagents from within the current session
- Enable cross-CLI orchestration between different AI models
- Maintain conversation continuity while leveraging external tools
- Pass file references efficiently to conserve tokens
- Use role-based processing for specialized tasks

## Configuration Options

Each CLI client configuration includes:
- `command`: The CLI command to execute
- `args`: Arguments to pass to the command
- `description`: Brief description of the client
- `capabilities`: List of supported capabilities