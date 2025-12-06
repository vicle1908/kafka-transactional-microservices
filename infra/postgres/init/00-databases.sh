#!/bin/bash
set -e

# Create additional databases if they don't exist
# The 'orders' database is created by POSTGRES_DB env var

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE payments OWNER app' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payments')\gexec
    SELECT 'CREATE DATABASE inventory OWNER app' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory')\gexec
    SELECT 'CREATE DATABASE notifications OWNER app' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notifications')\gexec
EOSQL

echo "All databases created successfully"
