---
alwaysApply: true
description: "Research workflow aligned with AGENTS.md: CLI-aware, Claude Context for code search, mem0 for knowledge, and mandatory diagnostics via mcp-router."
tags:
  - research
  - cli-first
  - mem0
  - diagnostics
---
# Research Strategy Rules (CLI-aware)

## Core Enforcement

- Always research before making code changes or architectural decisions.
- Prefer CLI for local tasks; use MCP tools for code search, documentation, and web research where they add value.

## 10-Step Research Workflow

1. Existing code patterns (mandatory)
   - Primary: Claude Context MCP repository-level indexing and search
   - Optional: CLI assist (rg/grep) for quick literal checks
2. Official documentation
   - Choose one MCP: Context7 (resolve ID → get docs) or DocFork (single-step)
3. Repository best practices
   - DeepWiki MCP (GitHub repository docs)
4. Real-world code examples
   - Grep-Remote MCP (public repositories)
5. Semantic understanding
   - Exa MCP
6. Current information and trends
   - Tavily MCP and/or Brave MCP
7. Article insights and tutorials
   - Medium Search MCP
8. Diagnostics gate (mandatory)
   - mcp-router open_file_in_editor → get_file_problems on touched files/modules
9. Multi-AI consensus
   - Zen MCP: mcp_zen_consensus
10. Deep analysis (if complex)
   - Zen MCP: mcp_zen_thinkdeep

## Tool Selection Guidelines

- Code search: Always use Claude Context MCP for indexing/search.
- Documentation: Use Context7 or DocFork (choose one).
- Repo docs: Use DeepWiki before general web search.
- Web search: Use Tavily/Brave; use Exa for semantic gaps.
- Articles: Use Medium Search for tutorials/how-tos.

## Research Quality Rules

- Use multiple sources: at least 3 MCP tools for non-trivial topics.
- Cross-reference findings; ensure recency (within 1 year) when applicable.
- Provide concrete examples and file references where possible.
- Record sources and decisions in mem0.

## Research Storage (mem0 only)

- Store key findings and decisions: mcp_openmemory_add-memory
- Retrieve prior context: mcp_openmemory_search-memories
- Keep AGENTS.md and IMPLEMENTATION_PLAN.md synchronized with stored memories.

## Diagnostics Gate Details

- For all changed files: run mcp-router open_file_in_editor, then get_file_problems.
- Block commits if there are unresolved errors; warnings must be triaged (fix or justify).

## Failure Prevention

Do not proceed without research when:
- Adding or replacing libraries/frameworks
- Architectural or security-critical changes
- Performance optimization and dependency updates

Research must include:
- Existing codebase patterns via Claude Context
- Official docs
- Community best practices and real-world examples
- Multi-AI validation for critical decisions