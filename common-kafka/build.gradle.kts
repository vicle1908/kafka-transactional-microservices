plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    api(libs.spring.kafka)
    api(libs.spring.boot.autoconfigure)
    // Spring Boot 4.0 Kafka autoconfigure module for KafkaProperties
    api("org.springframework.boot:spring-boot-kafka:${libs.versions.spring.boot.get()}")
    implementation(libs.spring.boot.starter.json)
    implementation(project(":common-observability"))
    testImplementation(libs.junit.jupiter)
}
