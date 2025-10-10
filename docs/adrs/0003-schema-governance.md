# ADR 0003 – Schema Governance & Compatibility

## Status
Accepted – 2025-10-07

## Context
Event contracts must evolve safely while supporting downstream consumers. Options evaluated:
1. Informal reviews without tooling enforcement.
2. Formal governance with registry compatibility checks and documented change process.

## Decision
Adopt formal governance: use Confluent Schema Registry with backward compatibility (Avro `BACKWARD`, additive JSON schema changes). All changes require CI enforcement via `schemaCompatibilityCheck` Gradle task, documentation in `docs/schemas/CHANGELOG.md`, and communication to consumer teams. Breaking changes trigger new topics or versioned schemas with dual publishing.

## Consequences
- Prevents accidental breaking changes and aligns with data governance requirements.
- Adds CI gates and review ceremonies but reduces production regressions.
- Requires developer discipline to update changelog and notify stakeholders.
