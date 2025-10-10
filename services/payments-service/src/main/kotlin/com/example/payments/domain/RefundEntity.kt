@file:Suppress("JpaDataSourceORMInspection")

package com.example.payments.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refunds")
class RefundEntity(
    @Column(name = "payment_id", nullable = false)
    val paymentId: UUID,
    @Column(name = "amount", nullable = false)
    val amount: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private var status: RefundStatus,
    @Column(name = "requested_at", nullable = false)
    val requestedAt: Instant,
    @Column(name = "reason")
    val reason: String? = null,
    @Column(name = "completed_at")
    private var completedAt: Instant? = null,
    @Column(name = "failure_reason")
    private var failureReason: String? = null,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    constructor() : this(UUID.randomUUID(), BigDecimal.ZERO, RefundStatus.PENDING, Instant.EPOCH)

    fun status(): RefundStatus = status

    fun completedAt(): Instant? = completedAt

    fun failureReason(): String? = failureReason

    fun markCompleted(at: Instant) {
        status = RefundStatus.COMPLETED
        completedAt = at
        failureReason = null
    }

    fun markFailed(
        at: Instant,
        reason: String?,
    ) {
        status = RefundStatus.FAILED
        completedAt = at
        failureReason = reason?.take(MAX_FAILURE_REASON_LENGTH)
    }

    companion object {
        private const val MAX_FAILURE_REASON_LENGTH = 1024
    }
}
