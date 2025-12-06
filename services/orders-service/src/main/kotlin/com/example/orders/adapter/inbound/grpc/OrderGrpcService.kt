@file:Suppress("TooGenericExceptionCaught", "LongMethod")

package com.example.orders.adapter.inbound.grpc

import com.example.observability.StructuredLogger
import com.example.orders.proto.GetOrderRequest
import com.example.orders.proto.GetOrderResponse
import com.example.orders.proto.OrderServiceGrpc
import com.example.orders.query.OrdersQueryService
import io.grpc.stub.StreamObserver
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrderGrpcService(
    private val ordersQueryService: OrdersQueryService,
) : OrderServiceGrpc.OrderServiceImplBase() {
    private val logger = StructuredLogger.getLogger(OrderGrpcService::class.java)

    override fun getOrder(
        request: GetOrderRequest,
        responseObserver: StreamObserver<GetOrderResponse>,
    ) {
        try {
            val orderId = UUID.fromString(request.orderId)
            logger.info("Received gRPC GetOrder request", "orderId" to orderId)

            val orderDto = ordersQueryService.getOrder(orderId)

            if (orderDto == null) {
                val errorResponse =
                    GetOrderResponse
                        .newBuilder()
                        .setOrderId(request.orderId)
                        .setStatus("NOT_FOUND")
                        .build()
                responseObserver.onNext(errorResponse)
                responseObserver.onCompleted()
                logger.warn("Order not found", "orderId" to orderId)
                return
            }

            val customerInfo =
                com.example.orders.proto.CustomerInfo
                    .newBuilder()
                    .setCustomerId(orderDto.customerId)
                    .build()

            val orderItems =
                orderDto.orderItems.map { item ->
                    com.example.orders.proto.OrderItem
                        .newBuilder()
                        .setProductId(item.productId)
                        .setProductName(item.productName)
                        .setQuantity(item.quantity)
                        .setUnitPrice(item.unitPrice.toDouble())
                        .build()
                }

            val response =
                GetOrderResponse
                    .newBuilder()
                    .setOrderId(orderDto.id.toString())
                    .setCustomer(customerInfo)
                    .addAllItems(orderItems)
                    .setStatus(orderDto.status.name)
                    .setTotalAmount(orderDto.totalAmount.toDouble())
                    .setCreatedAt(orderDto.createdAt.toEpochMilli())
                    .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

            logger.info(
                "gRPC GetOrder response sent",
                "orderId" to orderId,
                "itemCount" to orderItems.size,
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid orderId format in gRPC request", "orderId" to request.orderId, "error" to e.message)
            val errorResponse =
                GetOrderResponse
                    .newBuilder()
                    .setOrderId(request.orderId)
                    .setStatus("INVALID_REQUEST")
                    .build()
            responseObserver.onNext(errorResponse)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Error processing gRPC GetOrder request", "orderId" to request.orderId, "error" to e.message)
            responseObserver.onError(e)
        }
    }
}
