-- Create outbox table for transactional outbox pattern
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_aggregate ON outbox (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_occurred_at ON outbox (occurred_at);

-- Create processed_events table for idempotent consumers
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);

-- Create sagas table for saga orchestration
CREATE TABLE sagas (
    saga_id UUID PRIMARY KEY,
    saga_type TEXT NOT NULL,
    correlation_id TEXT NOT NULL,
    state TEXT NOT NULL,
    data TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_saga_type_correlation UNIQUE (saga_type, correlation_id)
);

CREATE INDEX idx_sagas_type_state ON sagas (saga_type, state);
