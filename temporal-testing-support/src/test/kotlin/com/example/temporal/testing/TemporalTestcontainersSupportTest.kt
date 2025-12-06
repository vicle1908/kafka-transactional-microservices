package com.example.temporal.testing

import io.temporal.client.WorkflowClient
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class TemporalTestcontainersSupportTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:15.3").apply {
            withDatabaseName("temporal_test")
            withUsername("temporal")
            withPassword("temporal")
        }

    @Test
    fun `can create workflow client from container`() {
        val temporalContainer = TemporalTestcontainersSupport.TemporalContainer(postgresContainer = postgres)
        temporalContainer.start()

        try {
            val serviceStubs = TemporalTestcontainersSupport.createWorkflowServiceStubs(temporalContainer)
            val workflowClient = TemporalTestcontainersSupport.createWorkflowClientWithKotlinConverter(serviceStubs)

            assertThat(workflowClient).isNotNull
            assertThat(temporalContainer.getTemporalEndpoint()).isNotEmpty
        } finally {
            temporalContainer.stop()
        }
    }

    @WorkflowInterface
    interface TestWorkflow {
        @WorkflowMethod
        fun execute(input: String): String
    }
}
