package com.example.orders.query

import com.example.orders.domain.OrderItemRepository
import com.example.orders.domain.OrderRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrdersQueryService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["orders:by-id"], key = "#orderId")
    fun getOrder(orderId: UUID): OrderDto? {
        val order = orderRepository.findById(orderId).orElse(null) ?: return null
        val orderItems = orderItemRepository.findAllByOrderIdOrderByCreatedAt(orderId)
        return order.toDto(orderItems)
    }

    @Transactional(readOnly = true)
    fun getOrderAmount(orderId: UUID): java.math.BigDecimal? {
        val order = orderRepository.findById(orderId).orElse(null)
        return order?.totalAmount
    }
}
