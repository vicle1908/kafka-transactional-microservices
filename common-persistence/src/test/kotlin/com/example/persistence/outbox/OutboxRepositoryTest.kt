package com.example.persistence.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxRepositoryTest {
    @Autowired
    private lateinit var repository: OutboxRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    fun `save and retrieve outbox message`() {
        val message =
            OutboxMessage(
                aggregateType = "Order",
                aggregateId = "order-123",
                eventType = "OrderCreated",
                payload = """{"orderId":"order-123"}""",
                headers = null,
                occurredAt = Instant.parse("2025-01-01T00:00:00Z"),
            )

        val saved = repository.save(message)
        entityManager.flush()

        val found = repository.findById(saved.id!!)
        assertThat(found).isPresent
        assertThat(found.get().aggregateType).isEqualTo("Order")
    }

    companion object {
        @Container
        private val postgres =
            PostgreSQLContainer("postgres:16.3-alpine").apply {
                withDatabaseName("outbox_test")
                withUsername("postgres")
                withPassword("postgres")
            }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.flyway.clean-disabled") { false }
        }
    }
}
