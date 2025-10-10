package com.example.payments.adapter.inbound.http

import com.example.payments.application.PaymentProcessingOutcome
import com.example.payments.application.PaymentService
import com.example.payments.application.ProcessPaymentCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentService: PaymentService,
) {
    @PostMapping
    fun processPayment(
        @Valid @RequestBody request: ProcessPaymentRequest,
    ): ResponseEntity<ProcessPaymentResponse> {
        val outcome =
            paymentService.handle(
                ProcessPaymentCommand(
                    orderId = request.orderId,
                    amount = request.amount,
                ),
            )
        return when (outcome) {
            is PaymentProcessingOutcome.Completed ->
                ResponseEntity.ok(
                    ProcessPaymentResponse(
                        status = "COMPLETED",
                        paymentId = outcome.paymentId,
                        failureReason = null,
                    ),
                )

            is PaymentProcessingOutcome.AlreadyProcessed ->
                ResponseEntity.ok(
                    ProcessPaymentResponse(
                        status = "ALREADY_PROCESSED",
                        paymentId = outcome.paymentId,
                        failureReason = null,
                    ),
                )

            is PaymentProcessingOutcome.Failed ->
                ResponseEntity
                    .unprocessableEntity()
                    .body(
                        ProcessPaymentResponse(
                            status = "FAILED",
                            paymentId = outcome.paymentId,
                            failureReason = outcome.reason,
                        ),
                    )
        }
    }
}

data class ProcessPaymentRequest(
    @field:NotNull
    val orderId: UUID,
    @field:Positive
    val amount: BigDecimal,
)

data class ProcessPaymentResponse(
    val status: String,
    val paymentId: UUID?,
    val failureReason: String? = null,
)
