package com.example.saga

import java.util.UUID

class SagaAlreadyExistsException(
    sagaType: String,
    correlationId: String,
) : IllegalStateException(
        "Saga of type '$sagaType' with correlation '$correlationId' already exists",
    )

class SagaNotFoundException(
    identifier: Any,
) : NoSuchElementException(
        "Saga '$identifier' was not found",
    )

class InvalidSagaStateTransitionException(
    sagaId: UUID,
    current: SagaStatus,
    expected: SagaStatus,
) : IllegalStateException(
        "Saga '$sagaId' is in state '$current' but expected '$expected' for transition",
    )
