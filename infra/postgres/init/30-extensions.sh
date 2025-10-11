#!/usr/bin/env bash
# Ensure common extensions are present across all service databases.
# Runs during container initialization via docker-entrypoint-initdb.d.
# Uses environment defaults if per-service names are not provided.
set -euo pipefail

DBS=("${ORDERS_DB_NAME:-orders}" "${PAYMENTS_DB_NAME:-payments}" "${INVENTORY_DB_NAME:-inventory}" "${NOTIFICATIONS_DB_NAME:-notifications}")

for db in "${DBS[@]}"; do
  echo "Ensuring extensions in $db"
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$db" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
  # uuid-ossp is optional; pgcrypto's gen_random_uuid() is preferred, but enable if available
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$db" -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" || true
done
