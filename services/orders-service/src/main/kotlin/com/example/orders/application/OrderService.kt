package com.example.orders.application

import com.example.events.avro.OrderCreatedEvent
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxRepository: OutboxRepository,
    private val sagaStateService: SagaStateService,
    private val sagaMetrics: SagaMetricsRecorder,
    private val workflowClient: WorkflowClient,
) {
    @Transactional
    fun handle(command: CreateOrderCommand): UUID {
        validate(command)
        val occurredAt = Instant.now()

        val order =
            OrderEntity(
                customerId = command.customerId,
                status = OrderStatus.PENDING,
                createdAt = occurredAt,
            )
        val saved = orderRepository.save(order)

        val event =
            OrderCreatedEvent
                .newBuilder()
                .setEventId(UUID.randomUUID())
                .setAggregateId(saved.id!!)
                .setOccurredAt(occurredAt)
                .setPayload(serializePayload(saved.id!!, command))
                .build()

        val payload = encodeEvent(event)

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

        val workflowOptions =
            WorkflowOptions
                .newBuilder()
                .setTaskQueue(TaskQueues.ORDER_FULFILLMENT_WORKFLOW_TASK_QUEUE)
                .build()
        val workflow = workflowClient.newWorkflowStub(OrderFulfillmentWorkflow::class.java, workflowOptions)
        workflow.start(saved.id!!)

        return saved.id!!
    }

    private fun validate(command: CreateOrderCommand) {
        require(command.customerId.isNotBlank()) {
            "customerId must not be blank"
        }
        require(command.orderItems.isNotEmpty()) {
            "orderItems must not be empty"
        }
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
