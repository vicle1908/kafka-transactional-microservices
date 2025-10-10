plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    api(libs.spring.boot.starter)
    api(libs.kotlin.reflect)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
