import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

// This is a shared library module; disable bootJar to avoid requiring a main class
tasks.named<BootJar>("bootJar") {
    enabled = false
}

dependencies {
    api(libs.spring.boot.starter.data.jpa)
    implementation(libs.flyway.core)
    implementation(libs.kotlin.reflect)
    api(libs.spring.boot.starter.actuator)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)

    // Dependencies from other common modules
    implementation(project(":common-persistence"))
    implementation(project(":common-kafka"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
