-- Repeatable seed data for notification-service (dev only)
-- Idempotent inserts (ON CONFLICT DO NOTHING)

INSERT INTO notifications (id, order_id, channel, template, payload, status, failure_reason, created_at, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000501',
  '00000000-0000-0000-0000-000000000101',
  'EMAIL',
  'ORDER_CONFIRMED',
  '{"orderId":"00000000-0000-0000-0000-000000000101","email":"customer@example.com"}',
  'QUEUED',
  NULL,
  NOW(),
  NOW()
)
ON CONFLICT (id) DO NOTHING;
