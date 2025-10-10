---
description: "CLI-first development policy and server usage guide. Enforces CLI-first preference with MCP as fallback, Desktop Commander MCP integration, comprehensive search tools, OpenMemory (mem0) for knowledge, JetBrains MCP integration, and mandatory diagnostics."
alwaysApply: true
tags:
  - cli-first
  - mcp-fallback
  - desktop-commander
  - jetbrains-mcp
  - search-tools
  - openmemory
  - diagnostics
---
# Server Usage Guide (CLI-first)

## Core Policy

- Prefer CLI for supported tasks; use MCP servers as fallback or when they offer clear advantages.
- Exception: Code indexing/search uses Claude Context MCP tools by user preference.
- Desktop Commander MCP: Use for terminal/file operations that benefit from MCP integration.
- JetBrains MCP: Use for IDE-specific operations that benefit from IntelliJ integration.
- Search Strategy: Use comprehensive MCP search tools following AGENTS.md guidance.
- Knowledge storage: Use OpenMemory (mem0) MCP exclusively; Byterover is deprecated.

## Task Preferences and Fallbacks

### Build and Test
- **Preferred (CLI)**: `./gradlew` tasks (e.g., `./gradlew clean build test detekt ktlintCheck`)
- **Desktop Commander MCP**: `start_process` + `interact_with_process` for interactive builds
- **JetBrains MCP**: `execute_run_configuration` for run configurations, `execute_terminal_command` for shell commands in IDE
- **Fallback (External MCP)**: `mcp_gradle-mcp-server_execute_gradle_task`

### Android Device Operations
- **Preferred (CLI)**: `adb` (install, logcat, pm, am)
- **Desktop Commander MCP**: `start_process` for adb commands with process management
- **JetBrains MCP**: `execute_terminal_command` for adb commands within IDE terminal
- **Fallback (External MCP)**: `mcp_android_execute_adb_shell_command` and related Android MCP tools

### File Operations and Project Management

#### Desktop Commander MCP (Advanced file operations)
- **Terminal Integration**:
  - `start_process`: Start programs with smart detection of readiness
  - `interact_with_process`: Send commands to running programs and get responses
  - `read_process_output`: Read output from running processes
  - `list_sessions`: List all active terminal sessions
  - `force_terminate`: Force terminate running sessions
- **File System Operations**:
  - `read_file`: Read contents with URL support and pagination
  - `read_multiple_files`: Read multiple files simultaneously
  - `create_directory`: Create directories
  - `list_directory`: Get detailed file/directory listings
  - `move_file`: Move or rename files and directories
  - `get_file_info`: Retrieve detailed metadata
- **Search Operations**:
  - `start_search`: Start streaming search for files by name or content patterns
  - `get_more_search_results`: Get paginated results from active search
  - `stop_search`: Stop active search gracefully
  - `list_searches`: List all active search sessions
- **Text Editing**:
  - `edit_block`: Apply targeted text replacements with diff feedback

#### JetBrains MCP (IDE-integrated operations)
- **File Management**:
  - `open_file_in_editor`: Open files in IntelliJ editor
  - `get_file_text_by_path`: Read file contents with IDE context
  - `replace_text_in_file`: Targeted text replacements
  - `create_new_file`: Create files with IDE indexing
  - `reformat_file`: Apply IntelliJ code formatting
  - `rename_refactoring`: Context-aware symbol renaming

#### CLI Fallback
- Standard file operations (`cat`, `touch`, `mv`, `mkdir`, `ls`, etc.)

### Code Analysis and Search

#### JetBrains MCP (IDE-integrated analysis)
- `get_file_problems`: IntelliJ inspections and error analysis
- `search_in_files_by_text`: Fast IDE-based text search
- `search_in_files_by_regex`: Pattern search with highlighting
- `get_symbol_info`: Symbol documentation and declarations

#### Claude Context MCP (Repository-level search)
- Repository-level indexing and semantic search (user preference)

#### Desktop Commander MCP (System-wide search)
- `start_search` with `searchType='content'` for ripgrep-based content search
- `start_search` with `searchType='files'` for file name search

#### CLI assist
- ripgrep/grep for quick literal checks

### Project Discovery

#### JetBrains MCP (IDE project context)
- `get_run_configurations`: List available run configurations
- `get_project_modules`: Project structure and modules
- `get_project_dependencies`: Dependency analysis
- `find_files_by_glob`: Pattern-based file search
- `find_files_by_name_keyword`: Name-based file search
- `list_directory_tree`: Project tree visualization
- `get_repositories`: VCS roots identification

#### Desktop Commander MCP (System-wide discovery)
- `list_directory`: Detailed file/directory listings
- `start_search`: Advanced file and content search

#### CLI Fallback
- `find`, `ls`, `tree` commands

### Research and Documentation (MCP Search Tools)

