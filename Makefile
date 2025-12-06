# Standardized local orchestration

ROOT_DIR := $(shell pwd)
COMPOSE := docker compose -f infra/compose.yml

.PHONY: up down restart ps logs migrate connectors smoke clean-volumes temporal-ui temporal-logs

up:
	$(COMPOSE) --profile local up -d

down:
	$(COMPOSE) down

restart:
	$(COMPOSE) --profile local up -d --force-recreate

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs --no-color --tail=200

# Run Flyway migrations (one-off containers) after Postgres is healthy
migrate:
	$(COMPOSE) --profile migrate up --abort-on-container-exit flyway-orders flyway-payments flyway-inventory flyway-notification
	$(COMPOSE) --profile migrate rm -f flyway-orders flyway-payments flyway-inventory flyway-notification

# Register or update Debezium connectors
connectors:
	bash scripts/connectors/register_all.sh

# End-to-end smoke test for outbox -> Kafka topic
smoke:
	bash scripts/smoke/outbox_orders.sh

# DANGER: remove data volumes (fresh start)
clean-volumes:
	$(COMPOSE) down -v

# Temporal commands
temporal-ui:
	@echo "Temporal Web UI: http://localhost:8089"
	@echo "Temporal gRPC: localhost:7233"

temporal-logs:
	$(COMPOSE) logs --no-color --tail=100 temporal