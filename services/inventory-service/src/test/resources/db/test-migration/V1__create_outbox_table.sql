-- V1: create outbox table aligned with com.example.outbox.entity.OutboxMessage
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status ON outbox (status);
CREATE INDEX idx_outbox_occurred_at ON outbox (occurred_at);
