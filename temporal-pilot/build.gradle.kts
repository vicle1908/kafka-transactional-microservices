plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":common-temporal"))
    implementation(project(":common-observability"))
    implementation(libs.temporal.spring.boot.starter)
    implementation(libs.temporal.sdk)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.opentracing.shim)


    testImplementation(libs.temporal.testing)
}
