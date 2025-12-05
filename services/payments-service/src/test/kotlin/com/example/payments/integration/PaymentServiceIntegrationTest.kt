package com.example.payments.integration

import com.example.outbox.repository.OutboxRepository
import com.example.payments.application.PaymentProcessingOutcome
import com.example.payments.application.PaymentService
import com.example.payments.application.ProcessPaymentCommand
import com.example.payments.domain.PaymentRepository
import com.example.payments.domain.PaymentStatus
import com.example.saga.SagaNames
import com.example.saga.SagaStateService
import com.example.saga.SagaStatus
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
@org.springframework.context.annotation.Import(
    com.example.payments.PaymentServiceIntegrationTestSupport.WebClientTestConfig::class,
)
class PaymentServiceIntegrationTest {
    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: SagaStateService

    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.0"))
                .withDatabaseName("payments_test")
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
            registry.add("spring.kafka.consumer.group-id") { "payments-service-test" }
            registry.add("temporal.connection.target") { "127.0.0.1:7233" }
            registry.add("temporal.tracing.enabled") { false }
            registry.add("orders.grpc.host") { "localhost" }
            registry.add("orders.grpc.port") { "9090" }
        }
    }

    private fun createSagaForOrder(orderId: UUID) {
        sagaStateService.start(
            sagaType = SagaNames.ORDER_FULFILLMENT,
            correlationId = orderId.toString(),
            data = """{"orderId":"$orderId"}""",
        )
    }

    @Test
    fun `should process payment and persist to database`() {
        val orderId = UUID.randomUUID()
        createSagaForOrder(orderId)

        val command =
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("100.00"),
                eventId = UUID.randomUUID(),
            )

        val outcome = paymentService.handle(command)

        assertThat(outcome).isNotNull
        when (outcome) {
            is PaymentProcessingOutcome.Completed -> {
                val payment = paymentRepository.findById(outcome.paymentId).orElse(null)
                assertThat(payment).isNotNull
                assertThat(payment?.orderId).isEqualTo(orderId)
                assertThat(payment?.amount).isEqualByComparingTo(BigDecimal("100.00"))
                assertThat(payment?.status()).isIn(PaymentStatus.COMPLETED, PaymentStatus.PENDING)
            }
            is PaymentProcessingOutcome.Failed -> {
                val payment = paymentRepository.findById(outcome.paymentId).orElse(null)
                assertThat(payment).isNotNull
                assertThat(payment?.status()).isEqualTo(PaymentStatus.FAILED)
            }
            is PaymentProcessingOutcome.AlreadyProcessed -> {
            }
        }
    }

    @Test
    fun `should create outbox message when payment is processed`() {
        val orderId = UUID.randomUUID()
        createSagaForOrder(orderId)

        val command =
            ProcessPaymentCommand(
                orderId = orderId,
                amount = BigDecimal("50.00"),
                eventId = UUID.randomUUID(),
            )

        paymentService.handle(command)

        val outboxMessages = outboxRepository.findAll()
        assertThat(outboxMessages).isNotEmpty
        val paymentMessage = outboxMessages.find { it.eventType in listOf("PaymentCompleted", "PaymentFailed") }
        assertThat(paymentMessage).isNotNull
        assertThat(paymentMessage?.aggregateType).isEqualTo("Payment")
    }
}