Following AGENTS.md MCP Search Practice (lines 54-66), use this comprehensive search strategy:

#### Web Research Strategy
- **Broad Web Research**: `mcp_brave_web_search` for general web research
- **Fresh Information**: `mcp_brave_news_search` when freshness (≤7 days) matters
- **Visual Assets**: `mcp_brave_image_search` for visual assets
- **Deep Investigations**: Build map→extract pipeline:
  - `mcp_tavily_map` to enumerate relevant docs
  - `mcp_tavily_extract` or `mcp_tavily_search` with `search_depth='advanced'` for full-text pulls
  - Request `include_raw_content` when evaluating technical specs

#### Documentation Research Strategy
- **Repository Documentation**: `mcp_deepwiki_ask_question` (GitHub doc crawler) before general search
- **Library Documentation**: 
  - `mcp_context7_resolve-library-id` → `mcp_context7_get-library-docs` for up-to-date library documentation
  - Alternative: `mcp_docfork_get-library-docs` for single-step library docs access

#### Code Examples and Implementation
- **Real-world Code**: `mcp_grep-remote_searchGitHub` to find relevant code from over a million public repositories
- **Programming Context**: `mcp_exa_get_code_context_exa` for APIs, libraries, and SDKs with highest quality and freshest context

#### Advanced Search and Analysis  
- **Semantic Search**: `mcp_exa_web_search_exa` when Brave results are thin or contradictory
- **Medium Research**: 
  - `mcp_medium-search_search_medium_topic` for comprehensive topic-based research
  - `mcp_medium-search_search_by_author` for domain expert insights
  - `mcp_medium-search_research_compilation` for multi-topic synthesis with citations

#### Internal Knowledge Validation
- **Desktop Commander MCP**: `start_search` with `searchType='content'` to cross-check local docs
- **JetBrains MCP**: `search_in_files_by_text` for IDE-based local search
- **CLI**: grep/ripgrep for quick local checks

#### Multi-AI Consultation (Final Steps)
- **Complex Decisions**: `mcp_zen_consensus` (consulting Gemini, OpenAI, Grok-4) for multiple AI perspectives
- **Deep Analysis**: `mcp_zen_thinkdeep` when deeper reasoning or resolution is required

### Memory and Planning
- **Required**: OpenMemory (mem0) MCP — `mcp_openmemory_search-memories`, `mcp_openmemory_add-memory`

### Configuration Management

#### Desktop Commander MCP (System configuration)
- `get_config`: Get complete server configuration
- `set_config_value`: Set specific configuration values (blockedCommands, defaultShell, allowedDirectories, etc.)

## Search Tool Selection Matrix

Based on AGENTS.md guidance, select tools by research type:

| Research Type | Primary Tools | When to Use |
|---------------|---------------|-------------|
| **Quick Facts** | Brave Web Search | Low-latency fact checks |
| **Fresh News** | Brave News Search | Information ≤7 days old |
| **Deep Investigation** | Tavily Map → Tavily Extract | Technical specs, comprehensive research |
| **Repository Docs** | DeepWiki | GitHub repository documentation |
| **Library Docs** | Context7 (resolve → get) OR DocFork | Official library documentation |
| **Code Examples** | Grep-Remote (searchGitHub) | Production code patterns |
| **Programming Context** | Exa Code Context | API/SDK usage with high quality |
| **Semantic Understanding** | Exa Web Search | Neural search when others fail |
| **Articles/Tutorials** | Medium Search | In-depth topic research |
| **Local File Search** | Desktop Commander search OR JetBrains search | Internal doc/code search |
| **Repository Search** | Claude Context MCP | Semantic codebase search |
| **Multi-AI Analysis** | Zen Consensus/ThinkDeep | Complex decision validation |

## Usage Enforcement

### Build Operations
- Use `./gradlew` by default for standard tasks
- Use Desktop Commander MCP `start_process` for interactive build processes
- Use JetBrains MCP `execute_run_configuration` for complex run configurations
- Use external Gradle MCP only when remote automation is required

### Code Operations
- Always use JetBrains MCP `open_file_in_editor` before `get_file_problems` for proper indexing
- Prefer JetBrains MCP for refactoring operations (`rename_refactoring`)
- Use Claude Context MCP for repository-level code search before implementing new patterns
- Use Desktop Commander MCP or JetBrains MCP for file-level search and analysis

### Research Operations
- **Follow AGENTS.md search practice**: Triage query type first, use appropriate tools
- **Combine search tools**: Use multiple tools as recommended (`brave_web_search`, `tavily_search`, `web_search_exa`, `searchGitHub`, `search_medium_topic`) before Zen MCP evaluation
- **Document findings**: Always capture tool outputs, link to @IMPLEMENTATION_PLAN.md action items

