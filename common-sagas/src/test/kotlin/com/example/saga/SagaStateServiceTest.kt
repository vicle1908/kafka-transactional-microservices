package com.example.saga

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.Instant
import javax.sql.DataSource

@DataJpaTest
class SagaStateServiceTest {
    @Autowired
    private lateinit var repository: SagaStateRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var dataSource: DataSource

    private lateinit var service: SagaStateService

    @BeforeEach
    fun setup() {
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
        service = SagaStateService(repository)
    }

    @Test
    fun `start should persist saga with initial state`() {
        val started =
            service.start(
                sagaType = "order-fulfillment",
                correlationId = "order-123",
                initialState = SagaStatus.STARTED,
                data = """{"step":"created"}""",
                at = Instant.parse("2025-01-01T00:00:00Z"),
            )

        entityManager.flush()
        entityManager.clear()

        val found = repository.findById(started.sagaId)
        assertThat(found).isPresent
        assertThat(found.get().state()).isEqualTo(SagaStatus.STARTED)
        assertThat(found.get().data()).contains("created")
        assertThat(found.get().version()).isEqualTo(0)
    }

    @Test
    fun `start should reject duplicate correlation id`() {
        service.start(
            sagaType = "order-fulfillment",
            correlationId = "order-123",
        )

        assertThatThrownBy {
            service.start(
                sagaType = "order-fulfillment",
                correlationId = "order-123",
            )
        }.isInstanceOf(SagaAlreadyExistsException::class.java)
    }

    @Test
    fun `transition should update state data and bump version`() {
        val saga =
            service.start(
                sagaType = "inventory",
                correlationId = "order-456",
                initialState = SagaStatus.STARTED,
            )

        val transitioned =
            service.transition(
                sagaId = saga.sagaId,
                newState = SagaStatus.IN_PROGRESS,
                dataTransformer = { """{"step":"reserve"}""" },
            )

        entityManager.flush()
        assertThat(transitioned.state()).isEqualTo(SagaStatus.IN_PROGRESS)
        assertThat(transitioned.data()).contains("reserve")
        assertThat(transitioned.version()).isEqualTo(1)
    }

    @Test
    fun `transition should enforce expected state`() {
        val saga =
            service.start(
                sagaType = "notification",
                correlationId = "order-789",
                initialState = SagaStatus.STARTED,
            )

        assertThatThrownBy {
            service.transition(
                sagaId = saga.sagaId,
                newState = SagaStatus.COMPLETED,
                expectedState = SagaStatus.COMPLETED,
            )
        }.isInstanceOf(InvalidSagaStateTransitionException::class.java)
    }

    @Test
    fun `fail should append failure reason`() {
        val saga =
            service.start(
                sagaType = "payment",
                correlationId = "order-321",
            )

        val failed =
            service.fail(saga.sagaId, "gateway timeout")

        entityManager.flush()
        assertThat(failed.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(failed.data()).contains("gateway timeout")
    }
}
