package com.example.persistence.outbox

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.Instant
import javax.sql.DataSource

@DataJpaTest
class OutboxRepositoryTest
    @Autowired
    constructor(
        private val repository: OutboxRepository,
        private val entityManager: TestEntityManager,
        private val dataSource: DataSource,
    ) {
        @BeforeEach
        fun migrateSchema() {
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .also {
                    it.clean()
                    it.migrate()
                }
        }

        @Test
        fun `save and retrieve outbox message`() {
            val message =
                OutboxMessage(
                    aggregateType = "Order",
                    aggregateId = "order-123",
                    eventType = "OrderCreated",
                    payload = "{\"orderId\":\"order-123\"}",
                    headers = null,
                    occurredAt = Instant.parse("2025-01-01T00:00:00Z"),
                )

            val saved = repository.save(message)
            entityManager.flush()

            val found = repository.findById(saved.id!!)
            assertThat(found).isPresent
            assertThat(found.get().aggregateType).isEqualTo("Order")
        }
    }
