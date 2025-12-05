-- V1__baseline.sql
-- Baseline schema for notification service

-- Ensure required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Outbox table (transactional outbox pattern)
CREATE TABLE public.outbox (
    id uuid PRIMARY KEY,
    aggregate_id text NOT NULL,
    aggregate_type text NOT NULL,
    event_type text NOT NULL,
    payload text NOT NULL,
    headers text,
    status text NOT NULL DEFAULT 'PENDING',
    occurred_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    version bigint NOT NULL DEFAULT 0
);

-- Indexes for outbox performance
CREATE INDEX idx_outbox_aggregate ON public.outbox (aggregate_type, aggregate_id);

-- Processed events table (idempotent consumer support)
CREATE TABLE public.processed_events (
    event_id uuid PRIMARY KEY,
    processed_at timestamptz NOT NULL DEFAULT now()
);

-- Sagas state table (saga orchestration)
CREATE TABLE public.sagas (
    saga_id uuid PRIMARY KEY,
    saga_type text NOT NULL,
    correlation_id text NOT NULL,
    state text NOT NULL,
    data text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint
);

-- Indexes for sagas performance
CREATE INDEX idx_sagas_type_state ON public.sagas (saga_type, state);
