@file:Suppress("JpaDataSourceORMInspection", "unused")

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
@Table(name = "payments")
class PaymentEntity(
    @Column(name = "order_id", nullable = false)
    val orderId: UUID,
    @Column(name = "amount", nullable = false)
    val amount: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private var status: PaymentStatus,
    @Column(name = "processed_at", nullable = false)
    private var processedAt: Instant,
    @Column(name = "failure_reason")
    private var failureReason: String? = null,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    constructor() : this(UUID.randomUUID(), BigDecimal.ZERO, PaymentStatus.PENDING, Instant.EPOCH)

    fun status(): PaymentStatus = status

    fun processedAt(): Instant = processedAt

    fun failureReason(): String? = failureReason

    fun markCompleted(at: Instant) {
        status = PaymentStatus.COMPLETED
        processedAt = at
        failureReason = null
    }

    fun markFailed(
        at: Instant,
        reason: String?,
    ) {
        status = PaymentStatus.FAILED
        processedAt = at
        failureReason = reason?.take(MAX_FAILURE_REASON_LENGTH)
    }

    fun markRefunding(at: Instant) {
        status = PaymentStatus.REFUNDING
        processedAt = at
        failureReason = null
    }

    fun markRefunded(at: Instant) {
        status = PaymentStatus.REFUNDED
        processedAt = at
        failureReason = null
    }

    companion object {
        private const val MAX_FAILURE_REASON_LENGTH = 1024
    }
}
