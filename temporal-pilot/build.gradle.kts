plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":common-temporal"))
    implementation(libs.temporal.spring.boot.starter)
    implementation(libs.temporal.sdk)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.opentracing.shim)
    implementation(libs.temporal.opentracing)

    testImplementation(libs.temporal.testing)
}
