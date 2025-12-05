-- notification-service: additional schema (PostgreSQL)
-- Note: outbox, processed_events, and sagas tables are created in V1__baseline.sql

-- Notifications domain table
CREATE TABLE notifications (
    id uuid primary key,
    order_id uuid not null,
    channel text not null,
    template text not null,
    payload text not null,
    status varchar not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    failure_reason text
);
