# Temporal Workflow Orchestration

This document describes the Temporal workflow implementation in the microservices project, including architecture, best practices compliance, and operational guidance.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Temporal Server (port 7233)                        │
│                    temporalio/auto-setup:1.29.1 + PostgreSQL                │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ gRPC (1.77.0)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Temporal UI (port 8088)                              │
│                         temporalio/ui:2.43.3                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│   temporal-pilot  │   │ payments-service  │   │ inventory-service │
│   (Workflow Worker)│   │ (Activity Worker) │   │ (Activity Worker) │
│   Port: 8089      │   │   Port: 8084      │   │   Port: 8085      │
└───────────────────┘   └───────────────────┘   └───────────────────┘
```

## Version Matrix

| Component | Version | Notes |
|-----------|---------|-------|
| Temporal Server | 1.29.1 | auto-setup image with PostgreSQL |
| Temporal UI | 2.43.3 | Web-based workflow monitoring |
| temporal-shaded | 1.32.1 | Shaded SDK with relocated gRPC/Netty/Protobuf |
| temporal-spring-boot-starter | 1.32.1 | Spring Boot integration |
| Project gRPC | 1.77.0 | Used by common-proto and other services |

**Note**: We use `temporal-shaded` instead of `temporal-sdk` to avoid gRPC version conflicts. The shaded artifact relocates gRPC/Netty/Protobuf to `io.temporal.shaded.*` packages, allowing the project to use gRPC 1.77.0 independently.

## Task Queues

| Queue Name | Purpose | Worker Service |
|------------|---------|----------------|
| OrderFulfillmentWorkflowTaskQueue | Workflow orchestration | temporal-pilot |
| PaymentsTaskQueue | Payment processing activities | payments-service |
| InventoryTaskQueue | Inventory reservation activities | inventory-service |
| NotificationsTaskQueue | Notification delivery activities | notification-service |

## Workflow Definition

### Interface (`common-temporal/OrderFulfillmentWorkflow.kt`)

```kotlin
@WorkflowInterface
interface OrderFulfillmentWorkflow {
    @WorkflowMethod
    fun execute(orderId: UUID): OrderFulfillmentResult

    @QueryMethod
    fun getStatus(): WorkflowStatus

    @SignalMethod
    fun cancel(reason: String)
}
```

- **@WorkflowMethod**: Main entry point - executes the order fulfillment saga
- **@QueryMethod**: Query current workflow status without affecting execution
- **@SignalMethod**: Send cancellation signal to running workflow

## Saga Pattern Implementation

The workflow implements the Saga pattern with 3 steps and parallel compensation:

```
1. Payment Processing → PaymentsTaskQueue
   ├── getOrderAmount(orderId)
   └── processPayment(orderId, amount)
   
2. Inventory Reservation → InventoryTaskQueue
   └── reserveInventory(orderId)
   
3. Notification → NotificationsTaskQueue
   └── sendOrderConfirmation(orderId, email)
```

### Compensation (Rollback)

If any step fails, the Saga compensates in parallel:
- `refundPayment(orderId)` - Refunds the payment
- `releaseInventory(orderId)` - Releases reserved inventory

```kotlin
val saga = Saga(
    Saga.Options.Builder()
        .setParallelCompensation(true)  // Run compensations in parallel
        .build()
)
```

## Activity Configuration

### Timeout and Retry Policies

| Activity | Start-to-Close | Max Attempts | Initial Interval | Max Interval | Backoff |
|----------|----------------|--------------|------------------|--------------|---------|
| Payment | 30s | 3 | 1s | 10s | 2.0x |
| Inventory | 15s | 3 | 1s | 8s | 2.0x |
| Notification | 10s | 2 | 1s | 5s | 2.0x |
| Refund | 20s | 2 | 2s | 10s | 2.0x |

### Non-Retryable Errors

- Payment: `BusinessRuleException`, `ValidationException`
- Inventory: `InsufficientInventoryException`



## Best Practices Compliance Analysis

### ✅ Following Best Practices

| Practice | Implementation | Status |
|----------|----------------|--------|
| **Saga Pattern** | Using `Saga` class with parallel compensation | ✅ Correct |
| **Activity Stubs** | Using `Workflow.newActivityStub()` with task queue routing | ✅ Correct |
| **Retry Policies** | Configured per activity with exponential backoff | ✅ Correct |
| **Non-Retryable Errors** | Business exceptions excluded from retry | ✅ Correct |
| **Spring Boot Starter** | Using `temporal-spring-boot-starter` for auto-config | ✅ Correct |
| **Activities as Beans** | Activities are `@Component` with DI | ✅ Correct |
| **Separate Task Queues** | Each service has dedicated task queue | ✅ Correct |
| **WorkerFactory Lifecycle** | Using `destroyMethod="shutdown"` | ✅ Correct |
| **Configuration Externalized** | Using `application.yml` for connection settings | ✅ Correct |
| **Workflow Determinism** | Non-deterministic code in activities only | ✅ Correct |

### ⚠️ Recommendations for Improvement

| Area | Current | Recommended | Priority |
|------|---------|-------------|----------|
| **Schedule-to-Close Timeout** | Not explicitly set | Set generous timeout (hours) for long-running sagas | Medium |
| **Heartbeat Timeout** | Not configured | Add heartbeats for long-running activities | Low |
| **Workflow Versioning** | Not implemented | Add versioning for workflow evolution | Medium |
| **Idempotency Keys** | Implicit via orderId | Make compensation idempotent explicitly | Medium |
| **Metrics/Monitoring** | Basic counters | Add detailed Temporal metrics to Prometheus | Medium |

## Worker Configuration

### temporal-pilot (Workflow Worker)

```kotlin
@Configuration
@ConditionalOnProperty(name = ["temporal.worker.enabled"], havingValue = "true", matchIfMissing = true)
class TemporalWorkerConfig {
    @Bean(destroyMethod = "shutdown")
    fun workerFactory(workflowClient: WorkflowClient): WorkerFactory {
        val factory = WorkerFactory.newInstance(workflowClient)
        val worker = factory.newWorker(TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE)
        worker.registerWorkflowImplementationTypes(OrderFulfillmentWorkflowImpl::class.java)
        factory.start()
        return factory
    }
}
```

### Service Activity Workers (e.g., payments-service)

```kotlin
@Configuration
@ConditionalOnProperty(name = ["temporal.worker.enabled"], havingValue = "true", matchIfMissing = false)
class TemporalWorkerConfig {
    @Bean(destroyMethod = "shutdown")
    fun workerFactory(
        workflowClient: WorkflowClient,
        paymentActivityImpl: PaymentActivityImpl,
        refundPaymentActivityImpl: RefundPaymentActivityImpl,
    ): WorkerFactory {
        val factory = WorkerFactory.newInstance(workflowClient)
        val worker = factory.newWorker(TaskQueues.PAYMENTS_TASK_QUEUE)
        worker.registerActivitiesImplementations(paymentActivityImpl, refundPaymentActivityImpl)
        factory.start()
        return factory
    }
}
```

## Configuration

### temporal-pilot/application.yml

```yaml
spring:
  temporal:
    connection:
      target: ${TEMPORAL_TARGET:127.0.0.1:7233}
    namespace: default
