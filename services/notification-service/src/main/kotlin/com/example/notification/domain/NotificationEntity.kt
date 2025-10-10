@file:Suppress("JpaDataSourceORMInspection", "unused", "LongParameterList")

package com.example.notification.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    @Column(name = "channel", nullable = false)
    val channel: String,
    @Column(name = "template", nullable = false)
    val template: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private var status: NotificationStatus,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    private var updatedAt: Instant,
    @Column(name = "failure_reason")
    private var failureReason: String? = null,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        private set

    fun status(): NotificationStatus = status

    fun updatedAt(): Instant = updatedAt

    fun failureReason(): String? = failureReason

    fun markSent(at: Instant): NotificationEntity {
        status = NotificationStatus.SENT
        updatedAt = at
        failureReason = null
        return this
    }

    fun markFailed(
        at: Instant,
        reason: String?,
    ): NotificationEntity {
        status = NotificationStatus.FAILED
        updatedAt = at
        failureReason = reason?.take(MAX_FAILURE_REASON_LENGTH)
        return this
    }

    companion object {
        private const val MAX_FAILURE_REASON_LENGTH = 512
    }

    constructor() : this(UUID.randomUUID(), "", "", "", NotificationStatus.QUEUED, Instant.EPOCH, Instant.EPOCH)
}
