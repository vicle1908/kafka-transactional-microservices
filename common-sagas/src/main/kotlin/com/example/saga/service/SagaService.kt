package com.example.saga.service

import com.example.saga.entity.SagaEntity
import com.example.saga.repository.SagaRepository
import jakarta.persistence.OptimisticLockException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SagaService(
    private val sagaRepository: SagaRepository,
) {
    @Transactional
    fun startSaga(
        sagaType: String,
        sagaId: String,
        correlationId: String,
        initialState: String,
        businessData: String? = null,
    ): SagaEntity {
        val saga =
            SagaEntity(
                sagaId = sagaId,
                sagaType = sagaType,
                currentState = initialState,
                correlationId = correlationId,
                businessData = businessData,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        return sagaRepository.save(saga)
    }

    @Transactional
    fun updateSagaState(
        sagaId: String,
        newState: String,
    ): Boolean {
        val saga = sagaRepository.findBySagaId(sagaId) ?: return false
        saga.updateState(newState, Instant.now())
        return try {
            sagaRepository.save(saga)
            true
        } catch (_: OptimisticLockException) {
            false
        }
    }

    @Transactional
    fun markSagaFailed(sagaId: String): Boolean {
        val saga = sagaRepository.findBySagaId(sagaId) ?: return false
        saga.markFailed(Instant.now())
        return try {
            sagaRepository.save(saga)
            true
        } catch (_: OptimisticLockException) {
            false
        }
    }

    @Transactional
    fun markSagaCompensating(sagaId: String): Boolean {
        val saga = sagaRepository.findBySagaId(sagaId) ?: return false
        saga.markCompensating(Instant.now())
        return try {
            sagaRepository.save(saga)
            true
        } catch (_: OptimisticLockException) {
            false
        }
    }

    fun findSagaById(sagaId: String): SagaEntity? = sagaRepository.findBySagaId(sagaId)

    fun findSagaByCorrelationId(correlationId: String): SagaEntity? = sagaRepository.findByCorrelationId(correlationId)

    fun findSagasByTypeAndState(
        sagaType: String,
        state: String,
    ): List<SagaEntity> = sagaRepository.findBySagaTypeAndCurrentState(sagaType, state)
}
