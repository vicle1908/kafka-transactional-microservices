package com.example.orders.integration

import com.example.orders.application.CreateOrderCommand
import com.example.orders.application.OrderItemCommand
import com.example.orders.application.OrderService
import com.example.orders.domain.OrderItemRepository
import com.example.orders.domain.OrderRepository
import com.example.orders.domain.OrderStatus
import com.example.outbox.repository.OutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.0"))
                .withDatabaseName("orders_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)

        @Container
        @JvmStatic
        val kafka =
            KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"))
                .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.flyway.enabled") { false }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.kafka.consumer.group-id") { "orders-service-test" }
            registry.add("temporal.connection.target") { "127.0.0.1:7233" }
            registry.add("temporal.tracing.enabled") { false }
        }
    }

    @Test
    fun `should create order with items and persist to database`() {
        val command =
            CreateOrderCommand(
                customerId = "customer-001",
                orderItems =
                    listOf(
                        OrderItemCommand(
                            productId = "product-001",
                            quantity = 2,
                            unitPrice = BigDecimal("29.99"),
                            productName = "Test Product",
                        ),
                        OrderItemCommand(
                            productId = "product-002",
                            quantity = 1,
                            unitPrice = BigDecimal("49.99"),
                            productName = "Another Product",
                        ),
                    ),
            )

        val orderId = orderService.handle(command)

        val order = orderRepository.findById(orderId).orElse(null)
        assertThat(order).isNotNull
        assertThat(order?.customerId).isEqualTo("customer-001")
        assertThat(order?.status).isEqualTo(OrderStatus.PENDING)
        assertThat(order?.totalAmount).isEqualByComparingTo(BigDecimal("109.97"))

        val orderItems = orderItemRepository.findAllByOrderIdOrderByCreatedAt(orderId)
        assertThat(orderItems).hasSize(2)
        assertThat(orderItems.first().productId).isEqualTo("product-001")
        assertThat(orderItems.first().quantity).isEqualTo(2)
    }

    @Test
    fun `should create outbox message when order is created`() {
        val command =
            CreateOrderCommand(
                customerId = "customer-002",
                orderItems =
                    listOf(
                        OrderItemCommand(
                            productId = "product-003",
                            quantity = 1,
                            unitPrice = BigDecimal("19.99"),
                        ),
                    ),
            )

        val orderId = orderService.handle(command)

        val outboxMessages = outboxRepository.findAll()
        assertThat(outboxMessages).isNotEmpty
        val orderCreatedMessage =
            outboxMessages.find {
                it.eventType == "OrderCreated" && it.aggregateId == orderId.toString()
            }
        assertThat(orderCreatedMessage).isNotNull
        assertThat(orderCreatedMessage?.aggregateType).isEqualTo("Order")
    }

    @Test
    fun `should calculate total amount correctly from order items`() {
        val command =
            CreateOrderCommand(
                customerId = "customer-003",
                orderItems =
                    listOf(
                        OrderItemCommand(
                            productId = "product-004",
                            quantity = 3,
                            unitPrice = BigDecimal("10.00"),
                        ),
                        OrderItemCommand(
                            productId = "product-005",
                            quantity = 2,
                            unitPrice = BigDecimal("15.00"),
                        ),
                    ),
            )

        val orderId = orderService.handle(command)

        val order = orderRepository.findById(orderId).orElse(null)
        assertThat(order).isNotNull
        assertThat(order?.totalAmount).isEqualByComparingTo(BigDecimal("60.00"))
    }
}