```

### Docker Compose (infra/compose.yml)

```yaml
temporal:
  image: temporalio/auto-setup:1.29.1
  environment:
    - DB=postgres12
    - DB_PORT=5432
    - POSTGRES_USER=postgres
    - POSTGRES_PWD=postgres
    - POSTGRES_SEEDS=postgres
  ports:
    - "7233:7233"
  depends_on:
    postgres:
      condition: service_healthy

temporal-ui:
  image: temporalio/ui:2.43.3
  environment:
    - TEMPORAL_ADDRESS=temporal:7233
  ports:
    - "8088:8080"
```

## Execution Flow

```
1. orders-service creates order → starts workflow
   └── WorkflowClient.start(OrderFulfillmentWorkflow::execute, orderId)

2. Temporal Server schedules workflow task
   └── Task Queue: OrderFulfillmentWorkflowTaskQueue

3. temporal-pilot picks up workflow task
   └── Executes OrderFulfillmentWorkflowImpl.execute()

4. Workflow calls paymentActivities.processPayment()
   └── Temporal schedules activity task on PaymentsTaskQueue

5. payments-service picks up activity task
   └── PaymentActivityImpl.processPayment() executes
   └── Returns PaymentResult to Temporal

6. Workflow continues with inventoryActivities.reserveInventory()
   └── Same pattern: task → worker → result

7. Workflow completes → OrderFulfillmentResult returned
```

## Operational Commands

### Start Temporal Stack

```bash
# Start infrastructure with Temporal
docker compose -f infra/compose.yml --profile services up -d temporal temporal-ui

# Verify Temporal health
docker exec temporal tctl cluster health
```

### Monitor Workflows

```bash
# List running workflows
docker exec temporal tctl workflow list

# Describe specific workflow
docker exec temporal tctl workflow describe -w order-fulfillment-<orderId>

# Query workflow status
docker exec temporal tctl workflow query -w order-fulfillment-<orderId> -qt getStatus
```

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Workflow stuck in RUNNING | Activity worker not running | Start service with `temporal.worker.enabled=true` |
| Activity timeout | Worker overloaded or crashed | Check worker logs, scale workers |
| gRPC connection refused | Temporal server not ready | Wait for health check, check network |
| NoClassDefFoundError gRPC | Version mismatch | Use `temporal-shaded` instead of `temporal-sdk` |

**gRPC Version Conflicts**: If you encounter gRPC-related `NoClassDefFoundError` or `ClassNotFoundException`, ensure you're using `temporal-shaded` which bundles and relocates gRPC dependencies. Do NOT manually exclude gRPC from `temporal-sdk` - use the shaded artifact instead.

## File Structure

```
common-temporal/
├── src/main/kotlin/com/example/temporal/
│   ├── OrderFulfillmentWorkflow.kt      # Workflow interface
│   ├── TaskQueues.kt                     # Task queue constants
│   ├── activity/
│   │   ├── PaymentActivity.kt           # Activity interfaces
│   │   ├── InventoryActivity.kt
│   │   ├── NotificationActivity.kt
│   │   └── RefundPaymentActivity.kt
│   └── workflow/
│       ├── OrderFulfillmentWorkflowImpl.kt  # Workflow implementation
│       ├── OrderFulfillmentResult.kt
│       └── WorkflowStatus.kt

temporal-pilot/
├── src/main/kotlin/com/example/temporalpilot/
│   ├── config/TemporalWorkerConfig.kt   # Worker configuration
│   └── implementation/
│       └── OrderFulfillmentWorkflowImpl.kt  # Local workflow impl
└── src/main/resources/application.yml

services/payments-service/
├── src/main/kotlin/com/example/payments/
│   ├── activity/
│   │   ├── PaymentActivityImpl.kt       # Activity implementation
│   │   └── RefundPaymentActivityImpl.kt
│   └── config/TemporalWorkerConfig.kt   # Activity worker config
```

## References

- [Temporal Java SDK Documentation](https://docs.temporal.io/develop/java/)
- [Temporal Spring Boot Integration](https://docs.temporal.io/develop/java/spring-boot-integration)
- [Temporal Best Practices](https://docs.temporal.io/best-practices)
- [Saga Pattern in Temporal](https://docs.temporal.io/develop/java/failure-detection#saga)
