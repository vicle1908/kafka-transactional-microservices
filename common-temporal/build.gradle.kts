plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    api(libs.temporal.sdk)
    api(libs.temporal.spring.boot.starter)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.kotlin.reflect)
    implementation(project(":common-proto"))
    implementation("io.micrometer:micrometer-core:1.13.0")
    implementation("io.opentelemetry:opentelemetry-api:1.44.0")

    testImplementation(libs.temporal.testing)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
