plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.json)
    implementation(libs.kotlin.reflect)
    // Spring Data Redis's GenericJackson2JsonRedisSerializer requires Jackson 2.x
    implementation(libs.jackson2.databind)
    implementation(libs.jackson2.datatype.jsr310)
}
