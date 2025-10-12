-- Baseline schema for tests: create shared tables used by the service
-- Outbox table for transactional outbox pattern
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_id TEXT NOT NULL,
    aggregate_type TEXT NOT NULL,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at TIMESTAMP WITH TIME ZONE
);

-- Idempotency ledger for processed events
CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Saga state storage
CREATE TABLE IF NOT EXISTS sagas (
    saga_id UUID PRIMARY KEY,
    saga_type TEXT NOT NULL,
    correlation_id TEXT NOT NULL,
    state TEXT NOT NULL,
    data TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT
);
