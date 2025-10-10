dependencies {
    api(libs.spring.kafka)
    api(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.starter.json)
    implementation(project(":common-observability"))
    testImplementation(libs.junit.jupiter)
}
