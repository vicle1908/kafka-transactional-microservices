---
alwaysApply: true  
description: "Updated MCP Integration Requirements section for Android Development Standards with CLI-first preference."
tags:
  - android
  - development-standards
  - mcp-integration
  - cli-first
---
# Android Development Standards - MCP Integration Requirements Update

## MCP/CLI Integration Requirements (Updated)

### Build Operations (Preferred: CLI)
- Prefer ./gradlew for all build, test, and quality tasks (e.g., detekt, ktlintCheck, assembleDebug).
- Use Gradle MCP (mcp_gradle-mcp-server_execute_gradle_task) only when CLI is unavailable or when remote automation via MCP is required.

### Device Operations (Preferred: CLI)
- Prefer adb for install, logs, and package management.
- Use Android MCP for device operations when CLI is impractical (e.g., remote fleets) or for scripted automation.
- Use Mobile-MCP for UI automation as a fallback to CLI-based scripts.

### Research and Documentation
- Validate Navigation 3 and best practices using DeepWiki and Context7/DocFork.
- Code indexing/search remains with Claude Context MCP.

### Code Indexing and Search
- Use Claude Context MCP for repository-level indexing and code search (mandatory for new pattern discovery).

### Notes
- Maintain version catalog and convention plugins standards unchanged.
