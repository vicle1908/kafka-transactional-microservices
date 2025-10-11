#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/compose.yml"

# Insert a row into orders.outbox and read it back from Kafka

echo "Inserting an outbox row into orders DB"
INSERT_SQL=$(cat <<'SQL'
INSERT INTO public.outbox (
  id, aggregate_id, aggregate_type, event_type, payload, headers, occurred_at, version
) VALUES (
  gen_random_uuid(), 'order-smoke', 'orders', 'OrderCreated', '{"orderId":"order-smoke","amount":101}', '{"source":"smoke"}', now(), 0
);
SQL
)

docker compose -f "$COMPOSE_FILE" exec -T postgres bash -lc "psql -U ${POSTGRES_USER:-app} -d ${POSTGRES_DB_ORDERS:-orders} -v ON_ERROR_STOP=1 <<'EOF'
$INSERT_SQL
EOF" >/dev/null

echo "Consuming from outbox.orders (max 1 message, 10s timeout)"
docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic outbox.orders \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 10000 || true
