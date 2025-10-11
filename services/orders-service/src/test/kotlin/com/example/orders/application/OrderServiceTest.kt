package com.example.orders.application

import com.example.orders.OrdersServiceApplication
import com.example.orders.domain.OrderRepository
import com.example.persistence.outbox.OutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [OrdersServiceApplication::class])
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceTest {
    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Test
    @Transactional
    fun `handle should persist order and append outbox event`() {
        val command =
            CreateOrderCommand(
                customerId = "customer-123",
                orderItems = listOf("item-1", "item-2"),
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
