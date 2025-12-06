package com.example.temporal.workflow

import com.example.temporal.OrderFulfillmentWorkflow
import com.example.temporal.TaskQueues
import com.example.temporal.activity.InventoryActivity
import com.example.temporal.activity.InventoryReservationResult
import com.example.temporal.activity.NotificationActivity
import com.example.temporal.activity.NotificationResult
import com.example.temporal.activity.PaymentActivity
import com.example.temporal.activity.PaymentResult
import com.example.temporal.activity.RefundPaymentActivity
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.failure.ActivityFailure
import io.temporal.workflow.Saga
import io.temporal.workflow.Workflow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Implementation of the OrderFulfillmentWorkflow using Temporal Saga pattern.
 *
 * This workflow orchestrates the order fulfillment process across multiple services:
 * 1. Payment processing
 * 2. Inventory reservation
 * 3. Notification delivery
 *
 * Compensations are configured to run in parallel for faster recovery.
 * The workflow provides detailed logging and error handling for operational visibility.
 */

data class CreateSuccessResultParams(
    val orderId: UUID,
    val paymentResult: PaymentResult,
    val inventoryResult: InventoryReservationResult,
    val notificationResult: NotificationResult,
    val steps: MutableList<WorkflowStep>,
    val workflowStartTime: Instant,
)

@Component
@Suppress("TooGenericExceptionCaught")
class OrderFulfillmentWorkflowImpl : OrderFulfillmentWorkflow {
    private val logger = LoggerFactory.getLogger(OrderFulfillmentWorkflowImpl::class.java)

    companion object {
        private const val DEFAULT_CUSTOMER_EMAIL = "customer@example.com"
    }

