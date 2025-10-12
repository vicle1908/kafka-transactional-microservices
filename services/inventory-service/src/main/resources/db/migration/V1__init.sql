-- inventory-service: initial schema (PostgreSQL)
-- Outbox
create table if not exists outbox (
    id uuid primary key,
    aggregate_id text not null,
    aggregate_type text not null,
    event_type text not null,
    payload text not null,
    headers text,
    status varchar not null default 'PENDING',
    occurred_at timestamptz not null,
    published_at timestamptz,
    version bigint not null default 0
);

-- Processed events
create table if not exists processed_events (
    event_id uuid primary key,
    processed_at timestamptz not null
);

-- Sagas
create table if not exists sagas (
    saga_id uuid primary key,
    saga_type text not null,
    correlation_id text not null,
    state varchar not null,
    data text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint uq_saga_type_correlation unique (saga_type, correlation_id)
);

-- Inventory stock and reservations
create table if not exists inventory_stock (
    id uuid primary key,
    sku text not null unique,
    available_quantity int not null,
    version bigint not null default 0
);

create table if not exists inventory_reservations (
    id uuid primary key,
    order_id uuid not null,
    sku text not null,
    quantity int not null,
    status varchar not null,
    reserved_at timestamptz not null,
    updated_at timestamptz not null
);
