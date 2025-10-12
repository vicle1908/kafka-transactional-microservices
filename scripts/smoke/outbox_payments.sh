#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/compose.yml"

# Insert an outbox row into payments DB and consume from outbox.payments

echo "Inserting a payments outbox row"
read -r -d '' INSERT_SQL <<'SQL'
INSERT INTO public.outbox (
  id, aggregate_id, aggregate_type, event_type, payload, headers, occurred_at, version
) VALUES (
  gen_random_uuid(), 'payment-smoke', 'payments', 'PaymentCompleted', '{"paymentId":"payment-smoke","amount":42}', '{"source":"smoke"}', now(), 0
);
SQL

docker compose -f "$COMPOSE_FILE" exec -T postgres bash -lc "psql -U ${POSTGRES_USER:-app} -d payments -v ON_ERROR_STOP=1 <<'EOF'
$INSERT_SQL
EOF" >/dev/null

echo "Consuming from outbox.payments (max 1 message, 10s timeout)"
docker compose -f "$COMPOSE_FILE" exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic outbox.payments \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 10000 || true
