package com.example.temporal.workflow

import java.time.Instant
import java.util.*

/**
 * Result of the OrderFulfillmentWorkflow execution.
 * Contains comprehensive information about the workflow outcome.
 */
data class OrderFulfillmentResult(
    val success: Boolean,
    val orderId: UUID,
    val paymentId: UUID? = null,
    val reservationId: UUID? = null,
    val confirmationNotificationId: UUID? = null,
    val status: String,
    val failureReason: String? = null,
    val compensationIssues: List<String> = emptyList(),
    val completedAt: Instant = Instant.now(),
    val executionDuration: Long = 0L, // Duration in milliseconds
    val steps: List<WorkflowStep> = emptyList()
) {
    companion object {
        fun success(
            orderId: UUID,
            paymentId: UUID,
            reservationId: UUID,
            confirmationNotificationId: UUID? = null,
            steps: List<WorkflowStep> = emptyList(),
            executionDuration: Long = 0L
        ): OrderFulfillmentResult {
            return OrderFulfillmentResult(
                success = true,
                orderId = orderId,
                paymentId = paymentId,
                reservationId = reservationId,
                confirmationNotificationId = confirmationNotificationId,
                status = "COMPLETED",
                steps = steps,
                executionDuration = executionDuration
            )
        }

        fun failure(
            orderId: UUID,
            failureReason: String,
            compensationIssues: List<String> = emptyList(),
            steps: List<WorkflowStep> = emptyList(),
            executionDuration: Long = 0L
        ): OrderFulfillmentResult {
            return OrderFulfillmentResult(
                success = false,
                orderId = orderId,
                status = "FAILED",
                failureReason = failureReason,
                compensationIssues = compensationIssues,
                steps = steps,
                executionDuration = executionDuration
            )
        }
    }
}

/**
 * Represents a step in the workflow execution.
 */
data class WorkflowStep(
    val stepName: String,
    val status: String,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val duration: Long = 0L, // Duration in milliseconds
    val result: String? = null,
    val error: String? = null
) {
    companion object {
        fun started(stepName: String): WorkflowStep {
            return WorkflowStep(
                stepName = stepName,
                status = "STARTED",
                startedAt = Instant.now()
            )
        }

        fun completed(
            stepName: String,
            result: String,
            duration: Long
        ): WorkflowStep {
            val now = Instant.now()
            return WorkflowStep(
                stepName = stepName,
                status = "COMPLETED",
                startedAt = now.minusMillis(duration),
                completedAt = now,
                duration = duration,
                result = result
            )
        }

        fun failed(
            stepName: String,
            error: String,
            duration: Long
        ): WorkflowStep {
            val now = Instant.now()
            return WorkflowStep(
                stepName = stepName,
                status = "FAILED",
                startedAt = now.minusMillis(duration),
                completedAt = now,
                duration = duration,
                error = error
            )
        }
    }
}