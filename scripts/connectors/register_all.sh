#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/infra/compose.yml"
CONNECT_URL="http://localhost:${DEBEZIUM_PORT:-8083}"

register() {
  local name="$1"; local file="$2";
  echo "Registering $name"
  local code
  code=$(curl -s -o /tmp/connector.json -w "%{http_code}" -X POST -H 'Content-Type: application/json' \
    --data @"$file" "$CONNECT_URL/connectors" || true)
  if [[ "$code" == "201" || "$code" == "200" ]]; then
    echo "Created $name"; jq . < /tmp/connector.json || cat /tmp/connector.json; return 0
  fi
  if [[ "$code" == "409" ]]; then
    echo "$name exists, updating config"
    cfg=$(jq -c '.config' "$file")
    code=$(curl -s -o /tmp/connector.json -w "%{http_code}" -X PUT -H 'Content-Type: application/json' \
      --data "$cfg" "$CONNECT_URL/connectors/${name}/config" || true)
    echo "Update $name -> $code"; jq . < /tmp/connector.json || cat /tmp/connector.json; return 0
  fi
  echo "Unexpected response ($code) for $name:"; cat /tmp/connector.json; return 1
}

# Ensure stack is up
echo "Ensuring Kafka stack is running"
docker compose -f "$COMPOSE_FILE" up -d kafka schema-registry debezium-connect >/dev/null

# Wait for Connect REST
for i in {1..60}; do
  if curl -sf "$CONNECT_URL/" >/dev/null; then echo "Debezium Connect is ready"; break; fi
  sleep 2; if [[ $i -eq 60 ]]; then echo "Connect did not become ready"; exit 1; fi
done

register orders-outbox-connector "$ROOT_DIR/infra/debezium/connectors/orders-outbox-connector.json"
register payments-outbox-connector "$ROOT_DIR/infra/debezium/connectors/payments-outbox-connector.json"
register inventory-outbox-connector "$ROOT_DIR/infra/debezium/connectors/inventory-outbox-connector.json"
register notification-outbox-connector "$ROOT_DIR/infra/debezium/connectors/notification-outbox-connector.json"

echo "Connector statuses:"
for n in orders-outbox-connector payments-outbox-connector inventory-outbox-connector notification-outbox-connector; do
  echo "-- $n"; curl -s "$CONNECT_URL/connectors/$n/status" | jq . || curl -s "$CONNECT_URL/connectors/$n/status"; echo
done
