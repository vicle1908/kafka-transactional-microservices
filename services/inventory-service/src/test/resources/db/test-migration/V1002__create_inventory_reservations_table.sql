-- V1002: create inventory_reservations table aligned with InventoryReservationEntity
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    sku VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inventory_reservations_order_id ON inventory_reservations (order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_reservations_sku ON inventory_reservations (sku);