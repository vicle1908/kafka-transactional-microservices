package com.example.orders.application

import com.example.orders.OrdersServiceApplication
import com.example.orders.domain.OrderRepository
import com.example.orders.testsupport.OrdersServiceIntegrationTestSupport
import com.example.outbox.repository.OutboxRepository
import com.example.temporal.OrderFulfillmentWorkflow
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [OrdersServiceApplication::class], properties = ["spring.kafka.listener.auto-startup=false"])
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceTest : OrdersServiceIntegrationTestSupport() {
    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var workflowClient: WorkflowClient

    @Test
    @Transactional
    fun `handle should persist order and append outbox event`() {
        val workflowStub = mock<OrderFulfillmentWorkflow>()
        whenever(workflowClient.newWorkflowStub(eq(OrderFulfillmentWorkflow::class.java), any<WorkflowOptions>()))
            .thenReturn(workflowStub)

        val command =
            CreateOrderCommand(
                customerId = "customer-123",
                orderItems =
                    listOf(
                        OrderItemCommand(productId = "item-1", quantity = 1, unitPrice = java.math.BigDecimal("10.00")),
                        OrderItemCommand(productId = "item-2", quantity = 2, unitPrice = java.math.BigDecimal("20.00")),
                    ),
            )

        val orderId = orderService.handle(command)

        val orders = orderRepository.findAll()
        val events = outboxRepository.findAll()

        assertThat(orderId).isNotNull
        assertThat(orders).hasSize(1)
        assertThat(events).hasSize(1)
        assertThat(events.first().aggregateId).isEqualTo(orderId.toString())
    }
}
