package com.example.temporal.workflow

import java.time.Instant
import java.util.UUID

data class WorkflowStatus(
    val orderId: UUID,
    val status: String,
    val currentStep: String? = null,
    val steps: List<WorkflowStep> = emptyList(),
    val startedAt: Instant? = null,
    val cancelled: Boolean = false,
    val cancellationReason: String? = null,
    val progress: Double = 0.0,
)
