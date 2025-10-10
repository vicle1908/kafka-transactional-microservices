package com.example.persistence.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Suppress("JpaDataSourceORMInspection", "unused")
@Entity
@Table(name = "outbox")
class OutboxMessage(
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "headers", columnDefinition = "TEXT")
    val headers: String?,
    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        private set

    @Column(name = "published_at")
    var publishedAt: Instant? = null
        private set

    constructor() : this("", "", "", "", null, Instant.EPOCH)

    fun markPublished(publishedAt: Instant) {
        this.publishedAt = publishedAt
    }
}
