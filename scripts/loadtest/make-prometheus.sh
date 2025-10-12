#!/usr/bin/env bash
set -euo pipefail

mkdir -p monitoring

if [ ! -f "monitoring/prometheus.yml" ]; then
cat > monitoring/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'orders-service'
    static_configs:
      - targets: ['orders-service:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 5s

  - job_name: 'payments-service'
    static_configs:
      - targets: ['payments-service:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 5s

  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8888']
    scrape_interval: 10s
EOF
fi
