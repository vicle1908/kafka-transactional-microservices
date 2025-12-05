package com.example.notification.integration

import com.example.notification.application.NotificationService
import com.example.notification.application.SendNotificationCommand
import com.example.notification.domain.NotificationRepository
import com.example.notification.domain.NotificationStatus
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
import java.util.UUID

@SpringBootTest
@Testcontainers
@org.springframework.transaction.annotation.Transactional
class NotificationServiceIntegrationTest {
    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var outboxRepository: OutboxRepository

    @Autowired
    private lateinit var sagaStateService: com.example.saga.SagaStateService

    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.0"))
                .withDatabaseName("notifications_test")
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
            registry.add("spring.kafka.consumer.group-id") { "notification-service-test" }
            registry.add("temporal.connection.target") { "127.0.0.1:7233" }
            registry.add("temporal.tracing.enabled") { false }
        }
    }

    @Test
    fun `should send notification and persist to database`() {
        val orderId = UUID.randomUUID()

        // Create a saga for the order in IN_PROGRESS state (as it would be after payment/inventory)
        sagaStateService.start(
            sagaType = "order-fulfillment",
            correlationId = orderId.toString(),
            initialState = com.example.saga.SagaStatus.IN_PROGRESS,
            data = """{"orderId":"$orderId"}""",
        )

        val command =
            SendNotificationCommand(
                orderId = orderId,
                recipient = "test@example.com",
                channel = "email",
                template = "order-confirmation",
                payload = """{"orderId":"$orderId","email":"test@example.com"}""",
            )

        val notificationId = notificationService.send(command)

        val notification = notificationRepository.findById(notificationId).orElse(null)
        assertThat(notification).isNotNull
        assertThat(notification?.orderId).isEqualTo(orderId)
        assertThat(notification?.channel).isEqualTo("email")
        assertThat(notification?.template).isEqualTo("order-confirmation")
        assertThat(notification?.status()).isIn(NotificationStatus.SENT, NotificationStatus.QUEUED)
    }

    @Test
    fun `should create outbox message when notification is sent`() {
        val orderId = UUID.randomUUID()

        // Create a saga for the order in IN_PROGRESS state (as it would be after payment/inventory)
        sagaStateService.start(
            sagaType = "order-fulfillment",
            correlationId = orderId.toString(),
            initialState = com.example.saga.SagaStatus.IN_PROGRESS,
            data = """{"orderId":"$orderId"}""",
        )

        val command =
            SendNotificationCommand(
                orderId = orderId,
                recipient = "test@example.com",
                channel = "email",
                template = "order-confirmation",
                payload = """{"orderId":"$orderId","email":"test@example.com"}""",
            )

        notificationService.send(command)

        val outboxMessages = outboxRepository.findAll()
        assertThat(outboxMessages).isNotEmpty
        val notificationMessage = outboxMessages.find { it.eventType == "NotificationSent" }
        assertThat(notificationMessage).isNotNull
        assertThat(notificationMessage?.aggregateType).isEqualTo("Notification")
    }
}
