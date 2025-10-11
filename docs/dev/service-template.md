# Service Template

This document provides a template for creating new services in the Kafka Transactional Microservices platform.

## Project Structure

```text
service-name/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/servicename/
│   │   │   ├── ServiceNameApplication.kt
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── client/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
│       └── kotlin/com/example/servicename/
└── README.md
```

## Build Configuration

### build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    // Core dependencies
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.kafka)
    implementation(libs.flyway.core)
    implementation(libs.postgresql)
    implementation(libs.kotlin.reflect)
    implementation(libs.avro)
    
    // Shared modules
    implementation(project(":common-events"))
    implementation(project(":common-events-avro"))
    implementation(project(":common-kafka"))
    implementation(project(":common-persistence"))
    implementation(project(":common-sagas"))
    implementation(project(":common-outbox-relay"))
    implementation(project(":common-observability"))
    
    // Testing dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.h2)
    
    // Development dependencies
    developmentOnly(libs.spring.boot.devtools)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootBuildImage {
    builder.set("paketobuildpacks/builder-jammy-base")
    environment.put("BP_NATIVE_IMAGE", "false")
}
```

## Core Components

### Application Class

```kotlin
package com.example.servicename

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "com.example.servicename",
        "com.example.outbox",
        "com.example.saga",
        "com.example.kafka"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "com.example.servicename.repository",
        "com.example.outbox.repository",
        "com.example.saga.repository"
    ]
)
@EnableKafka
@EnableScheduling
class ServiceNameApplication

