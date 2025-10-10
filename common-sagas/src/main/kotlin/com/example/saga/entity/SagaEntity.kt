package com.example.saga.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Deprecated("Use SagaStateEntity; this mapping will be removed after migrations are adopted.")
@Entity
@Table(name = "sagas")
class SagaEntity(
    @Column(name = "saga_id", nullable = false)
    val sagaId: String,
    @Column(name = "saga_type", nullable = false)
    val sagaType: String,
    @Column(name = "current_state", nullable = false)
    var currentState: String,
    @Column(name = "correlation_id", nullable = false)
    val correlationId: String,
    @Column(name = "business_data", columnDefinition = "TEXT")
    val businessData: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    fun updateState(
        newState: String,
        updatedAt: Instant,
    ) {
        currentState = newState
        this.updatedAt = updatedAt
    }

    fun markFailed(updatedAt: Instant) {
        currentState = "FAILED"
        this.updatedAt = updatedAt
    }

    fun markCompensating(updatedAt: Instant) {
        currentState = "COMPENSATING"
        this.updatedAt = updatedAt
    }
}
