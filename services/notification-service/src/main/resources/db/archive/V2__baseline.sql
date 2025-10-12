-- V2__baseline.sql
-- Baseline schema for notification service (excludes shared tables provided by common modules)

-- Ensure required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- NOTE:
-- - Outbox table is created by common-persistence (V1__create_outbox_table.sql)
-- - Sagas table is created by common-sagas (V100__create_sagas_table.sql)
-- This baseline intentionally excludes those to avoid Flyway version conflicts.

-- Processed events table (idempotent consumer support)
CREATE TABLE IF NOT EXISTS public.processed_events (
    event_id uuid PRIMARY KEY,
    processed_at timestamptz NOT NULL DEFAULT now()
);
