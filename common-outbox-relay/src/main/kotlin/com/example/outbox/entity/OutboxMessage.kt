package com.example.outbox.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Suppress("LongParameterList")
@Entity
@Table(name = "outbox")
class OutboxMessage(
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "headers", columnDefinition = "TEXT")
    val headers: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant = Instant.now(),
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
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

    fun markAsSent(publishedAt: Instant = Instant.now()) {
        this.status = OutboxStatus.SENT
        this.publishedAt = publishedAt
    }

    fun markAsFailed() {
        this.status = OutboxStatus.FAILED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutboxMessage) return false

        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class OutboxStatus {
    PENDING,
    SENT,
    FAILED,
}
