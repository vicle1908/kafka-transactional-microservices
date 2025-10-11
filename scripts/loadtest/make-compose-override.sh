#!/usr/bin/env bash
set -euo pipefail

mkdir -p infra

cat > infra/compose.loadtest.yml << 'EOF'
version: "3.9"

services:
  orders-service:
    image: orders-service:test
    container_name: orders-service-loadtest
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-test:5432/test
      - SPRING_DATASOURCE_USERNAME=app
      - SPRING_DATASOURCE_PASSWORD=app
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-test:9092
      - SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL=http://schema-registry-test:8081
      - MANAGEMENT_OTLP_ENDPOINT=http://otel-collector:4317
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
      - MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
    depends_on:
      postgres-test:
        condition: service_healthy
      kafka-test:
        condition: service_healthy
      schema-registry-test:
        condition: service_healthy
    ports:
      - "8081:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    volumes:
      - ./logs:/app/logs
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M

  payments-service:
    image: payments-service:test
    container_name: payments-service-loadtest
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-test:5432/test
      - SPRING_DATASOURCE_USERNAME=app
      - SPRING_DATASOURCE_PASSWORD=app
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-test:9092
      - SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL=http://schema-registry-test:8081
      - MANAGEMENT_OTLP_ENDPOINT=http://otel-collector:4317
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
      - MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
    depends_on:
      postgres-test:
        condition: service_healthy
      kafka-test:
        condition: service_healthy
      schema-registry-test:
        condition: service_healthy
    ports:
      - "8082:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    volumes:
      - ./logs:/app/logs
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M

  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.137.0
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./infra/otel/otel-collector-config.yaml:/etc/otel-collector-config.yaml:ro
    ports:
      - "4317:4317"
      - "4318:4318"
      - "13133:13133"
    depends_on:
      - jaeger
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:13133"]
      interval: 30s
      timeout: 10s
      retries: 3

  jaeger:
    image: jaegertracing/all-in-one:1.74.0
    ports:
      - "16686:16686"
      - "14250:14250"
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:14269/metrics"]
      interval: 30s
      timeout: 10s
      retries: 3

  prometheus:
    image: prom/prometheus:v3.6.0
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=1h'
      - '--web.enable-lifecycle'
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  default:
    name: loadtest-network
EOF
