plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Use temporal-shaded to avoid gRPC version conflicts
    // temporal-shaded relocates gRPC/Netty/Protobuf to io.temporal.shaded.* packages
    api(libs.temporal.shaded)
    api(libs.temporal.spring.boot.starter)

    // Project's gRPC dependencies for common-proto and other gRPC services
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.netty.shaded)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.kotlin.reflect)
    implementation(project(":common-proto"))
    implementation("io.micrometer:micrometer-core:1.13.0")
    implementation("io.opentelemetry:opentelemetry-api:1.44.0")
    implementation(libs.temporal.opentracing)
    implementation(libs.opentelemetry.opentracing.shim)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.44.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
