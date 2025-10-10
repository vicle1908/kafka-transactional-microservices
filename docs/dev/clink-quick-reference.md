# Clink Quick Reference Guide

## Command Structure

```
clink <cli_name> [role] <prompt>
```

## Available CLI Clients

- `gemini` - Best for: Large context analysis, web research, strategic planning
- `claude` - Best for: Code review, detailed analysis, editing tasks
- `codex` - Best for: Code generation, implementation, complex problem solving
- `qwen` - Best for: General assistance, alternative perspectives

## Available Roles

- `planner` - Strategic planning and architecture decisions
- `codereviewer` - Code quality and security analysis  
- `default` - General assistance (default if no role specified)

## Common Use Cases in Microservices Project

### Architecture & Planning

```bash
# Design new saga patterns
clink gemini planner "Design saga for Order → Payment → Inventory flow"

# Architecture decision analysis
clink claude planner "Evaluate database per service vs shared database approach"
```

### Code Quality & Review

```bash
# Review Kafka transaction configuration
clink claude codereviewer "Review transaction manager setup in KafkaConfig.kt"

# Security audit
clink gemini codereviewer "Audit security implications of transactional outbox pattern"
```

### Complex Analysis

```bash
# Performance analysis
clink codex "Analyze potential bottlenecks in Kafka consumer lag monitoring"

# Debugging assistance
clink gemini "Debug race condition in transactional outbox event processing"
```

### Research & Investigation

```bash
# Technology research
clink gemini "Research best practices for Kafka transaction timeout settings"

# Schema design
clink claude "Design Avro schema for InventoryReservationEvent with backward compatibility"
```

## Security Considerations

⚠️ The configurations use bypass flags for autonomous operations:

- Use cautiously with file modification commands
- Verify all generated code before applying
- Consider removing bypass flags in production environments

## Tips

- Use clink when you need to preserve your main session context
- Pass file references instead of large content blocks to save tokens
- Use specific roles for specialized tasks
- Combine with other MCP tools for enhanced capabilities
