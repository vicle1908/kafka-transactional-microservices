package com.example.orders.adapter.inbound.http

import com.example.observability.StructuredLogger
import com.example.orders.application.CreateOrderCommand
import com.example.orders.application.OrderService
import com.example.temporal.workflow.WorkflowStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {
    private val logger = StructuredLogger.getLogger(OrderController::class.java)

    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest,
    ): ResponseEntity<CreateOrderResponse> {
        logger.info(
            "Received create order request",
            "customerId" to request.customerId,
            "itemCount" to request.orderItems.size,
        )

        val id =
            orderService.handle(
                CreateOrderCommand(
                    customerId = request.customerId,
                    orderItems =
                        request.orderItems.map { item ->
                            com.example.orders.application.OrderItemCommand(
                                productId = item.productId,
                                quantity = item.quantity,
                                unitPrice = item.unitPrice,
                                productName = item.productName,
                            )
                        },
                ),
            )

        logger.info(
            "Order created successfully",
            "orderId" to id,
            "customerId" to request.customerId,
        )

        return ResponseEntity.ok(CreateOrderResponse(id))
    }

    @GetMapping("/{orderId}/workflow/status")
    fun getWorkflowStatus(
        @PathVariable orderId: UUID,
    ): ResponseEntity<WorkflowStatusResponse> {
        logger.info("Received workflow status query request", "orderId" to orderId)
        val status = orderService.getWorkflowStatus(orderId)
        return ResponseEntity.ok(WorkflowStatusResponse(status))
    }

    @PutMapping("/{orderId}/workflow/cancel")
    fun cancelWorkflow(
        @PathVariable orderId: UUID,
        @RequestBody request: CancelWorkflowRequest,
    ): ResponseEntity<CancelWorkflowResponse> {
        logger.info("Received workflow cancellation request", "orderId" to orderId, "reason" to request.reason)
        orderService.cancelWorkflow(orderId, request.reason)
        return ResponseEntity.ok(CancelWorkflowResponse(orderId, "Cancellation signal sent"))
    }
}

data class WorkflowStatusResponse(
    val orderId: UUID,
    val status: String,
    val currentStep: String?,
    val steps: List<WorkflowStepResponse>,
    val startedAt: String?,
    val cancelled: Boolean,
    val cancellationReason: String?,
    val progress: Double,
) {
    constructor(status: WorkflowStatus) : this(
        orderId = status.orderId,
        status = status.status,
        currentStep = status.currentStep,
        steps = status.steps.map { WorkflowStepResponse(it) },
        startedAt = status.startedAt?.toString(),
        cancelled = status.cancelled,
        cancellationReason = status.cancellationReason,
        progress = status.progress,
    )
}

data class WorkflowStepResponse(
    val stepName: String,
    val status: String,
    val startedAt: String,
    val completedAt: String?,
    val duration: Long,
    val result: String?,
    val error: String?,
) {
    constructor(step: com.example.temporal.workflow.WorkflowStep) : this(
        stepName = step.stepName,
        status = step.status,
        startedAt = step.startedAt.toString(),
        completedAt = step.completedAt?.toString(),
        duration = step.duration,
        result = step.result,
        error = step.error,
    )
}

data class CancelWorkflowRequest(
    val reason: String,
)

data class CancelWorkflowResponse(
    val orderId: UUID,
    val message: String,
)

data class CreateOrderRequest(
    @field:NotBlank
    val customerId: String,
    @field:Size(min = 1, message = "orderItems must contain at least one item")
    val orderItems: List<OrderItemRequest>,
)

data class OrderItemRequest(
    @field:NotBlank
    val productId: String,
    @field:jakarta.validation.constraints.Min(1, message = "quantity must be at least 1")
    val quantity: Int,
    @field:jakarta.validation.constraints.DecimalMin(value = "0.01", message = "unitPrice must be positive")
    val unitPrice: java.math.BigDecimal,
    val productName: String? = null,
)

data class CreateOrderResponse(
    val orderId: UUID,
)
