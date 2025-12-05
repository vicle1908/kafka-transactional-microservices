plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Core dependencies
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.kafka)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.kotlin.reflect)
    implementation(libs.avro)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":common-temporal"))
    implementation(project(":common-proto"))
    implementation(libs.grpc.netty)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    runtimeOnly(libs.grpc.netty.shaded)

    // Shared modules
    implementation(project(":common-events"))
    implementation(project(":common-events-avro"))
    implementation(project(":common-kafka"))
    implementation(project(":common-persistence"))
    implementation(project(":common-sagas"))
    implementation(project(":common-outbox-relay"))
    implementation(project(":common-observability"))
    implementation(project(":common-cache"))

    // Testing dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.okhttp.mockwebserver)
    rootProject.findProject("temporal-testing-support")?.let { temporalTesting ->
        testImplementation(testFixtures(temporalTesting))
    }

    // Development dependencies
    developmentOnly(libs.spring.boot.devtools)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootBuildImage {
    builder.set("paketobuildpacks/builder-jammy-base")
    environment.put("BP_NATIVE_IMAGE", "false")
}
