#!/usr/bin/env bash
set -euo pipefail

mkdir -p reports
REPORT_FILE="reports/load-test-report-$(date +%Y%m%d-%H%M%S).md"

cat > "$REPORT_FILE" << EOF
# Load Test Report

## Test Configuration
- **Date**: $(date)
- **Branch**: ${GITHUB_REF_NAME:-unknown}
- **Commit**: ${GITHUB_SHA:-unknown}
- **Load Level**: ${LOAD_LEVEL:-unknown}
- **Duration**: ${LOAD_TEST_DURATION:-unknown} minutes
- **Runner**: ${RUNNER_OS:-unknown}

## Test Environment
- **Infrastructure**: Docker Compose (Test Environment)
- **Services**: Orders, Payments
- **Database**: PostgreSQL 18
- **Message Broker**: Apache Kafka 4.1.0
- **Schema Registry**: Confluent Schema Registry
- **CDC**: Debezium 3.3
- **Observability**: OpenTelemetry + Jaeger

## Key Metrics

TODO: Extract actual metrics from k6 results and add them here.

## EOS Validation Results

TODO: Add EOS validation results from outbox analysis.

## Recommendations

TODO: Add recommendations based on test results.

---
*Report generated automatically by GitHub Actions*
EOF

# Optional: add k6 metrics to report if available
if [ -f "load-summary.json" ]; then
  echo "\n\n<!-- k6 summary placeholder -->" >> "$REPORT_FILE"
fi

echo "✅ Report generated: $REPORT_FILE"