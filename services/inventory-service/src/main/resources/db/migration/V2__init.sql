-- inventory-service: additional schema (PostgreSQL)
-- Note: outbox, processed_events, and sagas tables are created in V1__baseline.sql

-- Inventory stock table
CREATE TABLE inventory_stock (
    id uuid primary key,
    sku text not null unique,
    available_quantity int not null,
    version bigint not null default 0
);

-- Inventory reservations table
CREATE TABLE inventory_reservations (
    id uuid primary key,
    order_id uuid not null,
    sku text not null,
    quantity int not null,
    status varchar not null,
    reserved_at timestamptz not null,
    updated_at timestamptz not null
);
