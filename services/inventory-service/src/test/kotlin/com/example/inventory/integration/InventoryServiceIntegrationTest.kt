package com.example.inventory.integration

import com.example.inventory.application.InventoryService
import com.example.inventory.application.ReserveInventoryCommand
import com.example.inventory.domain.InventoryReservationRepository
import com.example.inventory.domain.InventoryReservationStatus
import com.example.inventory.domain.InventoryStockRepository
import com.example.outbox.repository.OutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import java.util.UUID

@SpringBootTest
@Testcontainers
@org.springframework.transaction.annotation.Transactional
class InventoryServiceIntegrationTest {
    @Autowired
    private lateinit var inventoryService: InventoryService

    @Autowired
    private lateinit var stockRepository: InventoryStockRepository

    @Autowired
    private lateinit var reservationRepository: InventoryReservationRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: com.example.saga.SagaStateService

    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.0"))
                .withDatabaseName("inventory_test")
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
            registry.add("spring.kafka.consumer.group-id") { "inventory-service-test" }
            registry.add("temporal.connection.target") { "127.0.0.1:7233" }
            registry.add("temporal.tracing.enabled") { false }
        }
    }

    @BeforeEach
    fun setUp() {
        val stock = stockRepository.lockBySku("SKU-001")
        if (stock == null) {
            val newStock =
                com.example.inventory.domain.InventoryStockEntity(
                    sku = "SKU-001",
                    availableQuantity = 100,
                )
            stockRepository.save(newStock)
        }
    }

    @Test
    fun `should reserve inventory and persist reservation`() {
        val orderId = UUID.randomUUID()

        // Create a saga for the order in IN_PROGRESS state (as it would be after order creation)
        sagaStateService.start(
            sagaType = "order-fulfillment",
            correlationId = orderId.toString(),
            initialState = com.example.saga.SagaStatus.IN_PROGRESS,
            data = """{"orderId":"$orderId"}""",
        )

        val command =
            ReserveInventoryCommand(
                orderId = orderId,
                sku = "SKU-001",
                quantity = 5,
            )

        val reservationId = inventoryService.reserve(command)

        val reservation = reservationRepository.findById(reservationId).orElse(null)
        assertThat(reservation).isNotNull
        assertThat(reservation?.orderId).isEqualTo(orderId)
        assertThat(reservation?.sku).isEqualTo("SKU-001")
        assertThat(reservation?.quantity).isEqualTo(5)
        assertThat(reservation?.status).isEqualTo(InventoryReservationStatus.RESERVED)
    }

    @Test
    fun `should create outbox message when inventory is reserved`() {
        val orderId = UUID.randomUUID()

        // Create a saga for the order in IN_PROGRESS state (as it would be after order creation)
        sagaStateService.start(
            sagaType = "order-fulfillment",
            correlationId = orderId.toString(),
            initialState = com.example.saga.SagaStatus.IN_PROGRESS,
            data = """{"orderId":"$orderId"}""",
        )

        val command =
            ReserveInventoryCommand(
                orderId = orderId,
                sku = "SKU-001",
                quantity = 3,
            )

        inventoryService.reserve(command)

        val outboxMessages = outboxRepository.findAll()
        assertThat(outboxMessages).isNotEmpty
        val inventoryMessage = outboxMessages.find { it.eventType == "InventoryReserved" }
        assertThat(inventoryMessage).isNotNull
        assertThat(inventoryMessage?.aggregateType).isEqualTo("InventoryReservation")
    }
}
