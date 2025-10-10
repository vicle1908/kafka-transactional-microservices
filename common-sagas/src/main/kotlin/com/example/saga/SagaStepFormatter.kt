package com.example.saga

import java.time.Instant

object SagaStepFormatter {
    fun append(
        existing: String?,
        step: String,
        at: Instant,
    ): String =
        listOfNotNull(
            existing?.takeIf { it.isNotBlank() },
            "$step@$at",
        ).joinToString(SEPARATOR)

    private const val SEPARATOR = "\n"
}
