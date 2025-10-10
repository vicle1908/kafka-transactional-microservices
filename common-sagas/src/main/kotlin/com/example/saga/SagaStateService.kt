package com.example.saga

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SagaStateService(
    private val repository: SagaStateRepository,
) {
    @Transactional
    fun start(
        sagaType: String,
        correlationId: String,
        initialState: SagaStatus = SagaStatus.STARTED,
        data: String? = null,
        at: Instant = Instant.now(),
    ): SagaStateEntity {
        require(sagaType.isNotBlank()) { "sagaType must not be blank" }
        require(correlationId.isNotBlank()) { "correlationId must not be blank" }
        repository.findBySagaTypeAndCorrelationId(sagaType, correlationId)?.let {
            throw SagaAlreadyExistsException(sagaType, correlationId)
        }
        val entity =
            SagaStateEntity(
                sagaId = UUID.randomUUID(),
                sagaType = sagaType,
                correlationId = correlationId,
                state = initialState,
                data = data,
                createdAt = at,
                updatedAt = at,
            )
        return repository.save(entity)
    }

    @Transactional
    fun transition(
        sagaId: UUID,
        newState: SagaStatus,
        expectedState: SagaStatus? = null,
        dataTransformer: (String?) -> String? = { it },
        at: Instant = Instant.now(),
    ): SagaStateEntity {
        val entity = repository.findByIdOrNull(sagaId) ?: throw SagaNotFoundException(sagaId)
        if (expectedState != null && entity.state() != expectedState) {
            throw InvalidSagaStateTransitionException(sagaId, entity.state(), expectedState)
        }
        val newData = dataTransformer(entity.data())
        entity.transitionTo(newState, newData, at)
        return repository.save(entity)
    }

    @Transactional
    fun transitionByCorrelation(
        sagaType: String,
        correlationId: String,
        newState: SagaStatus,
        options: SagaTransitionOptions = SagaTransitionOptions(),
    ): SagaStateEntity {
        val entity =
            repository.findBySagaTypeAndCorrelationId(sagaType, correlationId)
                ?: throw SagaNotFoundException("$sagaType:$correlationId")
        options.expectedState?.let { expected ->
            if (entity.state() != expected) {
                throw InvalidSagaStateTransitionException(entity.sagaId, entity.state(), expected)
            }
        }
        val newData = options.dataTransformer(entity.data())
        val transitionedAt = options.occurredAt ?: Instant.now()
        entity.transitionTo(newState, newData, transitionedAt)
        return repository.save(entity)
    }

    @Transactional
    fun complete(
        sagaId: UUID,
        dataTransformer: (String?) -> String? = { it },
        at: Instant = Instant.now(),
    ): SagaStateEntity =
        transition(
            sagaId = sagaId,
            newState = SagaStatus.COMPLETED,
            dataTransformer = dataTransformer,
            at = at,
        )

    @Transactional
    fun fail(
        sagaId: UUID,
        reason: String?,
        at: Instant = Instant.now(),
    ): SagaStateEntity =
        transition(
            sagaId = sagaId,
            newState = SagaStatus.FAILED,
            dataTransformer = { existing -> mergeFailure(existing, reason) },
            at = at,
        )

    @Transactional(readOnly = true)
    fun findById(sagaId: UUID): SagaStateEntity? = repository.findByIdOrNull(sagaId)

    private fun mergeFailure(
        existing: String?,
        reason: String?,
    ): String? {
        if (reason.isNullOrBlank()) {
            return existing
        }
        return listOfNotNull(existing, reason).joinToString(separator = "\n")
    }
}
