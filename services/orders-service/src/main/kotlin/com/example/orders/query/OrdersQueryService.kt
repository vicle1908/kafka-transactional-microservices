package com.example.orders.query

import com.example.orders.domain.OrderRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrdersQueryService(
    private val orderRepository: OrderRepository,
) {
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["orders:by-id"], key = "#orderId")
    fun getOrder(orderId: UUID): OrderDto? = orderRepository.findById(orderId).orElse(null)?.toDto()
}
