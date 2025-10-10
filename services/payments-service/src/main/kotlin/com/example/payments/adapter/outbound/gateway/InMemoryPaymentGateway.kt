package com.example.payments.adapter.outbound.gateway

import com.example.payments.application.port.out.PaymentChargeRequest
import com.example.payments.application.port.out.PaymentChargeResult
import com.example.payments.application.port.out.PaymentChargeResult.Approved
import com.example.payments.application.port.out.PaymentChargeResult.Declined
import com.example.payments.application.port.out.PaymentGateway
import com.example.payments.config.PaymentGatewayProperties
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class InMemoryPaymentGateway(
    private val properties: PaymentGatewayProperties.InMemory,
) : PaymentGateway {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun charge(request: PaymentChargeRequest): PaymentChargeResult {
        logger.info(
            "Processing payment {} for order {} amount {}",
            request.paymentId,
            request.orderId,
            request.amount,
        )

        if (properties.declinedOrderIds.contains(request.orderId)) {
            val reason = "Order ${request.orderId} configured to decline"
            logger.warn(reason)
            return Declined(reason)
        }

        if (request.amount > properties.approvalLimit) {
            val reason = "Amount ${request.amount} exceeds approval limit ${properties.approvalLimit}"
            logger.warn(reason)
            return Declined(reason)
        }

        if (request.amount <= BigDecimal.ZERO) {
            val reason = "Amount must be positive"
            logger.warn(reason)
            return Declined(reason)
        }

        return Approved(
            confirmationCode = "CONF-${request.paymentId}",
        )
    }
}