    // Payment activity configuration - 3 attempts with exponential backoff, 30s timeout
    private val paymentRetryOptions =
        RetryOptions
            .newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(10))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(3)
            .setDoNotRetry("BusinessRuleException", "ValidationException")
            .build()

    private val paymentActivityOptions =
        ActivityOptions
            .newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(paymentRetryOptions)
            .build()

    // Inventory activity configuration - 3 attempts with exponential backoff, 15s timeout
    private val inventoryRetryOptions =
        RetryOptions
            .newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(8))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(3)
            .setDoNotRetry("InsufficientInventoryException")
            .build()

    private val inventoryActivityOptions =
        ActivityOptions
            .newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(15))
            .setRetryOptions(inventoryRetryOptions)
            .build()

    // Notification activity configuration - 2 attempts, 10s timeout
    private val notificationRetryOptions =
        RetryOptions
            .newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(5))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(2)
            .build()

    private val notificationActivityOptions =
        ActivityOptions
            .newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(notificationRetryOptions)
            .build()

    // Refund activity configuration - 2 attempts, 20s timeout (for compensation)
    private val refundRetryOptions =
        RetryOptions
            .newBuilder()
            .setInitialInterval(Duration.ofSeconds(2))
            .setMaximumInterval(Duration.ofSeconds(10))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(2)
            .build()

    private val refundActivityOptions =
        ActivityOptions
            .newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(20))
            .setRetryOptions(refundRetryOptions)
            .build()

    @Suppress("LongMethod")
    override fun execute(orderId: UUID): OrderFulfillmentResult {
        val workflowStartTime = Instant.now()
        val steps = mutableListOf<WorkflowStep>()

        logger.info("Starting OrderFulfillmentWorkflow for orderId: $orderId")

        val saga =
            Saga(
                Saga.Options
                    .Builder()
                    .setParallelCompensation(true)
                    .build(),
            )

        var result: OrderFulfillmentResult? = null
        var shouldContinue = true
        var paymentResult: PaymentResult? = null
        var inventoryResult: InventoryReservationResult? = null

        try {
            if (shouldContinue) {
                paymentResult = processPaymentStep(orderId, saga, steps)
                if (!paymentResult.success) {
                    result =
                        handleWorkflowFailure(
                            orderId,
                            paymentResult.message ?: "Payment processing failed",
                            saga,
                            steps,
                        )
                    shouldContinue = false
                }
            }

            if (shouldContinue) {
                inventoryResult = processInventoryStep(orderId, saga, steps)
                if (!inventoryResult.success) {
                    result =
                        handleWorkflowFailure(
                            orderId,
                            inventoryResult.message ?: "Inventory reservation failed",
                            saga,
                            steps,
                        )
                    shouldContinue = false
                }
            }

            if (shouldContinue) {
                val notificationResult = processNotificationStep(orderId, steps)
                result =
                    createSuccessResult(
                        CreateSuccessResultParams(
                            orderId = orderId,
                            paymentResult = paymentResult!!,
                            inventoryResult = inventoryResult!!,
                            notificationResult = notificationResult,
                            steps = steps,
                            workflowStartTime = workflowStartTime,
                        ),
                    )
            }
        } catch (e: ActivityFailure) {
            logger.error("Activity failure in OrderFulfillmentWorkflow for orderId: $orderId", e)
            result =
                handleActivityFailure(orderId, e, saga, steps, workflowStartTime)
        } catch (e: Exception) {
            logger.error("Unexpected error in OrderFulfillmentWorkflow for orderId: $orderId", e)
            result =
                handleUnexpectedError(orderId, e, steps, workflowStartTime)
        }

        return result ?: handleUnexpectedError(
            orderId,
            IllegalStateException("Unexpected null result"),
            steps,
            workflowStartTime,
        )
    }

    private fun processPaymentStep(
        orderId: UUID,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): PaymentResult {
        val paymentStep = WorkflowStep.started("Payment Processing")
        steps.add(paymentStep)
        saga.addCompensation { refundActivities.refundPayment(orderId) }

        val paymentStartTime = Instant.now()
        val paymentResult = paymentActivities.processPayment(orderId)
        val paymentDuration = Duration.between(paymentStartTime, Instant.now()).toMillis()

        return if (paymentResult.success) {
            val completedPaymentStep =
                WorkflowStep.completed(
                    "Payment Processing",
                    "Payment successful",
                    paymentDuration,
                )
            steps.add(completedPaymentStep)
            logger.info("Payment completed for orderId: $orderId, paymentId: ${paymentResult.paymentId}")
            paymentResult
        } else {
            val failedStep =
                WorkflowStep.failed(
                    "Payment Processing",
                    paymentResult.message ?: "Payment failed",
                    paymentDuration,
                )
            steps.add(failedStep)
            logger.error("Payment failed for orderId: $orderId, reason: ${paymentResult.message}")
            paymentResult
        }
    }

    private fun processInventoryStep(
        orderId: UUID,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): InventoryReservationResult {
        val inventoryStep = WorkflowStep.started("Inventory Reservation")
        steps.add(inventoryStep)
        saga.addCompensation(inventoryActivities::releaseInventory, orderId)

        val inventoryStartTime = Instant.now()
        val inventoryResult = inventoryActivities.reserveInventory(orderId)
        val inventoryDuration = Duration.between(inventoryStartTime, Instant.now()).toMillis()

        return if (inventoryResult.success) {
            val completedInventoryStep =
                WorkflowStep.completed(
                    "Inventory Reservation",
                    "Inventory reserved",
                    inventoryDuration,
                )
            steps.add(completedInventoryStep)
            logger.info("Inventory reserved for orderId: $orderId, reservationId: ${inventoryResult.reservationId}")
            inventoryResult
        } else {
            val failedStep =
                WorkflowStep.failed(
                    "Inventory Reservation",
                    inventoryResult.message ?: "Inventory reservation failed",
                    inventoryDuration,
                )
            steps.add(failedStep)
            logger.error("Inventory reservation failed for orderId: $orderId, reason: ${inventoryResult.message}")
            inventoryResult
        }
    }

    private fun processNotificationStep(
        orderId: UUID,
        steps: MutableList<WorkflowStep>,
    ): NotificationResult {
        val notificationStep = WorkflowStep.started("Order Confirmation")
        steps.add(notificationStep)

        val notificationStartTime = Instant.now()
        val notificationResult = notificationActivities.sendOrderConfirmation(orderId, DEFAULT_CUSTOMER_EMAIL)
        val notificationDuration = Duration.between(notificationStartTime, Instant.now()).toMillis()

        val completedNotificationStep =
            if (notificationResult.success) {
                WorkflowStep.completed("Order Confirmation", "Confirmation sent", notificationDuration)
            } else {
                WorkflowStep.failed(
                    "Order Confirmation",
                    notificationResult.message ?: "Notification failed",
                    notificationDuration,
                )
            }
        steps.add(completedNotificationStep)

        return notificationResult
    }

    private fun createSuccessResult(params: CreateSuccessResultParams): OrderFulfillmentResult {
        val executionDuration = Duration.between(params.workflowStartTime, Instant.now()).toMillis()
        val successParams =
            SuccessParams(
                orderId = params.orderId,
                paymentId = params.paymentResult.paymentId!!,
                reservationId = params.inventoryResult.reservationId!!,
                confirmationNotificationId =
                    if (params.notificationResult.success) {
                        params.notificationResult.notificationId
                    } else {
                        null
                    },
                steps = params.steps,
                executionDuration = executionDuration,
            )

        logger.info("OrderFulfillmentWorkflow completed successfully for orderId={}", params.orderId)
        return OrderFulfillmentResult.success(successParams)
    }

    private fun handleWorkflowFailure(
        orderId: UUID,
        failureReason: String,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): OrderFulfillmentResult {
        try {
            saga.compensate()
        } catch (e: Exception) {
            logger.error("Compensation failed during workflow failure for orderId: $orderId", e)
            return OrderFulfillmentResult.failure(
                orderId = orderId,
                failureReason = failureReason,
                compensationIssues = listOf("Compensation failed: ${e.message}"),
                steps = steps,
            )
        }

        return OrderFulfillmentResult.failure(orderId = orderId, failureReason = failureReason, steps = steps)
    }

    private fun handleActivityFailure(
        orderId: UUID,
        e: ActivityFailure,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
        workflowStartTime: Instant,
    ): OrderFulfillmentResult {
        val failedStep =
            WorkflowStep.failed(
                "Activity Execution",
                e.message ?: "Activity failed",
                Duration.between(workflowStartTime, Instant.now()).toMillis(),
            )
        steps.add(failedStep)

        try {
            saga.compensate()
        } catch (compensationException: Exception) {
            logger.error("Compensation failed for orderId: $orderId", compensationException)
            return OrderFulfillmentResult.failure(
                orderId = orderId,
                failureReason = e.message ?: "Activity and compensation failed",
                compensationIssues = listOf("Compensation failed: ${compensationException.message}"),
                steps = steps,
            )
        }

        return OrderFulfillmentResult.failure(
            orderId = orderId,
            failureReason = e.message ?: "Activity execution failed",
            steps = steps,
        )
    }

    private fun handleUnexpectedError(
        orderId: UUID,
        e: Exception,
        steps: MutableList<WorkflowStep>,
        workflowStartTime: Instant,
    ): OrderFulfillmentResult {
        val failedStep =
            WorkflowStep.failed(
                "Workflow Execution",
                e.message ?: "Unexpected error",
                Duration.between(workflowStartTime, Instant.now()).toMillis(),
            )
        steps.add(failedStep)

        return OrderFulfillmentResult.failure(
            orderId = orderId,
            failureReason = e.message ?: "Unexpected workflow error",
            steps = steps,
        )
    }

    // Activity stubs with proper task queue configuration
    private val paymentActivities =
        Workflow.newActivityStub(
            PaymentActivity::class.java,
            paymentActivityOptions.toBuilder().setTaskQueue(TaskQueues.PAYMENTS_TASK_QUEUE).build(),
        )

    private val inventoryActivities =
        Workflow.newActivityStub(
            InventoryActivity::class.java,
            inventoryActivityOptions.toBuilder().setTaskQueue(TaskQueues.INVENTORY_TASK_QUEUE).build(),
        )

    private val notificationActivities =
        Workflow.newActivityStub(
            NotificationActivity::class.java,
            notificationActivityOptions.toBuilder().setTaskQueue(TaskQueues.NOTIFICATIONS_TASK_QUEUE).build(),
        )

    private val refundActivities =
        Workflow.newActivityStub(
            RefundPaymentActivity::class.java,
            refundActivityOptions.toBuilder().setTaskQueue(TaskQueues.PAYMENTS_TASK_QUEUE).build(),
        )

    // Workflow state for query and signal methods
    private var currentStatus: WorkflowStatus = WorkflowStatus.PENDING
    private var cancelled: Boolean = false
    private var cancellationReason: String? = null

    override fun getStatus(): WorkflowStatus = currentStatus

    override fun cancel(reason: String) {
        cancelled = true
        cancellationReason = reason
        currentStatus = WorkflowStatus.CANCELLED
        logger.info("Workflow cancellation requested: $reason")
    }
}
