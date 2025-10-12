-- V100: create sagas table aligned with com.example.saga.SagaStateEntity
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

CREATE INDEX IF NOT EXISTS idx_sagas_type_corr ON sagas (saga_type, correlation_id);