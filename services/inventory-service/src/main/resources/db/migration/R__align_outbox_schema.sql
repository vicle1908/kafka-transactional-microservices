-- Ensure outbox table has required columns when prior migrations omitted them
ALTER TABLE IF EXISTS outbox
    ADD COLUMN IF NOT EXISTS status TEXT;

ALTER TABLE IF EXISTS outbox
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE outbox SET status = COALESCE(status, 'PENDING');
UPDATE outbox SET version = COALESCE(version, 0);

ALTER TABLE outbox
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE outbox
    ALTER COLUMN version SET NOT NULL;
