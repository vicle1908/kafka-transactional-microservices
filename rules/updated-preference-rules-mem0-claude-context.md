---
alwaysApply: true
description: "Updated user preferences for MCP tool selection: Claude Context for code search, OpenMemory (mem0) for knowledge management."
tags:
  - preferences
  - claude-context
  - mem0
  - knowledge-management
  - code-search
---
# Updated User Preference Rules

## Code Indexing and Search Preferences

User prefers using Claude Context MCP tools for indexing the codebase and performing code search operations. This includes:

- Repository-level indexing and semantic search
- Code pattern discovery before implementing new features
- Existing codebase exploration and understanding
- Cross-reference analysis within the project

## Knowledge Management Preferences

For knowledge management and planning workflows, use OpenMemory (mem0) MCP exclusively:

- `mcp_openmemory_search-memories`: Retrieve prior decisions and context
- `mcp_openmemory_add-memory`: Store outcomes, insights, and action items
- Maintain synchronization between stored memories and canonical documentation (AGENTS.md, IMPLEMENTATION_PLAN.md)

## Deprecated Tools

**Byterover is deprecated and must not be used.** All knowledge management functions previously handled by Byterover should now use OpenMemory (mem0) MCP exclusively.

## Integration Requirements

- Always use Claude Context MCP for repository search before implementing new patterns
- Store all durable insights and decisions in mem0
- Keep documentation synchronized between mem0 storage and canonical files