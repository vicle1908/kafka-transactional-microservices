-- Repeatable seed data for payments-service (dev only)
-- Idempotent inserts (ON CONFLICT DO NOTHING)

-- Seed one completed payment linked to the seeded order
INSERT INTO payments (id, order_id, amount, status, processed_at, failure_reason)
VALUES (
  '00000000-0000-0000-0000-000000000301',
  '00000000-0000-0000-0000-000000000101',
  123.45,
  'COMPLETED',
  NOW(),
  NULL
)
ON CONFLICT (id) DO NOTHING;
