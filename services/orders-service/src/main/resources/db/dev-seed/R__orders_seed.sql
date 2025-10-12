-- Development-only sample data for orders
INSERT INTO orders (id, customer_id, total_amount, status, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0000-000000000101', 'customer-001', 123.45, 'PENDING', NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, created_at)
VALUES
  ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', 'SKU-ABC', 'Sample Product A', 1, 23.45, NOW()),
  ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000101', 'SKU-XYZ', 'Sample Product B', 2, 50.00, NOW())
ON CONFLICT (id) DO NOTHING;
