-- Development-only sample payment linked to seeded order
INSERT INTO payments (id, order_id, amount, status, processed_at, failure_reason, created_at, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000301',
  '00000000-0000-0000-0000-000000000101',
  123.45,
  'COMPLETED',
  NOW(),
  NULL,
  NOW(),
  NOW()
)
ON CONFLICT (id) DO NOTHING;
