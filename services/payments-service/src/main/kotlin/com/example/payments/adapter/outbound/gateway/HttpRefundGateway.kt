package com.example.payments.adapter.outbound.gateway

import com.example.payments.application.port.out.RefundGateway
import com.example.payments.application.port.out.RefundRequest
import com.example.payments.application.port.out.RefundResult
import com.example.payments.application.port.out.RefundResult.Completed
import com.example.payments.application.port.out.RefundResult.Failed
import com.example.payments.config.RefundGatewayProperties
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

class HttpRefundGateway(
    private val webClient: WebClient,
    private val properties: RefundGatewayProperties.Http,
) : RefundGateway {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun refund(request: RefundRequest): RefundResult {
        val payload =
            RefundHttpRequest(
                refundId = request.refundId.toString(),
                paymentId = request.paymentId.toString(),
                orderId = request.orderId.toString(),
                amount = request.amount.toPlainString(),
                reason = request.reason,
            )

        var attempt = 0
        val maxAttempts = properties.retries.coerceAtLeast(0) + 1
        while (attempt < maxAttempts) {
            attempt++
            try {
                val response =
                    webClient
                        .post()
                        .uri(properties.refundPath)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(RefundHttpResponse::class.java)
                        .timeout(properties.readTimeout)
                        .block()
                        ?: return Failed("Refund provider returned empty response")
                return when (response.status.uppercase()) {
                    COMPLETED_STATUS -> Completed
                    FAILED_STATUS -> Failed(response.reason ?: "Refund declined by gateway")
                    else -> Failed("Unexpected refund status ${response.status}")
                }
            } catch (ex: WebClientResponseException) {
                logger.warn(
                    "Refund gateway responded with {} on attempt {} for refund {}",
                    ex.statusCode,
                    attempt,
                    request.refundId,
                    ex,
                )
                if (attempt == maxAttempts) {
                    val reason =
                        ex.responseBodyAsString?.ifBlank { null }
                            ?: "Refund provider error ${ex.statusCode.value()}"
                    return Failed(reason.take(MAX_REASON_LENGTH))
                }
            } catch (ex: Exception) {
                logger.error("Refund gateway invocation failed on attempt {}", attempt, ex)
                if (attempt == maxAttempts) {
                    return Failed(ex.message?.take(MAX_REASON_LENGTH) ?: "Refund provider failure")
                }
            }
        }
        return Failed("Refund provider attempts exhausted")
    }

    private data class RefundHttpRequest(
        val refundId: String,
        val paymentId: String,
        val orderId: String,
        val amount: String,
        val reason: String?,
    )

    private data class RefundHttpResponse(
        val status: String,
        val reason: String? = null,
    )

    private companion object {
        private const val COMPLETED_STATUS = "COMPLETED"
        private const val FAILED_STATUS = "FAILED"
        private const val MAX_REASON_LENGTH = 1024
    }
}
