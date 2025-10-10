package com.example.payments.adapter.outbound.gateway

import com.example.payments.application.port.out.RefundGateway
import com.example.payments.application.port.out.RefundRequest
import com.example.payments.application.port.out.RefundResult
import com.example.payments.application.port.out.RefundResult.Completed
import com.example.payments.application.port.out.RefundResult.Failed
import com.example.payments.config.RefundGatewayProperties
import org.slf4j.LoggerFactory

class InMemoryRefundGateway(
    private val properties: RefundGatewayProperties.InMemory,
) : RefundGateway {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun refund(request: RefundRequest): RefundResult {
        if (properties.declinedPaymentIds.contains(request.paymentId)) {
            val reason = "Refund for payment ${request.paymentId} configured to fail"
            logger.warn(reason)
            return Failed(reason)
        }
        logger.info(
            "Simulating refund {} for payment {} amount {}",
            request.refundId,
            request.paymentId,
            request.amount,
        )
        return Completed
    }
}
