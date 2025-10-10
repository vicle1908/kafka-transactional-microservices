dependencies {
    api(libs.spring.boot.starter.data.jpa)
    implementation(libs.flyway.core)
    implementation(libs.spring.boot.starter.json)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.h2)
}