fun main(args: Array<String>) {
    runApplication<ServiceNameApplication>(*args)
}
```

### Domain Entities

Domain entities should follow these conventions:

1. Use JPA annotations for persistence mapping
2. Include proper equals/hashCode implementations
3. Use optimistic locking with @Version
4. Implement business logic within the entities
5. Use UUID primary keys with @UuidGenerator

Example:

```kotlin
@Entity
@Table(name = "entities")
class ServiceEntity(
    @Column(name = "name", nullable = false)
    val name: String,
    
    @Column(name = "description")
    val description: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        private set

    // Business logic methods
    fun updateDescription(description: String) {
        this.description = description
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServiceEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
```

### Repositories

Repositories should extend JpaRepository and include custom query methods:

```kotlin
@Repository
interface ServiceRepository : JpaRepository<ServiceEntity, UUID> {
    
    @Query("SELECT s FROM ServiceEntity s WHERE s.name = :name")
    fun findByName(@Param("name") name: String): ServiceEntity?
    
    @Query("SELECT s FROM ServiceEntity s WHERE s.createdAt BETWEEN :startTime AND :endTime")
    fun findByCreatedAtBetween(
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant,
        pageable: Pageable
    ): Page<ServiceEntity>
}
```

### Services

Services should handle business logic and coordinate with repositories:

```kotlin
@Service
@Transactional
class ServiceService(
    private val serviceRepository: ServiceRepository,
    private val outboxRepository: OutboxRepository
) {
    private val logger = StructuredLogger.getLogger(ServiceService::class.java)
    
    fun createService(createServiceRequest: CreateServiceRequest): ServiceEntity {
        logger.info(
            "Creating new service",
            "requestName" to createServiceRequest.name,
            "requestDescription" to createServiceRequest.description
        )
        
        // Business logic
        val entity = ServiceEntity(
            name = createServiceRequest.name,
            description = createServiceRequest.description
        )
        
        // Save entity
        val savedEntity = serviceRepository.save(entity)
        
        logger.info(
            "Service entity saved",
            "entityId" to savedEntity.id,
            "entityName" to savedEntity.name
        )
        
        // Create outbox message for events
        val outboxMessage = OutboxMessage(
            aggregateId = savedEntity.id.toString(),
            aggregateType = "service",
            eventType = "ServiceCreated",
            payload = createEventPayload(savedEntity),
            headers = """{"contentType": "application/json"}""",
            createdAt = Instant.now()
        )
        
        outboxRepository.save(outboxMessage)
        
        logger.info(
            "Outbox message created",
            "aggregateId" to outboxMessage.aggregateId,
            "eventType" to outboxMessage.eventType
        )
        
        return savedEntity
    }
    
    @Transactional(readOnly = true)
    fun getServiceById(serviceId: UUID): ServiceEntity? {
        logger.debug(
            "Retrieving service by ID",
            "serviceId" to serviceId
        )
        
        val entity = serviceRepository.findById(serviceId).orElse(null)
        
        if (entity != null) {
            logger.debug(
                "Service found",
                "serviceId" to serviceId,
                "serviceName" to entity.name
            )
        } else {
            logger.warn(
                "Service not found",
                "serviceId" to serviceId
            )
        }
        
        return entity
    }
}
```

Remember to add the import for StructuredLogger:

```kotlin
import com.example.observability.StructuredLogger
```

### Controllers

Controllers should handle HTTP requests and delegate to services. Include structured logging to capture request/response information:

```kotlin
@RestController
@RequestMapping("/api/services")
@Validated
class ServiceController(
    private val serviceService: ServiceService
) {
    private val logger = StructuredLogger.getLogger(ServiceController::class.java)
    
    @PostMapping
    fun createService(@RequestBody @Valid createServiceRequest: CreateServiceRequestDto): ResponseEntity<ServiceResponse> {
        logger.info(
            "Received create service request",
            "requestName" to createServiceRequest.name,
            "requestDescription" to createServiceRequest.description
        )
        
        val request = CreateServiceRequest(
            name = createServiceRequest.name,
            description = createServiceRequest.description
        )
        
        val entity = serviceService.createService(request)
        val response = ServiceResponse.fromEntity(entity)
        
        logger.info(
            "Service created successfully",
            "entityId" to entity.id,
            "entityName" to entity.name
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping("/{serviceId}")
    fun getServiceById(@PathVariable serviceId: UUID): ResponseEntity<ServiceResponse> {
        logger.debug(
            "Received get service request",
            "serviceId" to serviceId
        )
        
        val entity = serviceService.getServiceById(serviceId)
        
        if (entity != null) {
            val response = ServiceResponse.fromEntity(entity)
            logger.debug(
                "Service retrieved successfully", 
                "serviceId" to serviceId,
                "entityName" to entity.name
            )
            return ResponseEntity.ok(response)
        } else {
            logger.warn(
                "Service not found",
                "serviceId" to serviceId
            )
            return ResponseEntity.notFound().build()
        }
    }
}
```

Remember to add the import for StructuredLogger:

```kotlin
import com.example.observability.StructuredLogger
```

### Kafka Listeners

Kafka listeners should handle events from other services:

```kotlin
@Component
class ServiceEventListener(
    private val serviceService: ServiceService
) {
    private val logger = StructuredLogger.getLogger(ServiceEventListener::class.java)
    
    @KafkaListener(topics = ["orders"], groupId = "service-name")
    fun handleOrderEvent(
        @Payload payload: String,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long
    ) {
        try {
            logger.info(
                "Received order event",
                "payload" to payload,
                "key" to key,
                "timestamp" to timestamp
            )
            // Process the event
        } catch (e: Exception) {
            logger.error(
                "Failed to process order event",
                "payload" to payload,
                "key" to key,
                "timestamp" to timestamp,
                "error" to e.message
            )
        }
    }
}
```

Remember to add the import for StructuredLogger:

```kotlin
import com.example.observability.StructuredLogger
```

## Configuration (env-driven; prefer placeholders with safe fallbacks)

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: service-name
  datasource:
    url: ${SERVICE_DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${SERVICE_DB_NAME:servicename}}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:app}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    properties:
      schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      enable-idempotence: true
      acks: all
      retries: 2147483647
      transaction-id-prefix: ${SERVICE_KAFKA_TX_PREFIX:service-tx-}
    consumer:
      group-id: service-name
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false
      isolation-level: read_committed

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

See `docs/dev/env-reference.md` for the canonical list of variables and defaults. Copy `.env.example` to `.env` and use direnv or `source scripts/export-env.sh` to load them locally.

## Database Migrations

Database migrations should follow Flyway conventions:

```sql
-- Create entities table
CREATE TABLE entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_entities_name ON entities(name);
CREATE INDEX idx_entities_created_at ON entities(created_at);

-- Constraints
ALTER TABLE entities ADD CONSTRAINT uk_entities_name UNIQUE (name);

-- Comments
COMMENT ON TABLE entities IS 'Service entities';
COMMENT ON COLUMN entities.id IS 'Unique identifier';
COMMENT ON COLUMN entities.name IS 'Entity name';
COMMENT ON COLUMN entities.description IS 'Entity description';
COMMENT ON COLUMN entities.created_at IS 'Creation timestamp';
COMMENT ON COLUMN entities.updated_at IS 'Last update timestamp';
COMMENT ON COLUMN entities.version IS 'Optimistic locking version';
```

## Testing

### Unit Tests

```kotlin
@ExtendWith(MockKExtension::class)
class ServiceServiceTest {
    
    @MockK
    private lateinit var serviceRepository: ServiceRepository
    
    @InjectMockKs
    private lateinit var serviceService: ServiceService
    
    @Test
    fun `should create service successfully`() {
        // Given
        val createRequest = CreateServiceRequest("Test Service", "Test Description")
        val entity = ServiceEntity("Test Service", "Test Description")
        entity.id = UUID.randomUUID()
        
        every { serviceRepository.save(any()) } answers { firstArg() }
        
        // When
        val result = serviceService.createService(createRequest)
        
        // Then
        assertNotNull(result)
        assertEquals("Test Service", result.name)
        assertEquals("Test Description", result.description)
        
        verify(exactly = 1) { serviceRepository.save(any()) }
    }
}
```

### Integration Tests

```kotlin
@SpringBootTest
@Testcontainers
class ServiceIntegrationTest {
    
    companion object {
        @Container
        val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:18")
            .apply {
                withDatabaseName("test")
                withUsername("test")
                withPassword("test")
            }
        
        @Container
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"))
            .apply {
                // KRaft mode without ZooKeeper
            }
        
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }
    
    @Autowired
    private lateinit var serviceService: ServiceService
    
    @Test
    @Sql(scripts = ["classpath:db/test-data.sql"])
    fun `should retrieve service by id`() {
        // Given
        val serviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        
        // When
        val result = serviceService.getServiceById(serviceId)
        
        // Then
        assertNotNull(result)
        assertEquals(serviceId, result?.id)
    }
}
```

## Documentation

Each service should include:

1. README.md with service overview
2. API documentation
3. Configuration documentation
4. Deployment documentation
5. Monitoring and alerting documentation

## Best Practices

1. **Transaction Management**: Use Spring's @Transactional annotation appropriately
2. **Error Handling**: Implement proper exception handling and logging
3. **Security**: Apply security measures for endpoints and data access
4. **Monitoring**: Include health checks and metrics
5. **Testing**: Write comprehensive unit and integration tests
6. **Documentation**: Maintain up-to-date documentation
7. **Versioning**: Follow semantic versioning for APIs and services
8. **Configuration**: Use externalized configuration with sensible defaults
9. **Logging**: Implement structured logging with appropriate levels
10. **Observability**: Include distributed tracing and metrics collection
