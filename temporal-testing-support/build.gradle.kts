import org.gradle.api.artifacts.VersionCatalogsExtension

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

dependencies {
    val temporalVersion = libsCatalog.findVersion("temporal").get().requiredVersion
    implementation(platform("io.temporal:temporal-bom:$temporalVersion"))
    implementation(libs.temporal.sdk)
    implementation(libs.temporal.testing)
    // Temporal SDK requires Jackson 2.x (hasn't migrated to Jackson 3.x yet)
    implementation(libs.jackson2.module.kotlin)
    implementation(libs.jackson2.datatype.jsr310)

    testFixturesImplementation(platform("io.temporal:temporal-bom:$temporalVersion"))
    testFixturesImplementation(libs.temporal.sdk)
    testFixturesImplementation(libs.temporal.testing)
    // Temporal SDK requires Jackson 2.x (hasn't migrated to Jackson 3.x yet)
    testFixturesImplementation(libs.jackson2.module.kotlin)
    testFixturesImplementation(libs.jackson2.datatype.jsr310)
    testFixturesImplementation(libs.testcontainers.junit.jupiter)
    testFixturesImplementation(libs.testcontainers.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.grpc") {
            useVersion("1.58.1")
            because("Temporal's embedded test server requires the gRPC internals present in 1.58.x")
        }
    }
}

configurations.testFixturesRuntimeClasspath {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.grpc") {
            useVersion("1.58.1")
            because("Temporal's embedded test server requires the gRPC internals present in 1.58.x")
        }
    }
}
