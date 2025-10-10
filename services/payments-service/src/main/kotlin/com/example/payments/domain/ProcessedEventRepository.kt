package com.example.payments.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProcessedEventRepository : JpaRepository<ProcessedEventEntity, UUID>
