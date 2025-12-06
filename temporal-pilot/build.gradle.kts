plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":common-temporal"))
    implementation(project(":common-observability"))
    // temporal-shaded and temporal-spring-boot-starter are provided by common-temporal
    // No need for explicit gRPC dependencies - temporal-shaded bundles its own

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.opentracing.shim)

    testImplementation(libs.temporal.testing)
}
