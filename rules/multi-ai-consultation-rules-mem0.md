---
description: "Framework for multi-AI consultation with mem0 knowledge, referencing CLI-aware research workflow."
alwaysApply: true
globs:
  - "**/*"
tags:
  - consultation
  - consensus
  - multi-ai
  - decision-making
  - mem0
---
# Multi-AI Consultation Rules (mem0)

## Core Consultation Enforcement

- Use multi-AI consultation for critical decisions, architecture, complex problems, and major code reviews.

## Integration with Research Workflow

- Follow the 10-step Research Strategy Rules (CLI-aware) before consultation.
- Steps 9-10: Use Zen MCP (mcp_zen_consensus, mcp_zen_thinkdeep).

## Knowledge Integration (mem0)

- Before: mcp_openmemory_search-memories to retrieve relevant context.
- After: mcp_openmemory_add-memory to store decisions, rationale, and action items.
- Keep AGENTS.md and IMPLEMENTATION_PLAN.md synchronized.

## Trigger Conditions

- Architecture reviews, critical stack choices, complex problem solving, major code or security reviews, planning, performance, and security implementation.

## Primary Consultation Tools

- mcp_zen_consensus: multi-model validation
- mcp_zen_thinkdeep: complex analysis
- Optional: planning via local docs/AGENTS.md templates

## Process

1. Pre-Consultation
   - Ensure research workflow completion
   - Retrieve mem0 knowledge
   - Run diagnostics gate (mcp-router open_file_in_editor → get_file_problems) on touched modules
2. Multi-AI Analysis
   - Build consensus (mcp_zen_consensus)
   - Deep analysis as needed (mcp_zen_thinkdeep)
3. Knowledge Integration
   - Store outcomes in mem0 with tags and links to PRs/docs

## Usage Enforcement

- Do not proceed with consultation if research is incomplete or diagnostics reveal blocking errors.
- Ensure mem0 updates are made after conclusions.

## Quality Assurance

- Aim for 90%+ consensus for major recommendations.
- Provide implementation plans aligned with AGENTS.md guidance.
