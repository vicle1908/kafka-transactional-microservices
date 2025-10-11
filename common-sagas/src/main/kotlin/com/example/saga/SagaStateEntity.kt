package com.example.saga

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sagas")
@Suppress("LongParameterList")
open class SagaStateEntity(
    @Id
    @Column(name = "saga_id", nullable = false)
    val sagaId: UUID,
    @Column(name = "saga_type", nullable = false)
    val sagaType: String,
    @Column(name = "correlation_id", nullable = false)
    val correlationId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private var state: SagaStatus,
    @Column(name = "data", columnDefinition = "TEXT")
    private var data: String?,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    private var updatedAt: Instant,
    @Version
    @Column(name = "version")
    private var version: Long? = null,
) {
    fun state(): SagaStatus = state

    fun data(): String? = data

    fun updatedAt(): Instant = updatedAt

    fun version(): Long? = version

    fun transitionTo(
        newState: SagaStatus,
        newData: String?,
        at: Instant,
    ) {
        state = newState
        data = newData
        updatedAt = at
    }

    /**
     * No-arg constructor for JPA only.
     * This constructor should not be used directly in application code.
     * Use the primary constructor instead.
     */
    @Deprecated("Only for JPA. Use the primary constructor instead.", level = DeprecationLevel.WARNING)
    protected constructor() : this(
        sagaId = UUID.randomUUID(),
        sagaType = "unknown",
        correlationId = UUID.randomUUID().toString(),
        state = SagaStatus.STARTED,
        data = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
