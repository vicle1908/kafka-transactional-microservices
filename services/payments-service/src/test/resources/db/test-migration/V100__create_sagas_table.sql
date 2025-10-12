-- V100: sagas table
CREATE TABLE IF NOT EXISTS sagas (
    saga_id UUID PRIMARY KEY,
    saga_type VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    state VARCHAR(50) NOT NULL,
    data TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);