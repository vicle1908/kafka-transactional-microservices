-- V1001: create inventory_stock table aligned with InventoryStockEntity
CREATE TABLE IF NOT EXISTS inventory_stock (
    id UUID PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    available_quantity INTEGER NOT NULL,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_sku ON inventory_stock (sku);