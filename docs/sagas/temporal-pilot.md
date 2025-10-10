# Temporal Pilot Plan

## Objectives

- Orchestrate the order-payment-inventory-notification workflow using Temporal to validate long-running saga orchestration.
- Demonstrate retries, compensation hooks, and state visibility complementary to the existing event-choreographed flow.

## Architecture

The saga will be implemented using a distributed worker model:

- **Workflow Client**: The `orders-service` will host the Temporal `WorkflowClient`. When a new order is created, this service will be responsible for starting the `OrderFulfillmentWorkflow`.
- **Activity Workers**: Each microservice responsible for a step in the saga will host a Temporal Worker to execute the relevant activities.
  - `payments-service` will implement the `PaymentActivity`.
  - `inventory-service` will implement the `InventoryActivity`.
  - `notification-service` will implement the `NotificationActivity`.

1. **Activities** (to be implemented):
   - `ProcessPaymentActivity` (idempotent, delegates to payments-service API/gRPC stub).
   - `ReserveInventoryActivity` (delegates to inventory stock API / future gRPC).
   - `SendNotificationActivity` (delegates to notification service channel dispatch).
2. **Compensation**:
   - On payment failure, emit `PaymentFailedEvent` and mark saga failed.
   - On inventory failure, trigger `inventoryService.release` and mark saga failed.
   - On notification failure, retry per policy, then mark `NOTIFICATION_FAILED`.
3. **Retries & Timeouts**:
   - Payment: exponential backoff, max 3 attempts, 15s timeout.
   - Inventory: retry twice with jitter, 10s timeout.
   - Notification: reuse channel-level retry configuration.
4. **Telemetry**:
   - Record Temporal metrics (queue size, workflow latency) and feed into Grafana dashboard in Phase 5.

## Deliverables

## Implementation Plan

1. **Shared Components**: Granular activity interfaces (`PaymentActivity`, `InventoryActivity`, `NotificationActivity`) are defined in the `common-temporal` module.
2. **Workflow Orchestration**: The `OrderFulfillmentWorkflowImpl` is implemented in the `temporal-pilot` service, which acts as a dedicated workflow worker.
3. **Workflow Trigger**: The `orders-service` uses a `WorkflowClient` to start the saga when an order is created.
4. **Activity Workers**: The `payments-service`, `inventory-service`, and `notification-service` each host a worker configured to handle their specific activity.
