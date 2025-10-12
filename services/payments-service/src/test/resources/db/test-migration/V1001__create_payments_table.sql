-- V1001: payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    failure_reason VARCHAR(1024)
);
