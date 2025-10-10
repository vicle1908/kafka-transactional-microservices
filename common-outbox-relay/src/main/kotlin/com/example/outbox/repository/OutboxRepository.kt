package com.example.outbox.repository

import com.example.outbox.entity.OutboxMessage
import com.example.outbox.entity.OutboxStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface OutboxRepository : JpaRepository<OutboxMessage, UUID> {
    @Query("SELECT o FROM OutboxMessage o WHERE o.status = :status ORDER BY o.occurredAt ASC")
    fun findByStatusOrderByOccurredAtAsc(
        @Param("status") status: OutboxStatus,
        pageable: Pageable,
    ): List<OutboxMessage>

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :newStatus WHERE o.id IN :ids")
    fun updateStatusForIds(
        @Param("ids") ids: List<UUID>,
        @Param("newStatus") newStatus: OutboxStatus,
    )

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :newStatus, o.publishedAt = :publishedAt WHERE o.id = :id")
    fun markAsSent(
        @Param("id") id: UUID,
        @Param("newStatus") newStatus: OutboxStatus,
        @Param("publishedAt") publishedAt: Instant,
    )

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.status = :newStatus WHERE o.id = :id")
    fun markAsFailed(
        @Param("id") id: UUID,
        @Param("newStatus") newStatus: OutboxStatus,
    )

    @Query("SELECT COUNT(o) FROM OutboxMessage o WHERE o.status = :status")
    fun countByStatus(
        @Param("status") status: OutboxStatus,
    ): Long

    @Query("SELECT o FROM OutboxMessage o WHERE o.occurredAt BETWEEN :startTime AND :endTime ORDER BY o.occurredAt ASC")
    fun findByOccurredAtBetweenOrderByOccurredAtAsc(
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant,
        pageable: Pageable,
    ): List<OutboxMessage>
}
