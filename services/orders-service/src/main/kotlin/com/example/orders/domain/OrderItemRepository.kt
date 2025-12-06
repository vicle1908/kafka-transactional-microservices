package com.example.orders.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderItemRepository : JpaRepository<OrderItemEntity, UUID> {
    fun findAllByOrderIdOrderByCreatedAt(orderId: UUID): List<OrderItemEntity>
}
