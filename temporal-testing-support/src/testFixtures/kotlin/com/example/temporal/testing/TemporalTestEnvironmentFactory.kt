package com.example.temporal.testing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.converter.ByteArrayPayloadConverter
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.common.converter.NullPayloadConverter
import io.temporal.common.converter.ProtobufJsonPayloadConverter
import io.temporal.testing.TestEnvironmentOptions
import io.temporal.testing.TestWorkflowEnvironment

object TemporalTestEnvironmentFactory {
    fun newInstance(
        configureClient: (WorkflowClientOptions.Builder.() -> Unit)? = null,
        configureOptions: (TestEnvironmentOptions.Builder.() -> Unit)? = null,
    ): TestWorkflowEnvironment {
        val dataConverter = kotlinDataConverter()

        val clientBuilder =
            WorkflowClientOptions
                .newBuilder()
                .setDataConverter(dataConverter)
        configureClient?.invoke(clientBuilder)

        val optionsBuilder =
            TestEnvironmentOptions
                .newBuilder()
                .setUseExternalService(false)
                .setWorkflowClientOptions(clientBuilder.build())
        configureOptions?.invoke(optionsBuilder)

        return TestWorkflowEnvironment.newInstance(optionsBuilder.build())
    }

    fun kotlinDataConverter(): DataConverter =
        DefaultDataConverter(
            JacksonJsonPayloadConverter(kotlinObjectMapper()),
            ByteArrayPayloadConverter(),
            ProtobufJsonPayloadConverter(),
            NullPayloadConverter(),
        )

    fun kotlinObjectMapper(): ObjectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
