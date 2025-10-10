package com.example.saga

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class SagaMetricsRecorder(
    private val meterRegistry: MeterRegistry,
) {
    fun recordStep(
        sagaType: String,
        step: String,
        state: SagaStatus,
    ) {
        val counter =
            meterRegistry.counter(
                STEP_COUNTER_NAME,
                listOf(
                    Tag.of(SAGA_TYPE_TAG, sagaType),
                    Tag.of(STEP_TAG, step),
                    Tag.of(STATE_TAG, state.name),
                ),
            )
        counter.increment()
    }

    companion object {
        private const val STEP_COUNTER_NAME = "saga.step.processed"
        private const val SAGA_TYPE_TAG = "sagaType"
        private const val STEP_TAG = "step"
        private const val STATE_TAG = "state"
    }
}
