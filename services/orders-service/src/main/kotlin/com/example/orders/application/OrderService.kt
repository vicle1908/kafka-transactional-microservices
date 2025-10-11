@file:Suppress("ImportOrdering", "ktlint:standard:import-ordering")

package com.example.orders.application

import com.example.events.avro.OrderCreatedEvent
import com.example.observability.StructuredLogger
import com.example.orders.domain.OrderEntity
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
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
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
    private val outboxRepository: OutboxRepository,
    private val sagaStateService: SagaStateService,
    private val sagaMetrics: SagaMetricsRecorder,
    private val workflowClient: WorkflowClient,
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

        val order =
            OrderEntity(
                customerId = command.customerId,
                status = OrderStatus.PENDING,
                createdAt = occurredAt,
            )
        val saved = orderRepository.save(order)

        logger.info(
            "Order entity created",
            "orderId" to saved.id,
            "status" to order.status.name,
        )

        val event =
            OrderCreatedEvent
                .newBuilder()
                .setEventId(UUID.randomUUID())
                .setAggregateId(saved.id!!)
                .setOccurredAt(occurredAt)
                .setPayload(serializePayload(saved.id!!, command))
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

        logger.info(
            "Starting Temporal workflow",
            "workflowType" to "OrderFulfillmentWorkflow",
            "orderId" to saved.id!!,
            "taskQueue" to TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE,
        )

        val workflowOptions =
            WorkflowOptions
                .newBuilder()
                .setTaskQueue(TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE)
                .build()
        val workflow = workflowClient.newWorkflowStub(OrderFulfillmentWorkflow::class.java, workflowOptions)
        workflow.start(saved.id!!)

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

        logger.debug(
            "Order validation passed",
            "customerId" to command.customerId,
            "itemCount" to command.orderItems.size,
        )
    }

    private fun serializePayload(
        orderId: UUID,
        command: CreateOrderCommand,
    ): String {
        val payload =
            OrderCreatedPayload(
                orderId = orderId.toString(),
                customerId = command.customerId,
                items = command.orderItems,
                itemCount = command.orderItems.size,
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
        val items: List<String>,
        val itemCount: Int,
    )

    private companion object {
        val json = Json.Default
    }
}
