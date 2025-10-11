#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/compose.yml"

# Insert an outbox row into inventory DB and consume from outbox.inventory

echo "Inserting an inventory outbox row"
read -r -d '' INSERT_SQL <<'SQL'
INSERT INTO public.outbox (
  id, aggregate_id, aggregate_type, event_type, payload, headers, occurred_at, version
) VALUES (
  gen_random_uuid(), 'inventory-smoke', 'inventory', 'InventoryReserved', '{"inventoryId":"inv-1","sku":"SMOKE-1","qty":1}', '{"source":"smoke"}', now(), 0
);
SQL

docker compose -f "$COMPOSE_FILE" exec -T postgres bash -lc "psql -U ${POSTGRES_USER:-app} -d inventory -v ON_ERROR_STOP=1 <<'EOF'
$INSERT_SQL
EOF" >/dev/null

echo "Consuming from outbox.inventory (max 1 message, 10s timeout)"
docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic outbox.inventory \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 10000 || true
