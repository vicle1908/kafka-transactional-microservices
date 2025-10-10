package com.example.temporalpilot.implementation

import com.example.temporal.OrderFulfillmentWorkflow
import com.example.temporal.TaskQueues
import com.example.temporal.activity.InventoryActivity
import com.example.temporal.activity.NotificationActivity
import com.example.temporal.activity.PaymentActivity
import com.example.temporal.activity.RefundPaymentActivity
import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Saga
import io.temporal.workflow.Workflow
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class OrderFulfillmentWorkflowImpl : OrderFulfillmentWorkflow {
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
        val saga =
            Saga(
                Saga.Options
                    .Builder()
                    .setParallelCompensation(true)
                    .build(),
            )
        try {
            saga.addCompensation(refundPaymentActivities::refundPayment, orderId)
            paymentActivities.processPayment(orderId)
            inventoryActivities.reserveInventory(orderId)
            notificationActivities.sendNotification(orderId)
        } catch (e: Exception) {
            saga.compensate()
            throw e
        }
    }
}
