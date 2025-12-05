pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
//    versionCatalogs {
//        create("libs") {
//            from(files("gradle/libs.versions.toml"))
//        }
//    }
}

rootProject.name = "microservices"

include(
    "common-events",
    "common-kafka",
    "common-persistence",
    "common-sagas",
    "common-events-avro",
    "common-proto",
    "common-temporal",
    "common-outbox-relay",
    "common-observability",
    "common-cache",
    "services:orders-service",
    "services:payments-service",
    "services:inventory-service",
    "services:notification-service",
)

// Include optional modules only if directories exist (for local development)
if (file("temporal-testing-support").exists()) {
    include("temporal-testing-support")
}
if (file("temporal-pilot").exists()) {
    include("temporal-pilot")
}
if (file("services/api-gateway").exists()) {
    include("services:api-gateway")
}