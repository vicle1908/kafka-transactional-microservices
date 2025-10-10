package com.example.saga

import java.time.Instant

data class SagaTransitionOptions(
    val expectedState: SagaStatus? = null,
    val dataTransformer: (String?) -> String? = { it },
    val occurredAt: Instant? = null,
)
