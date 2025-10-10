package com.example.saga

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SagaStateRepository : JpaRepository<SagaStateEntity, UUID> {
    fun findBySagaTypeAndCorrelationId(
        sagaType: String,
        correlationId: String,
    ): SagaStateEntity?
}
