# Saga Dashboard

## Overview

Placeholder for Grafana dashboard that will visualize saga metrics collected through `SagaMetricsRecorder`.

## Metrics to Display

### Saga Step Processing

- Metric: `saga.step.processed`
- Tags: `sagaType`, `step`, `state`
- Visualization: Time series showing step processing rate over time

### Saga Completion Rate

- Metric: `saga.step.processed` with `state=COMPLETED`
- Visualization: Percentage of successfully completed sagas over time

### Saga Failure Analysis

- Metric: `saga.step.processed` with `state=FAILED`
- Visualization: Stacked bar chart showing failure reasons by saga type

### Compensation Tracking

- Metric: `saga.step.processed` with `state=COMPENSATING`
- Visualization: Counter of compensation actions triggered

## Dashboard Layout

### Row 1: Overall Saga Health

- Total saga instances over time
- Success rate (%)
- Failure rate (%)
- Compensation rate (%)

### Row 2: Saga Performance by Type

- Processing time distribution by saga type
- Step completion rate per service
- Error rate per saga step

### Row 3: Failure Analysis

- Failure reasons breakdown
- Compensation triggers by service
- Retry attempts before failure/compensation

### Row 4: Real-time Monitoring

- Active sagas
- Recently completed sagas
- Recently failed sagas with error details

## Alerting Rules

### High Failure Rate

- Alert when failure rate exceeds 5% over a 5-minute window

### Compensation Spike

- Alert when compensation rate exceeds 2% over a 5-minute window

### Stuck Sagas

- Alert when sagas remain in IN_PROGRESS state for more than 30 minutes

## Implementation Notes

This dashboard will be implemented in Phase 5 alongside the Grafana integration. The metrics are already being collected through `SagaMetricsRecorder` in all services.
