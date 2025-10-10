package com.example.persistence.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OutboxRepository : JpaRepository<OutboxMessage, UUID>
