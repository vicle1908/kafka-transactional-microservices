package com.example.saga

enum class SagaStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    FAILED,
}
