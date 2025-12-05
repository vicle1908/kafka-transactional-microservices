plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.avro)
}

dependencies {
    api(libs.avro)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.junit.jupiter)
}

avro {
    isCreateSetters.set(false)
    fieldVisibility.set("PRIVATE")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Ensure Kotlin compilation depends on Avro Java generation
tasks.named("compileKotlin") {
    dependsOn("generateAvroJava")
}

// Add generated Avro sources to the Kotlin source set
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-main-avro-java"))
        }
    }
}
