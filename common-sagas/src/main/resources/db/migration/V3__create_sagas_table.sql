CREATE TABLE sagas (
    saga_id UUID PRIMARY KEY,
    saga_type VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    state VARCHAR(100) NOT NULL,
    data TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_sagas_type_correlation ON sagas (saga_type, correlation_id);
CREATE INDEX idx_sagas_state ON sagas (state);
CREATE INDEX idx_sagas_created_at ON sagas (created_at);

COMMENT ON TABLE sagas IS 'Distributed saga state management table for coordinating long-running business processes';
COMMENT ON COLUMN sagas.saga_id IS 'Business identifier and primary key for the saga instance';
COMMENT ON COLUMN sagas.saga_type IS 'Type/classifier of the saga (e.g., ORDER_FULFILLMENT, PAYMENT_PROCESSING)';
COMMENT ON COLUMN sagas.correlation_id IS 'Identifier that relates the saga to business entities';
COMMENT ON COLUMN sagas.state IS 'Current execution state of the saga';
COMMENT ON COLUMN sagas.data IS 'Serialized business data relevant to the saga execution';
COMMENT ON COLUMN sagas.created_at IS 'Timestamp when the saga was initiated';
COMMENT ON COLUMN sagas.updated_at IS 'Timestamp when the saga state was last updated';
COMMENT ON COLUMN sagas.version IS 'Optimistic locking version for concurrent access protection';
