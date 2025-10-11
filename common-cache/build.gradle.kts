plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.json)
    implementation(libs.kotlin.reflect)
}
