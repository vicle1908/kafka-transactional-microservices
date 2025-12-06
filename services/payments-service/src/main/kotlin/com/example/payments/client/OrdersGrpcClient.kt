package com.example.payments.client

import com.example.observability.StructuredLogger
import com.example.orders.proto.GetOrderRequest
import com.example.orders.proto.OrderServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class OrdersGrpcClient(
    @Value("\${orders.grpc.host:localhost}") private val ordersGrpcHost: String,
    @Value("\${orders.grpc.port:9090}") private val ordersGrpcPort: Int,
) {
    private val logger = StructuredLogger.getLogger(OrdersGrpcClient::class.java)
    private var channel: ManagedChannel? = null
    private var orderServiceStub: OrderServiceGrpc.OrderServiceBlockingStub? = null

    @EventListener(ApplicationReadyEvent::class)
    fun initializeChannel() {
        channel =
            ManagedChannelBuilder
                .forAddress(ordersGrpcHost, ordersGrpcPort)
                .usePlaintext()
                .build()
        orderServiceStub = OrderServiceGrpc.newBlockingStub(channel)
        logger.info(
            "Orders gRPC client initialized",
            "host" to ordersGrpcHost,
            "port" to ordersGrpcPort,
        )
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun getOrderAmount(orderId: UUID): BigDecimal? {
        val stub =
            orderServiceStub ?: run {
                logger.warn("Orders gRPC client not initialized", "orderId" to orderId)
                return null
            }

        return try {
            val request = GetOrderRequest.newBuilder().setOrderId(orderId.toString()).build()
            logger.debug("Calling orders-service gRPC GetOrder", "orderId" to orderId)
            val response = stub.getOrder(request)

            when (response.status) {
                "NOT_FOUND", "INVALID_REQUEST" -> {
                    logger.warn(
                        "Order not found or invalid via gRPC",
                        "orderId" to orderId,
                        "status" to response.status,
                    )
                    null
                }

                else -> {
                    val amount = BigDecimal.valueOf(response.totalAmount)
                    logger.info(
                        "Retrieved order amount via gRPC",
                        "orderId" to orderId,
                        "amount" to amount,
                    )
                    amount
                }
            }
        } catch (e: io.grpc.StatusRuntimeException) {
            logger.error(
                "gRPC error calling orders-service",
                "orderId" to orderId,
                "error" to (e.message ?: "Unknown"),
            )
            null
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Invalid argument in gRPC call",
                "orderId" to orderId,
                "error" to (e.message ?: "Unknown"),
            )
            null
        }
    }

    @PreDestroy
    fun shutdownChannel() {
        channel?.shutdown()
        logger.info("Orders gRPC client shutdown")
    }
}
