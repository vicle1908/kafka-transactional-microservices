package com.example.saga

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import java.time.Instant

@DataJpaTest
class SagaStateServiceTest {
    @Autowired
    private lateinit var repository: SagaStateRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var service: SagaStateService

    @BeforeEach
    fun setup() {
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
    fun `transitionByCorrelation should reject blank inputs`() {
        assertThatThrownBy {
            service.transitionByCorrelation(
                sagaType = "",
                correlationId = "order-123",
                newState = SagaStatus.IN_PROGRESS,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("sagaType must not be blank")

        assertThatThrownBy {
            service.transitionByCorrelation(
                sagaType = "order-fulfillment",
                correlationId = "",
                newState = SagaStatus.IN_PROGRESS,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("correlationId must not be blank")
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

    @Test
    fun `completeByCorrelation should update state to completed`() {
        val saga =
            service.start(
                sagaType = "fulfillment",
                correlationId = "order-444",
                initialState = SagaStatus.IN_PROGRESS,
            )

        val completed =
            service.completeByCorrelation(
                sagaType = "fulfillment",
                correlationId = "order-444",
            )

        entityManager.flush()
        assertThat(completed.state()).isEqualTo(SagaStatus.COMPLETED)
    }

    @Test
    fun `failByCorrelation should update state to failed with reason`() {
        service.start(
            sagaType = "shipping",
            correlationId = "order-555",
            initialState = SagaStatus.IN_PROGRESS,
        )

        val failed =
            service.failByCorrelation(
                sagaType = "shipping",
                correlationId = "order-555",
                reason = "address invalid",
            )

        entityManager.flush()
        assertThat(failed.state()).isEqualTo(SagaStatus.FAILED)
        assertThat(failed.data()).contains("address invalid")
    }
}
