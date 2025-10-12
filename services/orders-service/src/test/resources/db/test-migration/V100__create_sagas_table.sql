-- V100: sagas table for tests
CREATE TABLE IF NOT EXISTS sagas (
    saga_id UUID PRIMARY KEY,
    saga_type VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    state VARCHAR(50) NOT NULL,
    data TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT
);
