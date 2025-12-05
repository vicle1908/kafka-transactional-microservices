package com.example.temporal.testing

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

object TemporalTestcontainersSupport {
    private const val TEMPORAL_IMAGE = "temporalio/auto-setup:1.24.1"
    private const val TEMPORAL_FRONTEND_PORT = 7233
    private const val TEMPORAL_UI_PORT = 8088

    class TemporalContainer(
        imageName: String = TEMPORAL_IMAGE,
        postgresContainer: PostgreSQLContainer<*>? = null,
    ) : GenericContainer<TemporalContainer>(DockerImageName.parse(imageName)) {
        init {
            val network = Network.newNetwork()
            withNetwork(network)
            withExposedPorts(TEMPORAL_FRONTEND_PORT, TEMPORAL_UI_PORT)
            withEnv("DB", "postgresql")
            if (postgresContainer != null) {
                dependsOn(postgresContainer)
                withNetworkAliases("temporal")
                withEnv("DB_PORT", "5432")
                withEnv("POSTGRES_USER", postgresContainer.username)
                withEnv("POSTGRES_PWD", postgresContainer.password)
                withEnv("POSTGRES_SEEDS", postgresContainer.networkAliases.firstOrNull() ?: "postgres")
            } else {
                withEnv("SKIP_DEFAULT_NAMESPACE_CREATION", "false")
                withEnv("SKIP_DB_SETUP", "false")
            }
            withEnv("DYNAMIC_CONFIG_FILE_PATH", "config/dynamicconfig/development-sql.yaml")
            waitingFor(Wait.forListeningPort().withStartupTimeout(java.time.Duration.ofMinutes(3)))
        }

        fun getTemporalEndpoint(): String = "localhost:${getMappedPort(TEMPORAL_FRONTEND_PORT)}"

        fun getTemporalUIPort(): Int = getMappedPort(TEMPORAL_UI_PORT)
    }

    fun createWorkflowServiceStubs(endpoint: String): WorkflowServiceStubs =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions
                .newBuilder()
                .setTarget(endpoint)
                .build(),
        )

    fun createWorkflowServiceStubs(container: TemporalContainer): WorkflowServiceStubs = createWorkflowServiceStubs(container.getTemporalEndpoint())

    fun createWorkflowClient(
        serviceStubs: WorkflowServiceStubs,
        options: WorkflowClientOptions.Builder.() -> Unit = {},
    ): WorkflowClient {
        val clientOptions = WorkflowClientOptions.newBuilder()
        options.invoke(clientOptions)
        return WorkflowClient.newInstance(serviceStubs, clientOptions.build())
    }

    fun createWorkflowClientWithKotlinConverter(
        serviceStubs: WorkflowServiceStubs,
        options: WorkflowClientOptions.Builder.() -> Unit = {},
    ): WorkflowClient {
        val dataConverter = TemporalTestEnvironmentFactory.kotlinDataConverter()
        val clientOptions =
            WorkflowClientOptions
                .newBuilder()
                .setDataConverter(dataConverter)
        options.invoke(clientOptions)
        return WorkflowClient.newInstance(serviceStubs, clientOptions.build())
    }
}
