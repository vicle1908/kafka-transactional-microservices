package com.example.temporalpilot.implementation

import com.example.temporal.OrderFulfillmentWorkflow
import com.example.temporal.TaskQueues
import com.example.temporal.activity.InventoryActivity
import com.example.temporal.activity.InventoryReservationResult
import com.example.temporal.activity.NotificationActivity
import com.example.temporal.activity.NotificationResult
import com.example.temporal.activity.PaymentActivity
import com.example.temporal.activity.PaymentResult
import com.example.temporal.activity.RefundPaymentActivity
import com.example.temporal.workflow.OrderFulfillmentResult
import com.example.temporal.workflow.WorkflowStep
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Saga
import io.temporal.workflow.Workflow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
class OrderFulfillmentWorkflowImpl : OrderFulfillmentWorkflow {
    private val logger = LoggerFactory.getLogger(OrderFulfillmentWorkflowImpl::class.java)

    private val retryOptions =
        RetryOptions
            .newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumAttempts(3)
            .build()

    private val activityOptions =
        ActivityOptions
            .newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(retryOptions)
            .build()

    private val paymentActivities =
        Workflow.newActivityStub(
            PaymentActivity::class.java,
            activityOptions.toBuilder().setTaskQueue(TaskQueues.PAYMENTS_TASK_QUEUE).build(),
        )
    private val inventoryActivities =
        Workflow.newActivityStub(
            InventoryActivity::class.java,
            activityOptions.toBuilder().setTaskQueue(TaskQueues.INVENTORY_TASK_QUEUE).build(),
        )
    private val notificationActivities =
        Workflow.newActivityStub(
            NotificationActivity::class.java,
            activityOptions.toBuilder().setTaskQueue(TaskQueues.NOTIFICATIONS_TASK_QUEUE).build(),
        )
    private val refundPaymentActivities =
        Workflow.newActivityStub(
            RefundPaymentActivity::class.java,
            activityOptions.toBuilder().setTaskQueue(TaskQueues.PAYMENTS_TASK_QUEUE).build(),
        )

    override fun start(orderId: UUID) {
        execute(orderId)
    }

    override fun execute(orderId: UUID): OrderFulfillmentResult {
        val workflowStart = Instant.now()
        val steps = mutableListOf<WorkflowStep>()
        val saga = createSaga()

        var result: OrderFulfillmentResult? = null
        var shouldContinue = true
        var paymentResult: PaymentResult? = null
        var inventoryResult: InventoryReservationResult? = null

        try {
            if (shouldContinue) {
                paymentResult = processPayment(orderId, saga, steps)
                if (!paymentResult.success) {
                    result = handleWorkflowFailure(orderId, paymentResult.message, saga, steps)
                    shouldContinue = false
                }
            }

            if (shouldContinue) {
                inventoryResult = processInventory(orderId, saga, steps)
                if (!inventoryResult.success) {
                    result = handleWorkflowFailure(orderId, inventoryResult.message, saga, steps)
                    shouldContinue = false
                }
            }

            if (shouldContinue) {
                val notificationResult = processNotification(orderId, steps)
                result = createSuccessResult(
                    orderId,
                    paymentResult!!,
                    inventoryResult!!,
                    notificationResult,
                    workflowStart,
                    steps,
                )
            }
        } catch (ex: Exception) {
            result = handleUnexpectedError(orderId, ex, saga, steps)
        }

        return result ?: handleUnexpectedError(
            orderId,
            IllegalStateException("Unexpected null result"),
            saga,
            steps,
        )
    }

    private fun createSaga(): Saga =
        Saga(
            Saga.Options
                .Builder()
                .setParallelCompensation(true)
                .build(),
        )

    private fun processPayment(
        orderId: UUID,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): PaymentResult {
        val paymentStep = WorkflowStep.started("Payment Processing")
        steps += paymentStep
        val payment = paymentActivities.processPayment(orderId)

        if (payment.success) {
            return payment
        } else {
            saga.addCompensation(refundPaymentActivities::refundPayment, orderId)
            saga.compensate()
            return payment
        }
    }

    private fun processInventory(
        orderId: UUID,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): InventoryReservationResult {
        val inventoryStep = WorkflowStep.started("Inventory Reservation")
        steps += inventoryStep
        val inventory = inventoryActivities.reserveInventory(orderId)

        if (inventory.success) {
            return inventory
        } else {
            saga.addCompensation(refundPaymentActivities::refundPayment, orderId)
            saga.compensate()
            return inventory
        }
    }

    private fun processNotification(
        orderId: UUID,
        steps: MutableList<WorkflowStep>,
    ): NotificationResult {
        val notificationStep = WorkflowStep.started("Order Confirmation")
        steps += notificationStep
        return notificationActivities.sendOrderConfirmation(orderId, placeholderCustomerEmail(orderId))
    }

    private fun createSuccessResult(
        orderId: UUID,
        paymentResult: PaymentResult,
        inventoryResult: InventoryReservationResult,
        notificationResult: NotificationResult,
        workflowStart: Instant,
        steps: MutableList<WorkflowStep>,
    ): OrderFulfillmentResult {
        val duration = Duration.between(workflowStart, Instant.now()).toMillis()
        logger.info("Temporal pilot workflow completed for orderId={} in {} ms", orderId, duration)

        return OrderFulfillmentResult(
            success = true,
            orderId = orderId,
            paymentId = paymentResult.paymentId,
            reservationId = inventoryResult.reservationId,
            confirmationNotificationId = notificationResult.notificationId,
            status = "COMPLETED",
            steps = steps,
            executionDuration = duration,
        )
    }

    private fun handleWorkflowFailure(
        orderId: UUID,
        failureReason: String,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): OrderFulfillmentResult {
        saga.compensate()
        return OrderFulfillmentResult(
            success = false,
            orderId = orderId,
            status = "FAILED",
            failureReason = failureReason,
            steps = steps,
        )
    }

    private fun handleUnexpectedError(
        orderId: UUID,
        ex: Exception,
        saga: Saga,
        steps: MutableList<WorkflowStep>,
    ): OrderFulfillmentResult {
        logger.error("Temporal pilot workflow failed for orderId={}", orderId, ex)
        try {
            saga.compensate()
        } catch (compensation: Exception) {
            logger.warn("Temporal pilot compensation failed for orderId={}", orderId, compensation)
        }
        return OrderFulfillmentResult(
            success = false,
            orderId = orderId,
            status = "FAILED",
            failureReason = ex.message,
            steps = steps,
        )
    }

    private fun placeholderCustomerEmail(orderId: UUID): String = "workflow+$orderId@example.com"
}
