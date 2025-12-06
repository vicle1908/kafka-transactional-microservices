package com.example.temporal.testing

import com.example.temporal.testing.TemporalTestEnvironmentFactory
import io.temporal.client.WorkflowOptions
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.io.use

class TemporalTestEnvironmentFactoryTest {
    @Test
    fun `factory configures kotlin aware data converter`() {
        TemporalTestEnvironmentFactory.newInstance().use { env ->
            val worker = env.newWorker(TEST_QUEUE)
            worker.registerWorkflowImplementationTypes(SampleWorkflowImpl::class.java)

            env.start()

            val stub =
                env.workflowClient.newWorkflowStub(
                    SampleWorkflow::class.java,
                    WorkflowOptions.newBuilder().setTaskQueue(TEST_QUEUE).build(),
                )

            val payload =
                SamplePayload(
                    orderId = UUID.randomUUID(),
                    metadata = mapOf("attempts" to 2, "priority" to 5),
                )

            val result = stub.process(payload)

            assertThat(result.orderId).isEqualTo(payload.orderId)
            assertThat(result.metadata).isEqualTo(payload.metadata)
            assertThat(result.processedAt)
                .describedAs("processedAt should be set using workflow clock")
                .isNotNull
                .isBetween(
                    Instant.ofEpochMilli(env.currentTimeMillis()).minusSeconds(60),
                    Instant.ofEpochMilli(env.currentTimeMillis()).plusSeconds(60),
                )
        }
    }

    @WorkflowInterface
    interface SampleWorkflow {
        @WorkflowMethod
        fun process(payload: SamplePayload): SamplePayload
    }

    class SampleWorkflowImpl : SampleWorkflow {
        override fun process(payload: SamplePayload): SamplePayload = payload.copy(processedAt = Instant.ofEpochMilli(Workflow.currentTimeMillis()))
    }

    data class SamplePayload(
        val orderId: UUID,
        val metadata: Map<String, Int>,
        val processedAt: Instant? = null,
    )

    private companion object {
        const val TEST_QUEUE = "temporal-test-environment-factory"
    }
}
