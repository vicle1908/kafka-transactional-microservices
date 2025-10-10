package com.example.orders.adapter.inbound.http

import com.example.orders.application.CreateOrderCommand
import com.example.orders.application.OrderService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest,
    ): ResponseEntity<CreateOrderResponse> {
        val id =
            orderService.handle(
                CreateOrderCommand(
                    customerId = request.customerId,
                    orderItems = request.orderItems,
                ),
            )
        return ResponseEntity.ok(CreateOrderResponse(id))
    }
}

data class CreateOrderRequest(
    @field:NotBlank
    val customerId: String,
    @field:Size(min = 1, message = "orderItems must contain at least one item")
    val orderItems: List<String>,
)

data class CreateOrderResponse(
    val orderId: UUID,
)