### Android Operations
- Use `adb` first
- Use Desktop Commander MCP `start_process` for adb with process management
- Use JetBrains MCP `execute_terminal_command` for adb within IDE context
- Use Android/Mobile MCP when scripting via external MCP is preferable or devices are remote

### Memory Management
- All durable insights and decisions must go to OpenMemory (mem0); do not use Byterover

## Diagnostics Gate (MANDATORY)

Before commit/push and before running long builds:
1. **File Indexing**: JetBrains MCP `open_file_in_editor` on all changed files to ensure IntelliJ indexing
2. **Problem Analysis**: JetBrains MCP `get_file_problems` to review errors/warnings with IntelliJ inspections
3. **Issue Resolution**: Address reported issues or document waivers in the PR

Examples:
```bash
# JetBrains MCP diagnostics
open_file_in_editor(filePath="src/main/kotlin/orders/OrderService.kt")
get_file_problems(filePath="src/main/kotlin/orders/OrderService.kt", errorsOnly=false)
```

## Research Workflow Example

Following AGENTS.md comprehensive search practice:

```bash
# 1. Internal patterns first (Claude Context MCP)
mcp_claude-context_search_code("Spring Boot Kafka transaction configuration")

# 2. Broad web research (Brave)
mcp_brave_web_search(query="Spring Boot 3.5 Kafka transactions")

# 3. Deep investigation (Tavily pipeline)
mcp_tavily_map(url="https://spring.io/projects/spring-kafka")
mcp_tavily_extract(urls=["mapped_docs_urls"], include_raw_content=true)

# 4. Repository best practices (DeepWiki)  
mcp_deepwiki_ask_question(repoName="spring-projects/spring-kafka", question="transaction configuration best practices")

# 5. Library documentation (Context7)
mcp_context7_resolve-library-id(libraryName="spring-kafka")
mcp_context7_get-library-docs(context7CompatibleLibraryID="/spring/kafka", topic="transactions")

# 6. Real-world code examples (Grep-Remote)
mcp_grep-remote_searchGitHub(query="KafkaTransactionManager", language=["Kotlin"], repo="spring*")

# 7. Programming context (Exa)
mcp_exa_get_code_context_exa(query="Spring Kafka transaction manager configuration examples")

# 8. Semantic gaps (Exa Web Search)
mcp_exa_web_search_exa(query="Spring Boot Kafka exactly-once semantics implementation")

# 9. Articles and tutorials (Medium)
mcp_medium-search_search_medium_topic(query="Spring Kafka transactions", filters={"tags": ["spring-boot", "kafka"]})

# 10. Internal validation (Desktop Commander or JetBrains)
# Desktop Commander approach:
start_search(path="/project/root", pattern="KafkaTransactionManager", searchType="content")
# OR JetBrains approach:
search_in_files_by_text(searchText="KafkaTransactionManager", fileMask="*.kt")

# 11. Multi-AI validation (Zen)
mcp_zen_consensus(models=[{"model": "gemini-2.5-pro"}, {"model": "gpt-5"}], step="Evaluate Spring Kafka transaction configuration approaches")
```

## Fallback Priority

### Build Operations
1. **CLI**: `./gradlew clean build test`
2. **Desktop Commander MCP**: `start_process` for interactive builds
3. **JetBrains MCP**: `execute_run_configuration` or `execute_terminal_command`
4. **External MCP**: Gradle MCP server

### File Operations
1. **JetBrains MCP**: IDE-integrated tools with proper indexing
2. **Desktop Commander MCP**: Advanced file operations and search
3. **CLI**: Standard file operations

### Code Analysis
1. **JetBrains MCP**: IntelliJ inspections and IDE search
2. **Claude Context MCP**: Repository-level semantic search
3. **Desktop Commander MCP**: System-wide search capabilities
4. **CLI**: grep/rg for quick checks

### Research Operations
1. **MCP Search Tools**: Follow comprehensive search matrix above
2. **Browser/CLI**: Manual research when MCP tools are unavailable
3. **Document gaps**: Record in Research Backlog when sources are inconclusive

### Android Operations
1. **CLI**: Direct `adb` commands
2. **Desktop Commander MCP**: `start_process` for adb with process management
3. **JetBrains MCP**: `execute_terminal_command` for adb in IDE
4. **External MCP**: Android MCP → Mobile-MCP (UI)

## Knowledge Integration (mem0)

### Before Work
- `mcp_openmemory_search-memories` with focus keywords (e.g., `Kafka outbox`, `Debezium`) to surface prior conclusions
- Use JetBrains MCP `get_all_open_file_paths` to understand current work context
- Use Desktop Commander MCP `list_sessions` to check active processes

### During Work
- Use JetBrains MCP diagnostics for continuous quality assurance
- Use Desktop Commander MCP for advanced terminal and file operations
- Leverage comprehensive search tool matrix for research
- Follow AGENTS.md guidance: "Always capture tool outputs in the working note, link to @IMPLEMENTATION_PLAN.md action items"

