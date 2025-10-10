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
