dependencies {
    api(libs.spring.boot.starter.data.jpa)
    implementation(libs.flyway.core)
    implementation(libs.spring.boot.starter.json)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(libs.postgresql)
}
