CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_id TEXT NOT NULL,
    aggregate_type TEXT NOT NULL,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
occurred_at TIMESTAMP NOT NULL,
  published_at TIMESTAMP
    version BIGINT NOT NULL DEFAULT 0
);
