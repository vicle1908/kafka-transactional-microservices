-- Development-only sample inventory stock
INSERT INTO inventory_stock (id, sku, available_quantity, version)
VALUES
  ('00000000-0000-0000-0000-000000000401', 'SKU-ABC', 100, 0),
  ('00000000-0000-0000-0000-000000000402', 'SKU-XYZ', 200, 0)
ON CONFLICT (id) DO NOTHING;
