package com.example.payments.adapter.outbound.gateway

import com.example.payments.application.port.out.PaymentChargeRequest
import com.example.payments.application.port.out.PaymentChargeResult
import com.example.payments.application.port.out.PaymentChargeResult.Approved
import com.example.payments.application.port.out.PaymentChargeResult.Declined
import com.example.payments.application.port.out.PaymentGateway
import com.example.payments.config.PaymentGatewayProperties
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigDecimal
import java.util.UUID

class HttpPaymentGateway(
    private val webClient: WebClient,
    private val properties: PaymentGatewayProperties.Http,
) : PaymentGateway {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun charge(request: PaymentChargeRequest): PaymentChargeResult {
        val payload =
            ChargeRequest(
                paymentId = request.paymentId,
                orderId = request.orderId,
                amount = request.amount,
                metadata = request.metadata,
            )

        val maxRetries = properties.retries.coerceAtLeast(0)
        var attempt = 0
        while (true) {
            attempt++
            try {
                val response =
                    webClient
                        .post()
                        .uri(resolvePath(properties.chargePath))
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(ChargeResponse::class.java)
                        .block(properties.readTimeout)
                        ?: return Declined("Empty response from payment provider")

                return mapOutcome(response)
            } catch (ex: WebClientResponseException) {
                logger.warn("Payment gateway responded with status {} on attempt {}", ex.statusCode, attempt)
                if (attempt > maxRetries) {
                    val reason =
                        ex.responseBodyAsString
                            ?.takeIf { it.isNotBlank() }
                            ?.let { it.take(MAX_REASON_LENGTH) }
                            ?: "Gateway responded with status ${ex.statusCode.value()}"
                    return Declined(reason)
                }
            } catch (ex: Exception) {
                logger.error("Payment gateway invocation failed on attempt {}", attempt, ex)
                if (attempt > maxRetries) {
                    val message = ex.message?.take(MAX_REASON_LENGTH) ?: "Gateway invocation error"
                    return Declined(message)
                }
            }
        }
    }

    private fun mapOutcome(response: ChargeResponse): PaymentChargeResult =
        when (response.status.uppercase()) {
            APPROVED -> {
                val confirmation = response.confirmationCode
                if (confirmation.isNullOrBlank()) {
                    Declined("Gateway approval missing confirmation code")
                } else {
                    Approved(confirmation)
                }
            }

            DECLINED ->
                Declined(response.declineReason?.take(MAX_REASON_LENGTH) ?: "Gateway declined payment")

            else -> Declined("Unexpected gateway status ${response.status}")
        }

    private fun resolvePath(path: String): String =
        if (path.startsWith("/")) {
            path
        } else {
            "/$path"
        }

    private data class ChargeRequest(
        val paymentId: UUID,
        val orderId: UUID,
        val amount: BigDecimal,
        val metadata: Map<String, String>,
    )

    private data class ChargeResponse(
        val status: String,
        val confirmationCode: String? = null,
        val declineReason: String? = null,
    )

    private companion object {
        private const val MAX_REASON_LENGTH = 256
        private const val APPROVED = "APPROVED"
        private const val DECLINED = "DECLINED"
    }
}
