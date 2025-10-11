package com.example.saga.repository

import com.example.saga.entity.SagaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SagaRepository : JpaRepository<SagaEntity, UUID> {
    fun findBySagaId(sagaId: String): SagaEntity?

    fun findByCorrelationId(correlationId: String): SagaEntity?

    fun findBySagaTypeAndCurrentState(
        sagaType: String,
        state: String,
    ): List<SagaEntity>
}
