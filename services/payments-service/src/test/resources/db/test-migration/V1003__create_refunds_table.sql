-- V1003: refunds table
CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    reason TEXT,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(1024)
);
