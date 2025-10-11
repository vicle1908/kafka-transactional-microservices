# Phase 0 Workshops – Discovery & Architecture

## Session 1 – Service Catalog & Data Stores

- Participants: TBD (Product Owner, Tech Lead, Domain SMEs)
- Agenda:
  - Confirm target microservices (Order, Payment, Inventory, Notification).
  - Identify upstream/downstream systems and data stores.
  - Classify flows requiring exactly-once vs at-least-once guarantees.
- Notes:
  - Record data ownership and read/write patterns.
  - Capture integration constraints (legacy systems, batch processes).

## Session 2 – Consistency & Messaging Requirements

- Participants: TBD (Architects, Domain Experts, Ops)
- Agenda:
  - Walk through order-to-cash process and failure scenarios.
  - Define compensation expectations per domain.
  - Validate events needed for consumer teams.
- Notes:
  - Highlight SLAs, throughput targets, retention requirements.
  - Capture regulatory constraints affecting data replication.

## Session 3 – Technical Stack Finalization

- Participants: TBD (Platform/Ops, Security, Architects)
- Agenda:
  - Review proposed platform stack (Spring Boot 3.5.6, Java 25 primary with Java 23/21 fallback, Kafka 4.1.0, Debezium 3.3.0.Final, Istio ambient, Temporal, Redis, CDN strategy).
  - Confirm hosting options (self-managed vs managed services).
  - Align on observability and DevOps tooling.
- Notes:
  - Document decisions and open questions for ADRs.
  - Identify security reviews and compliance requirements.

## Action Items

- Assign note takers and owners for each workshop.
- Store outputs in `docs/notes/phase-0-workshops.md` and link to relevant ADRs.
- Update `@AGENTS.md` and `IMPLEMENTATION_PLAN.md` with finalized outcomes.
