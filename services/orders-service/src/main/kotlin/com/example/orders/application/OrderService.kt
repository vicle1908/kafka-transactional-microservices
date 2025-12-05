@file:Suppress("ImportOrdering", "ktlint:standard:import-ordering")

package com.example.orders.application

import com.example.events.avro.OrderCreatedEvent
import com.example.observability.StructuredLogger
import com.example.orders.domain.OrderEntity
import com.example.orders.domain.OrderItemEntity
import com.example.orders.domain.OrderItemRepository
import com.example.orders.domain.OrderRepository
import com.example.orders.domain.OrderStatus
import com.example.outbox.entity.OutboxMessage
import com.example.outbox.entity.OutboxStatus
import com.example.outbox.repository.OutboxRepository
import com.example.saga.SagaMetricsRecorder
import com.example.saga.SagaNames
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
import com.example.saga.SagaStepFormatter
import com.example.saga.SagaStepNames
import com.example.temporal.OrderFulfillmentWorkflow
import com.example.temporal.TaskQueues
import com.example.temporal.workflow.WorkflowStatus
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.client.WorkflowStub
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val outboxRepository: OutboxRepository,
    private val sagaStateService: SagaStateService,
    private val sagaMetrics: SagaMetricsRecorder,
    @Autowired(required = false) private val workflowClient: WorkflowClient?,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = StructuredLogger.getLogger(OrderService::class.java)

    @Transactional
    @Suppress("LongMethod")
    fun handle(command: CreateOrderCommand): UUID {
        logger.info(
            "Starting order creation",
            "customerId" to command.customerId,
            "itemCount" to command.orderItems.size,
        )
        validate(command)
        val occurredAt = Instant.now()

        logger.info(
            "Creating order",
            "customerId" to command.customerId,
            "itemCount" to command.orderItems.size,
            "occurredAt" to occurredAt,
        )

        val totalAmount = calculateTotalAmount(command.orderItems)
        val order =
            OrderEntity(
                customerId = command.customerId,
                totalAmount = totalAmount,
                status = OrderStatus.PENDING,
                createdAt = occurredAt,
            )
        val saved = orderRepository.save(order)

        logger.info(
            "Order entity created",
            "orderId" to saved.id,
            "status" to order.status.name,
        )

        val orderItems =
            command.orderItems.map { item ->
                OrderItemEntity(
                    order = saved,
                    productId = item.productId,
                    productName = item.productName ?: item.productId,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    createdAt = occurredAt,
                )
            }
        orderItemRepository.saveAll(orderItems)

        logger.info(
            "Order items persisted",
            "orderId" to saved.id,
            "itemCount" to orderItems.size,
        )

        val event =
            OrderCreatedEvent
                .newBuilder()
                .setEventId(UUID.randomUUID())
                .setAggregateId(saved.id!!)
                .setOccurredAt(occurredAt)
                .setPayload(serializePayload(saved.id!!, command, totalAmount))
                .build()

        val payload = encodeEvent(event)

        logger.info(
            "Creating outbox message for OrderCreated event",
            "eventId" to event.eventId,
            "aggregateId" to event.aggregateId,
            "eventType" to "OrderCreated",
        )

        val outbox =
            OutboxMessage(
                aggregateId = saved.id!!.toString(),
                aggregateType = "Order",
                eventType = "OrderCreated",
                payload = payload,
                headers = null,
                status = OutboxStatus.PENDING,
                occurredAt = occurredAt,
            )
        outboxRepository.save(outbox)

        logger.info(
            "Saga state started",
            "sagaType" to SagaNames.ORDER_FULFILLMENT,
            "correlationId" to saved.id!!,
            "step" to SagaStepNames.ORDER_CREATED,
        )

        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = saved.id!!.toString(),
            data = SagaStepFormatter.append(null, SagaStepNames.ORDER_CREATED, occurredAt),
            at = occurredAt,
        )
        sagaMetrics.recordStep(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            step = SagaStepNames.ORDER_CREATED,
            state = SagaStatus.STARTED,
        )

        workflowClient?.let {
            logger.info(
                "Starting Temporal workflow",
                "workflowType" to "OrderFulfillmentWorkflow",
                "orderId" to saved.id!!,
                "taskQueue" to TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE,
            )

            val workflowId = "order-fulfillment-${saved.id!!}"
            val workflowOptions =
                WorkflowOptions
                    .newBuilder()
                    .setTaskQueue(TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE)
                    .setWorkflowId(workflowId)
                    .build()
            val workflow = it.newWorkflowStub(OrderFulfillmentWorkflow::class.java, workflowOptions)
            WorkflowClient.start(workflow::execute, saved.id!!)

            logger.info(
                "Temporal workflow started",
                "orderId" to saved.id!!,
                "workflowId" to workflowId,
            )
        } ?: logger.info(
            "Temporal workflow skipped (WorkflowClient not available)",
            "orderId" to saved.id!!,
        )

        // Publish change event for after-commit cache eviction
        eventPublisher.publishEvent(OrderChangedEvent(saved.id!!))

        logger.info(
            "Order creation completed successfully",
            "orderId" to saved.id!!,
            "customerId" to command.customerId,
            "status" to saved.status.name,
        )

        return saved.id!!
    }

    fun getWorkflowStatus(orderId: UUID): WorkflowStatus {
        logger.info("Querying workflow status", "orderId" to orderId)
        val client = workflowClient ?: throw IllegalStateException("Temporal WorkflowClient is not available")
        val workflowId = "order-fulfillment-$orderId"
        val workflow = client.newWorkflowStub(OrderFulfillmentWorkflow::class.java, workflowId)
        return workflow.getStatus()
    }

    fun cancelWorkflow(
        orderId: UUID,
        reason: String,
    ) {
        logger.info("Cancelling workflow", "orderId" to orderId, "reason" to reason)
        val client = workflowClient ?: throw IllegalStateException("Temporal WorkflowClient is not available")
        val workflowId = "order-fulfillment-$orderId"
        val workflow = client.newWorkflowStub(OrderFulfillmentWorkflow::class.java, workflowId)
        workflow.cancel(reason)
        logger.info("Workflow cancellation signal sent", "orderId" to orderId)
    }

    private fun calculateTotalAmount(orderItems: List<OrderItemCommand>): java.math.BigDecimal =
        orderItems.fold(java.math.BigDecimal.ZERO) { total, item ->
            val itemTotal = item.unitPrice.multiply(java.math.BigDecimal(item.quantity))
            total.add(itemTotal)
        }

    private fun validate(command: CreateOrderCommand) {
        if (command.customerId.isBlank()) {
            logger.error(
                "Order validation failed: customerId cannot be blank",
                "operation" to "order.create",
                "customerId" to command.customerId,
                "reason" to "validation_error",
            )
            throw IllegalArgumentException("customerId must not be blank")
        }

        if (command.orderItems.isEmpty()) {
            logger.error(
                "Order validation failed: orderItems cannot be empty",
                "operation" to "order.create",
                "customerId" to command.customerId,
                "itemCount" to command.orderItems.size,
                "reason" to "validation_error",
            )
            throw IllegalArgumentException("orderItems must not be empty")
        }

        command.orderItems.forEach { item ->
            if (item.productId.isBlank()) {
                throw IllegalArgumentException("productId must not be blank")
            }
            if (item.quantity <= 0) {
                throw IllegalArgumentException("quantity must be positive")
            }
            if (item.unitPrice <= java.math.BigDecimal.ZERO) {
                throw IllegalArgumentException("unitPrice must be positive")
            }
        }

        logger.debug(
            "Order validation passed",
            "customerId" to command.customerId,
            "itemCount" to command.orderItems.size,
        )
    }

    private fun serializePayload(
        orderId: UUID,
        command: CreateOrderCommand,
        totalAmount: java.math.BigDecimal,
    ): String {
        val payload =
            OrderCreatedPayload(
                orderId = orderId.toString(),
                customerId = command.customerId,
                items =
                    command.orderItems.map { item ->
                        OrderItemPayload(
                            productId = item.productId,
                            quantity = item.quantity,
                            unitPrice = item.unitPrice.toPlainString(),
                            productName = item.productName,
                        )
                    },
                itemCount = command.orderItems.size,
                totalAmount = totalAmount.toPlainString(),
            )
        return json.encodeToString(payload)
    }

    private fun encodeEvent(event: OrderCreatedEvent): String {
        val writer = SpecificDatumWriter(OrderCreatedEvent::class.java)
        val output = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().jsonEncoder(OrderCreatedEvent.getClassSchema(), output)
        writer.write(event, encoder)
        encoder.flush()
        return output.toString(StandardCharsets.UTF_8)
    }

    @Serializable
    private data class OrderCreatedPayload(
        val orderId: String,
        val customerId: String,
        val items: List<OrderItemPayload>,
        val itemCount: Int,
        val totalAmount: String,
    )

    @Serializable
    private data class OrderItemPayload(
        val productId: String,
        val quantity: Int,
        val unitPrice: String,
        val productName: String? = null,
    )

    private companion object {
        val json = Json.Default
    }
}
