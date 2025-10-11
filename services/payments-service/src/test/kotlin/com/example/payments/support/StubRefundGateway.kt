package com.example.payments.support

import com.example.payments.application.port.out.RefundGateway
import com.example.payments.application.port.out.RefundRequest
import com.example.payments.application.port.out.RefundResult
import com.example.payments.application.port.out.RefundResult.Completed
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Simple in-memory stub used by integration tests to deterministically control refund outcomes
 * without relying on mocking frameworks.
 */
class StubRefundGateway : RefundGateway {
    @Volatile
    private var defaultResult: RefundResult = Completed

    private val queuedResults = ConcurrentLinkedQueue<RefundResult>()
    private val recordedRequests = CopyOnWriteArrayList<RefundRequest>()

    val requests: List<RefundRequest>
        get() = recordedRequests.toList()

    fun reset(defaultResult: RefundResult = Completed) {
        this.defaultResult = defaultResult
        queuedResults.clear()
        recordedRequests.clear()
    }

    fun enqueue(result: RefundResult) {
        queuedResults.add(result)
    }

    fun setDefault(result: RefundResult) {
        this.defaultResult = result
    }

    override fun refund(request: RefundRequest): RefundResult {
        recordedRequests.add(request)
        return queuedResults.poll() ?: defaultResult
    }
}
