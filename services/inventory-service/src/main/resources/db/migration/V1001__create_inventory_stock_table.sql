CREATE TABLE inventory_stock (
    id UUID PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_inventory_stock_sku ON inventory_stock(sku);
