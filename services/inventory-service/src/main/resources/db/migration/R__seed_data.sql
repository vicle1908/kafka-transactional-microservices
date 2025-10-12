-- Repeatable seed data for inventory-service (dev only)
-- Idempotent inserts (ON CONFLICT DO NOTHING)

-- Seed stock for two SKUs referenced by orders seed
INSERT INTO inventory_stock (id, sku, available_quantity, version)
VALUES
  ('00000000-0000-0000-0000-000000000401', 'SKU-ABC', 100, 0),
  ('00000000-0000-0000-0000-000000000402', 'SKU-XYZ', 200, 0)
ON CONFLICT (id) DO NOTHING;