### After Work
- `mcp_openmemory_add-memory` using succinct, action-oriented phrasing; tag entries with related doc path (`@AGENTS`, `@IMPLEMENTATION_PLAN`, etc.)
- Summaries captured in memory must also be reflected in canonical docs (AGENTS.md, IMPLEMENTATION_PLAN.md)
- Keep AGENTS.md and IMPLEMENTATION_PLAN.md in sync with stored memories

## MCP Server Configuration

### Desktop Commander MCP Setup
1. **Installation**: Use npx, bash installer, Smithery, or Docker installation
2. **Configuration**: Set appropriate `allowedDirectories`, `blockedCommands`, and `defaultShell`
3. **Security**: Consider Docker installation for complete isolation

### JetBrains MCP Setup
1. **Enable MCP Server**: Settings → Tools → MCP Server → Enable MCP Server
2. **Auto-Configure Clients**: Use Auto-Configure for supported clients (Claude Desktop, Cursor, VS Code)
3. **Brave Mode** (Optional): Enable "Run shell commands or run configurations without confirmation" for automated workflows

### Key Integration Patterns

#### Mandatory Diagnostics Flow (JetBrains MCP)
```bash
# AGENTS.md line 50: "open the target file with `open_file_in_editor` before running `get_file_problems`"
1. open_file_in_editor(filePath="path/to/file.kt")
2. get_file_problems(filePath="path/to/file.kt") 
```

#### Interactive Development Flow (Desktop Commander MCP)
```bash
# Start interactive development session
1. start_process(command="./gradlew bootRun", timeout_ms=30000)
2. interact_with_process(pid=process_id, input="additional commands")
3. read_process_output(pid=process_id) # Check for readiness
4. force_terminate(pid=process_id) # When done
```

## Quality Standards

- **Completeness**: Use at least 3 independent sources for non-trivial research (following search tool matrix)
- **Accuracy**: 90%+ consensus on major recommendations via multi-AI consultation
- **Diagnostics**: No unreviewed `get_file_problems` errors prior to PR
- **Build**: PRs must pass `./gradlew build` (or equivalent) and CI gates
- **IDE Integration**: Leverage JetBrains MCP for operations that benefit from IntelliJ context
- **Process Management**: Use Desktop Commander MCP for advanced terminal/file operations
- **Search Coverage**: Use comprehensive search strategy as per AGENTS.md guidance

## Error Handling

- **CLI command fails**: Switch to appropriate MCP fallback for that task
- **Desktop Commander MCP unavailable**: Fall back to CLI, then JetBrains MCP or external MCP
- **JetBrains MCP unavailable**: Fall back to Desktop Commander MCP, then CLI, then external MCP
- **Search tool unavailable**: Use alternative from search tool matrix; prefer Brave over raw Google, fall back to Tavily advanced search or Exa when results are thin
- **External MCP server unavailable**: Prefer CLI or alternate MCP; document deviation in PR
- **IDE not available**: Use Desktop Commander MCP or CLI equivalents

## Operational Examples

### Comprehensive Development Flow
```bash
# 1. Research existing patterns (Claude Context)
mcp_claude-context_search_code("similar implementation patterns")

# 2. External research (multiple search tools)
mcp_brave_web_search(query="Spring Boot 3.5 specific question")
mcp_deepwiki_ask_question(repoName="spring-projects/spring-boot", question="implementation guidance")

# 3. Build and test (CLI preferred)  
./gradlew clean build test detekt

# 4. Interactive development (Desktop Commander MCP)
start_process(command="./gradlew bootRun", timeout_ms=30000)
interact_with_process(pid=process_id, input="test commands")

# 5. IDE integration for file work (JetBrains MCP)
open_file_in_editor(filePath="src/main/kotlin/OrderService.kt")
get_file_problems(filePath="src/main/kotlin/OrderService.kt")

# 6. Apply changes with IDE support (JetBrains MCP)
rename_refactoring(symbolName="oldMethodName", newName="newMethodName")
search_in_files_by_text(searchText="related patterns", fileMask="*.kt")

# 7. Advanced search (Desktop Commander MCP)
start_search(path="/project/root", pattern="configuration files", searchType="content")

# 8. Final verification (CLI)
./gradlew assembleDebug

# 9. Process cleanup (Desktop Commander MCP)
force_terminate(pid=process_id)

# 10. Store insights (mem0)
mcp_openmemory_add-memory(content="Implementation decision: ...", tags="@IMPLEMENTATION_PLAN")
```

This comprehensive approach balances CLI efficiency with the full power of Desktop Commander MCP's terminal/file operations, JetBrains MCP's IDE integration, comprehensive MCP search tools, and follows the detailed guidance from AGENTS.md.